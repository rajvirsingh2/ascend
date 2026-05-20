package com.ascend.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ascend.app.ui.theme.DarkColors
import com.ascend.app.ui.theme.Gradients
import com.ascend.app.util.ChamferShape
import com.ascend.app.util.horizontalGradientBrush

@Composable
fun AscendButton(
    text:String,
    onClick: ()-> Unit,
    gradient: List<Color> = Gradients.ArcaneFlow,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
    enabled: Boolean = true
){
    val haptic=LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if(enabled) 1f else 0.96f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "btn_scale"
    )

    val brush=if(enabled)
        horizontalGradientBrush(gradient)
    else
        Brush.horizontalGradient(listOf(Color(0xFF333355), Color(0xFF333355)))

    Box(
        modifier=modifier
            .scale(scale)
            .clip(ChamferShape(8.dp))
            .background(brush)
            .clickable(enabled){
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ){
        Text(
            text=text.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            style=MaterialTheme.typography.labelLarge,
            color=Color.White,
            letterSpacing=0.08.sp
        )
    }
}

@Composable
fun AscendOutlinedButton(
    text:String,
    onClick: () -> Unit,
    borderColor: Color= Color(0xFF7B61FF)
){
    val haptic=LocalHapticFeedback.current
    Box(
        modifier=Modifier
            .clip(ChamferShape(6.dp))
            .background(borderColor.copy(alpha = 0.12f))
            .clickable{
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ){
        Text(
            text=text.uppercase(),
            fontSize=11.sp,
            fontWeight=FontWeight.Medium,            color=borderColor,
            letterSpacing=0.06.sp
        )
    }
}



@Preview(showBackground = true, name = "Ascend Buttons")
@Composable
fun AscendButtonsPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .background(DarkColors.Void) // Using your app's dark background
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // 1. Primary Button (Enabled)
            AscendButton(
                text = "Primary Action",
                onClick = {},
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Primary Button (Disabled)
            AscendButton(
                text = "Disabled Action",
                onClick = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth()
            )

            // 3. Outlined Button
            AscendOutlinedButton(
                text = "Secondary Action",
                onClick = {}
            )
        }
    }
}