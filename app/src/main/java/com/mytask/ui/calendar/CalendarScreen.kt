@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.data.local.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val CompletedGreen = Color(0xFF2E7D32)
private val CompletedGreenContainer = Color(0xFFE8F5E9)

@Composable
fun CalendarScreen(
    onBack: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val tasks by viewModel.tasks.collectAsState()

    var displayedMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    var selectedDate by remember { mutableStateOf(Date()) }

    val currentMonth = displayedMonth.get(Calendar.MONTH)
    val currentYear = displayedMonth.get(Calendar.YEAR)
    val today = remember { Calendar.getInstance() }

    val monthTitle = SimpleDateFormat(
        "MMMM yyyy",
        Locale("id", "ID")
    ).format(displayedMonth.time)

    val daysInMonth = displayedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = displayedMonth.get(Calendar.DAY_OF_WEEK)

    val selectedTasks = tasks.filter {
        viewModel.isSameDay(it.deadline, selectedDate)
    }

    val selectedDateLabel = SimpleDateFormat(
        "EEEE, dd MMMM yyyy",
        Locale("id", "ID")
    ).format(selectedDate)

    val selectedDayHasOverdueTask = selectedTasks.any { task ->
        task.deadline?.let(::isDateOverdue) == true && !task.isCompleted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Kalender", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val now = Calendar.getInstance()
                        displayedMonth = Calendar.getInstance().apply {
                            set(Calendar.YEAR, now.get(Calendar.YEAR))
                            set(Calendar.MONTH, now.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, 1)
                        }
                        selectedDate = now.time
                    }) {
                        Icon(Icons.Default.Today, contentDescription = "Hari ini")
                    }
                }
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 8.dp,
                end = 16.dp,
                bottom = 112.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                CalendarMonthHeader(
                    monthTitle = monthTitle,
                    onPrevious = {
                        displayedMonth = shiftMonth(displayedMonth, -1)
                        selectedDate = displayedMonth.time
                    },
                    onNext = {
                        displayedMonth = shiftMonth(displayedMonth, 1)
                        selectedDate = displayedMonth.time
                    }
                )
            }

            item {
                CalendarGrid(
                    daysInMonth = daysInMonth,
                    firstDayOfWeek = firstDayOfWeek,
                    currentMonth = currentMonth,
                    currentYear = currentYear,
                    today = today,
                    selectedDate = selectedDate,
                    tasks = tasks,
                    viewModel = viewModel,
                    onDateSelected = { selectedDate = it }
                )
            }

            item {
                SelectedDateHeader(
                    dateLabel = selectedDateLabel,
                    taskCount = selectedTasks.size,
                    hasOverdueTask = selectedDayHasOverdueTask
                )
            }

            if (selectedTasks.isEmpty()) {
                item { EmptyCalendarAgenda() }
            } else {
                items(
                    count = selectedTasks.size,
                    key = { index -> selectedTasks[index].id }
                ) { index ->
                    CalendarTaskCard(task = selectedTasks[index])
                }
            }
        }
    }
}

@Composable
private fun CalendarMonthHeader(
    monthTitle: String,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "Bulan sebelumnya")
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.size(10.dp))
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, contentDescription = "Bulan berikutnya")
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    daysInMonth: Int,
    firstDayOfWeek: Int,
    currentMonth: Int,
    currentYear: Int,
    today: Calendar,
    selectedDate: Date,
    tasks: List<TaskEntity>,
    viewModel: CalendarViewModel,
    onDateSelected: (Date) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab").forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            var day = 1
            val totalCells = ((firstDayOfWeek - 1 + daysInMonth + 6) / 7) * 7

            repeat(totalCells / 7) { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    repeat(7) { column ->
                        val cellIndex = week * 7 + column

                        if (cellIndex < firstDayOfWeek - 1 || day > daysInMonth) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            )
                        } else {
                            val currentDay = day
                            val dateForCell = Calendar.getInstance().apply {
                                set(Calendar.YEAR, currentYear)
                                set(Calendar.MONTH, currentMonth)
                                set(Calendar.DAY_OF_MONTH, currentDay)
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.time

                            val isSelected = viewModel.isSameDay(selectedDate, dateForCell)
                            val isToday = viewModel.isSameDay(today.time, dateForCell)
                            val dayTasks = tasks.filter { task ->
                                viewModel.isSameDay(task.deadline, dateForCell)
                            }
                            val hasCompletedTask = dayTasks.any { it.isCompleted }
                            val hasOverdueTask = dayTasks.any { task ->
                                task.deadline?.let(::isDateOverdue) == true && !task.isCompleted
                            }
                            val hasTask = dayTasks.isNotEmpty()

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clickable { onDateSelected(dateForCell) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> MaterialTheme.colorScheme.primary
                                                    isToday -> MaterialTheme.colorScheme.primaryContainer
                                                    else -> MaterialTheme.colorScheme.surface
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentDay.toString(),
                                            color = when {
                                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                                isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                                else -> MaterialTheme.colorScheme.onSurface
                                            },
                                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }

                                    Spacer(Modifier.height(2.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (hasTask) {
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            hasOverdueTask -> MaterialTheme.colorScheme.error
                                                            hasCompletedTask -> CompletedGreen
                                                            else -> MaterialTheme.colorScheme.primary
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }

                            day++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDateHeader(
    dateLabel: String,
    taskCount: Int,
    hasOverdueTask: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Agenda",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (hasOverdueTask) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        ) {
            Text(
                text = "$taskCount tugas",
                style = MaterialTheme.typography.labelMedium,
                color = if (hasOverdueTask) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun EmptyCalendarAgenda() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Tidak ada tugas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tidak ada deadline pada tanggal ini.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CalendarTaskCard(task: TaskEntity) {
    val overdue = task.deadline?.let(::isDateOverdue) == true && !task.isCompleted

    val containerColor = when {
        task.isCompleted -> CompletedGreenContainer
        overdue -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor = when {
        task.isCompleted -> CompletedGreen
        overdue -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderColor = when {
        task.isCompleted -> CompletedGreen.copy(alpha = 0.45f)
        overdue -> MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor
                    )

                    if (task.description.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (task.isCompleted) {
                                CompletedGreen.copy(alpha = 0.82f)
                            } else if (overdue) {
                                MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.82f)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 2
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when {
                        task.isCompleted -> CompletedGreen
                        overdue -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = when {
                            task.isCompleted -> "Selesai"
                            overdue -> "Terlambat"
                            else -> "Aktif"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted || overdue) {
                            Color.White
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            HorizontalDivider(
                color = when {
                    task.isCompleted -> CompletedGreen.copy(alpha = 0.18f)
                    overdue -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                }
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        task.isCompleted -> "Tugas selesai"
                        task.deadline == null -> "Tanpa deadline"
                        overdue -> "Terlambat ${daysOverdue(task.deadline)} hari"
                        else -> relativeDeadline(task.deadline)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        task.isCompleted -> CompletedGreen
                        overdue -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = when (task.priority) {
                        3 -> "Prioritas Tinggi"
                        2 -> "Prioritas Sedang"
                        else -> "Prioritas Rendah"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (task.isCompleted) {
                        CompletedGreen.copy(alpha = 0.78f)
                    } else if (overdue) {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.78f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

private fun shiftMonth(source: Calendar, amount: Int): Calendar {
    return Calendar.getInstance().apply {
        time = source.time
        add(Calendar.MONTH, amount)
        set(Calendar.DAY_OF_MONTH, 1)
    }
}

private fun isDateOverdue(date: Date): Boolean {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val target = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return target.timeInMillis < today.timeInMillis
}

private fun daysOverdue(date: Date?): Long {
    if (date == null) return 0L

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val target = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    return TimeUnit.MILLISECONDS.toDays(
        today.timeInMillis - target.timeInMillis
    ).coerceAtLeast(1L)
}

private fun relativeDeadline(date: Date?): String {
    if (date == null) return "Tanpa deadline"

    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val target = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val days = TimeUnit.MILLISECONDS.toDays(
        target.timeInMillis - today.timeInMillis
    )

    return when {
        days == 0L -> "Hari ini"
        days == 1L -> "1 hari lagi"
        days > 1L -> "$days hari lagi"
        else -> "Terlambat ${-days} hari"
    }
}
