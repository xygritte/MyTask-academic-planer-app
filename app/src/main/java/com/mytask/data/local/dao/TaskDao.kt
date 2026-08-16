package com.mytask.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mytask.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY deadline ASC")
    suspend fun getAllTasksSnapshot(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Long): Flow<TaskEntity?>

    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getActiveTaskCount(): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE deadline BETWEEN :start AND :end
        AND isCompleted = 0
        """
    )
    fun getTodayDeadlineCount(start: Date, end: Date): Flow<Int>

    @Query(
        """
        SELECT * FROM tasks
        WHERE isCompleted = 0
        AND deadline BETWEEN :start AND :end
        ORDER BY deadline ASC
        """
    )
    fun getPendingTasksBetween(start: Date, end: Date): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()
}
