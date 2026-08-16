@file:OptIn(ExperimentalMaterial3Api::class)

package com.mytask.ui.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mytask.R
import com.mytask.data.repository.UserProfile
import com.mytask.data.repository.UserProfileRepository
import com.mytask.debug.AuthDebugLog
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ProfileScreen(
    profile: UserProfile,
    canSaveOnline: Boolean = false,
    isSavingOnline: Boolean = false,
    onlineSaveMessage: String? = null,
    onBack: () -> Unit = {},
    onNotificationSettings: () -> Unit = {},
    onBackupData: () -> Unit = {},
    onEditProfile: () -> Unit = {},
    onSaveDataOnline: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val userProfileRepository = remember(context) {
        UserProfileRepository(context)
    }
    val storedProfile by userProfileRepository.profile.collectAsState(initial = profile)
    val storedPhotoUri by userProfileRepository.profilePhotoUri.collectAsState(initial = null)
    val displayedProfile = storedProfile ?: profile
    val firebasePhotoUri = FirebaseAuth.getInstance().currentUser?.photoUrl?.toString()
    val photoModel = storedPhotoUri ?: firebasePhotoUri

    var showEditDialog by remember { mutableStateOf(false) }
    var isUpdatingProfile by remember { mutableStateOf(false) }
    var editError by remember { mutableStateOf<String?>(null) }
    var editSuccess by remember { mutableStateOf<String?>(null) }
    var isCroppingPhoto by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isCroppingPhoto = false

        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val outputUri = result.data?.let { UCrop.getOutput(it) }
            if (outputUri != null) {
                scope.launch {
                    runCatching {
                        userProfileRepository.saveProfilePhotoUri(outputUri.toString())
                    }.onSuccess {
                        photoError = null
                        AuthDebugLog.d("PROFILE_PHOTO cropped and saved")
                    }.onFailure { error ->
                        photoError = error.message ?: "Foto profil gagal disimpan."
                        AuthDebugLog.e("PROFILE_PHOTO save failed", error)
                    }
                }
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR) {
            val error = result.data?.let { UCrop.getError(it) }
            photoError = error?.message ?: "Foto profil gagal dipotong."
            AuthDebugLog.e("PROFILE_PHOTO crop failed", error)
        }
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        try {
            val outputFile = File(context.filesDir, "profile_photo.jpg")
            outputFile.parentFile?.mkdirs()
            outputFile.delete()

            val options = UCrop.Options().apply {
                setCircleDimmedLayer(true)
                setShowCropFrame(true)
                setShowCropGrid(true)
                setCompressionQuality(90)
            }

            val cropIntent = UCrop
                .of(uri, Uri.fromFile(outputFile))
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(1024, 1024)
                .withOptions(options)
                .getIntent(context)

            isCroppingPhoto = true
            photoError = null
            cropLauncher.launch(cropIntent)
            AuthDebugLog.d("PROFILE_PHOTO picker result received; crop launched")
        } catch (error: Throwable) {
            isCroppingPhoto = false
            photoError = error.message ?: "Foto profil gagal diproses."
            AuthDebugLog.e("PROFILE_PHOTO crop launch failed", error)
        }
    }

    fun launchPhotoPicker() {
        photoPicker.launch(
            PickVisualMediaRequest(
                ActivityResultContracts.PickVisualMedia.ImageOnly
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profil",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(104.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .clickable(
                                    enabled = !isCroppingPhoto,
                                    onClick = ::launchPhotoPicker
                                ),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            if (photoModel.isNullOrBlank()) {
                                Image(
                                    painter = painterResource(
                                        id = R.mipmap.mytask_background
                                    ),
                                    contentDescription = "Foto profil",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            } else {
                                AsyncImage(
                                    model = photoModel,
                                    contentDescription = "Foto profil",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                )
                            }
                        }

                        if (isCroppingPhoto) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(28.dp),
                                    strokeWidth = 3.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(34.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 4.dp
                        ) {
                            IconButton(
                                onClick = ::launchPhotoPicker,
                                enabled = !isCroppingPhoto,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Ubah foto profil",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = displayedProfile.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = displayedProfile.program,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Mahasiswa",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(
                                horizontal = 8.dp,
                                vertical = 5.dp
                            )
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            editError = null
                            editSuccess = null
                            showEditDialog = true
                        },
                        enabled = !isUpdatingProfile && !isCroppingPhoto
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text("Edit profil")
                    }
                }
            }

            photoError?.let { message ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            editSuccess?.let { message ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            ProfileMenuCard(
                icon = Icons.Default.Notifications,
                title = "Notifikasi",
                description = "Atur pengingat deadline dan tugas aktif.",
                onClick = onNotificationSettings
            )

            ProfileMenuCard(
                icon = Icons.Default.CloudUpload,
                title = "Simpan data ke online",
                description = if (canSaveOnline) {
                    "Upload tugas, jadwal, dan mata kuliah ke akun kamu."
                } else {
                    "Tersedia setelah login menggunakan akun."
                },
                onClick = onSaveDataOnline,
                enabled = canSaveOnline && !isSavingOnline
            )

            onlineSaveMessage?.let { message ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            ProfileMenuCard(
                icon = Icons.Default.Cloud,
                title = "Backup & Data",
                description = "Ekspor dan impor data MyTask.",
                onClick = onBackupData
            )

            ProfileMenuCard(
                icon = Icons.Default.Logout,
                title = "Keluar",
                description = "Keluar dari akun MyTask di perangkat ini.",
                onClick = onLogout,
                destructive = true,
                enabled = !isSavingOnline && !isUpdatingProfile && !isCroppingPhoto
            )

            Spacer(Modifier.height(4.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            Text(
                text = "Tentang",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            AboutRow(
                icon = Icons.Default.Person,
                label = "MyTask Academic Planner\napp by Furqon Ramadhani"
            )

            ProfileMenuCard(
                icon = Icons.Default.Person,
                title = "Instagram",
                description = "@ramadhani.frqn",
                onClick = {
                    context.startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.instagram.com/ramadhani.frqn")
                        )
                    )
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showEditDialog) {
        EditProfileDialog(
            initialName = displayedProfile.name,
            initialProgram = displayedProfile.program,
            isSaving = isUpdatingProfile,
            errorMessage = editError,
            onDismiss = {
                if (!isUpdatingProfile) {
                    showEditDialog = false
                }
            },
            onSave = { name, program ->
                scope.launch {
                    isUpdatingProfile = true
                    editError = null
                    editSuccess = null

                    try {
                        val cleanName = name.trim()
                        val cleanProgram = program.trim()

                        if (cleanName.isBlank() || cleanProgram.isBlank()) {
                            error("Nama dan program studi wajib diisi.")
                        }

                        val firebaseUser = FirebaseAuth.getInstance().currentUser

                        if (firebaseUser != null) {
                            firebaseUser.updateProfile(
                                UserProfileChangeRequest.Builder()
                                    .setDisplayName(cleanName)
                                    .build()
                            ).await()

                            FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(firebaseUser.uid)
                                .set(
                                    mapOf(
                                        "uid" to firebaseUser.uid,
                                        "name" to cleanName,
                                        "program" to cleanProgram,
                                        "email" to firebaseUser.email,
                                        "updatedAt" to System.currentTimeMillis()
                                    ),
                                    SetOptions.merge()
                                )
                                .await()

                            withContext(NonCancellable) {
                                userProfileRepository.saveProfile(
                                    uid = firebaseUser.uid,
                                    name = cleanName,
                                    program = cleanProgram
                                )
                            }

                            AuthDebugLog.d(
                                "PROFILE_UPDATE online success: uid=${AuthDebugLog.uid(firebaseUser.uid)}"
                            )
                        } else {
                            withContext(NonCancellable) {
                                userProfileRepository.saveGuestProfile(
                                    name = cleanName,
                                    program = cleanProgram
                                )
                            }
                            AuthDebugLog.d("PROFILE_UPDATE guest local success")
                        }

                        editSuccess = "Profil berhasil diperbarui."
                        showEditDialog = false
                    } catch (error: Throwable) {
                        AuthDebugLog.e("PROFILE_UPDATE failed", error)
                        editError = error.message
                            ?: "Profil gagal diperbarui."
                    } finally {
                        isUpdatingProfile = false
                    }
                }
            }
        )
    }
}

@Composable
private fun EditProfileDialog(
    initialName: String,
    initialProgram: String,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var program by remember(initialProgram) { mutableStateOf(initialProgram) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Edit profil",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama mahasiswa") },
                    singleLine = true,
                    enabled = !isSaving
                )

                OutlinedTextField(
                    value = program,
                    onValueChange = { program = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Program studi") },
                    singleLine = true,
                    enabled = !isSaving
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
                onClick = {
                    onSave(name, program)
                },
                enabled = !isSaving &&
                    name.trim().isNotBlank() &&
                    program.trim().isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.size(8.dp))
                }
                Text(if (isSaving) "Menyimpan..." else "Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun ProfileMenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    val accent = if (destructive) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }

    val iconBackground = if (destructive) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }

    val iconOnBackground = if (destructive) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.primary
    }

    val contentAlpha = if (enabled) 1f else 0.48f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconBackground.copy(alpha = contentAlpha)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconOnBackground.copy(alpha = contentAlpha),
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(Modifier.size(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (destructive) {
                        accent.copy(alpha = contentAlpha)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha)
                    }
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                        alpha = contentAlpha
                    )
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Buka",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = contentAlpha
                )
            )
        }
    }
}

@Composable
private fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.size(10.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
