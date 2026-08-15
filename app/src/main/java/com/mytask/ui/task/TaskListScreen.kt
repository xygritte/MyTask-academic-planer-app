@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.data.local.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun TaskListScreen(
    onAddTask: () -> Unit = {},
    onEditTask: (Long) -> Unit = {},
    viewModel: TaskViewModel = hiltViewModel()
) {

    val tasks by viewModel.tasks.collectAsState()
    val courses by viewModel.courses.collectAsState()

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(

                        "Tugas",

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            )
        },

        floatingActionButton = {

            FloatingActionButton(
                onClick =
                    onAddTask
            ) {

                Icon(

                    Icons.Default.Add,

                    contentDescription =
                        "Tambah Tugas"
                )
            }
        }

    ) { paddingValues ->

        if (tasks.isEmpty()) {

            Column(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        ),

                horizontalAlignment =
                    Alignment.CenterHorizontally,

                verticalArrangement =
                    Arrangement.Center
            ) {

                Icon(

                    Icons.Default.Task,

                    contentDescription =
                        null,

                    modifier =
                        Modifier.size(48.dp)
                )

                Spacer(
                    Modifier.height(16.dp)
                )

                Text(
                    "Belum ada tugas"
                )
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

                    tasks,

                    key = {
                        it.id
                    }

                ) { task ->

                    val courseName =
                        courses.find {

                            it.id ==
                                    task.courseId

                        }?.name
                            ?: "Mata Kuliah belum dipilih"

                    TaskCard(

                        task =
                            task,

                        courseName =
                            courseName,

                        onToggle = {

                            viewModel
                                .toggleTask(task)
                        },

                        onEdit = {

                            onEditTask(
                                task.id
                            )
                        },

                        onDelete = {

                            viewModel
                                .deleteTask(task)
                        }
                    )
                }
                }
            }
        }
    }


@Composable
private fun TaskCard(

    task:
    TaskEntity,

    courseName:
    String,

    onToggle:
        () -> Unit,

    onEdit:
        () -> Unit,

    onDelete:
        () -> Unit

) {

    val dateFormat =
        SimpleDateFormat(

            "dd MMM yyyy",

            Locale(
                "id",
                "ID"
            )
        )

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            RoundedCornerShape(20.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            /*
             * STATUS BUTTON
             */

            IconButton(
                onClick =
                    onToggle
            ) {

                Icon(

                    imageVector =
                        if (
                            task.isCompleted
                        ) {

                            Icons.Default
                                .CheckCircle

                        } else {

                            Icons.Default
                                .RadioButtonUnchecked
                        },

                    contentDescription =
                        "Status tugas",

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            Column(

                modifier =
                    Modifier.weight(1f)
            ) {

                /*
                 * JUDUL
                 */

                Text(

                    text =
                        task.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.Bold
                )

                /*
                 * MATA KULIAH
                 */

                Row(

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(

                        Icons.Default.MenuBook,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(18.dp)
                    )

                    Spacer(
                        Modifier.size(6.dp)
                    )

                    Text(
                        courseName
                    )
                }

                Spacer(
                    Modifier.height(6.dp)
                )

                /*
                 * STATUS TEXT
                 */

                Text(

                    text =
                        if (
                            task.isCompleted
                        ) {
                            "Tugas selesai"
                        } else {
                            "Belum selesai"
                        },

                    style =
                        MaterialTheme
                            .typography
                            .labelMedium,

                    fontWeight =
                        FontWeight.SemiBold
                )

                /*
                 * DESKRIPSI
                 */

                if (
                    task.description
                        .isNotBlank()
                ) {

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(

                        text =
                            task.description,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                /*
                 * DEADLINE
                 */

                if (
                    task.deadline != null
                ) {

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Row {

                        Icon(

                            Icons.Default
                                .CalendarMonth,

                            contentDescription =
                                null,

                            modifier =
                                Modifier.size(18.dp)
                        )

                        Spacer(
                            Modifier.size(6.dp)
                        )

                        Text(

                            "Deadline: ${
                                dateFormat.format(
                                    task.deadline
                                )
                            }"
                        )
                    }
                }

                /*
                 * PRIORITAS
                 */

                Spacer(
                    Modifier.height(6.dp)
                )

                Text(

                    when (
                        task.priority
                    ) {

                        3 ->
                            "Prioritas Tinggi"

                        2 ->
                            "Prioritas Sedang"

                        else ->
                            "Prioritas Rendah"
                    },

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )
            }

            /*
             * EDIT
             */

            IconButton(
                onClick =
                    onEdit
            ) {

                Icon(

                    Icons.Default.Edit,

                    contentDescription =
                        "Edit Tugas"
                )
            }

            /*
             * DELETE
             */

            IconButton(
                onClick =
                    onDelete
            ) {

                Icon(

                    Icons.Default.Delete,

                    contentDescription =
                        "Hapus Tugas"
                )
            }
        }
    }
}