package com.mytask.data.local

import com.mytask.data.local.entity.ScheduleEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Representasi satu rentang waktu pada sebuah jadwal.
 *
 * Contoh:
 * 01:00 - 02:00
 * 03:00 - 04:00
 * 20:00 - 23:00
 */
data class ScheduleTimeRange(
    val startMinutes: Int,
    val endMinutes: Int
) {

    init {
        require(startMinutes in 0..1439) {
            "startMinutes must be between 0 and 1439"
        }

        require(endMinutes in 0..1439) {
            "endMinutes must be between 0 and 1439"
        }

        require(endMinutes > startMinutes) {
            "endMinutes must be greater than startMinutes"
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("startMinutes", startMinutes)
            put("endMinutes", endMinutes)
            put("startTime", startMinutes.toDisplayTime())
            put("endTime", endMinutes.toDisplayTime())
        }
    }

    companion object {
        fun fromJson(
            json: JSONObject
        ): ScheduleTimeRange? {
            val startMinutes = when {
                json.has("startMinutes") -> json.optInt("startMinutes", -1)
                json.has("startTime") -> json.optString("startTime")
                    .toMinuteOfDayOrNull() ?: -1
                else -> -1
            }

            val endMinutes = when {
                json.has("endMinutes") -> json.optInt("endMinutes", -1)
                json.has("endTime") -> json.optString("endTime")
                    .toMinuteOfDayOrNull() ?: -1
                else -> -1
            }

            if (startMinutes !in 0..1439) return null
            if (endMinutes !in 0..1439) return null
            if (endMinutes <= startMinutes) return null

            return ScheduleTimeRange(
                startMinutes = startMinutes,
                endMinutes = endMinutes
            )
        }
    }
}

/** Convert list of time ranges menjadi JSON array string. */
fun List<ScheduleTimeRange>.toJsonString(): String {
    return JSONArray().apply {
        forEach { range -> put(range.toJson()) }
    }.toString()
}

/** Parse JSON array menjadi list time range. */
fun String.toScheduleTimeRangesOrNull(): List<ScheduleTimeRange>? {
    if (isBlank()) return null

    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val range = ScheduleTimeRange.fromJson(array.getJSONObject(index))
                if (range != null) add(range)
            }
        }
    }.getOrNull()
}

/**
 * Mengambil semua rentang waktu dari ScheduleEntity.
 * Data baru memakai timeRangesJson; data lama fallback ke startMinutes/endMinutes.
 */
fun ScheduleEntity.getTimeRanges(): List<ScheduleTimeRange> {
    val savedRanges = timeRangesJson.toScheduleTimeRangesOrNull()

    if (!savedRanges.isNullOrEmpty()) {
        return savedRanges.sortedBy { it.startMinutes }
    }

    return listOf(
        ScheduleTimeRange(
            startMinutes = startMinutes,
            endMinutes = endMinutes
        )
    )
}

/** Validasi seluruh rentang waktu. */
fun List<ScheduleTimeRange>.validateTimeRanges(): String? {
    if (isEmpty()) return "Minimal satu rentang waktu harus ditambahkan."

    val sortedRanges = sortedBy { it.startMinutes }

    for (range in sortedRanges) {
        if (range.endMinutes <= range.startMinutes) {
            return "Waktu selesai harus setelah waktu mulai."
        }
    }

    for (index in 0 until sortedRanges.lastIndex) {
        val current = sortedRanges[index]
        val next = sortedRanges[index + 1]

        if (next.startMinutes < current.endMinutes) {
            return "Ada rentang waktu yang saling bertabrakan."
        }
    }

    return null
}
