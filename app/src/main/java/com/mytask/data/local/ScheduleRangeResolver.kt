package com.mytask.data.local

import com.mytask.data.local.entity.ScheduleEntity
import java.util.Calendar

/**
 * Resolves the range currently active or the next range for a schedule.
 * This is presentation/scheduling logic only; it never changes persisted data.
 */
object ScheduleRangeResolver {
    enum class State { ACTIVE, NEXT, FINISHED }

    data class ResolvedRange(
        val rangeIndex: Int,
        val range: ScheduleTimeRange,
        val state: State,
        val startAt: Long,
        val endAt: Long
    )

    fun resolve(schedule: ScheduleEntity, nowMillis: Long = System.currentTimeMillis()): ResolvedRange? {
        val ranges = schedule.getTimeRanges().sortedBy { it.startMinutes }
        if (ranges.isEmpty()) return null

        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val today = startOfDay(nowMillis)
        val todayMatches = schedule.dayOfWeek == EVERY_DAY ||
            schedule.dayOfWeek == now.get(Calendar.DAY_OF_WEEK)

        if (todayMatches) {
            ranges.forEachIndexed { index, range ->
                val start = atMinutes(today, range.startMinutes)
                val end = atMinutes(today, range.endMinutes)
                if (nowMillis in start until end) {
                    return ResolvedRange(index, range, State.ACTIVE, start, end)
                }
                if (nowMillis < start) {
                    return ResolvedRange(index, range, State.NEXT, start, end)
                }
            }
        }

        val next = ranges.mapIndexedNotNull { index, range ->
            nextOccurrenceStart(schedule, range.startMinutes, nowMillis)?.let { start ->
                val end = atMinutes(start, range.endMinutes)
                ResolvedRange(index, range, State.NEXT, start, end)
            }
        }.minByOrNull { it.startAt }

        if (next != null) return next

        if (todayMatches) {
            val lastIndex = ranges.lastIndex
            val lastRange = ranges[lastIndex]
            val start = atMinutes(today, lastRange.startMinutes)
            val end = atMinutes(today, lastRange.endMinutes)
            return ResolvedRange(lastIndex, lastRange, State.FINISHED, start, end)
        }

        return null
    }

    private fun nextOccurrenceStart(
        schedule: ScheduleEntity,
        startMinutes: Int,
        nowMillis: Long
    ): Long? {
        for (offset in 0..7) {
            val candidate = Calendar.getInstance().apply {
                timeInMillis = startOfDay(nowMillis)
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, startMinutes / 60)
                set(Calendar.MINUTE, startMinutes % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val dayMatches = schedule.dayOfWeek == EVERY_DAY ||
                schedule.dayOfWeek == candidate.get(Calendar.DAY_OF_WEEK)
            if (dayMatches && candidate.timeInMillis > nowMillis) {
                return candidate.timeInMillis
            }
        }
        return null
    }

    private fun startOfDay(timeMillis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun atMinutes(baseMillis: Long, minutes: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = baseMillis
            set(Calendar.HOUR_OF_DAY, minutes / 60)
            set(Calendar.MINUTE, minutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private const val EVERY_DAY = 0
}
