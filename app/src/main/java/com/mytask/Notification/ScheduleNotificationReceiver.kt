package com.mytask.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.toDisplayTime
import com.mytask.debug.AppDebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ScheduleNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SCHEDULE_ALARM = "com.mytask.action.SCHEDULE_ALARM"
        const val EXTRA_SCHEDULE_ID = "schedule_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action != ACTION_SCHEDULE_ALARM) return@launch

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
                        append("\n\nKuliah dimulai dalam 2 jam.")
                    }

                    NotificationHelper.showScheduleNotification(
                        context.applicationContext,
                        schedule.id.toString(),
                        "🕒 Jadwal Kuliah 2 Jam Lagi",
                        message
                    )

                    // This schedule is weekly. After firing once, schedule the
                    // next occurrence seven days later.
                    ReminderScheduler.scheduleNextScheduleReminder(
                        context.applicationContext,
                        schedule
                    )
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
