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
    MaterialTheme(
        colorScheme = AscendColorScheme,
        typography  = AscendTypography,
        content     = content
    )
}