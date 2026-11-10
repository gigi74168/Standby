package com.standbyme.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Palette « braise » de secours (utilisée dans les aperçus). Sur l'appareil, l'appli suit
 * toujours les couleurs Material You du fond d'écran (minSdk 31).
 */
val EmberDarkColors = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF56190E),
    secondary = Color(0xFFE8C48E),
    tertiary = Color(0xFFE8C48E),
    background = Color(0xFF0E0B0A),
    onBackground = Color(0xFFF7EBE8),
    surface = Color(0xFF1C1210),
    onSurface = Color(0xFFF7EBE8),
    surfaceVariant = Color(0xFF2E1D1A),
    onSurfaceVariant = Color(0xFFC6AEA9),
    outline = Color(0x26FFB4A8),
)

/** Teinte rouge du mode nuit, appliquée en multiplication par-dessus tout l'écran. */
val NightFilterRed = Color(0xFF7A160A)

@Composable
fun StandByMeTheme(dynamic: Boolean = true, content: @Composable () -> Unit) {
    val colors = if (dynamic) dynamicDarkColorScheme(LocalContext.current) else EmberDarkColors
    MaterialTheme(colorScheme = colors, typography = StandByMeTypography, content = content)
}
