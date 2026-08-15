package com.mytask.ui.loading

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mytask.R

@Composable
fun LoadingScreen() {

    Column(

        modifier =
            Modifier.fillMaxSize(),

        horizontalAlignment =
            Alignment.CenterHorizontally,

        verticalArrangement =
            Arrangement.Center
    ) {

        Image(

            painter =
                painterResource(
                    id =
                        R.mipmap.mytask_background
                ),

            contentDescription =
                "MyTask",

            modifier =
                Modifier.size(110.dp)
        )

        Spacer(
            modifier =
                Modifier.height(16.dp)
        )

        Text(

            text =
                "MyTask",

            style =
                MaterialTheme
                    .typography
                    .headlineMedium,

            fontWeight =
                FontWeight.Bold
        )

        Text(

            text =
                "Academic Planner",

            style =
                MaterialTheme
                    .typography
                    .bodyLarge
        )

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )

        CircularProgressIndicator(

            modifier =
                Modifier.size(32.dp)
        )

        Spacer(
            modifier =
                Modifier.height(12.dp)
        )

        Text(

            text =
                "Memuat...",

            style =
                MaterialTheme
                    .typography
                    .bodyMedium
        )
    }
}