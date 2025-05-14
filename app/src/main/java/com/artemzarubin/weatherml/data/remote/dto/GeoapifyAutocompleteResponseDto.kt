package com.artemzarubin.weatherml.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeoapifyAutocompleteResponseDto(
    // The API returns a GeoJSON FeatureCollection, but we are interested in the features array.
    // Depending on the exact structure, you might need a wrapper class if "features" is nested.
    // Assuming for now the top level is a list of features or a wrapper containing features.
    // If the API returns {"type": "FeatureCollection", "features": [...]}, you need a wrapper:
    // val type: String?,
    // val features: List<GeoapifyFeatureDto>?
    // For simplicity, let's assume the endpoint can directly return List<GeoapifyFeatureDto>
    // or that Retrofit can handle extracting the list.
    // If not, you might need a custom deserializer or a wrapper class.
    // Let's assume a simpler structure for now or that the actual response is a list of features.
    // UPDATE: Geoapify returns a FeatureCollection object.
    @SerialName("features") val features: List<GeoapifyFeatureDto>?
)

@Serializable
data class GeoapifyFeatureDto(
    @SerialName("properties") val properties: GeoapifyPropertiesDto?,
    @SerialName("geometry") val geometry: GeoapifyGeometryDto? // For coordinates if not in properties
)

@Serializable
data class GeoapifyPropertiesDto(
    @SerialName("country") val country: String?,
    @SerialName("country_code") val countryCode: String?,
    @SerialName("state") val state: String? = null,
    @SerialName("county") val county: String? = null,
    @SerialName("city") val city: String? = null,
    @SerialName("postcode") val postcode: String? = null,
    @SerialName("suburb") val suburb: String? = null,
    @SerialName("street") val street: String? = null,
    @SerialName("housenumber") val housenumber: String? = null,
    @SerialName("lat") val latitude: Double?,
    @SerialName("lon") val longitude: Double?,
    @SerialName("formatted") val formattedAddress: String?, // Full formatted address
    @SerialName("address_line1") val addressLine1: String?,
    @SerialName("address_line2") val addressLine2: String?,
    @SerialName("place_id") val placeId: String? // Unique ID for the place
)

@Serializable
data class GeoapifyGeometryDto(
    @SerialName("type") val type: String?, // e.g., "Point"
    @SerialName("coordinates") val coordinates: List<Double>? // [longitude, latitude]
)