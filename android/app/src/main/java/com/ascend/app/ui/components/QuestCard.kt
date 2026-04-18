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
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if(flipped) 180f else 0f,
        animationSpec = tween(500, easing = EaseInOutCubic),
        label = "card_flip"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer(rotationY = rotation)
            .clip(RoundedCornerShape(12.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(DarkColors.Abyss, DarkColors.Deep),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
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
            .fillMaxHeight()
            .background(Brush.verticalGradient(rarity.gradient))
        )
        Column(Modifier.padding(
            start=14.dp, end=12.dp, top=12.dp, bottom=12.dp
        )) {
            Row(verticalAlignment = Alignment.CenterVertically){
                Box(Modifier
                    .clip(ChamferShape(4.dp))
                    .background(Brush.horizontalGradient(rarity.gradient))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ){
                    Text(rarity.label, fontSize = 9.sp, color=Color.White, fontWeight = FontWeight.Medium, letterSpacing = 0.06.sp)
                }
                if(quest.isAiGenerated){
                    Spacer(Modifier.width(6.dp))
                    Text(text="AI Generated", fontSize = 9.sp,
                        color = rarity.borderColor.copy(alpha = 0.7f),
                        letterSpacing = 0.06.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(quest.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = DarkColors.TextPrimary)
            Text(quest.description, fontSize = 11.sp, color = DarkColors.TextMuted,
                lineHeight = 16.sp)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AscendButton("ACCEPT", onComplete,
                    gradient = rarity.gradient,
                    modifier = Modifier.weight(1f))
                OutlinedButton(
                    onClick = onSkip,
                    border = BorderStroke(1.dp, rarity.borderColor.copy(.5f)),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = rarity.borderColor)
                ) { Text("SKIP", fontSize = 11.sp, letterSpacing = 0.04.sp) }
            }
        }
    }
}