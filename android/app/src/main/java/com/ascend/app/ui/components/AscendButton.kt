package com.ascend.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.app.ui.theme.Gradients
import com.ascend.app.util.ChamferShape

@Composable
fun AscendButton(
    text:String,
    onClick: ()-> Unit,
    gradient: List<Color> = Gradients.ArcaneFlow,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
){
    val haptic=LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if(enabled) 1f else 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    Box(
        modifier=modifier
            .scale(scale)
            .clip(ChamferShape(8.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = if(enabled) gradient else listOf(Color(0xFF333355), Color(0xFF333355)),
                    start = Offset(0f,0f),
                    end = Offset(Float.POSITIVE_INFINITY,0f)
                )
            )
            .clickable(enabled){
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ){
        Text(
            text=text.uppercase(),
            style=MaterialTheme.typography.labelLarge,
            color=Color.White,
            letterSpacing=0.08.sp
        )
    }
}