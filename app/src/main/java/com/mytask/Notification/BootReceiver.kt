package com.mytask.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action ==
            Intent.ACTION_BOOT_COMPLETED
        ) {

            /*
             * HP baru selesai boot.
             *
             * Jangan menunggu 00.00.
             * Langsung cek hari ini.
             */

            ReminderScheduler.syncToday(
                context
            )

            /*
             * Jadwalkan pergantian
             * hari berikutnya.
             */

            ReminderScheduler.scheduleNextMidnight(
                context
            )
        }
    }
}