@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.course

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.data.local.entity.CourseEntity

@Composable
fun CourseListScreen(
    onAddCourse: () -> Unit = {},
    onEditCourse: (Long) -> Unit = {},
    viewModel: CourseViewModel = hiltViewModel()
) {
    val courses by viewModel.courses.collectAsState()
    var courseToDelete by remember { mutableStateOf<CourseEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Mata Kuliah", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddCourse) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Mata Kuliah")
            }
        }
    ) { paddingValues ->
        if (courses.isEmpty()) {
            EmptyCourseList(paddingValues = paddingValues, onAddCourse = onAddCourse)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = courses, key = { it.id }) { course ->
                    CourseCard(
                        course = course,
                        onEdit = { onEditCourse(course.id) },
                        onDelete = { courseToDelete = course }
                    )
                }
            }
        }
    }

    courseToDelete?.let { course ->
        AlertDialog(
            onDismissRequest = { courseToDelete = null },
            title = { Text("Hapus Mata Kuliah?") },
            text = { Text("\"${course.name}\" akan dihapus dari daftar mata kuliah.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCourse(course)
                        courseToDelete = null
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { courseToDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun CourseCard(
    course: CourseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(
                    course.code,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                if (course.lecturer.isNotBlank() || course.room.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    val details = buildString {
                        if (course.lecturer.isNotBlank()) append(course.lecturer)
                        if (course.lecturer.isNotBlank() && course.room.isNotBlank()) append(" • ")
                        if (course.room.isNotBlank()) append(course.room)
                    }
                    Text(
                        details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Mata Kuliah", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(40.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Hapus Mata Kuliah", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun EmptyCourseList(
    paddingValues: PaddingValues,
    onAddCourse: () -> Unit
) {
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
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Belum ada mata kuliah", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tambahkan mata kuliah untuk mulai mengatur jadwal dan tugas akademik kamu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
