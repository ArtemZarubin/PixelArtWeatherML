package com.artemzarubin.weatherml.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 32-color Pixel Art Palette (as defined before)
val px_White = Color(0xFFFFFFFF)
val px_LightGreyStone = Color(0xFFCBC4BA)
val px_CoolGrey = Color(0xFF8E9C99)
val px_OliveGreenDark = Color(0xFF8B8D5A)
val px_DarkOlive = Color(0xFF5C6A53)
val px_VeryDarkGreen = Color(0xFF262921)
val px_AlmostBlack = Color(0xFF141415)
val px_CyanLight = Color(0xFF72CBC6)
val px_BlueLight = Color(0xFF729DE0)
val px_BlueSteel = Color(0xFF3C7E97)
val px_BlueGreyDark = Color(0xFF4A5C78)
val px_TealDark = Color(0xFF2F4541)
val px_GreenLime = Color(0xFF9EDB7B)
val px_GreenOliveLight = Color(0xFFA2AA54)
val px_GreenForest = Color(0xFF68924F)
val px_GreenDark = Color(0xFF305838)
val px_YellowSand = Color(0xFFEFC26D)
val px_BrownMustard = Color(0xFFB68D47)
val px_BrownOliveDark = Color(0xFF616528)
val px_BrownDark = Color(0xFF4A4126)
val px_PurpleLavender = Color(0xFFA98AC6)
val px_MauveGrey = Color(0xFF87716F)
val px_PurpleDarkPlum = Color(0xFF6A445A)
val px_BrownMaroon = Color(0xFF4C3435)
val px_OrangePeach = Color(0xFFE08F5E)
val px_OrangeBurnt = Color(0xFFCD6627)
val px_BrownMedium = Color(0xFF875C30)
val px_BrownDarkRed = Color(0xFF703A1A)
val px_TerracottaLight = Color(0xFFBF8D77)
val px_TerracottaRed = Color(0xFFCB593A)
val px_BrownRose = Color(0xFF7D544F)
val px_MaroonDark = Color(0xFF682F2F)

// Light Theme Color Scheme using palette
val LightColorScheme = lightColorScheme(
    primary = px_BlueSteel,           // Main interactive elements, buttons
    onPrimary = px_White,             // Text/icons on primary color
    primaryContainer = px_BlueLight,  // Lighter shade for containers related to primary
    onPrimaryContainer = px_AlmostBlack, // Text on primary container

    secondary = px_GreenForest,       // Secondary interactive elements
    onSecondary = px_White,
    secondaryContainer = px_GreenLime,
    onSecondaryContainer = px_VeryDarkGreen,

    tertiary = px_OrangePeach,        // Accent color
    onTertiary = px_AlmostBlack,
    tertiaryContainer = px_YellowSand,
    onTertiaryContainer = px_BrownDark,

    error = px_TerracottaRed,         // Errors
    onError = px_White,
    errorContainer = px_TerracottaLight,
    onErrorContainer = px_MaroonDark,

    background = px_LightGreyStone,   // Screen background
    onBackground = px_AlmostBlack,    // Text on screen background

    surface = px_White,               // Surface of cards, sheets, menus
    onSurface = px_AlmostBlack,       // Text on surfaces

    surfaceVariant = px_CoolGrey,       // Subtle backgrounds, borders
    onSurfaceVariant = px_VeryDarkGreen,// Text/icons on surface variants (e.g., "Feels like" text)

    outline = px_DarkOlive            // Borders, dividers
)

// Dark Theme Color Scheme using palette
val DarkColorScheme = darkColorScheme(
    primary = px_BlueLight,           // Main interactive elements
    onPrimary = px_AlmostBlack,
    primaryContainer = px_BlueSteel,
    onPrimaryContainer = px_White,

    secondary = px_GreenLime,
    onSecondary = px_AlmostBlack,
    secondaryContainer = px_GreenForest,
    onSecondaryContainer = px_White,

    tertiary = px_YellowSand,
    onTertiary = px_AlmostBlack,
    tertiaryContainer = px_OrangePeach,
    onTertiaryContainer = px_BrownDark,

    error = px_OrangeBurnt,
    onError = px_AlmostBlack,
    errorContainer = px_BrownDarkRed,
    onErrorContainer = px_White,

    background = px_AlmostBlack,      // Screen background
    onBackground = px_LightGreyStone, // Main text color

    surface = px_VeryDarkGreen,       // Surface of cards, sheets, menus
    onSurface = px_LightGreyStone,    // Text on surfaces

    surfaceVariant = px_BlueGreyDark,   // Subtle backgrounds, borders
    onSurfaceVariant = px_CoolGrey,     // Text/icons on surface variants

    outline = px_OliveGreenDark       // Borders, dividers
)