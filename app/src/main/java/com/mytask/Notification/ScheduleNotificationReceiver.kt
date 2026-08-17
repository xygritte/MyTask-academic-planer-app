package com.mytask.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.mytask.data.local.MyTaskDatabase
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
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                if (scheduleId <= 0L) return@launch

                val database = Room.databaseBuilder(
                    context.applicationContext,
                    MyTaskDatabase::class.java,
                    "mytask_db"
                ).build()

                try {
                    val schedule = database.scheduleDao().getScheduleById(scheduleId).first()
                    if (schedule == null) {
                        AppDebugLog.d("NOTIFICATION", "schedule alarm ignored: schedule missing id=$scheduleId")
                        return@launch
                    }

                    val settingsRepository = SettingsRepository(context.applicationContext)
                    val notificationsEnabled = settingsRepository.scheduleNotificationEnabled(scheduleId).first()

                    when (intent.action) {
                        ACTION_SCHEDULE_START -> {
                            NotificationHelper.cancelScheduleNotification(
                                context.applicationContext,
                                scheduleId.toString()
                            )
                            ReminderScheduler.scheduleNextScheduleReminder(
                                context.applicationContext,
                                schedule
                            )
                            AppDebugLog.d(
                                "NOTIFICATION",
                                "schedule started; notification cleared scheduleId=$scheduleId enabled=$notificationsEnabled"
                            )
                        }

                        ACTION_SCHEDULE_ALARM,
                        ACTION_SCHEDULE_COUNTDOWN -> {
                            if (!notificationsEnabled) {
                                NotificationHelper.cancelScheduleNotification(
                                    context.applicationContext,
                                    scheduleId.toString()
                                )
                                ReminderScheduler.cancelScheduleCountdown(context.applicationContext, scheduleId)
                                AppDebugLog.d(
                                    "NOTIFICATION",
                                    "schedule notification disabled scheduleId=$scheduleId"
                                )
                                return@launch
                            }

                            val startAt = ReminderScheduler.currentOrNextScheduleStartTime(schedule)
                            val remainingMillis = startAt - System.currentTimeMillis()
                            if (remainingMillis <= 0L) {
                                NotificationHelper.cancelScheduleNotification(
                                    context.applicationContext,
                                    scheduleId.toString()
                                )
                                ReminderScheduler.scheduleNextScheduleReminder(
                                    context.applicationContext,
                                    schedule
                                )
                                return@launch
                            }

                            val course = schedule.courseId?.let {
                                database.courseDao().getCourseById(it).first()
                            }
                            val message = buildString {
                                append(schedule.startMinutes.toDisplayTime())
                                    .append(" - ")
                                    .append(schedule.endMinutes.toDisplayTime())
                                    .append("\n")
                                append(course?.name ?: "Mata Kuliah")
                                if (schedule.room.isNotBlank()) {
                                    append("\nRuangan: ").append(schedule.room)
                                }
                                append("\n\nWaktu berjalan live sampai kuliah dimulai.")
                            }

                            NotificationHelper.showScheduleNotification(
                                context.applicationContext,
                                schedule.id.toString(),
                                "🕒 Kuliah yang akan datang",
                                message,
                                countdownUntilMillis = startAt
                            )

                            ReminderScheduler.cancelScheduleCountdown(
                                context.applicationContext,
                                scheduleId
                            )

                            AppDebugLog.d(
                                "NOTIFICATION",
                                "schedule chronometer started scheduleId=$scheduleId countdownUntil=$startAt"
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
}
