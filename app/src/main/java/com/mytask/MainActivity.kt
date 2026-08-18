@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.unit.dp
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
import com.mytask.ui.login.OfflineLoginScreen
import com.mytask.ui.login.isNetworkAvailable
import com.mytask.ui.login.rememberNetworkAvailable
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
    @Inject lateinit var firebaseAuthRepository: FirebaseAuthRepository
    @Inject lateinit var templateDataImporter: TemplateDataImporter
    @Inject lateinit var cloudDataSyncRepository: CloudDataSyncRepository

    private val notificationPermissionLauncher = registerForActivityResult(
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
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
    val userProfileRepository = remember(context) { UserProfileRepository(context) }
    val templatePreferenceRepository = remember(context) { TemplatePreferenceRepository(context) }
    val firebaseUser by authRepository.authState.collectAsState(initial = authRepository.currentUser)
    val localProfile by userProfileRepository.profile.collectAsState(initial = null)

    val currentFirebaseUser = firebaseUser
    val currentLocalProfile = localProfile

    var networkRefreshKey by remember { mutableStateOf(0) }
    val isOnline = rememberNetworkAvailable(context, networkRefreshKey)
    val networkAvailable = isNetworkAvailable(context)

    var sessionProfile by remember { mutableStateOf<UserProfile?>(null) }
    var sessionUid by remember { mutableStateOf<String?>(null) }
    var restorePendingState by remember { mutableStateOf(false) }
    var accountLoading by remember { mutableStateOf(false) }
    var syncReady by remember { mutableStateOf(false) }
    var minimumLoading by remember { mutableStateOf(true) }
    var isApplyingTemplate by remember { mutableStateOf(false) }
    var templateError by remember { mutableStateOf<String?>(null) }
    var isSavingOnline by remember { mutableStateOf(false) }
    var isAutoSyncing by remember { mutableStateOf(false) }
    var onlineSaveMessage by remember { mutableStateOf<String?>(null) }
    var shouldShowTemplatePrompt by remember { mutableStateOf(false) }
    var isRefreshingConnectivity by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        delay(500)
        minimumLoading = false
    }

    LaunchedEffect(currentLocalProfile) {
        val profile = currentLocalProfile
        val uid = runCatching { userProfileRepository.uid.first() }.getOrNull()
        if (profile != null && uid != null) {
            sessionProfile = profile
            sessionUid = uid
            if (uid == "guest" && currentFirebaseUser == null) {
                syncReady = true
                val guestPromptShown = runCatching {
                    templatePreferenceRepository.promptShown("guest").first()
                }.getOrDefault(false)
                shouldShowTemplatePrompt = !guestPromptShown
            }
        }
    }

    LaunchedEffect(currentFirebaseUser?.uid, isOnline) {
        val user = currentFirebaseUser

        if (user == null) {
            accountLoading = false
            restorePendingState = false

            if (sessionUid == "guest") {
                syncReady = true
            } else {
                sessionProfile = null
                sessionUid = null
                syncReady = false
                shouldShowTemplatePrompt = false
                templateError = null
                onlineSaveMessage = null
            }
            return@LaunchedEffect
        }

        accountLoading = true
        onlineSaveMessage = null
        shouldShowTemplatePrompt = false
        templateError = null

        val cachedUid = runCatching { userProfileRepository.uid.first() }.getOrNull()
        val cachedProfile = currentLocalProfile?.takeIf { cachedUid == user.uid }
        val immediateProfile = cachedProfile
            ?: UserProfile(
                name = user.displayName?.trim()?.takeIf { it.isNotBlank() } ?: "Mahasiswa",
                program = "Program Studi belum diatur"
            )

        sessionProfile = immediateProfile
        sessionUid = user.uid

        val shouldRestoreFromCloud = runCatching {
            userProfileRepository.restorePending.first()
        }.getOrDefault(false)
        restorePendingState = shouldRestoreFromCloud

        if (shouldRestoreFromCloud) {
            val restoreResult = runCatching {
                cloudDataSyncRepository.syncOnLogin(user.uid)
            }

            restoreResult.onSuccess { cloudDataExists ->
                userProfileRepository.clearCloudRestorePending()
                restorePendingState = false

                if (!cloudDataExists) {
                    val promptShown = runCatching {
                        templatePreferenceRepository.promptShown(user.uid).first()
                    }.getOrDefault(false)
                    shouldShowTemplatePrompt = !promptShown
                } else {
                    shouldShowTemplatePrompt = false
                }

                syncReady = true
                accountLoading = false
            }.onFailure { error ->
                templateError = error.message ?: "Data akun tidak dapat dimuat dari online."
                sessionProfile = null
                sessionUid = null
                restorePendingState = false
                syncReady = false
                accountLoading = false
                shouldShowTemplatePrompt = false
                runCatching { authRepository.clearLocalSession() }
            }
        } else {
            shouldShowTemplatePrompt = false
            syncReady = true
            accountLoading = false
        }
    }

    LaunchedEffect(
        currentFirebaseUser?.uid,
        syncReady,
        networkAvailable,
        restorePendingState,
        shouldShowTemplatePrompt
    ) {
        val user = currentFirebaseUser
        if (
            user != null &&
            syncReady &&
            networkAvailable &&
            !restorePendingState &&
            !shouldShowTemplatePrompt &&
            !isSavingOnline &&
            !isAutoSyncing
        ) {
            isAutoSyncing = true
            try {
                authRepository.syncCurrentUserProfile()
                    .onFailure { error ->
                        onlineSaveMessage = error.message ?: "Sinkronisasi profil gagal."
                    }
                cloudDataSyncRepository.uploadCurrentData(user.uid)
            } catch (error: Throwable) {
                onlineSaveMessage = error.message ?: "Sinkronisasi otomatis gagal."
            } finally {
                isAutoSyncing = false
            }
        }
    }

    if (minimumLoading) {
        LoadingScreen("Memulai MyTask...")
        return
    }

    val activeProfile = sessionProfile ?: currentLocalProfile
    val isGuestSession = sessionUid == "guest" && currentFirebaseUser == null
    val workspaceReady = syncReady || isGuestSession

    if (activeProfile != null && workspaceReady) {
        val templateUid = sessionUid ?: currentFirebaseUser?.uid ?: "guest"
        Box(modifier = Modifier.fillMaxSize()) {
            MyTaskMainContent(
                profile = activeProfile,
                canSaveOnline = currentFirebaseUser != null && networkAvailable,
                isSavingOnline = isSavingOnline || isAutoSyncing || !networkAvailable,
                onlineSaveMessage = onlineSaveMessage,
                authRepository = authRepository,
                onSaveDataOnline = {
                    val user = currentFirebaseUser
                    if (user != null && networkAvailable && !isSavingOnline && !isAutoSyncing) {
                        scope.launch {
                            isSavingOnline = true
                            onlineSaveMessage = null
                            runCatching {
                                authRepository.syncCurrentUserProfile().getOrThrow()
                                cloudDataSyncRepository.uploadCurrentData(user.uid)
                            }
                                .onSuccess { onlineSaveMessage = "Data berhasil disimpan ke online." }
                                .onFailure { error -> onlineSaveMessage = error.message ?: "Gagal menyimpan data ke online." }
                            isSavingOnline = false
                        }
                    }
                },
                onRefreshNetwork = {
                    if (!isRefreshingConnectivity && !isSavingOnline && !isAutoSyncing) {
                        scope.launch {
                            isRefreshingConnectivity = true
                            networkRefreshKey += 1
                            delay(550)

                            val refreshedOnline = isNetworkAvailable(context)
                            val user = currentFirebaseUser
                            if (refreshedOnline && user != null) {
                                isAutoSyncing = true
                                onlineSaveMessage = null
                                try {
                                    authRepository.syncCurrentUserProfile()
                                        .onFailure { error ->
                                            onlineSaveMessage = error.message ?: "Sinkronisasi profil gagal."
                                        }
                                    cloudDataSyncRepository.uploadCurrentData(user.uid)
                                    if (onlineSaveMessage == null) {
                                        onlineSaveMessage = "Data berhasil disinkronkan."
                                    }
                                } catch (error: Throwable) {
                                    onlineSaveMessage = error.message ?: "Gagal menyinkronkan data."
                                } finally {
                                    isAutoSyncing = false
                                }
                            }

                            isRefreshingConnectivity = false
                        }
                    }
                },
                isRefreshingConnectivity = isRefreshingConnectivity,
                onLoggedOut = {
                    sessionProfile = null
                    sessionUid = null
                    restorePendingState = false
                    syncReady = false
                    accountLoading = false
                    onlineSaveMessage = null
                    shouldShowTemplatePrompt = false
                },
                onLogout = {
                    val user = currentFirebaseUser
                    if (networkAvailable && !isSavingOnline && !isAutoSyncing) {
                        scope.launch {
                            isSavingOnline = true
                            onlineSaveMessage = null

                            if (user != null) {
                                runCatching {
                                    authRepository.syncCurrentUserProfile().getOrThrow()
                                    cloudDataSyncRepository.uploadCurrentData(user.uid)
                                }.onSuccess {
                                    onlineSaveMessage = "Data berhasil disimpan. Keluar dari akun..."
                                    authRepository.clearLocalSession()
                                }.onFailure { error ->
                                    onlineSaveMessage = error.message
                                        ?: "Data gagal disimpan. Logout dibatalkan."
                                    isSavingOnline = false
                                    return@launch
                                }
                            } else {
                                authRepository.clearLocalSession()
                            }

                            isSavingOnline = false
                            onlineSaveMessage = null
                        }
                    }
                }
            )

            if (shouldShowTemplatePrompt && !accountLoading) {
                AcademicTemplateDialog(
                    isApplying = isApplyingTemplate,
                    errorMessage = templateError,
                    onSkip = {
                        if (!isApplyingTemplate) {
                            scope.launch {
                                templatePreferenceRepository.markPromptShown(templateUid)
                                shouldShowTemplatePrompt = false
                            }
                        }
                    },
                    onApply = {
                        if (!isApplyingTemplate) {
                            scope.launch {
                                isApplyingTemplate = true
                                templateError = null
                                runCatching { templateDataImporter.importTemplate() }
                                    .onSuccess {
                                        templatePreferenceRepository.markPromptShown(templateUid)
                                        shouldShowTemplatePrompt = false
                                    }
                                    .onFailure { error -> templateError = error.message ?: "Template gagal diterapkan." }
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
            if (restorePendingState) "Menyiapkan akun dan memulihkan data..."
            else "Menyiapkan akun..."
        )
        return
    }

    if (currentLocalProfile != null && sessionUid == "guest") {
        MyTaskMainContent(
            profile = currentLocalProfile,
            canSaveOnline = false,
            isSavingOnline = false,
            onlineSaveMessage = null,
            authRepository = authRepository,
            onSaveDataOnline = {},
            onRefreshNetwork = {
                if (!isRefreshingConnectivity) {
                    scope.launch {
                        isRefreshingConnectivity = true
                        networkRefreshKey += 1
                        delay(550)
                        isRefreshingConnectivity = false
                    }
                }
            },
            isRefreshingConnectivity = isRefreshingConnectivity,
            onLoggedOut = {
                sessionProfile = null
                sessionUid = null
                restorePendingState = false
                syncReady = false
                shouldShowTemplatePrompt = false
            },
            onLogout = {
                if (networkAvailable) {
                    scope.launch {
                        authRepository.clearLocalSession()
                    }
                }
            }
        )
        return
    }

    if (isOnline) {
        LoginScreen(authRepository = authRepository)
    } else {
        OfflineLoginScreen(
            authRepository = authRepository,
            onRefresh = { networkRefreshKey += 1 }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyTaskMainContent(
    profile: UserProfile,
    canSaveOnline: Boolean,
    isSavingOnline: Boolean,
    onlineSaveMessage: String?,
    authRepository: FirebaseAuthRepository,
    onSaveDataOnline: () -> Unit,
    onRefreshNetwork: () -> Unit,
    isRefreshingConnectivity: Boolean,
    onLoggedOut: () -> Unit,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val courseViewModel: CourseViewModel = hiltViewModel()
    val courses by courseViewModel.courses.collectAsState()
    val hasCourses = courses.isNotEmpty()

    var showAddDataDialog by remember { mutableStateOf(false) }
    var scheduleAddRequestKey by remember { mutableStateOf(0) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 6 })
    val currentPage = pagerState.currentPage
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isSubScreen = currentRoute == Screen.AddTask.route ||
        currentRoute == Screen.AddCourse.route ||
        currentRoute == Screen.NotificationSettings.route ||
        currentRoute == Screen.Backup.route

    fun openAddDataDialog() { showAddDataDialog = true }
    fun openTasks() {
        if (hasCourses) scope.launch { pagerState.animateScrollToPage(1) }
        else openAddDataDialog()
    }
    fun openSchedule() {
        if (hasCourses) scope.launch { pagerState.animateScrollToPage(2) }
        else openAddDataDialog()
    }
    fun openAddTask() {
        showAddDataDialog = false
        if (hasCourses) navController.navigate("add_task?taskId=-1")
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

    val pullRefreshState = rememberPullToRefreshState()

    Scaffold(
        bottomBar = {
            if (!isSubScreen) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 10.dp,
                    tonalElevation = 3.dp
                ) {
                    NavigationBar(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        val navigationColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )

                        NavigationBarItem(selected = currentPage == 0, onClick = { scope.launch { pagerState.animateScrollToPage(0) } }, icon = { Icon(Icons.Default.Dashboard, "Dashboard") }, alwaysShowLabel = false, colors = navigationColors)
                        NavigationBarItem(selected = currentPage == 1, onClick = ::openTasks, icon = { Icon(Icons.Default.Task, "Tugas") }, alwaysShowLabel = false, colors = navigationColors)
                        NavigationBarItem(selected = currentPage == 2, onClick = ::openSchedule, icon = { Icon(Icons.Default.Schedule, "Jadwal") }, alwaysShowLabel = false, colors = navigationColors)
                        NavigationBarItem(selected = currentPage == 3, onClick = { scope.launch { pagerState.animateScrollToPage(3) } }, icon = { Icon(Icons.Default.CalendarMonth, "Kalender") }, alwaysShowLabel = false, colors = navigationColors)
                        NavigationBarItem(selected = currentPage == 4, onClick = { scope.launch { pagerState.animateScrollToPage(4) } }, icon = { Icon(Icons.Default.MenuBook, "Mata Kuliah") }, alwaysShowLabel = false, colors = navigationColors)
                        NavigationBarItem(selected = currentPage == 5, onClick = { scope.launch { pagerState.animateScrollToPage(5) } }, icon = { Icon(Icons.Default.Person, "Profile") }, alwaysShowLabel = false, colors = navigationColors)
                    }
                }
            }
        }
    ) { paddingValues ->
        PullToRefreshBox(
            state = pullRefreshState,
            isRefreshing = isRefreshingConnectivity,
            onRefresh = onRefreshNetwork,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize()) {
                NavGraph(
                    navController = navController,
                    paddingValues = paddingValues,
                    modifier = Modifier.fillMaxSize().zIndex(if (isSubScreen) 10f else 0f)
                )

                if (!isSubScreen) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = hasCourses,
                        modifier = Modifier.fillMaxSize().padding(paddingValues).zIndex(1f),
                        beyondViewportPageCount = 1
                    ) { page ->
                        when (page) {
                            0 -> DashboardScreen(
                                onCoursesClick = { scope.launch { pagerState.animateScrollToPage(4) } },
                                onTasksClick = ::openTasks,
                                onScheduleClick = ::openSchedule,
                                onCalendarClick = { scope.launch { pagerState.animateScrollToPage(3) } },
                                onAddDataClick = ::openAddDataDialog
                            )
                            1 -> TaskListScreen(
                                onAddTask = ::openAddDataDialog,
                                onEditTask = { id ->
                                    if (hasCourses) navController.navigate("add_task?taskId=$id")
                                    else openAddDataDialog()
                                }
                            )
                            2 -> ScheduleScreen(addRequestKey = scheduleAddRequestKey, onAddData = ::openAddDataDialog)
                            3 -> CalendarScreen(onBack = { scope.launch { if (currentPage > 0) pagerState.animateScrollToPage(currentPage - 1) } })
                            4 -> CourseListScreen(
                                onAddCourse = ::openAddDataDialog,
                                onEditCourse = { id -> navController.navigate("add_course?courseId=$id") }
                            )
                            5 -> ProfileScreen(
                                profile = profile,
                                canSaveOnline = canSaveOnline,
                                isSavingOnline = isSavingOnline,
                                onlineSaveMessage = onlineSaveMessage,
                                onBack = { scope.launch { if (currentPage > 0) pagerState.animateScrollToPage(currentPage - 1) } },
                                onNotificationSettings = { navController.navigate(Screen.NotificationSettings.route) },
                                onBackupData = { navController.navigate(Screen.Backup.route) },
                                onEditProfile = {},
                                onSaveDataOnline = onSaveDataOnline,
                                onLogout = onLogout
                            )
                        }
                    }
                }

                if (!isSubScreen) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 10.dp, end = 12.dp)
                            .zIndex(4f),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shadowElevation = 6.dp,
                        tonalElevation = 2.dp
                    ) {
                        IconButton(
                            onClick = { navController.navigate(Screen.NotificationSettings.route) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Pengaturan notifikasi",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
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
