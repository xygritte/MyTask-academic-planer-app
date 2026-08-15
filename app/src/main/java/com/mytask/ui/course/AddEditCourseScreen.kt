@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.course

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AddEditCourseScreen(
    courseId: Long? = null,
    onBack: () -> Unit = {},
    viewModel: CourseViewModel = hiltViewModel()
) {

    /*
     * Ambil data mata kuliah berdasarkan ID
     */
    val courseFlow = remember(courseId) {

        courseId?.let { id ->
            viewModel.getCourseById(id)
        }
    }

    /*
     * Ambil hasil dari Flow
     */
    val course by if (courseFlow != null) {

        courseFlow.collectAsState()

    } else {

        remember {
            mutableStateOf(null)
        }
    }

    /*
     * State form
     */
    var name by remember {
        mutableStateOf("")
    }

    var code by remember {
        mutableStateOf("")
    }

    var lecturer by remember {
        mutableStateOf("")
    }

    var room by remember {
        mutableStateOf("")
    }

    /*
     * Mencegah data di-reset terus menerus
     */
    var initialized by remember(courseId) {
        mutableStateOf(false)
    }

    /*
     * Isi form otomatis ketika data dari Room sudah tersedia
     */
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

                title = {

                    Text(

                        text =
                            if (courseId == null) {
                                "Tambah Mata Kuliah"
                            } else {
                                "Edit Mata Kuliah"
                            },

                        fontWeight =
                            FontWeight.Bold
                    )
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.ArrowBack,

                            contentDescription =
                                "Kembali"
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            /*
             * NAMA MATA KULIAH
             */

            OutlinedTextField(

                value = name,

                onValueChange = {
                    name = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Nama Mata Kuliah")
                },

                singleLine = true
            )

            /*
             * KODE
             */

            OutlinedTextField(

                value = code,

                onValueChange = {
                    code = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Kode Mata Kuliah/jumlah SKS")
                },

                singleLine = true
            )

            /*
             * DOSEN
             */

            OutlinedTextField(

                value = lecturer,

                onValueChange = {
                    lecturer = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Dosen")
                },

                singleLine = true
            )

            /*
             * RUANGAN
             */

            OutlinedTextField(

                value = room,

                onValueChange = {
                    room = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Ruangan")
                },

                singleLine = true
            )

            /*
             * SIMPAN / UPDATE
             */

            Button(

                onClick = {

                    if (name.isNotBlank()) {

                        /*
                         * MODE TAMBAH
                         */

                        if (course == null) {

                            viewModel.addCourse(

                                name =
                                    name,

                                code =
                                    code,

                                lecturer =
                                    lecturer,

                                room =
                                    room,

                                onSaved =
                                    onBack
                            )

                        } else {

                            /*
                             * MODE EDIT
                             */

                            viewModel.updateCourse(

                                course!!.copy(

                                    name =
                                        name,

                                    code =
                                        code,

                                    lecturer =
                                        lecturer,

                                    room =
                                        room
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
                    name.isNotBlank()
            ) {

                Text(

                    if (courseId == null) {
                        "Simpan Mata Kuliah"
                    } else {
                        "Update Mata Kuliah"
                    }
                )
            }
        }
    }
}