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
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.toDisplayTime

@Composable
fun ScheduleScreen(
    addRequestKey: Int = 0,
    onAddData: () -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val schedules by viewModel.schedules.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var editingScheduleId by remember {
        mutableStateOf<Long?>(null)
    }

    LaunchedEffect(addRequestKey) {
        if (addRequestKey > 0) {
            editingScheduleId = -1L
        }
    }

    if (editingScheduleId != null) {
        ScheduleForm(
            scheduleId = editingScheduleId?.takeIf { it != -1L },
            courses = courses,
            viewModel = viewModel,
            onCancel = {
                editingScheduleId = null
            },
            onSaved = {
                editingScheduleId = null
            }
        )

        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Jadwal Kuliah")
                }
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
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    dayGroups.forEach { (dayNumber, dayName) ->

                        val daySchedules = schedules
                            .filter { it.dayOfWeek == dayNumber }
                            .sortedBy { it.startMinutes }

                        if (daySchedules.isNotEmpty()) {

                            item(
                                key = "day_$dayNumber"
                            ) {
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

/*
 * dayOfWeek:
 *
 * 0 = Setiap Hari
 * 1 = Minggu
 * 2 = Senin
 * 3 = Selasa
 * 4 = Rabu
 * 5 = Kamis
 * 6 = Jumat
 * 7 = Sabtu
 */
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.width(72.dp)
            ) {
                Text(
                    text = schedule.startMinutes.toDisplayTime(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = schedule.endMinutes.toDisplayTime(),
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

            Column(
                modifier = Modifier.weight(1f)
            ) {
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
            }

            IconButton(
                onClick = onEdit
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit Jadwal"
                )
            }

            IconButton(
                onClick = onDelete
            ) {
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
            Box(
                contentAlignment = Alignment.Center
            ) {
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
    val context = LocalContext.current

    val scheduleFlow = remember(scheduleId) {
        scheduleId?.let(viewModel::getScheduleById)
    }

    val schedule by (
            scheduleFlow?.collectAsState()
                ?: remember {
                    mutableStateOf<ScheduleEntity?>(null)
                }
            )

    var selectedCourse by remember(scheduleId) {
        mutableStateOf<CourseEntity?>(null)
    }

    var courseExpanded by remember(scheduleId) {
        mutableStateOf(false)
    }

    var day by remember(scheduleId) {
        mutableStateOf(2)
    }

    var dayExpanded by remember(scheduleId) {
        mutableStateOf(false)
    }

    var startMinutes by remember(scheduleId) {
        mutableStateOf(8 * 60)
    }

    var endMinutes by remember(scheduleId) {
        mutableStateOf(10 * 60)
    }

    var room by remember(scheduleId) {
        mutableStateOf("")
    }

    var timeError by remember(scheduleId) {
        mutableStateOf<String?>(null)
    }

    /*
     * Saat edit jadwal, isi form dari data Room.
     */
    LaunchedEffect(
        scheduleId,
        schedule,
        courses
    ) {
        val current = schedule ?: return@LaunchedEffect

        selectedCourse = courses.find {
            it.id == current.courseId
        }

        day = current.dayOfWeek
        startMinutes = current.startMinutes
        endMinutes = current.endMinutes
        room = current.room
    }

    fun pickTime(
        initialMinutes: Int,
        onPicked: (Int) -> Unit
    ) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                onPicked(
                    hour * 60 + minute
                )
            },
            initialMinutes / 60,
            initialMinutes % 60,
            true
        ).show()
    }

    val selectedDayName = dayGroups
        .firstOrNull { it.first == day }
        ?.second
        ?: "Senin"

    val isEditing = scheduleId != null

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = onCancel
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = if (isEditing) {
                                "Edit Jadwal"
                            } else {
                                "Tambah Jadwal"
                            },
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (isEditing) {
                                "Perbarui informasi jadwal"
                            } else {
                                "Buat jadwal kuliah baru"
                            },
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
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 12.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(0.8f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Batal")
                    }

                    Button(
                        onClick = {
                            if (endMinutes <= startMinutes) {
                                timeError =
                                    "Waktu selesai harus setelah waktu mulai."
                                return@Button
                            }

                            val course = selectedCourse
                                ?: return@Button

                            timeError = null

                            if (scheduleId == null) {
                                viewModel.addSchedule(
                                    course.id,
                                    day,
                                    startMinutes,
                                    endMinutes,
                                    room.trim(),
                                    onSaved
                                )
                            } else {
                                val current = schedule
                                    ?: return@Button

                                viewModel.updateSchedule(
                                    current.copy(
                                        courseId = course.id,
                                        dayOfWeek = day,
                                        startMinutes = startMinutes,
                                        endMinutes = endMinutes,
                                        room = room.trim()
                                    ),
                                    onSaved
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(52.dp),
                        enabled = selectedCourse != null,
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (isEditing) {
                                "Simpan Perubahan"
                            } else {
                                "Simpan Jadwal"
                            },
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
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(paddingValues)
                .padding(
                    horizontal = 20.dp,
                    vertical = 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            FormSection(
                title = "Mata Kuliah",
                subtitle = "Pilih mata kuliah yang memiliki jadwal ini"
            ) {
                ExposedDropdownMenuBox(
                    expanded = courseExpanded,
                    onExpandedChange = {
                        courseExpanded = !courseExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = selectedCourse?.let {
                            if (it.code.isBlank()) {
                                it.name
                            } else {
                                "${it.code} • ${it.name}"
                            }
                        } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = {
                            Text("Mata Kuliah")
                        },
                        placeholder = {
                            Text("Pilih mata kuliah")
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = courseExpanded
                            )
                        },
                        enabled = courses.isNotEmpty(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = courseExpanded,
                        onDismissRequest = {
                            courseExpanded = false
                        }
                    ) {
                        courses.forEach { course ->

                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            text = course.name,
                                            fontWeight = FontWeight.Medium
                                        )

                                        if (course.code.isNotBlank()) {
                                            Text(
                                                text = course.code,
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
                    onExpandedChange = {
                        dayExpanded = !dayExpanded
                    }
                ) {
                    OutlinedTextField(
                        value = selectedDayName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = {
                            Text("Hari kuliah")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(
                                expanded = dayExpanded
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = dayExpanded,
                        onDismissRequest = {
                            dayExpanded = false
                        }
                    ) {
                        dayGroups.forEach { item ->

                            DropdownMenuItem(
                                text = {
                                    Text(item.second)
                                },
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
                subtitle = "Atur jam mulai dan selesai • format 24 jam"
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    TimeField(
                        label = "Mulai",
                        value = startMinutes.toDisplayTime(),
                        modifier = Modifier.weight(1f)
                    ) {
                        pickTime(
                            startMinutes
                        ) { pickedTime ->

                            startMinutes = pickedTime

                            if (endMinutes <= pickedTime) {
                                endMinutes = (
                                        pickedTime + 60
                                        ).coerceAtMost(1439)
                            }

                            timeError = null
                        }
                    }

                    TimeField(
                        label = "Selesai",
                        value = endMinutes.toDisplayTime(),
                        modifier = Modifier.weight(1f)
                    ) {
                        pickTime(
                            endMinutes
                        ) { pickedTime ->

                            endMinutes = pickedTime
                            timeError = null
                        }
                    }
                }

                if (timeError != null) {
                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = timeError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            FormSection(
                title = "Lokasi",
                subtitle = "Opsional"
            ) {
                OutlinedTextField(
                    value = room,
                    onValueChange = {
                        room = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Ruangan")
                    },
                    placeholder = {
                        Text("Contoh: Lab 2 / Ruang 301")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null
                        )
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
private fun FormSection(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(
                alpha = 0.18f
            )
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
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
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(82.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(
            alpha = 0.45f
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(
                alpha = 0.22f
            )
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Ketuk untuk mengubah",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}