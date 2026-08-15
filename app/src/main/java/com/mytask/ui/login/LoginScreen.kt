package com.mytask.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mytask.R
import com.mytask.data.repository.UserProfileRepository
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    repository: UserProfileRepository
) {

    var name by remember { mutableStateOf("") }
    var program by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(R.mipmap.mytask_background),
            contentDescription = "MyTask",
            modifier = Modifier
                .size(96.dp)
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = "Selamat datang di MyTask",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Masukkan data mahasiswa untuk menyiapkan profil kamu.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                showError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Nama Mahasiswa")
            },
            placeholder = {
                Text("Contoh: Ahmad Furqon Ramadhani")
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = program,
            onValueChange = {
                program = it
                showError = false
            },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("Program Studi")
            },
            placeholder = {
                Text("Contoh: Teknik Informatika")
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text
            ),
            shape = RoundedCornerShape(16.dp)
        )

        if (showError) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Nama dan program studi wajib diisi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {
                if (name.isBlank() || program.isBlank()) {
                    showError = true
                    return@Button
                }

                scope.launch {
                    repository.saveProfile(
                        name = name,
                        program = program
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Login,
                contentDescription = null
            )
            Spacer(Modifier.size(8.dp))
            Text("Masuk ke MyTask")
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Data ini disimpan secara lokal di perangkat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
