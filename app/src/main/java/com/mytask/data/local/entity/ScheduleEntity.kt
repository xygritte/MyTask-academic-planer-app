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

    /**
     * Compatibility field.
     *
     * Untuk data lama dan backup lama.
     * Pada data baru, nilai ini berisi rentang pertama.
     */
    val startMinutes: Int,

    /**
     * Compatibility field.
     *
     * Untuk data lama dan backup lama.
     * Pada data baru, nilai ini berisi rentang pertama.
     */
    val endMinutes: Int,

    val room: String = "",

    /**
     * JSON array:
     *
     * [
     *   {
     *     "startMinutes": 60,
     *     "endMinutes": 120
     *   },
     *   {
     *     "startMinutes": 180,
     *     "endMinutes": 240
     *   }
     * ]
     *
     * Blank = gunakan startMinutes/endMinutes lama.
     */
    val timeRangesJson: String = ""
)