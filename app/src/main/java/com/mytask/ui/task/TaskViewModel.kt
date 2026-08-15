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

    private val appContext =
        application.applicationContext

    val tasks: StateFlow<List<TaskEntity>> =
        repository
            .getAllTasks()
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

    fun getTaskById(
        id: Long
    ): StateFlow<TaskEntity?> =
        repository
            .getTaskById(id)
            .stateIn(
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

            repository.addTask(

                TaskEntity(
                    courseId = courseId,
                    title = title,
                    description = description,
                    priority = priority,
                    deadline = deadline
                )
            )

            // Update notifikasi tugas aktif
            ReminderScheduler.syncToday(
                appContext
            )

            onSaved()
        }
    }

    fun updateTask(
        task: TaskEntity,
        onSaved: () -> Unit
    ) {

        viewModelScope.launch {

            repository.updateTask(
                task
            )

            if (task.isCompleted) {

                NotificationHelper
                    .cancelTaskNotification(
                        appContext,
                        task.id.toString()
                    )
            }

            // Update ringkasan tugas aktif
            ReminderScheduler.syncToday(
                appContext
            )

            onSaved()
        }
    }

    fun deleteTask(
        task: TaskEntity
    ) {

        viewModelScope.launch {

            repository.deleteTask(
                task
            )

            NotificationHelper
                .cancelTaskNotification(
                    appContext,
                    task.id.toString()
                )

            ReminderScheduler.syncToday(
                appContext
            )
        }
    }

    fun toggleTask(
        task: TaskEntity
    ) {

        viewModelScope.launch {

            val newCompleted =
                !task.isCompleted

            val updatedTask =
                task.copy(
                    isCompleted =
                        newCompleted
                )

            repository.updateTask(
                updatedTask
            )

            if (newCompleted) {

                NotificationHelper
                    .cancelTaskNotification(
                        appContext,
                        task.id.toString()
                    )
            }

            /*
             * Perbarui:
             * - Tugas Aktif
             * - Tugas Hari Ini
             */
            ReminderScheduler.syncToday(
                appContext
            )
        }
    }
}