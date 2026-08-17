package com.mytask.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.ScheduleRangeResolver
import com.mytask.data.local.getTimeRanges
import com.mytask.data.local.toDisplayTime
import com.mytask.data.repository.SettingsRepository
import com.mytask.debug.AppDebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScheduleNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SCHEDULE_ALARM = "com.mytask.action.SCHEDULE_ALARM"
        const val ACTION_SCHEDULE_COUNTDOWN = "com.mytask.action.SCHEDULE_COUNTDOWN"
        const val ACTION_SCHEDULE_START = "com.mytask.action.SCHEDULE_START"
        const val ACTION_SCHEDULE_END = "com.mytask.action.SCHEDULE_END"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
        const val EXTRA_RANGE_INDEX = "range_index"
        const val EXTRA_START_AT = "start_at"
        const val EXTRA_END_AT = "end_at"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                if (scheduleId <= 0L) return@launch

                val database = MyTaskDatabase.builder(context.applicationContext).build()
                try {
                    val schedule = database.scheduleDao().getScheduleById(scheduleId).first()
                    if (schedule == null) {
                        AppDebugLog.d("NOTIFICATION", "schedule alarm ignored: schedule missing id=$scheduleId")
                        return@launch
                    }

                    val settingsRepository = SettingsRepository(context.applicationContext)
                    val notificationsEnabled = settingsRepository
                        .scheduleNotificationEnabled(scheduleId)
                        .first()

                    when (intent.action) {
                        ACTION_SCHEDULE_START -> {
                            ReminderScheduler.scheduleNextScheduleReminder(
                                context.applicationContext,
                                schedule
                            )

                            if (!notificationsEnabled) {
                                NotificationHelper.cancelScheduleNotification(
                                    context.applicationContext,
                                    scheduleId.toString()
                                )
                                ReminderScheduler.cancelScheduleCountdown(
                                    context.applicationContext,
                                    scheduleId
                                )
                            } else {
                                val rangeIndex = intent.getIntExtra(EXTRA_RANGE_INDEX, -1)
                                val ranges = schedule.getTimeRanges().sortedBy { it.startMinutes }
                                val range = ranges.getOrNull(rangeIndex)
                                val resolved = ScheduleRangeResolver.resolve(schedule)

                                if (range != null && resolved?.state == ScheduleRangeResolver.State.ACTIVE) {
                                    NotificationHelper.showScheduleNotification(
                                        context.applicationContext,
                                        scheduleId.toString(),
                                        "🔵 Sedang berlangsung",
                                        buildActiveMessage(schedule.room, range),
                                        countdownUntilMillis = resolved.endAt
                                    )
                                    ReminderScheduler.scheduleScheduleEnd(
                                        context.applicationContext,
                                        scheduleId,
                                        resolved.rangeIndex,
                                        resolved.endAt
                                    )
                                    ReminderScheduler.cancelScheduleCountdown(
                                        context.applicationContext,
                                        scheduleId
                                    )
                                }
                            }

                            AppDebugLog.d(
                                "NOTIFICATION",
                                "schedule range started scheduleId=$scheduleId rangeIndex=${intent.getIntExtra(EXTRA_RANGE_INDEX, -1)} enabled=$notificationsEnabled"
                            )
                        }

                        ACTION_SCHEDULE_END -> {
                            NotificationHelper.cancelScheduleNotification(
                                context.applicationContext,
                                scheduleId.toString()
                            )
                            ReminderScheduler.cancelScheduleCountdown(
                                context.applicationContext,
                                scheduleId
                            )
                            ReminderScheduler.scheduleNextScheduleReminder(
                                context.applicationContext,
                                schedule
                            )
                            AppDebugLog.d(
                                "NOTIFICATION",
                                "schedule range ended scheduleId=$scheduleId rangeIndex=${intent.getIntExtra(EXTRA_RANGE_INDEX, -1)}"
                            )
                        }

                        ACTION_SCHEDULE_ALARM,
                        ACTION_SCHEDULE_COUNTDOWN -> {
                            if (!notificationsEnabled) {
                                NotificationHelper.cancelScheduleNotification(
                                    context.applicationContext,
                                    scheduleId.toString()
                                )
                                ReminderScheduler.cancelScheduleCountdown(
                                    context.applicationContext,
                                    scheduleId
                                )
                                AppDebugLog.d(
                                    "NOTIFICATION",
                                    "schedule notification disabled scheduleId=$scheduleId"
                                )
                                return@launch
                            }

                            val startAt = intent.getLongExtra(EXTRA_START_AT, -1L).takeIf { it > 0L }
                                ?: ReminderScheduler.currentOrNextScheduleStartTime(schedule)
                            val remainingMillis = startAt - System.currentTimeMillis()
                            if (remainingMillis <= 0L) {
                                ReminderScheduler.scheduleNextScheduleReminder(
                                    context.applicationContext,
                                    schedule
                                )
                                return@launch
                            }

                            val rangeIndex = intent.getIntExtra(EXTRA_RANGE_INDEX, -1)
                            val ranges = schedule.getTimeRanges().sortedBy { it.startMinutes }
                            val range = ranges.getOrNull(rangeIndex)
                                ?: ReminderScheduler.currentOrNextScheduleOccurrence(schedule)?.let { occurrence ->
                                    com.mytask.data.local.ScheduleTimeRange(
                                        occurrence.startMinutes,
                                        occurrence.endMinutes
                                    )
                                }

                            val course = schedule.courseId?.let {
                                database.courseDao().getCourseById(it).first()
                            }

                            val message = buildString {
                                if (range != null) {
                                    append(range.startMinutes.toDisplayTime())
                                        .append(" - ")
                                        .append(range.endMinutes.toDisplayTime())
                                        .append("\n")
                                }
                                append(course?.name ?: "Mata Kuliah")
                                if (schedule.room.isNotBlank()) {
                                    append("\nRuangan: ").append(schedule.room)
                                }
                                append("\n\nTap untuk melihat jadwal")
                            }

                            NotificationHelper.showScheduleNotification(
                                context.applicationContext,
                                schedule.id.toString(),
                                "🕒 Jadwal yang akan datang",
                                message,
                                countdownUntilMillis = startAt
                            )

                            AppDebugLog.d(
                                "NOTIFICATION",
                                "schedule notification started scheduleId=$scheduleId rangeIndex=$rangeIndex countdownUntil=$startAt"
                            )
                        }
                    }
                } finally {
                    database.close()
                }
            } catch (error: Throwable) {
                AppDebugLog.e("NOTIFICATION", "schedule alarm failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun buildActiveMessage(room: String, range: com.mytask.data.local.ScheduleTimeRange): String =
        buildString {
            append(range.startMinutes.toDisplayTime())
                .append(" - ")
                .append(range.endMinutes.toDisplayTime())
            if (room.isNotBlank()) {
                append("\nRuangan: ").append(room)
            }
            append("\n\nJadwal sedang berlangsung")
        }
}
