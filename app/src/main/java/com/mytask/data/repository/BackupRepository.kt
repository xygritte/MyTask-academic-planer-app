package com.mytask.data.repository

import androidx.room.withTransaction
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.local.toDisplayTime
import com.mytask.data.local.toMinuteOfDayOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val database: MyTaskDatabase
) {

    suspend fun exportData(): String {
        val courses = database.courseDao().getAllCoursesSnapshot()
        val tasks = database.taskDao().getAllTasksSnapshot()
        val schedules = database.scheduleDao().getAllSchedulesSnapshot()

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
                    put(JSONObject().apply {
                        put("id", schedule.id)
                        put("courseId", schedule.courseId ?: JSONObject.NULL)
                        put("dayOfWeek", schedule.dayOfWeek)
                        put("startMinutes", schedule.startMinutes)
                        put("endMinutes", schedule.endMinutes)
                        put("startTime", schedule.startMinutes.toDisplayTime())
                        put("endTime", schedule.endMinutes.toDisplayTime())
                        put("room", schedule.room)
                    })
                }
            })
        }.toString(2)
    }

    suspend fun importData(json: String) {
        val root = JSONObject(json)
        val version = root.optInt("version", 0)

        require(root.optString("app") == "MyTask") {
            "File bukan backup MyTask."
        }
        require(version == 1 || version == 2) {
            "Versi backup tidak didukung."
        }

        val courses = mutableListOf<CourseEntity>()
        val courseArray = root.optJSONArray("courses") ?: JSONArray()
        for (index in 0 until courseArray.length()) {
            val item = courseArray.getJSONObject(index)
            courses += CourseEntity(
                id = item.optLong("id"),
                name = item.optString("name"),
                code = item.optString("code"),
                lecturer = item.optString("lecturer"),
                room = item.optString("room")
            )
        }

        val tasks = mutableListOf<TaskEntity>()
        val taskArray = root.optJSONArray("tasks") ?: JSONArray()
        for (index in 0 until taskArray.length()) {
            val item = taskArray.getJSONObject(index)
            val courseId = if (item.isNull("courseId")) null else item.optLong("courseId")
            val deadline = if (item.isNull("deadline")) null else Date(item.optLong("deadline"))
            val completedAt = if (item.isNull("completedAt")) null else Date(item.optLong("completedAt"))
            tasks += TaskEntity(
                id = item.optLong("id"),
                courseId = courseId,
                title = item.optString("title"),
                description = item.optString("description"),
                deadline = deadline,
                priority = item.optInt("priority", 1),
                isCompleted = item.optBoolean("isCompleted", false),
                completedAt = completedAt
            )
        }

        val schedules = mutableListOf<ScheduleEntity>()
        val scheduleArray = root.optJSONArray("schedules") ?: JSONArray()
        for (index in 0 until scheduleArray.length()) {
            val item = scheduleArray.getJSONObject(index)
            val courseId = if (item.isNull("courseId")) null else item.optLong("courseId")
            val startMinutes = item.optInt("startMinutes", -1).takeIf { it >= 0 }
                ?: item.optString("startTime").toMinuteOfDayOrNull()
                ?: 0
            val endMinutes = item.optInt("endMinutes", -1).takeIf { it >= 0 }
                ?: item.optString("endTime").toMinuteOfDayOrNull()
                ?: startMinutes

            schedules += ScheduleEntity(
                id = item.optLong("id"),
                courseId = courseId,
                dayOfWeek = item.optInt("dayOfWeek"),
                startMinutes = startMinutes.coerceIn(0, 1439),
                endMinutes = endMinutes.coerceIn(0, 1439),
                room = item.optString("room")
            )
        }

        database.withTransaction {
            database.scheduleDao().deleteAll()
            database.taskDao().deleteAll()
            database.courseDao().deleteAll()
            database.courseDao().insertAll(courses)
            database.taskDao().insertAll(tasks)
            database.scheduleDao().insertAll(schedules)
        }
    }
}
