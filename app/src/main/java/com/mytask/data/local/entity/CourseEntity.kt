package com.mytask.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "courses")
data class CourseEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val code: String = "",

    val lecturer: String = "",

    val room: String = "",

    /** Last local change timestamp used for entity-level cloud merge. */
    val updatedAt: Long = System.currentTimeMillis()
)
