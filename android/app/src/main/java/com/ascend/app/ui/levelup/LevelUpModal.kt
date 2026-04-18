package com.ascend.app.ui.levelup

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.components.AscendButton
import com.ascend.app.ui.components.GoldShimmerText
import com.ascend.app.ui.components.ParticleField
import com.ascend.app.ui.theme.Gradients
import kotlinx.coroutines.delay
import kotlin.collections.forEachIndexed

@Composable
fun LevelUpModal(
    newLevel: Int,
    titleUnlocked: String?,
    statDeltas: List<StatDelta>,
    onContinue: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // orchestrated entrance — each element has a staggered delay
    val bannerAlpha    = remember { Animatable(0f) }
    val levelScale     = remember { Animatable(0.4f) }
    val statsVisible   = remember { mutableStateListOf(*Array(statDeltas.size) { false }) }
    val buttonAlpha    = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // beat 1 (0ms): haptic + rings start via particle system
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // beat 2 (100ms): banner fades in
        delay(100)
        bannerAlpha.animateTo(1f, tween(300))

        // beat 3 (200ms): level number slams in with overshoot spring
        delay(100)
        levelScale.animateTo(1f, spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ))
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)

        // beat 4 (600ms): stat lines reveal sequentially
        delay(200)
        statsVisible.forEachIndexed { i, _ ->
            delay(150L * i)
            statsVisible[i] = true
        }

        // beat 5 (1200ms): CTA button fades in
        delay(400)
        buttonAlpha.animateTo(1f, tween(400))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // particle system
        ParticleField()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // rank banner
            Box(modifier = Modifier.alpha(bannerAlpha.value)) {
                RankBanner()
            }

            // level number with shimmer gold text
            Box(modifier = Modifier.scale(levelScale.value)) {
                GoldShimmerText(
                    text = newLevel.toString(),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // stat delta rows
            statDeltas.forEachIndexed { i, delta ->
                AnimatedVisibility(
                    visible = statsVisible.getOrElse(i) { false },
                    enter = fadeIn(tween(300)) + slideInHorizontally { -40 }
                ) {
                    StatDeltaRow(delta = delta)
                }
            }

            // continue button
            Box(modifier = Modifier.alpha(buttonAlpha.value)) {
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