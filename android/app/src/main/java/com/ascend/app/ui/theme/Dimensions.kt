package com.ascend.app.ui.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimensions(
    val default: Dp = 0.dp,
    val xxs: Dp = 2.dp,
    val xs: Dp = 4.dp,
    val sm: Dp = 8.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 24.dp,
    val xl: Dp = 32.dp,
    val xxl: Dp = 48.dp,
    val xxxl: Dp = 64.dp,
    
    // Semantic spacings
    val screenPadding: Dp = 16.dp,
    val cardPadding: Dp = 16.dp,
    val itemSpacing: Dp = 8.dp
)

val CompactDimensions = Dimensions(
    screenPadding = 16.dp,
    cardPadding = 16.dp,
    itemSpacing = 8.dp
)

val MediumDimensions = Dimensions(
    screenPadding = 24.dp,
    cardPadding = 24.dp,
    itemSpacing = 12.dp,
    md = 20.dp,
    lg = 32.dp,
    xl = 40.dp,
    xxl = 56.dp
)

val ExpandedDimensions = Dimensions(
    screenPadding = 32.dp,
    cardPadding = 32.dp,
    itemSpacing = 16.dp,
    sm = 12.dp,
    md = 24.dp,
    lg = 40.dp,
    xl = 48.dp,
    xxl = 72.dp,
    xxxl = 96.dp
)

val LocalSpacing = compositionLocalOf { CompactDimensions }
