@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.R
import com.mytask.data.local.ScheduleRangeResolver
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import kotlinx.coroutines.delay
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Composable
fun DashboardScreen(
    onCoursesClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onAddDataClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val courseCount by viewModel.courseCount.collectAsState()
    val activeTaskCount by viewModel.activeTaskCount.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    val disabledScheduleNotificationIds by
    viewModel.disabledScheduleNotificationIds.collectAsState()

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000L)
            nowMillis = System.currentTimeMillis()
        }
    }

    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)

    // dayOfWeek == 0 berarti "Setiap Hari"
    val todaySchedules = schedules
        .filter {
            it.dayOfWeek == today || it.dayOfWeek == 0
        }
        .sortedBy { it.startMinutes }

    val upcomingTasks = tasks
        .filter { !it.isCompleted }
        .sortedBy { it.deadline?.time ?: Long.MAX_VALUE }
        .take(5)

    val completedTaskCount = tasks.count { it.isCompleted }

    val overdueTaskCount = tasks.count { task ->
        !task.isCompleted &&
                task.deadline?.let(::isOverdue) == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.mipmap.mytask_background),
                            contentDescription = "MyTask",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(11.dp))
                        )

                        Spacer(Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "MyTask",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Academic Planner",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddDataClick
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Data"
                )
            }
        }
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            item {
                WelcomeHeader()
            }

            item {
                TodaySummaryCard(
                    scheduleCount = todaySchedules.size,
                    activeTaskCount = activeTaskCount,
                    onClick = onTasksClick
                )
            }

            item {
                SectionHeader(
                    title = "Overview",
                    action = "Lihat semua",
                    onClick = onCoursesClick
                )
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OverviewCard(
                            title = "Mata Kuliah",
                            value = courseCount.toString(),
                            icon = Icons.Default.MenuBook,
                            modifier = Modifier.weight(1f),
                            onClick = onCoursesClick
                        )

                        OverviewCard(
                            title = "Tugas Aktif",
                            value = activeTaskCount.toString(),
                            icon = Icons.Default.Task,
                            modifier = Modifier.weight(1f),
                            onClick = onTasksClick
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OverviewCard(
                            title = "Tugas Selesai",
                            value = completedTaskCount.toString(),
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f),
                            onClick = onTasksClick
                        )

                        OverviewCard(
                            title = "Tugas Terlewat",
                            value = overdueTaskCount.toString(),
                            icon = Icons.Default.Schedule,
                            modifier = Modifier.weight(1f),
                            isAlert = overdueTaskCount > 0,
                            onClick = onTasksClick
                        )
                    }
                }
            }

            item {
                SectionHeader(
                    title = "Jadwal Hari Ini",
                    action = if (todaySchedules.isNotEmpty()) {
                        "Lihat jadwal"
                    } else {
                        null
                    },
                    onClick = onScheduleClick
                )
            }

            item {
                TodayScheduleCard(
                    schedules = todaySchedules,
                    courses = courses,
                    disabledScheduleNotificationIds =
                        disabledScheduleNotificationIds,
                    nowMillis = nowMillis,
                    onNotificationToggle =
                        viewModel::setScheduleNotificationEnabled,
                    onClick = onScheduleClick
                )
            }

            item {
                SectionHeader(
                    title = "Tugas Mendatang",
                    action = if (upcomingTasks.isNotEmpty()) {
                        "Lihat semua"
                    } else {
                        null
                    },
                    onClick = onTasksClick
                )
            }

            if (upcomingTasks.isEmpty()) {
                item {
                    EmptyTaskCard()
                }
            } else {
                items(
                    items = upcomingTasks,
                    key = { it.id }
                ) { task ->
                    DashboardTaskCard(
                        task = task,
                        courses = courses,
                        onClick = onTasksClick
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeHeader() {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

    val greeting = when (hour) {
        in 5..11 -> "Selamat pagi"
        in 12..17 -> "Selamat sore"
        else -> "Selamat malam"
    }

    Column {
        Text(
            text = greeting,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = "Siap mengatur hari ini?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TodaySummaryCard(
    scheduleCount: Int,
    activeTaskCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Task,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Hari ini",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "$activeTaskCount tugas aktif · $scheduleCount jadwal",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Buka tugas",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    action: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )

        if (action != null) {
            Row(
                modifier = Modifier.clickable(onClick = onClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
    isAlert: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (isAlert) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isAlert) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isAlert) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isAlert) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isAlert) {
                    MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun TodayScheduleCard(
    schedules: List<ScheduleEntity>,
    courses: List<CourseEntity>,
    disabledScheduleNotificationIds: Set<Long>,
    nowMillis: Long,
    onNotificationToggle: (ScheduleEntity, Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        if (schedules.isEmpty()) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Tidak ada jadwal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Hari ini kamu tidak memiliki jadwal kuliah.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                schedules.forEachIndexed { index, schedule ->
                    val course = courses.find {
                        it.id == schedule.courseId
                    }

                    val notificationsDisabled =
                        schedule.id in disabledScheduleNotificationIds
                    val resolved = ScheduleRangeResolver.resolve(schedule, nowMillis)

                    TodayScheduleRow(
                        schedule = schedule,
                        courseName = course?.name ?: "Mata Kuliah",
                        notificationsDisabled = notificationsDisabled,
                        resolved = resolved,
                        nowMillis = nowMillis,
                        onNotificationToggle = { checked ->
                            // checked = true berarti notifikasi dimatikan
                            onNotificationToggle(
                                schedule,
                                !checked
                            )
                        },
                        onClick = onClick
                    )

                    if (index < schedules.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                horizontal = 18.dp,
                                vertical = 4.dp
                            ),
                            color = MaterialTheme.colorScheme.outline.copy(
                                alpha = 0.22f
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayScheduleRow(
    schedule: ScheduleEntity,
    courseName: String,
    notificationsDisabled: Boolean,
    resolved: ScheduleRangeResolver.ResolvedRange?,
    nowMillis: Long,
    onNotificationToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val displayStart = resolved?.range?.startMinutes ?: schedule.startMinutes
    val displayEnd = resolved?.range?.endMinutes ?: schedule.endMinutes

    val statusText = when (resolved?.state) {
        ScheduleRangeResolver.State.ACTIVE ->
            "Sedang berlangsung · ${formatDuration(resolved.endAt - nowMillis)}"
        ScheduleRangeResolver.State.NEXT ->
            "Mulai dalam ${formatDuration(resolved.startAt - nowMillis)}"
        ScheduleRangeResolver.State.FINISHED ->
            "Selesai hari ini"
        null -> null
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .padding(
                    horizontal = 6.dp,
                    vertical = 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(68.dp)
            ) {
                Text(
                    text = formatScheduleTime(displayStart),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = formatScheduleTime(displayEnd),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(14.dp))

            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = when {
                    notificationsDisabled -> MaterialTheme.colorScheme.outline
                    resolved?.state == ScheduleRangeResolver.State.ACTIVE -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.secondary
                }
            ) {}

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (schedule.room.isNotBlank()) {
                    Text(
                        text = schedule.room,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (statusText != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (resolved?.state) {
                            ScheduleRangeResolver.State.ACTIVE -> MaterialTheme.colorScheme.primary
                            ScheduleRangeResolver.State.NEXT -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.outline
                        },
                        fontWeight = if (resolved?.state == ScheduleRangeResolver.State.ACTIVE) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        }
                    )
                }
            }
        }

        Checkbox(
            checked = notificationsDisabled,
            onCheckedChange = onNotificationToggle,
            modifier = Modifier.size(48.dp)
        )
    }
}

@Composable
private fun DashboardTaskCard(
    task: TaskEntity,
    courses: List<CourseEntity>,
    onClick: () -> Unit
) {
    val course = courses.find {
        it.id == task.courseId
    }

    val deadlineLabel = task.deadline?.let {
        relativeDeadline(it)
    }

    val overdue = task.deadline?.let {
        isOverdue(it)
    } == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(34.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (overdue) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Task,
                            contentDescription = null,
                            tint = if (overdue) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = course?.name
                            ?: "Mata Kuliah belum dipilih",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (deadlineLabel != null) {
                Spacer(Modifier.height(10.dp))

                Text(
                    text = deadlineLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (overdue) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
private fun EmptyTaskCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column {
                Text(
                    text = "Tidak ada tugas mendatang",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "Semua tugasmu sudah selesai atau belum memiliki deadline.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatScheduleTime(totalMinutes: Int): String {
    val safeMinutes = totalMinutes.coerceIn(0, 1439)
    val hour = safeMinutes / 60
    val minute = safeMinutes % 60

    return "%02d:%02d".format(hour, minute)
}

private fun formatDuration(durationMillis: Long): String {
    val safeSeconds = (durationMillis.coerceAtLeast(0L) / 1000L)
    val hours = safeSeconds / 3600L
    val minutes = (safeSeconds % 3600L) / 60L
    val seconds = safeSeconds % 60L

    return if (hours > 0L) {
        "%02d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%02d:%02d".format(minutes, seconds)
    }
}

private fun isOverdue(deadline: java.util.Date): Boolean {
    return deadline.time < System.currentTimeMillis()
}

private fun relativeDeadline(
    deadline: java.util.Date
): String {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val target = Calendar.getInstance().apply {
        time = deadline
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val days = TimeUnit.MILLISECONDS.toDays(
        target.timeInMillis - today.timeInMillis
    )

    return when {
        days < 0 -> "Terlambat ${-days} hari"
        days == 0L -> "Deadline hari ini"
        days == 1L -> "Deadline besok"
        else -> "Deadline ${days} hari lagi"
    }
}
