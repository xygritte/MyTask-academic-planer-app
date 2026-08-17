@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.schedule

import android.app.TimePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.data.local.ScheduleTimeRange
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.getTimeRanges
import com.mytask.data.local.toDisplayTime
import com.mytask.data.local.toJsonString
import com.mytask.data.local.validateTimeRanges

@Composable
fun ScheduleScreen(
    addRequestKey: Int = 0,
    onAddData: () -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val schedules by viewModel.schedules.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var editingScheduleId by remember { mutableStateOf<Long?>(null) }
    var scheduleToDelete by remember { mutableStateOf<ScheduleEntity?>(null) }

    LaunchedEffect(addRequestKey) {
        if (addRequestKey > 0) editingScheduleId = -1L
    }

    if (editingScheduleId != null) {
        ScheduleForm(
            scheduleId = editingScheduleId?.takeIf { it != -1L },
            courses = courses,
            viewModel = viewModel,
            onCancel = { editingScheduleId = null },
            onSaved = { editingScheduleId = null }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(text = "Jadwal Kuliah") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddData) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Data")
            }
        }
    ) { paddingValues ->
        when {
            schedules.isEmpty() -> EmptyScheduleState(paddingValues = paddingValues)
            else -> LazyColumn(
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
                dayGroups.forEach { (dayNumber, dayName) ->
                    val daySchedules = schedules
                        .filter { it.dayOfWeek == dayNumber }
                        .sortedBy {
                            it.getTimeRanges().minOfOrNull { range -> range.startMinutes }
                                ?: it.startMinutes
                        }

                    if (daySchedules.isNotEmpty()) {
                        item(key = "day_$dayNumber") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold
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
                                courseName = courses.find { it.id == schedule.courseId }?.name
                                    ?: "Mata Kuliah",
                                onEdit = { editingScheduleId = schedule.id },
                                onDelete = { scheduleToDelete = schedule }
                            )
                        }
                    }
                }
            }
        }
    }

    scheduleToDelete?.let { schedule ->
        AlertDialog(
            onDismissRequest = { scheduleToDelete = null },
            title = { Text("Hapus Jadwal?") },
            text = {
                Text(
                    "Jadwal ini beserta seluruh rentang waktunya akan dihapus. Tindakan ini tidak dapat dibatalkan."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSchedule(schedule)
                        scheduleToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { scheduleToDelete = null }) { Text("Batal") }
            }
        )
    }
}

private val dayGroups = listOf(
    0 to "Setiap Hari",
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
    val ranges = schedule.getTimeRanges().sortedBy { it.startMinutes }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(90.dp)) {
                ranges.forEachIndexed { index, range ->
                    if (index > 0) Spacer(Modifier.height(5.dp))
                    Text(
                        text = "${range.startMinutes.toDisplayTime()} – ${range.endMinutes.toDisplayTime()}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
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
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (schedule.room.isNotBlank()) {
                    Text(
                        text = schedule.room,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${ranges.size} rentang waktu",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Jadwal")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus Jadwal")
            }
        }
    }
}

@Composable
private fun EmptyScheduleState(paddingValues: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp),
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
        Text("Belum ada jadwal", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tambahkan jadwal kuliah untuk melihat rutinitas akademik kamu.",
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
    val context = LocalContext.current
    val scheduleFlow = remember(scheduleId) { scheduleId?.let(viewModel::getScheduleById) }
    val schedule by (
        scheduleFlow?.collectAsState()
            ?: remember { mutableStateOf<ScheduleEntity?>(null) }
        )

    var selectedCourse by remember(scheduleId) { mutableStateOf<CourseEntity?>(null) }
    var courseExpanded by remember(scheduleId) { mutableStateOf(false) }
    var day by remember(scheduleId) { mutableStateOf(2) }
    var dayExpanded by remember(scheduleId) { mutableStateOf(false) }
    var room by remember(scheduleId) { mutableStateOf("") }
    var timeRanges by remember(scheduleId) {
        mutableStateOf(
            listOf(ScheduleTimeRange(startMinutes = 8 * 60, endMinutes = 10 * 60))
        )
    }
    var timeError by remember(scheduleId) { mutableStateOf<String?>(null) }

    LaunchedEffect(scheduleId, schedule, courses) {
        val current = schedule ?: return@LaunchedEffect
        selectedCourse = courses.find { it.id == current.courseId }
        day = current.dayOfWeek
        room = current.room
        timeRanges = current.getTimeRanges().sortedBy { it.startMinutes }
    }

    fun pickTime(initialMinutes: Int, onPicked: (Int) -> Unit) {
        TimePickerDialog(
            context,
            { _, hour, minute -> onPicked(hour * 60 + minute) },
            initialMinutes / 60,
            initialMinutes % 60,
            true
        ).show()
    }

    val selectedDayName = dayGroups.firstOrNull { it.first == day }?.second ?: "Senin"
    val isEditing = scheduleId != null

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                title = {
                    Column {
                        Text(
                            if (isEditing) "Edit Jadwal" else "Tambah Jadwal",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isEditing) "Perbarui informasi jadwal" else "Buat jadwal kuliah baru",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, tonalElevation = 2.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(0.8f).height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("Batal") }

                    Button(
                        onClick = {
                            val validationError = timeRanges.validateTimeRanges()
                            if (validationError != null) {
                                timeError = validationError
                                return@Button
                            }

                            val course = selectedCourse ?: return@Button
                            val sortedRanges = timeRanges.sortedBy { it.startMinutes }
                            timeError = null

                            if (scheduleId == null) {
                                viewModel.addSchedule(
                                    courseId = course.id,
                                    dayOfWeek = day,
                                    timeRanges = sortedRanges,
                                    room = room.trim(),
                                    onSaved = onSaved
                                )
                            } else {
                                val current = schedule ?: return@Button
                                val firstRange = sortedRanges.first()
                                val updatedSchedule = current.copy(
                                    courseId = course.id,
                                    dayOfWeek = day,
                                    startMinutes = firstRange.startMinutes,
                                    endMinutes = firstRange.endMinutes,
                                    room = room.trim(),
                                    timeRangesJson = sortedRanges.toJsonString()
                                )
                                viewModel.updateSchedule(updatedSchedule, onSaved)
                            }
                        },
                        modifier = Modifier.weight(1.2f).height(52.dp),
                        enabled = selectedCourse != null,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            if (isEditing) "Simpan Perubahan" else "Simpan Jadwal",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FormSection(
                title = "Mata Kuliah",
                subtitle = "Pilih mata kuliah yang memiliki jadwal ini"
            ) {
                ExposedDropdownMenuBox(
                    expanded = courseExpanded,
                    onExpandedChange = { courseExpanded = !courseExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCourse?.let {
                            if (it.code.isBlank()) it.name else "${it.code} • ${it.name}"
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Mata Kuliah") },
                        placeholder = { Text("Pilih mata kuliah") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = courseExpanded)
                        },
                        enabled = courses.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = courseExpanded,
                        onDismissRequest = { courseExpanded = false }
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(course.name, fontWeight = FontWeight.Medium)
                                        if (course.code.isNotBlank()) {
                                            Text(
                                                course.code,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedCourse = course
                                    courseExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            FormSection(
                title = "Hari",
                subtitle = "Tentukan hari berlangsungnya kelas"
            ) {
                ExposedDropdownMenuBox(
                    expanded = dayExpanded,
                    onExpandedChange = { dayExpanded = !dayExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedDayName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        label = { Text("Hari kuliah") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayExpanded)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = { dayExpanded = false }
                    ) {
                        dayGroups.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.second) },
                                onClick = {
                                    day = item.first
                                    dayExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            FormSection(
                title = "Waktu Kuliah",
                subtitle = "Satu jadwal dapat memiliki beberapa rentang waktu"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    timeRanges.forEachIndexed { index, range ->
                        TimeRangeEditorRow(
                            index = index,
                            range = range,
                            canDelete = timeRanges.size > 1,
                            onStartChanged = { newStart ->
                                val updated = timeRanges.toMutableList()
                                val newEnd = if (range.endMinutes <= newStart) {
                                    (newStart + 60).coerceAtMost(1439)
                                } else {
                                    range.endMinutes
                                }
                                updated[index] = range.copy(
                                    startMinutes = newStart,
                                    endMinutes = newEnd
                                )
                                timeRanges = updated
                            },
                            onEndChanged = { newEnd ->
                                val updated = timeRanges.toMutableList()
                                updated[index] = range.copy(endMinutes = newEnd)
                                timeRanges = updated
                            },
                            onDelete = {
                                timeRanges = timeRanges.toMutableList().apply { removeAt(index) }
                                timeError = null
                            }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val lastEnd = timeRanges.maxOfOrNull { it.endMinutes } ?: 8 * 60
                            val newStart = (lastEnd + 60).coerceAtMost(1439)
                            val newEnd = (newStart + 60).coerceAtMost(1439)
                            if (newEnd > newStart) {
                                timeRanges = timeRanges + ScheduleTimeRange(newStart, newEnd)
                                timeError = null
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tambah Rentang Waktu")
                    }
                }

                if (timeError != null) {
                    Text(
                        timeError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            FormSection(title = "Lokasi", subtitle = "Opsional") {
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ruangan") },
                    placeholder = { Text("Contoh: Lab 2 / Ruang 301") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TimeRangeEditorRow(
    index: Int,
    range: ScheduleTimeRange,
    canDelete: Boolean,
    onStartChanged: (Int) -> Unit,
    onEndChanged: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeField(
            label = "Mulai ${index + 1}",
            value = range.startMinutes.toDisplayTime(),
            modifier = Modifier.weight(1f)
        ) {
            val context = LocalContext.current
            TimePickerDialog(
                context,
                { _, hour, minute -> onStartChanged(hour * 60 + minute) },
                range.startMinutes / 60,
                range.startMinutes % 60,
                true
            ).show()
        }

        Spacer(Modifier.width(8.dp))
        Text("→", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(8.dp))

        TimeField(
            label = "Selesai ${index + 1}",
            value = range.endMinutes.toDisplayTime(),
            modifier = Modifier.weight(1f)
        ) {
            val context = LocalContext.current
            TimePickerDialog(
                context,
                { _, hour, minute -> onEndChanged(hour * 60 + minute) },
                range.endMinutes / 60,
                range.endMinutes % 60,
                true
            ).show()
        }

        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus rentang waktu")
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    modifier: Modifier,
    onClick: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)),
        onClick = onClick as () -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Ketuk untuk mengubah",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
