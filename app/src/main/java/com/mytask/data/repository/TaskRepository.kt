package com.mytask.data.repository

import com.mytask.data.local.dao.TaskDao
import com.mytask.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {

    fun getAllTasks(): Flow<List<TaskEntity>> = taskDao.getAllTasks()

    fun getTaskById(id: Long): Flow<TaskEntity?> = taskDao.getTaskById(id)

    fun getActiveTaskCount(): Flow<Int> = taskDao.getActiveTaskCount()

    fun getTodayDeadlineCount(): Flow<Int> {
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time

        return taskDao.getTodayDeadlineCount(start, end)
    }

    fun getUpcomingDeadlines(): Flow<List<TaskEntity>> {
        val now = Date()
        val end = Calendar.getInstance().apply {
            time = now
            add(Calendar.DAY_OF_YEAR, 7)
        }.time
        return taskDao.getPendingTasksBetween(now, end)
    }

    suspend fun addTask(task: TaskEntity): Long = taskDao.insert(task)

    suspend fun updateTask(task: TaskEntity) = taskDao.update(task)

    suspend fun deleteTask(task: TaskEntity) = taskDao.delete(task)
}
