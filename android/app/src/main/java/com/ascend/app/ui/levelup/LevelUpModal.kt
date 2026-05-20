package com.ascend.app.ui.levelup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ascend.app.domain.model.StatDelta
import com.ascend.app.ui.components.AscendButton
import com.ascend.app.ui.components.GoldShimmerText
import com.ascend.app.ui.components.ParticleField
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients
import kotlinx.coroutines.delay

@Composable
fun LevelUpModal(
    newLevel: Int,
    titleUnlocked: String?,
    statDeltas: List<StatDelta>,
    onContinue: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val bannerAlpha  = remember { Animatable(0f) }
    val levelScale   = remember { Animatable(0.4f) }
    val titleAlpha   = remember { Animatable(0f) }
    val buttonAlpha  = remember { Animatable(0f) }
    val statsVisible = remember { mutableStateListOf(*Array(statDeltas.size) { false }) }

    LaunchedEffect(Unit) {
        // beat 1 — haptic + banner
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        delay(100)
        bannerAlpha.animateTo(1f, tween(300))

        // beat 2 — level number slams in
        delay(100)
        levelScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessMedium)
        )
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // beat 3 — title unlocked
        delay(200)
        titleAlpha.animateTo(1f, tween(400))

        // beat 4 — stat lines staggered
        delay(150)
        statsVisible.forEachIndexed { i, _ ->
            delay(160L * i)
            statsVisible[i] = true
        }

        // beat 5 — CTA
        delay(400)
        buttonAlpha.animateTo(1f, tween(400))
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD0D0030))
        ) {
            // particles behind everything
            ParticleField(modifier = Modifier.fillMaxSize())

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // rank banner
                Box(modifier = Modifier.alpha(bannerAlpha.value)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF7B61FF).copy(0.3f),
                                        Color(0xFF00D4FF).copy(0.3f)
                                    )
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "RANK UP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB8A4FF),
                            letterSpacing = 0.14.sp
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // level number
                Box(modifier = Modifier.scale(levelScale.value)) {
                    GoldShimmerText(text = newLevel.toString(), fontSize = 72.sp)
                }

                Text(
                    text = "LEVEL REACHED",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB8A4FF),
                    letterSpacing = 0.14.sp
                )

                Spacer(Modifier.height(20.dp))

                // title unlocked
                if (titleUnlocked != null) {
                    Box(
                        modifier = Modifier
                            .alpha(titleAlpha.value)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF1A1020),
                                        Color(0xFF200A1A)
                                    )
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "TITLE UNLOCKED",
                                fontSize = 10.sp,
                                color = Color(0xFFFF2D78),
                                letterSpacing = 0.1.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "\"$titleUnlocked\"",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFFE8F0)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }

                // stat deltas
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkColors.Abyss.copy(alpha = 0.9f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = "STAT INCREASES",
                        fontSize = 10.sp,
                        color = DarkColors.TextMuted,
                        letterSpacing = 0.1.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    statDeltas.forEachIndexed { i, delta ->
                        AnimatedVisibility(
                            visible = statsVisible.getOrElse(i) { false },
                            enter = fadeIn(tween(300)) + slideInHorizontally { -40 }
                        ) {
                            StatDeltaRow(
                                delta = delta,
                                modifier = Modifier.padding(vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // CTA
                Box(modifier = Modifier
                    .alpha(buttonAlpha.value)
                    .fillMaxWidth()) {
                    AscendButton(
                        text = "CONTINUE YOUR JOURNEY",
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onContinue()
                        },
                        gradient = Gradients.LegendaryFlame,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun StatDeltaRow(delta: StatDelta, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    if (delta.deltaPositive) Color(0xFF00D4FF).copy(0.2f)
                    else Color(0xFFFF6B35).copy(0.2f)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = delta.statName,
                fontSize = 11.sp,
                color = if (delta.deltaPositive) Color(0xFF00D4FF) else Color(0xFFFF6B35),
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.weight(1f))

        Text(text = delta.before, fontSize = 12.sp, color = DarkColors.TextMuted)
        Text(text = " → ", fontSize = 12.sp, color = DarkColors.TextHint)
        Text(
            text = delta.after,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (delta.deltaPositive) Color(0xFF00D4FF) else Color(0xFFFF6B35)
        )
        Text(
            text = "  ${delta.delta}",
            fontSize = 11.sp,
            color = if (delta.deltaPositive)
                Color(0xFF39FF14).copy(0.8f)
            else Color(0xFFFF6B35).copy(0.8f)
        )
    }
}


@Preview(name = "1. Standard Level Up", showBackground = true)
@Composable
fun LevelUpModalPreview_Standard() {
    MaterialTheme {
        LevelUpModal(
            newLevel = 13,
            titleUnlocked = null,
            statDeltas = listOf(
                StatDelta(
                    statName = "STRENGTH",
                    before = "45",
                    after = "48",
                    delta = "+3",
                    deltaPositive = true
                ),
                StatDelta(
                    statName = "AGILITY",
                    before = "30",
                    after = "32",
                    delta = "+2",
                    deltaPositive = true
                ),
                StatDelta(
                    statName = "INTELLIGENCE",
                    before = "85",
                    after = "86",
                    delta = "+1",
                    deltaPositive = true
                )
            ),
            onContinue = {}
        )
    }
}

@Preview(name = "2. Milestone Level Up (Title Unlocked)", showBackground = true)
@Composable
fun LevelUpModalPreview_Milestone() {
    MaterialTheme {
        LevelUpModal(
            newLevel = 15,
            titleUnlocked = "Shadow Apprentice",
            statDeltas = listOf(
                StatDelta(
                    statName = "MAX HP",
                    before = "450",
                    after = "500",
                    delta = "+50",
                    deltaPositive = true
                ),
                StatDelta(
                    statName = "MANA",
                    before = "100",
                    after = "120",
                    delta = "+20",
                    deltaPositive = true
                ),
                StatDelta(
                    statName = "STAMINA",
                    before = "80",
                    after = "85",
                    delta = "+5",
                    deltaPositive = true
                )
            ),
            onContinue = {}
        )
    }
}