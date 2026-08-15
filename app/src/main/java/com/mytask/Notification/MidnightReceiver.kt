package com.mytask.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MidnightReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent?
    ) {

        /*
         * Hari berubah.
         *
         * Langsung sinkronisasi
         * semua tugas dan jadwal hari baru.
         */

        ReminderScheduler.syncToday(
            context
        )

        /*
         * Siapkan pergantian hari berikutnya.
         */

        ReminderScheduler.scheduleNextMidnight(
            context
        )
    }
}