package com.ascend.app.util

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

class ChamferShape(private val cut: Dp=8.dp): Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val c=with(density){cut.toPx()}
        return Outline.Generic(Path().apply {
            moveTo(c,0f)
            lineTo(size.width,0f)
            lineTo(size.width-c, size.height)
            lineTo(0f, size.height)
            close()
        })
    }
}