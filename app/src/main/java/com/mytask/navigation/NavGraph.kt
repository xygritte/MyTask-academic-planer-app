package com.mytask.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.mytask.ui.course.AddEditCourseScreen
import com.mytask.ui.profile.BackupScreen
import com.mytask.ui.profile.NotificationSettingsScreen
import com.mytask.ui.task.AddEditTaskScreen

@Composable
fun NavGraph(

    navController:
    NavHostController,

    paddingValues:
    PaddingValues,

    modifier:
    Modifier = Modifier

) {

    NavHost(

        navController =
            navController,

        startDestination =
            "navigation_idle",

        modifier =
            modifier
                .fillMaxSize()
                .padding(
                    paddingValues
                )

    ) {

        /*
         * =========================================
         * NOTIFICATION SETTINGS
         * =========================================
         */

        composable(

            route =
                Screen.NotificationSettings.route

        ) {

            NotificationSettingsScreen(

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        }


        /*
         * =========================================
         * BACKUP & DATA
         * =========================================
         */

        composable(

            route =
                Screen.Backup.route

        ) {

            BackupScreen(

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        }


        /*
         * =========================================
         * IDLE
         * =========================================
         */

        composable(

            "navigation_idle"

        ) {

            Box(

                modifier =
                    Modifier.fillMaxSize()
            )
        }


        /*
         * =========================================
         * ADD / EDIT TUGAS
         * =========================================
         */

        composable(

            route =
                Screen.AddTask.route,

            arguments =
                listOf(

                    navArgument(
                        "taskId"
                    ) {

                        type =
                            NavType.LongType

                        defaultValue =
                            -1L
                    }
                )

        ) { entry ->

            val taskId =

                entry
                    .arguments
                    ?.getLong(
                        "taskId"
                    )
                    ?.takeIf {

                        it != -1L
                    }

            AddEditTaskScreen(

                taskId =
                    taskId,

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        }


        /*
         * =========================================
         * ADD / EDIT MATA KULIAH
         * =========================================
         */

        composable(

            route =
                Screen.AddCourse.route,

            arguments =
                listOf(

                    navArgument(
                        "courseId"
                    ) {

                        type =
                            NavType.LongType

                        defaultValue =
                            -1L
                    }
                )

        ) { entry ->

            val courseId =

                entry
                    .arguments
                    ?.getLong(
                        "courseId"
                    )
                    ?.takeIf {

                        it != -1L
                    }

            AddEditCourseScreen(

                courseId =
                    courseId,

                onBack = {

                    navController
                        .popBackStack()
                }
            )
        }
    }
}