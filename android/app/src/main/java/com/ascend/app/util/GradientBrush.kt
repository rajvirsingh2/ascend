package com.ascend.app.util

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.ascend.app.ui.theme.Gradients

fun horizontalGradientBrush(colors: List<Color>): Brush =
    Brush.linearGradient(colors, start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, 0f))

fun diagonalGradientBrush(colors: List<Color>): Brush =
    Brush.linearGradient(colors, start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY))

@Composable
fun rememberShimmerBrush(colors:List<Color> = Gradients.GoldShimmer): Brush{
    val transition= rememberInfiniteTransition(label="shimmer")
    val offset by transition.animateFloat(
        initialValue = -400f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(tween(2500, easing= LinearEasing)),
        label = "shimmer_x"
    )
    return remember(offset) {
        Brush.linearGradient(
            colors=colors,
            start=Offset(offset,0f),
            end=Offset(offset+30,0f)
        )
    }
}