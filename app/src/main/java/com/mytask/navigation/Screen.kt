package com.mytask.navigation

sealed class Screen(
    val route: String
) {
    data object Dashboard :
        Screen("dashboard")

    data object Task :
        Screen("task")

    data object AddTask :
        Screen("add_task?taskId={taskId}")

    data object Schedule :
        Screen("schedule")

    data object Calendar :
        Screen("calendar")

    data object Course :
        Screen("course")

    data object AddCourse :
        Screen("add_course?courseId={courseId}")

    data object Profile :
        Screen("profile")


    data object NotificationSettings :
        Screen("notification_settings")
}