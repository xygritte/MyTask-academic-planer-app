package com.mytask.Notification

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
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

        return try {

            NotificationHelper.createChannels(
                applicationContext
            )

            val tasks =
                database
                    .taskDao()
                    .getAllTasks()
                    .first()

            // 1. Semua tugas aktif
            checkActiveTasks(tasks)

            // 2. Tugas yang deadline hari ini
            checkTodayTasks(tasks)

            // 3. JADWAL
            // Tidak kita ubah logikanya
            checkTodaySchedules(database)

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
                    }
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
                            activeTasks.lastIndex
                        ) {

                            append("\n")
                        }
                    }
            }

        NotificationHelper
            .showActiveTasksNotification(
                applicationContext,
                message
            )
    }

    // =================================================
    // 2. TUGAS HARI INI
    // TIDAK BISA DI-SWIPE
    // =================================================

    private fun checkTodayTasks(
        tasks: List<TaskEntity>
    ) {

        val today =
            Calendar.getInstance()

        val todayDay =
            today.get(
                Calendar.DAY_OF_YEAR
            )

        val todayYear =
            today.get(
                Calendar.YEAR
            )

        tasks
            .filter { task ->

                val deadline =
                    task.deadline
                        ?: return@filter false

                val deadlineCalendar =
                    Calendar.getInstance()

                deadlineCalendar.time =
                    deadline

                deadlineCalendar.get(
                    Calendar.DAY_OF_YEAR
                ) == todayDay &&

                        deadlineCalendar.get(
                            Calendar.YEAR
                        ) == todayYear &&

                        !task.isCompleted
            }
            .forEach { task ->

                val detail =
                    buildTaskDetail(
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
    // DETAIL TUGAS
    // =================================================

    private fun buildTaskDetail(
        task: TaskEntity
    ): String {

        return buildString {

            task.description
                .takeIf {
                    it.isNotBlank()
                }
                ?.let {

                    append(it)
                    append("\n\n")
                }

            task.deadline?.let {

                val formatter =
                    SimpleDateFormat(
                        "dd MMM yyyy • HH:mm",
                        Locale("id")
                    )

                append(
                    "Deadline: ${formatter.format(it)}"
                )
            }

            append("\n")

            append(
                "Prioritas: "
            )

            append(
                when (task.priority) {

                    1 -> "Tinggi"
                    2 -> "Sedang"
                    3 -> "Rendah"
                    else -> "Normal"
                }
            )

            append(
                "\n\nTap untuk membuka tugas."
            )
        }
    }

    // =================================================
    // 3. JADWAL
    // BIARKAN SAMA
    // =================================================

    private suspend fun checkTodaySchedules(
        database: MyTaskDatabase
    ) {

        val today =
            Calendar.getInstance()
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
                            schedule.room.isNotBlank()
                        ) {

                            append(
                                "\nRuangan: ${schedule.room}"
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
    // DEADLINE X HARI
    // =================================================

    private fun getDeadlineText(
        now: Calendar,
        deadline: java.util.Date
    ): String {

        val today =
            Calendar.getInstance().apply {

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

        val difference =
            target.timeInMillis -
                    today.timeInMillis

        val days =
            TimeUnit.MILLISECONDS
                .toDays(
                    difference
                )

        return when {

            days < 0L ->
                "terlambat ${-days} hari"

            days == 0L ->
                "hari ini"

            days == 1L ->
                "1 hari"

            else ->
                "$days hari"
        }
    }
}