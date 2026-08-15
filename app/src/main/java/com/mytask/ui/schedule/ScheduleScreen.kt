@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.schedule

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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
    viewModel: ScheduleViewModel = hiltViewModel()
) {

    val schedules by viewModel.schedules.collectAsState()
    val courses by viewModel.courses.collectAsState()

    var editingScheduleId by remember {
        mutableStateOf<Long?>(null)
    }

    Scaffold(

        topBar = {

            TopAppBar(
                title = {
                    Text(
                        "Jadwal Kuliah"
                    )
                }
            )
        },

        floatingActionButton = {

            FloatingActionButton(

                onClick = {
                    editingScheduleId = -1L
                }

            ) {

                Icon(
                    Icons.Default.Add,
                    contentDescription =
                        "Tambah Jadwal"
                )
            }
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)

        ) {

            /*
             * =========================================
             * FORM TAMBAH / EDIT
             * =========================================
             */

            if (
                editingScheduleId != null
            ) {

                ScheduleForm(

                    scheduleId =
                        editingScheduleId
                            ?.takeIf {
                                it != -1L
                            },

                    courses =
                        courses,

                    viewModel =
                        viewModel,

                    onCancel = {
                        editingScheduleId = null
                    },

                    onSaved = {
                        editingScheduleId = null
                    }
                )
            }

            /*
             * =========================================
             * DAFTAR JADWAL
             * =========================================
             */

            if (
                schedules.isEmpty()
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(24.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    verticalArrangement =
                        Arrangement.Center

                ) {

                    Icon(

                        Icons.Default.Schedule,

                        contentDescription =
                            null
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    Text(
                        "Belum ada jadwal"
                    )
                }

            } else {

                LazyColumn(

                    modifier =
                        Modifier
                            .fillMaxSize()
                            .padding(
                                horizontal = 16.dp
                            ),

                    /*
                     * Ruang ekstra di bagian bawah
                     * agar card terakhir tidak tertutup FAB.
                     */
                    contentPadding =
                        PaddingValues(
                            bottom = 96.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)

                ) {

                    items(

                        schedules,

                        key = {
                            it.id
                        }

                    ) { schedule ->

                        ScheduleCard(

                            schedule =
                                schedule,

                            courses =
                                courses,

                            onEdit = {

                                editingScheduleId =
                                    schedule.id
                            },

                            onDelete = {

                                viewModel
                                    .deleteSchedule(
                                        schedule
                                    )
                            }
                        )
                    }
                }
            }
        }
    }
}


/*
 * =====================================================
 * FORM TAMBAH / EDIT JADWAL
 * =====================================================
 */

@Composable
private fun ScheduleForm(

    scheduleId:
    Long?,

    courses:
    List<CourseEntity>,

    viewModel:
    ScheduleViewModel,

    onCancel:
        () -> Unit,

    onSaved:
        () -> Unit

) {

    /*
     * Hanya membuat flow baru ketika ID berubah.
     */
    val scheduleFlow =
        remember(scheduleId) {

            scheduleId?.let { id ->

                viewModel.getScheduleById(
                    id
                )
            }
        }

    /*
     * Data jadwal dari Room.
     */
    val schedule by (

            scheduleFlow
                ?.collectAsState()

                ?: remember {

                    mutableStateOf(
                        null
                    )
                }
            )

    /*
     * =========================================
     * STATE FORM
     * =========================================
     */

    var selectedCourse by
    remember(scheduleId) {

        mutableStateOf<CourseEntity?>(
            null
        )
    }

    var courseExpanded by
    remember(scheduleId) {

        mutableStateOf(false)
    }

    var day by
    remember(scheduleId) {

        mutableStateOf(2)
    }

    var dayName by
    remember(scheduleId) {

        mutableStateOf("Senin")
    }

    var dayExpanded by
    remember(scheduleId) {

        mutableStateOf(false)
    }

    var startTime by
    remember(scheduleId) {

        mutableStateOf("08:00")
    }

    var endTime by
    remember(scheduleId) {

        mutableStateOf("10:00")
    }

    var room by
    remember(scheduleId) {

        mutableStateOf("")
    }


    /*
     * =========================================
     * LOAD DATA EDIT
     * =========================================
     */

    LaunchedEffect(

        scheduleId,

        schedule,

        courses

    ) {

        /*
         * Mode tambah.
         */
        if (
            scheduleId == null
        ) {

            return@LaunchedEffect
        }

        /*
         * Tunggu data dari Room.
         */
        val currentSchedule =
            schedule
                ?: return@LaunchedEffect

        /*
         * MATA KULIAH
         */
        selectedCourse =
            courses.find { course ->

                course.id ==
                        currentSchedule.courseId
            }

        /*
         * HARI
         */
        day =
            currentSchedule.dayOfWeek

        dayName =
            when (
                currentSchedule.dayOfWeek
            ) {

                1 ->
                    "Minggu"

                2 ->
                    "Senin"

                3 ->
                    "Selasa"

                4 ->
                    "Rabu"

                5 ->
                    "Kamis"

                6 ->
                    "Jumat"

                7 ->
                    "Sabtu"

                else ->
                    "Senin"
            }

        /*
         * JAM MULAI
         */
        startTime =
            currentSchedule.startTime

        /*
         * JAM SELESAI
         */
        endTime =
            currentSchedule.endTime

        /*
         * RUANGAN
         */
        room =
            currentSchedule.room
    }


    /*
     * =========================================
     * FORM UI
     * =========================================
     */

    Card(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp)

    ) {

        Column(

            modifier =
                Modifier
                    .padding(16.dp)

        ) {

            Text(

                text =
                    if (
                        scheduleId == null
                    ) {
                        "Tambah Jadwal"
                    } else {
                        "Edit Jadwal"
                    },

                style =
                    MaterialTheme
                        .typography
                        .titleLarge
            )

            Spacer(
                Modifier.height(12.dp)
            )


            /*
             * =========================================
             * MATA KULIAH
             * =========================================
             */

            OutlinedTextField(

                value =
                    selectedCourse?.name
                        ?: "Pilih Mata Kuliah",

                onValueChange = {},

                readOnly = true,

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text(
                        "Mata Kuliah"
                    )
                }
            )

            Button(

                onClick = {

                    courseExpanded =
                        true
                },

                modifier =
                    Modifier.fillMaxWidth(),

                enabled =
                    courses.isNotEmpty()

            ) {

                Text(

                    selectedCourse?.name
                        ?: "Pilih Mata Kuliah"
                )
            }

            DropdownMenu(

                expanded =
                    courseExpanded,

                onDismissRequest = {

                    courseExpanded =
                        false
                }

            ) {

                courses.forEach { course ->

                    DropdownMenuItem(

                        text = {

                            Text(
                                "${course.code} - ${course.name}"
                            )
                        },

                        onClick = {

                            selectedCourse =
                                course

                            courseExpanded =
                                false
                        }
                    )
                }
            }


            Spacer(
                Modifier.height(8.dp)
            )


            /*
             * =========================================
             * HARI
             * =========================================
             */

            OutlinedTextField(

                value =
                    dayName,

                onValueChange = {},

                readOnly = true,

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text(
                        "Hari"
                    )
                }
            )

            Button(

                onClick = {

                    dayExpanded =
                        true
                },

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Text(
                    dayName
                )
            }

            DropdownMenu(

                expanded =
                    dayExpanded,

                onDismissRequest = {

                    dayExpanded =
                        false
                }

            ) {

                listOf(

                    1 to "Minggu",
                    2 to "Senin",
                    3 to "Selasa",
                    4 to "Rabu",
                    5 to "Kamis",
                    6 to "Jumat",
                    7 to "Sabtu"

                ).forEach { item ->

                    DropdownMenuItem(

                        text = {

                            Text(
                                item.second
                            )
                        },

                        onClick = {

                            day =
                                item.first

                            dayName =
                                item.second

                            dayExpanded =
                                false
                        }
                    )
                }
            }


            Spacer(
                Modifier.height(8.dp)
            )


            /*
             * =========================================
             * JAM
             * =========================================
             */

            Row(

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                OutlinedTextField(

                    value =
                        startTime,

                    onValueChange = {

                        startTime =
                            it
                    },

                    modifier =
                        Modifier.weight(1f),

                    label = {
                        Text(
                            "Mulai"
                        )
                    },

                    singleLine = true
                )

                Spacer(
                    Modifier.padding(4.dp)
                )

                OutlinedTextField(

                    value =
                        endTime,

                    onValueChange = {

                        endTime =
                            it
                    },

                    modifier =
                        Modifier.weight(1f),

                    label = {
                        Text(
                            "Selesai"
                        )
                    },

                    singleLine = true
                )
            }


            Spacer(
                Modifier.height(8.dp)
            )


            /*
             * =========================================
             * RUANGAN
             * =========================================
             */

            OutlinedTextField(

                value =
                    room,

                onValueChange = {

                    room =
                        it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text(
                        "Ruangan"
                    )
                }
            )


            Spacer(
                Modifier.height(16.dp)
            )


            /*
             * =========================================
             * BUTTON
             * =========================================
             */

            Row(

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Button(

                    onClick =
                        onCancel,

                    modifier =
                        Modifier.weight(1f)

                ) {

                    Text(
                        "Batal"
                    )
                }

                Spacer(
                    Modifier.padding(4.dp)
                )

                Button(

                    onClick = {

                        /*
                         * Mata kuliah wajib.
                         */
                        val course =
                            selectedCourse
                                ?: return@Button

                        /*
                         * TAMBAH
                         */
                        if (
                            scheduleId == null
                        ) {

                            viewModel.addSchedule(

                                courseId =
                                    course.id,

                                dayOfWeek =
                                    day,

                                startTime =
                                    startTime,

                                endTime =
                                    endTime,

                                room =
                                    room,

                                onSaved =
                                    onSaved
                            )

                        } else {

                            /*
                             * EDIT
                             */
                            val currentSchedule =
                                schedule
                                    ?: return@Button

                            val updatedSchedule =
                                currentSchedule.copy(

                                    /*
                                     * ID lama
                                     * dipertahankan.
                                     */
                                    id =
                                        currentSchedule.id,

                                    courseId =
                                        course.id,

                                    dayOfWeek =
                                        day,

                                    startTime =
                                        startTime,

                                    endTime =
                                        endTime,

                                    room =
                                        room
                                )

                            viewModel.updateSchedule(

                                updatedSchedule,

                                onSaved =
                                    onSaved
                            )
                        }
                    },

                    modifier =
                        Modifier.weight(1f),

                    enabled =
                        selectedCourse != null

                ) {

                    Text(

                        if (
                            scheduleId == null
                        ) {
                            "Simpan"
                        } else {
                            "Update"
                        }
                    )
                }
            }
        }
    }
}


/*
 * =====================================================
 * SCHEDULE CARD
 * =====================================================
 */

@Composable
private fun ScheduleCard(

    schedule:
    ScheduleEntity,

    courses:
    List<CourseEntity>,

    onEdit:
        () -> Unit,

    onDelete:
        () -> Unit

) {

    val course =
        courses.find {

            it.id ==
                    schedule.courseId
        }

    val dayName =
        when (
            schedule.dayOfWeek
        ) {

            1 ->
                "Minggu"

            2 ->
                "Senin"

            3 ->
                "Selasa"

            4 ->
                "Rabu"

            5 ->
                "Kamis"

            6 ->
                "Jumat"

            7 ->
                "Sabtu"

            else ->
                "-"
        }

    Card(

        modifier =
            Modifier.fillMaxWidth()

    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically

        ) {

            Column(

                modifier =
                    Modifier.weight(1f)

            ) {

                Text(

                    course?.name
                        ?: "Mata Kuliah"
                )

                Text(
                    dayName
                )

                Text(

                    "${schedule.startTime} - ${schedule.endTime}"
                )

                if (
                    schedule.room.isNotBlank()
                ) {

                    Text(

                        schedule.room,

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }

            IconButton(

                onClick =
                    onEdit

            ) {

                Icon(

                    Icons.Default.Edit,

                    contentDescription =
                        "Edit"
                )
            }

            IconButton(

                onClick =
                    onDelete

            ) {

                Icon(

                    Icons.Default.Delete,

                    contentDescription =
                        "Hapus"
                )
            }
        }
    }
}