package com.mytask.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mytask.debug.AppDebugLog

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        AppDebugLog.d("NOTIFICATION", "boot completed: restoring schedule alarms")
        ReminderScheduler.syncToday(context)
        ReminderScheduler.rescheduleAllStoredScheduleReminders(context)
        ReminderScheduler.scheduleNextMidnight(context)
    }
}
