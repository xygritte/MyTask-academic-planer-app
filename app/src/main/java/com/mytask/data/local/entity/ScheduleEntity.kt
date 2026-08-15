package com.mytask.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val courseId: Long? = null,

    val dayOfWeek: Int,

    val startTime: String,

    val endTime: String,

    val room: String = ""
)