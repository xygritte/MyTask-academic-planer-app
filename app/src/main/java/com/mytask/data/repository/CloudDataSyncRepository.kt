package com.mytask.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.google.firebase.firestore.FirebaseFirestore
import com.mytask.Notification.NotificationHelper
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
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
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudDataSyncRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MyTaskDatabase
) {

    private val firestore = FirebaseFirestore.getInstance()

    private companion object {
        const val CLOUD_TIMEOUT_MS = 15_000L
    }

    private fun document(uid: String) =
        firestore
            .collection("users")
            .document(uid)
            .collection("backups")
            .document("academic")

    val databaseJson: Flow<String> =
        combine(
            database.courseDao().getAllCourses(),
            database.taskDao().getAllTasks(),
            database.scheduleDao().getAllSchedules()
        ) { courses, tasks, schedules ->
            AuthDebugLog.d(
                "DB_STATE courses=${courses.size} tasks=${tasks.size} schedules=${schedules.size}"
            )
            buildJson(courses, tasks, schedules)
        }

    suspend fun syncOnLogin(uid: String): Boolean {
        require(uid.isNotBlank())

        AuthDebugLog.d(
            "CLOUD_RESTORE start: uid=${AuthDebugLog.uid(uid)} online=${isNetworkAvailable()}"
        )

        if (!isNetworkAvailable()) {
            AuthDebugLog.d(
                "CLOUD_RESTORE skipped: device offline, keeping local Room data"
            )
            ReminderScheduler.initialize(context)
            return false
        }

        return try {
            // IMPORTANT: do not clear Room before the cloud request succeeds.
            // A failed restore must never erase a valid offline workspace.
            val snapshot = withTimeout(CLOUD_TIMEOUT_MS) {
                document(uid)
                    .get()
                    .await()
            }

            val cloudJson = snapshot
                .getString("dataJson")
                ?.takeIf { it.isNotBlank() }

            if (cloudJson == null) {
                // A freshly created account has no cloud backup. In that case
                // start with a clean local workspace. Logout already clears the
                // normal account-switch path, but this also protects against a
                // stale local workspace after an interrupted login.
                clearLocalAcademicData()
                NotificationHelper.cancelAllAppNotifications(context)
                ReminderScheduler.cancel(context)
                ReminderScheduler.initialize(context)
                AuthDebugLog.d(
                    "CLOUD_RESTORE no cloud data; local workspace cleared: uid=${AuthDebugLog.uid(uid)}"
                )
                false
            } else {
                replaceLocalDatabase(cloudJson)
                saveLocalJson(uid, cloudJson)
                NotificationHelper.cancelAllAppNotifications(context)
                ReminderScheduler.cancel(context)
                ReminderScheduler.initialize(context)
                AuthDebugLog.d(
                    "CLOUD_RESTORE success: uid=${AuthDebugLog.uid(uid)} jsonLength=${cloudJson.length}"
                )
                true
            }
        } catch (error: Throwable) {
            val message = when (error) {
                is TimeoutCancellationException ->
                    "Cloud restore timed out after ${CLOUD_TIMEOUT_MS / 1000}s."
                else -> error.message ?: "Cloud restore failed."
            }

            AuthDebugLog.e(
                "CLOUD_RESTORE failed: uid=${AuthDebugLog.uid(uid)} ${error::class.simpleName}: $message",
                error
            )
            ReminderScheduler.initialize(context)
            throw error
        }
    }

    suspend fun uploadCurrentData(uid: String) {
        require(uid.isNotBlank())

        if (!isNetworkAvailable()) {
            AuthDebugLog.d(
                "CLOUD_UPLOAD rejected: device offline uid=${AuthDebugLog.uid(uid)}"
            )
            throw IllegalStateException(
                "Tidak ada koneksi internet. Hubungkan internet lalu coba lagi."
            )
        }

        AuthDebugLog.d(
            "CLOUD_UPLOAD start: uid=${AuthDebugLog.uid(uid)} online=true"
        )

        val json = databaseJson.first()
        AuthDebugLog.d(
            "CLOUD_UPLOAD payload ready: uid=${AuthDebugLog.uid(uid)} jsonLength=${json.length}"
        )

        uploadJson(uid, json)
    }

    suspend fun uploadJson(uid: String, json: String) {
        require(uid.isNotBlank())

        if (!isNetworkAvailable()) {
            AuthDebugLog.d(
                "CLOUD_UPLOAD rejected before Firestore: device offline uid=${AuthDebugLog.uid(uid)}"
            )
            throw IllegalStateException(
                "Tidak ada koneksi internet. Hubungkan internet lalu coba lagi."
            )
        }

        try {
            withTimeout(CLOUD_TIMEOUT_MS) {
                document(uid)
                    .set(
                        mapOf(
                            "uid" to uid,
                            "dataJson" to json,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    )
                    .await()
            }

            saveLocalJson(uid, json)
            AuthDebugLog.d(
                "CLOUD_UPLOAD success: uid=${AuthDebugLog.uid(uid)} jsonLength=${json.length}"
            )
        } catch (error: Throwable) {
            val message = when (error) {
                is TimeoutCancellationException ->
                    "Cloud upload timed out after ${CLOUD_TIMEOUT_MS / 1000}s."
                else -> error.message ?: "Cloud upload failed."
            }

            AuthDebugLog.e(
                "CLOUD_UPLOAD failed: uid=${AuthDebugLog.uid(uid)} ${error::class.simpleName}: $message",
                error
            )

            if (error is TimeoutCancellationException) {
                throw IllegalStateException(message, error)
            }

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
        AuthDebugLog.d("ROOM clearLocalAcademicData completed")
    }

    suspend fun clearLocalSessionData() {
        clearLocalAcademicData()
        NotificationHelper.cancelAllAppNotifications(context)
        ReminderScheduler.cancel(context)

        context.filesDir
            .listFiles()
            ?.filter {
                it.name.startsWith("mytask_data_") &&
                    it.name.endsWith(".json")
            }
            ?.forEach { file ->
                runCatching { file.delete() }
            }

        AuthDebugLog.d("LOCAL_SESSION clear completed")
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

        AuthDebugLog.d(
            "ROOM restore completed: courses=${courses.size} tasks=${tasks.size} schedules=${schedules.size}"
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun buildJson(
        courses: List<CourseEntity>,
        tasks: List<TaskEntity>,
        schedules: List<ScheduleEntity>
    ): String {
        return JSONObject().apply {
            put("app", "MyTask")
            put("version", 1)
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
                    })
                }
            })

            put("schedules", JSONArray().apply {
                schedules.forEach { schedule ->
                    put(JSONObject().apply {
                        put("id", schedule.id)
                        put("courseId", schedule.courseId ?: JSONObject.NULL)
                        put("dayOfWeek", schedule.dayOfWeek)
                        put("startTime", schedule.startTime)
                        put("endTime", schedule.endTime)
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
                add(
                    CourseEntity(
                        id = item.optLong("id", 0L),
                        name = item.optString("name"),
                        code = item.optString("code"),
                        lecturer = item.optString("lecturer"),
                        room = item.optString("room")
                    )
                )
            }
        }
    }

    private fun parseTasks(array: JSONArray?): List<TaskEntity> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val courseId = if (item.isNull("courseId")) null else item.optLong("courseId")
                val deadline = if (item.isNull("deadline")) null else Date(item.optLong("deadline"))
                add(
                    TaskEntity(
                        id = item.optLong("id", 0L),
                        courseId = courseId,
                        title = item.optString("title"),
                        description = item.optString("description"),
                        deadline = deadline,
                        priority = item.optInt("priority", 1),
                        isCompleted = item.optBoolean("isCompleted", false)
                    )
                )
            }
        }
    }

    private fun parseSchedules(array: JSONArray?): List<ScheduleEntity> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                val courseId = if (item.isNull("courseId")) null else item.optLong("courseId")
                add(
                    ScheduleEntity(
                        id = item.optLong("id", 0L),
                        courseId = courseId,
                        dayOfWeek = item.optInt("dayOfWeek", 1),
                        startTime = item.optString("startTime"),
                        endTime = item.optString("endTime"),
                        room = item.optString("room")
                    )
                )
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
