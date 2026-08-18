package com.mytask.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mytask.Notification.NotificationHelper
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.ScheduleTimeRange
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.local.getTimeRanges
import com.mytask.data.local.toDisplayTime
import com.mytask.data.local.toJsonString
import com.mytask.data.local.toMinuteOfDayOrNull
import com.mytask.data.local.toScheduleTimeRangesOrNull
import com.mytask.debug.AuthDebugLog
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

class CloudSyncConflictException(
    message: String = "Data online berubah dari perangkat lain."
) : IllegalStateException(message)

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
    }

    private fun document(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("backups")
            .document("academic")

    val databaseJson: Flow<String> = combine(
        database.courseDao().getAllCourses(),
        database.taskDao().getAllTasks(),
        database.scheduleDao().getAllSchedules()
    ) { courses, tasks, schedules ->
        AuthDebugLog.d("DB_STATE courses=${courses.size} tasks=${tasks.size} schedules=${schedules.size}")
        buildJson(courses, tasks, schedules)
    }

    suspend fun syncOnLogin(uid: String): Boolean {
        require(uid.isNotBlank())
        if (!isNetworkAvailable()) {
            throw IllegalStateException("Login membutuhkan koneksi internet untuk memuat data akun.")
        }

        return try {
            val snapshot = withTimeout(CLOUD_TIMEOUT_MS) { document(uid).get().await() }
            applyCloudSnapshot(uid, snapshot.getString("dataJson"), snapshot.getLong("updatedAt") ?: 0L)
        } catch (error: Throwable) {
            val message = when (error) {
                is TimeoutCancellationException -> "Cloud restore timed out after ${CLOUD_TIMEOUT_MS / 1000}s."
                else -> error.message ?: "Cloud restore failed."
            }
            AuthDebugLog.e("CLOUD_RESTORE failed: uid=${AuthDebugLog.uid(uid)} ${error::class.simpleName}: $message", error)
            runCatching {
                clearLocalSessionData()
                userProfileRepository.clearProfile()
                FirebaseAuth.getInstance().signOut()
            }
            ReminderScheduler.initialize(context)
            throw error
        }
    }

    suspend fun resyncFromCloud(uid: String): Boolean {
        require(uid.isNotBlank())
        if (!isNetworkAvailable()) {
            throw IllegalStateException("Tidak ada koneksi internet. Hubungkan internet lalu coba lagi.")
        }

        return try {
            val snapshot = withTimeout(CLOUD_TIMEOUT_MS) { document(uid).get().await() }
            applyCloudSnapshot(uid, snapshot.getString("dataJson"), snapshot.getLong("updatedAt") ?: 0L)
        } catch (error: Throwable) {
            val message = when (error) {
                is TimeoutCancellationException -> "Sinkronisasi cloud timeout setelah ${CLOUD_TIMEOUT_MS / 1000}s."
                else -> error.message ?: "Sinkronisasi cloud gagal."
            }
            AuthDebugLog.e("CLOUD_RESYNC failed: uid=${AuthDebugLog.uid(uid)} ${error::class.simpleName}: $message", error)
            throw error
        }
    }

    /**
     * Bidirectional sync used by startup, pull-to-refresh, manual save, and logout.
     * updatedAt is the primary version signal. A content-hash fallback also detects
     * manual Firebase edits where the timestamp was not changed.
     */
    suspend fun uploadCurrentData(uid: String) {
        require(uid.isNotBlank())
        if (!isNetworkAvailable()) {
            throw IllegalStateException("Tidak ada koneksi internet. Hubungkan internet lalu coba lagi.")
        }

        val localJson = databaseJson.first()
        val localHash = syncDataHash(localJson)
        val lastSyncedUpdatedAt = userProfileRepository.lastCloudUpdatedAt.first()
        val lastSnapshotHash = readLastLocalSnapshotHash(uid)

        val snapshot = withTimeout(CLOUD_TIMEOUT_MS) { document(uid).get().await() }
        val cloudJson = snapshot.getString("dataJson")
        val cloudUpdatedAt = snapshot.getLong("updatedAt") ?: 0L
        val cloudExists = snapshot.exists() && !cloudJson.isNullOrBlank()
        val cloudHash = cloudJson?.let(::syncDataHash).orEmpty()

        if (!cloudExists) {
            AuthDebugLog.d("CLOUD_SYNC cloud document missing -> publish local workspace")
            uploadJson(uid, localJson)
            return
        }

        if (cloudUpdatedAt > lastSyncedUpdatedAt) {
            AuthDebugLog.d("CLOUD_SYNC cloud newer: cloud=$cloudUpdatedAt localLast=$lastSyncedUpdatedAt -> download")
            applyCloudSnapshot(uid, cloudJson, cloudUpdatedAt)
            return
        }

        if (cloudUpdatedAt == lastSyncedUpdatedAt) {
            if (cloudHash == localHash) {
                AuthDebugLog.d("CLOUD_SYNC unchanged: updatedAt=$cloudUpdatedAt")
                return
            }

            if (lastSnapshotHash != null && localHash == lastSnapshotHash) {
                AuthDebugLog.d("CLOUD_SYNC cloud content changed at same timestamp; local unchanged -> download")
                applyCloudSnapshot(uid, cloudJson, cloudUpdatedAt)
                return
            }

            saveConflictBackup(uid, localJson)
            throw CloudSyncConflictException(
                "Data lokal dan online sama-sama berubah. Sinkronisasi dihentikan untuk mencegah data tertimpa."
            )
        }

        // Cloud timestamp is older than the last synchronized version. If the local
        // workspace has not changed since that version, republish it as the newest cloud version.
        if (lastSnapshotHash != null && localHash != lastSnapshotHash) {
            saveConflictBackup(uid, localJson)
            throw CloudSyncConflictException(
                "Data online kembali ke versi lebih lama sementara data lokal juga berubah. Sinkronisasi dihentikan untuk keamanan data."
            )
        }

        AuthDebugLog.d("CLOUD_SYNC cloud older: cloud=$cloudUpdatedAt localLast=$lastSyncedUpdatedAt -> restore latest local")
        uploadLatestLocalSnapshot(uid, localJson, localHash, cloudUpdatedAt)
    }

    private suspend fun uploadLatestLocalSnapshot(
        uid: String,
        json: String,
        dataHash: String,
        expectedCloudUpdatedAt: Long
    ) {
        val newUpdatedAt = try {
            withTimeout(CLOUD_TIMEOUT_MS) {
                firestore.runTransaction { transaction ->
                    val reference = document(uid)
                    val snapshot = transaction.get(reference)
                    val currentUpdatedAt = snapshot.getLong("updatedAt") ?: 0L

                    if (currentUpdatedAt != expectedCloudUpdatedAt) {
                        throw CloudSyncConflictException()
                    }

                    val version = maxOf(System.currentTimeMillis(), currentUpdatedAt + 1L)
                    transaction.set(
                        reference,
                        mapOf("uid" to uid, "dataJson" to json, "updatedAt" to version)
                    )
                    version
                }.await()
            }
        } catch (error: Throwable) {
            if (error is CloudSyncConflictException) {
                saveConflictBackup(uid, json)
                throw CloudSyncConflictException(
                    "Data online berubah selama sinkronisasi. Data lokal tetap dipertahankan dan perlu disinkronkan ulang."
                )
            }
            throw error
        }

        saveLocalJson(uid, json)
        userProfileRepository.saveCloudSyncState(newUpdatedAt, dataHash)
        AuthDebugLog.d("CLOUD_SYNC local published as latest: uid=${AuthDebugLog.uid(uid)} updatedAt=$newUpdatedAt")
    }

    suspend fun uploadJson(uid: String, json: String) {
        require(uid.isNotBlank())
        if (!isNetworkAvailable()) {
            throw IllegalStateException("Tidak ada koneksi internet. Hubungkan internet lalu coba lagi.")
        }

        val dataHash = syncDataHash(json)
        val lastSyncedUpdatedAt = userProfileRepository.lastCloudUpdatedAt.first()

        try {
            val newUpdatedAt = withTimeout(CLOUD_TIMEOUT_MS) {
                firestore.runTransaction { transaction ->
                    val reference = document(uid)
                    val snapshot = transaction.get(reference)
                    val currentUpdatedAt = snapshot.getLong("updatedAt") ?: 0L
                    val cloudExists = snapshot.exists() && snapshot.getString("dataJson")?.isNotBlank() == true

                    if (cloudExists && currentUpdatedAt != lastSyncedUpdatedAt) {
                        throw CloudSyncConflictException()
                    }

                    val version = maxOf(System.currentTimeMillis(), currentUpdatedAt + 1L)
                    transaction.set(
                        reference,
                        mapOf("uid" to uid, "dataJson" to json, "updatedAt" to version)
                    )
                    version
                }.await()
            }

            saveLocalJson(uid, json)
            userProfileRepository.saveCloudSyncState(newUpdatedAt, dataHash)
            AuthDebugLog.d("CLOUD_UPLOAD success: uid=${AuthDebugLog.uid(uid)} updatedAt=$newUpdatedAt")
        } catch (error: Throwable) {
            if (error is CloudSyncConflictException) {
                saveConflictBackup(uid, json)
                try {
                    resyncFromCloud(uid)
                } catch (resyncError: Throwable) {
                    AuthDebugLog.e("CLOUD_CONFLICT recovery failed: uid=${AuthDebugLog.uid(uid)}", resyncError)
                    throw CloudSyncConflictException(
                        "Data online berubah dan sinkronisasi otomatis gagal. Data lokal tetap dipertahankan."
                    )
                }

                throw CloudSyncConflictException(
                    "Data online berubah. Workspace sudah disinkronkan ke versi online terbaru; perubahan lokal sebelum konflik disimpan sebagai cadangan internal."
                )
            }

            val message = when (error) {
                is TimeoutCancellationException -> "Cloud upload timed out after ${CLOUD_TIMEOUT_MS / 1000}s."
                else -> error.message ?: "Cloud upload failed."
            }
            AuthDebugLog.e("CLOUD_UPLOAD failed: uid=${AuthDebugLog.uid(uid)} ${error::class.simpleName}: $message", error)
            if (error is TimeoutCancellationException) throw IllegalStateException(message, error)
            throw error
        }
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

    private suspend fun applyCloudSnapshot(uid: String, cloudJson: String?, cloudUpdatedAt: Long): Boolean {
        if (cloudJson == null) {
            clearLocalAcademicData()
            userProfileRepository.saveCloudSyncState(0L, "")
            NotificationHelper.cancelAllAppNotifications(context)
            ReminderScheduler.cancel(context)
            ReminderScheduler.initialize(context)
            return false
        }

        replaceLocalDatabase(cloudJson)
        saveLocalJson(uid, cloudJson)
        userProfileRepository.saveCloudSyncState(cloudUpdatedAt, syncDataHash(cloudJson))
        NotificationHelper.cancelAllAppNotifications(context)
        ReminderScheduler.cancel(context)
        ReminderScheduler.initialize(context)
        return true
    }

    private fun saveConflictBackup(uid: String, json: String) {
        val safeUid = uid.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File(context.filesDir, "mytask_conflict_${safeUid}_${System.currentTimeMillis()}.json")
        runCatching { file.writeText(json, Charsets.UTF_8) }
        context.filesDir.listFiles()
            ?.filter { it.name.startsWith("mytask_conflict_${safeUid}_") && it.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?.drop(MAX_CONFLICT_BACKUPS)
            ?.forEach { old -> runCatching { old.delete() } }
    }

    private suspend fun replaceLocalDatabase(json: String) {
        val root = JSONObject(json)
        val courses = parseCourses(root.optJSONArray("courses"))
        val tasks = parseTasks(root.optJSONArray("tasks"))
        val schedules = parseSchedules(root.optJSONArray("schedules"))

        database.withTransaction {
            database.scheduleDao().deleteAll()
            database.taskDao().deleteAll()
            database.courseDao().deleteAll()
            if (courses.isNotEmpty()) database.courseDao().insertAll(courses)
            if (tasks.isNotEmpty()) database.taskDao().insertAll(tasks)
            if (schedules.isNotEmpty()) database.scheduleDao().insertAll(schedules)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun syncDataHash(value: String): String {
        return try {
            val normalized = JSONObject(value).apply {
                remove("createdAt")
            }.toString()
            sha256(normalized)
        } catch (_: Throwable) {
            sha256(value)
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun readLastLocalSnapshotHash(uid: String): String? {
        val file = localFile(uid)
        if (!file.exists()) return null
        return runCatching { syncDataHash(file.readText(Charsets.UTF_8)) }.getOrNull()
    }

    private fun buildJson(courses: List<CourseEntity>, tasks: List<TaskEntity>, schedules: List<ScheduleEntity>): String {
        return JSONObject().apply {
            put("app", "MyTask")
            put("version", 2)
            put("createdAt", System.currentTimeMillis())
            put("courses", JSONArray().apply {
                courses.forEach { course ->
                    put(JSONObject().apply {
                        put("id", course.id)
                        put("name", course.name)
                        put("code", course.code)
                        put("lecturer", course.lecturer)
                        put("room", course.room)
                    })
                }
            })
            put("tasks", JSONArray().apply {
                tasks.forEach { task ->
                    put(JSONObject().apply {
                        put("id", task.id)
                        put("courseId", task.courseId ?: JSONObject.NULL)
                        put("title", task.title)
                        put("description", task.description)
                        put("deadline", task.deadline?.time ?: JSONObject.NULL)
                        put("priority", task.priority)
                        put("isCompleted", task.isCompleted)
                        put("completedAt", task.completedAt?.time ?: JSONObject.NULL)
                    })
                }
            })
            put("schedules", JSONArray().apply {
                schedules.forEach { schedule ->
                    val ranges = schedule.getTimeRanges()
                    put(JSONObject().apply {
                        put("id", schedule.id)
                        put("courseId", schedule.courseId ?: JSONObject.NULL)
                        put("dayOfWeek", schedule.dayOfWeek)
                        put("startMinutes", schedule.startMinutes)
                        put("endMinutes", schedule.endMinutes)
                        put("startTime", schedule.startMinutes.toDisplayTime())
                        put("endTime", schedule.endMinutes.toDisplayTime())
                        put("timeRanges", JSONArray().apply { ranges.forEach { put(it.toJson()) } })
                        put("room", schedule.room)
                    })
                }
            })
        }.toString()
    }

    private fun parseCourses(array: JSONArray?): List<CourseEntity> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(CourseEntity(
                    id = item.optLong("id", 0L),
                    name = item.optString("name"),
                    code = item.optString("code"),
                    lecturer = item.optString("lecturer"),
                    room = item.optString("room")
                ))
            }
        }
    }

    private fun parseTasks(array: JSONArray?): List<TaskEntity> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(TaskEntity(
                    id = item.optLong("id", 0L),
                    courseId = if (item.isNull("courseId")) null else item.optLong("courseId"),
                    title = item.optString("title"),
                    description = item.optString("description"),
                    deadline = if (item.isNull("deadline")) null else Date(item.optLong("deadline")),
                    priority = item.optInt("priority", 1),
                    isCompleted = item.optBoolean("isCompleted", false),
                    completedAt = if (item.isNull("completedAt")) null else Date(item.optLong("completedAt"))
                ))
            }
        }
    }

    private fun parseSchedules(array: JSONArray?): List<ScheduleEntity> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val courseId = if (item.isNull("courseId")) null else item.optLong("courseId")
                val legacyStart = item.optString("startTime", "")
                val legacyEnd = item.optString("endTime", "")
                val legacyStartMinutes = if (item.has("startMinutes")) item.optInt("startMinutes") else legacyStart.toMinuteOfDayOrNull() ?: -1
                val legacyEndMinutes = if (item.has("endMinutes")) item.optInt("endMinutes") else legacyEnd.toMinuteOfDayOrNull() ?: -1

                val timeRanges = when {
                    item.has("timeRanges") -> when (val value = item.get("timeRanges")) {
                        is JSONArray -> buildList {
                            for (rangeIndex in 0 until value.length()) {
                                ScheduleTimeRange.fromJson(value.getJSONObject(rangeIndex))?.let(::add)
                            }
                        }
                        is String -> value.toScheduleTimeRangesOrNull() ?: emptyList()
                        else -> emptyList()
                    }
                    else -> emptyList()
                }

                val sortedRanges = if (timeRanges.isNotEmpty()) {
                    val sorted = timeRanges.sortedBy { it.startMinutes }
                    if (sorted.zipWithNext().any { (current, next) -> next.startMinutes < current.endMinutes }) emptyList() else sorted
                } else if (legacyStartMinutes in 0..1439 && legacyEndMinutes in 0..1439 && legacyEndMinutes > legacyStartMinutes) {
                    listOf(ScheduleTimeRange(legacyStartMinutes, legacyEndMinutes))
                } else {
                    emptyList()
                }

                if (sortedRanges.isEmpty()) continue
                val firstRange = sortedRanges.first()
                add(ScheduleEntity(
                    id = item.optLong("id", 0L),
                    courseId = courseId,
                    dayOfWeek = item.optInt("dayOfWeek", 1),
                    startMinutes = firstRange.startMinutes,
                    endMinutes = firstRange.endMinutes,
                    room = item.optString("room"),
                    timeRangesJson = sortedRanges.toJsonString()
                ))
            }
        }
    }

    private fun localFile(uid: String): File {
        val safeUid = uid.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return File(context.filesDir, "mytask_data_$safeUid.json")
    }

    private fun saveLocalJson(uid: String, json: String) {
        localFile(uid).writeText(json, Charsets.UTF_8)
    }
}
