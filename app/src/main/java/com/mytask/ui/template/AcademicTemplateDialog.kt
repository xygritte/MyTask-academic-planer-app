package com.mytask.ui.template

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mytask.data.repository.AppTemplate
import com.mytask.data.repository.TemplateCatalog
import com.mytask.data.repository.TemplateSelectionStore

@Composable
fun AcademicTemplateDialog(
    isApplying: Boolean,
    errorMessage: String?,
    onSkip: () -> Unit,
    onApply: () -> Unit
) {
    val context = LocalContext.current.applicationContext
    val catalog = remember(context) { TemplateCatalog(context) }
    val templates = remember(catalog) { catalog.templates }
    var selectedTemplateIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val selectedTemplates = templates.filter { it.id in selectedTemplateIds }
    val totalCourses = selectedTemplates.sumOf { runCatching { catalog.preview(it).courses }.getOrDefault(0) }
    val totalSchedules = selectedTemplates.sumOf { runCatching { catalog.preview(it).schedules }.getOrDefault(0) }

    AlertDialog(
        onDismissRequest = { if (!isApplying) onSkip() },
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text("Pilih template MyTask", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Pilih satu atau beberapa template sekaligus. Semua pilihan akan menambahkan data baru dan tidak menghapus data yang sudah ada.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    templates.forEach { template ->
                        val preview = runCatching { catalog.preview(template) }
                            .getOrDefault(com.mytask.data.repository.TemplatePreview(0, 0))
                        val selected = template.id in selectedTemplateIds
                        TemplateOptionCard(
                            template = template,
                            courses = preview.courses,
                            schedules = preview.schedules,
                            selected = selected,
                            enabled = !isApplying,
                            onClick = {
                                selectedTemplateIds = if (selected) {
                                    selectedTemplateIds - template.id
                                } else {
                                    selectedTemplateIds + template.id
                                }
                            }
                        )
                    }
                }

                if (selectedTemplates.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "${selectedTemplates.size} template dipilih",
                                fontWeight = FontWeight.Bold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TemplateCount(Icons.Default.MenuBook, totalCourses, "mata kuliah")
                                TemplateCount(Icons.Default.CalendarMonth, totalSchedules, "jadwal")
                            }
                        }
                    }
                } else {
                    Text(
                        "Belum ada template yang dipilih.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    "Template dapat digunakan kembali kapan saja dari Backup & Data.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    TemplateSelectionStore.selectAll(selectedTemplates)
                    onApply()
                },
                enabled = selectedTemplates.isNotEmpty() && !isApplying
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
                    if (isApplying) "Menerapkan..."
                    else "Terapkan ${selectedTemplates.size} template"
                )
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onSkip, enabled = !isApplying) {
                Text("Lewati")
            }
        }
    )
}

@Composable
private fun TemplateOptionCard(
    template: AppTemplate,
    courses: Int,
    schedules: Int,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val borderColor: Color = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            }
        ),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(template.icon, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(template.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(2.dp))
                Text(template.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TemplateCount(Icons.Default.MenuBook, courses, "mata kuliah")
                    TemplateCount(Icons.Default.CalendarMonth, schedules, "jadwal")
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Dipilih",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun TemplateCount(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: Int,
    label: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Text("$value $label", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
