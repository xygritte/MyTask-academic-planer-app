package com.mytask.data.repository

import androidx.room.withTransaction
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.dao.TaskDao
import com.mytask.data.local.entity.SyncTombstoneEntity
import com.mytask.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val database: MyTaskDatabase
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

    suspend fun addTask(task: TaskEntity): Long =
        taskDao.insert(task.copy(updatedAt = System.currentTimeMillis()))

    suspend fun updateTask(task: TaskEntity) =
        taskDao.update(task.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteTask(task: TaskEntity) {
        database.withTransaction {
            taskDao.delete(task)
            database.syncTombstoneDao().upsert(
                SyncTombstoneEntity("task", task.id, System.currentTimeMillis())
            )
        }
    }
}
