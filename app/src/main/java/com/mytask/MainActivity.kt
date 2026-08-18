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
                    uid = templateUid,
                    importer = templateDataImporter,
                    onApplyStarted = {
                        isApplyingTemplate = true
                        templateError = null
                    },
                    onApplied = {
                        isApplyingTemplate = false
                        shouldShowTemplatePrompt = false
                        onlineSaveMessage = null
                    },
                    onError = { error ->
                        isApplyingTemplate = false
                        templateError = error.message ?: "Template gagal diterapkan."
                    }
                )
            }

            if (isApplyingTemplate) {
                LoadingScreen("Menerapkan template...")
            }
        }
        return
    }

    if (accountLoading || restorePendingState) {
        LoadingScreen("Memulihkan data akun...")
        return
    }

    if (currentFirebaseUser == null && sessionUid == null) {
        LoginScreen(
            onLoggedIn = { profile ->
                sessionProfile = profile
                sessionUid = currentFirebaseUser?.uid
            },
            onGuest = { profile ->
                sessionProfile = profile
                sessionUid = "guest"
                syncReady = true
            }
        )
        return
    }

    LoadingScreen("Menyiapkan workspace...")
}

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
    val context = LocalContext.current
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentScreen = Screen.fromRoute(currentRoute)
    val mainRoutes = remember {
        setOf(
            Screen.Dashboard.route,
            Screen.Tasks.route,
            Screen.Schedule.route,
            Screen.Calendar.route,
            Screen.Courses.route,
            Screen.Profile.route
        )
    }
    val showGlobalNotificationButton = currentRoute in mainRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showGlobalNotificationButton) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        IconButton(
                            onClick = { navController.navigate(Screen.NotificationSettings.route) },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .zIndex(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Pengaturan notifikasi",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 8.dp
            ) {
                val items = listOf(
                    Screen.Dashboard to Icons.Filled.Dashboard,
                    Screen.Tasks to Icons.Filled.Task,
                    Screen.Schedule to Icons.Filled.Schedule,
                    Screen.Calendar to Icons.Filled.CalendarMonth,
                    Screen.Courses to Icons.Filled.MenuBook,
                    Screen.Profile to Icons.Filled.Person
                )
                items.forEach { (screen, icon) ->
                    NavigationBarItem(
                        selected = currentScreen == screen,
                        onClick = {
                            navController.navigate(screen.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(Screen.Dashboard.route) { saveState = true }
                            }
                        },
                        icon = { Icon(icon, contentDescription = screen.label) },
                        label = null,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        NavGraph(
            navController = navController,
            modifier = Modifier.padding(paddingValues),
            profile = profile,
            authRepository = authRepository,
            canSaveOnline = canSaveOnline,
            isSavingOnline = isSavingOnline,
            onlineSaveMessage = onlineSaveMessage,
            onSaveDataOnline = onSaveDataOnline,
            onRefreshNetwork = onRefreshNetwork,
            isRefreshingConnectivity = isRefreshingConnectivity,
            onLoggedOut = onLoggedOut,
            onLogout = onLogout
        )
    }
}
