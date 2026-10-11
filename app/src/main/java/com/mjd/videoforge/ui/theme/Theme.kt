package com.mjd.videoforge.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Light Theme Colors
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B6B93),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC6E8F5),
    onPrimaryContainer = Color(0xFF001F29),
    secondary = Color(0xFFFC9D4A),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFDCC0),
    onSecondaryContainer = Color(0xFF2B1500),
    tertiary = Color(0xFF5C6280),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1E0FF),
    onTertiaryContainer = Color(0xFF181E39),
    background = Color(0xFFF8FAFB),
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFFF0F0F5),
    onSurfaceVariant = Color(0xFF49454F),
    error = Color(0xFFB00020),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFF79747E),
)

// Dark Theme Colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4FC3B0),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF00504A),
    onPrimaryContainer = Color(0xFF6DFBD0),
    secondary = Color(0xFFFFB74D),
    onSecondary = Color(0xFF422C00),
    secondaryContainer = Color(0xFF5F4000),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Color(0xFFC5C6ED),
    onTertiary = Color(0xFF2E304F),
    tertiaryContainer = Color(0xFF444667),
    onTertiaryContainer = Color(0xFFE1E0FF),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = Color(0xFFCF6679),
    onError = Color.Black,
    errorContainer = Color(0xFFB00020),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF938F99),
)

@Composable
fun VideoForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VideoForgeTypography,
        content = content
    )
}
