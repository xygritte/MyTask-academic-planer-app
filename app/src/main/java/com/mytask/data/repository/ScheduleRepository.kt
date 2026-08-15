package com.mytask.data.repository

import com.mytask.data.local.dao.ScheduleDao
import com.mytask.data.local.entity.ScheduleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepository @Inject constructor(
    private val scheduleDao: ScheduleDao
) {

    fun getAllSchedules(): Flow<List<ScheduleEntity>> {
        return scheduleDao.getAllSchedules()
    }

    fun getScheduleById(
        id: Long
    ): Flow<ScheduleEntity?> {
        return scheduleDao.getScheduleById(id)
    }

    fun getSchedulesByDay(
        dayOfWeek: Int
    ): Flow<List<ScheduleEntity>> {
        return scheduleDao.getSchedulesByDay(dayOfWeek)
    }

    suspend fun addSchedule(
        schedule: ScheduleEntity
    ) {
        scheduleDao.insert(schedule)
    }

    suspend fun updateSchedule(
        schedule: ScheduleEntity
    ) {
        scheduleDao.update(schedule)
    }

    suspend fun deleteSchedule(
        schedule: ScheduleEntity
    ) {
        scheduleDao.delete(schedule)
    }
}