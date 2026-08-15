package com.mytask.Notification

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class DailyReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParams
) {

    override suspend fun doWork(): Result {

        val database =
            Room.databaseBuilder(
                applicationContext,
                MyTaskDatabase::class.java,
                "mytask_db"
            ).build()

        val settingsRepository =
            SettingsRepository(
                applicationContext
            )

        return try {

            NotificationHelper.createChannels(
                applicationContext
            )

            /*
             * ==========================================
             * DATA
             * ==========================================
             */

            val tasks =
                database
                    .taskDao()
                    .getAllTasks()
                    .first()

            /*
             * ==========================================
             * SETTINGS
             * ==========================================
             */

            val reminderDays =
                settingsRepository
                    .taskReminderDays
                    .first()

            val activeTaskNotificationEnabled =
                settingsRepository
                    .activeTaskNotification
                    .first()

            /*
             * ==========================================
             * 1. TUGAS AKTIF
             * ==========================================
             */

            if (
                activeTaskNotificationEnabled
            ) {

                checkActiveTasks(
                    tasks
                )

            } else {

                NotificationHelper
                    .cancelActiveTasksNotification(
                        applicationContext
                    )
            }

            /*
             * ==========================================
             * 2. DEADLINE TASK
             * ==========================================
             */

            checkDeadlineTasks(
                tasks = tasks,
                reminderDays = reminderDays
            )

            /*
             * ==========================================
             * 3. JADWAL KULIAH
             * ==========================================
             */

            checkTodaySchedules(
                database
            )

            /*
             * ==========================================
             * MIDNIGHT BERIKUTNYA
             * ==========================================
             */

            ReminderScheduler
                .scheduleNextMidnight(
                    applicationContext
                )

            Result.success()

        } catch (e: Exception) {

            e.printStackTrace()

            Result.failure()

        } finally {

            database.close()
        }
    }


    // =================================================
    // TUGAS AKTIF
    // =================================================

    private fun checkActiveTasks(
        tasks: List<TaskEntity>
    ) {

        val activeTasks =
            tasks.filter {
                !it.isCompleted
            }

        if (
            activeTasks.isEmpty()
        ) {

            NotificationHelper
                .cancelActiveTasksNotification(
                    applicationContext
                )

            return
        }

        val today =
            Calendar.getInstance()

        val visibleTasks =
            activeTasks
                .sortedBy {
                    it.deadline
                        ?: Date(Long.MAX_VALUE)
                }
                .take(4)

        val message =
            buildString {

                visibleTasks
                    .forEachIndexed {
                            index,
                            task ->

                        append("• ")
                        append(task.title)

                        task.deadline?.let { deadline ->

                            append(
                                " — "
                            )

                            append(
                                getDeadlineText(
                                    today,
                                    deadline
                                )
                            )
                        }

                        if (
                            index <
                            visibleTasks.lastIndex
                        ) {

                            append("\n")
                        }
                    }

                if (
                    activeTasks.size >
                    visibleTasks.size
                ) {

                    append(
                        "\n+ ${
                            activeTasks.size -
                                    visibleTasks.size
                        } tugas lainnya"
                    )
                }
            }

        NotificationHelper
            .showActiveTasksNotification(
                applicationContext,
                message
            )
    }


    // =================================================
    // DEADLINE
    // =================================================
    //
    // Mulai H-X sesuai setting.
    //
    // Setelah deadline:
    // tetap permanen sampai task selesai.
    //
    // Overdue memakai tampilan berbeda.
    // =================================================

    private fun checkDeadlineTasks(
        tasks: List<TaskEntity>,
        reminderDays: Int
    ) {

        val today =
            Calendar.getInstance().apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

        val reminderStart =
            Calendar.getInstance().apply {

                time =
                    today.time

                add(
                    Calendar.DAY_OF_YEAR,
                    reminderDays
                )
            }

        tasks.forEach { task ->

            /*
             * ==========================================
             * SELESAI
             * ==========================================
             */

            if (
                task.isCompleted
            ) {

                NotificationHelper
                    .cancelTaskNotification(
                        applicationContext,
                        task.id.toString()
                    )

                return@forEach
            }

            /*
             * ==========================================
             * TANPA DEADLINE
             * ==========================================
             */

            val deadline =
                task.deadline

                    ?: run {

                        NotificationHelper
                            .cancelTaskNotification(
                                applicationContext,
                                task.id.toString()
                            )

                        return@forEach
                    }

            /*
             * ==========================================
             * NORMALISASI DEADLINE
             * ==========================================
             */

            val deadlineDay =
                Calendar.getInstance().apply {

                    time =
                        deadline

                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )

                    set(
                        Calendar.MINUTE,
                        0
                    )

                    set(
                        Calendar.SECOND,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

            /*
             * ==========================================
             * SUDAH TERLAMBAT?
             * ==========================================
             */

            val isOverdue =
                deadlineDay.timeInMillis <
                        today.timeInMillis

            /*
             * ==========================================
             * SUDAH MASUK WINDOW PENGINGAT?
             * ==========================================
             */

            val shouldShow =
                deadlineDay.timeInMillis <=
                        reminderStart.timeInMillis

            if (!shouldShow) {

                /*
                 * Deadline masih terlalu jauh.
                 */
                NotificationHelper
                    .cancelTaskNotification(
                        applicationContext,
                        task.id.toString()
                    )

                return@forEach
            }

            /*
             * ==========================================
             * OVERDUE
             * ==========================================
             */

            if (
                isOverdue
            ) {

                val overdueDays =
                    getOverdueDays(
                        today,
                        deadlineDay
                    )

                val detail =
                    buildOverdueTaskDetail(
                        task = task,
                        overdueDays = overdueDays
                    )

                NotificationHelper
                    .showOverdueTaskNotification(

                        applicationContext,

                        task.id.toString(),

                        "⚠️ ${task.title}",

                        detail
                    )

                return@forEach
            }

            /*
             * ==========================================
             * BELUM OVERDUE
             * ==========================================
             */

            val detail =
                buildUpcomingTaskDetail(
                    task
                )

            NotificationHelper
                .showTaskNotification(

                    applicationContext,

                    task.id.toString(),

                    "📝 ${task.title}",

                    detail
                )
        }
    }


    // =================================================
    // DETAIL TUGAS YANG BELUM TERLAMBAT
    // =================================================

    private fun buildUpcomingTaskDetail(
        task: TaskEntity
    ): String {

        val deadline =
            task.deadline
                ?: return buildBasicTaskDetail(
                    task
                )

        val status =
            getDeadlineText(
                Calendar.getInstance(),
                deadline
            )

        return buildString {

            task.description
                .takeIf {
                    it.isNotBlank()
                }
                ?.let {

                    append(it)

                    append(
                        "\n\n"
                    )
                }

            append(
                "Deadline: "
            )

            append(
                status
            )

            append(
                "\n"
            )

            append(
                "Prioritas: "
            )

            append(
                getPriorityText(
                    task.priority
                )
            )

            append(
                "\n\nTap untuk membuka tugas."
            )
        }
    }


    // =================================================
    // DETAIL TUGAS TERLAMBAT
    // =================================================

    private fun buildOverdueTaskDetail(
        task: TaskEntity,
        overdueDays: Long
    ): String {

        return buildString {

            append(
                "⚠️ TERLAMBAT"
            )

            append(
                "\n"
            )

            append(
                "Sudah lewat "
            )

            append(
                overdueDays
            )

            append(
                if (
                    overdueDays == 1L
                ) {
                    " hari"
                } else {
                    " hari"
                }
            )

            task.description
                .takeIf {
                    it.isNotBlank()
                }
                ?.let {

                    append(
                        "\n\n"
                    )

                    append(it)
                }

            append(
                "\n\nPrioritas: "
            )

            append(
                getPriorityText(
                    task.priority
                )
            )

            append(
                "\n\nTap untuk membuka tugas."
            )
        }
    }


    // =================================================
    // TASK TANPA DETAIL
    // =================================================

    private fun buildBasicTaskDetail(
        task: TaskEntity
    ): String {

        return buildString {

            task.description
                .takeIf {
                    it.isNotBlank()
                }
                ?.let {

                    append(it)

                    append(
                        "\n\n"
                    )
                }

            append(
                "Prioritas: "
            )

            append(
                getPriorityText(
                    task.priority
                )
            )

            append(
                "\n\nTap untuk membuka tugas."
            )
        }
    }


    // =================================================
    // JADWAL
    // =================================================

    private suspend fun checkTodaySchedules(
        database: MyTaskDatabase
    ) {

        val today =
            Calendar
                .getInstance()
                .get(
                    Calendar.DAY_OF_WEEK
                )

        val schedules =
            database
                .scheduleDao()
                .getSchedulesByDay(
                    today
                )
                .first()

        if (
            schedules.isEmpty()
        ) {

            return
        }

        val courses =
            database
                .courseDao()
                .getAllCourses()
                .first()

        schedules
            .sortedBy {
                it.startTime
            }
            .forEach { schedule ->

                val course =
                    courses.find {

                        it.id ==
                                schedule.courseId
                    }

                val courseName =
                    course?.name
                        ?: "Mata Kuliah"

                val message =
                    buildString {

                        append(
                            "${schedule.startTime} - " +
                                    schedule.endTime
                        )

                        append(
                            "\n$courseName"
                        )

                        if (
                            schedule.room
                                .isNotBlank()
                        ) {

                            append(
                                "\nRuangan: " +
                                        schedule.room
                            )
                        }

                        append(
                            "\n\nTap untuk konfirmasi."
                        )
                    }

                NotificationHelper
                    .showScheduleNotification(

                        applicationContext,

                        schedule.id.toString(),

                        "🕒 Jadwal Kuliah",

                        message
                    )
            }
    }


    // =================================================
    // RELATIVE DEADLINE
    // =================================================

    private fun getDeadlineText(
        now: Calendar,
        deadline: Date
    ): String {

        val today =
            Calendar
                .getInstance()
                .apply {

                    time =
                        now.time

                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )

                    set(
                        Calendar.MINUTE,
                        0
                    )

                    set(
                        Calendar.SECOND,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

        val target =
            Calendar
                .getInstance()
                .apply {

                    time =
                        deadline

                    set(
                        Calendar.HOUR_OF_DAY,
                        0
                    )

                    set(
                        Calendar.MINUTE,
                        0
                    )

                    set(
                        Calendar.SECOND,
                        0
                    )

                    set(
                        Calendar.MILLISECOND,
                        0
                    )
                }

        val difference =
            target.timeInMillis -
                    today.timeInMillis

        val days =
            TimeUnit.MILLISECONDS
                .toDays(
                    difference
                )

        return when {

            days < 0L -> {

                "terlambat ${-days} hari"
            }

            days == 0L -> {

                "hari ini"
            }

            days == 1L -> {

                "1 hari lagi"
            }

            else -> {

                "$days hari lagi"
            }
        }
    }


    // =================================================
    // JUMLAH HARI TERLAMBAT
    // =================================================

    private fun getOverdueDays(
        today: Calendar,
        deadline: Calendar
    ): Long {

        val difference =
            today.timeInMillis -
                    deadline.timeInMillis

        return TimeUnit.MILLISECONDS
            .toDays(
                difference
            )
            .coerceAtLeast(
                1L
            )
    }


    // =================================================
    // PRIORITY
    // =================================================

    private fun getPriorityText(
        priority: Int
    ): String {

        return when (priority) {

            1 ->
                "Tinggi"

            2 ->
                "Sedang"

            3 ->
                "Rendah"

            else ->
                "Normal"
        }
    }
}