package com.ascend.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.domain.model.Quest
import com.ascend.app.ui.components.AscendButton
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients
import com.ascend.app.ui.theme.toRarity
import com.ascend.app.util.ChamferShape
import kotlinx.coroutines.delay
import kotlin.collections.forEachIndexed
import kotlin.random.Random

data class Particle(
    val x: Float, val y: Float,
    val color: Color,
    val radius: Float,
    val speedY: Float,
    val phase: Float    // randomises animation start
)

@Composable
fun ParticleField(
    particleCount: Int = 30,
    colors: List<Color> = listOf(
        Color(0xFFFFD700), Color(0xFF7B61FF),
        Color(0xFF00D4FF), Color(0xFFFF2D78),
        Color(0xFF39FF14), Color(0xFFFF6B35)
    )
) {
    val particles = remember {
        List(particleCount) {
            Particle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.8f + 0.2f,
                color = colors.random(),
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

    Canvas(modifier = Modifier.fillMaxSize()) {
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