@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.task

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AddEditTaskScreen(
    taskId: Long? = null,
    onBack: () -> Unit = {},
    viewModel: TaskViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsState()
    val taskFlow = remember(taskId) { taskId?.let(viewModel::getTaskById) }
    val task by if (taskFlow != null) taskFlow.collectAsState() else remember { mutableStateOf(null) }

    var selectedCourseId by remember(taskId) { mutableStateOf<Long?>(null) }
    var selectedCourseName by remember(taskId) { mutableStateOf("") }
    var courseExpanded by remember(taskId) { mutableStateOf(false) }
    var title by remember(taskId) { mutableStateOf("") }
    var description by remember(taskId) { mutableStateOf("") }
    var priority by remember(taskId) { mutableIntStateOf(1) }
    var deadline by remember(taskId) { mutableStateOf<Date?>(null) }
    var initialized by remember(taskId) { mutableStateOf(false) }
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")) }

    LaunchedEffect(task, courses) {
        if (task != null && !initialized) {
            selectedCourseId = task!!.courseId
            selectedCourseName = courses.find { it.id == task!!.courseId }?.name ?: ""
            title = task!!.title
            description = task!!.description
            priority = task!!.priority
            deadline = task!!.deadline
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (taskId == null) "Tambah Tugas" else "Edit Tugas") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Kembali") }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.padding(end = 12.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Task, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                }
                Column {
                    Text(if (taskId == null) "Buat tugas baru" else "Perbarui tugas", style = MaterialTheme.typography.titleLarge)
                    Text("Lengkapi informasi tugas dan deadline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Detail Tugas", style = MaterialTheme.typography.titleMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { courseExpanded = true }, modifier = Modifier.fillMaxWidth(), enabled = courses.isNotEmpty()) {
                            Text(if (selectedCourseName.isBlank()) "Pilih Mata Kuliah" else selectedCourseName)
                        }
                        DropdownMenu(expanded = courseExpanded, onDismissRequest = { courseExpanded = false }) {
                            courses.forEach { course ->
                                DropdownMenuItem(
                                    text = { Text("${course.code} - ${course.name}") },
                                    onClick = { selectedCourseId = course.id; selectedCourseName = course.name; courseExpanded = false }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama Tugas") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Deskripsi") },
                        minLines = 4
                    )
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Deadline & Prioritas", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            deadline?.let { calendar.time = it }
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    deadline = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, 23, 59, 59)
                                        set(Calendar.MILLISECOND, 999)
                                    }.time
                                },
                                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(deadline?.let(dateFormat::format) ?: "Pilih Deadline")
                    }

                    Text("Prioritas", style = MaterialTheme.typography.labelLarge)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "Rendah", 2 to "Sedang", 3 to "Tinggi").forEach { (value, label) ->
                            Button(
                                onClick = { priority = value },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (priority == value) "✓ $label" else label)
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Batal") }
                Button(
                    onClick = {
                        if (selectedCourseId == null || title.isBlank()) return@Button
                        if (task == null) {
                            viewModel.addTask(selectedCourseId, title.trim(), description.trim(), priority, deadline, onBack)
                        } else {
                            viewModel.updateTask(
                                task!!.copy(
                                    courseId = selectedCourseId,
                                    title = title.trim(),
                                    description = description.trim(),
                                    priority = priority,
                                    deadline = deadline
                                ),
                                onSaved = onBack
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedCourseId != null && title.isNotBlank()
                ) {
                    Text(if (taskId == null) "Simpan Tugas" else "Update Tugas")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
