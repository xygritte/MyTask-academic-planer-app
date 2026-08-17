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
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.R
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
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
    val disabledScheduleNotificationIds by viewModel.disabledScheduleNotificationIds.collectAsState()

    val today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
    val todaySchedules = schedules.filter { it.dayOfWeek == today }.sortedBy { it.startMinutes }

    val upcomingTasks = tasks.filter { !it.isCompleted }.sortedBy { it.deadline?.time ?: Long.MAX_VALUE }.take(5)
    val completedTaskCount = tasks.count { it.isCompleted }
    val overdueTaskCount = tasks.count { task -> !task.isCompleted && task.deadline?.let(::isOverdue) == true }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.mipmap.mytask_background),
                            contentDescription = "MyTask",
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(11.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("MyTask", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Academic Planner", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddDataClick) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Data")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item { WelcomeHeader() }
            item {
                TodaySummaryCard(scheduleCount = todaySchedules.size, activeTaskCount = activeTaskCount, onClick = onTasksClick)
            }
            item { SectionHeader(title = "Overview", action = "Lihat semua", onClick = onCoursesClick) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OverviewCard(title = "Mata Kuliah", value = courseCount.toString(), icon = Icons.Default.MenuBook, modifier = Modifier.weight(1f), onClick = onCoursesClick)
                        OverviewCard(title = "Tugas Aktif", value = activeTaskCount.toString(), icon = Icons.Default.Task, modifier = Modifier.weight(1f), onClick = onTasksClick)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OverviewCard(title = "Tugas Selesai", value = completedTaskCount.toString(), icon = Icons.Default.CheckCircle, modifier = Modifier.weight(1f), onClick = onTasksClick)
                        OverviewCard(title = "Tugas Terlewat", value = overdueTaskCount.toString(), icon = Icons.Default.Schedule, modifier = Modifier.weight(1f), isAlert = overdueTaskCount > 0, onClick = onTasksClick)
                    }
                }
            }
            item { SectionHeader(title = "Jadwal Hari Ini", action = if (todaySchedules.isNotEmpty()) "Lihat jadwal" else null, onClick = onScheduleClick) }
            item {
                TodayScheduleCard(
                    schedules = todaySchedules,
                    courses = courses,
                    disabledScheduleNotificationIds = disabledScheduleNotificationIds,
                    onNotificationToggle = viewModel::setScheduleNotificationEnabled,
                    onClick = onScheduleClick
                )
            }
            item { SectionHeader(title = "Tugas Mendatang", action = if (upcomingTasks.isNotEmpty()) "Lihat semua" else null, onClick = onTasksClick) }
            if (upcomingTasks.isEmpty()) {
                item { EmptyTaskCard() }
            } else {
                items(items = upcomingTasks, key = { it.id }) { task ->
                    DashboardTaskCard(task = task, courses = courses, onClick = onTasksClick)
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
        Text(greeting, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text("Siap mengatur hari ini?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TodaySummaryCard(scheduleCount: Int, activeTaskCount: Int, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(44.dp), CircleShape, color = MaterialTheme.colorScheme.primary) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Task, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary) }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Hari ini", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text("$activeTaskCount tugas aktif · $scheduleCount jadwal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Buka tugas", tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: String?, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        if (action != null) {
            Row(modifier = Modifier.clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
                Text(action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun OverviewCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, isAlert: Boolean = false, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = if (isAlert) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isAlert) MaterialTheme.colorScheme.error.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Surface(Modifier.size(34.dp), RoundedCornerShape(10.dp), color = if (isAlert) MaterialTheme.colorScheme.error.copy(alpha = 0.14f) else MaterialTheme.colorScheme.secondaryContainer) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(2.dp))
            Text(title, style = MaterialTheme.typography.bodyMedium, color = if (isAlert) MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TodayScheduleCard(
    schedules: List<ScheduleEntity>,
    courses: List<CourseEntity>,
    disabledScheduleNotificationIds: Set<Long>,
    onNotificationToggle: (ScheduleEntity, Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        if (schedules.isEmpty()) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(Modifier.size(40.dp), RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Tidak ada jadwal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Hari ini kamu tidak memiliki jadwal kuliah.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            Column(Modifier.padding(vertical = 8.dp)) {
                schedules.forEachIndexed { index, schedule ->
                    val course = courses.find { it.id == schedule.courseId }
                    TodayScheduleRow(
                        schedule = schedule,
                        courseName = course?.name ?: "Mata Kuliah",
                        notificationsEnabled = schedule.id !in disabledScheduleNotificationIds,
                        onNotificationToggle = { enabled -> onNotificationToggle(schedule, enabled) },
                        onClick = onClick
                    )
                    if (index < schedules.lastIndex) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f))
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
    notificationsEnabled: Boolean,
    onNotificationToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f).clickable(onClick = onClick).padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.width(68.dp)) {
                Text(formatScheduleTime(schedule.startMinutes), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(formatScheduleTime(schedule.endMinutes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(14.dp))
            Surface(Modifier.size(8.dp), CircleShape, color = if (notificationsEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline) {}
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (schedule.room.isNotBlank()) {
                    Text(schedule.room, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Checkbox(
            checked = notificationsEnabled,
            onCheckedChange = onNotificationToggle,
            modifier = Modifier.size(48.dp),
            enabled = true
        )
    }
}

@Composable
private fun DashboardTaskCard(task: TaskEntity, courses: List<CourseEntity>, onClick: () -> Unit) {
    val course = courses.find { it.id == task.courseId }
    val deadlineLabel = task.deadline?.let { relativeDeadline(it) }
    val overdue = task.deadline?.let { isOverdue(it) } == true
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (overdue) MaterialTheme.colorScheme.error.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(34.dp), RoundedCornerShape(10.dp), color = if (overdue) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Task, contentDescription = null, tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(course?.name ?: "Mata Kuliah belum dipilih", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (deadlineLabel != null) {
                Spacer(Modifier.height(10.dp))
                Text(deadlineLabel, style = MaterialTheme.typography.labelMedium, color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun EmptyTaskCard() {
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(40.dp), RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Tidak ada tugas mendatang", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Semua tugasmu sudah selesai atau belum memiliki deadline.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

private fun isOverdue(deadline: java.util.Date): Boolean = deadline.time < System.currentTimeMillis()

private fun relativeDeadline(deadline: java.util.Date): String {
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
    val days = TimeUnit.MILLISECONDS.toDays(target.timeInMillis - today.timeInMillis)
    return when {
        days < 0 -> "Terlambat ${-days} hari"
        days == 0L -> "Deadline hari ini"
        days == 1L -> "Deadline besok"
        else -> "Deadline ${days} hari lagi"
    }
}