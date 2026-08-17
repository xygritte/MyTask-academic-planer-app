@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.profile

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mytask.data.repository.AppTemplate
import com.mytask.data.repository.TemplateApplyResult
import com.mytask.data.repository.UserDataFile
import java.text.DecimalFormat
import java.util.Date

@Composable
fun BackupScreen(
    onBack: () -> Unit = {},
    viewModel: BackupViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val files by viewModel.files.collectAsStateWithLifecycle()
    var pendingJson by remember { mutableStateOf<String?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var selectedTemplate by remember { mutableStateOf<AppTemplate?>(null) }
    var isApplyingTemplate by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportData(
                onSuccess = { json ->
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(json.toByteArray(Charsets.UTF_8))
                        } ?: error("File tidak dapat ditulis.")
                        runCatching {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        }
                        viewModel.rememberFile(uri)
                        Toast.makeText(context, "Backup berhasil disimpan.", Toast.LENGTH_LONG).show()
                    }.onFailure {
                        Toast.makeText(context, "Gagal menyimpan backup.", Toast.LENGTH_LONG).show()
                    }
                },
                onError = { Toast.makeText(context, it, Toast.LENGTH_LONG).show() }
            )
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: error("File tidak dapat dibaca.")
            }.onSuccess { json ->
                viewModel.rememberFile(uri)
                pendingJson = json
                showImportConfirm = true
            }.onFailure {
                Toast.makeText(context, "Gagal membaca file backup.", Toast.LENGTH_LONG).show()
            }
        }
    }

    val addFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                viewModel.rememberFile(uri)
            }.onFailure {
                Toast.makeText(context, "File tidak dapat ditambahkan.", Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false; pendingJson = null },
            title = { Text("Impor Backup?") },
            text = {
                Text(
                    "Impor backup akan mengganti seluruh data MyTask yang sedang ada. " +
                        "Ini berbeda dengan Template Bawaan yang hanya menambahkan data."
                )
            },
            confirmButton = {
                Button(onClick = {
                    pendingJson?.let { json ->
                        viewModel.importData(
                            json,
                            onSuccess = {
                                showImportConfirm = false
                                pendingJson = null
                                Toast.makeText(context, "Backup berhasil diimpor.", Toast.LENGTH_LONG).show()
                            },
                            onError = {
                                showImportConfirm = false
                                pendingJson = null
                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                }) { Text("Impor") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showImportConfirm = false; pendingJson = null }) {
                    Text("Batal")
                }
            }
        )
    }

    selectedTemplate?.let { template ->
        val state = viewModel.templates.firstOrNull { it.template.id == template.id }
        AlertDialog(
            onDismissRequest = { if (!isApplyingTemplate) selectedTemplate = null },
            title = { Text("${template.icon} ${template.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(template.description)
                    state?.let {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(onClick = {}, label = { Text("${it.courses} mata kuliah") })
                            AssistChip(onClick = {}, label = { Text("${it.tasks} tugas") })
                            AssistChip(onClick = {}, label = { Text("${it.schedules} jadwal") })
                        }
                    }
                    Text(
                        "Data yang sudah ada tidak akan dihapus. Template ditambahkan sebagai data baru dan notification schedule akan disinkronkan.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = !isApplyingTemplate,
                    onClick = {
                        isApplyingTemplate = true
                        viewModel.applyTemplate(
                            template,
                            onSuccess = { result: TemplateApplyResult ->
                                isApplyingTemplate = false
                                selectedTemplate = null
                                val message = if (result.alreadyApplied) {
                                    "Template sudah pernah ditambahkan."
                                } else {
                                    "Template ditambahkan: ${result.addedCourses} mata kuliah, ${result.addedTasks} tugas, ${result.addedSchedules} jadwal."
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            },
                            onError = {
                                isApplyingTemplate = false
                                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                ) { Text(if (isApplyingTemplate) "Menambahkan…" else "Tambahkan") }
            },
            dismissButton = {
                OutlinedButton(enabled = !isApplyingTemplate, onClick = { selectedTemplate = null }) {
                    Text("Batal")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Data", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { paddingValues ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Kelola Data MyTask", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "Gunakan template bawaan, simpan backup, atau kelola file data yang kamu pilih dari perangkat.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Text("✨ Template Bawaan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    viewModel.templates.forEach { state ->
                        TemplateCard(state = state, onClick = { selectedTemplate = state.template })
                    }
                }
            }

            item {
                Text("📦 Backup Data", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { exportLauncher.launch("MyTask_Backup_${Date().time}.json") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ekspor Data")
                    }
                    OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Impor Data")
                    }
                    Text(
                        "Impor Backup mengganti seluruh workspace. Template tidak melakukan penggantian data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("📁 File Data Saya", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Referensi file yang pernah kamu pilih atau ekspor dari MyTask.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { addFileLauncher.launch(arrayOf("image/png", "image/jpeg", "application/json")) }) {
                        Text("Tambah File")
                    }
                }
            }

            if (files.isEmpty()) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.FilePresent, contentDescription = null, modifier = Modifier.size(36.dp))
                            Text("Belum ada file tersimpan", fontWeight = FontWeight.SemiBold)
                            Text("Ekspor backup atau tambahkan PNG, JPG, atau JSON dari perangkat.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(files.size, key = { files[it].uri }) { index ->
                    UserFileCard(
                        file = files[index],
                        onOpen = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(files[index].uri), files[index].mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(intent) }
                                .onFailure { Toast.makeText(context, "Tidak ada aplikasi untuk membuka file ini.", Toast.LENGTH_LONG).show() }
                        },
                        onShare = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = files[index].mimeType
                                putExtra(Intent.EXTRA_STREAM, Uri.parse(files[index].uri))
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            runCatching { context.startActivity(Intent.createChooser(intent, "Bagikan file")) }
                        },
                        onDelete = { viewModel.removeFile(files[index].uri) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateCard(
    state: TemplateCardState,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(260.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(state.template.icon, style = MaterialTheme.typography.headlineMedium)
            Text(state.template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(state.template.description, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            Text(
                "${state.tasks} tugas • ${state.schedules} jadwal",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text("Gunakan Template") }
        }
    }
}

@Composable
private fun UserFileCard(
    file: UserDataFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val isImage = file.mimeType.startsWith("image/")

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isImage) {
                    AsyncImage(
                        model = Uri.parse(file.uri),
                        contentDescription = file.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxSize()) {
                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.padding(16.dp))
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(file.name, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "${formatBytes(file.sizeBytes)} • ${formatDate(file.modifiedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(file.mimeType, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Menu file")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Buka") },
                        leadingIcon = { Icon(Icons.Default.OpenInNew, contentDescription = null) },
                        onClick = { menuOpen = false; onOpen() }
                    )
                    DropdownMenuItem(
                        text = { Text("Bagikan") },
                        leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                        onClick = { menuOpen = false; onShare() }
                    )
                    DropdownMenuItem(
                        text = { Text("Hapus dari daftar") },
                        leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() }
                    )
                }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "Ukuran tidak diketahui"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return "${DecimalFormat("0.#").format(value)} ${units[index]}"
}

private fun formatDate(millis: Long): String = runCatching {
    java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale("id", "ID")).format(Date(millis))
}.getOrDefault("Tanggal tidak diketahui")

@Composable
private fun <T> Flow<T>.collectAsStateWithLifecycle(): androidx.compose.runtime.State<T> =
    androidx.lifecycle.compose.collectAsStateWithLifecycle(this)
