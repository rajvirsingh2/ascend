package com.ascend.app.ui.levelup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ascend.app.domain.model.StatDelta
import com.ascend.app.ui.auth.jetBrainsMono
import com.ascend.app.ui.auth.orbitron
import com.ascend.app.ui.components.rankForLevel
import com.ascend.app.ui.components.scanlineOverlay
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// --- Mapped Tailwind Colors ---
val LevelUpBg = Color(0xFF0A0A0F)
val LevelUpTertiary = Color(0xFFE9C349) // Gold
val LevelUpSecondary = Color(0xFF4CD7F6) // Cyan
val LevelUpPrimary = Color(0xFFD2BBFF) // Purple
val LevelUpSurface = Color(0xFF131318)
val LevelUpOutline = Color(0xFF4A4455)
val LevelUpOnSurfaceVariant = Color(0xFFCCC3D8)
val LevelUpSurfaceContainer = Color(0xFF1F1F25)
val LevelUpSurfaceBright = Color(0xFF39383E)

/* ============================================================
 * LEVEL UP MODAL
 * ============================================================ */
@Composable
fun LevelUpModal(
    newLevel: Int,
    titleUnlocked: String?,
    statDeltas: List<StatDelta>,
    onContinue: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // ── ANIMATIONS ──
    val popScale = remember { Animatable(0.8f) }
    val popAlpha = remember { Animatable(0f) }
    val headerAlpha = remember { Animatable(0f) }
    val badgeScale = remember { Animatable(0.4f) }
    val titleAlpha = remember { Animatable(0f) }
    val statsAlpha = remember { Animatable(0f) }
    val buttonAlpha = remember { Animatable(0f) }
    val statsVisible = remember { mutableStateListOf(*Array(statDeltas.size) { false }) }

    LaunchedEffect(Unit) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        popAlpha.animateTo(1f, tween(500, easing = EaseOutCubic))
        popScale.animateTo(
            1f,
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
        )

        headerAlpha.animateTo(1f, tween(300))

        delay(100)
        badgeScale.animateTo(
            1f,
            spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
        )
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        delay(150)
        titleAlpha.animateTo(1f, tween(400))

        delay(150)
        statsAlpha.animateTo(1f, tween(300))
        statsVisible.forEachIndexed { i, _ ->
            delay(140L)
            statsVisible[i] = true
        }

        delay(200)
        buttonAlpha.animateTo(1f, tween(400))
    }

    val modalContent = @Composable {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LevelUpBg)
        ) {
            // Ambient Layer 1: Radial Glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                LevelUpTertiary.copy(alpha = 0.15f),
                                Color.Transparent
                            ),
                            radius = 1200f
                        )
                    )
            )

            // Ambient Layer 2: Spinning Light Rays
            LightRaysLayer()

            // Ambient Layer 3: Scanlines
            Box(modifier = Modifier.fillMaxSize().scanlineOverlay())

            // Ambient Layer 4: Particles
            ParticleField(modifier = Modifier.fillMaxSize())

            // Main Content Container
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp) // px-margin-mobile roughly
                    .graphicsLayer {
                        scaleX = popScale.value
                        scaleY = popScale.value
                        alpha = popAlpha.value
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Main flex-col space-y-12 wrapper (~48.dp gap)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(48.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // ─── SYSTEM HEADER ───
                    Column(
                        modifier = Modifier.alpha(headerAlpha.value),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "◈ LEVEL UP!",
                            fontFamily = orbitron, // Space Grotesk mapped to orbitron
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            color = LevelUpTertiary,
                            style = TextStyle(
                                shadow = Shadow(LevelUpTertiary.copy(alpha = 0.5f), blurRadius = 20f)
                            )
                        )
                        Text(
                            "SYSTEM NOTIFICATION",
                            fontFamily = jetBrainsMono, // Geist/Tech mapped to Jetbrains
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 2.5.sp,
                            color = LevelUpSecondary
                        )
                    }

                    // ─── RANK BADGE ───
                    RankBadge(
                        rank = rankForLevel(newLevel),
                        modifier = Modifier.scale(badgeScale.value)
                    )

                    // ─── OPTIONAL TITLE ───
                    if (titleUnlocked != null) {
                        Box(
                            modifier = Modifier
                                .alpha(titleAlpha.value)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LevelUpSurface.copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFFFF2D78).copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "TITLE UNLOCKED",
                                    fontFamily = jetBrainsMono,
                                    fontSize = 10.sp,
                                    letterSpacing = 2.sp,
                                    color = Color(0xFFFF2D78)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    titleUnlocked,
                                    fontFamily = orbitron,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // ─── STATS PANEL ───
                    Box(
                        modifier = Modifier
                            .alpha(statsAlpha.value)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LevelUpSurface.copy(alpha = 0.85f))
                            .border(1.dp, LevelUpOutline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            // Subtle cyan outer glow
                            .shadow(
                                elevation = 20.dp,
                                shape = RoundedCornerShape(12.dp),
                                ambientColor = LevelUpSecondary.copy(alpha = 0.1f),
                                spotColor = LevelUpSecondary.copy(alpha = 0.1f)
                            )
                    ) {
                        // Top gradient line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            LevelUpSecondary.copy(alpha = 0.5f),
                                            Color.Transparent
                                        )
                                    )
                                )
                                .align(Alignment.TopCenter)
                        )

                        Column(modifier = Modifier.padding(24.dp)) {
                            // "You are now Level 24"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 24.dp),
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    "You are now Level ",
                                    fontFamily = orbitron,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "$newLevel",
                                    fontFamily = orbitron,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LevelUpSecondary
                                )
                            }

                            // Stat Changes
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                statDeltas.forEachIndexed { i, delta ->
                                    AnimatedVisibility(
                                        visible = statsVisible.getOrElse(i) { false },
                                        enter = fadeIn(tween(300)) + slideInHorizontally { -40 }
                                    ) {
                                        Column {
                                            StatDeltaRow(delta = delta)
                                            if (i < statDeltas.size - 1) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 12.dp)
                                                        .height(1.dp)
                                                        .background(LevelUpOutline.copy(alpha = 0.3f))
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─── CONTINUE BUTTON ───
                    ContinueButton(
                        alpha = buttonAlpha.value,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onContinue()
                        }
                    )
                }
            }
        }
    }

    if (LocalInspectionMode.current) {
        modalContent()
    } else {
        Dialog(
            onDismissRequest = {},
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            ),
            content = modalContent
        )
    }
}

/* ============================================================
 * RANK BADGE (Tailwind mapped)
 * ============================================================ */
@Composable
private fun RankBadge(rank: String, modifier: Modifier = Modifier) {
    val rankCol = LevelUpTertiary // Always gold for level up in the design
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        Box(modifier = modifier.size(256.dp), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(LevelUpSurface.copy(alpha = 0.8f))
                    .border(2.dp, rankCol, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("RANK", fontFamily = jetBrainsMono, fontSize = 12.sp, color = LevelUpOutline)
                    Text(rank, fontFamily = orbitron, fontSize = 64.sp, color = rankCol)
                }
            }
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "badge")

    // Float up/down
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -10f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = EaseInOutSine), RepeatMode.Reverse
        ),
        label = "float"
    )

    // Pulse glow
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = EaseInOutCubic), RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Ping ring (expand + fade)
    val pingScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = EaseOutCubic), RepeatMode.Restart
        ),
        label = "ping"
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = EaseOutCubic), RepeatMode.Restart
        ),
        label = "pingA"
    )

    Box(
        modifier = modifier
            .size(256.dp)
            .graphicsLayer { translationY = floatY * density },
        contentAlignment = Alignment.Center
    ) {
        // Outer Ping ring
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(pingScale)
                .alpha(pingAlpha)
                .border(1.dp, rankCol.copy(alpha = 0.3f), CircleShape)
        )

        // Static inner ring
        Box(
            modifier = Modifier
                .size(224.dp)
                .border(1.dp, rankCol.copy(alpha = 0.5f), CircleShape)
        )

        // Badge Core
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(LevelUpSurface.copy(alpha = 0.8f))
                .border(2.dp, rankCol, CircleShape)
                .shadow(
                    elevation = (pulseGlow * 30).dp,
                    shape = CircleShape,
                    ambientColor = rankCol.copy(alpha = 0.4f),
                    spotColor = rankCol.copy(alpha = 0.4f)
                ),
            contentAlignment = Alignment.Center
        ) {
            // Internal scanlines via custom modifier
            Box(modifier = Modifier.fillMaxSize().alpha(0.5f).scanlineOverlay())

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "RANK",
                    fontFamily = jetBrainsMono,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = LevelUpOutline,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    rank,
                    fontFamily = orbitron,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = rankCol,
                    style = TextStyle(shadow = Shadow(rankCol, blurRadius = 16f))
                )
            }
        }
    }
}

/* ============================================================
 * STAT DELTA ROW
 * ============================================================ */
@Composable
private fun StatDeltaRow(delta: StatDelta) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "▸",
                fontFamily = jetBrainsMono,
                fontSize = 12.sp,
                color = LevelUpSecondary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                delta.statName.uppercase(),
                fontFamily = jetBrainsMono,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.25.sp,
                color = LevelUpOnSurfaceVariant
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                delta.after,
                fontFamily = jetBrainsMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = LevelUpTertiary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                delta.delta,
                fontFamily = jetBrainsMono,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = LevelUpSecondary.copy(alpha = 0.75f)
            )
        }
    }
}

/* ============================================================
 * CONTINUE BUTTON
 * ============================================================ */
@Composable
private fun ContinueButton(alpha: Float, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .alpha(alpha)
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LevelUpSurfaceContainer)
            .clickable { onClick() }
    ) {
        // Top glowing line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(LevelUpPrimary)
                .shadow(10.dp, ambientColor = LevelUpPrimary, spotColor = LevelUpPrimary)
                .align(Alignment.TopCenter)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                "CONTINUE",
                fontFamily = orbitron,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = LevelUpPrimary
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward, null,
                tint = LevelUpPrimary, modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* ============================================================
 * AMBIENT — LIGHT RAYS spinning conic
 * ============================================================ */
@Composable
private fun LightRaysLayer() {
    val isPreview = LocalInspectionMode.current
    if (isPreview) return

    val infiniteTransition = rememberInfiniteTransition(label = "rays")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(20_000, easing = LinearEasing), RepeatMode.Restart
        ),
        label = "rotate"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .scale(1.5f)
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2, h / 2)
        val radius = (w.coerceAtLeast(h)) * 0.7f

        rotate(rotation, center) {
            val rayCount = 10
            val arcSweep = 360f / (rayCount * 2)
            for (i in 0 until rayCount) {
                val start = i * (360f / rayCount)
                drawArc(
                    color = LevelUpTertiary.copy(alpha = 0.10f),
                    startAngle = start,
                    sweepAngle = arcSweep,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
            }
        }

        // Inner mask (radial gradient to hide center rays)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(LevelUpBg, Color.Transparent),
                radius = radius * 0.7f,
                center = center
            ),
            radius = radius * 0.7f,
            center = center,
            blendMode = androidx.compose.ui.graphics.BlendMode.DstOut
        )
    }
}

/* ============================================================
 * PARTICLE FIELD
 * ============================================================ */
@Composable
fun ParticleField(modifier: Modifier = Modifier, count: Int = 30) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val isPreview = LocalInspectionMode.current

    val actualCount = if (isPreview) 15 else count

    val particles = remember(actualCount) {
        val initialSize = if (canvasSize == Size.Zero) Size(1000f, 1000f) else canvasSize
        List(actualCount) { Particle.spawn(initialSize) }
    }

    val tick = rememberInfiniteTransition(label = "particleTick")
    val t by tick.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = LinearEasing), RepeatMode.Restart
        ),
        label = "t"
    )

    LaunchedEffect(canvasSize == Size.Zero, isPreview) {
        if (canvasSize == Size.Zero || isPreview) return@LaunchedEffect
        while (true) {
            withFrameMillis {
                particles.forEach { it.update(canvasSize) }
            }
        }
    }

    Canvas(modifier = modifier.onSizeChanged {
        val newSize = Size(it.width.toFloat(), it.height.toFloat())
        if (canvasSize != newSize) {
            canvasSize = newSize
        }
    }) {
        val _trigger = t
        particles.forEach { p ->
            drawCircle(
                color = p.color.copy(alpha = p.alpha * 0.3f),
                radius = p.size * 2f,
                center = p.pos
            )
            drawCircle(
                color = p.color.copy(alpha = p.alpha),
                radius = p.size,
                center = p.pos
            )
        }
    }
}

private class Particle(
    var pos: Offset,
    var vel: Offset,
    val size: Float,
    val color: Color,
    var life: Float = 1f,
    val maxLife: Float
) {
    var alpha: Float = 1f
        private set

    fun update(canvasSize: Size) {
        if (canvasSize.width <= 0f || canvasSize.height <= 0f) return

        pos = Offset(pos.x + vel.x, pos.y + vel.y - 0.5f) // Upward drift
        life -= 0.015f
        alpha = (life / maxLife).coerceIn(0f, 1f).let { 1f - (1f - it) * (1f - it) } // ease-out fade
        if (life <= 0f) {
            respawn(canvasSize)
        }
    }

    fun respawn(canvasSize: Size) {
        val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
        pos = Offset(
            center.x + Random.nextFloat() * 100f - 50f,
            center.y + Random.nextFloat() * 100f - 50f
        )
        val angle = Random.nextFloat() * (2 * Math.PI).toFloat()
        val speed = 1f + Random.nextFloat() * 2f
        vel = Offset(cos(angle) * speed, sin(angle) * speed)
        life = 1f
    }

    companion object {
        fun spawn(canvasSize: Size): Particle {
            val center = Offset(canvasSize.width / 2, canvasSize.height / 2)
            val angle = Random.nextFloat() * (2 * Math.PI).toFloat()
            val speed = 1f + Random.nextFloat() * 2f
            val isGold = Random.nextBoolean()
            return Particle(
                pos = Offset(
                    center.x + Random.nextFloat() * 100f - 50f,
                    center.y + Random.nextFloat() * 100f - 50f
                ),
                vel = Offset(cos(angle) * speed, sin(angle) * speed),
                size = 1f + Random.nextFloat() * 2f,
                color = if (isGold) LevelUpTertiary else LevelUpSecondary,
                maxLife = 1f
            )
        }
    }
}



/* ============================================================
 * PREVIEWS
 * ============================================================ */
@Preview(name = "Standard Level Up", showBackground = true, backgroundColor = 0xFF0A0A0F)
@Composable
fun LevelUpModalPreview_Standard() {
    MaterialTheme {
        LevelUpModal(
            newLevel = 24,
            titleUnlocked = null,
            statDeltas = listOf(
                StatDelta("STRENGTH", "45", "45", "+2", true),
                StatDelta("AGILITY", "38", "38", "+1", true),
                StatDelta("MANA", "120", "120", "+10", true)
            ),
            onContinue = {}
        )
    }
}