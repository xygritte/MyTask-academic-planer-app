package com.mytask.data.local

/**
 * Represents a time of day as minutes elapsed since 00:00.
 * This keeps schedule times sortable and easy to validate without relying on
 * string comparison.
 */
@JvmInline
value class MinuteOfDay(val value: Int) {
    init {
        require(value in 0..(23 * 60 + 59))
    }
}

fun MinuteOfDay.toDisplayString(): String {
    val hour = value / 60
    val minute = value % 60
    return "%02d:%02d".format(hour, minute)
}

fun String.toMinuteOfDayOrNull(): Int? {
    val parts = trim().split(":")
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

fun Int.toDisplayTime(): String = MinuteOfDay(this).toDisplayString()
