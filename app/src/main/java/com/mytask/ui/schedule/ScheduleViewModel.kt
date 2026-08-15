package com.mytask.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.repository.CourseRepository
import com.mytask.data.repository.ScheduleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
    private val courseRepository: CourseRepository
) : ViewModel() {

    val schedules: StateFlow<List<ScheduleEntity>> =
        scheduleRepository
            .getAllSchedules()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val courses: StateFlow<List<CourseEntity>> =
        courseRepository
            .getAllCourses()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun getScheduleById(
        id: Long
    ): StateFlow<ScheduleEntity?> =
        scheduleRepository
            .getScheduleById(id)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    fun addSchedule(
        courseId: Long?,
        dayOfWeek: Int,
        startTime: String,
        endTime: String,
        room: String,
        onSaved: () -> Unit
    ) {

        viewModelScope.launch {

            scheduleRepository.addSchedule(
                ScheduleEntity(
                    courseId = courseId,
                    dayOfWeek = dayOfWeek,
                    startTime = startTime,
                    endTime = endTime,
                    room = room
                )
            )

            onSaved()
        }
    }

    fun updateSchedule(
        schedule: ScheduleEntity,
        onSaved: () -> Unit
    ) {

        viewModelScope.launch {

            scheduleRepository.updateSchedule(
                schedule
            )

            onSaved()
        }
    }

    fun deleteSchedule(
        schedule: ScheduleEntity
    ) {

        viewModelScope.launch {

            scheduleRepository.deleteSchedule(
                schedule
            )
        }
    }
}