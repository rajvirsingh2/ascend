package com.ascend.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.domain.model.Habit
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients

@Composable
fun HabitRow(
    habit: Habit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (habit.completedToday)
            Color(0xFF39FF14).copy(alpha = 0.4f)
        else
            DarkColors.Arcane.copy(alpha = 0.3f),
        animationSpec = tween(400),
        label = "habit_border"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (habit.completedToday)
                    Brush.horizontalGradient(
                        listOf(Color(0xFF0D1A0D), Color(0xFF0A140A))
                    )
                else
                    Brush.horizontalGradient(
                        listOf(DarkColors.Abyss, DarkColors.Deep)
                    )
            )
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // check box
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (habit.completedToday)
                        Brush.linearGradient(listOf(Color(0xFF1A6B1A), Color(0xFF39FF14)))
                    else
                        Brush.linearGradient(listOf(DarkColors.Deep, DarkColors.Dusk))
                )
                .border(
                    1.5.dp,
                    if (habit.completedToday) Color.Transparent
                    else DarkColors.Arcane.copy(0.5f),
                    RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (habit.completedToday) {
                Text("✓", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = habit.title,
                fontSize = 13.sp,
                color = if (habit.completedToday)
                    Color(0xFF90CC90) else DarkColors.TextPrimary,
                fontWeight = FontWeight.Medium,
                textDecoration = if (habit.completedToday)
                    TextDecoration.LineThrough else TextDecoration.None
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (habit.currentStreak > 0) Color(0xFFFFD700)
                            else DarkColors.TextHint
                        )
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (habit.currentStreak > 0)
                        "${habit.currentStreak}-DAY STREAK"
                    else "START YOUR STREAK",
                    fontSize = 10.sp,
                    color = if (habit.currentStreak > 0)
                        Color(0xFFFFD700) else DarkColors.TextHint,
                    letterSpacing = 0.06.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (habit.completedToday) {
            Text(
                text = "+${habit.xpReward} XP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF39FF14)
            )
        } else {
            AscendButton(
                text = "CHECK IN",
                onClick = onComplete,
                gradient = Gradients.ArcaneFlow
            )
        }
    }
}