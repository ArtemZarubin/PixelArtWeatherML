// File: com/artemzarubin/weatherml/data/remote/dto/AirPollutionResponseDto.kt
package com.artemzarubin.weatherml.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the entire response from the Air Pollution API.
 * It contains coordinates and a list of pollution data points (usually one).
 */
@Serializable
data class AirPollutionResponseDto(
    @SerialName("coord") val coordinates: AirPollutionCoordinatesDto? = null, // Optional coordinates
    @SerialName("list") val list: List<AirPollutionDataEntryDto>? = null // List of pollution data entries
)

/**
 * Coordinates provided in the Air Pollution API response.
 * Note: This might be redundant if you already have a similar DTO.
 * If so, you can reuse your existing CoordinatesDto.
 */
@Serializable
data class AirPollutionCoordinatesDto(
    @SerialName("lon") val longitude: Double?,
    @SerialName("lat") val latitude: Double?
)

/**
 * Represents a single data entry in the Air Pollution API response list.
 * Contains the main AQI and component concentrations.
 */
@Serializable
data class AirPollutionDataEntryDto( // Renamed from AirPollutionDataDto for clarity
    @SerialName("main") val mainAqiComponents: MainAqiDataDto?, // Renamed for clarity
    @SerialName("components") val pollutantComponents: PollutantComponentsDto?, // Renamed for clarity
    @SerialName("dt") val dateTime: Long? // Unix timestamp, UTC
)

/**
 * Main Air Quality Index (AQI) data.
 * The AQI value from OpenWeatherMap is a scale from 1 (Good) to 5 (Very Poor).
 */
@Serializable
data class MainAqiDataDto( // Renamed from MainAqiDto
    @SerialName("aqi") val aqi: Int? // Air Quality Index. Value 1, 2, 3, 4, 5.
    // 1=Good, 2=Fair, 3=Moderate, 4=Poor, 5=Very Poor.
)

/**
 * Concentrations of different air pollutants.
 * Units are μg/m3 (micrograms per cubic meter).
 */
@Serializable
data class PollutantComponentsDto( // Renamed from ComponentsDto
    @SerialName("co") val co: Double?,         // Carbon monoxide
    @SerialName("no") val no: Double? = null,  // Nitrogen monoxide
    @SerialName("no2") val no2: Double?,       // Nitrogen dioxide
    @SerialName("o3") val o3: Double?,         // Ozone
    @SerialName("so2") val so2: Double? = null,// Sulphur dioxide
    @SerialName("pm2_5") val pm25: Double?,    // PM2.5 (Fine particles matter)
    @SerialName("pm10") val pm10: Double? = null,// PM10 (Coarse particulate matter)
    @SerialName("nh3") val nh3: Double? = null // Ammonia
)