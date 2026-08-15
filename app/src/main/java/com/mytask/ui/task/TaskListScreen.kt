@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.task

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.data.local.entity.TaskEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

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
                        text = "Tugas",
                        fontWeight = FontWeight.Bold
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

                    imageVector =
                        Icons.Default.Add,

                    contentDescription =
                        "Tambah Tugas"
                )
            }
        }

    ) { paddingValues ->

        if (tasks.isEmpty()) {

            EmptyTaskList(
                paddingValues =
                    paddingValues,
                onAddTask =
                    onAddTask
            )

        } else {

            LazyColumn(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        ),

                contentPadding =
                    PaddingValues(
                        horizontal = 16.dp,
                        top = 8.dp,
                        bottom = 112.dp
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )

            ) {

                items(

                    items =
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
                                .toggleTask(
                                    task
                                )
                        },

                        onEdit = {
                            onEditTask(
                                task.id
                            )
                        },

                        onDelete = {
                            viewModel
                                .deleteTask(
                                    task
                                )
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

    val deadlineInfo =
        task.deadline?.let {
            getDeadlineInfo(it)
        }

    val borderColor =
        when {

            task.isCompleted ->
                MaterialTheme
                    .colorScheme
                    .outline
                    .copy(
                        alpha = 0.30f
                    )

            deadlineInfo?.isOverdue == true ->
                MaterialTheme
                    .colorScheme
                    .error
                    .copy(
                        alpha = 0.55f
                    )

            else ->
                MaterialTheme
                    .colorScheme
                    .outline
                    .copy(
                        alpha = 0.45f
                    )
        }

    Card(

        modifier =
            Modifier.fillMaxWidth(),

        shape =
            MaterialTheme.shapes.large,

        colors =
            CardDefaults.cardColors(

                containerColor =
                    MaterialTheme
                        .colorScheme
                        .surface
            ),

        border =
            BorderStroke(
                width = 1.dp,
                color = borderColor
            )
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 14.dp,
                        vertical = 12.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            StatusButton(

                completed =
                    task.isCompleted,

                onClick =
                    onToggle
            )

            Spacer(
                Modifier.width(
                    10.dp
                )
            )

            Column(

                modifier =
                    Modifier.weight(
                        1f
                    )

            ) {

                Text(

                    text =
                        task.title,

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

                    fontWeight =
                        FontWeight.SemiBold,

                    color =
                        if (
                            task.isCompleted
                        ) {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurface
                        }
                )

                Spacer(
                    Modifier.height(
                        3.dp
                    )
                )

                Row(

                    verticalAlignment =
                        Alignment.CenterVertically

                ) {

                    Icon(

                        imageVector =
                            Icons.Default
                                .MenuBook,

                        contentDescription =
                            null,

                        tint =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        modifier =
                            Modifier.size(
                                16.dp
                            )
                    )

                    Spacer(
                        Modifier.width(
                            5.dp
                        )
                    )

                    Text(

                        text =
                            courseName,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                if (
                    !task.isCompleted &&
                    deadlineInfo != null
                ) {

                    Spacer(
                        Modifier.height(
                            8.dp
                        )
                    )

                    Row(

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                6.dp
                            ),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        DeadlineChip(
                            info =
                                deadlineInfo
                        )

                        PriorityChip(
                            priority =
                                task.priority
                        )
                    }

                } else if (
                    task.isCompleted
                ) {

                    Spacer(
                        Modifier.height(
                            7.dp
                        )
                    )

                    StatusLabel(
                        text =
                            "Tugas selesai"
                    )
                }
            }

            Spacer(
                Modifier.width(
                    4.dp
                )
            )

            IconButton(

                onClick =
                    onEdit,

                modifier =
                    Modifier.size(
                        40.dp
                    )

            ) {

                Icon(

                    imageVector =
                        Icons.Default.Edit,

                    contentDescription =
                        "Edit Tugas",

                    modifier =
                        Modifier.size(
                            20.dp
                        )
                )
            }

            IconButton(

                onClick =
                    onDelete,

                modifier =
                    Modifier.size(
                        40.dp
                    )

            ) {

                Icon(

                    imageVector =
                        Icons.Default.Delete,

                    contentDescription =
                        "Hapus Tugas",

                    modifier =
                        Modifier.size(
                            20.dp
                        )
                )
            }
        }
    }
}


@Composable
private fun StatusButton(

    completed:
        Boolean,

    onClick:
        () -> Unit

) {

    IconButton(

        onClick =
            onClick,

        modifier =
            Modifier.size(
                40.dp
            )

    ) {

        Icon(

            imageVector =
                if (
                    completed
                ) {

                    Icons.Default
                        .CheckCircle

                } else {

                    Icons.Default
                        .RadioButtonUnchecked
                },

            contentDescription =
                if (
                    completed
                ) {
                    "Tugas selesai"
                } else {
                    "Tandai tugas selesai"
                },

            tint =
                if (
                    completed
                ) {
                    MaterialTheme
                        .colorScheme
                        .primary
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                },

            modifier =
                Modifier.size(
                    24.dp
                )
        )
    }
}


@Composable
private fun DeadlineChip(

    info:
        DeadlineInfo

) {

    val backgroundColor =
        if (
            info.isOverdue
        ) {

            MaterialTheme
                .colorScheme
                .errorContainer

        } else {

            MaterialTheme
                .colorScheme
                .secondaryContainer
        }

    val contentColor =
        if (
            info.isOverdue
        ) {

            MaterialTheme
                .colorScheme
                .onErrorContainer

        } else {

            MaterialTheme
                .colorScheme
                .onSecondaryContainer
        }

    Surface(

        shape =
            RoundedCornerShape(
                8.dp
            ),

        color =
            backgroundColor

    ) {

        Row(

            modifier =
                Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 5.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Icon(

                imageVector =
                    Icons.Default
                        .CalendarMonth,

                contentDescription =
                    null,

                tint =
                    contentColor,

                modifier =
                    Modifier.size(
                        14.dp
                    )
            )

            Spacer(
                Modifier.width(
                    4.dp
                )
            )

            Text(

                text =
                    info.label,

                style =
                    MaterialTheme
                        .typography
                        .labelMedium,

                color =
                    contentColor
            )
        }
    }
}


@Composable
private fun PriorityChip(

    priority:
        Int

) {

    val label =
        when (
            priority
        ) {

            3 ->
                "Tinggi"

            2 ->
                "Sedang"

            else ->
                "Rendah"
        }

    val containerColor =
        when (
            priority
        ) {

            3 ->
                MaterialTheme
                    .colorScheme
                    .errorContainer

            2 ->
                MaterialTheme
                    .colorScheme
                    .secondaryContainer

            else ->
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
        }

    val contentColor =
        when (
            priority
        ) {

            3 ->
                MaterialTheme
                    .colorScheme
                    .onErrorContainer

            2 ->
                MaterialTheme
                    .colorScheme
                    .onSecondaryContainer

            else ->
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        }

    Surface(

        shape =
            RoundedCornerShape(
                8.dp
            ),

        color =
            containerColor

    ) {

        Text(

            text =
                label,

            style =
                MaterialTheme
                    .typography
                    .labelMedium,

            color =
                contentColor,

            modifier =
                Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 5.dp
                )
        )
    }
}


@Composable
private fun StatusLabel(

    text:
        String

) {

    Surface(

        shape =
            RoundedCornerShape(
                8.dp
            ),

        color =
            MaterialTheme
                .colorScheme
                .secondaryContainer

    ) {

        Text(

            text =
                text,

            style =
                MaterialTheme
                    .typography
                    .labelMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSecondaryContainer,

            modifier =
                Modifier.padding(
                    horizontal = 8.dp,
                    vertical = 5.dp
                )
        )
    }
}


@Composable
private fun EmptyTaskList(

    paddingValues:
        PaddingValues,

    onAddTask:
        () -> Unit

) {

    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    paddingValues
                )
                .padding(
                    horizontal = 24.dp
                ),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center

    ) {

        Surface(

            modifier =
                Modifier.size(
                    64.dp
                ),

            shape =
                RoundedCornerShape(
                    18.dp
                ),

            color =
                MaterialTheme
                    .colorScheme
                    .secondaryContainer

        ) {

            Box(

                contentAlignment =
                    Alignment.Center

            ) {

                Icon(

                    imageVector =
                        Icons.Default.Task,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    modifier =
                        Modifier.size(
                            32.dp
                        )
                )
            }
        }

        Spacer(
            Modifier.height(
                16.dp
            )
        )

        Text(

            text =
                "Belum ada tugas",

            style =
                MaterialTheme
                    .typography
                    .titleLarge,

            fontWeight =
                FontWeight.Bold
        )

        Spacer(
            Modifier.height(
                6.dp
            )
        )

        Text(

            text =
                "Tambahkan tugas pertama kamu untuk mulai mengatur aktivitas akademik.",

            style =
                MaterialTheme
                    .typography
                    .bodyMedium,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}


private data class DeadlineInfo(

    val label:
        String,

    val isOverdue:
        Boolean
)


private fun getDeadlineInfo(

    deadline:
        java.util.Date

): DeadlineInfo {

    val today =
        Calendar
            .getInstance()
            .apply {

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

    val target =
        Calendar
            .getInstance()
            .apply {

                time =
                    deadline

                set(
                    Calendar.HOUR_OF_DAY,
                    0
                )

                set(
                    Calendar.MINUTE,
                    0
                )

                set(
                    Calendar.SECOND,
                    0
                )

                set(
                    Calendar.MILLISECOND,
                    0
                )
            }

    val days =
        TimeUnit.MILLISECONDS.toDays(

            target.timeInMillis -
                today.timeInMillis
        )

    return when {

        days < 0L ->
            DeadlineInfo(
                label =
                    "Terlambat ${-days} hari",
                isOverdue =
                    true
            )

        days == 0L ->
            DeadlineInfo(
                label =
                    "Hari ini",
                isOverdue =
                    false
            )

        days == 1L ->
            DeadlineInfo(
                label =
                    "1 hari lagi",
                isOverdue =
                    false
            )

        else ->
            DeadlineInfo(
                label =
                    "$days hari lagi",
                isOverdue =
                    false
            )
    }
}
