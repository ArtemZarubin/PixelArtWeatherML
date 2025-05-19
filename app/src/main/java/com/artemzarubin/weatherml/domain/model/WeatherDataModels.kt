package com.artemzarubin.weatherml.domain.model

/**
 * Represents the current weather conditions for a location.
 */
data class CurrentWeather(
    val dateTimeMillis: Long,         // Unix timestamp, UTC
    val sunriseMillis: Long,          // Sunrise time, Unix, UTC
    val sunsetMillis: Long,           // Sunset time, Unix, UTC
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val pressureHpa: Int,
    val humidityPercent: Int,
    val cloudinessPercent: Int,       // Cloudiness, %
    val visibilityMeters: Int,
    val windSpeedMps: Double,         // Wind speed, meter/sec
    val windDirectionDegrees: Int,
    val weatherConditionId: Int,      // Weather condition ID (for icon mapping, ML model)
    val weatherCondition: String,     // Main weather condition (e.g., "Clouds", "Rain")
    val weatherDescription: String,   // Detailed weather description
    val weatherIconId: String,        // Icon ID from API (e.g., "01d")
    val cityName: String,
    val countryCode: String?,         // e.g., "UA"
    val timezoneOffsetSeconds: Int,    // Shift in seconds from UTC for the location
    val mlFeelsLikeCelsius: Float? = null
)

/**
 * Represents a single hourly forecast item.
 */
data class HourlyForecast(
    val dateTimeMillis: Long,
    val temperatureCelsius: Double,
    val feelsLikeCelsius: Double,
    val probabilityOfPrecipitation: Double,
    val weatherConditionId: Int,
    val weatherCondition: String,
    val weatherDescription: String,
    val weatherIconId: String,
    val windSpeedMps: Double,
    val windDirectionDegrees: Int,
    val humidityPercent: Int,
    val pressureHpa: Int,
    val cloudinessPercent: Int? // <--- ADDED: Cloudiness in percent
)

/**
 * Represents a single daily forecast item.
 */
data class DailyForecast(
    val dateTimeMillis: Long,         // Unix timestamp, UTC
    val sunriseMillis: Long,
    val sunsetMillis: Long,
    val tempMinCelsius: Double,
    val tempMaxCelsius: Double,
    val tempDayCelsius: Double,
    val tempNightCelsius: Double,
    val feelsLikeDayCelsius: Double,
    val feelsLikeNightCelsius: Double,
    val probabilityOfPrecipitation: Double, // 0.0 to 1.0
    val weatherConditionId: Int,
    val weatherCondition: String,
    val weatherDescription: String,
    val weatherIconId: String,
    val windSpeedMps: Double,
    val windDirectionDegrees: Int,
    val humidityPercent: Int,
    val pressureHpa: Int,
    val uvi: Double // UV index
)

/**
 * A bundle containing all relevant weather information for a location.
 * This can be what our UseCases primarily return to the ViewModel.
 */
data class WeatherDataBundle(
    val latitude: Double,
    val longitude: Double,
    val timezone: String, // e.g., "Europe/Kiev"
    val currentWeather: CurrentWeather,
    val hourlyForecasts: List<HourlyForecast>, // e.g., next 24-48 hours
    val dailyForecasts: List<DailyForecast>    // e.g., next 7 days
)