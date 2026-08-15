@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.task

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    val taskFlow = remember(taskId) {
        taskId?.let {
            viewModel.getTaskById(it)
        }
    }

    val task by if (taskFlow != null) {
        taskFlow.collectAsState()
    } else {
        remember {
            mutableStateOf(null)
        }
    }

    var selectedCourseId by remember {
        mutableStateOf<Long?>(null)
    }

    var selectedCourseName by remember {
        mutableStateOf("")
    }

    var courseMenuExpanded by remember {
        mutableStateOf(false)
    }

    var title by remember {
        mutableStateOf("")
    }

    var description by remember {
        mutableStateOf("")
    }

    var priority by remember {
        mutableIntStateOf(1)
    }

    var deadline by remember {
        mutableStateOf<Date?>(null)
    }

    var initialized by remember {
        mutableStateOf(false)
    }

    val context = LocalContext.current

    val dateFormat = remember {
        SimpleDateFormat(
            "dd MMMM yyyy",
            Locale("id", "ID")
        )
    }

    /*
     * Isi form otomatis ketika mode EDIT.
     */
    LaunchedEffect(task, courses) {

        if (task != null && !initialized) {

            selectedCourseId = task!!.courseId

            selectedCourseName =
                courses.find {
                    it.id == task!!.courseId
                }?.name ?: ""

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

                title = {

                    Text(
                        if (taskId == null) {
                            "Tambah Tugas"
                        } else {
                            "Edit Tugas"
                        }
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            /*
             * MATA KULIAH
             */

            Text(
                text = "Mata Kuliah"
            )

            Button(

                onClick = {
                    courseMenuExpanded = true
                },

                modifier = Modifier.fillMaxWidth(),

                enabled = courses.isNotEmpty()
            ) {

                Text(
                    if (selectedCourseName.isBlank()) {
                        "Pilih Mata Kuliah"
                    } else {
                        selectedCourseName
                    }
                )
            }

            DropdownMenu(

                expanded = courseMenuExpanded,

                onDismissRequest = {
                    courseMenuExpanded = false
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

                            selectedCourseId =
                                course.id

                            selectedCourseName =
                                course.name

                            courseMenuExpanded =
                                false
                        }
                    )
                }
            }

            /*
             * NAMA TUGAS
             */

            OutlinedTextField(

                value = title,

                onValueChange = {
                    title = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Nama Tugas")
                },

                singleLine = true
            )

            /*
             * DESKRIPSI
             */

            OutlinedTextField(

                value = description,

                onValueChange = {
                    description = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Deskripsi")
                },

                minLines = 3
            )

            /*
             * DEADLINE
             */

            Button(

                onClick = {

                    val calendar =
                        Calendar.getInstance()

                    if (deadline != null) {
                        calendar.time = deadline!!
                    }

                    DatePickerDialog(

                        context,

                        { _, year, month, dayOfMonth ->

                            deadline =
                                Calendar.getInstance().apply {

                                    set(
                                        Calendar.YEAR,
                                        year
                                    )

                                    set(
                                        Calendar.MONTH,
                                        month
                                    )

                                    set(
                                        Calendar.DAY_OF_MONTH,
                                        dayOfMonth
                                    )

                                    set(
                                        Calendar.HOUR_OF_DAY,
                                        23
                                    )

                                    set(
                                        Calendar.MINUTE,
                                        59
                                    )

                                    set(
                                        Calendar.SECOND,
                                        59
                                    )

                                    set(
                                        Calendar.MILLISECOND,
                                        999
                                    )
                                }.time
                        },

                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)

                    ).show()
                },

                modifier =
                    Modifier.fillMaxWidth()
            ) {

                Text(

                    if (deadline != null) {

                        dateFormat.format(
                            deadline!!
                        )

                    } else {

                        "Pilih Deadline"
                    }
                )
            }

            /*
             * PRIORITAS
             */

            Text(
                text = "Prioritas"
            )

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Button(

                    onClick = {
                        priority = 1
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        if (priority == 1) {
                            "✓ Rendah"
                        } else {
                            "Rendah"
                        }
                    )
                }

                Button(

                    onClick = {
                        priority = 2
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        if (priority == 2) {
                            "✓ Sedang"
                        } else {
                            "Sedang"
                        }
                    )
                }

                Button(

                    onClick = {
                        priority = 3
                    },

                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        if (priority == 3) {
                            "✓ Tinggi"
                        } else {
                            "Tinggi"
                        }
                    )
                }
            }

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            /*
             * SIMPAN / UPDATE
             */

            Button(

                onClick = {

                    if (
                        selectedCourseId != null &&
                        title.isNotBlank()
                    ) {

                        if (task == null) {

                            /*
                             * MODE TAMBAH
                             */

                            viewModel.addTask(

                                courseId =
                                    selectedCourseId,

                                title =
                                    title,

                                description =
                                    description,

                                priority =
                                    priority,

                                deadline =
                                    deadline,

                                onSaved =
                                    onBack
                            )

                        } else {

                            /*
                             * MODE EDIT
                             */

                            viewModel.updateTask(

                                task!!.copy(

                                    courseId =
                                        selectedCourseId,

                                    title =
                                        title,

                                    description =
                                        description,

                                    priority =
                                        priority,

                                    deadline =
                                        deadline
                                ),

                                onSaved =
                                    onBack
                            )
                        }
                    }
                },

                modifier =
                    Modifier.fillMaxWidth(),

                enabled =
                    selectedCourseId != null &&
                            title.isNotBlank()
            ) {

                Text(

                    if (taskId == null) {
                        "Simpan Tugas"
                    } else {
                        "Update Tugas"
                    }
                )
            }
        }
    }
}