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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
    viewModel: NotificationSettingsViewModel = hiltViewModel()
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
                        text = "Notifikasi",
                        fontWeight = FontWeight.Bold
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

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Spacer(
                Modifier.height(4.dp)
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

                Surface(

                    modifier =
                        Modifier.size(48.dp),

                    shape =
                        RoundedCornerShape(14.dp),

                    color =
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                ) {

                    Icon(

                        imageVector =
                            Icons.Default.Notifications,

                        contentDescription =
                            null,

                        tint =
                            MaterialTheme
                                .colorScheme
                                .onPrimaryContainer,

                        modifier =
                            Modifier
                                .padding(12.dp)
                    )
                }

                Spacer(
                    Modifier.size(12.dp)
                )

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

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
                            "Atur kapan dan apa yang perlu diingatkan.",

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

            /*
             * =========================================
             * DEADLINE REMINDER CARD
             * =========================================
             */

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    MaterialTheme
                        .shapes
                        .large,

                colors =
                    CardDefaults.cardColors(

                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surface
                    )
            ) {

                Column(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Surface(

                            modifier =
                                Modifier.size(40.dp),

                            shape =
                                RoundedCornerShape(12.dp),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .secondaryContainer
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.Notifications,

                                contentDescription =
                                    null,

                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .primary,

                                modifier =
                                    Modifier
                                        .padding(9.dp)
                            )
                        }

                        Spacer(
                            Modifier.size(12.dp)
                        )

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

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

                            Text(

                                text =
                                    "Tetap aktif sampai tugas selesai, " +
                                            "termasuk setelah deadline terlewat.",

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
                    }

                    Spacer(
                        Modifier.height(18.dp)
                    )

                    HorizontalDivider(
                        color =
                            MaterialTheme
                                .colorScheme
                                .outline
                                .copy(alpha = 0.35f)
                    )

                    Spacer(
                        Modifier.height(18.dp)
                    )

                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier =
                                Modifier.weight(1f)
                        ) {

                            Text(

                                text =
                                    "Mulai mengingatkan",

                                style =
                                    MaterialTheme
                                        .typography
                                        .titleMedium,

                                fontWeight =
                                    FontWeight.SemiBold
                            )

                            Spacer(
                                Modifier.height(2.dp)
                            )

                            Text(

                                text =
                                    when {

                                        reminderDays == 0 ->
                                            "Pada hari deadline"

                                        reminderDays == 1 ->
                                            "1 hari sebelum deadline"

                                        else ->
                                            "$reminderDays hari sebelum deadline"
                                    },

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

                        OutlinedButton(

                            onClick = {

                                viewModel
                                    .setTaskReminderDays(
                                        reminderDays - 1
                                    )
                            },

                            enabled =
                                reminderDays > 0,

                            modifier =
                                Modifier.size(44.dp),

                            contentPadding =
                                ButtonDefaults
                                    .ContentPadding
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Remove,

                                contentDescription =
                                    "Kurangi hari"
                            )
                        }

                        Surface(

                            modifier =
                                Modifier
                                    .padding(horizontal = 10.dp)
                                    .size(44.dp),

                            shape =
                                RoundedCornerShape(12.dp),

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primaryContainer
                        ) {

                            BoxNumber(
                                value = reminderDays
                            )
                        }

                        OutlinedButton(

                            onClick = {

                                viewModel
                                    .setTaskReminderDays(
                                        reminderDays + 1
                                    )
                            },

                            enabled =
                                reminderDays < 30,

                            modifier =
                                Modifier.size(44.dp),

                            contentPadding =
                                ButtonDefaults
                                    .ContentPadding
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Default.Add,

                                contentDescription =
                                    "Tambah hari"
                            )
                        }
                    }

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Surface(

                        shape =
                            RoundedCornerShape(10.dp),

                        color =
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                    ) {

                        Row(

                            modifier =
                                Modifier.padding(12.dp),

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Icon(

                                imageVector =
                                    Icons.Default.CheckCircle,

                                contentDescription =
                                    null,

                                tint =
                                    MaterialTheme
                                        .colorScheme
                                        .primary,

                                modifier =
                                    Modifier.size(18.dp)
                            )

                            Spacer(
                                Modifier.size(8.dp)
                            )

                            Text(

                                text =
                                    "Contoh: 3 berarti pengingat mulai " +
                                            "muncul sejak H-3.",

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
                    }
                }
            }

            /*
             * =========================================
             * ACTIVE TASK CARD
             * =========================================
             */

            Card(

                modifier =
                    Modifier.fillMaxWidth(),

                shape =
                    MaterialTheme
                        .shapes
                        .large,

                colors =
                    CardDefaults.cardColors(

                        containerColor =
                            MaterialTheme
                                .colorScheme
                                .surface
                    )
            ) {

                Row(

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(18.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Surface(

                        modifier =
                            Modifier.size(40.dp),

                        shape =
                            RoundedCornerShape(12.dp),

                        color =
                            MaterialTheme
                                .colorScheme
                                .secondaryContainer
                    ) {

                        Icon(

                            imageVector =
                                Icons.Default.Notifications,

                            contentDescription =
                                null,

                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .primary,

                            modifier =
                                Modifier
                                    .padding(9.dp)
                        )
                    }

                    Spacer(
                        Modifier.size(12.dp)
                    )

                    Column(
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        Text(

                            text =
                                "Tugas Aktif",

                            style =
                                MaterialTheme
                                    .typography
                                    .titleMedium,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            Modifier.height(2.dp)
                        )

                        Text(

                            text =
                                "Daftar hingga 4 tugas aktif dalam " +
                                        "satu notifikasi yang dapat di-swipe.",

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

                    Spacer(
                        Modifier.size(8.dp)
                    )

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
            }

            Spacer(
                Modifier.height(4.dp)
            )
        }
    }
}

@Composable
private fun BoxNumber(
    value: Int
) {
    BoxNumberContent(
        value = value
    )
}

@Composable
private fun BoxNumberContent(
    value: Int
) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}
