package com.skd.videoplayer.presentation.theme

import androidx.compose.ui.graphics.Color

// Lumora palette: near-black graphite surfaces with a warm amber accent and a cool mint secondary.

internal object DarkPalette {
    val Background = Color(0xFF0D0E12)
    val SurfaceLowest = Color(0xFF0A0B0E)
    val SurfaceLow = Color(0xFF14161B)
    val Surface = Color(0xFF181A20)
    val SurfaceHigh = Color(0xFF1F2128)
    val SurfaceHighest = Color(0xFF282B34)
    val OnSurface = Color(0xFFECEDF2)
    val OnSurfaceVariant = Color(0xFF9DA1AD)
    val Outline = Color(0xFF3A3D47)
    val OutlineVariant = Color(0xFF2A2D35)

    val Primary = Color(0xFFFFB45C)
    val OnPrimary = Color(0xFF2B1700)
    val PrimaryContainer = Color(0xFF4A2E0A)
    val OnPrimaryContainer = Color(0xFFFFDDB8)

    val Secondary = Color(0xFF8FD6C9)
    val OnSecondary = Color(0xFF00382F)
    val SecondaryContainer = Color(0xFF173C36)
    val OnSecondaryContainer = Color(0xFFB4F1E4)

    val Tertiary = Color(0xFFB8A6FF)
    val OnTertiary = Color(0xFF241466)

    val Error = Color(0xFFFF6B6B)
    val OnError = Color(0xFF3B0000)
    val ErrorContainer = Color(0xFF5C1414)
}

internal object LightPalette {
    val Background = Color(0xFFF7F6F3)
    val SurfaceLowest = Color(0xFFFFFFFF)
    val SurfaceLow = Color(0xFFF1F0EC)
    val Surface = Color(0xFFECEAE5)
    val SurfaceHigh = Color(0xFFE5E3DE)
    val SurfaceHighest = Color(0xFFDEDCD6)
    val OnSurface = Color(0xFF1A1B1F)
    val OnSurfaceVariant = Color(0xFF5A5E69)
    val Outline = Color(0xFFB9B8B3)
    val OutlineVariant = Color(0xFFD6D4CE)

    val Primary = Color(0xFFA85800)
    val OnPrimary = Color(0xFFFFFFFF)
    val PrimaryContainer = Color(0xFFFFDDB8)
    val OnPrimaryContainer = Color(0xFF2B1700)

    val Secondary = Color(0xFF006B5E)
    val OnSecondary = Color(0xFFFFFFFF)
    val SecondaryContainer = Color(0xFFB4F1E4)
    val OnSecondaryContainer = Color(0xFF00201B)

    val Tertiary = Color(0xFF5B4BC4)
    val OnTertiary = Color(0xFFFFFFFF)

    val Error = Color(0xFFBA1A1A)
    val OnError = Color(0xFFFFFFFF)
    val ErrorContainer = Color(0xFFFFDAD6)
}

/** Scrim drawn over thumbnails behind badges and text. */
val ThumbnailScrim = Color(0xB3000000)

/** Background of the "NEW" pill on unplayed videos. White text on it reads in light and dark themes. */
val NewBadgeRed = Color(0xFFE53935)
