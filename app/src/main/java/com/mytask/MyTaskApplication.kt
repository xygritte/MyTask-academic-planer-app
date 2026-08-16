package com.mytask

import android.app.Application
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
}
