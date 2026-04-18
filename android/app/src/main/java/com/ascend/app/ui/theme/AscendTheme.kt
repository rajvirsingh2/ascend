package com.ascend.app.ui.theme

import androidx.compose.runtime.Composable

@Composable
fun AscendTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            background       = DarkColors.Void,
            surface          = DarkColors.Abyss,
            surfaceVariant   = DarkColors.Deep,
            primary          = DarkColors.Arcane,
            secondary        = DarkColors.Cyan,
            tertiary         = DarkColors.Gold,
            error            = DarkColors.Ember,
            onBackground     = DarkColors.TextPrimary,
            onSurface        = DarkColors.TextPrimary,
            onSurfaceVariant = DarkColors.TextMuted,
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