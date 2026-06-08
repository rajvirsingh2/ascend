package com.ascend.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.QuestRarity
import com.ascend.app.util.ChamferShape
import com.ascend.app.util.horizontalGradientBrush

@Composable
fun RarityBadge(
    rarity: QuestRarity,
    modifier: Modifier=Modifier
){
    Box(
        modifier= Modifier
            .clip(ChamferShape(4.dp))
            .background(horizontalGradientBrush(rarity.gradient))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ){
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${rarity.rank}-RANK",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                letterSpacing = 0.08.sp
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text=". ${rarity.label.uppercase()}",
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.8f),
                letterSpacing = 0.06.sp
            )
        }
    }
}