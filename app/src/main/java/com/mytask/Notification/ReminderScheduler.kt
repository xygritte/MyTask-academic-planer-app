package com.mytask.Notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar

object ReminderScheduler {

    private const val ALARM_REQUEST_CODE = 5001

    private const val IMMEDIATE_WORK_NAME =
        "mytask_today_notification"

    fun syncToday(
        context: Context
    ) {

        val request =
            OneTimeWorkRequestBuilder<DailyReminderWorker>()
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
    }

    fun scheduleNextMidnight(
        context: Context
    ) {

        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

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

    fun initialize(
        context: Context
    ) {
        syncToday(context)
        scheduleNextMidnight(context)
    }

    fun cancel(
        context: Context
    ) {
        val alarmManager =
            context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

        alarmManager.cancel(
            midnightPendingIntent(context)
        )

        WorkManager
            .getInstance(context)
            .cancelUniqueWork(IMMEDIATE_WORK_NAME)
    }

    private fun midnightPendingIntent(
        context: Context
    ): PendingIntent {
        val intent = Intent(
            context,
            MidnightReceiver::class.java
        )

        return PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )
    }
}
