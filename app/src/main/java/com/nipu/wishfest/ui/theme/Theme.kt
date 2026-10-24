package com.nipu.wishfest.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WishfestColorScheme = darkColorScheme(
    primary = Marigold,
    onPrimary = Color(0xFF1A1205),
    primaryContainer = SurfaceOverlay,
    onPrimaryContainer = MarigoldSoft,
    secondary = Blush,
    onSecondary = Color(0xFF2A0E14),
    secondaryContainer = Color(0xFF3D1F28),
    onSecondaryContainer = Color(0xFFFFD0D8),
    tertiary = MarigoldSoft,
    onTertiary = Color(0xFF1A1205),
    surface = SurfaceDeep,
    onSurface = Ivory,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = Mist,
    surfaceContainerHighest = SurfaceOverlay,
    background = Ink,
    onBackground = Ivory,
    outline = Hairline,
    outlineVariant = Color(0xFF2A2533),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF3B0A0A)
)

@Composable
fun WishfestTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = WishfestColorScheme,
        typography = Typography,
        content = content
    )
}
