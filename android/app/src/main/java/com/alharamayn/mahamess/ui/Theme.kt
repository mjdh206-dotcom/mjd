package com.alharamayn.mahamess.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Brown = Color(0xFF4E342E)
val BrownDark = Color(0xFF3E2723)
val BrownLight = Color(0xFF8D6E63)
val Gold = Color(0xFFD4A017)
val GoldLight = Color(0xFFFFD54F)
val GoldDark = Color(0xFFB28704)
val Cream = Color(0xFFFFF8E1)
val CreamBg = Color(0xFFFDF6E3)
val Ink = Color(0xFF3E2723)
val Success = Color(0xFF2E7D32)
val Danger = Color(0xFFC62828)
val Muted = Color(0xFF8D7B68)

private val LightColors = lightColorScheme(
    primary = Brown,
    onPrimary = Color.White,
    primaryContainer = Cream,
    onPrimaryContainer = BrownDark,
    secondary = Gold,
    onSecondary = BrownDark,
    secondaryContainer = Color(0xFFFFF3CD),
    onSecondaryContainer = BrownDark,
    background = CreamBg,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Cream,
    onSurfaceVariant = Muted,
    error = Danger,
    outline = Color(0xFFE0D7C8),
)

private val DarkColors = darkColorScheme(
    primary = GoldLight,
    onPrimary = BrownDark,
    primaryContainer = Brown,
    onPrimaryContainer = Cream,
    secondary = Gold,
    onSecondary = BrownDark,
    secondaryContainer = Color(0xFF4A3517),
    onSecondaryContainer = Color(0xFFFFECB3),
    background = Color(0xFF1A1207),
    onBackground = Color(0xFFFFECCF),
    surface = Color(0xFF2C1E0F),
    onSurface = Color(0xFFFFECCF),
    surfaceVariant = Color(0xFF3E2F1E),
    onSurfaceVariant = Color(0xFFD9C6A8),
    error = Color(0xFFEF9A9A),
    outline = Color(0xFF5A4632),
)

@Composable
fun MahamessTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
