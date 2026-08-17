package com.mytask.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.ScheduleTimeRange
import com.mytask.data.local.toJsonString
import com.mytask.data.local.toMinuteOfDayOrNull
import com.mytask.data.local.toScheduleTimeRangesOrNull
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

data class TemplateApplyResult(
    val alreadyApplied: Boolean,
    val addedCourses: Int,
    val addedTasks: Int,
    val addedSchedules: Int
)

@Singleton
class TemplateApplyRepository @Inject constructor(
    private val context: Context,
    private val database: MyTaskDatabase,
    private val catalog: TemplateCatalog,
    private val preferences: TemplatePreferenceRepository
) {

    suspend fun apply(template: AppTemplate): TemplateApplyResult {
        if (preferences.isTemplateApplied(template.id, template.version)) {
            return TemplateApplyResult(true, 0, 0, 0)
        }

        val root = catalog.readJson(template)
        var result = TemplateApplyResult(false, 0, 0, 0)

        database.withTransaction {
            val existingCourses = database.courseDao().getAllCoursesSnapshot()
            val courseIdsByName = existingCourses.associate { it.name to it.id }.toMutableMap()
            val templateCourseNames = mutableMapOf<Long, String>()

            val courseArray = root.optJSONArray("courses") ?: JSONArray()
            var addedCourses = 0
            for (index in 0 until courseArray.length()) {
                val item = courseArray.getJSONObject(index)
                val templateId = item.optLong("id", index.toLong())
                val name = item.optString("name").trim()
                require(name.isNotBlank()) { "Template memiliki mata kuliah tanpa nama." }
                templateCourseNames[templateId] = name

                if (!courseIdsByName.containsKey(name)) {
                    val id = database.courseDao().insert(
                        CourseEntity(
                            name = name,
                            code = item.optString("code"),
                            lecturer = item.optString("lecturer"),
                            room = item.optString("room")
                        )
                    )
                    courseIdsByName[name] = id
                    addedCourses++
                }
            }

            val taskArray = root.optJSONArray("tasks") ?: JSONArray()
            val tasks = mutableListOf<TaskEntity>()
            for (index in 0 until taskArray.length()) {
                val item = taskArray.getJSONObject(index)
                val templateCourseId = item.optLong("courseId", Long.MIN_VALUE)
                val courseName = templateCourseNames[templateCourseId]
                val actualCourseId = courseName?.let { courseIdsByName[it] }
                val title = item.optString("title").trim()
                require(title.isNotBlank()) { "Template memiliki tugas tanpa judul." }
                tasks += TaskEntity(
                    courseId = actualCourseId,
                    title = title,
                    description = item.optString("description"),
                    deadline = buildDeadline(item.optInt("deadlineOffsetDays", 7)),
                    priority = item.optInt("priority", 1).coerceIn(1, 5),
                    isCompleted = item.optBoolean("isCompleted", false),
                    completedAt = null
                )
            }
            if (tasks.isNotEmpty()) database.taskDao().insertAll(tasks)

            val scheduleArray = root.optJSONArray("schedules") ?: JSONArray()
            val schedules = mutableListOf<ScheduleEntity>()
            for (index in 0 until scheduleArray.length()) {
                val item = scheduleArray.getJSONObject(index)
                val templateCourseId = item.optLong("courseId", Long.MIN_VALUE)
                val courseName = templateCourseNames[templateCourseId]
                val actualCourseId = courseName?.let { courseIdsByName[it] }

                val ranges = when {
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

                val legacyStart = item.optString("startTime", "")
                val legacyEnd = item.optString("endTime", "")
                val startMinutes = item.optInt("startMinutes", -1).takeIf { it >= 0 }
                    ?: legacyStart.toMinuteOfDayOrNull() ?: -1
                val endMinutes = item.optInt("endMinutes", -1).takeIf { it >= 0 }
                    ?: legacyEnd.toMinuteOfDayOrNull() ?: -1
                val sortedRanges = if (ranges.isNotEmpty()) {
                    ranges.sortedBy { it.startMinutes }
                } else if (startMinutes in 0..1439 && endMinutes in 0..1439 && endMinutes > startMinutes) {
                    listOf(ScheduleTimeRange(startMinutes, endMinutes))
                } else {
                    emptyList()
                }
                require(sortedRanges.isNotEmpty()) { "Template memiliki jadwal dengan waktu tidak valid." }
                for (rangeIndex in 0 until sortedRanges.lastIndex) {
                    require(sortedRanges[rangeIndex + 1].startMinutes >= sortedRanges[rangeIndex].endMinutes) {
                        "Template memiliki rentang jadwal yang bertabrakan."
                    }
                }

                val firstRange = sortedRanges.first()
                schedules += ScheduleEntity(
                    courseId = actualCourseId,
                    dayOfWeek = item.optInt("dayOfWeek", 0).coerceIn(0, 7),
                    startMinutes = firstRange.startMinutes,
                    endMinutes = firstRange.endMinutes,
                    room = item.optString("room"),
                    timeRangesJson = sortedRanges.toJsonString()
                )
            }
            if (schedules.isNotEmpty()) database.scheduleDao().insertAll(schedules)

            result = TemplateApplyResult(false, addedCourses, tasks.size, schedules.size)
        }

        preferences.markTemplateApplied(template.id, template.version)
        ReminderScheduler.initialize(context)
        return result
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
