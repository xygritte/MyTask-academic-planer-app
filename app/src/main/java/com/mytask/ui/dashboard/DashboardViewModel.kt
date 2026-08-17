package com.mytask.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.repository.CourseRepository
import com.mytask.data.repository.ScheduleRepository
import com.mytask.data.repository.SettingsRepository
import com.mytask.data.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

@HiltViewModel
class DashboardViewModel @Inject constructor(
    courseRepository: CourseRepository,
    taskRepository: TaskRepository,
    scheduleRepository: ScheduleRepository,
    private val settingsRepository: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val courseCount: StateFlow<Int> =
        courseRepository.getCourseCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val activeTaskCount: StateFlow<Int> =
        taskRepository.getActiveTaskCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val courses: StateFlow<List<CourseEntity>> =
        courseRepository.getAllCourses().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> =
        taskRepository.getAllTasks().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val rawSchedules: StateFlow<List<ScheduleEntity>> =
        scheduleRepository.getAllSchedules().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    /**
     * Dashboard-only presentation state. The persisted schedule is never modified.
     * For schedules with multiple ranges, the UI receives the active range or the
     * nearest upcoming range so the dashboard always shows the most relevant time.
     */
    val schedules: StateFlow<List<ScheduleEntity>> =
        combine(rawSchedules, minuteTicker) { schedules, nowMillis ->
            val now = java.util.Calendar.getInstance().apply { timeInMillis = nowMillis }
            val nowMinutes = now.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                    now.get(java.util.Calendar.MINUTE)
            schedules.map { it.withDashboardTimeRange(nowMinutes) }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val disabledScheduleNotificationIds: StateFlow<Set<Long>> =
        settingsRepository.disabledScheduleNotificationIds()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun setScheduleNotificationEnabled(schedule: ScheduleEntity, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setScheduleNotificationEnabled(schedule.id, enabled)
            val sourceSchedule = rawSchedules.value.firstOrNull { it.id == schedule.id } ?: schedule
            if (enabled) {
                ReminderScheduler.scheduleScheduleReminder(context, sourceSchedule)
            } else {
                ReminderScheduler.cancelScheduleReminder(context, sourceSchedule.id)
            }
        }
    }
}

private val minuteTicker = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(30_000L)
    }
}

private data class DashboardTimeRange(
    val startMinutes: Int,
    val endMinutes: Int
)

private fun ScheduleEntity.withDashboardTimeRange(nowMinutes: Int): ScheduleEntity {
    val ranges = parseDashboardTimeRanges()
        .sortedBy { it.startMinutes }

    if (ranges.isEmpty()) return this

    val selected =
        ranges.firstOrNull { nowMinutes >= it.startMinutes && nowMinutes < it.endMinutes }
            ?: ranges.firstOrNull { it.startMinutes > nowMinutes }
            ?: ranges.last()

    return copy(
        startMinutes = selected.startMinutes,
        endMinutes = selected.endMinutes
    )
}

private fun ScheduleEntity.parseDashboardTimeRanges(): List<DashboardTimeRange> {
    if (timeRangesJson.isBlank()) {
        return listOf(
            DashboardTimeRange(startMinutes, endMinutes)
        )
    }

    return runCatching {
        val array = JSONArray(timeRangesJson)
        buildList {
            for (index in 0 until array.length()) {
                val range = array.optJSONObject(index) ?: continue
                val start = range.optInt("startMinutes", -1)
                val end = range.optInt("endMinutes", -1)
                if (start in 0..1439 && end in 1..1440 && start < end) {
                    add(DashboardTimeRange(start, end))
                }
            }
        }
    }.getOrDefault(emptyList())
        .ifEmpty {
            listOf(DashboardTimeRange(startMinutes, endMinutes))
        }
}
