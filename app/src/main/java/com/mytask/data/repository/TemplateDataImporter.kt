package com.mytask.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.ScheduleTimeRange
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.local.toJsonString
import com.mytask.data.local.toMinuteOfDayOrNull
import com.mytask.data.local.toScheduleTimeRangesOrNull
import com.mytask.debug.AppDebugLog
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateDataImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MyTaskDatabase
) {

    suspend fun importTemplate() {
        AppDebugLog.d("TEMPLATE", "import start")

        database.withTransaction {
            val root = JSONObject(
                context.assets.open("template_academic.json")
                    .bufferedReader()
                    .use { it.readText() }
            )

            val courseArray = root.getJSONArray("courses")
            val taskArray = root.getJSONArray("tasks")
            val scheduleArray = root.getJSONArray("schedules")

            val existingCourses = database.courseDao().getAllCoursesSnapshot()
            val templateCourseNames = mutableMapOf<Long, String>()

            for (index in 0 until courseArray.length()) {
                val item = courseArray.getJSONObject(index)
                val templateId = item.getLong("id")
                val name = item.getString("name")
                templateCourseNames[templateId] = name

                val existing = existingCourses.firstOrNull { it.name == name }
                if (existing == null) {
                    database.courseDao().insert(
                        CourseEntity(
                            name = name,
                            code = item.optString("code"),
                            lecturer = item.optString("lecturer"),
                            room = item.optString("room")
                        )
                    )
                }
            }

            val allCourses = database.courseDao().getAllCoursesSnapshot()
            val courseIdByName = allCourses.associate { it.name to it.id }

            val tasks = mutableListOf<TaskEntity>()
            for (index in 0 until taskArray.length()) {
                val item = taskArray.getJSONObject(index)
                val courseTemplateId = item.optLong("courseId", -1L)
                val courseName = templateCourseNames[courseTemplateId]
                val actualCourseId = courseName?.let { courseIdByName[it] }

                tasks += TaskEntity(
                    courseId = actualCourseId,
                    title = item.getString("title"),
                    description = item.optString("description"),
                    deadline = buildDeadline(item.optInt("deadlineOffsetDays", 7)),
                    priority = item.optInt("priority", 1),
                    isCompleted = item.optBoolean("isCompleted", false)
                )
            }

            if (tasks.isNotEmpty()) database.taskDao().insertAll(tasks)

            val schedules = mutableListOf<ScheduleEntity>()
            for (index in 0 until scheduleArray.length()) {
                val item = scheduleArray.getJSONObject(index)
                val courseTemplateId = item.optLong("courseId", -1L)
                val courseName = templateCourseNames[courseTemplateId]
                val actualCourseId = courseName?.let { courseIdByName[it] }

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
                    "Template memiliki jadwal dengan rentang waktu tidak valid pada index $index."
                }

                for (rangeIndex in 0 until sortedRanges.lastIndex) {
                    require(sortedRanges[rangeIndex + 1].startMinutes >= sortedRanges[rangeIndex].endMinutes) {
                        "Template memiliki rentang jadwal yang bertabrakan pada index $index."
                    }
                }

                val firstRange = sortedRanges.first()
                schedules += ScheduleEntity(
                    courseId = actualCourseId,
                    dayOfWeek = item.getInt("dayOfWeek").coerceIn(0, 7),
                    startMinutes = firstRange.startMinutes,
                    endMinutes = firstRange.endMinutes,
                    room = item.optString("room"),
                    timeRangesJson = sortedRanges.toJsonString()
                )
            }

            if (schedules.isNotEmpty()) database.scheduleDao().insertAll(schedules)
            AppDebugLog.d("TEMPLATE", "import completed courses=${courseArray.length()} tasks=${taskArray.length()} schedules=${scheduleArray.length()}")
        }

        // Template import changes Room data after the normal ViewModel save path, so explicitly
        // rebuild deadline/schedule alarms for the newly imported records.
        ReminderScheduler.syncToday(context)
        ReminderScheduler.rescheduleAllStoredScheduleReminders(context)
    }

    private fun buildDeadline(offsetDays: Int): Date {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
            add(Calendar.DAY_OF_YEAR, offsetDays.coerceAtLeast(0))
        }
        return calendar.time
    }
}
