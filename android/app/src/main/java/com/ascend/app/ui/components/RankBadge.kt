package com.ascend.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.*

fun rankFromLevel(level: Int): Pair<String, Color> = when {
    level >= 80 -> "S" to RankS
    level >= 60 -> "A" to RankA
    level >= 40 -> "B" to RankB
    level >= 20 -> "C" to RankC
    level >= 10 -> "D" to RankD
    else        -> "E" to RankE
}

@Composable
fun RankBadge(level: Int, modifier: Modifier = Modifier) {
    val (rank, color) = rankFromLevel(level)
    Box(
        modifier = modifier
            .shadow(8.dp, RoundedCornerShape(6.dp), ambientColor = color, spotColor = color)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "RANK $rank",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            color = color
        )
    }
}

@Composable
fun QuestDifficultyBadge(difficulty: Int, modifier: Modifier = Modifier) {
    val (label, color) = when (difficulty) {
        5    -> "S-RANK" to RankS
        4    -> "A-RANK" to RankA
        3    -> "B-RANK" to RankB
        2    -> "C-RANK" to RankC
        else -> "D-RANK" to RankD
    }
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp, color = color)
    }
}