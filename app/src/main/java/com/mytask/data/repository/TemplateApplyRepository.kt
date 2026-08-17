package com.mytask.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.ScheduleTimeRange
import com.mytask.data.local.toJsonString
import com.mytask.data.local.toMinuteOfDayOrNull
import com.mytask.data.local.toScheduleTimeRangesOrNull
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import javax.inject.Inject
import javax.inject.Singleton

data class TemplateApplyResult(
    val alreadyApplied: Boolean,
    val addedCourses: Int,
    val addedSchedules: Int
)

@Singleton
class TemplateApplyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MyTaskDatabase,
    private val catalog: TemplateCatalog
) {

    suspend fun apply(template: AppTemplate): TemplateApplyResult {
        FirebaseAuth.getInstance().currentUser?.uid ?: "guest"

        val root = catalog.readJson(template)
        var result = TemplateApplyResult(false, 0, 0)

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
                    database.courseDao().insert(
                        CourseEntity(
                            name = name,
                            code = item.optString("code"),
                            lecturer = item.optString("lecturer"),
                            room = item.optString("room")
                        )
                    )
                    addedCourses++
                }
            }

            courseIdsByName.clear()
            courseIdsByName.putAll(
                database.courseDao().getAllCoursesSnapshot().associate { it.name to it.id }
            )

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

            result = TemplateApplyResult(false, addedCourses, schedules.size)
        }

        ReminderScheduler.initialize(context)
        return result
    }
}
