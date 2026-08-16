package com.mytask.Notification

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mytask.debug.AppDebugLog
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.local.toDisplayTime
import com.mytask.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        AppDebugLog.d("NOTIFICATION", "worker start id=$id")
        val database = Room.databaseBuilder(applicationContext, MyTaskDatabase::class.java, "mytask_db").build()
        val settingsRepository = SettingsRepository(applicationContext)

        return try {
            NotificationHelper.createChannels(applicationContext)
            val tasks = database.taskDao().getAllTasks().first()
            val reminderDays = settingsRepository.taskReminderDays.first()
            val activeTaskNotificationEnabled = settingsRepository.activeTaskNotification.first()
            AppDebugLog.d("NOTIFICATION", "worker data tasks=${tasks.size} reminderDays=$reminderDays activeTasksNotification=$activeTaskNotificationEnabled")

            if (activeTaskNotificationEnabled) checkActiveTasks(tasks)
            else NotificationHelper.cancelActiveTasksNotification(applicationContext)

            checkDeadlineTasks(tasks, reminderDays)
            ReminderScheduler.rescheduleAllTaskDeadlines(applicationContext, tasks)
            checkTodaySchedules(database)
            ReminderScheduler.scheduleNextMidnight(applicationContext)

            AppDebugLog.d("NOTIFICATION", "worker success")
            Result.success()
        } catch (e: Exception) {
            AppDebugLog.e("NOTIFICATION", "worker failed", e)
            Result.failure()
        } finally {
            database.close()
            AppDebugLog.d("NOTIFICATION", "worker database closed")
        }
    }

    private fun checkActiveTasks(tasks: List<TaskEntity>) {
        val activeTasks = tasks.filter { !it.isCompleted }
        if (activeTasks.isEmpty()) {
            NotificationHelper.cancelActiveTasksNotification(applicationContext)
            return
        }

        val today = Calendar.getInstance()
        val visibleTasks = activeTasks.sortedBy { it.deadline ?: Date(Long.MAX_VALUE) }.take(4)
        val message = buildString {
            visibleTasks.forEachIndexed { index, task ->
                append("• ").append(task.title)
                task.deadline?.let { append(" — ").append(getDeadlineText(today, it)) }
                if (index < visibleTasks.lastIndex) append("\n")
            }
            if (activeTasks.size > visibleTasks.size) append("\n+ ${activeTasks.size - visibleTasks.size} tugas lainnya")
        }
        NotificationHelper.showActiveTasksNotification(applicationContext, message)
    }

    private fun checkDeadlineTasks(tasks: List<TaskEntity>, reminderDays: Int) {
        val today = Calendar.getInstance().apply { resetTime() }
        val reminderStart = Calendar.getInstance().apply {
            time = today.time
            add(Calendar.DAY_OF_YEAR, reminderDays)
        }

        tasks.forEach { task ->
            if (task.isCompleted || task.deadline == null) {
                NotificationHelper.cancelTaskNotification(applicationContext, task.id.toString())
                return@forEach
            }

            val deadlineDay = Calendar.getInstance().apply {
                time = task.deadline!!
                resetTime()
            }
            val isOverdue = deadlineDay.timeInMillis < today.timeInMillis
            val shouldShow = deadlineDay.timeInMillis <= reminderStart.timeInMillis

            if (!shouldShow) {
                NotificationHelper.cancelTaskNotification(applicationContext, task.id.toString())
                return@forEach
            }

            if (isOverdue) {
                val overdueDays = getOverdueDays(today, deadlineDay)
                NotificationHelper.showOverdueTaskNotification(
                    applicationContext,
                    task.id.toString(),
                    task.deadline!!.time,
                    "⚠️ ${task.title}",
                    buildOverdueTaskDetail(task, overdueDays)
                )
                return@forEach
            }

            NotificationHelper.showTaskNotification(
                applicationContext,
                task.id.toString(),
                "📝 ${task.title}",
                buildUpcomingTaskDetail(task)
            )
        }
    }

    private fun buildUpcomingTaskDetail(task: TaskEntity): String = buildString {
        task.description.takeIf { it.isNotBlank() }?.let { append(it).append("\n\n") }
        append("Deadline: ").append(getDeadlineText(Calendar.getInstance(), task.deadline!!))
        append("\nPrioritas: ").append(getPriorityText(task.priority))
        append("\n\nTap untuk membuka tugas.")
    }

    private fun buildOverdueTaskDetail(task: TaskEntity, overdueDays: Long): String = buildString {
        append("⚠️ TERLAMBAT\nSudah lewat ").append(overdueDays).append(" hari")
        task.description.takeIf { it.isNotBlank() }?.let { append("\n\n").append(it) }
        append("\n\nPrioritas: ").append(getPriorityText(task.priority))
        append("\n\nTap untuk membuka tugas.")
    }

    private suspend fun checkTodaySchedules(database: MyTaskDatabase) {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        val schedules = database.scheduleDao().getSchedulesByDay(today).first()
        if (schedules.isEmpty()) return

        val courses = database.courseDao().getAllCourses().first()
        schedules.sortedBy { it.startMinutes }.forEach { schedule ->
            val course = courses.find { it.id == schedule.courseId }
            val message = buildString {
                append("${schedule.startMinutes.toDisplayTime()} - ${schedule.endMinutes.toDisplayTime()}\n")
                append(course?.name ?: "Mata Kuliah")
                if (schedule.room.isNotBlank()) append("\nRuangan: ${schedule.room}")
                append("\n\nTap untuk konfirmasi.")
            }
            NotificationHelper.showScheduleNotification(
                applicationContext,
                schedule.id.toString(),
                "🕒 Jadwal Kuliah",
                message
            )
        }
    }

    private fun getDeadlineText(now: Calendar, deadline: Date): String {
        val today = Calendar.getInstance().apply { time = now.time; resetTime() }
        val target = Calendar.getInstance().apply { time = deadline; resetTime() }
        val days = TimeUnit.MILLISECONDS.toDays(target.timeInMillis - today.timeInMillis)
        return when {
            days < 0L -> "terlambat ${-days} hari"
            days == 0L -> "hari ini"
            days == 1L -> "1 hari lagi"
            else -> "$days hari lagi"
        }
    }

    private fun getOverdueDays(today: Calendar, deadline: Calendar): Long =
        TimeUnit.MILLISECONDS.toDays(today.timeInMillis - deadline.timeInMillis).coerceAtLeast(1L)

    private fun getPriorityText(priority: Int): String = when (priority) {
        1 -> "Tinggi"
        2 -> "Sedang"
        3 -> "Rendah"
        else -> "Normal"
    }

    private fun Calendar.resetTime() {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
