package com.mytask.Notification

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
             *
             * Bisa diaktifkan / dinonaktifkan
             * dari Profile > Notifikasi.
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
             * 2. PENGINGAT DEADLINE
             * ==========================================
             *
             * Aturan:
             *
             * reminderDays = 0
             * → mulai pada hari deadline
             *
             * reminderDays = 1
             * → mulai H-1
             *
             * reminderDays = 3
             * → mulai H-3
             *
             * dan seterusnya.
             *
             * Setelah melewati deadline,
             * notifikasi tetap ada selama
             * tugas belum selesai.
             */

            checkDeadlineTasks(
                tasks = tasks,
                reminderDays = reminderDays
            )

            /*
             * ==========================================
             * 3. JADWAL KULIAH
             * ==========================================
             *
             * Logikanya tetap seperti sebelumnya.
             */

            checkTodaySchedules(
                database
            )

            /*
             * ==========================================
             * JADWAL MIDNIGHT BERIKUTNYA
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
    // 1. SEMUA TUGAS AKTIF
    // SATU NOTIFIKASI
    // BISA DI-SWIPE
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

        val message =
            buildString {

                activeTasks
                    .sortedBy {
                        it.deadline
                            ?: Date(Long.MAX_VALUE)
                    }
                    .take(4)
                    .forEachIndexed {
                            index,
                            task ->

                        append("• ")
                        append(task.title)

                        val deadline =
                            task.deadline

                        if (
                            deadline != null
                        ) {

                            append(
                                " — deadline "
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
                            activeTasks
                                .take(4)
                                .lastIndex
                        ) {

                            append("\n")
                        }
                    }

                if (
                    activeTasks.size > 4
                ) {

                    append(
                        "\n+ ${activeTasks.size - 4} tugas lainnya"
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
    // 2. PENGINGAT DEADLINE
    //
    // MULAI H-X
    // TETAP ADA SETELAH DEADLINE
    // SAMPAI TUGAS SELESAI
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

        /*
         * Semua task memiliki notification state
         * yang perlu kita sinkronkan.
         *
         * Task:
         *
         * - selesai
         * - tidak punya deadline
         * - deadline masih terlalu jauh
         *
         * harus dipastikan tidak meninggalkan
         * notifikasi permanen lama.
         */

        tasks.forEach { task ->

            val deadline =
                task.deadline

            /*
             * ==========================================
             * TASK SUDAH SELESAI
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
             * TASK TIDAK MEMILIKI DEADLINE
             * ==========================================
             */

            if (
                deadline == null
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
             * APAKAH SUDAH MASUK WINDOW REMINDER?
             * ==========================================
             *
             * Contoh:
             *
             * today = 15
             * reminderDays = 3
             *
             * reminderStart = 18
             *
             * deadline:
             *
             * 17 → belum tampil
             * 18 → tampil
             * 19 → tampil
             * 20 → tampil
             * 10 → tampil karena sudah lewat deadline
             */

            val shouldShow =
                deadlineDay.timeInMillis <=
                        reminderStart.timeInMillis

            if (
                shouldShow
            ) {

                val detail =
                    buildDeadlineTaskDetail(
                        task
                    )

                NotificationHelper
                    .showTaskNotification(

                        applicationContext,

                        task.id.toString(),

                        "📝 ${task.title}",

                        detail
                    )

            } else {

                /*
                 * Deadline masih terlalu jauh.
                 *
                 * Hapus notifikasi lama jika sebelumnya
                 * user pernah menggunakan reminderDays
                 * yang lebih besar.
                 */

                NotificationHelper
                    .cancelTaskNotification(
                        applicationContext,
                        task.id.toString()
                    )
            }
        }
    }


    // =================================================
    // DETAIL NOTIFIKASI DEADLINE
    // =================================================

    private fun buildDeadlineTaskDetail(
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

            task.deadline?.let {

                val formatter =
                    SimpleDateFormat(
                        "dd MMM yyyy • HH:mm",
                        Locale(
                            "id",
                            "ID"
                        )
                    )

                append(
                    "Deadline: "
                )

                append(
                    formatter.format(
                        it
                    )
                )

                append(
                    "\n"
                )

                append(
                    "Status: "
                )

                append(
                    getDeadlineText(
                        Calendar
                            .getInstance(),
                        it
                    )
                )
            }

            append(
                "\n\nPrioritas: "
            )

            append(

                when (
                    task.priority
                ) {

                    1 ->
                        "Tinggi"

                    2 ->
                        "Sedang"

                    3 ->
                        "Rendah"

                    else ->
                        "Normal"
                }
            )

            append(
                "\n\nTap untuk membuka tugas."
            )
        }
    }


    // =================================================
    // 3. JADWAL KULIAH
    // LOGIKA TETAP
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
                            "${schedule.startTime} - ${schedule.endTime}"
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
    // DEADLINE TEXT
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
}