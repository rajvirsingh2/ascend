package com.ascend.app.ui.components

import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.domain.model.Quest
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.toRarity
import com.ascend.app.util.ChamferShape

@Composable
fun QuestCard(
    quest: Quest,
    onComplete:()-> Unit,
    onSkip:()->Unit,
    modifier: Modifier=Modifier
){
    val rarity=quest.difficulty.toRarity()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    listOf(DarkColors.Abyss, DarkColors.Deep)
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(rarity.gradient),
                shape = RoundedCornerShape(12.dp)
            )
    ){
        Box(modifier
            .width(3.dp)
            .height(130.dp)
            .background(Brush.verticalGradient(rarity.gradient))
        )
        Column(Modifier
            .fillMaxWidth()
            .padding(start=14.dp, end=12.dp, top=12.dp, bottom=12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RarityBadge(rarity)
                if (quest.isAiGenerated) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "AI Quest",
                        fontSize = 9.sp,
                        color = rarity.borderColor.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.06.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = quest.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${quest.xpReward}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = rarity.borderColor
                    )
                    Text(
                        text = "XP",
                        fontSize = 9.sp,
                        color = DarkColors.TextHint,
                        letterSpacing = 0.06.sp
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = quest.description,
                fontSize = 11.sp,
                color = DarkColors.TextMuted,
                lineHeight = 16.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = quest.description,
                fontSize = 11.sp,
                color = DarkColors.TextMuted,
                lineHeight = 16.sp
            )
        }
    }
}