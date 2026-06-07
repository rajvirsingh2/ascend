package com.ascend.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle

@Composable
fun RollingDigitCounter(
    count: Int,
    textStyle: TextStyle,
    color: Color,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    var animatedTarget by remember { mutableIntStateOf(if (isPreview) count else 0) }
    LaunchedEffect(count) { animatedTarget = count }

    val countString = if (animatedTarget >= 1000) "%,d".format(animatedTarget) else animatedTarget.toString()

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        countString.forEachIndexed { index, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    (slideInVertically(spring(dampingRatio = 0.7f, stiffness = 80f)) { it } + fadeIn(tween(300))) togetherWith
                            (slideOutVertically(spring(dampingRatio = 0.7f, stiffness = 80f)) { -it } + fadeOut(tween(300)))
                },
                label = "RollingDigit_$index"
            ) { digit ->
                Text(
                    text = digit.toString(),
                    style = textStyle,
                    color = color
                )
            }
        }
    }
}
