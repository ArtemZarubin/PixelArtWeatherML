package com.artemzarubin.weatherml.ui.util

import com.artemzarubin.weatherml.R // Ensure R is imported from your main application package

/**
 * Enum to represent different icon size prefixes for type safety.
 * The prefix value should match the beginning of your drawable resource names.
 */
enum class IconSizeQualifier(val prefix: String) {
    SIZE_128("icon_128_"),
    SIZE_512("icon_512_")
}

/**
 * Object responsible for mapping API weather icon codes to local drawable resource IDs.
 */
object WeatherIconMapper {

    // Map to associate a constructed resource name key (e.g., "icon_128_01d")
    // with its corresponding R.drawable resource ID.
    private val iconMap: Map<String, Int> by lazy { // lazy initialization for potentially large map
        mapOf(
            // --- 128px Icons ---
            // Clear sky
            "${IconSizeQualifier.SIZE_128.prefix}01d" to R.drawable.icon_128_01d, // day
            "${IconSizeQualifier.SIZE_128.prefix}01n" to R.drawable.icon_128_01n, // night
            // Few clouds
            "${IconSizeQualifier.SIZE_128.prefix}02d" to R.drawable.icon_128_02d, // day
            "${IconSizeQualifier.SIZE_128.prefix}02n" to R.drawable.icon_128_02n, // night
            // Scattered clouds
            "${IconSizeQualifier.SIZE_128.prefix}03d" to R.drawable.icon_128_03d, // day
            "${IconSizeQualifier.SIZE_128.prefix}03n" to R.drawable.icon_128_03n, // night (example: using same icon as day)
            // Broken clouds / Overcast clouds
            "${IconSizeQualifier.SIZE_128.prefix}04d" to R.drawable.icon_128_04d, // day
            "${IconSizeQualifier.SIZE_128.prefix}04n" to R.drawable.icon_128_04n, // night (example: using same icon as day)
            // Shower rain
            "${IconSizeQualifier.SIZE_128.prefix}09d" to R.drawable.icon_128_09d, // day
            "${IconSizeQualifier.SIZE_128.prefix}09n" to R.drawable.icon_128_09n, // night (example: using same icon as day)
            // Rain
            "${IconSizeQualifier.SIZE_128.prefix}10d" to R.drawable.icon_128_10d, // day
            "${IconSizeQualifier.SIZE_128.prefix}10n" to R.drawable.icon_128_10n, // night
            // Thunderstorm
            "${IconSizeQualifier.SIZE_128.prefix}11d" to R.drawable.icon_128_11d, // day
            "${IconSizeQualifier.SIZE_128.prefix}11n" to R.drawable.icon_128_11n, // night (example: using same icon as day)
            // Snow
            "${IconSizeQualifier.SIZE_128.prefix}13d" to R.drawable.icon_128_13d, // day
            "${IconSizeQualifier.SIZE_128.prefix}13n" to R.drawable.icon_128_13n, // night (example: using same icon as day)
            // Mist (fog, haze, etc.)
            "${IconSizeQualifier.SIZE_128.prefix}50d" to R.drawable.icon_128_50d, // day
            "${IconSizeQualifier.SIZE_128.prefix}50n" to R.drawable.icon_128_50n, // night (example: using same icon as day)

            // --- 512px Icons ---
            // Clear sky
            "${IconSizeQualifier.SIZE_512.prefix}01d" to R.drawable.icon_512_01d, // day
            "${IconSizeQualifier.SIZE_512.prefix}01n" to R.drawable.icon_512_01n, // night
            // Few clouds
            "${IconSizeQualifier.SIZE_512.prefix}02d" to R.drawable.icon_512_02d, // day
            "${IconSizeQualifier.SIZE_512.prefix}02n" to R.drawable.icon_512_02n, // night
            // Scattered clouds
            "${IconSizeQualifier.SIZE_512.prefix}03d" to R.drawable.icon_512_03d, // day
            "${IconSizeQualifier.SIZE_512.prefix}03n" to R.drawable.icon_512_03n, // night (example)
            // Broken clouds / Overcast clouds
            "${IconSizeQualifier.SIZE_512.prefix}04d" to R.drawable.icon_512_04d, // day
            "${IconSizeQualifier.SIZE_512.prefix}04n" to R.drawable.icon_512_04n, // night (example)
            // Shower rain
            "${IconSizeQualifier.SIZE_512.prefix}09d" to R.drawable.icon_512_09d, // day
            "${IconSizeQualifier.SIZE_512.prefix}09n" to R.drawable.icon_512_09n, // night (example)
            // Rain
            "${IconSizeQualifier.SIZE_512.prefix}10d" to R.drawable.icon_512_10d, // day
            "${IconSizeQualifier.SIZE_512.prefix}10n" to R.drawable.icon_512_10n, // night
            // Thunderstorm
            "${IconSizeQualifier.SIZE_512.prefix}11d" to R.drawable.icon_512_11d, // day
            "${IconSizeQualifier.SIZE_512.prefix}11n" to R.drawable.icon_512_11n, // night (example)
            // Snow
            "${IconSizeQualifier.SIZE_512.prefix}13d" to R.drawable.icon_512_13d, // day
            "${IconSizeQualifier.SIZE_512.prefix}13n" to R.drawable.icon_512_13n, // night (example)
            // Mist (fog, haze, etc.)
            "${IconSizeQualifier.SIZE_512.prefix}50d" to R.drawable.icon_512_50d, // day
            "${IconSizeQualifier.SIZE_512.prefix}50n" to R.drawable.icon_512_50n  // night (example)
        )
    }

    /**
     * Retrieves the drawable resource ID for a given weather icon code from the API
     * and the desired size qualifier.
     *
     * @param iconCodeFromApi The icon code string received from the weather API (e.g., "01d", "10n").
     * @param sizeQualifier The [IconSizeQualifier] enum indicating the desired icon size prefix.
     * @return The resource ID (e.g., `R.drawable.icon_128_01d`) or a placeholder ID if not found.
     */
    fun getResourceId(iconCodeFromApi: String?, sizeQualifier: IconSizeQualifier): Int {
        if (iconCodeFromApi.isNullOrBlank()) {
            return R.drawable.ic_weather_placeholder // Default placeholder
        }

        // Construct the lookup key for the map.
        // Ensure iconCodeFromApi is lowercased if your API might send mixed case
        // and your map keys (the part after the prefix) are consistently lowercase.
        val lookupKey = sizeQualifier.prefix + iconCodeFromApi.lowercase()

        return iconMap[lookupKey]
            ?: R.drawable.ic_weather_placeholder // Return mapped ID or placeholder
    }
}