package com.mytask.Notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import com.mytask.debug.AppDebugLog
import java.util.Calendar
import java.util.Date

object ReminderScheduler {

    private const val ALARM_REQUEST_CODE = 5001
    private const val TASK_DEADLINE_REQUEST_BASE = 6000
    private const val SCHEDULE_REQUEST_BASE = 7000
    private const val IMMEDIATE_WORK_NAME = "mytask_today_notification"
    private const val TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L

    fun syncToday(context: Context) {
        AppDebugLog.d("NOTIFICATION", "syncToday enqueue worker")
        val request = androidx.work.OneTimeWorkRequestBuilder<DailyReminderWorker>().build()
        androidx.work.WorkManager.getInstance(context).enqueueUniqueWork(
            IMMEDIATE_WORK_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun scheduleTaskDeadline(
        context: Context,
        taskId: Long,
        deadline: Date?
    ) {
        cancelTaskDeadline(context, taskId)

        if (deadline == null || deadline.time <= System.currentTimeMillis()) {
            AppDebugLog.d("NOTIFICATION", "deadline alarm skipped taskId=$taskId deadline=$deadline")
            return
        }

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

        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            deadline.time,
            pendingIntent
        )

        AppDebugLog.d("NOTIFICATION", "deadline alarm scheduled taskId=$taskId at=${deadline.time}")
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

    fun scheduleScheduleReminder(context: Context, schedule: ScheduleEntity) {
        cancelScheduleReminder(context, schedule.id)

        val triggerAt = nextScheduleReminderTime(schedule, System.currentTimeMillis())
        if (triggerAt == null) {
            AppDebugLog.d("NOTIFICATION", "schedule reminder skipped scheduleId=${schedule.id}")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            action = ScheduleNotificationReceiver.ACTION_SCHEDULE_ALARM
            putExtra(ScheduleNotificationReceiver.EXTRA_SCHEDULE_ID, schedule.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            scheduleRequestCode(schedule.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } else {
            // Fallback when exact-alarm access has not been granted.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }

        AppDebugLog.d(
            "NOTIFICATION",
            "schedule reminder scheduled scheduleId=${schedule.id} triggerAt=$triggerAt"
        )
    }

    fun cancelScheduleReminder(context: Context, scheduleId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(scheduleReminderPendingIntent(context, scheduleId))
        NotificationHelper.cancelScheduleNotification(context, scheduleId.toString())
        AppDebugLog.d("NOTIFICATION", "schedule reminder cancelled scheduleId=$scheduleId")
    }

    fun rescheduleAllScheduleReminders(context: Context, schedules: List<ScheduleEntity>) {
        schedules.forEach { scheduleScheduleReminder(context, it) }
    }

    fun scheduleNextScheduleReminder(context: Context, schedule: ScheduleEntity) {
        scheduleScheduleReminder(context, schedule)
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

        AppDebugLog.d("NOTIFICATION", "next midnight scheduled=${nextMidnight.timeInMillis}")
    }

    fun initialize(context: Context) {
        AppDebugLog.d("NOTIFICATION", "initialize reminder scheduler")
        syncToday(context)
        scheduleNextMidnight(context)
    }

    fun cancel(context: Context) {
        AppDebugLog.d("NOTIFICATION", "cancel reminder scheduler")
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(midnightPendingIntent(context))
        androidx.work.WorkManager.getInstance(context).cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }

    private fun nextScheduleReminderTime(schedule: ScheduleEntity, nowMillis: Long): Long? {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_WEEK, schedule.dayOfWeek)
            set(Calendar.HOUR_OF_DAY, schedule.startMinutes / 60)
            set(Calendar.MINUTE, schedule.startMinutes % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var triggerAt = candidate.timeInMillis - TWO_HOURS_MILLIS

        // If today's two-hour reminder has already passed, move to next week's occurrence.
        if (triggerAt <= now.timeInMillis) {
            candidate.add(Calendar.WEEK_OF_YEAR, 1)
            triggerAt = candidate.timeInMillis - TWO_HOURS_MILLIS
        }

        return triggerAt.takeIf { it > nowMillis }
    }

    private fun taskDeadlineRequestCode(taskId: Long): Int =
        TASK_DEADLINE_REQUEST_BASE + (taskId.hashCode() and 0x7fffffff) % 100000

    private fun scheduleRequestCode(scheduleId: Long): Int =
        SCHEDULE_REQUEST_BASE + (scheduleId.hashCode() and 0x7fffffff) % 100000

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

    private fun scheduleReminderPendingIntent(context: Context, scheduleId: Long): PendingIntent {
        val intent = Intent(context, ScheduleNotificationReceiver::class.java).apply {
            action = ScheduleNotificationReceiver.ACTION_SCHEDULE_ALARM
            putExtra(ScheduleNotificationReceiver.EXTRA_SCHEDULE_ID, scheduleId)
        }
        return PendingIntent.getBroadcast(
            context,
            scheduleRequestCode(scheduleId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun midnightPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MidnightReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
