package com.ascend.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AscendColorScheme = darkColorScheme(
    primary           = PurplePrimary,
    onPrimary         = TextPrimary,
    primaryContainer  = PanelDark,
    secondary         = CyanAccent,
    onSecondary       = SystemBlack,
    background        = SystemBlack,
    onBackground      = TextPrimary,
    surface           = PanelDark,
    onSurface         = TextPrimary,
    surfaceVariant    = PanelMid,
    outline           = BorderGlow,
    error             = DangerRed,
)

@Composable
fun AscendTheme(content: @Composable () -> Unit) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val dimensions = when {
        screenWidth < 600 -> CompactDimensions
        screenWidth < 840 -> MediumDimensions
        else -> ExpandedDimensions
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalSpacing provides dimensions) {
        MaterialTheme(
            colorScheme = AscendColorScheme,
            typography  = AscendTypography,
            content     = content
        )
    }
}