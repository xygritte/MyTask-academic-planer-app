package com.mytask

import android.app.Application
import android.content.Intent
import com.mytask.Notification.NotificationHelper
import com.mytask.Notification.ReminderScheduler
import com.mytask.debug.AppDebugLog
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyTaskApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AppDebugLog.d(
            "APP",
            "Application.onCreate package=$packageName process=${android.os.Process.myPid()}"
        )

        NotificationHelper.createChannels(this)
        ReminderScheduler.initialize(this)

        AppDebugLog.d("APP", "Application initialization completed")
    }

    /**
     * Compose ProfileScreen intentionally uses the application context.
     * Add NEW_TASK automatically so external links (Instagram, browser, etc.)
     * can safely be launched from that context.
     */
    override fun startActivity(intent: Intent) {
        super.startActivity(
            Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
