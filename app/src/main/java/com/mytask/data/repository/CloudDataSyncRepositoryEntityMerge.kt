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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/** Offline-first cloud synchronization using entity-level last-write-wins. */
@Singleton
class CloudDataSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MyTaskDatabase,
    private val userProfileRepository: UserProfileRepository
) {
    private val firestore = FirebaseFirestore.getInstance()

    private companion object {
        const val CLOUD_TIMEOUT_MS = 15_000L
        const val MAX_CONFLICT_BACKUPS = 3
        const val COURSE = "course"
        const val TASK = "task"
        const val SCHEDULE = "schedule"
    }

    private data class Snapshot(
        val courses: List<CourseEntity>,
        val tasks: List<TaskEntity>,
        val schedules: List<ScheduleEntity>,
        val tombstones: List<SyncTombstoneEntity>
    )

    private fun document(uid: String) =
        firestore.collection("users").document(uid).collection("backups").document("academic")

    val databaseJson: Flow<String> = combine(
        database.courseDao().getAllCourses(),
        database.taskDao().getAllTasks(),
        database.scheduleDao().getAllSchedules()
    ) { courses, tasks, schedules -> buildDataJson(courses, tasks, schedules) }

    /** Login restore is cloud-authoritative; it never uploads an old local account into a new account. */
    suspend fun syncOnLogin(uid: String): Boolean {
        require(uid.isNotBlank())
        ensureNetwork()
        return try {
            val snapshot = withTimeout(CLOUD_TIMEOUT_MS) { document(uid).get().await() }
            val cloudJson = snapshot.getString("dataJson")
            val cloudUpdatedAt = snapshot.getLong("updatedAt") ?: 0L
            if (cloudJson.isNullOrBlank()) {
                clearLocalAcademicData()
                userProfileRepository.saveCloudSyncState(0L, "")
                refreshSchedulers()
                false
            } else {
                replaceLocalDatabase(parseSnapshot(cloudJson))
                saveLocalJson(uid, cloudJson)
                userProfileRepository.saveCloudSyncState(cloudUpdatedAt, syncDataHash(cloudJson))
                refreshSchedulers()
                true
            }
        } catch (error: Throwable) {
            runCatching {
                clearLocalSessionData()
                userProfileRepository.clearProfile()
                FirebaseAuth.getInstance().signOut()
            }
            throw error
        }
    }

    /** Explicit cloud restore used by recovery flows. */
    suspend fun resyncFromCloud(uid: String): Boolean {
        require(uid.isNotBlank())
        ensureNetwork()
        val snapshot = withTimeout(CLOUD_TIMEOUT_MS) { document(uid).get().await() }
        val cloudJson = snapshot.getString("dataJson")
        val cloudUpdatedAt = snapshot.getLong("updatedAt") ?: 0L
        if (cloudJson.isNullOrBlank()) {
            clearLocalAcademicData()
            userProfileRepository.saveCloudSyncState(0L, "")
            refreshSchedulers()
            return false
        }
        replaceLocalDatabase(parseSnapshot(cloudJson))
        saveLocalJson(uid, cloudJson)
        userProfileRepository.saveCloudSyncState(cloudUpdatedAt, syncDataHash(cloudJson))
        refreshSchedulers()
        return true
    }

    /** Bidirectional entity-level LWW sync for startup, refresh, manual save, and logout. */
    suspend fun uploadCurrentData(uid: String) {
        require(uid.isNotBlank())
        ensureNetwork()

        repeat(2) { attempt ->
            val local = readLocalSnapshot()
            val remoteDocument = withTimeout(CLOUD_TIMEOUT_MS) { document(uid).get().await() }
            val cloudJson = remoteDocument.getString("dataJson")
            val cloudVersion = remoteDocument.getLong("updatedAt") ?: 0L

            if (cloudJson.isNullOrBlank()) {
                publishMergedSnapshot(uid, local, cloudVersion)
                return
            }

            val merged = mergeSnapshots(local, parseSnapshot(cloudJson))
            if (snapshotsEqual(merged, parseSnapshot(cloudJson))) {
                replaceLocalDatabase(merged)
                val mergedJson = buildSyncJson(merged)
                saveLocalJson(uid, mergedJson)
                userProfileRepository.saveCloudSyncState(cloudVersion, syncDataHash(mergedJson))
                refreshSchedulers()
                return
            }

            try {
                publishMergedSnapshot(uid, merged, cloudVersion)
                return
            } catch (error: CloudSyncConflictException) {
                if (attempt == 0) continue
                saveConflictBackup(uid, buildSyncJson(local))
                throw error
            }
        }
    }

    /** Compatibility API: merge imported JSON with cloud rather than overwriting unrelated entities. */
    suspend fun uploadJson(uid: String, json: String) {
        require(uid.isNotBlank())
        ensureNetwork()
        val incoming = parseSnapshot(json)
        val remoteDocument = withTimeout(CLOUD_TIMEOUT_MS) { document(uid).get().await() }
        val remoteJson = remoteDocument.getString("dataJson")
        val cloudVersion = remoteDocument.getLong("updatedAt") ?: 0L
        val merged = if (remoteJson.isNullOrBlank()) incoming else mergeSnapshots(incoming, parseSnapshot(remoteJson))
        publishMergedSnapshot(uid, merged, cloudVersion)
    }

    suspend fun exportLocalJson(uid: String): File {
        val json = databaseJson.first()
        saveLocalJson(uid, json)
        return localFile(uid)
    }

    suspend fun clearLocalAcademicData() {
        database.withTransaction {
            database.scheduleDao().deleteAll()
            database.taskDao().deleteAll()
            database.courseDao().deleteAll()
            database.syncTombstoneDao().deleteAll()
        }
    }

    suspend fun clearLocalSessionData() {
        clearLocalAcademicData()
        NotificationHelper.cancelAllAppNotifications(context)
        ReminderScheduler.cancel(context)
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("mytask_data_") && it.name.endsWith(".json") }
            ?.forEach { file -> runCatching { file.delete() } }
    }

    private suspend fun readLocalSnapshot() = Snapshot(
        database.courseDao().getAllCoursesSnapshot(),
        database.taskDao().getAllTasksSnapshot(),
        database.scheduleDao().getAllSchedulesSnapshot(),
        database.syncTombstoneDao().getAll()
    )

    private fun mergeSnapshots(local: Snapshot, cloud: Snapshot): Snapshot {
        val tombstones = (local.tombstones + cloud.tombstones)
            .groupBy { it.entityType to it.entityId }
            .mapValues { (_, values) -> values.maxByOrNull { it.deletedAt }!! }

        val courses = mergeEntityList(COURSE, local.courses, cloud.courses, local.tombstones, cloud.tombstones).filterIsInstance<CourseEntity>()
        val tasks = mergeEntityList(TASK, local.tasks, cloud.tasks, local.tombstones, cloud.tombstones).filterIsInstance<TaskEntity>()
        val schedules = mergeEntityList(SCHEDULE, local.schedules, cloud.schedules, local.tombstones, cloud.tombstones).filterIsInstance<ScheduleEntity>()

        val survivingKeys = buildSet {
            courses.forEach { add(COURSE to it.id) }
            tasks.forEach { add(TASK to it.id) }
            schedules.forEach { add(SCHEDULE to it.id) }
        }
        val finalTombstones = tombstones.values.filter { (it.entityType to it.entityId) !in survivingKeys }
        return Snapshot(courses, tasks, schedules, finalTombstones)
    }

    private fun <T : Any> mergeEntityList(
        entityType: String,
        local: List<T>,
        cloud: List<T>,
        localTombstones: List<SyncTombstoneEntity>,
        cloudTombstones: List<SyncTombstoneEntity>
    ): List<T> {
        val localMap = local.associateBy { entityId(it) }
        val cloudMap = cloud.associateBy { entityId(it) }
        val localDelete = localTombstones.filter { it.entityType == entityType }.associateBy { it.entityId }
        val cloudDelete = cloudTombstones.filter { it.entityType == entityType }.associateBy { it.entityId }

        return (localMap.keys + cloudMap.keys + localDelete.keys + cloudDelete.keys).mapNotNull { id ->
            val localItem = localMap[id]
            val cloudItem = cloudMap[id]
            val localDeletedAt = localDelete[id]?.deletedAt ?: 0L
            val cloudDeletedAt = cloudDelete[id]?.deletedAt ?: 0L
            val localTime = localItem?.let { updatedAt(it) } ?: 0L
            val cloudTime = cloudItem?.let { updatedAt(it) } ?: 0L
            val bestDelete = maxOf(localDeletedAt, cloudDeletedAt)
            val bestUpdate = maxOf(localTime, cloudTime)

            when {
                bestDelete > bestUpdate -> null
                localItem != null && cloudItem == null -> localItem
                cloudItem != null && localItem == null -> cloudItem
                localItem != null && cloudItem != null -> {
                    when {
                        localTime > cloudTime -> localItem
                        cloudTime > localTime -> cloudItem
                        else -> cloudItem
                    }
                }
                else -> null
            }
        }
    }

    private fun entityId(entity: Any): Long = when (entity) {
        is CourseEntity -> entity.id
        is TaskEntity -> entity.id
        is ScheduleEntity -> entity.id
        else -> error("Unsupported sync entity: ${entity::class.simpleName}")
    }

    private fun updatedAt(entity: Any): Long = when (entity) {
        is CourseEntity -> entity.updatedAt
        is TaskEntity -> entity.updatedAt
        is ScheduleEntity -> entity.updatedAt
        else -> 0L
    }

    private suspend fun publishMergedSnapshot(uid: String, snapshot: Snapshot, expectedCloudVersion: Long) {
        val json = buildSyncJson(snapshot)
        val newVersion = try {
            withTimeout(CLOUD_TIMEOUT_MS) {
                firestore.runTransaction { transaction ->
                    val reference = document(uid)
                    val current = transaction.get(reference)
                    val currentVersion = current.getLong("updatedAt") ?: 0L
                    if (currentVersion != expectedCloudVersion) {
                        throw CloudSyncConflictException("Data online berubah saat sinkronisasi. Mencoba ulang.")
                    }
                    val version = maxOf(System.currentTimeMillis(), currentVersion + 1L)
                    transaction.set(reference, mapOf("uid" to uid, "dataJson" to json, "updatedAt" to version))
                    version
                }.await()
            }
        } catch (error: Throwable) {
            if (error is CloudSyncConflictException) throw error
            if (error is TimeoutCancellationException) throw IllegalStateException("Cloud sync timeout setelah ${CLOUD_TIMEOUT_MS / 1000}s.", error)
            throw error
        }

        replaceLocalDatabase(snapshot)
        saveLocalJson(uid, json)
        userProfileRepository.saveCloudSyncState(newVersion, syncDataHash(json))
        refreshSchedulers()
    }

    private suspend fun replaceLocalDatabase(snapshot: Snapshot) {
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

    private fun buildDataJson(courses: List<CourseEntity>, tasks: List<TaskEntity>, schedules: List<ScheduleEntity>): String = JSONObject().apply {
        put("app", "MyTask")
        put("version", 3)
        put("createdAt", System.currentTimeMillis())
        put("courses", JSONArray().apply { courses.forEach { put(courseJson(it)) } })
        put("tasks", JSONArray().apply { tasks.forEach { put(taskJson(it)) } })
        put("schedules", JSONArray().apply { schedules.forEach { put(scheduleJson(it)) } })
    }.toString()

    private fun buildSyncJson(snapshot: Snapshot): String = JSONObject().apply {
        put("app", "MyTask")
        put("version", 4)
        put("createdAt", System.currentTimeMillis())
        put("courses", JSONArray().apply { snapshot.courses.forEach { put(courseJson(it)) } })
        put("tasks", JSONArray().apply { snapshot.tasks.forEach { put(taskJson(it)) } })
        put("schedules", JSONArray().apply { snapshot.schedules.forEach { put(scheduleJson(it)) } })
        put("deleted", JSONArray().apply {
            snapshot.tombstones.forEach { put(JSONObject().apply { put("entityType", it.entityType); put("entityId", it.entityId); put("deletedAt", it.deletedAt) }) }
        })
    }.toString()

    private fun courseJson(course: CourseEntity) = JSONObject().apply {
        put("id", course.id); put("name", course.name); put("code", course.code); put("lecturer", course.lecturer); put("room", course.room); put("updatedAt", course.updatedAt)
    }

    private fun taskJson(task: TaskEntity) = JSONObject().apply {
        put("id", task.id); put("courseId", task.courseId ?: JSONObject.NULL); put("title", task.title); put("description", task.description); put("deadline", task.deadline?.time ?: JSONObject.NULL); put("priority", task.priority); put("isCompleted", task.isCompleted); put("completedAt", task.completedAt?.time ?: JSONObject.NULL); put("updatedAt", task.updatedAt)
    }

    private fun scheduleJson(schedule: ScheduleEntity) = JSONObject().apply {
        val ranges = schedule.getTimeRanges()
        put("id", schedule.id); put("courseId", schedule.courseId ?: JSONObject.NULL); put("dayOfWeek", schedule.dayOfWeek); put("startMinutes", schedule.startMinutes); put("endMinutes", schedule.endMinutes); put("startTime", schedule.startMinutes.toDisplayTime()); put("endTime", schedule.endMinutes.toDisplayTime()); put("timeRanges", JSONArray().apply { ranges.forEach { put(it.toJson()) } }); put("room", schedule.room); put("updatedAt", schedule.updatedAt)
    }

    private fun parseSnapshot(json: String): Snapshot {
        val root = JSONObject(json)
        return Snapshot(parseCourses(root.optJSONArray("courses")), parseTasks(root.optJSONArray("tasks")), parseSchedules(root.optJSONArray("schedules")), parseTombstones(root.optJSONArray("deleted")))
    }

    private fun parseTombstones(array: JSONArray?): List<SyncTombstoneEntity> = buildList {
        if (array == null) return@buildList
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(SyncTombstoneEntity(item.optString("entityType"), item.optLong("entityId"), item.optLong("deletedAt", 0L)))
        }
    }

    private fun parseCourses(array: JSONArray?): List<CourseEntity> = if (array == null) emptyList() else buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(CourseEntity(item.optLong("id"), item.optString("name"), item.optString("code"), item.optString("lecturer"), item.optString("room"), item.optLong("updatedAt", 0L).takeIf { it > 0 } ?: System.currentTimeMillis()))
        }
    }

    private fun parseTasks(array: JSONArray?): List<TaskEntity> = if (array == null) emptyList() else buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(TaskEntity(item.optLong("id"), if (item.isNull("courseId")) null else item.optLong("courseId"), item.optString("title"), item.optString("description"), if (item.isNull("deadline")) null else Date(item.optLong("deadline")), item.optInt("priority", 1), item.optBoolean("isCompleted", false), if (item.isNull("completedAt")) null else Date(item.optLong("completedAt")), item.optLong("updatedAt", 0L).takeIf { it > 0 } ?: System.currentTimeMillis()))
        }
    }

    private fun parseSchedules(array: JSONArray?): List<ScheduleEntity> = if (array == null) emptyList() else buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val courseId = if (item.isNull("courseId")) null else item.optLong("courseId")
            val legacyStart = item.optString("startTime", "")
            val legacyEnd = item.optString("endTime", "")
            val legacyStartMinutes = if (item.has("startMinutes")) item.optInt("startMinutes") else legacyStart.toMinuteOfDayOrNull() ?: -1
            val legacyEndMinutes = if (item.has("endMinutes")) item.optInt("endMinutes") else legacyEnd.toMinuteOfDayOrNull() ?: -1
            val ranges = when {
                item.has("timeRanges") -> when (val value = item.get("timeRanges")) {
                    is JSONArray -> buildList { for (rangeIndex in 0 until value.length()) ScheduleTimeRange.fromJson(value.getJSONObject(rangeIndex))?.let(::add) }
                    is String -> value.toScheduleTimeRangesOrNull() ?: emptyList()
                    else -> emptyList()
                }
                else -> emptyList()
            }
            val sortedRanges = if (ranges.isNotEmpty()) ranges.sortedBy { it.startMinutes } else if (legacyStartMinutes in 0..1439 && legacyEndMinutes in 0..1439 && legacyEndMinutes > legacyStartMinutes) listOf(ScheduleTimeRange(legacyStartMinutes, legacyEndMinutes)) else emptyList()
            if (sortedRanges.isEmpty()) continue
            val first = sortedRanges.first()
            add(ScheduleEntity(item.optLong("id"), courseId, item.optInt("dayOfWeek", 1).coerceIn(0, 7), first.startMinutes, first.endMinutes, item.optString("room"), sortedRanges.toJsonString(), item.optLong("updatedAt", 0L).takeIf { it > 0 } ?: System.currentTimeMillis()))
        }
    }

    private fun snapshotsEqual(first: Snapshot, second: Snapshot): Boolean = syncDataHash(buildSyncJson(first)) == syncDataHash(buildSyncJson(second))

    private fun syncDataHash(json: String): String = try { sha256(JSONObject(json).apply { remove("createdAt") }.toString()) } catch (_: Throwable) { sha256(json) }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { byte -> "%02x".format(byte) }

    private fun saveLocalJson(uid: String, json: String) { localFile(uid).writeText(json, Charsets.UTF_8) }

    private fun localFile(uid: String): File {
        val safeUid = uid.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(context.filesDir, "mytask_data_$safeUid.json")
    }

    private fun saveConflictBackup(uid: String, json: String) {
        val safeUid = uid.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(context.filesDir, "mytask_conflict_${safeUid}_${System.currentTimeMillis()}.json")
        runCatching { file.writeText(json, Charsets.UTF_8) }
        context.filesDir.listFiles()?.filter { it.name.startsWith("mytask_conflict_${safeUid}_") && it.name.endsWith(".json") }?.sortedByDescending { it.lastModified() }?.drop(MAX_CONFLICT_BACKUPS)?.forEach { runCatching { it.delete() } }
    }

    private fun refreshSchedulers() {
        NotificationHelper.cancelAllAppNotifications(context)
        ReminderScheduler.cancel(context)
        ReminderScheduler.initialize(context)
    }

    private fun ensureNetwork() {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: error("Tidak ada koneksi internet. Hubungkan internet lalu coba lagi.")
        val capabilities = manager.getNetworkCapabilities(network)
        check(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
            "Tidak ada koneksi internet. Hubungkan internet lalu coba lagi."
        }
    }
}
