package com.artemzarubin.weatherml.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Main response object for /forecast endpoint
@Serializable
data class ForecastResponseDto(
    @SerialName("cod") val cod: String?, // Internal parameter, e.g., "200"
    @SerialName("message") val message: Int?, // Internal parameter
    @SerialName("cnt") val count: Int?, // Number of forecast items returned
    @SerialName("list") val list: List<ForecastItemDto>?, // List of forecast items
    @SerialName("city") val city: CityDto? // Information about the city
)

@Serializable
data class ForecastItemDto(
    @SerialName("dt") val dateTime: Long?, // Time of data forecasted, unix, UTC
    @SerialName("main") val main: MainWeatherInfoDto?, // Reusing MainWeatherInfoDto from CurrentWeatherResponseDto.kt
    // Sometimes fields like temp_kf might be present here.
    @SerialName("weather") val weather: List<WeatherConditionDto>?, // Reusing WeatherConditionDto
    @SerialName("clouds") val clouds: CloudsDto? = null, // Reusing CloudsDto
    @SerialName("wind") val wind: WindDto? = null, // Reusing WindDto
    @SerialName("visibility") val visibility: Int? = null, // Average visibility, metres
    @SerialName("pop") val probabilityOfPrecipitation: Double? = null, // Probability of precipitation (0 to 1)
    @SerialName("sys") val sys: ForecastSysDto? = null, // Contains part of day (d or n)
    @SerialName("dt_txt") val dateTimeText: String?, // Data/time of calculation, ISO, UTC e.g., "2020-05-06 18:00:00"

    // Optional: Rain information for the last 3 hours (if present in API)
    // The structure for "rain" and "snow" in forecast can be {"3h": value}
    @SerialName("rain") val rain: RainSnowVolumeDto? = null,
    // Optional: Snow information for the last 3 hours (if present in API)
    @SerialName("snow") val snow: RainSnowVolumeDto? = null
)

// Reusable DTOs (if not already defined elsewhere or if structure is identical)

// If MainWeatherInfoDto from CurrentWeatherResponseDto.kt is suitable, use it.
// Otherwise, define a specific one if fields like "temp_kf" are present in forecast's "main" object.
// For example, if it's identical:
// typealias MainWeatherInfoDto = com.artemzarubin.weatherml.data.remote.dto.MainWeatherInfoDto (if in another file)

// If WeatherConditionDto from CurrentWeatherResponseDto.kt is suitable, use it.
// typealias WeatherConditionDto = com.artemzarubin.weatherml.data.remote.dto.WeatherConditionDto

// If CloudsDto from CurrentWeatherResponseDto.kt is suitable, use it.
// typealias CloudsDto = com.artemzarubin.weatherml.data.remote.dto.CloudsDto

// If WindDto from CurrentWeatherResponseDto.kt is suitable, use it.
// typealias WindDto = com.artemzarubin.weatherml.data.remote.dto.WindDto

@Serializable
data class ForecastSysDto( // System information within a forecast item
    @SerialName("pod") val partOfDay: String? // Part of the day (d = day, n = night)
)

@Serializable
data class CityDto(
    @SerialName("id") val id: Int?,
    @SerialName("name") val name: String?,
    @SerialName("coord") val coordinates: CoordinatesDto? = null, // <--- МАЄ БУТИ NULLABLE
    @SerialName("country") val country: String?,
    @SerialName("population") val population: Int?,
    @SerialName("timezone") val timezone: Int?,
    @SerialName("sunrise") val sunrise: Long?,
    @SerialName("sunset") val sunset: Long?
)

// Reusing CoordinatesDto from CurrentWeatherResponseDto.kt if suitable.
// typealias CoordinatesDto = com.artemzarubin.weatherml.data.remote.dto.CoordinatesDto

// DTO for rain/snow volume, typically for "3h" period in forecast
@Serializable
data class RainSnowVolumeDto(
    @SerialName("3h") val threeHourVolume: Double?
)