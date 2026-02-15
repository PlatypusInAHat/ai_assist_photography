package com.aiphoto.assist.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64FFDA),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF7FFFD4),
    secondary = Color(0xFFB2CCC6),
    onSecondary = Color(0xFF1D352F),
    secondaryContainer = Color(0xFF344C45),
    onSecondaryContainer = Color(0xFFCDE8E2),
    tertiary = Color(0xFFADCAE6),
    background = Color(0xFF0A0F14),
    onBackground = Color(0xFFE1E3E0),
    surface = Color(0xFF121820),
    onSurface = Color(0xFFE1E3E0),
    surfaceVariant = Color(0xFF1E2A30),
    onSurfaceVariant = Color(0xFFBFC9C4),
    error = Color(0xFFFF6B6B),
)

@Composable
fun AIPhotoAssistTheme(content: @Composable () -> Unit) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
