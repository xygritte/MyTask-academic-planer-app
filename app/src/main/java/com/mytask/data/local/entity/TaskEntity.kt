package com.mytask.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tasks")
data class TaskEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val courseId: Long? = null,

    val title: String,

    val description: String = "",

    val deadline: Date? = null,

    val priority: Int = 1,

    val isCompleted: Boolean = false,

    /**
     * Timestamp of the most recent transition into the completed state.
     * Null means the task has never been completed (or predates this field).
     */
    val completedAt: Date? = null,

    /** Last local change timestamp used for entity-level cloud merge. */
    val updatedAt: Long = System.currentTimeMillis()
)
