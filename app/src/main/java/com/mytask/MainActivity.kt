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
import androidx.compose.runtime.collectAsState
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
import com.mytask.data.repository.CloudDataSyncRepository
import com.mytask.data.repository.FirebaseAuthRepository
import com.mytask.data.repository.TemplateDataImporter
import com.mytask.data.repository.TemplatePreferenceRepository
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
import com.mytask.ui.template.AcademicTemplateDialog
import com.mytask.ui.theme.MyTaskTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firebaseAuthRepository: FirebaseAuthRepository

    @Inject
    lateinit var templateDataImporter: TemplateDataImporter

    @Inject
    lateinit var cloudDataSyncRepository: CloudDataSyncRepository

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()

        setContent {
            MyTaskTheme {
                MyTaskApp(
                    authRepository = firebaseAuthRepository,
                    templateDataImporter = templateDataImporter,
                    cloudDataSyncRepository = cloudDataSyncRepository
                )
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
private fun MyTaskApp(
    authRepository: FirebaseAuthRepository,
    templateDataImporter: TemplateDataImporter,
    cloudDataSyncRepository: CloudDataSyncRepository
) {
    val context = LocalContext.current.applicationContext

    val userProfileRepository = remember(context) {
        UserProfileRepository(context)
    }

    val templatePreferenceRepository = remember(context) {
        TemplatePreferenceRepository(context)
    }

    val firebaseUser by authRepository.authState.collectAsState(
        initial = authRepository.currentUser
    )

    val localProfile by userProfileRepository.profile.collectAsState(
        initial = null
    )

    val currentLocalProfile = localProfile
    val currentFirebaseUser = firebaseUser

    var sessionProfile by remember {
        mutableStateOf<UserProfile?>(null)
    }

    var sessionUid by remember {
        mutableStateOf<String?>(null)
    }

    var accountLoading by remember {
        mutableStateOf(false)
    }

    var syncReady by remember {
        mutableStateOf(false)
    }

    var minimumLoading by remember {
        mutableStateOf(true)
    }

    var isApplyingTemplate by remember {
        mutableStateOf(false)
    }

    var templateError by remember {
        mutableStateOf<String?>(null)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(800)
        minimumLoading = false
    }

    // Recover the locally persisted session first. This prevents the UI from
    // flashing back to Login while Firebase/AuthState or DataStore is settling.
    LaunchedEffect(currentLocalProfile) {
        val profile = currentLocalProfile
        val uid = runCatching {
            userProfileRepository.uid.first()
        }.getOrNull()

        if (profile != null && uid != null) {
            sessionProfile = profile
            sessionUid = uid
        }
    }

    LaunchedEffect(currentFirebaseUser?.uid) {
        val user = currentFirebaseUser

        accountLoading = user != null
        syncReady = false

        if (user != null) {
            val cachedProfile = currentLocalProfile
            val cachedUid = runCatching {
                userProfileRepository.uid.first()
            }.getOrNull()

            val immediateProfile =
                cachedProfile?.takeIf { cachedUid == user.uid }
                    ?: UserProfile(
                        name = user.displayName
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }
                            ?: "Mahasiswa",
                        program = "Program Studi belum diatur"
                    )

            // Latch the authenticated session immediately. Do not clear this
            // merely because AuthState emits a transient null during startup.
            sessionProfile = immediateProfile
            sessionUid = user.uid
            accountLoading = false

            launch {
                authRepository.reloadProfile()
                    .onSuccess { refreshedProfile ->
                        sessionProfile = refreshedProfile
                    }
            }

            launch {
                runCatching {
                    cloudDataSyncRepository.syncOnLogin(user.uid)
                }
                syncReady = true
            }
        }
    }

    LaunchedEffect(currentFirebaseUser?.uid, syncReady) {
        val user = currentFirebaseUser

        if (user == null || !syncReady) {
            return@LaunchedEffect
        }

        cloudDataSyncRepository.databaseJson
            .debounce(1200)
            .collectLatest { json ->
                runCatching {
                    cloudDataSyncRepository.uploadJson(
                        uid = user.uid,
                        json = json
                    )
                }
            }
    }

    if (minimumLoading) {
        LoadingScreen()
        return
    }

    val activeProfile = sessionProfile

    if (activeProfile != null) {
        val templateUid = sessionUid ?: currentFirebaseUser?.uid ?: "guest"

        val promptFlow = remember(templateUid) {
            templatePreferenceRepository.promptShown(templateUid)
        }

        val templatePromptShown by promptFlow.collectAsState(
            initial = false
        )

        Box(modifier = Modifier.fillMaxSize()) {
            MyTaskMainContent(
                profile = activeProfile,
                authRepository = authRepository,
                onLoggedOut = {
                    sessionProfile = null
                    sessionUid = null
                }
            )

            // Template is only shown after a session has been established.
            if (!templatePromptShown && !accountLoading) {
                AcademicTemplateDialog(
                    isApplying = isApplyingTemplate,
                    errorMessage = templateError,
                    onSkip = {
                        if (!isApplyingTemplate) {
                            scope.launch {
                                templatePreferenceRepository
                                    .markPromptShown(templateUid)
                            }
                        }
                    },
                    onApply = {
                        if (!isApplyingTemplate) {
                            scope.launch {
                                isApplyingTemplate = true
                                templateError = null

                                runCatching {
                                    templateDataImporter.importTemplate()

                                    val user = currentFirebaseUser
                                    if (user != null) {
                                        cloudDataSyncRepository.uploadCurrentData(
                                            user.uid
                                        )
                                    }
                                }.onSuccess {
                                    templatePreferenceRepository
                                        .markPromptShown(templateUid)
                                }.onFailure { error ->
                                    templateError =
                                        error.message
                                            ?: "Template gagal diterapkan."
                                }

                                isApplyingTemplate = false
                            }
                        }
                    }
                )
            }
        }
        return
    }

    // Only show Login when there is genuinely no persisted session/profile.
    LoginScreen(
        authRepository = authRepository
    )
}

@Composable
private fun MyTaskMainContent(
    profile: UserProfile,
    authRepository: FirebaseAuthRepository,
    onLoggedOut: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { 6 }
    )

    val currentPage = pagerState.currentPage
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

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
                            scope.launch { pagerState.animateScrollToPage(0) }
                        },
                        icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = currentPage == 1,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(1) }
                        },
                        icon = { Icon(Icons.Default.Task, "Tugas") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = currentPage == 2,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(2) }
                        },
                        icon = { Icon(Icons.Default.Schedule, "Jadwal") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = currentPage == 3,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(3) }
                        },
                        icon = { Icon(Icons.Default.CalendarMonth, "Kalender") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = currentPage == 4,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(4) }
                        },
                        icon = { Icon(Icons.Default.MenuBook, "Mata Kuliah") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = currentPage == 5,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(5) }
                        },
                        icon = { Icon(Icons.Default.Person, "Profile") },
                        alwaysShowLabel = false
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(Modifier.fillMaxSize()) {
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
                        0 -> DashboardScreen(
                            onCoursesClick = {
                                scope.launch { pagerState.animateScrollToPage(4) }
                            },
                            onTasksClick = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onScheduleClick = {
                                scope.launch { pagerState.animateScrollToPage(2) }
                            },
                            onCalendarClick = {
                                scope.launch { pagerState.animateScrollToPage(3) }
                            }
                        )

                        1 -> TaskListScreen(
                            onAddTask = {
                                navController.navigate("add_task?taskId=-1")
                            },
                            onEditTask = { id ->
                                navController.navigate("add_task?taskId=$id")
                            }
                        )

                        2 -> ScheduleScreen()

                        3 -> CalendarScreen(
                            onBack = {
                                scope.launch {
                                    if (currentPage > 0) {
                                        pagerState.animateScrollToPage(currentPage - 1)
                                    }
                                }
                            }
                        )

                        4 -> CourseListScreen(
                            onAddCourse = {
                                navController.navigate("add_course?courseId=-1")
                            },
                            onEditCourse = { id ->
                                navController.navigate("add_course?courseId=$id")
                            }
                        )

                        5 -> ProfileScreen(
                            profile = profile,
                            onBack = {
                                scope.launch {
                                    if (currentPage > 0) {
                                        pagerState.animateScrollToPage(currentPage - 1)
                                    }
                                }
                            },
                            onNotificationSettings = {
                                navController.navigate(Screen.NotificationSettings.route)
                            },
                            onBackupData = {
                                navController.navigate(Screen.Backup.route)
                            },
                            onEditProfile = {},
                            onLogout = {
                                scope.launch {
                                    authRepository.clearLocalSession()
                                    onLoggedOut()
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
