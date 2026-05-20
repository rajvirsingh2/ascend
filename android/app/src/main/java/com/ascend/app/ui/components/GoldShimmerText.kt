package com.ascend.app.ui.components

import android.annotation.SuppressLint
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import com.ascend.app.ui.theme.Gradients
import com.ascend.app.util.rememberShimmerBrush

@Composable
fun GoldShimmerText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Medium,
    @SuppressLint("ModifierParameter") modifier: Modifier= Modifier
) {
    val brush= rememberShimmerBrush(Gradients.GoldShimmer)
    Text(
        text = text,
        fontSize = fontSize,
        modifier=modifier,
        fontWeight = fontWeight,
        style = TextStyle(
            brush =brush,
            fontSize=fontSize,
            fontWeight = fontWeight
        )
    )
}

