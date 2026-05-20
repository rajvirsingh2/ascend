package com.ascend.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AscendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
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
    } else {
        lightColorScheme(
            background       = LightColors.Frost,
            surface          = LightColors.Surface,
            surfaceVariant   = LightColors.Mist,
            primary          = LightColors.Royal,
            secondary        = LightColors.Azure,
            tertiary         = LightColors.Amber,
            error            = LightColors.Flame,
            onBackground     = LightColors.TextPrimary,
            onSurface        = LightColors.TextPrimary,
            onSurfaceVariant = LightColors.TextMuted,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AscendTypography,
        content = content
    )
}