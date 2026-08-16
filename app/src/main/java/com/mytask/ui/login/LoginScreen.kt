package com.mytask.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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

    var mode by remember {
        mutableStateOf(AuthMode.LOGIN)
    }

    var name by remember {
        mutableStateOf("")
    }

    var program by remember {
        mutableStateOf("")
    }

    var email by remember {
        mutableStateOf("")
    }

    var password by remember {
        mutableStateOf("")
    }

    var confirmPassword by remember {
        mutableStateOf("")
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var confirmPasswordVisible by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

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

            val result =
                if (mode == AuthMode.LOGIN) {
                    authRepository.login(
                        email = cleanEmail,
                        password = password
                    )
                } else {
                    authRepository.register(
                        name = name,
                        program = program,
                        email = cleanEmail,
                        password = password
                    )
                }

            result.onFailure { error ->
                errorMessage = friendlyFirebaseError(
                    error.message
                )
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
                    .background(
                        MaterialTheme.colorScheme.primaryContainer
                    )
                    .padding(
                        top = 44.dp,
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 64.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(
                        R.mipmap.mytask_background
                    ),
                    contentDescription = "MyTask",
                    modifier = Modifier
                        .size(78.dp)
                        .clip(
                            RoundedCornerShape(20.dp)
                        )
                )

                Spacer(
                    Modifier.height(14.dp)
                )

                Text(
                    text = "MyTask",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(
                    Modifier.height(2.dp)
                )

                Text(
                    text = "Academic Planner",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(
                        alpha = 0.78f
                    )
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
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 4.dp
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {

                    Text(
                        text = if (mode == AuthMode.LOGIN) {
                            "Masuk ke akun"
                        } else {
                            "Buat akun MyTask"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        text = if (mode == AuthMode.LOGIN) {
                            "Gunakan email dan password untuk melanjutkan."
                        } else {
                            "Buat akun agar profil dan sesi login tersimpan dengan aman."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        Modifier.height(18.dp)
                    )

                    if (mode == AuthMode.REGISTER) {

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Nama Mahasiswa")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PersonOutline,
                                    contentDescription = null
                                )
                            },
                            placeholder = {
                                Text("Nama lengkap")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = authFieldColors()
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        OutlinedTextField(
                            value = program,
                            onValueChange = {
                                program = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Program Studi")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null
                                )
                            },
                            placeholder = {
                                Text("Contoh: Teknik Informatika")
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = authFieldColors()
                        )

                        Spacer(
                            Modifier.height(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Email")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = null
                            )
                        },
                        placeholder = {
                            Text("nama@email.com")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = authFieldColors()
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Password")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    passwordVisible = !passwordVisible
                                }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) {
                                        Icons.Default.VisibilityOff
                                    } else {
                                        Icons.Default.Visibility
                                    },
                                    contentDescription = if (passwordVisible) {
                                        "Sembunyikan password"
                                    } else {
                                        "Tampilkan password"
                                    }
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = authFieldColors()
                    )

                    if (mode == AuthMode.REGISTER) {

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = {
                                Text("Konfirmasi Password")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        confirmPasswordVisible = !confirmPasswordVisible
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) {
                                            Icons.Default.VisibilityOff
                                        } else {
                                            Icons.Default.Visibility
                                        },
                                        contentDescription = if (confirmPasswordVisible) {
                                            "Sembunyikan password"
                                        } else {
                                            "Tampilkan password"
                                        }
                                    )
                                }
                            },
                            visualTransformation = if (confirmPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password
                            ),
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = authFieldColors()
                        )
                    }

                    errorMessage?.let { message ->

                        Spacer(
                            Modifier.height(10.dp)
                        )

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

                    Spacer(
                        Modifier.height(18.dp)
                    )

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

                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = null
                        )

                        Spacer(
                            Modifier.width(10.dp)
                        )

                        Text(
                            text = when {
                                isLoading -> "Memproses..."
                                mode == AuthMode.LOGIN -> "Masuk"
                                else -> "Buat Akun"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = if (mode == AuthMode.LOGIN) {
                                "Belum punya akun?"
                            } else {
                                "Sudah punya akun?"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text = if (mode == AuthMode.LOGIN) {
                                " Daftar"
                            } else {
                                " Masuk"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Akun digunakan untuk menjaga sesi login dan menghubungkan profil mahasiswa kamu.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            Spacer(
                Modifier.height(28.dp)
            )
        }
    }

    // Area teks switch dibuat sebagai overlay klik supaya seluruh label mudah disentuh.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 70.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = MaterialTheme.colorScheme.transparentColor(),
            modifier = Modifier.size(1.dp)
        ) {}
    }
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

private fun friendlyFirebaseError(
    message: String?
): String {

    return when {
        message.isNullOrBlank() ->
            "Terjadi kesalahan. Coba lagi."

        message.contains("The email address is badly formatted", ignoreCase = true) ->
            "Format email tidak valid."

        message.contains("password is invalid", ignoreCase = true) ||
            message.contains("auth/invalid-credential", ignoreCase = true) ->
            "Email atau password salah."

        message.contains("no user record", ignoreCase = true) ||
            message.contains("user-not-found", ignoreCase = true) ->
            "Akun dengan email tersebut belum terdaftar."

        message.contains("email address is already in use", ignoreCase = true) ||
            message.contains("email-already-in-use", ignoreCase = true) ->
            "Email tersebut sudah digunakan."

        message.contains("network", ignoreCase = true) ->
            "Tidak dapat terhubung ke internet."

        else ->
            "Tidak dapat memproses permintaan. Coba lagi."
    }
}

private fun androidx.compose.material3.ColorScheme.transparentColor() =
    this.background.copy(alpha = 0f)
