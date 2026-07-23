package com.cozypomo.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Điền ĐẦY ĐỦ mọi vai trò ColorScheme (không chỉ primary/secondary/error) — nếu bỏ trống,
 * Material3 tự dùng màu tím baseline mặc định của nó cho NavigationBar (pill chọn), Slider
 * (track chưa gạt), Snackbar (nền inverseSurface)... khiến các phần đó lệch khỏi palette
 * thương hiệu dù các nút/chữ chính vẫn đúng màu. Xem docs/wireframes/use-case-flow.html
 * để đối chiếu — mọi màn hình phải theo đúng bảng màu đó, không chỉ những gì tự set thủ công.
 */
private val LightColors = lightColorScheme(
    background = CozyBackground,
    onBackground = CozyOnBackground,
    surface = CozySurface,
    onSurface = CozyOnSurface,
    surfaceVariant = CozySurfaceVariant,
    onSurfaceVariant = CozyOnSurfaceVariant,
    surfaceDim = CozySurfaceDim,
    surfaceBright = CozySurfaceBright,
    surfaceContainerLowest = CozySurfaceContainerLowest,
    surfaceContainerLow = CozySurfaceContainerLow,
    surfaceContainer = CozySurfaceContainer,
    surfaceContainerHigh = CozySurfaceContainerHigh,
    surfaceContainerHighest = CozySurfaceContainerHighest,
    primary = CozyPrimary,
    onPrimary = CozyOnPrimary,
    primaryContainer = CozyPrimaryContainer,
    onPrimaryContainer = CozyOnPrimaryContainer,
    secondary = CozySecondary,
    onSecondary = CozyOnSecondary,
    secondaryContainer = CozySecondaryContainer,
    onSecondaryContainer = CozyOnSecondaryContainer,
    tertiary = CozyTertiary,
    onTertiary = CozyOnTertiary,
    tertiaryContainer = CozyTertiaryContainer,
    onTertiaryContainer = CozyOnTertiaryContainer,
    error = CozyError,
    onError = CozyOnError,
    errorContainer = CozyErrorContainer,
    onErrorContainer = CozyOnErrorContainer,
    outline = CozyOutline,
    outlineVariant = CozyOutlineVariant,
    inverseSurface = CozyInverseSurface,
    inverseOnSurface = CozyInverseOnSurface,
    inversePrimary = CozyInversePrimary,
    scrim = CozyScrim,
)

private val DarkColors = darkColorScheme(
    background = CozyBackgroundDark,
    onBackground = CozyOnBackgroundDark,
    surface = CozySurfaceDark,
    onSurface = CozyOnSurfaceDark,
    surfaceVariant = CozySurfaceVariantDark,
    onSurfaceVariant = CozyOnSurfaceVariantDark,
    surfaceDim = CozySurfaceDimDark,
    surfaceBright = CozySurfaceBrightDark,
    surfaceContainerLowest = CozySurfaceContainerLowestDark,
    surfaceContainerLow = CozySurfaceContainerLowDark,
    surfaceContainer = CozySurfaceContainerDark,
    surfaceContainerHigh = CozySurfaceContainerHighDark,
    surfaceContainerHighest = CozySurfaceContainerHighestDark,
    primary = CozyPrimaryDark,
    onPrimary = CozyOnPrimaryDark,
    primaryContainer = CozyPrimaryContainerDark,
    onPrimaryContainer = CozyOnPrimaryContainerDark,
    secondary = CozySecondaryDark,
    onSecondary = CozyOnSecondaryDark,
    secondaryContainer = CozySecondaryContainerDark,
    onSecondaryContainer = CozyOnSecondaryContainerDark,
    tertiary = CozyTertiaryDark,
    onTertiary = CozyOnTertiaryDark,
    tertiaryContainer = CozyTertiaryContainerDark,
    onTertiaryContainer = CozyOnTertiaryContainerDark,
    error = CozyErrorDark,
    onError = CozyOnErrorDark,
    errorContainer = CozyErrorContainerDark,
    onErrorContainer = CozyOnErrorContainerDark,
    outline = CozyOutlineDark,
    outlineVariant = CozyOutlineVariantDark,
    inverseSurface = CozyInverseSurfaceDark,
    inverseOnSurface = CozyInverseOnSurfaceDark,
    inversePrimary = CozyInversePrimaryDark,
    scrim = CozyScrimDark,
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
