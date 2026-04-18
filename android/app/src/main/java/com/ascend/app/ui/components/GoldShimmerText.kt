package com.ascend.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

@Composable
fun GoldShimmerText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Medium
) {
    val transition = rememberInfiniteTransition(label = "gold_shimmer")
    val offset by transition.animateFloat(
        initialValue = -400f, targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "shimmer_x"
    )

    Text(
        text = text,
        fontSize = fontSize,
        fontWeight = fontWeight,
        style = TextStyle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFFF0A0),
                    Color(0xFFFFD700),
                    Color(0xFFFF9900)
                ),
                start = Offset(offset, 0f),
                end = Offset(offset + 300f, 0f)
            )
        )
    )
}