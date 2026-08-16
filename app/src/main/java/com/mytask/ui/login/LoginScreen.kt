package com.mytask.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mytask.R
import com.mytask.data.repository.FirebaseAuthRepository
import kotlinx.coroutines.launch

private enum class AuthMode {
    LOGIN,
    REGISTER
}

@Composable
fun LoginScreen(
    authRepository: FirebaseAuthRepository
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(AuthMode.LOGIN) }
    var name by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showGuestForm by remember { mutableStateOf(false) }

    fun switchMode(newMode: AuthMode) {
        mode = newMode
        errorMessage = null
        password = ""
        confirmPassword = ""
    }

    fun submit() {
        errorMessage = null
        val cleanEmail = email.trim()

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            errorMessage = "Masukkan email yang valid."
            return
        }

        if (password.length < 6) {
            errorMessage = "Password minimal 6 karakter."
            return
        }

        if (mode == AuthMode.REGISTER) {
            if (name.isBlank() || program.isBlank()) {
                errorMessage = "Nama dan program studi wajib diisi."
                return
            }
            if (password != confirmPassword) {
                errorMessage = "Konfirmasi password tidak sama."
                return
            }
        }

        scope.launch {
            isLoading = true

            val result = if (mode == AuthMode.LOGIN) {
                authRepository.login(cleanEmail, password)
            } else {
                authRepository.register(
                    name = name,
                    program = program,
                    email = cleanEmail,
                    password = password
                )
            }

            result.onFailure { error ->
                errorMessage = friendlyFirebaseError(error.message)
            }

            isLoading = false
        }
    }

    fun signInGoogle() {
        scope.launch {
            isLoading = true
            errorMessage = null

            authRepository
                .signInWithGoogle(context)
                .onFailure { error ->
                    errorMessage = friendlyFirebaseError(error.message)
                }

            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(
                        RoundedCornerShape(
                            bottomStart = 32.dp,
                            bottomEnd = 32.dp
                        )
                    )
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(
                        top = 44.dp,
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 64.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.mipmap.mytask_background),
                    contentDescription = "MyTask",
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "MyTask",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Academic Planner",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                )
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp),
                shape = MaterialTheme.shapes.extraLarge,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Text(
                        text = if (mode == AuthMode.LOGIN) "Masuk ke akun" else "Buat akun MyTask",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = if (mode == AuthMode.LOGIN) {
                            "Gunakan email, Google, atau masuk sebagai guest."
                        } else {
                            "Buat akun agar data dapat disinkronkan antar perangkat."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(18.dp))

                    if (mode == AuthMode.REGISTER) {
                        AuthTextField(
                            value = name,
                            onValueChange = { name = it; errorMessage = null },
                            label = "Nama Mahasiswa",
                            placeholder = "Nama lengkap",
                            icon = Icons.Default.PersonOutline
                        )
                        Spacer(Modifier.height(12.dp))
                        AuthTextField(
                            value = program,
                            onValueChange = { program = it; errorMessage = null },
                            label = "Program Studi",
                            placeholder = "Contoh: Teknik Informatika",
                            icon = Icons.Default.School
                        )
                        Spacer(Modifier.height(12.dp))
                    }

                    AuthTextField(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = "Email",
                        placeholder = "nama@email.com",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email
                    )

                    Spacer(Modifier.height(12.dp))

                    PasswordField(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = "Password",
                        visible = passwordVisible,
                        onToggle = { passwordVisible = !passwordVisible }
                    )

                    if (mode == AuthMode.REGISTER) {
                        Spacer(Modifier.height(12.dp))
                        PasswordField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = null },
                            label = "Konfirmasi Password",
                            visible = confirmPasswordVisible,
                            onToggle = {
                                confirmPasswordVisible = !confirmPasswordVisible
                            }
                        )
                    }

                    errorMessage?.let { message ->
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = ::submit,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Login, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = when {
                                isLoading -> "Memproses..."
                                mode == AuthMode.LOGIN -> "Masuk"
                                else -> "Buat Akun"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (mode == AuthMode.LOGIN) {
                        Spacer(Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(Modifier.weight(1f))
                            Text(
                                text = "  atau  ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            HorizontalDivider(Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(14.dp))

                        OutlinedButton(
                            onClick = ::signInGoogle,
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Lanjutkan dengan Google",
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { showGuestForm = true },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Lanjutkan sebagai Guest",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                switchMode(
                                    if (mode == AuthMode.LOGIN) AuthMode.REGISTER
                                    else AuthMode.LOGIN
                                )
                            }
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (mode == AuthMode.LOGIN) "Belum punya akun?" else "Sudah punya akun?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (mode == AuthMode.LOGIN) " Daftar" else " Masuk",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (mode == AuthMode.LOGIN) {
                                "Akun tersinkron antar perangkat. Guest hanya tersimpan di perangkat ini."
                            } else {
                                "Akun akan menggunakan Firebase Authentication dan sinkronisasi data MyTask."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }

    if (showGuestForm) {
        GuestProfileDialog(
            name = name,
            program = program,
            isLoading = isLoading,
            onNameChange = { name = it; errorMessage = null },
            onProgramChange = { program = it; errorMessage = null },
            onDismiss = {
                if (!isLoading) showGuestForm = false
            },
            onConfirm = {
                scope.launch {
                    isLoading = true
                    errorMessage = null

                    authRepository
                        .continueAsGuest(name, program)
                        .onSuccess {
                            showGuestForm = false
                        }
                        .onFailure { error ->
                            errorMessage =
                                error.message
                                    ?: "Profil guest tidak dapat disimpan."
                        }

                    isLoading = false
                }
            }
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        placeholder = { Text(placeholder) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = authFieldColors()
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    visible: Boolean,
    onToggle: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
        trailingIcon = {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                )
            }
        },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = authFieldColors()
    )
}

@Composable
private fun GuestProfileDialog(
    name: String,
    program: String,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onProgramChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Masuk sebagai Guest",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Tidak perlu akun. Data hanya tersimpan di perangkat ini.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                AuthTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = "Nama Mahasiswa",
                    placeholder = "Nama lengkap",
                    icon = Icons.Default.PersonOutline
                )
                Spacer(Modifier.height(10.dp))
                AuthTextField(
                    value = program,
                    onValueChange = onProgramChange,
                    label = "Program Studi",
                    placeholder = "Teknik Informatika",
                    icon = Icons.Default.School
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading
            ) {
                Text(if (isLoading) "Menyimpan..." else "Masuk")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun authFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        cursorColor = MaterialTheme.colorScheme.primary
    )

private fun friendlyFirebaseError(message: String?): String {
    return when {
        message.isNullOrBlank() -> "Terjadi kesalahan. Coba lagi."
        message.contains("badly formatted", ignoreCase = true) -> "Format email tidak valid."
        message.contains("invalid-credential", ignoreCase = true) ||
            message.contains("password is invalid", ignoreCase = true) ->
            "Email atau password salah."
        message.contains("user-not-found", ignoreCase = true) ->
            "Akun dengan email tersebut belum terdaftar."
        message.contains("email-already-in-use", ignoreCase = true) ->
            "Email tersebut sudah digunakan."
        message.contains("network", ignoreCase = true) ->
            "Tidak dapat terhubung ke internet."
        message.contains("credential", ignoreCase = true) ->
            "Login Google tidak dapat diselesaikan. Coba pilih akun lagi."
        else -> "Tidak dapat memproses permintaan. Coba lagi."
    }
}
