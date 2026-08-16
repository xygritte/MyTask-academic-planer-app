package com.mytask.ui.schedule

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.repository.CourseRepository
import com.mytask.data.repository.ScheduleRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val courseRepository: CourseRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val schedules: StateFlow<List<ScheduleEntity>> =
        scheduleRepository
            .getAllSchedules()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courses: StateFlow<List<CourseEntity>> =
        courseRepository
            .getAllCourses()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getScheduleById(id: Long): StateFlow<ScheduleEntity?> =
        scheduleRepository
            .getScheduleById(id)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun addSchedule(
        courseId: Long?,
        dayOfWeek: Int,
        startMinutes: Int,
        endMinutes: Int,
        room: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val id = scheduleRepository.addSchedule(
                ScheduleEntity(
                    courseId = courseId,
                    dayOfWeek = dayOfWeek,
                    startMinutes = startMinutes,
                    endMinutes = endMinutes,
                    room = room
                )
            )
            scheduleRepository.getScheduleById(id).stateIn(this).value?.let {
                ReminderScheduler.scheduleScheduleReminder(context, it)
            }
            onSaved()
        }
    }

    fun updateSchedule(schedule: ScheduleEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            ReminderScheduler.cancelScheduleReminder(context, schedule.id)
            scheduleRepository.updateSchedule(schedule)
            ReminderScheduler.scheduleScheduleReminder(context, schedule)
            onSaved()
        }
    }

    fun deleteSchedule(schedule: ScheduleEntity) {
        viewModelScope.launch {
            ReminderScheduler.cancelScheduleReminder(context, schedule.id)
            scheduleRepository.deleteSchedule(schedule)
        }
    }
}
