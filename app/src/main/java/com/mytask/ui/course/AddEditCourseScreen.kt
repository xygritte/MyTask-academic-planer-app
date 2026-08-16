@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.course

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
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AddEditCourseScreen(
    courseId: Long? = null,
    onBack: () -> Unit = {},
    viewModel: CourseViewModel = hiltViewModel()
) {
    val courseFlow = remember(courseId) { courseId?.let(viewModel::getCourseById) }
    val course by if (courseFlow != null) courseFlow.collectAsState() else remember { mutableStateOf(null) }

    var name by remember(courseId) { mutableStateOf("") }
    var code by remember(courseId) { mutableStateOf("") }
    var lecturer by remember(courseId) { mutableStateOf("") }
    var room by remember(courseId) { mutableStateOf("") }
    var initialized by remember(courseId) { mutableStateOf(false) }

    LaunchedEffect(course) {
        if (course != null && !initialized) {
            name = course!!.name
            code = course!!.code
            lecturer = course!!.lecturer
            room = course!!.room
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (courseId == null) "Tambah Mata Kuliah" else "Edit Mata Kuliah") },
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
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                }
                Column {
                    Text(if (courseId == null) "Buat mata kuliah baru" else "Perbarui mata kuliah", style = MaterialTheme.typography.titleLarge)
                    Text("Simpan identitas mata kuliah dan informasi pengajar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Informasi Mata Kuliah", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nama Mata Kuliah") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Kode Mata Kuliah / SKS") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = lecturer,
                        onValueChange = { lecturer = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Dosen") },
                        singleLine = true
                    )
                }
            }

            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Lokasi", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = room,
                        onValueChange = { room = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ruangan") },
                        singleLine = true
                    )
                    Text("Informasi ruangan digunakan pada daftar jadwal dan notifikasi kelas.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Batal") }
                Button(
                    onClick = {
                        if (name.isBlank()) return@Button
                        if (course == null) {
                            viewModel.addCourse(name.trim(), code.trim(), lecturer.trim(), room.trim(), onBack)
                        } else {
                            viewModel.updateCourse(
                                course!!.copy(
                                    name = name.trim(),
                                    code = code.trim(),
                                    lecturer = lecturer.trim(),
                                    room = room.trim()
                                ),
                                onSaved = onBack
                            )
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank()
                ) {
                    Text(if (courseId == null) "Simpan Mata Kuliah" else "Update Mata Kuliah")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
