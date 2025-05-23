package com.artemzarubin.weatherml.ui.theme

// No need to import DarkColorScheme and LightColorScheme if they are in the same package (defined in Color.kt)

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.artemzarubin.weatherml.data.preferences.AppTheme

@Composable
fun WeatherMLTheme(
    userSelectedTheme: AppTheme = AppTheme.SYSTEM, // <--- ПАРАМЕТР З ТИПОМ AppTheme
    content: @Composable () -> Unit
) {
    val darkThemeEnabled: Boolean = when (userSelectedTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkThemeEnabled) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography, // AppTypography from Type.kt
        content = content
    )
}