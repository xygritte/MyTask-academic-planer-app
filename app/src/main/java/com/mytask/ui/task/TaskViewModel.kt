package com.mytask.ui.task

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.Notification.NotificationHelper
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.repository.CourseRepository
import com.mytask.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class TaskViewModel @Inject constructor(
    application: Application,
    private val repository: TaskRepository,
    private val courseRepository: CourseRepository
) : AndroidViewModel(application) {

    private val appContext = application.applicationContext

    val tasks: StateFlow<List<TaskEntity>> = repository.getAllTasks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val courses: StateFlow<List<CourseEntity>> = courseRepository.getAllCourses().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun getTaskById(id: Long): StateFlow<TaskEntity?> = repository.getTaskById(id).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    fun addTask(
        courseId: Long?,
        title: String,
        description: String,
        priority: Int,
        deadline: Date?,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                courseId = courseId,
                title = title,
                description = description,
                priority = priority,
                deadline = deadline
            )

            repository.addTask(task)
            val savedTask = tasks.firstOrNull { it.title == task.title && it.deadline == task.deadline }
            savedTask?.let {
                ReminderScheduler.scheduleTaskDeadline(appContext, it.id, it.deadline)
            }
            ReminderScheduler.syncToday(appContext)
            onSaved()
        }
    }

    fun updateTask(task: TaskEntity, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.updateTask(task)

            if (task.isCompleted) {
                NotificationHelper.cancelTaskNotification(appContext, task.id.toString())
                ReminderScheduler.cancelTaskDeadline(appContext, task.id)
            } else {
                ReminderScheduler.scheduleTaskDeadline(appContext, task.id, task.deadline)
            }

            ReminderScheduler.syncToday(appContext)
            onSaved()
        }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.deleteTask(task)
            NotificationHelper.cancelTaskNotification(appContext, task.id.toString())
            ReminderScheduler.cancelTaskDeadline(appContext, task.id)
            ReminderScheduler.syncToday(appContext)
        }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updatedTask)

            if (updatedTask.isCompleted) {
                NotificationHelper.cancelTaskNotification(appContext, task.id.toString())
                ReminderScheduler.cancelTaskDeadline(appContext, task.id)
            } else {
                ReminderScheduler.scheduleTaskDeadline(appContext, task.id, updatedTask.deadline)
            }

            ReminderScheduler.syncToday(appContext)
        }
    }
}
