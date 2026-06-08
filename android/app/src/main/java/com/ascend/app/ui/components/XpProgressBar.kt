package com.ascend.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.domain.model.User
import com.ascend.app.ui.theme.BorderGlow
import com.ascend.app.ui.theme.CyanAccent
import com.ascend.app.ui.theme.PanelMid
import com.ascend.app.ui.theme.PurplePrimary
import com.ascend.app.ui.theme.TextSecondary

@Composable
fun AscendXpBar(
    user: User,
    animatedFraction: Float,
    modifier: Modifier = Modifier
) {
    // Pulse glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "xp_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "glow"
    )

    val xpGradient = Brush.horizontalGradient(
        colors = listOf(PurplePrimary, CyanAccent)
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXP",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = CyanAccent
            )
            Text(
                text = "${user.currentXp} / ${user.xpToNext} XP",
                fontSize = 10.sp,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(4.dp),
                    ambientColor = PurplePrimary.copy(alpha = glowAlpha),
                    spotColor = CyanAccent.copy(alpha = glowAlpha)
                )
                .clip(RoundedCornerShape(4.dp))
                .background(PanelMid)
                .border(1.dp, BorderGlow, RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .background(xpGradient)
            )
        }
    }
}