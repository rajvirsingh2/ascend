package com.ascend.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients

data class AchievementItem(
    val key: String,
    val title: String,
    val tag: String,
    val icon: String,
    val earned: Boolean,
    val earnedAt: String?
)

@Composable
fun AchievementsSection(
    achievements: List<AchievementItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkColors.Abyss)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ACHIEVEMENTS", fontSize = 11.sp, fontWeight = FontWeight.Medium,
                color = DarkColors.Arcane, letterSpacing = 0.1.sp)
            val earnedCount = achievements.count { it.earned }
            Text("$earnedCount / ${achievements.size}",
                fontSize = 11.sp, color = DarkColors.Gold, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.heightIn(max = 600.dp)
        ) {
            items(achievements) { ach ->
                AchievementBadge(achievement = ach)
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: AchievementItem) {
    val alpha = if (achievement.earned) 1f else 0.35f

    Column(
        modifier = Modifier
            .alpha(alpha)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (achievement.earned)
                    Brush.linearGradient(
                        listOf(DarkColors.Deep, DarkColors.Dusk)
                    )
                else Brush.linearGradient(
                    listOf(DarkColors.Abyss, DarkColors.Abyss)
                )
            )
            .then(
                if (achievement.earned)
                    Modifier.padding(1.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(Gradients.ArcaneFlow)
                        )
                else Modifier
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(achievement.icon, fontSize = 24.sp)
        Spacer(Modifier.height(4.dp))
        Text(
            text = achievement.tag,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = if (achievement.earned) DarkColors.Gold else DarkColors.TextHint,
            letterSpacing = 0.06.sp,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = achievement.title,
            fontSize = 9.sp,
            color = if (achievement.earned) DarkColors.TextMuted else DarkColors.TextHint,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 12.sp
        )
    }
}


@Preview(showBackground = true, backgroundColor = 0xFF000000, name = "Achievements Section")
@Composable
fun AchievementsSectionPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0F)) // Simulating DarkColors.Void context
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            AchievementsSection(
                achievements = listOf(
                    AchievementItem(
                        key = "awakened",
                        title = "Enter the System",
                        tag = "AWAKENED",
                        icon = "👁️",
                        earned = true,
                        earnedAt = "2026-05-10"
                    ),
                    AchievementItem(
                        key = "first_blood",
                        title = "Clear 1st Quest",
                        tag = "FIRST BLOOD",
                        icon = "⚔️",
                        earned = true,
                        earnedAt = "2026-05-11"
                    ),
                    AchievementItem(
                        key = "shadow_step",
                        title = "Reach Level 10",
                        tag = "SHADOW STEP",
                        icon = "👣",
                        earned = true,
                        earnedAt = "2026-05-19"
                    ),
                    AchievementItem(
                        key = "iron_will",
                        title = "7-Day Streak",
                        tag = "IRON WILL",
                        icon = "🛡️",
                        earned = false,
                        earnedAt = null
                    ),
                    AchievementItem(
                        key = "treasury",
                        title = "Earn 10k XP",
                        tag = "HOARDER",
                        icon = "💰",
                        earned = false,
                        earnedAt = null
                    ),
                    AchievementItem(
                        key = "monarch",
                        title = "Reach Level 100",
                        tag = "MONARCH",
                        icon = "👑",
                        earned = false,
                        earnedAt = null
                    )
                )
            )
        }
    }
}
