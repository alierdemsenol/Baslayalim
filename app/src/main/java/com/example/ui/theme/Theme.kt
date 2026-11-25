package com.example.ui.theme

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MinimalPrimary,
    onPrimary = MinimalOnPrimary,
    primaryContainer = MinimalPrimaryContainer,
    onPrimaryContainer = MinimalOnPrimaryContainer,
    secondary = MinimalSecondary,
    onSecondary = MinimalOnSecondary,
    secondaryContainer = MinimalSecondaryContainer,
    onSecondaryContainer = MinimalOnSecondaryContainer,
    tertiary = MinimalTertiary,
    onTertiary = MinimalOnTertiary,
    background = MinimalBackground,
    onBackground = MinimalOnSurface,
    surface = MinimalSurface,
    onSurface = MinimalOnSurface,
    surfaceVariant = MinimalSurfaceVariant,
    onSurfaceVariant = MinimalOnSurfaceVariant,
    error = MinimalError,
    onError = MinimalOnError
)

private val DarkColorScheme = darkColorScheme(
    primary = MinimalDarkPrimary,
    onPrimary = MinimalDarkOnPrimary,
    primaryContainer = MinimalDarkPrimaryContainer,
    onPrimaryContainer = MinimalDarkOnPrimaryContainer,
    background = MinimalDarkBackground,
    onBackground = MinimalDarkOnSurface,
    surface = MinimalDarkSurface,
    onSurface = MinimalDarkOnSurface,
    surfaceVariant = MinimalDarkSurfaceVariant,
    onSurfaceVariant = MinimalDarkOnSurface
)

@Composable
fun YKSOdakTheme(
    themeMode: String = "SYSTEM",
    accentColorHex: String = "#FF2D75",
    pitchBlackDark: Boolean = false,
    darkTheme: Boolean = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = androidx.compose.runtime.remember(accentColorHex, darkTheme, dynamicColor, pitchBlackDark) {
        val baseAccentColor = try {
            androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(accentColorHex))
        } catch (_: Exception) {
            MinimalPrimary
        }

        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> null
            darkTheme -> darkColorScheme(
                primary = baseAccentColor,
                onPrimary = androidx.compose.ui.graphics.Color.White,
                primaryContainer = baseAccentColor.copy(alpha = 0.35f),
                onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
                background = if (pitchBlackDark) androidx.compose.ui.graphics.Color.Black else MinimalDarkBackground,
                onBackground = MinimalDarkOnSurface,
                surface = if (pitchBlackDark) androidx.compose.ui.graphics.Color(0xFF080808) else MinimalDarkSurface,
                onSurface = MinimalDarkOnSurface,
                surfaceVariant = if (pitchBlackDark) androidx.compose.ui.graphics.Color(0xFF141414) else MinimalDarkSurfaceVariant,
                onSurfaceVariant = MinimalDarkOnSurface
            )
            else -> lightColorScheme(
                primary = baseAccentColor,
                onPrimary = androidx.compose.ui.graphics.Color.White,
                primaryContainer = baseAccentColor.copy(alpha = 0.18f),
                onPrimaryContainer = baseAccentColor,
                secondary = MinimalSecondary,
                onSecondary = MinimalOnSecondary,
                secondaryContainer = MinimalSecondaryContainer,
                onSecondaryContainer = MinimalOnSecondaryContainer,
                tertiary = MinimalTertiary,
                onTertiary = MinimalOnTertiary,
                background = MinimalBackground,
                onBackground = MinimalOnSurface,
                surface = MinimalSurface,
                onSurface = MinimalOnSurface,
                surfaceVariant = MinimalSurfaceVariant,
                onSurfaceVariant = MinimalOnSurfaceVariant,
                error = MinimalError,
                onError = MinimalOnError
            )
        }
    } ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    YKSOdakTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}
