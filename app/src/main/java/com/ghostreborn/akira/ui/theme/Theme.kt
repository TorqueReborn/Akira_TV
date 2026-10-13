package com.ghostreborn.akira.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Android TV interface uses Dark Theme by default for cinema-like experience
private val AkiraTVDarkColorScheme = darkColorScheme(
    primary = AkiraPrimaryLight,
    onPrimary = Color.White,

    primaryContainer = AkiraDarkAccentBackground,
    onPrimaryContainer = AkiraDarkTextPrimary,

    secondary = AkiraPrimary,
    onSecondary = Color.White,

    background = AkiraDarkBackground,
    onBackground = AkiraDarkTextPrimary,

    surface = AkiraDarkSurface,
    onSurface = AkiraDarkTextPrimary,

    surfaceVariant = AkiraDarkSurfaceVariant,
    onSurfaceVariant = AkiraDarkTextSecondary,

    outline = AkiraDarkSurfaceBorder,
    outlineVariant = AkiraDarkSurfaceBorder
)

@Composable
fun AkiraTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = AkiraTVDarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = false
                insetsController.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}