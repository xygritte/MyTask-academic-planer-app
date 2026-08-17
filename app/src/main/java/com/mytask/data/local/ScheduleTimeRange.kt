package com.mytask.data.local

import org.json.JSONArray
import org.json.JSONObject

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
        fun fromJson(json: JSONObject): ScheduleTimeRange? {
            val start = when {
                json.has("startMinutes") ->
                    json.optInt("startMinutes", -1)

                json.has("startTime") ->
                    json.optString("startTime")
                        .toMinuteOfDayOrNull()
                        ?: -1

                else -> -1
            }

            val end = when {
                json.has("endMinutes") ->
                    json.optInt("endMinutes", -1)

                json.has("endTime") ->
                    json.optString("endTime")
                        .toMinuteOfDayOrNull()
                        ?: -1

                else -> -1
            }

            if (start !in 0..1439 || end !in 0..1439) {
                return null
            }

            if (end <= start) {
                return null
            }

            return ScheduleTimeRange(
                startMinutes = start,
                endMinutes = end
            )
        }
    }
}

fun List<ScheduleTimeRange>.toJsonString(): String {
    return JSONArray().apply {
        forEach { put(it.toJson()) }
    }.toString()
}

fun String.toScheduleTimeRangesOrNull(): List<ScheduleTimeRange>? {
    if (isBlank()) return null

    return runCatching {
        val array = JSONArray(this)
        buildList {
            for (index in 0 until array.length()) {
                val range = ScheduleTimeRange.fromJson(
                    array.getJSONObject(index)
                )

                if (range != null) {
                    add(range)
                }
            }
        }
    }.getOrNull()
}