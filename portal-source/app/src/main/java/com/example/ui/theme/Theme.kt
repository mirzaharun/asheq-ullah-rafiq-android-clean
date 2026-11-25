package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlue800,
    onPrimary = Color.White,
    primaryContainer = RoyalBlue50,
    onPrimaryContainer = RoyalBlue900,
    secondary = OceanTeal,
    onSecondary = Color.White,
    secondaryContainer = PastelCardCyan,
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = GoldAccent,
    onTertiary = Color(0xFF3E2723),
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = PastelCardBlue,
    onSurfaceVariant = TextSecondary,
    outline = DividerColor
)

private val DarkColorScheme = darkColorScheme(
    primary = RoyalBlue600,
    onPrimary = Color.White,
    primaryContainer = RoyalBlue900,
    onPrimaryContainer = RoyalBlue100,
    secondary = OceanTeal,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = PastelCardCyan,
    tertiary = GoldAccent,
    onTertiary = Color.Black,
    background = Color(0xFF0B132B),
    onBackground = Color(0xFFE2E8F0),
    surface = Color(0xFF141F36),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF1E2D4A),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent branding colors
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
        typography = Typography,
        content = content
    )
}
