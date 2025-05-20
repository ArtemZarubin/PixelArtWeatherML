package com.artemzarubin.weatherml.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val cityName: String,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double,
    val isCurrentLocation: Boolean = false,
    val orderIndex: Int = 0 // For sorting
)