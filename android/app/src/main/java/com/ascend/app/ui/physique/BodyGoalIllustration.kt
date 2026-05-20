package com.ascend.app.ui.physique

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Simple silhouette illustrations for each body goal.
 * Uses Canvas primitives — no external assets needed.
 */
@Composable
fun BodyGoalIllustration(goal: String, modifier: Modifier = Modifier) {
    val primaryColor = Color(0xFF7B61FF)
    val accentColor  = Color(0xFF00D4FF)

    Canvas(modifier = modifier.size(80.dp, 120.dp)) {
        when (goal) {
            "lean_athletic"  -> drawLeanAthletic(primaryColor, accentColor)
            "bulky_muscular" -> drawBulkyMuscular(primaryColor, accentColor)
            "powerlifter"    -> drawPowerlifter(primaryColor, accentColor)
            "endurance"      -> drawEndurance(primaryColor, accentColor)
            "lose_fat"       -> drawLoseFat(primaryColor, accentColor)
            "maintain"       -> drawMaintain(primaryColor, accentColor)
        }
    }
}

private fun DrawScope.drawLeanAthletic(primary: Color, accent: Color) {
    val cx = size.width / 2
    // head
    drawCircle(primary, radius = 10.dp.toPx(), center = Offset(cx, 12.dp.toPx()))
    // torso — inverted triangle (V-taper)
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - 18.dp.toPx(), 25.dp.toPx())
        lineTo(cx + 18.dp.toPx(), 25.dp.toPx())
        lineTo(cx + 10.dp.toPx(), 60.dp.toPx())
        lineTo(cx - 10.dp.toPx(), 60.dp.toPx())
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(primary, accent)))
    // legs
    drawLine(primary.copy(.8f), Offset(cx - 8.dp.toPx(), 60.dp.toPx()),
        Offset(cx - 12.dp.toPx(), 100.dp.toPx()), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(primary.copy(.8f), Offset(cx + 8.dp.toPx(), 60.dp.toPx()),
        Offset(cx + 12.dp.toPx(), 100.dp.toPx()), strokeWidth = 8.dp.toPx(), cap = StrokeCap.Round)
    // arms
    drawLine(accent, Offset(cx - 18.dp.toPx(), 30.dp.toPx()),
        Offset(cx - 24.dp.toPx(), 58.dp.toPx()), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
    drawLine(accent, Offset(cx + 18.dp.toPx(), 30.dp.toPx()),
        Offset(cx + 24.dp.toPx(), 58.dp.toPx()), strokeWidth = 6.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawBulkyMuscular(primary: Color, accent: Color) {
    val cx = size.width / 2
    drawCircle(primary, radius = 11.dp.toPx(), center = Offset(cx, 12.dp.toPx()))
    // wide rectangular torso
    drawRoundRect(
        Brush.verticalGradient(listOf(primary, accent)),
        topLeft = Offset(cx - 24.dp.toPx(), 24.dp.toPx()),
        size = Size(48.dp.toPx(), 36.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
    )
    // thick legs
    drawLine(primary.copy(.8f), Offset(cx - 10.dp.toPx(), 60.dp.toPx()),
        Offset(cx - 14.dp.toPx(), 100.dp.toPx()), strokeWidth = 14.dp.toPx(), cap = StrokeCap.Round)
    drawLine(primary.copy(.8f), Offset(cx + 10.dp.toPx(), 60.dp.toPx()),
        Offset(cx + 14.dp.toPx(), 100.dp.toPx()), strokeWidth = 14.dp.toPx(), cap = StrokeCap.Round)
    // thick arms
    drawLine(accent, Offset(cx - 24.dp.toPx(), 28.dp.toPx()),
        Offset(cx - 32.dp.toPx(), 56.dp.toPx()), strokeWidth = 12.dp.toPx(), cap = StrokeCap.Round)
    drawLine(accent, Offset(cx + 24.dp.toPx(), 28.dp.toPx()),
        Offset(cx + 32.dp.toPx(), 56.dp.toPx()), strokeWidth = 12.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawPowerlifter(primary: Color, accent: Color) {
    val cx = size.width / 2
    drawCircle(primary, radius = 12.dp.toPx(), center = Offset(cx, 13.dp.toPx()))
    // very wide, stocky torso
    drawRoundRect(
        Brush.verticalGradient(listOf(primary.copy(.9f), accent.copy(.7f))),
        topLeft = Offset(cx - 28.dp.toPx(), 26.dp.toPx()),
        size = Size(56.dp.toPx(), 38.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
    )
    // very thick legs
    drawLine(primary, Offset(cx - 12.dp.toPx(), 64.dp.toPx()),
        Offset(cx - 16.dp.toPx(), 104.dp.toPx()), strokeWidth = 18.dp.toPx(), cap = StrokeCap.Round)
    drawLine(primary, Offset(cx + 12.dp.toPx(), 64.dp.toPx()),
        Offset(cx + 16.dp.toPx(), 104.dp.toPx()), strokeWidth = 18.dp.toPx(), cap = StrokeCap.Round)
    // barbell hint
    drawLine(accent, Offset(cx - 36.dp.toPx(), 26.dp.toPx()),
        Offset(cx + 36.dp.toPx(), 26.dp.toPx()), strokeWidth = 4.dp.toPx())
}

private fun DrawScope.drawEndurance(primary: Color, accent: Color) {
    val cx = size.width / 2
    drawCircle(primary, radius = 9.dp.toPx(), center = Offset(cx, 11.dp.toPx()))
    // lean, slightly smaller torso, running posture
    val torso = androidx.compose.ui.graphics.Path().apply {
        moveTo(cx - 12.dp.toPx(), 22.dp.toPx())
        lineTo(cx + 12.dp.toPx(), 22.dp.toPx())
        lineTo(cx + 8.dp.toPx(), 55.dp.toPx())
        lineTo(cx - 8.dp.toPx(), 55.dp.toPx())
        close()
    }
    drawPath(torso, Brush.verticalGradient(listOf(primary, accent)))
    // running legs (asymmetric)
    drawLine(primary.copy(.8f), Offset(cx - 6.dp.toPx(), 55.dp.toPx()),
        Offset(cx - 16.dp.toPx(), 90.dp.toPx()), strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
    drawLine(primary.copy(.8f), Offset(cx + 6.dp.toPx(), 55.dp.toPx()),
        Offset(cx + 2.dp.toPx(), 90.dp.toPx()), strokeWidth = 7.dp.toPx(), cap = StrokeCap.Round)
    // running arms (asymmetric)
    drawLine(accent, Offset(cx - 12.dp.toPx(), 28.dp.toPx()),
        Offset(cx - 22.dp.toPx(), 48.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(accent, Offset(cx + 12.dp.toPx(), 28.dp.toPx()),
        Offset(cx + 8.dp.toPx(), 42.dp.toPx()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawLoseFat(primary: Color, accent: Color) {
    val cx = size.width / 2
    drawCircle(primary, radius = 10.dp.toPx(), center = Offset(cx, 12.dp.toPx()))
    // rounder torso (before)
    drawOval(
        color = primary.copy(.4f),
        topLeft = Offset(cx - 22.dp.toPx(), 23.dp.toPx()),
        size = Size(44.dp.toPx(), 38.dp.toPx())
    )
    // arrow showing reduction
    drawLine(accent, Offset(cx + 28.dp.toPx(), 38.dp.toPx()),
        Offset(cx + 20.dp.toPx(), 38.dp.toPx()), strokeWidth = 3.dp.toPx(),
        cap = StrokeCap.Round)
    // legs
    drawLine(primary.copy(.7f), Offset(cx - 8.dp.toPx(), 61.dp.toPx()),
        Offset(cx - 10.dp.toPx(), 100.dp.toPx()), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
    drawLine(primary.copy(.7f), Offset(cx + 8.dp.toPx(), 61.dp.toPx()),
        Offset(cx + 10.dp.toPx(), 100.dp.toPx()), strokeWidth = 10.dp.toPx(), cap = StrokeCap.Round)
    // target arrow downward
    drawLine(accent, Offset(cx, 65.dp.toPx()), Offset(cx, 80.dp.toPx()),
        strokeWidth = 2.dp.toPx())
}

private fun DrawScope.drawMaintain(primary: Color, accent: Color) {
    val cx = size.width / 2
    drawCircle(primary, radius = 10.dp.toPx(), center = Offset(cx, 12.dp.toPx()))
    // balanced torso
    drawRoundRect(
        Brush.verticalGradient(listOf(primary.copy(.8f), accent.copy(.8f))),
        topLeft = Offset(cx - 16.dp.toPx(), 23.dp.toPx()),
        size = Size(32.dp.toPx(), 34.dp.toPx()),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
    )
    // balanced legs
    drawLine(primary.copy(.8f), Offset(cx - 8.dp.toPx(), 57.dp.toPx()),
        Offset(cx - 10.dp.toPx(), 97.dp.toPx()), strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round)
    drawLine(primary.copy(.8f), Offset(cx + 8.dp.toPx(), 57.dp.toPx()),
        Offset(cx + 10.dp.toPx(), 97.dp.toPx()), strokeWidth = 9.dp.toPx(), cap = StrokeCap.Round)
    // checkmark
    drawLine(accent, Offset(cx - 6.dp.toPx(), 38.dp.toPx()),
        Offset(cx - 1.dp.toPx(), 44.dp.toPx()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
    drawLine(accent, Offset(cx - 1.dp.toPx(), 44.dp.toPx()),
        Offset(cx + 8.dp.toPx(), 32.dp.toPx()), strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
}

// --- PREVIEWS ---

@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "1. All Body Goals Gallery")
@Composable
fun BodyGoalIllustrationGalleryPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IllustrationWithLabel("Lean Athletic", "lean_athletic")
                IllustrationWithLabel("Bulky Muscular", "bulky_muscular")
                IllustrationWithLabel("Powerlifter", "powerlifter")
            }

            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IllustrationWithLabel("Endurance", "endurance")
                IllustrationWithLabel("Lose Fat", "lose_fat")
                IllustrationWithLabel("Maintain", "maintain")
            }
        }
    }
}

@Composable
private fun IllustrationWithLabel(label: String, goalKey: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        BodyGoalIllustration(goal = goalKey)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label.uppercase(),
            color = Color(0xFFB8A4FF), // Muted purple text matching your theme
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D12, name = "2. Lean Athletic Focus")
@Composable
fun BodyGoalIllustrationSinglePreview() {
    MaterialTheme {
        BodyGoalIllustration(
            goal = "lean_athletic",
            modifier = Modifier.padding(32.dp)
        )
    }
}