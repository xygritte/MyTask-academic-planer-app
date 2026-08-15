package com.mytask

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mytask.data.repository.UserProfile
import com.mytask.data.repository.UserProfileRepository
import com.mytask.navigation.NavGraph
import com.mytask.navigation.Screen
import com.mytask.ui.calendar.CalendarScreen
import com.mytask.ui.course.CourseListScreen
import com.mytask.ui.dashboard.DashboardScreen
import com.mytask.ui.loading.LoadingScreen
import com.mytask.ui.login.LoginScreen
import com.mytask.ui.profile.ProfileScreen
import com.mytask.ui.schedule.ScheduleScreen
import com.mytask.ui.task.TaskListScreen
import com.mytask.ui.theme.MyTaskTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.produceState

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
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        setContent {
            MyTaskTheme {
                MyTaskApp()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted =
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (!granted) {
                notificationPermissionLauncher.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            }
        }
    }
}

@Composable
private fun MyTaskApp() {

    val context = LocalContext.current.applicationContext

    val repository = remember(context) {
        UserProfileRepository(context)
    }

    val profileState = produceState<Pair<Boolean, UserProfile?>>(
        initialValue = false to null,
        key1 = repository
    ) {
        repository.profile.collectLatest { profile ->
            value = true to profile
        }
    }

    var minimumLoading by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        delay(800)
        minimumLoading = false
    }

    val profileLoaded = profileState.value.first
    val profile = profileState.value.second

    if (minimumLoading || !profileLoaded) {
        LoadingScreen()
        return
    }

    if (profile == null) {
        LoginScreen(repository = repository)
        return
    }

    MyTaskMainContent(
        profile = profile,
        repository = repository
    )
}

@Composable
private fun MyTaskMainContent(
    profile: UserProfile,
    repository: UserProfileRepository
) {

    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 6 }
    )

    val currentPage = pagerState.currentPage

    val backStackEntry by
        navController.currentBackStackEntryAsState()

    val currentRoute =
        backStackEntry?.destination?.route

    val isSubScreen =
        currentRoute == Screen.AddTask.route ||
        currentRoute == Screen.AddCourse.route ||
        currentRoute == Screen.NotificationSettings.route ||
        currentRoute == Screen.Backup.route

    Scaffold(
        bottomBar = {
            if (!isSubScreen) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentPage == 0,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(0)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Dashboard,
                                contentDescription = "Dashboard"
                            )
                        },
                        alwaysShowLabel = false
                    )

                    NavigationBarItem(
                        selected = currentPage == 1,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Task,
                                contentDescription = "Tugas"
                            )
                        },
                        alwaysShowLabel = false
                    )

                    NavigationBarItem(
                        selected = currentPage == 2,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(2)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Jadwal"
                            )
                        },
                        alwaysShowLabel = false
                    )

                    NavigationBarItem(
                        selected = currentPage == 3,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Kalender"
                            )
                        },
                        alwaysShowLabel = false
                    )

                    NavigationBarItem(
                        selected = currentPage == 4,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(4)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.MenuBook,
                                contentDescription = "Mata Kuliah"
                            )
                        },
                        alwaysShowLabel = false
                    )

                    NavigationBarItem(
                        selected = currentPage == 5,
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(5)
                            }
                        },
                        icon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Profile"
                            )
                        },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {

            NavGraph(
                navController = navController,
                paddingValues = paddingValues,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (isSubScreen) 10f else 0f)
            )

            if (!isSubScreen) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .zIndex(1f),
                    beyondViewportPageCount = 1
                ) { page ->

                    when (page) {

                        0 -> {
                            DashboardScreen(
                                onCoursesClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(4)
                                    }
                                },
                                onTasksClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(1)
                                    }
                                },
                                onScheduleClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(2)
                                    }
                                },
                                onCalendarClick = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(3)
                                    }
                                }
                            )
                        }

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

                        2 -> {
                            ScheduleScreen()
                        }

                        3 -> {
                            CalendarScreen(
                                onBack = {
                                    scope.launch {
                                        if (currentPage > 0) {
                                            pagerState.animateScrollToPage(
                                                currentPage - 1
                                            )
                                        }
                                    }
                                }
                            )
                        }

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

                        5 -> {
                            ProfileScreen(
                                profile = profile,
                                onBack = {
                                    scope.launch {
                                        if (currentPage > 0) {
                                            pagerState.animateScrollToPage(
                                                currentPage - 1
                                            )
                                        }
                                    }
                                },
                                onNotificationSettings = {
                                    navController.navigate(
                                        Screen.NotificationSettings.route
                                    )
                                },
                                onBackupData = {
                                    navController.navigate(
                                        Screen.Backup.route
                                    )
                                },
                                onEditProfile = {
                                    scope.launch {
                                        repository.clearProfile()
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
