@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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

@Composable
fun NotificationSettingsScreen(
    onBack: () -> Unit = {},
    viewModel: NotificationSettingsViewModel =
        hiltViewModel()
) {

    val reminderDays by
    viewModel
        .taskReminderDays
        .collectAsState()

    val activeTaskNotification by
    viewModel
        .activeTaskNotification
        .collectAsState()

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Text(
                        "Notifikasi"
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
                Modifier.height(8.dp)
            )

            /*
             * =========================================
             * HEADER
             * =========================================
             */

            Row(

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Icon(

                    imageVector =
                        Icons.Default
                            .Notifications,

                    contentDescription =
                        null,

                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary,

                    modifier =
                        Modifier.size(
                            36.dp
                        )
                )

                Spacer(
                    Modifier.size(
                        12.dp
                    )
                )

                Column {

                    Text(

                        text =
                            "Pengaturan Notifikasi",

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge,

                        fontWeight =
                            FontWeight.Bold
                    )

                    Text(

                        text =
                            "Atur bagaimana MyTask mengingatkan kamu.",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }

            HorizontalDivider()

            /*
             * =========================================
             * PENGINGAT DEADLINE
             * =========================================
             */

            Column {

                Text(

                    text =
                        "Pengingat Deadline",

                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,

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
                        "Notifikasi tugas akan mulai muncul " +
                                "beberapa hari sebelum deadline dan " +
                                "tetap ada setelah deadline sampai " +
                                "tugas dinyatakan selesai.",

                    style =
                        MaterialTheme
                            .typography
                            .bodyMedium
                )
            }

            /*
             * =========================================
             * JUMLAH HARI
             * =========================================
             */

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Column(

                    modifier =
                        Modifier.weight(
                            1f
                        )

                ) {

                    Text(

                        text =
                            "Mulai mengingatkan",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium
                    )

                    Text(

                        text =
                            if (
                                reminderDays == 0
                            ) {

                                "Pada hari deadline"

                            } else {

                                "$reminderDays " +
                                        "hari sebelum deadline"
                            },

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                OutlinedButton(

                    onClick = {

                        viewModel
                            .setTaskReminderDays(
                                reminderDays - 1
                            )
                    },

                    enabled =
                        reminderDays > 0

                ) {

                    Icon(

                        imageVector =
                            Icons.Default
                                .Remove,

                        contentDescription =
                            "Kurangi"
                    )
                }

                Text(

                    text =
                        "$reminderDays",

                    modifier =
                        Modifier.padding(
                            horizontal = 16.dp
                        ),

                    style =
                        MaterialTheme
                            .typography
                            .titleLarge,

                    fontWeight =
                        FontWeight.Bold
                )

                OutlinedButton(

                    onClick = {

                        viewModel
                            .setTaskReminderDays(
                                reminderDays + 1
                            )
                    },

                    enabled =
                        reminderDays < 30

                ) {

                    Icon(

                        imageVector =
                            Icons.Default
                                .Add,

                        contentDescription =
                            "Tambah"
                    )
                }
            }

            HorizontalDivider()

            /*
             * =========================================
             * TUGAS AKTIF
             * =========================================
             */

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically

            ) {

                Column(

                    modifier =
                        Modifier.weight(
                            1f
                        )

                ) {

                    Text(

                        text =
                            "Tugas Aktif",

                        style =
                            MaterialTheme
                                .typography
                                .titleMedium,

                        fontWeight =
                            FontWeight.SemiBold
                    )

                    Text(

                        text =
                            "Tampilkan notifikasi yang " +
                                    "berisi daftar tugas aktif.",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }

                Switch(

                    checked =
                        activeTaskNotification,

                    onCheckedChange = { enabled ->

                        viewModel
                            .setActiveTaskNotification(
                                enabled
                            )
                    }
                )
            }

            Spacer(
                Modifier.height(
                    8.dp
                )
            )

            Text(

                text =
                    "Notifikasi Tugas Aktif dapat dihapus " +
                            "dengan swipe. Pengaturan ini tidak " +
                            "memengaruhi pengingat deadline.",

                style =
                    MaterialTheme
                        .typography
                        .bodySmall
            )
        }
    }
}