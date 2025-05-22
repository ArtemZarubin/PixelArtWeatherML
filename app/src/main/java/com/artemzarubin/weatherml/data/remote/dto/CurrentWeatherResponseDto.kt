// File: com/artemzarubin/weatherml/data/remote/dto/CurrentWeatherResponseDto.kt
package com.artemzarubin.weatherml.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// This DTO is for the response from the /weather endpoint
@Serializable
data class CurrentWeatherResponseDto(
    @SerialName("coord") val coordinates: CoordinatesDto?, // Added coordinates
    @SerialName("weather") val weather: List<WeatherConditionDto>?,
    @SerialName("base") val base: String?, // Internal parameter
    @SerialName("main") val main: MainWeatherInfoDto?,
    @SerialName("visibility") val visibility: Int?, // Visibility, meter
    @SerialName("wind") val wind: WindDto?,
    @SerialName("clouds") val clouds: CloudsDto?, // Added clouds
    @SerialName("rain") val rain: RainDataDto? = null, // Optional rain data
    @SerialName("snow") val snow: SnowDataDto? = null, // Optional snow data
    @SerialName("dt") val dateTime: Long?, // Time of data calculation, unix, UTC
    @SerialName("sys") val sys: SysDto?, // Added system data (country, sunrise, sunset)
    @SerialName("timezone") val timezone: Int?, // Shift in seconds from UTC
    @SerialName("id") val cityId: Int?, // City ID
    @SerialName("name") val cityName: String?,
    @SerialName("cod") val cod: Int? // Internal parameter
)

// Common DTO, can be used by both current weather and forecast
@Serializable
data class WeatherConditionDto(
    @SerialName("id") val id: Int?,
    @SerialName("main") val main: String?,
    @SerialName("description") val description: String?,
    @SerialName("icon") val icon: String?
)

// DTO for the "main" object in /weather response
@Serializable
data class MainWeatherInfoDto(
    @SerialName("temp") val temp: Double?,
    @SerialName("feels_like") val feelsLike: Double?,
    @SerialName("temp_min") val tempMin: Double?,
    @SerialName("temp_max") val tempMax: Double?,
    @SerialName("pressure") val pressure: Int?,
    @SerialName("humidity") val humidity: Int?,
    @SerialName("sea_level") val seaLevelPressure: Int? = null, // Optional
    @SerialName("grnd_level") val groundLevelPressure: Int? = null // Optional
)

// DTO for the "wind" object
@Serializable
data class WindDto(
    @SerialName("speed") val speed: Double?,
    @SerialName("deg") val deg: Int?,
    @SerialName("gust") val gust: Double? = null // Optional
)

// DTO for the "clouds" object
@Serializable
data class CloudsDto(
    @SerialName("all") val all: Int? // Cloudiness, %
)

// DTO for the "coord" object
@Serializable
data class CoordinatesDto(
    @SerialName("lon") val longitude: Double?, // <--- МАЄ БУТИ NULLABLE
    @SerialName("lat") val latitude: Double?
)

// DTO for the "sys" object in /weather response
@Serializable
data class SysDto( // Це частина CurrentWeatherResponseDto
    @SerialName("type") val type: Int? = null, // Залишаємо nullable, якщо вони можуть бути відсутні
    @SerialName("id") val id: Int? = null,   // Залишаємо nullable
    @SerialName("country") val country: String? = null, // <--- ЗРОБЛЕНО NULLABLE
    @SerialName("sunrise") val sunrise: Long?, // Залишаємо nullable
    @SerialName("sunset") val sunset: Long?   // Залишаємо nullable
)

// Optional DTOs for rain and snow (structure might vary, check API for "1h" or "3h" fields)
@Serializable
data class RainDataDto(
    @SerialName("1h") val oneHour: Double? = null, // Rain volume for the last 1 hour
    @SerialName("3h") val threeHour: Double? = null // Rain volume for the last 3 hours
)

@Serializable
data class SnowDataDto(
    @SerialName("1h") val oneHour: Double? = null, // Snow volume for the last 1 hour
    @SerialName("3h") val threeHour: Double? = null // Snow volume for the last 3 hours
)