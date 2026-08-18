package com.mytask.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mytask.Notification.NotificationHelper
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.ScheduleTimeRange
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.SyncTombstoneEntity
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.local.getTimeRanges
import com.mytask.data.local.toDisplayTime
import com.mytask.data.local.toJsonString
import com.mytask.data.local.toMinuteOfDayOrNull
import com.mytask.data.local.toScheduleTimeRangesOrNull
import com.mytask.debug.AuthDebugLog
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudDataSyncRepositoryEntityMerge @Inject constructor(
    private val context: Context,
    private val database: MyTaskDatabase,
    private val userProfileRepository: UserProfileRepository
) {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TIMEOUT = 15_000L
        private const val COURSE = "course"
        private const val TASK = "task"
        private const val SCHEDULE = "schedule"
    }

    private data class Snapshot(
        val courses: List<CourseEntity>,
        val tasks: List<TaskEntity>,
        val schedules: List<ScheduleEntity>,
        val tombstones: List<SyncTombstoneEntity>
    )

    private fun doc(uid: String) = firestore.collection("users").document(uid)
        .collection("backups").document("academic")

    suspend fun sync(uid: String, mode: Mode = Mode.MERGE): Boolean {
        require(uid.isNotBlank())
        val cloud = withTimeout(TIMEOUT) { doc(uid).get().await() }
        val cloudJson = cloud.getString("dataJson")
        val cloudVersion = cloud.getLong("updatedAt") ?: 0L

        if (cloudJson.isNullOrBlank()) {
            val local = readLocal()
            publish(uid, local, cloudVersion)
            return true
        }

        return if (mode == Mode.REPLACE_CLOUD) {
            val incoming = parse(cloudJson)
            replaceLocal(incoming)
            userProfileRepository.saveCloudSyncState(cloudVersion, hash(cloudJson))
            refreshScheduler()
            true
        } else {
            val local = readLocal()
            val remote = parse(cloudJson)
            val merged = merge(local, remote)
            publish(uid, merged, cloudVersion)
            true
        }
    }

    suspend fun uploadLocal(uid: String) {
        val local = readLocal()
        val cloud = withTimeout(TIMEOUT) { doc(uid).get().await() }
        val cloudVersion = cloud.getLong("updatedAt") ?: 0L
        if (cloud.getString("dataJson").isNullOrBlank()) {
            publish(uid, local, cloudVersion)
            return
        }
        val merged = merge(local, parse(cloud.getString("dataJson")!!))
        publish(uid, merged, cloudVersion)
    }

    private suspend fun publish(uid: String, snapshot: Snapshot, expectedVersion: Long) {
        val json = build(snapshot)
        val version = withTimeout(TIMEOUT) {
            firestore.runTransaction { tx ->
                val ref = doc(uid)
                val current = tx.get(ref)
                val currentVersion = current.getLong("updatedAt") ?: 0L
                if (currentVersion != expectedVersion) {
                    error("Cloud berubah saat sinkronisasi. Coba refresh lagi.")
                }
                val newVersion = maxOf(System.currentTimeMillis(), currentVersion + 1L)
                tx.set(ref, mapOf("uid" to uid, "dataJson" to json, "updatedAt" to newVersion))
                newVersion
            }.await()
        }
        replaceLocal(snapshot)
        localFile(uid).writeText(json, Charsets.UTF_8)
        userProfileRepository.saveCloudSyncState(version, hash(json))
        refreshScheduler()
    }

    private suspend fun readLocal() = Snapshot(
        database.courseDao().getAllCoursesSnapshot(),
        database.taskDao().getAllTasksSnapshot(),
        database.scheduleDao().getAllSchedulesSnapshot(),
        database.syncTombstoneDao().getAll()
    )

    private fun merge(local: Snapshot, remote: Snapshot): Snapshot {
        val courseMap = mergeEntities(local.courses.associateBy { it.id }, remote.courses.associateBy { it.id }) { it.updatedAt }
        val taskMap = mergeEntities(local.tasks.associateBy { it.id }, remote.tasks.associateBy { it.id }) { it.updatedAt }
        val scheduleMap = mergeEntities(local.schedules.associateBy { it.id }, remote.schedules.associateBy { it.id }) { it.updatedAt }
        val tombstones = (local.tombstones + remote.tombstones)
            .groupBy { it.entityType to it.entityId }
            .values.mapNotNull { it.maxByOrNull(SyncTombstoneEntity::deletedAt) }
        val deleted = tombstones.map { it.entityType to it.entityId }.toSet()
        return Snapshot(
            courseMap.values.filterNot { (COURSE to it.id) in deleted },
            taskMap.values.filterNot { (TASK to it.id) in deleted },
            scheduleMap.values.filterNot { (SCHEDULE to it.id) in deleted },
            tombstones
        )
    }

    private fun <T> mergeEntities(local: Map<Long, T>, remote: Map<Long, T>, ts: (T) -> Long): Map<Long, T> {
        val result = linkedMapOf<Long, T>()
        for (id in local.keys + remote.keys) {
            val l = local[id]
            val r = remote[id]
            result[id] = when {
                l == null -> r!!
                r == null -> l
                ts(l) > ts(r) -> l
                else -> r
            }
        }
        return result
    }

    private suspend fun replaceLocal(snapshot: Snapshot) {
        database.withTransaction {
            database.courseDao().deleteAll()
            database.taskDao().deleteAll()
            database.scheduleDao().deleteAll()
            database.syncTombstoneDao().deleteAll()
            if (snapshot.courses.isNotEmpty()) database.courseDao().insertAll(snapshot.courses)
            if (snapshot.tasks.isNotEmpty()) database.taskDao().insertAll(snapshot.tasks)
            if (snapshot.schedules.isNotEmpty()) database.scheduleDao().insertAll(snapshot.schedules)
            if (snapshot.tombstones.isNotEmpty()) database.syncTombstoneDao().upsertAll(snapshot.tombstones)
        }
    }

    private fun build(s: Snapshot): String = JSONObject().apply {
        put("app", "MyTask")
        put("version", 4)
        put("createdAt", System.currentTimeMillis())
        put("courses", JSONArray().apply { s.courses.forEach { put(JSONObject().apply { put("id", it.id); put("name", it.name); put("code", it.code); put("lecturer", it.lecturer); put("room", it.room); put("updatedAt", it.updatedAt) }) } })
        put("tasks", JSONArray().apply { s.tasks.forEach { put(JSONObject().apply { put("id", it.id); put("courseId", it.courseId ?: JSONObject.NULL); put("title", it.title); put("description", it.description); put("deadline", it.deadline?.time ?: JSONObject.NULL); put("priority", it.priority); put("isCompleted", it.isCompleted); put("completedAt", it.completedAt?.time ?: JSONObject.NULL); put("updatedAt", it.updatedAt) }) } })
        put("schedules", JSONArray().apply { s.schedules.forEach { schedule -> put(JSONObject().apply { val ranges=schedule.getTimeRanges(); put("id",schedule.id); put("courseId",schedule.courseId ?: JSONObject.NULL); put("dayOfWeek",schedule.dayOfWeek); put("startMinutes",schedule.startMinutes); put("endMinutes",schedule.endMinutes); put("startTime",schedule.startMinutes.toDisplayTime()); put("endTime",schedule.endMinutes.toDisplayTime()); put("timeRanges",JSONArray().apply{ranges.forEach{put(it.toJson())}}); put("room",schedule.room); put("updatedAt",schedule.updatedAt) }) } })
        put("deleted", JSONArray().apply { s.tombstones.forEach { put(JSONObject().apply { put("entityType",it.entityType); put("entityId",it.entityId); put("deletedAt",it.deletedAt) }) } })
    }.toString()

    private fun parse(json: String): Snapshot {
        val root=JSONObject(json)
        return Snapshot(
            parseCourses(root.optJSONArray("courses")),
            parseTasks(root.optJSONArray("tasks")),
            parseSchedules(root.optJSONArray("schedules")),
            buildList {
                val a=root.optJSONArray("deleted") ?: return@buildList
                for(i in 0 until a.length()){ val x=a.getJSONObject(i); add(SyncTombstoneEntity(x.optString("entityType"),x.optLong("entityId"),x.optLong("deletedAt",0L))) }
            }
        )
    }

    private fun parseCourses(a: JSONArray?): List<CourseEntity> = if(a==null) emptyList() else buildList { for(i in 0 until a.length()){ val x=a.getJSONObject(i); add(CourseEntity(x.optLong("id"),x.optString("name"),x.optString("code"),x.optString("lecturer"),x.optString("room"),x.optLong("updatedAt").takeIf{it>0}?:System.currentTimeMillis())) } }

    private fun parseTasks(a: JSONArray?): List<TaskEntity> = if(a==null) emptyList() else buildList { for(i in 0 until a.length()){ val x=a.getJSONObject(i); add(TaskEntity(x.optLong("id"),if(x.isNull("courseId"))null else x.optLong("courseId"),x.optString("title"),x.optString("description"),if(x.isNull("deadline"))null else Date(x.optLong("deadline")),x.optInt("priority",1),x.optBoolean("isCompleted"),if(x.isNull("completedAt"))null else Date(x.optLong("completedAt")),x.optLong("updatedAt").takeIf{it>0}?:System.currentTimeMillis())) } }

    private fun parseSchedules(a: JSONArray?): List<ScheduleEntity> = if(a==null) emptyList() else buildList { for(i in 0 until a.length()){ val x=a.getJSONObject(i); val legacyStart=x.optString("startTime",""); val legacyEnd=x.optString("endTime",""); val start=if(x.has("startMinutes"))x.optInt("startMinutes") else legacyStart.toMinuteOfDayOrNull()?:-1; val end=if(x.has("endMinutes"))x.optInt("endMinutes") else legacyEnd.toMinuteOfDayOrNull()?:-1; val ranges=when{ x.has("timeRanges") -> when(val v=x.get("timeRanges")){is JSONArray->buildList{for(j in 0 until v.length()){ScheduleTimeRange.fromJson(v.getJSONObject(j))?.let(::add)}}; is String->v.toScheduleTimeRangesOrNull()?:emptyList(); else->emptyList()}; else->emptyList()}; val sorted=if(ranges.isNotEmpty())ranges.sortedBy{it.startMinutes}else if(start in 0..1439 && end in 0..1439 && end>start)listOf(ScheduleTimeRange(start,end))else emptyList(); if(sorted.isEmpty())continue; val first=sorted.first(); add(ScheduleEntity(x.optLong("id"),if(x.isNull("courseId"))null else x.optLong("courseId"),x.optInt("dayOfWeek",1),first.startMinutes,first.endMinutes,x.optString("room"),sorted.toJsonString(),x.optLong("updatedAt").takeIf{it>0}?:System.currentTimeMillis())) } }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(JSONObject(value).apply{remove("createdAt")}.toString().toByteArray()).joinToString(""){ "%02x".format(it) }
    private fun localFile(uid:String)=File(context.filesDir,"mytask_data_${uid.replace(Regex("[^A-Za-z0-9._-]"),"_")}.json")
    private fun refreshScheduler(){ NotificationHelper.cancelAllAppNotifications(context); ReminderScheduler.cancel(context); ReminderScheduler.initialize(context) }

    enum class Mode { MERGE, REPLACE_CLOUD }
}
