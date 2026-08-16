@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.schedule

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity

@Composable
fun ScheduleScreen(
    addRequestKey: Int = 0,
    onAddData: () -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val schedules by viewModel.schedules.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var editingScheduleId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(addRequestKey) {
        if (addRequestKey > 0) {
            editingScheduleId = -1L
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jadwal Kuliah") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddData
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Tambah Data"
                )
            }
        }
    ) { paddingValues ->
        when {
            editingScheduleId != null -> {
                ScheduleForm(
                    scheduleId = editingScheduleId?.takeIf { it != -1L },
                    courses = courses,
                    viewModel = viewModel,
                    onCancel = { editingScheduleId = null },
                    onSaved = { editingScheduleId = null }
                )
            }

            schedules.isEmpty() -> {
                EmptyScheduleState(paddingValues)
            }

            else -> {
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
                    dayGroups.forEach { (dayNumber, dayName) ->
                        val daySchedules = schedules
                            .filter { it.dayOfWeek == dayNumber }
                            .sortedBy { it.startTime }

                        if (daySchedules.isNotEmpty()) {
                            item(key = "day_$dayNumber") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = dayName,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${daySchedules.size} jadwal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            items(
                                items = daySchedules,
                                key = { it.id }
                            ) { schedule ->
                                ScheduleCard(
                                    schedule = schedule,
                                    courseName = courses.find {
                                        it.id == schedule.courseId
                                    }?.name ?: "Mata Kuliah",
                                    onEdit = {
                                        editingScheduleId = schedule.id
                                    },
                                    onDelete = {
                                        viewModel.deleteSchedule(schedule)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private val dayGroups = listOf(
    1 to "Minggu",
    2 to "Senin",
    3 to "Selasa",
    4 to "Rabu",
    5 to "Kamis",
    6 to "Jumat",
    7 to "Sabtu"
)

@Composable
private fun ScheduleCard(
    schedule: ScheduleEntity,
    courseName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(68.dp)) {
                Text(
                    text = schedule.startTime,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = schedule.endTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary
            ) {}

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = courseName,
                    style = MaterialTheme.typography.titleMedium
                )
                if (schedule.room.isNotBlank()) {
                    Text(
                        text = schedule.room,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Jadwal"
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus Jadwal"
                )
            }
        }
    }
}

@Composable
private fun EmptyScheduleState(
    paddingValues: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Belum ada jadwal",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "Tambahkan jadwal kuliah untuk melihat rutinitas akademik kamu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ScheduleForm(
    scheduleId: Long?,
    courses: List<CourseEntity>,
    viewModel: ScheduleViewModel,
    onCancel: () -> Unit,
    onSaved: () -> Unit
) {
    val scheduleFlow = remember(scheduleId) {
        scheduleId?.let { viewModel.getScheduleById(it) }
    }

    val schedule by (
        scheduleFlow?.collectAsState()
            ?: remember { mutableStateOf<ScheduleEntity?>(null) }
        )

    var selectedCourse by remember(scheduleId) {
        mutableStateOf<CourseEntity?>(null)
    }
    var courseExpanded by remember(scheduleId) { mutableStateOf(false) }
    var day by remember(scheduleId) { mutableStateOf(2) }
    var dayName by remember(scheduleId) { mutableStateOf("Senin") }
    var dayExpanded by remember(scheduleId) { mutableStateOf(false) }
    var startTime by remember(scheduleId) { mutableStateOf("08:00") }
    var endTime by remember(scheduleId) { mutableStateOf("10:00") }
    var room by remember(scheduleId) { mutableStateOf("") }

    LaunchedEffect(scheduleId, schedule, courses) {
        if (scheduleId == null) return@LaunchedEffect

        val currentSchedule = schedule ?: return@LaunchedEffect

        selectedCourse = courses.find {
            it.id == currentSchedule.courseId
        }
        day = currentSchedule.dayOfWeek
        dayName = dayGroups.firstOrNull {
            it.first == currentSchedule.dayOfWeek
        }?.second ?: "Senin"
        startTime = currentSchedule.startTime
        endTime = currentSchedule.endTime
        room = currentSchedule.room
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = if (scheduleId == null) "Tambah Jadwal" else "Edit Jadwal",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = selectedCourse?.name ?: "Pilih Mata Kuliah",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mata Kuliah") }
            )

            Button(
                onClick = { courseExpanded = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = courses.isNotEmpty()
            ) {
                Text(selectedCourse?.name ?: "Pilih Mata Kuliah")
            }

            DropdownMenu(
                expanded = courseExpanded,
                onDismissRequest = { courseExpanded = false }
            ) {
                courses.forEach { course ->
                    DropdownMenuItem(
                        text = { Text("${course.code} - ${course.name}") },
                        onClick = {
                            selectedCourse = course
                            courseExpanded = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = dayName,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hari") }
            )

            Button(
                onClick = { dayExpanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(dayName)
            }

            DropdownMenu(
                expanded = dayExpanded,
                onDismissRequest = { dayExpanded = false }
            ) {
                dayGroups.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item.second) },
                        onClick = {
                            day = item.first
                            dayName = item.second
                            dayExpanded = false
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Mulai") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Selesai") },
                    singleLine = true
                )
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ruangan") },
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Batal")
                }

                Button(
                    onClick = {
                        val course = selectedCourse ?: return@Button

                        if (scheduleId == null) {
                            viewModel.addSchedule(
                                courseId = course.id,
                                dayOfWeek = day,
                                startTime = startTime,
                                endTime = endTime,
                                room = room,
                                onSaved = onSaved
                            )
                        } else {
                            val currentSchedule = schedule ?: return@Button

                            viewModel.updateSchedule(
                                currentSchedule.copy(
                                    id = currentSchedule.id,
                                    courseId = course.id,
                                    dayOfWeek = day,
                                    startTime = startTime,
                                    endTime = endTime,
                                    room = room
                                ),
                                onSaved = onSaved
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedCourse != null
                ) {
                    Text(if (scheduleId == null) "Simpan" else "Update")
                }
            }
        }
    }
}
