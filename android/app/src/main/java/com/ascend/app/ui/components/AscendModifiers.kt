package com.ascend.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ascend.app.ui.theme.ReactPanel
import com.ascend.app.ui.theme.ReactPanelLine
import com.ascend.app.ui.theme.ReactPurple

fun Modifier.reactStyleCard(selected: Boolean, glowColor: Color, cornerRadius: Dp = 12.dp): Modifier {
    return this
        .alpha(if (selected) 1f else 0.7f)
        .then(
            if (selected) Modifier.shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = glowColor,
                spotColor = glowColor
            ) else Modifier
        )
        .clip(RoundedCornerShape(cornerRadius))
        .background(ReactPanel)
        .border(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) glowColor else ReactPanelLine,
            shape = RoundedCornerShape(cornerRadius)
        )
}

fun Modifier.scanlineOverlay(isLight: Boolean = false): Modifier = drawWithCache {
    val spacing = 4f
    val color = if (isLight) Color.White.copy(alpha = 0.015f) else Color.Black.copy(alpha = 0.06f)
    onDrawWithContent {
        drawContent()
        var y = 0f
        while (y < size.height) {
            drawLine(
                color,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f
            )
            y += spacing
        }
    }
}

fun Modifier.scanlineHorizontal(): Modifier = drawWithCache {
    val spacing = 4f
    onDrawWithContent {
        drawContent()
        var y = 0f
        while (y < size.height) {
            drawLine(
                Color.Black.copy(alpha = 0.18f),
                start = Offset(0f, y + 1.5f),
                end = Offset(size.width, y + 1.5f),
                strokeWidth = 2f
            )
            y += spacing
        }
    }
}

fun Modifier.gridBackground(): Modifier = drawWithCache {
    val spacing = 32f
    val color = ReactPurple.copy(alpha = 0.03f)
    onDrawWithContent {
        drawContent()
        var x = 0f
        while (x < size.width) {
            drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += spacing
        }
        var y = 0f
        while (y < size.height) {
            drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += spacing
        }
    }
}
