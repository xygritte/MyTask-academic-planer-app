@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mytask.R
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.TaskEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun DashboardScreen(
    onCoursesClick: () -> Unit = {},
    onTasksClick: () -> Unit = {},
    onScheduleClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {

    val courseCount by viewModel.courseCount.collectAsState()
    val activeTaskCount by viewModel.activeTaskCount.collectAsState()

    val tasks by viewModel.tasks.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    val today =
        Calendar.getInstance()
            .get(Calendar.DAY_OF_WEEK)

    val todaySchedules =
        schedules
            .filter {
                it.dayOfWeek == today
            }
            .sortedBy {
                it.startTime
            }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        /*
                         * Logo launcher aplikasi
                         */
                        androidx.compose.foundation.Image(

                            painter =
                                painterResource(
                                    id =
                                        R.mipmap.mytask_background
                                ),

                            contentDescription =
                                "MyTask",

                            modifier =
                                Modifier
                                    .size(44.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            12.dp
                                        )
                                    )
                        )

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Column {

                            Text(

                                text =
                                    "MyTask",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleLarge,

                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(

                                text =
                                    "Academic Planner",

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )
                        }
                    }
                }
            )
        }

    ) { paddingValues ->

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(
                        horizontal = 16.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            /*
             * =========================================
             * OVERVIEW
             * =========================================
             */

            item {

                Spacer(
                    Modifier.height(4.dp)
                )

                Text(

                    text =
                        "Overview",

                    style =
                        MaterialTheme
                            .typography
                            .headlineSmall,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(4.dp)
                )

                Text(

                    text =
                        "Ringkasan aktivitas akademik kamu.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }

            /*
             * =========================================
             * STATISTIK
             * =========================================
             */

            item {

                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    OverviewCard(
                        title =
                            "Mata Kuliah",

                        value =
                            courseCount.toString(),

                        icon =
                            Icons.Default.MenuBook,

                        modifier =
                            Modifier.weight(1f),

                        onClick =
                            onCoursesClick
                    )

                    Spacer(
                        Modifier.width(12.dp)
                    )

                    OverviewCard(
                        title =
                            "Tugas Aktif",

                        value =
                            activeTaskCount.toString(),

                        icon =
                            Icons.Default.Task,

                        modifier =
                            Modifier.weight(1f),

                        onClick =
                            onTasksClick
                    )
                }
            }

            /*
             * =========================================
             * JADWAL HARI INI
             * =========================================
             */

            item {

                Text(

                    text =
                        "Jadwal Hari Ini",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                TodayScheduleCard(

                    schedules =
                        todaySchedules,

                    courses =
                        courses,

                    onClick =
                        onScheduleClick
                )
            }

            /*
             * =========================================
             * TUGAS MENDATANG
             * =========================================
             */

            item {

                Text(

                    text =
                        "Tugas Mendatang",

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )
            }

            val upcomingTasks =
                tasks
                    .filter {
                        !it.isCompleted
                    }
                    .sortedBy {
                        it.deadline?.time
                            ?: Long.MAX_VALUE
                    }
                    .take(5)

            if (upcomingTasks.isEmpty()) {

                item {
                    EmptyTaskCard()
                }

            } else {

                items(

                    items =
                        upcomingTasks,

                    key = {
                        it.id
                    }

                ) { task ->

                    DashboardTaskCard(

                        task =
                            task,

                        courses =
                            courses,

                        onClick =
                            onTasksClick
                    )
                }
            }

            item {

                Spacer(
                    Modifier.height(24.dp)
                )
            }
        }
    }
}


/*
 * =====================================================
 * OVERVIEW CARD
 * =====================================================
 */

@Composable
private fun OverviewCard(

    title: String,

    value: String,

    icon:
    androidx.compose.ui.graphics.vector.ImageVector,

    modifier: Modifier,

    onClick: () -> Unit

) {

    Card(

        modifier =
            modifier
                .clickable {
                    onClick()
                },

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

        Column(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
        ) {

            Icon(

                imageVector =
                    icon,

                contentDescription =
                    null,

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary,

                modifier =
                    Modifier.size(34.dp)
            )

            Spacer(
                Modifier.height(12.dp)
            )

            Text(

                text =
                    value,

                style =
                    MaterialTheme
                        .typography
                        .headlineMedium,

                fontWeight =
                    FontWeight.Bold
            )

            Text(

                text =
                    title,

                style =
                    MaterialTheme
                        .typography
                        .bodyMedium
            )
        }
    }
}


/*
 * =====================================================
 * JADWAL HARI INI
 * =====================================================
 */

@Composable
private fun TodayScheduleCard(

    schedules:
    List<ScheduleEntity>,

    courses:
    List<CourseEntity>,

    onClick: () -> Unit
) {

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

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

        Column(

            modifier =
                Modifier.padding(18.dp)
        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(

                    imageVector =
                        Icons.Default.Schedule,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    modifier =
                        Modifier.size(30.dp)
                )

                Spacer(
                    Modifier.width(12.dp)
                )

                Column {

                    Text(

                        text =
                            SimpleDateFormat(
                                "EEEE, dd MMMM yyyy",
                                Locale(
                                    "id",
                                    "ID"
                                )
                            ).format(
                                Calendar
                                    .getInstance()
                                    .time
                            ),

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(

                        text =
                            if (
                                schedules.isEmpty()
                            ) {
                                "Tidak ada jadwal hari ini"
                            } else {
                                "${schedules.size} jadwal hari ini"
                            },

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }

            if (
                schedules.isNotEmpty()
            ) {

                Spacer(
                    Modifier.height(16.dp)
                )

                schedules.forEach { schedule ->

                    val course =
                        courses.find {
                            it.id ==
                                    schedule.courseId
                        }

                    TodayScheduleRow(

                        schedule =
                            schedule,

                        courseName =
                            course?.name
                                ?: "Mata Kuliah"
                    )

                    if (
                        schedule !=
                        schedules.last()
                    ) {

                        Spacer(
                            Modifier.height(12.dp)
                        )
                    }
                }
            }
        }
    }
}


/*
 * =====================================================
 * JADWAL ROW
 * =====================================================
 */

@Composable
private fun TodayScheduleRow(

    schedule:
    ScheduleEntity,

    courseName:
    String

) {

    Row(

        modifier =
            Modifier.fillMaxWidth(),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(

            modifier =
                Modifier.width(82.dp)
        ) {

            Text(

                text =
                    schedule.startTime,

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )

            Text(

                text =
                    schedule.endTime,

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }

        Spacer(
            Modifier.width(14.dp)
        )

        Column(

            modifier =
                Modifier.weight(1f)
        ) {

            Text(

                text =
                    courseName,

                style =
                    MaterialTheme
                        .typography
                        .titleMedium,

                fontWeight =
                    FontWeight.Bold
            )

            if (
                schedule.room.isNotBlank()
            ) {

                Text(

                    text =
                        schedule.room,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }
        }
    }
}


/*
 * =====================================================
 * TASK CARD DASHBOARD
 * =====================================================
 */

@Composable
private fun DashboardTaskCard(

    task:
    TaskEntity,

    courses:
    List<CourseEntity>,

    onClick: () -> Unit
) {

    val course =
        courses.find {

            it.id ==
                    task.courseId
        }

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
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClick()
                },

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

        Column(

            modifier =
                Modifier.padding(18.dp)
        ) {

            Row(

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Icon(

                    imageVector =
                        if (
                            task.isCompleted
                        ) {
                            Icons.Default.CheckCircle
                        } else {
                            Icons.Default.RadioButtonUnchecked
                        },

                    contentDescription =
                        "Status tugas",

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )

                Spacer(
                    Modifier.width(10.dp)
                )

                Column(

                    modifier =
                        Modifier.weight(1f)
                ) {

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

                    Text(

                        text =
                            course?.name
                                ?: "Mata Kuliah belum dipilih",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }

            Spacer(
                Modifier.height(10.dp)
            )

            /*
             * INDICATOR STATUS
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

            if (
                task.description.isNotBlank()
            ) {

                Spacer(
                    Modifier.height(8.dp)
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

            if (
                task.deadline != null
            ) {

                Spacer(
                    Modifier.height(8.dp)
                )

                Row(

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.CalendarMonth,

                        contentDescription =
                            null,

                        modifier =
                            Modifier.size(18.dp)
                    )

                    Spacer(
                        Modifier.width(6.dp)
                    )

                    Text(

                        text =
                            "Deadline: ${
                                dateFormat.format(
                                    task.deadline
                                )
                            }",

                        style =
                            MaterialTheme
                                .typography
                                .labelMedium
                    )
                }
            }

            Spacer(
                Modifier.height(8.dp)
            )

            Text(

                text =
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
    }
}


/*
 * =====================================================
 * EMPTY TASK
 * =====================================================
 */

@Composable
private fun EmptyTaskCard() {

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

        Column(

            modifier =
                Modifier.padding(20.dp)
        ) {

            Icon(

                imageVector =
                    Icons.Default.Task,

                contentDescription =
                    null,

                tint =
                    MaterialTheme
                        .colorScheme
                        .primary
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                text =
                    "Tidak ada tugas aktif"
            )

            Text(

                text =
                    "Semua tugas sudah selesai.",

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}