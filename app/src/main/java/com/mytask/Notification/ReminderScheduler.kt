package com.mytask.Notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.room.Room
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.local.getTimeRanges
import com.mytask.debug.AppDebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

object ReminderScheduler {
    private const val ALARM_REQUEST_CODE = 5001
    private const val TASK_DEADLINE_REQUEST_BASE = 6000
    private const val SCHEDULE_REQUEST_BASE = 7000
    private const val SCHEDULE_START_REQUEST_BASE = 8000
    private const val SCHEDULE_COUNTDOWN_REQUEST_BASE = 9000
    private const val IMMEDIATE_WORK_NAME = "mytask_today_notification"
    const val EVERY_DAY = 0
    const val TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L

    data class ScheduleOccurrence(
        val rangeIndex: Int,
        val startAt: Long,
        val startMinutes: Int,
        val endMinutes: Int
    )

    fun syncToday(context: Context) {
        AppDebugLog.d("NOTIFICATION", "syncToday enqueue worker")
        val request = androidx.work.OneTimeWorkRequestBuilder<DailyReminderWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleTaskDeadline(context: Context, taskId: Long, deadline: Date?) {
        cancelTaskDeadline(context, taskId)
        if (deadline == null || deadline.time <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DeadlineReceiver::class.java).apply {
            putExtra(DeadlineReceiver.EXTRA_TASK_ID, taskId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskDeadlineRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, deadline.time, pendingIntent)
    }

    fun cancelTaskDeadline(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(taskDeadlinePendingIntent(context, taskId))
    }

    fun rescheduleAllTaskDeadlines(context: Context, tasks: List<TaskEntity>) {
        tasks.forEach { task ->
            if (task.isCompleted) cancelTaskDeadline(context, task.id)
            else scheduleTaskDeadline(context, task.id, task.deadline)
        }
    }

    /**
     * Schedules only the next upcoming range for this schedule.
     * Once that range starts, ScheduleNotificationReceiver advances the chain
     * to the following range. This avoids overlapping PendingIntents while
     * still supporting any number of ranges within one schedule.
     */
    fun scheduleScheduleReminder(context: Context, schedule: ScheduleEntity) {
        cancelScheduleReminderAlarms(context, schedule.id)

        val occurrence = currentOrNextScheduleOccurrence(schedule) ?: run {
            AppDebugLog.d("NOTIFICATION", "no valid schedule ranges id=${schedule.id}")
            return
        }

        val nowMillis = System.currentTimeMillis()
        val reminderAt = occurrence.startAt - TWO_HOURS_MILLIS
        val triggerAt = if (nowMillis < reminderAt) reminderAt else nowMillis + 1_000L

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        setScheduleAlarm(
            alarmManager,
            triggerAt,
            scheduleReminderPendingIntent(context, schedule.id, occurrence)
        )
        setScheduleAlarm(
            alarmManager,
            occurrence.startAt,
            scheduleStartPendingIntent(context, schedule.id, occurrence)
        )

        AppDebugLog.d(
            "NOTIFICATION",
            "schedule reminder scheduled scheduleId=${schedule.id} rangeIndex=${occurrence.rangeIndex} " +
                "startAt=${occurrence.startAt} triggerAt=$triggerAt"
        )
    }

    fun scheduleNextScheduleReminder(context: Context, schedule: ScheduleEntity) {
        scheduleScheduleReminder(context, schedule)
    }

    fun cancelScheduleCountdown(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(scheduleCountdownPendingIntent(context, scheduleId))
    }

    fun cancelScheduleReminder(context: Context, scheduleId: Long) {
        cancelScheduleReminderAlarms(context, scheduleId)
        NotificationHelper.cancelScheduleNotification(context, scheduleId.toString())
    }

    fun rescheduleAllScheduleReminders(context: Context, schedules: List<ScheduleEntity>) {
        schedules.forEach { scheduleScheduleReminder(context, it) }
    }

    fun rescheduleAllStoredScheduleReminders(context: Context) {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val database = Room.databaseBuilder(
                context.applicationContext,
                MyTaskDatabase::class.java,
                "mytask_db"
            ).build()
            try {
                val schedules = database.scheduleDao().getAllSchedulesSnapshot()
                rescheduleAllScheduleReminders(context.applicationContext, schedules)
            } catch (error: Throwable) {
                AppDebugLog.e("NOTIFICATION", "schedule alarms restore failed", error)
            } finally {
                database.close()
            }
        }
    }

    fun scheduleNextMidnight(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = midnightPendingIntent(context)
        val nextMidnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        alarmManager.cancel(pendingIntent)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMidnight.timeInMillis,
            pendingIntent
        )
    }

    fun initialize(context: Context) {
        syncToday(context)
        scheduleNextMidnight(context)
        rescheduleAllStoredScheduleReminders(context)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(midnightPendingIntent(context))
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }

    fun currentOrNextScheduleOccurrence(
        schedule: ScheduleEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduleOccurrence? {
        return schedule.getTimeRanges()
            .sortedBy { it.startMinutes }
            .mapIndexedNotNull { index, range ->
                occurrenceAtOrAfterStart(schedule, range.startMinutes, nowMillis)?.let { startAt ->
                    ScheduleOccurrence(
                        rangeIndex = index,
                        startAt = startAt,
                        startMinutes = range.startMinutes,
                        endMinutes = range.endMinutes
                    )
                }
            }
            .minByOrNull { it.startAt }
    }

    fun currentOrNextScheduleStartTime(
        schedule: ScheduleEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        return currentOrNextScheduleOccurrence(schedule, nowMillis)?.startAt
            ?: nowMillis
    }

    fun occurrenceAfterCurrentRange(
        schedule: ScheduleEntity,
        rangeIndex: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): ScheduleOccurrence? {
        val ranges = schedule.getTimeRanges().sortedBy { it.startMinutes }
        val nextIndex = rangeIndex + 1
        if (nextIndex in ranges.indices) {
            val range = ranges[nextIndex]
            occurrenceAtOrAfterStart(schedule, range.startMinutes, nowMillis)?.let { startAt ->
                return ScheduleOccurrence(
                    rangeIndex = nextIndex,
                    startAt = startAt,
                    startMinutes = range.startMinutes,
                    endMinutes = range.endMinutes
                )
            }
        }
        return null
    }

    private fun occurrenceAtOrAfterStart(
        schedule: ScheduleEntity,
        startMinutes: Int,
        nowMillis: Long
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, startMinutes / 60)
            set(Calendar.MINUTE, startMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (schedule.dayOfWeek == EVERY_DAY) {
                if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
            } else {
                set(Calendar.DAY_OF_WEEK, schedule.dayOfWeek)
                if (timeInMillis <= nowMillis) add(Calendar.WEEK_OF_YEAR, 1)
            }
        }
        return candidate.timeInMillis
    }

    private fun setScheduleAlarm(
        alarmManager: AlarmManager,
        triggerAt: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun taskDeadlineRequestCode(taskId: Long): Int =
        TASK_DEADLINE_REQUEST_BASE + (taskId.hashCode() and 0x7fffffff) % 100000

    private fun scheduleRequestCode(scheduleId: Long): Int =
        SCHEDULE_REQUEST_BASE + (scheduleId.hashCode() and 0x7fffffff) % 100000

    private fun scheduleStartRequestCode(scheduleId: Long): Int =
        SCHEDULE_START_REQUEST_BASE + (scheduleId.hashCode() and 0x7fffffff) % 100000

    private fun scheduleCountdownRequestCode(scheduleId: Long): Int =
        SCHEDULE_COUNTDOWN_REQUEST_BASE + (scheduleId.hashCode() and 0x7fffffff) % 100000

    private fun taskDeadlinePendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, DeadlineReceiver::class.java).apply {
            putExtra(DeadlineReceiver.EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            taskDeadlineRequestCode(taskId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelScheduleReminderAlarms(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(scheduleReminderPendingIntent(context, scheduleId, null))
        alarmManager.cancel(scheduleStartPendingIntent(context, scheduleId, null))
        alarmManager.cancel(scheduleCountdownPendingIntent(context, scheduleId))
    }

    private fun scheduleReminderPendingIntent(
        context: Context,
        scheduleId: Long,
        occurrence: ScheduleOccurrence?
    ): PendingIntent {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            action = ScheduleNotificationReceiver.ACTION_SCHEDULE_ALARM
            putExtra(ScheduleNotificationReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            occurrence?.let {
                putExtra(ScheduleNotificationReceiver.EXTRA_RANGE_INDEX, it.rangeIndex)
                putExtra(ScheduleNotificationReceiver.EXTRA_START_AT, it.startAt)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleRequestCode(scheduleId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleStartPendingIntent(
        context: Context,
        scheduleId: Long,
        occurrence: ScheduleOccurrence?
    ): PendingIntent {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            action = ScheduleNotificationReceiver.ACTION_SCHEDULE_START
            putExtra(ScheduleNotificationReceiver.EXTRA_SCHEDULE_ID, scheduleId)
            occurrence?.let {
                putExtra(ScheduleNotificationReceiver.EXTRA_RANGE_INDEX, it.rangeIndex)
                putExtra(ScheduleNotificationReceiver.EXTRA_START_AT, it.startAt)
            }
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleStartRequestCode(scheduleId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun scheduleCountdownPendingIntent(context: Context, scheduleId: Long): PendingIntent {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            action = ScheduleNotificationReceiver.ACTION_SCHEDULE_COUNTDOWN
            putExtra(ScheduleNotificationReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleCountdownRequestCode(scheduleId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun midnightPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            Intent(context, MidnightReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
