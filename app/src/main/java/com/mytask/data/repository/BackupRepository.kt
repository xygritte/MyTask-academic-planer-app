package com.mytask.data.repository

import androidx.room.withTransaction
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
            put("version", 3)
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
                    val firstRange = ranges.firstOrNull()
                    put(JSONObject().apply {
                        put("id", schedule.id)
                        put("courseId", schedule.courseId ?: JSONObject.NULL)
                        put("dayOfWeek", schedule.dayOfWeek)
                        put("startMinutes", firstRange?.startMinutes ?: schedule.startMinutes)
                        put("endMinutes", firstRange?.endMinutes ?: schedule.endMinutes)
                        put("startTime", (firstRange?.startMinutes ?: schedule.startMinutes).toDisplayTime())
                        put("endTime", (firstRange?.endMinutes ?: schedule.endMinutes).toDisplayTime())
                        put("timeRanges", JSONArray().apply {
                            ranges.forEach { put(it.toJson()) }
                        })
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
        require(version in 1..3) {
            "Versi backup tidak didukung."
        }

        val courses = mutableListOf<CourseEntity>()
        val courseArray = root.optJSONArray("courses") ?: JSONArray()
        for (index in 0 until courseArray.length()) {
            val item = courseArray.getJSONObject(index)
            require(item.optString("name").isNotBlank()) { "Backup memiliki mata kuliah tanpa nama." }
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
            tasks += TaskEntity(
                id = item.optLong("id"),
                courseId = if (item.isNull("courseId")) null else item.optLong("courseId"),
                title = item.optString("title"),
                description = item.optString("description"),
                deadline = if (item.isNull("deadline")) null else Date(item.optLong("deadline")),
                priority = item.optInt("priority", 1),
                isCompleted = item.optBoolean("isCompleted", false),
                completedAt = if (item.isNull("completedAt")) null else Date(item.optLong("completedAt"))
            )
        }

        val schedules = mutableListOf<ScheduleEntity>()
        val scheduleArray = root.optJSONArray("schedules") ?: JSONArray()
        for (index in 0 until scheduleArray.length()) {
            val item = scheduleArray.getJSONObject(index)
            val courseId = if (item.isNull("courseId")) null else item.optLong("courseId")

            val legacyStart = item.optString("startTime", "")
            val legacyEnd = item.optString("endTime", "")
            val legacyStartMinutes = if (item.has("startMinutes")) {
                item.optInt("startMinutes")
            } else {
                legacyStart.toMinuteOfDayOrNull() ?: -1
            }
            val legacyEndMinutes = if (item.has("endMinutes")) {
                item.optInt("endMinutes")
            } else {
                legacyEnd.toMinuteOfDayOrNull() ?: -1
            }

            val ranges = when {
                item.has("timeRanges") -> {
                    when (val value = item.get("timeRanges")) {
                        is JSONArray -> buildList {
                            for (rangeIndex in 0 until value.length()) {
                                ScheduleTimeRange.fromJson(value.getJSONObject(rangeIndex))?.let(::add)
                            }
                        }
                        is String -> value.toScheduleTimeRangesOrNull() ?: emptyList()
                        else -> emptyList()
                    }
                }
                else -> emptyList()
            }

            val sortedRanges = if (ranges.isNotEmpty()) {
                ranges.sortedBy { it.startMinutes }
            } else if (
                legacyStartMinutes in 0..1439 &&
                legacyEndMinutes in 0..1439 &&
                legacyEndMinutes > legacyStartMinutes
            ) {
                listOf(ScheduleTimeRange(legacyStartMinutes, legacyEndMinutes))
            } else {
                emptyList()
            }

            require(sortedRanges.isNotEmpty()) {
                "Backup memiliki jadwal dengan rentang waktu tidak valid pada index $index."
            }

            for (rangeIndex in 0 until sortedRanges.lastIndex) {
                require(sortedRanges[rangeIndex + 1].startMinutes >= sortedRanges[rangeIndex].endMinutes) {
                    "Backup memiliki rentang jadwal yang bertabrakan pada index $index."
                }
            }

            val firstRange = sortedRanges.first()
            schedules += ScheduleEntity(
                id = item.optLong("id"),
                courseId = courseId,
                dayOfWeek = item.optInt("dayOfWeek").coerceIn(0, 7),
                startMinutes = firstRange.startMinutes,
                endMinutes = firstRange.endMinutes,
                room = item.optString("room"),
                timeRangesJson = sortedRanges.toJsonString()
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
