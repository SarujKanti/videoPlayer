package com.skd.videoplayer.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val DarkColors = darkColorScheme(
    primary = DarkPalette.Primary,
    onPrimary = DarkPalette.OnPrimary,
    primaryContainer = DarkPalette.PrimaryContainer,
    onPrimaryContainer = DarkPalette.OnPrimaryContainer,
    secondary = DarkPalette.Secondary,
    onSecondary = DarkPalette.OnSecondary,
    secondaryContainer = DarkPalette.SecondaryContainer,
    onSecondaryContainer = DarkPalette.OnSecondaryContainer,
    tertiary = DarkPalette.Tertiary,
    onTertiary = DarkPalette.OnTertiary,
    background = DarkPalette.Background,
    onBackground = DarkPalette.OnSurface,
    surface = DarkPalette.Background,
    onSurface = DarkPalette.OnSurface,
    surfaceVariant = DarkPalette.SurfaceHigh,
    onSurfaceVariant = DarkPalette.OnSurfaceVariant,
    surfaceContainerLowest = DarkPalette.SurfaceLowest,
    surfaceContainerLow = DarkPalette.SurfaceLow,
    surfaceContainer = DarkPalette.Surface,
    surfaceContainerHigh = DarkPalette.SurfaceHigh,
    surfaceContainerHighest = DarkPalette.SurfaceHighest,
    outline = DarkPalette.Outline,
    outlineVariant = DarkPalette.OutlineVariant,
    error = DarkPalette.Error,
    onError = DarkPalette.OnError,
    errorContainer = DarkPalette.ErrorContainer,
)

private val LightColors = lightColorScheme(
    primary = LightPalette.Primary,
    onPrimary = LightPalette.OnPrimary,
    primaryContainer = LightPalette.PrimaryContainer,
    onPrimaryContainer = LightPalette.OnPrimaryContainer,
    secondary = LightPalette.Secondary,
    onSecondary = LightPalette.OnSecondary,
    secondaryContainer = LightPalette.SecondaryContainer,
    onSecondaryContainer = LightPalette.OnSecondaryContainer,
    tertiary = LightPalette.Tertiary,
    onTertiary = LightPalette.OnTertiary,
    background = LightPalette.Background,
    onBackground = LightPalette.OnSurface,
    surface = LightPalette.Background,
    onSurface = LightPalette.OnSurface,
    surfaceVariant = LightPalette.SurfaceHigh,
    onSurfaceVariant = LightPalette.OnSurfaceVariant,
    surfaceContainerLowest = LightPalette.SurfaceLowest,
    surfaceContainerLow = LightPalette.SurfaceLow,
    surfaceContainer = LightPalette.Surface,
    surfaceContainerHigh = LightPalette.SurfaceHigh,
    surfaceContainerHighest = LightPalette.SurfaceHighest,
    outline = LightPalette.Outline,
    outlineVariant = LightPalette.OutlineVariant,
    error = LightPalette.Error,
    onError = LightPalette.OnError,
    errorContainer = LightPalette.ErrorContainer,
)

private val LumoraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** App theme. Dark is the default; dynamic colour is deliberately off to keep the app's identity. */
@Composable
fun LumoraTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = LumoraTypography,
        shapes = LumoraShapes,
        content = content,
    )
}
