package com.mytask.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long? = null,
    val dayOfWeek: Int,
    /** Minutes elapsed since 00:00. */
    val startMinutes: Int,
    /** Minutes elapsed since 00:00. */
    val endMinutes: Int,
    val room: String = ""
)
