@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    Scaffold(

        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mata Kuliah",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },

        floatingActionButton = {

            FloatingActionButton(
                onClick = onAddCourse
            ) {

                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah"
                )
            }
        }

    ) { paddingValues ->

        if (courses.isEmpty()) {

            Column(

                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),

                horizontalAlignment = Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center
            ) {

                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null
                )

                Spacer(
                    Modifier.height(12.dp)
                )

                Text("Belum ada mata kuliah")

            }

        } else {

            LazyColumn(

                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),

                contentPadding =
                    PaddingValues(
                        bottom = 96.dp
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)

            ) {

                items(
                    courses,
                    key = {
                        it.id
                    }
                ) { course ->

                    CourseCard(

                        course = course,

                        onEdit = {
                            onEditCourse(course.id)
                        },

                        onDelete = {
                            viewModel.deleteCourse(course)
                        }
                    )
                   }
                }
            }
        }
    }


@Composable
private fun CourseCard(
    course: CourseEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(

            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(
                Modifier.padding(8.dp)
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    course.name,
                    style =
                        MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    course.code,
                    style =
                        MaterialTheme.typography.bodyMedium
                )

                Text(
                    "${course.lecturer} • ${course.room}",
                    style =
                        MaterialTheme.typography.bodySmall
                )
            }

            IconButton(
                onClick = onEdit
            ) {

                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit"
                )
            }

            IconButton(
                onClick = onDelete
            ) {

                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Hapus"
                )
            }
        }
    }
}