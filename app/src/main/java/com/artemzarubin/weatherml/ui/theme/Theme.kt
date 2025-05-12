package com.artemzarubin.weatherml.ui.theme

// No need to import DarkColorScheme and LightColorScheme if they are in the same package (defined in Color.kt)

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun WeatherMLTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled for a consistent pixel art look
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme // Using defined DarkColorScheme
    } else {
        LightColorScheme // Using defined LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // AppTypography from Type.kt
        content = content
    )
}