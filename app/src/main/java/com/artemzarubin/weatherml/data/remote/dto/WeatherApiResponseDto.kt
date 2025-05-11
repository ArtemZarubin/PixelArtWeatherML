package com.artemzarubin.weatherml.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Main response object for One Call API 2.5
@Serializable
data class OneCallResponseDto(
    @SerialName("lat") val lat: Double?,
    @SerialName("lon") val lon: Double?,
    @SerialName("timezone") val timezone: String?,
    @SerialName("timezone_offset") val timezoneOffset: Int?, // In seconds
    @SerialName("current") val current: CurrentWeatherDto?,
    @SerialName("minutely") val minutely: List<MinutelyWeatherDto>? = null, // Optional, minute forecast for 1 hour
    @SerialName("hourly") val hourly: List<HourlyWeatherDto>?, // Hourly forecast for 48 hours
    @SerialName("daily") val daily: List<DailyWeatherDto>?,   // Daily forecast for 7 days
    @SerialName("alerts") val alerts: List<AlertDto>? = null  // Optional, weather alerts
)

@Serializable
data class WeatherConditionDto(
    @SerialName("id") val id: Int?, // Weather condition id
    @SerialName("main") val main: String?, // Group of weather parameters (Rain, Snow, Clouds, etc.)
    @SerialName("description") val description: String?, // Weather condition within the group
    @SerialName("icon") val icon: String? // Weather icon id
)

@Serializable
data class CurrentWeatherDto(
    @SerialName("dt") val dateTime: Long?, // Current time, Unix, UTC
    @SerialName("sunrise") val sunrise: Long?, // Sunrise time, Unix, UTC
    @SerialName("sunset") val sunset: Long?,   // Sunset time, Unix, UTC
    @SerialName("temp") val temperature: Double?, // Temperature
    @SerialName("feels_like") val feelsLike: Double?, // Feels like temperature
    @SerialName("pressure") val pressure: Int?, // Atmospheric pressure on the sea level, hPa
    @SerialName("humidity") val humidity: Int?, // Humidity, %
    @SerialName("dew_point") val dewPoint: Double?, // Atmospheric temperature below which water droplets begin to condense
    @SerialName("uvi") val uvi: Double?, // UV index
    @SerialName("clouds") val clouds: Int?, // Cloudiness, %
    @SerialName("visibility") val visibility: Int?, // Average visibility, metres
    @SerialName("wind_speed") val windSpeed: Double?, // Wind speed
    @SerialName("wind_deg") val windDeg: Int?, // Wind direction, degrees (meteorological)
    @SerialName("wind_gust") val windGust: Double? = null, // Optional: Wind gust
    @SerialName("weather") val weatherConditions: List<WeatherConditionDto>?
    // Optional: Rain information
    // @SerialName("rain") val rain: RainDto? = null,
    // Optional: Snow information
    // @SerialName("snow") val snow: SnowDto? = null
)

@Serializable
data class MinutelyWeatherDto( // Minute forecast for 1 hour
    @SerialName("dt") val dateTime: Long?, // Time of the forecasted data, Unix, UTC
    @SerialName("precipitation") val precipitation: Double? // Precipitation volume, mm
)

@Serializable
data class HourlyWeatherDto( // Hourly forecast for 48 hours
    @SerialName("dt") val dateTime: Long?,
    @SerialName("temp") val temperature: Double?,
    @SerialName("feels_like") val feelsLike: Double?,
    @SerialName("pressure") val pressure: Int?,
    @SerialName("humidity") val humidity: Int?,
    @SerialName("dew_point") val dewPoint: Double?,
    @SerialName("uvi") val uvi: Double?,
    @SerialName("clouds") val clouds: Int?,
    @SerialName("visibility") val visibility: Int?,
    @SerialName("wind_speed") val windSpeed: Double?,
    @SerialName("wind_deg") val windDeg: Int?,
    @SerialName("wind_gust") val windGust: Double? = null,
    @SerialName("weather") val weatherConditions: List<WeatherConditionDto>?,
    @SerialName("pop") val probabilityOfPrecipitation: Double? // Probability of precipitation. The values of the parameter vary between 0 and 1, where 0 is equal to 0%, 1 is equal to 100%
)

@Serializable
data class DailyTemperatureDto(
    @SerialName("day") val day: Double?, // Day temperature
    @SerialName("min") val min: Double?, // Min daily temperature
    @SerialName("max") val max: Double?, // Max daily temperature
    @SerialName("night") val night: Double?, // Night temperature
    @SerialName("eve") val evening: Double?, // Evening temperature
    @SerialName("morn") val morning: Double?  // Morning temperature
)

@Serializable
data class DailyFeelsLikeDto(
    @SerialName("day") val day: Double?,
    @SerialName("night") val night: Double?,
    @SerialName("eve") val evening: Double?,
    @SerialName("morn") val morning: Double?
)

@Serializable
data class DailyWeatherDto( // Daily forecast for 7 days
    @SerialName("dt") val dateTime: Long?,
    @SerialName("sunrise") val sunrise: Long?,
    @SerialName("sunset") val sunset: Long?,
    @SerialName("moonrise") val moonrise: Long?,
    @SerialName("moonset") val moonset: Long?,
    @SerialName("moon_phase") val moonPhase: Double?,
    @SerialName("summary") val summary: String? = null, // Optional: human-readable textual summary of the weather conditions
    @SerialName("temp") val temperature: DailyTemperatureDto?,
    @SerialName("feels_like") val feelsLike: DailyFeelsLikeDto?,
    @SerialName("pressure") val pressure: Int?,
    @SerialName("humidity") val humidity: Int?,
    @SerialName("dew_point") val dewPoint: Double?,
    @SerialName("wind_speed") val windSpeed: Double?,
    @SerialName("wind_deg") val windDeg: Int?,
    @SerialName("wind_gust") val windGust: Double? = null,
    @SerialName("weather") val weatherConditions: List<WeatherConditionDto>?,
    @SerialName("clouds") val clouds: Int?,
    @SerialName("pop") val probabilityOfPrecipitation: Double?,
    @SerialName("uvi") val uvi: Double?,
    @SerialName("rain") val rainVolume: Double? = null, // Optional: Precipitation volume, mm
    @SerialName("snow") val snowVolume: Double? = null  // Optional: Snow volume, mm
)

@Serializable
data class AlertDto( // Optional: Weather alerts
    @SerialName("sender_name") val senderName: String?, // Name of the alert source
    @SerialName("event") val event: String?, // Alert event name
    @SerialName("start") val start: Long?, // Date and time of the start of the alert, Unix, UTC
    @SerialName("end") val end: Long?,     // Date and time of the end of the alert, Unix, UTC
    @SerialName("description") val description: String?, // Description of the alert
    @SerialName("tags") val tags: List<String>? = null // Type of severe weather
)

// Optional: If rain/snow have more complex structure like {"1h": 0.5}
// @Serializable
// data class RainDto(
//    @SerialName("1h") val oneHour: Double? // Rain volume for the last 1 hour, mm
// )
// @Serializable
// data class SnowDto(
//    @SerialName("1h") val oneHour: Double? // Snow volume for the last 1 hour, mm
// )