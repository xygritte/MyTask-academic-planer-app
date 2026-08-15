package com.mytask.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MyTaskColors = lightColorScheme(
    primary = BluePrimary,
    secondary = BlueSecondary
)

@Composable
fun MyTaskTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MyTaskColors,
        typography = Typography(),
        content = content
    )
}