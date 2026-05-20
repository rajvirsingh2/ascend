package com.ascend.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

data class Particle(
    val x: Float, val y: Float,
    val color: Color,
    val radius: Float,
    val speedY: Float,
    val phase: Float
)

private val particleColors: List<Color> = listOf(
    Color(0xFFFFD700), Color(0xFF7B61FF),
    Color(0xFF00D4FF), Color(0xFFFF2D78),
    Color(0xFF39FF14), Color(0xFFFF6B35)
)
@Composable
fun ParticleField(
    particleCount: Int = 30,
    @SuppressLint("ModifierParameter") modifier: Modifier= Modifier
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.8f + 0.2f,
                color = particleColors.random(),
                radius = Random.nextFloat() * 5f + 2f,
                speedY = Random.nextFloat() * 0.4f + 0.2f,
                phase = Random.nextFloat()
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "particles")
    val tick by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "tick"
    )

    Canvas(modifier =modifier) {
        particles.forEach { p ->
            val progress = ((tick + p.phase) % 1f)
            val currentY = (p.y - progress * p.speedY)
            if (currentY < 0f) return@forEach

            drawCircle(
                color = p.color.copy(alpha = 1f - progress),
                radius = p.radius * (1f - progress * 0.5f),
                center = Offset(p.x * size.width, currentY * size.height)
            )
        }
    }
}