package com.mytask.ui.template

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Task
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AcademicTemplateDialog(
    isApplying: Boolean,
    errorMessage: String?,
    onSkip: () -> Unit,
    onApply: () -> Unit
) {

    AlertDialog(
        onDismissRequest = {
            if (!isApplying) {
                onSkip()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Siapkan MyTask dengan template",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text =
                        "Kami menyediakan data awal agar kamu bisa " +
                                "langsung mencoba semua fitur MyTask.",
                    style = MaterialTheme.typography.bodyMedium
                )

                TemplateStatRow(
                    icon = Icons.Default.MenuBook,
                    value = "7",
                    label = "Mata kuliah"
                )

                TemplateStatRow(
                    icon = Icons.Default.Task,
                    value = "14",
                    label = "Tugas"
                )

                TemplateStatRow(
                    icon = Icons.Default.CalendarMonth,
                    value = "7",
                    label = "Jadwal kuliah"
                )

                Spacer(
                    Modifier.height(2.dp)
                )

                Text(
                    text =
                        "Semua data menggunakan nama universal seperti " +
                                "Mata Kuliah 1, Tugas 1, Dosen 1, dan " +
                                "1 SKS. Data disimpan di perangkat ini.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                enabled = !isApplying
            ) {
                if (isApplying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }

                Text(
                    text = if (isApplying) {
                        "Menerapkan..."
                    } else {
                        "Terapkan template"
                    }
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onSkip,
                enabled = !isApplying
            ) {
                Text("Lewati")
            }
        }
    )
}

@Composable
private fun TemplateStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )

        Spacer(Modifier.width(12.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
