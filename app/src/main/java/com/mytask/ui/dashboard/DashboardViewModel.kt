package com.mytask.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.repository.CourseRepository
import com.mytask.data.repository.ScheduleRepository
import com.mytask.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class DashboardViewModel @Inject constructor(
    courseRepository: CourseRepository,
    taskRepository: TaskRepository,
    scheduleRepository: ScheduleRepository
) : ViewModel() {

    val courseCount: StateFlow<Int> =
        courseRepository
            .getCourseCount()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )

    val activeTaskCount: StateFlow<Int> =
        taskRepository
            .getActiveTaskCount()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                0
            )

    val courses: StateFlow<List<CourseEntity>> =
        courseRepository
            .getAllCourses()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val tasks: StateFlow<List<TaskEntity>> =
        taskRepository
            .getAllTasks()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    val schedules: StateFlow<List<ScheduleEntity>> =
        scheduleRepository
            .getAllSchedules()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
}