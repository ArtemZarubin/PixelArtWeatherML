package com.artemzarubin.weatherml.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a single location item returned by the Geocoding API.
 * The API returns a List of these items.
 */
@Serializable
data class GeocodingResponseItemDto(
    @SerialName("name") val name: String?, // Name of the found location
    @SerialName("local_names") val localNames: Map<String, String>? = null, // Map of local names (optional)
    @SerialName("lat") val latitude: Double?, // Geographical coordinates of the found location (latitude)
    @SerialName("lon") val longitude: Double?, // Geographical coordinates of the found location (longitude)
    @SerialName("country") val country: String?, // Country code (GB, JP etc.)
    @SerialName("state") val state: String? = null // State of the found location (optional)
)