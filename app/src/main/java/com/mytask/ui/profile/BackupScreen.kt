@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.profile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun BackupScreen(

    onBack:
        () -> Unit = {},

    viewModel:
    BackupViewModel =
        hiltViewModel()

) {

    val context =
        LocalContext.current

    var pendingJson by
    remember {

        mutableStateOf<String?>(
            null
        )
    }

    var showImportConfirm by
    remember {

        mutableStateOf(
            false
        )
    }


    /*
     * =========================================
     * EXPORT
     * =========================================
     */

    val exportLauncher =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts
                    .CreateDocument(
                        "application/json"
                    )

        ) { uri ->

            if (uri != null) {

                viewModel.exportData(

                    onSuccess = { json ->

                        try {

                            context
                                .contentResolver
                                .openOutputStream(
                                    uri
                                )
                                ?.use { output ->

                                    output.write(

                                        json.toByteArray(
                                            Charsets.UTF_8
                                        )
                                    )
                                }

                            Toast.makeText(

                                context,

                                "Backup berhasil disimpan.",

                                Toast.LENGTH_LONG

                            ).show()

                        } catch (
                            e:
                            Exception
                        ) {

                            Toast.makeText(

                                context,

                                "Gagal menyimpan backup.",

                                Toast.LENGTH_LONG

                            ).show()
                        }
                    },

                    onError = { error ->

                        Toast.makeText(

                            context,

                            error,

                            Toast.LENGTH_LONG

                        ).show()
                    }
                )
            }
        }


    /*
     * =========================================
     * IMPORT
     * =========================================
     */

    val importLauncher =
        rememberLauncherForActivityResult(

            contract =
                ActivityResultContracts
                    .OpenDocument()

        ) { uri ->

            if (uri != null) {

                try {

                    val json =
                        context
                            .contentResolver
                            .openInputStream(
                                uri
                            )
                            ?.bufferedReader()
                            ?.use {
                                it.readText()
                            }

                    if (json != null) {

                        pendingJson =
                            json

                        showImportConfirm =
                            true
                    }

                } catch (
                    e:
                    Exception
                ) {

                    Toast.makeText(

                        context,

                        "Gagal membaca file backup.",

                        Toast.LENGTH_LONG

                    ).show()
                }
            }
        }


    /*
     * =========================================
     * IMPORT CONFIRMATION
     * =========================================
     */

    if (
        showImportConfirm
    ) {

        AlertDialog(

            onDismissRequest = {

                showImportConfirm =
                    false

                pendingJson =
                    null
            },

            title = {

                Text(
                    "Impor Backup?"
                )
            },

            text = {

                Text(

                    """
                    Data MyTask yang sekarang akan diganti
                    dengan isi backup.

                    Pastikan file backup benar sebelum melanjutkan.
                    """.trimIndent()
                )
            },

            confirmButton = {

                Button(

                    onClick = {

                        val json =
                            pendingJson

                        if (
                            json != null
                        ) {

                            viewModel.importData(

                                json =
                                    json,

                                onSuccess = {

                                    showImportConfirm =
                                        false

                                    pendingJson =
                                        null

                                    Toast.makeText(

                                        context,

                                        "Backup berhasil diimpor.",

                                        Toast.LENGTH_LONG

                                    ).show()
                                },

                                onError = { error ->

                                    showImportConfirm =
                                        false

                                    pendingJson =
                                        null

                                    Toast.makeText(

                                        context,

                                        error,

                                        Toast.LENGTH_LONG

                                    ).show()
                                }
                            )
                        }
                    }

                ) {

                    Text(
                        "Impor"
                    )
                }
            },

            dismissButton = {

                OutlinedButton(

                    onClick = {

                        showImportConfirm =
                            false

                        pendingJson =
                            null
                    }

                ) {

                    Text(
                        "Batal"
                    )
                }
            }
        )
    }


    /*
     * =========================================
     * SCREEN
     * =========================================
     */

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        "Backup & Data"
                    )
                },

                navigationIcon = {

                    IconButton(

                        onClick =
                            onBack

                    ) {

                        Icon(

                            imageVector =
                                Icons.Default
                                    .ArrowBack,

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
                    .padding(
                        paddingValues
                    )
                    .padding(
                        horizontal = 16.dp
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    16.dp
                )
        ) {

            Spacer(
                Modifier.height(
                    8.dp
                )
            )

            Text(
                text =
                    "Kelola Data MyTask",

                style =
                    androidx.compose.material3
                        .MaterialTheme
                        .typography
                        .headlineSmall
            )

            Text(

                text =
                    "Simpan data MyTask ke file JSON " +
                            "untuk dipindahkan atau digunakan " +
                            "sebagai cadangan.",

                style =
                    androidx.compose.material3
                        .MaterialTheme
                        .typography
                        .bodyMedium
            )


            /*
             * =========================================
             * EXPORT
             * =========================================
             */

            Button(

                onClick = {

                    exportLauncher.launch(
                        "MyTask_Backup.json"
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Icon(

                    imageVector =
                        Icons.Default
                            .CloudDownload,

                    contentDescription =
                        null
                )

                Text(
                    "  Ekspor Data"
                )
            }


            /*
             * =========================================
             * IMPORT
             * =========================================
             */

            OutlinedButton(

                onClick = {

                    importLauncher.launch(

                        arrayOf(

                            "application/json",

                            "text/plain"
                        )
                    )
                },

                modifier =
                    Modifier.fillMaxWidth()

            ) {

                Icon(

                    imageVector =
                        Icons.Default
                            .CloudUpload,

                    contentDescription =
                        null
                )

                Text(
                    "  Impor Data"
                )
            }


            /*
             * =========================================
             * INFO
             * =========================================
             */

            Text(

                text =
                    "Backup mencakup Mata Kuliah, Tugas, " +
                            "deadline, prioritas, status tugas, " +
                            "dan Jadwal.",

                style =
                    androidx.compose.material3
                        .MaterialTheme
                        .typography
                        .bodySmall
            )

            Text(

                text =
                    "Catatan: impor backup akan mengganti " +
                            "data yang sedang ada di aplikasi.",

                style =
                    androidx.compose.material3
                        .MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}