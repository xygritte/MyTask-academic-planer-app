package com.mytask.data.repository

import androidx.room.withTransaction
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    private val database: MyTaskDatabase
) {

    suspend fun exportData(): String {

        val courses =
            database.courseDao()
                .getAllCoursesSnapshot()

        val tasks =
            database.taskDao()
                .getAllTasksSnapshot()

        val schedules =
            database.scheduleDao()
                .getAllSchedulesSnapshot()

        val root = JSONObject()

        root.put(
            "app",
            "MyTask"
        )

        root.put(
            "version",
            1
        )

        root.put(
            "createdAt",
            System.currentTimeMillis()
        )

        // ==========================================
        // COURSES
        // ==========================================

        val courseArray = JSONArray()

        courses.forEach { course ->

            courseArray.put(
                JSONObject().apply {

                    put(
                        "id",
                        course.id
                    )

                    put(
                        "name",
                        course.name
                    )

                    put(
                        "code",
                        course.code
                    )

                    put(
                        "lecturer",
                        course.lecturer
                    )

                    put(
                        "room",
                        course.room
                    )
                }
            )
        }

        root.put(
            "courses",
            courseArray
        )

        // ==========================================
        // TASKS
        // ==========================================

        val taskArray = JSONArray()

        tasks.forEach { task ->

            taskArray.put(
                JSONObject().apply {

                    put(
                        "id",
                        task.id
                    )

                    if (task.courseId != null) {

                        put(
                            "courseId",
                            task.courseId
                        )
                    } else {

                        put(
                            "courseId",
                            JSONObject.NULL
                        )
                    }

                    put(
                        "title",
                        task.title
                    )

                    put(
                        "description",
                        task.description
                    )

                    if (task.deadline != null) {

                        put(
                            "deadline",
                            task.deadline.time
                        )
                    } else {

                        put(
                            "deadline",
                            JSONObject.NULL
                        )
                    }

                    put(
                        "priority",
                        task.priority
                    )

                    put(
                        "isCompleted",
                        task.isCompleted
                    )
                }
            )
        }

        root.put(
            "tasks",
            taskArray
        )

        // ==========================================
        // SCHEDULES
        // ==========================================

        val scheduleArray = JSONArray()

        schedules.forEach { schedule ->

            scheduleArray.put(
                JSONObject().apply {

                    put(
                        "id",
                        schedule.id
                    )

                    if (schedule.courseId != null) {

                        put(
                            "courseId",
                            schedule.courseId
                        )
                    } else {

                        put(
                            "courseId",
                            JSONObject.NULL
                        )
                    }

                    put(
                        "dayOfWeek",
                        schedule.dayOfWeek
                    )

                    put(
                        "startTime",
                        schedule.startTime
                    )

                    put(
                        "endTime",
                        schedule.endTime
                    )

                    put(
                        "room",
                        schedule.room
                    )
                }
            )
        }

        root.put(
            "schedules",
            scheduleArray
        )

        return root.toString(2)
    }

    suspend fun importData(
        json: String
    ) {

        val root =
            JSONObject(json)

        val version =
            root.optInt(
                "version",
                0
            )

        require(
            root.optString("app") == "MyTask"
        ) {
            "File bukan backup MyTask."
        }

        require(
            version == 1
        ) {
            "Versi backup tidak didukung."
        }

        // ==========================================
        // PARSE COURSES
        // ==========================================

        val courses =
            mutableListOf<CourseEntity>()

        val courseArray =
            root.optJSONArray(
                "courses"
            )
                ?: JSONArray()

        for (
        index in 0 until courseArray.length()
        ) {

            val item =
                courseArray.getJSONObject(index)

            courses.add(

                CourseEntity(

                    id =
                        item.optLong("id"),

                    name =
                        item.optString("name"),

                    code =
                        item.optString("code"),

                    lecturer =
                        item.optString("lecturer"),

                    room =
                        item.optString("room")
                )
            )
        }

        // ==========================================
        // PARSE TASKS
        // ==========================================

        val tasks =
            mutableListOf<TaskEntity>()

        val taskArray =
            root.optJSONArray(
                "tasks"
            )
                ?: JSONArray()

        for (
        index in 0 until taskArray.length()
        ) {

            val item =
                taskArray.getJSONObject(index)

            val courseId =
                if (
                    item.isNull("courseId")
                ) {
                    null
                } else {
                    item.optLong("courseId")
                }

            val deadline =
                if (
                    item.isNull("deadline")
                ) {
                    null
                } else {
                    Date(
                        item.optLong("deadline")
                    )
                }

            tasks.add(

                TaskEntity(

                    id =
                        item.optLong("id"),

                    courseId =
                        courseId,

                    title =
                        item.optString("title"),

                    description =
                        item.optString(
                            "description"
                        ),

                    deadline =
                        deadline,

                    priority =
                        item.optInt(
                            "priority",
                            1
                        ),

                    isCompleted =
                        item.optBoolean(
                            "isCompleted",
                            false
                        )
                )
            )
        }

        // ==========================================
        // PARSE SCHEDULES
        // ==========================================

        val schedules =
            mutableListOf<ScheduleEntity>()

        val scheduleArray =
            root.optJSONArray(
                "schedules"
            )
                ?: JSONArray()

        for (
        index in 0 until scheduleArray.length()
        ) {

            val item =
                scheduleArray.getJSONObject(index)

            val courseId =
                if (
                    item.isNull("courseId")
                ) {
                    null
                } else {
                    item.optLong("courseId")
                }

            schedules.add(

                ScheduleEntity(

                    id =
                        item.optLong("id"),

                    courseId =
                        courseId,

                    dayOfWeek =
                        item.optInt(
                            "dayOfWeek"
                        ),

                    startTime =
                        item.optString(
                            "startTime"
                        ),

                    endTime =
                        item.optString(
                            "endTime"
                        ),

                    room =
                        item.optString(
                            "room"
                        )
                )
            )
        }

        // ==========================================
        // REPLACE DATABASE
        // ==========================================

        database.withTransaction {

            // Hapus data lama.
            database.scheduleDao()
                .deleteAll()

            database.taskDao()
                .deleteAll()

            database.courseDao()
                .deleteAll()

            // Masukkan data backup.
            database.courseDao()
                .insertAll(courses)

            database.taskDao()
                .insertAll(tasks)

            database.scheduleDao()
                .insertAll(schedules)
        }
    }
}