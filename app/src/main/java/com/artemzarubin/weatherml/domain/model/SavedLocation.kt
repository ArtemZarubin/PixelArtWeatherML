package com.artemzarubin.weatherml.domain.model

/**
 * Domain model representing a location saved by the user.
 */
data class SavedLocation(
    val id: Int = 0, // Will be 0 for a new location before insertion
    val cityName: String,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double,
    val isCurrentActive: Boolean = false,
    val orderIndex: Int = 0
)