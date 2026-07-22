package com.cozypomo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    background = CozyBackground,
    surface = CozySurface,
    primary = CozyPrimary,
    onPrimary = CozyPrimaryInk,
    secondary = CozyAccent,
    onSecondary = CozyAccentInk,
    error = CozyWarn,
    onError = CozyWarnInk,
    onBackground = CozyInk,
    onSurface = CozyInk,
)

private val DarkColors = darkColorScheme(
    background = CozyBackgroundDark,
    surface = CozySurfaceDark,
    primary = CozyPrimaryDark,
    onPrimary = CozyPrimaryInkDark,
    secondary = CozyAccentDark,
    onSecondary = CozyAccentInkDark,
    error = CozyWarnDark,
    onError = CozyInkDark,
    onBackground = CozyInkDark,
    onSurface = CozyInkDark,
)

@Composable
fun CozyPomoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = CozyPomoTypography,
        content = content,
    )
}
