package com.mytask.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val courseId: Long? = null,
    /**
     * 0 = Setiap Hari
     * 1 = Minggu
     * 2 = Senin
     * 3 = Selasa
     * 4 = Rabu
     * 5 = Kamis
     * 6 = Jumat
     * 7 = Sabtu
     */
    val dayOfWeek: Int,
    /** Compatibility field for old data; contains the first range on new data. */
    val startMinutes: Int,
    /** Compatibility field for old data; contains the first range on new data. */
    val endMinutes: Int,
    val room: String = "",
    /** JSON array containing all time ranges for this schedule. */
    val timeRangesJson: String = "",
    /** Last local change timestamp used for entity-level cloud merge. */
    val updatedAt: Long = System.currentTimeMillis()
)
