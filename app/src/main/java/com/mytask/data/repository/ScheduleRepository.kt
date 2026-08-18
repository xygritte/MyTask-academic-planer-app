package com.mytask.data.repository

import androidx.room.withTransaction
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.dao.ScheduleDao
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.SyncTombstoneEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao,
    private val database: MyTaskDatabase
) {

    fun getAllSchedules(): Flow<List<ScheduleEntity>> = scheduleDao.getAllSchedules()

    fun getScheduleById(id: Long): Flow<ScheduleEntity?> = scheduleDao.getScheduleById(id)

    fun getSchedulesByDay(dayOfWeek: Int): Flow<List<ScheduleEntity>> =
        scheduleDao.getSchedulesByDay(dayOfWeek)

    suspend fun addSchedule(schedule: ScheduleEntity): Long =
        scheduleDao.insert(schedule.copy(updatedAt = System.currentTimeMillis()))

    suspend fun updateSchedule(schedule: ScheduleEntity) =
        scheduleDao.update(schedule.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteSchedule(schedule: ScheduleEntity) {
        database.withTransaction {
            scheduleDao.delete(schedule)
            database.syncTombstoneDao().upsert(
                SyncTombstoneEntity("schedule", schedule.id, System.currentTimeMillis())
            )
        }
    }
}
