package com.mytask.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mytask.data.local.dao.CourseDao
import com.mytask.data.local.dao.ScheduleDao
import com.mytask.data.local.dao.TaskDao
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        CourseEntity::class,
        ScheduleEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MyTaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun courseDao(): CourseDao
    abstract fun scheduleDao(): ScheduleDao
}
