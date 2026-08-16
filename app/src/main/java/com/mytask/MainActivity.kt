package com.mytask

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.mytask.ui.add.AddDataDialog
import com.mytask.ui.calendar.CalendarScreen
import com.mytask.ui.course.CourseListScreen
import com.mytask.ui.course.CourseViewModel
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

    val restorePending by userProfileRepository.restorePending.collectAsState(
        initial = false
    )

    val currentFirebaseUser = firebaseUser
    val currentLocalProfile = localProfile

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

    var isSavingOnline by remember {
        mutableStateOf(false)
    }

    var onlineSaveMessage by remember {
        mutableStateOf<String?>(null)
    }

    var shouldShowTemplatePrompt by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(500)
        minimumLoading = false
    }

    LaunchedEffect(currentLocalProfile) {
        val profile = currentLocalProfile
        val uid = runCatching {
            userProfileRepository.uid.first()
        }.getOrNull()

        if (profile != null && uid != null) {
            sessionProfile = profile
            sessionUid = uid

            if (uid == "guest" && currentFirebaseUser == null) {
                syncReady = true

                val guestPromptShown = runCatching {
                    templatePreferenceRepository
                        .promptShown("guest")
                        .first()
                }.getOrDefault(false)

                shouldShowTemplatePrompt = !guestPromptShown
            }
        }
    }

    LaunchedEffect(currentFirebaseUser?.uid, restorePending) {
        val user = currentFirebaseUser

        if (user == null) {
            accountLoading = false
            if (sessionUid == "guest") {
                syncReady = true
            }
            return@LaunchedEffect
        }

        accountLoading = true
        syncReady = false
        onlineSaveMessage = null
        shouldShowTemplatePrompt = false

        val cachedUid = runCatching {
            userProfileRepository.uid.first()
        }.getOrNull()

        val immediateProfile =
            currentLocalProfile?.takeIf { cachedUid == user.uid }
                ?: UserProfile(
                    name = user.displayName
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: "Mahasiswa",
                    program = "Program Studi belum diatur"
                )

        sessionProfile = immediateProfile
        sessionUid = user.uid

        if (restorePending) {
            scope.launch {
                accountLoading = true
                templateError = null

                val cloudDataExists = runCatching {
                    cloudDataSyncRepository.syncOnLogin(user.uid)
                }.onSuccess {
                    userProfileRepository.clearCloudRestorePending()
                }.onFailure { error ->
                    templateError =
                        error.message
                            ?: "Data online belum dapat dimuat."
                }.getOrDefault(false)

                if (!cloudDataExists) {
                    val promptShown = runCatching {
                        templatePreferenceRepository
                            .promptShown(user.uid)
                            .first()
                    }.getOrDefault(false)

                    shouldShowTemplatePrompt = !promptShown
                } else {
                    shouldShowTemplatePrompt = false
                }

                syncReady = true
                accountLoading = false
            }
        } else {
            syncReady = true
            accountLoading = false
            shouldShowTemplatePrompt = false
        }
    }

    if (minimumLoading) {
        LoadingScreen("Memulai MyTask...")
        return
    }

    val activeProfile = sessionProfile ?: currentLocalProfile

    val isGuestSession =
        sessionUid == "guest" && currentFirebaseUser == null

    val workspaceReady =
        syncReady || isGuestSession

    if (activeProfile != null && workspaceReady) {
        val templateUid =
            sessionUid ?: currentFirebaseUser?.uid ?: "guest"

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            MyTaskMainContent(
                profile = activeProfile,
                canSaveOnline = currentFirebaseUser != null,
                isSavingOnline = isSavingOnline,
                onlineSaveMessage = onlineSaveMessage,
                authRepository = authRepository,
                onSaveDataOnline = {
                    val user = currentFirebaseUser

                    if (user != null && !isSavingOnline) {
                        scope.launch {
                            isSavingOnline = true
                            onlineSaveMessage = null

                            runCatching {
                                cloudDataSyncRepository
                                    .uploadCurrentData(user.uid)
                            }
                                .onSuccess {
                                    onlineSaveMessage =
                                        "Data berhasil disimpan ke online."
                                }
                                .onFailure { error ->
                                    onlineSaveMessage =
                                        error.message
                                            ?: "Gagal menyimpan data ke online."
                                }

                            isSavingOnline = false
                        }
                    }
                },
                onLoggedOut = {
                    sessionProfile = null
                    sessionUid = null
                    syncReady = false
                    accountLoading = false
                    onlineSaveMessage = null
                    shouldShowTemplatePrompt = false
                }
            )

            if (shouldShowTemplatePrompt && !accountLoading) {
                AcademicTemplateDialog(
                    isApplying = isApplyingTemplate,
                    errorMessage = templateError,
                    onSkip = {
                        if (!isApplyingTemplate) {
                            scope.launch {
                                templatePreferenceRepository
                                    .markPromptShown(templateUid)
                                shouldShowTemplatePrompt = false
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
                                }
                                    .onSuccess {
                                        templatePreferenceRepository
                                            .markPromptShown(templateUid)
                                        shouldShowTemplatePrompt = false
                                    }
                                    .onFailure { error ->
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

    if (currentFirebaseUser != null) {
        LoadingScreen(
            if (restorePending) {
                "Menyiapkan akun dan memulihkan data..."
            } else {
                "Menyiapkan akun..."
            }
        )
        return
    }

    if (currentLocalProfile != null) {
        MyTaskMainContent(
            profile = currentLocalProfile,
            canSaveOnline = false,
            isSavingOnline = false,
            onlineSaveMessage = null,
            authRepository = authRepository,
            onSaveDataOnline = {},
            onLoggedOut = {
                sessionProfile = null
                sessionUid = null
                syncReady = false
                shouldShowTemplatePrompt = false
            }
        )
        return
    }

    LoginScreen(
        authRepository = authRepository
    )
}

@Composable
private fun MyTaskMainContent(
    profile: UserProfile,
    canSaveOnline: Boolean,
    isSavingOnline: Boolean,
    onlineSaveMessage: String?,
    authRepository: FirebaseAuthRepository,
    onSaveDataOnline: () -> Unit,
    onLoggedOut: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val courseViewModel: CourseViewModel = hiltViewModel()
    val courses by courseViewModel.courses.collectAsState()
    val hasCourses = courses.isNotEmpty()

    var showAddDataDialog by remember {
        mutableStateOf(false)
    }

    var scheduleAddRequestKey by remember {
        mutableStateOf(0)
    }

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

    fun openAddDataDialog() {
        showAddDataDialog = true
    }

    fun openTasks() {
        if (hasCourses) {
            scope.launch { pagerState.animateScrollToPage(1) }
        } else {
            openAddDataDialog()
        }
    }

    fun openSchedule() {
        if (hasCourses) {
            scope.launch { pagerState.animateScrollToPage(2) }
        } else {
            openAddDataDialog()
        }
    }

    fun openAddTask() {
        showAddDataDialog = false
        if (hasCourses) {
            navController.navigate("add_task?taskId=-1")
        }
    }

    fun openAddCourse() {
        showAddDataDialog = false
        navController.navigate("add_course?courseId=-1")
    }

    fun openAddSchedule() {
        showAddDataDialog = false
        if (hasCourses) {
            scheduleAddRequestKey += 1
            scope.launch { pagerState.animateScrollToPage(2) }
        }
    }

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
                        onClick = ::openTasks,
                        icon = { Icon(Icons.Default.Task, "Tugas") },
                        alwaysShowLabel = false
                    )
                    NavigationBarItem(
                        selected = currentPage == 2,
                        onClick = ::openSchedule,
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
                    userScrollEnabled = hasCourses,
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
                            onTasksClick = ::openTasks,
                            onScheduleClick = ::openSchedule,
                            onCalendarClick = {
                                scope.launch { pagerState.animateScrollToPage(3) }
                            }
                        )
                        1 -> TaskListScreen(
                            onAddTask = ::openAddDataDialog,
                            onEditTask = { id ->
                                if (hasCourses) {
                                    navController.navigate("add_task?taskId=$id")
                                } else {
                                    openAddDataDialog()
                                }
                            }
                        )
                        2 -> ScheduleScreen(
                            addRequestKey = scheduleAddRequestKey,
                            onAddData = ::openAddDataDialog
                        )
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
                            onAddCourse = ::openAddDataDialog,
                            onEditCourse = { id ->
                                navController.navigate("add_course?courseId=$id")
                            }
                        )
                        5 -> ProfileScreen(
                            profile = profile,
                            canSaveOnline = canSaveOnline,
                            isSavingOnline = isSavingOnline,
                            onlineSaveMessage = onlineSaveMessage,
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
                            onSaveDataOnline = onSaveDataOnline,
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

            if (!isSubScreen && currentPage == 0) {
                FloatingActionButton(
                    onClick = ::openAddDataDialog,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Tambah Data"
                    )
                }
            }
        }

        if (showAddDataDialog) {
            AddDataDialog(
                hasCourses = hasCourses,
                onDismiss = { showAddDataDialog = false },
                onAddTask = ::openAddTask,
                onAddSchedule = ::openAddSchedule,
                onAddCourse = ::openAddCourse
            )
        }
    }
}
