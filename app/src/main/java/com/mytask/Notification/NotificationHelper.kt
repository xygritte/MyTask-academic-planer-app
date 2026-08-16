package com.mytask.Notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.mytask.MainActivity
import com.mytask.R
import kotlin.math.abs

object NotificationHelper {

    private const val TASK_CHANNEL_ID = "task_reminder"
    private const val SCHEDULE_CHANNEL_ID = "schedule_reminder"
    private const val ACTIVE_TASKS_NOTIFICATION_ID = 4000

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val taskChannel = NotificationChannel(
            TASK_CHANNEL_ID,
            "Pengingat Tugas",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pengingat tugas"
        }

        val scheduleChannel = NotificationChannel(
            SCHEDULE_CHANNEL_ID,
            "Jadwal Kuliah",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Jadwal kuliah hari ini"
        }

        manager.createNotificationChannel(taskChannel)
        manager.createNotificationChannel(scheduleChannel)
    }

    fun showActiveTasksNotification(
        context: Context,
        message: String
    ) {
        if (!canNotify(context)) return

        createChannels(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            ACTIVE_TASKS_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat
            .Builder(context, TASK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Tugas Aktif")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setOnlyAlertOnce(true)
            .build()

        NotificationManagerCompat
            .from(context)
            .notify(ACTIVE_TASKS_NOTIFICATION_ID, notification)
    }

    fun cancelActiveTasksNotification(context: Context) {
        NotificationManagerCompat
            .from(context)
            .cancel(ACTIVE_TASKS_NOTIFICATION_ID)
    }

    fun showTaskNotification(
        context: Context,
        taskId: String,
        title: String,
        message: String
    ) {
        showTaskNotificationInternal(
            context = context,
            taskId = taskId,
            title = title,
            message = message,
            overdue = false
        )
    }

    fun showOverdueTaskNotification(
        context: Context,
        taskId: String,
        title: String,
        message: String
    ) {
        showTaskNotificationInternal(
            context = context,
            taskId = taskId,
            title = title,
            message = message,
            overdue = true
        )
    }

    private fun showTaskNotificationInternal(
        context: Context,
        taskId: String,
        title: String,
        message: String,
        overdue: Boolean
    ) {
        if (!canNotify(context)) return

        createChannels(context)

        val notificationId = taskNotificationId(taskId)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat
            .Builder(context, TASK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (overdue) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
        } else {
            builder.setCategory(NotificationCompat.CATEGORY_REMINDER)
        }

        NotificationManagerCompat
            .from(context)
            .notify(notificationId, builder.build())
    }

    fun cancelTaskNotification(
        context: Context,
        taskId: String
    ) {
        NotificationManagerCompat
            .from(context)
            .cancel(taskNotificationId(taskId))
    }

    fun showScheduleNotification(
        context: Context,
        scheduleId: String,
        title: String,
        message: String
    ) {
        if (!canNotify(context)) return

        createChannels(context)

        val notificationId = scheduleNotificationId(scheduleId)

        val intent = Intent(
            context,
            ScheduleNotificationReceiver::class.java
        ).apply {
            putExtra("notification_id", notificationId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat
            .Builder(context, SCHEDULE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        NotificationManagerCompat
            .from(context)
            .notify(notificationId, notification)
    }

    /**
     * Clears every notification currently posted by MyTask on this device.
     * Needed when changing accounts so account A's reminders cannot remain
     * visible while account B is active.
     */
    fun cancelAllAppNotifications(context: Context) {
        NotificationManagerCompat
            .from(context)
            .cancelAll()
    }

    private fun taskNotificationId(taskId: String): Int {
        val hash = abs(taskId.hashCode())
        return if (hash == 0) 2000 else hash
    }

    private fun scheduleNotificationId(scheduleId: String): Int {
        val hash = abs(scheduleId.hashCode())
        return if (hash == 0) 3000 else hash
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }

        return NotificationManagerCompat
            .from(context)
            .areNotificationsEnabled()
    }
}
