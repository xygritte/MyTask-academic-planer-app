package com.mytask

import android.app.Application
import com.mytask.Notification.NotificationHelper
import com.mytask.Notification.ReminderScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyTaskApplication : Application() {

    override fun onCreate() {

        super.onCreate()

        NotificationHelper
            .createChannels(this)

        /*
         * Setiap aplikasi aktif,
         * sinkronisasi data hari ini.
         */
        ReminderScheduler.initialize(
            this
        )
    }
}