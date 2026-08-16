package com.mytask.Notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.mytask.data.local.MyTaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DeadlineReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val taskId = intent?.getLongExtra(EXTRA_TASK_ID, -1L) ?: -1L
        if (taskId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val database = Room.databaseBuilder(
                context.applicationContext,
                MyTaskDatabase::class.java,
                "mytask_db"
            ).build()

            try {
                val task = database.taskDao().getTaskById(taskId).first()
                if (task != null && !task.isCompleted && task.deadline != null) {
                    val overdueDays = java.util.concurrent.TimeUnit.MILLISECONDS.toDays(
                        (startOfToday().timeInMillis - startOfDay(task.deadline!!).timeInMillis)
                    ).coerceAtLeast(0L)

                    if (task.deadline!!.time <= System.currentTimeMillis()) {
                        NotificationHelper.showOverdueTaskNotification(
                            context.applicationContext,
                            task.id.toString(),
                            "⚠️ ${task.title}",
                            buildString {
                                append("⚠️ TERLAMBAT")
                                append("\n")
                                append("Sudah lewat ")
                                append(overdueDays.coerceAtLeast(1L))
                                append(" hari")
                                task.description.takeIf { it.isNotBlank() }?.let {
                                    append("\n\n")
                                    append(it)
                                }
                                append("\n\nPrioritas: ")
                                append(
                                    when (task.priority) {
                                        1 -> "Tinggi"
                                        2 -> "Sedang"
                                        3 -> "Rendah"
                                        else -> "Normal"
                                    }
                                )
                                append("\n\nTap untuk membuka tugas.")
                            }
                        )
                    }
                }
            } finally {
                database.close()
                pendingResult.finish()
            }
        }
    }

    private fun startOfToday(): java.util.Calendar = CalendarUtils.startOfToday()

    private fun startOfDay(date: java.util.Date): java.util.Calendar =
        CalendarUtils.startOfDay(date)

    companion object {
        const val EXTRA_TASK_ID = "task_id"
    }
}

private object CalendarUtils {
    fun startOfToday(): java.util.Calendar =
        java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

    fun startOfDay(date: java.util.Date): java.util.Calendar =
        java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
}
