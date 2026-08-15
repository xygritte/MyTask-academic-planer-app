package com.mytask.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(
                    rememberScrollState()
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            /*
             * =========================================
             * BRAND HERO
             * =========================================
             */

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
                        top = 48.dp,
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 72.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(
                        R.mipmap.mytask_background
                    ),
                    contentDescription = "MyTask",
                    modifier = Modifier
                        .size(82.dp)
                        .clip(
                            RoundedCornerShape(22.dp)
                        )
                )

                Spacer(
                    Modifier.height(16.dp)
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

            /*
             * =========================================
             * FORM CARD
             * =========================================
             */

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp
                    )
                    .padding(
                        top = 16.dp
                    ),
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
                        text = "Siapkan profil kamu",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        text = "Masukkan nama dan program studi untuk mempersonalisasi MyTask.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(
                        Modifier.height(20.dp)
                    )

                    /*
                     * NAMA
                     */

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
                        colors = loginFieldColors()
                    )

                    Spacer(
                        Modifier.height(12.dp)
                    )

                    /*
                     * PROGRAM STUDI
                     */

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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = loginFieldColors()
                    )

                    if (showError) {

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            text = "Nama dan program studi wajib diisi.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(
                        Modifier.height(20.dp)
                    )

                    Button(
                        onClick = {
                            if (
                                name.isBlank() ||
                                program.isBlank()
                            ) {
                                showError = true
                                return@Button
                            }

                            scope.launch {
                                repository.saveProfile(
                                    name = name.trim(),
                                    program = program.trim()
                                )
                            }
                        },
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
                            text = "Mulai menggunakan MyTask",
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(
                        Modifier.height(14.dp)
                    )

                    Text(
                        text = "Data profil disimpan secara lokal di perangkat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(
                Modifier.height(28.dp)
            )
        }
    }
}

@Composable
private fun loginFieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.65f
        ),
        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = 0.65f
        ),
        cursorColor = MaterialTheme.colorScheme.primary
    )
