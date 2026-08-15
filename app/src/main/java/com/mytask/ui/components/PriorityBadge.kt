package com.mytask.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PriorityBadge(
    priority: Int,
    modifier: Modifier = Modifier
) {

    val text = when (priority) {
        3 -> "High"
        2 -> "Medium"
        else -> "Low"
    }

    Text(
        text = text,
        modifier = modifier
            .background(
                color = Color.LightGray,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 4.dp
            )
    )
}