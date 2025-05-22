// File: com/artemzarubin/weatherml/ui/theme/Type.kt
package com.artemzarubin.weatherml.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.artemzarubin.weatherml.R // Import your app's R class to access resources

// Example for "minecraft.ttf" - assuming it's a normal weight font
val MinecraftFontFamily = FontFamily(
    Font(R.font.minecraft, FontWeight.Normal)
)

// Example for "rainyhearts.ttf"
val RainyHeartsFontFamily = FontFamily(
    Font(R.font.rainyhearts, FontWeight.Normal)
)

// Example for one of the "slkscr" fonts (e.g., slkscr.ttf)
val SlkscrFontFamily = FontFamily(
    Font(R.font.slkscr, FontWeight.Normal)
    // If having bold/italic versions for slkscr, can add them:
    // Font(R.font.slkscrb, FontWeight.Bold) // Assuming slkscrb.ttf is the bold version
)

// Example for one of the "vcr_osd_mono" fonts (e.g., slkscr.ttf)
val VCROSDFontFamily = FontFamily(
    Font(R.font.vcr_osd_mono, FontWeight.Normal)
    // If having bold/italic versions for slkscr, can add them:
    // Font(R.font.slkscrb, FontWeight.Bold) // Assuming slkscrb.ttf is the bold version
)

// Choosing one font family to be the primary one for app's typography,
// or creating different TextStyles using different families.
val DefaultPixelFontFamily =
    VCROSDFontFamily // Change this to MinecraftFontFamily or RainyHeartsFontFamily to test others

// Define the App Typography using the chosen Pixel Art Font Family
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Normal, // Pixel fonts often don't have many weights
        fontSize = 57.sp,
        lineHeight = 60.sp, // Adjust line height for pixel fonts
        letterSpacing = 0.sp // Pixel fonts usually don't need letter spacing
    ),
    displayMedium = TextStyle(
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 48.sp
    ),
    displaySmall = TextStyle(
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 36.sp,
        lineHeight = 40.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Bold, // Or Normal if no bold version, or if bold looks bad
        fontSize = 32.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 32.sp
    ),
    headlineSmall = TextStyle( // For titles like "Current Weather", "Hourly Forecast"
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp, // Adjusted for pixel font
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle( // For City Name
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp, // Adjusted
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle( // For main temperature
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Normal, // Large temperature might look better normal
        fontSize = 72.sp, // This was large temperature size
        lineHeight = 76.sp
    ),
    titleSmall = TextStyle( // For "Feels like"
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp, // Adjusted
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle( // For weather description, details grid values
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp, // Adjusted
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle( // For details grid labels, POP%
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp, // Adjusted
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle( // For "Last update", hourly forecast time & condition
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp, // Adjusted
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle( // If needed even smaller text
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DefaultPixelFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 25.sp,
        lineHeight = 32.sp
    ),
)