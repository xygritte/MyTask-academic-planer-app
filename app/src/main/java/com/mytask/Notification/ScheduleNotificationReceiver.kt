package com.mytask.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat

class ScheduleNotificationReceiver :
    BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        val notificationId =
            intent.getIntExtra(
                "notification_id",
                -1
            )

        if (notificationId != -1) {

            NotificationManagerCompat
                .from(context)
                .cancel(
                    notificationId
                )
        }
    }
}