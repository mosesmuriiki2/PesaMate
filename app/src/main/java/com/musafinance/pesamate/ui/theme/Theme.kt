package com.musafinance.pesamate.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784), // Light Green
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF1B5E20), // Dark Green
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFFA5D6A7),
    onSecondary = Color(0xFF1B5E20),
    secondaryContainer = Color(0xFF2E7D32),
    onSecondaryContainer = Color(0xFFE8F5E9),
    tertiary = Color(0xFF80CBC4),
    onTertiary = Color(0xFF004D40),
    tertiaryContainer = Color(0xFF00695C),
    onTertiaryContainer = Color(0xFFB2DFDB),
    background = Color(0xFF0A0D0A),
    surface = Color(0xFF0A0D0A),
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF1B2B1B),
    onSurfaceVariant = Color(0xFFC8E6C9),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2E7D32), // Green 800
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9), // Green 100
    onPrimaryContainer = Color(0xFF003300),
    secondary = Color(0xFF43A047),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = Color(0xFF00897B),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFB2DFDB),
    onTertiaryContainer = Color(0xFF004D40),
    background = Color(0xFFF1F8E9),
    surface = Color(0xFFF1F8E9),
    onSurface = Color(0xFF1B5E20),
    surfaceVariant = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF2E7D32),
)

@Composable
fun PesaMateTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
