package com.mytask

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mytask.navigation.NavGraph
import com.mytask.navigation.Screen
import com.mytask.ui.calendar.CalendarScreen
import com.mytask.ui.course.CourseListScreen
import com.mytask.ui.dashboard.DashboardScreen
import com.mytask.ui.loading.LoadingScreen
import com.mytask.ui.profile.ProfileScreen
import com.mytask.ui.schedule.ScheduleScreen
import com.mytask.ui.task.TaskListScreen
import com.mytask.ui.theme.MyTaskTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            // Tidak perlu melakukan apa-apa.
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        requestNotificationPermission()

        setContent {

            MyTaskTheme {

                MyTaskApp()
            }
        }
    }

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            val granted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) ==
                        PackageManager.PERMISSION_GRANTED

            if (!granted) {

                notificationPermissionLauncher
                    .launch(
                        Manifest.permission.POST_NOTIFICATIONS
                    )
            }
        }
    }
}


/*
 * =====================================================
 * ROOT APP
 * =====================================================
 */

@Composable
private fun MyTaskApp() {

    /*
     * Loading hanya ditampilkan pada
     * saat startup awal.
     *
     * UI utama belum dibuat selama loading.
     */
    var isLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {

        /*
         * Beri waktu Android/Compose melakukan
         * startup tanpa langsung menampilkan
         * UI utama yang masih berat.
         */
        delay(800)

        isLoading = false
    }

    if (isLoading) {

        LoadingScreen()

        return
    }

    /*
     * Setelah loading selesai,
     * baru buat seluruh aplikasi utama.
     */
    MyTaskMainContent()
}


/*
 * =====================================================
 * MAIN CONTENT
 * =====================================================
 */

@Composable
private fun MyTaskMainContent() {

    val navController =
        rememberNavController()

    val scope =
        rememberCoroutineScope()

    /*
     * =================================================
     * PAGER
     * =================================================
     *
     * 0 = Dashboard
     * 1 = Tugas
     * 2 = Jadwal
     * 3 = Kalender
     * 4 = Mata Kuliah
     * 5 = Profile
     */
    val pagerState =
        rememberPagerState(
            initialPage = 0,
            pageCount = {
                6
            }
        )

    val currentPage =
        pagerState.currentPage

    /*
     * =================================================
     * NAVGRAPH STATE
     * =================================================
     */

    val backStackEntry by
    navController
        .currentBackStackEntryAsState()

    val currentRoute =
        backStackEntry
            ?.destination
            ?.route

    val isSubScreen =
        currentRoute ==
                Screen.AddTask.route ||
                currentRoute ==
                Screen.AddCourse.route ||
                currentRoute ==
                Screen.NotificationSettings.route


    /*
     * =================================================
     * SCAFFOLD
     * =================================================
     */

    Scaffold(

        bottomBar = {

            if (!isSubScreen) {

                NavigationBar {

                    /*
                     * =================================
                     * DASHBOARD
                     * =================================
                     */

                    NavigationBarItem(

                        selected =
                            currentPage == 0,

                        onClick = {

                            scope.launch {

                                pagerState
                                    .animateScrollToPage(
                                        0
                                    )
                            }
                        },

                        icon = {

                            Icon(
                                Icons.Default.Dashboard,
                                contentDescription =
                                    "Dashboard"
                            )
                        },

                        alwaysShowLabel =
                            false
                    )


                    /*
                     * =================================
                     * TUGAS
                     * =================================
                     */

                    NavigationBarItem(

                        selected =
                            currentPage == 1,

                        onClick = {

                            scope.launch {

                                pagerState
                                    .animateScrollToPage(
                                        1
                                    )
                            }
                        },

                        icon = {

                            Icon(
                                Icons.Default.Task,
                                contentDescription =
                                    "Tugas"
                            )
                        },

                        alwaysShowLabel =
                            false
                    )


                    /*
                     * =================================
                     * JADWAL
                     * =================================
                     */

                    NavigationBarItem(

                        selected =
                            currentPage == 2,

                        onClick = {

                            scope.launch {

                                pagerState
                                    .animateScrollToPage(
                                        2
                                    )
                            }
                        },

                        icon = {

                            Icon(
                                Icons.Default.Schedule,
                                contentDescription =
                                    "Jadwal"
                            )
                        },

                        alwaysShowLabel =
                            false
                    )


                    /*
                     * =================================
                     * KALENDER
                     * =================================
                     */

                    NavigationBarItem(

                        selected =
                            currentPage == 3,

                        onClick = {

                            scope.launch {

                                pagerState
                                    .animateScrollToPage(
                                        3
                                    )
                            }
                        },

                        icon = {

                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription =
                                    "Kalender"
                            )
                        },

                        alwaysShowLabel =
                            false
                    )


                    /*
                     * =================================
                     * MATA KULIAH
                     * =================================
                     */

                    NavigationBarItem(

                        selected =
                            currentPage == 4,

                        onClick = {

                            scope.launch {

                                pagerState
                                    .animateScrollToPage(
                                        4
                                    )
                            }
                        },

                        icon = {

                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription =
                                    "Mata Kuliah"
                            )
                        },

                        alwaysShowLabel =
                            false
                    )


                    /*
                     * =================================
                     * PROFILE
                     * =================================
                     */

                    NavigationBarItem(

                        selected =
                            currentPage == 5,

                        onClick = {

                            scope.launch {

                                pagerState
                                    .animateScrollToPage(
                                        5
                                    )
                            }
                        },

                        icon = {

                            Icon(
                                Icons.Default.Person,
                                contentDescription =
                                    "Profile"
                            )
                        },

                        alwaysShowLabel =
                            false
                    )
                }
            }
        }

    ) { paddingValues ->

        Box(
            modifier =
                Modifier.fillMaxSize()
        ) {

            /*
             * =================================================
             * NAVGRAPH
             * =================================================
             *
             * SELALU aktif agar tombol Tambah/Edit
             * tidak force close.
             *
             * Saat idle:
             * berada di belakang pager.
             *
             * Saat edit:
             * berada di atas pager.
             */

            NavGraph(

                navController =
                    navController,

                paddingValues =
                    paddingValues,

                modifier =
                    Modifier
                        .fillMaxSize()
                        .zIndex(

                            if (
                                isSubScreen
                            ) {
                                10f
                            } else {
                                0f
                            }
                        )
            )


            /*
             * =================================================
             * HORIZONTAL PAGER
             * =================================================
             *
             * Tidak ditampilkan ketika Add/Edit.
             */
            if (!isSubScreen) {

                HorizontalPager(

                    state =
                        pagerState,

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                paddingValues
                            )
                            .zIndex(1f),

                    beyondViewportPageCount =
                        1

                ) { page ->

                    when (page) {

                        /*
                         * =====================================
                         * DASHBOARD
                         * =====================================
                         */

                        0 -> {

                            DashboardScreen(

                                onCoursesClick = {

                                    scope.launch {

                                        pagerState
                                            .animateScrollToPage(
                                                4
                                            )
                                    }
                                },

                                onTasksClick = {

                                    scope.launch {

                                        pagerState
                                            .animateScrollToPage(
                                                1
                                            )
                                    }
                                },

                                onScheduleClick = {

                                    scope.launch {

                                        pagerState
                                            .animateScrollToPage(
                                                2
                                            )
                                    }
                                },

                                onCalendarClick = {

                                    scope.launch {

                                        pagerState
                                            .animateScrollToPage(
                                                3
                                            )
                                    }
                                }
                            )
                        }


                        /*
                         * =====================================
                         * TUGAS
                         * =====================================
                         */

                        1 -> {

                            TaskListScreen(

                                onAddTask = {

                                    navController.navigate(
                                        "add_task?taskId=-1"
                                    )
                                },

                                onEditTask = { id ->

                                    navController.navigate(
                                        "add_task?taskId=$id"
                                    )
                                }
                            )
                        }


                        /*
                         * =====================================
                         * JADWAL
                         * =====================================
                         */

                        2 -> {

                            ScheduleScreen()
                        }


                        /*
                         * =====================================
                         * KALENDER
                         * =====================================
                         */

                        3 -> {

                            CalendarScreen(

                                onBack = {

                                    scope.launch {

                                        if (
                                            currentPage > 0
                                        ) {

                                            pagerState
                                                .animateScrollToPage(
                                                    currentPage - 1
                                                )
                                        }
                                    }
                                }
                            )
                        }


                        /*
                         * =====================================
                         * MATA KULIAH
                         * =====================================
                         */

                        4 -> {

                            CourseListScreen(

                                onAddCourse = {

                                    navController.navigate(
                                        "add_course?courseId=-1"
                                    )
                                },

                                onEditCourse = { id ->

                                    navController.navigate(
                                        "add_course?courseId=$id"
                                    )
                                }
                            )
                        }


                        /*
                         * =====================================
                         * PROFILE
                         * =====================================
                         */

                        5 -> {

                            ProfileScreen(

                                onBack = {

                                    scope.launch {

                                        if (
                                            currentPage > 0
                                        ) {

                                            pagerState
                                                .animateScrollToPage(
                                                    currentPage - 1
                                                )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}