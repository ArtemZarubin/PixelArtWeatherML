package com.artemzarubin.weatherml.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the response from Geoapify Reverse Geocoding API.
 * It's a FeatureCollection, typically containing one primary feature for the given coordinates.
 */
@Serializable
data class GeoapifyReverseGeocodeResponseDto(
    @SerialName("type") val type: String? = null,
    @SerialName("features") val features: List<GeoapifyFeatureDto>? = null
)