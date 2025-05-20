package com.artemzarubin.weatherml.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedLocationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocationEntity): Long

    @Update
    suspend fun updateLocation(location: SavedLocationEntity)

    @Delete
    suspend fun deleteLocation(location: SavedLocationEntity)

    @Query("DELETE FROM saved_locations WHERE id = :locationId")
    suspend fun deleteLocationById(locationId: Int)

    @Query("SELECT * FROM saved_locations WHERE id = :locationId")
    suspend fun getLocationById(locationId: Int): SavedLocationEntity?

    @Query("SELECT * FROM saved_locations WHERE isCurrentLocation = 1 LIMIT 1")
    fun getCurrentActiveLocation(): Flow<SavedLocationEntity?>

    @Query("SELECT * FROM saved_locations ORDER BY orderIndex ASC, cityName ASC") // Added sorting by name as a secondary option
    fun getAllSavedLocations(): Flow<List<SavedLocationEntity>>

    // Sets isCurrentLocation = true for the specified ID, and false for all others
    @Query("UPDATE saved_locations SET isCurrentLocation = CASE WHEN id = :locationId THEN 1 ELSE 0 END")
    suspend fun setCurrentActiveLocation(locationId: Int)

    // Update the order for the location list (used for drag-and-drop)
    @Update(entity = SavedLocationEntity::class) // Specify the Entity for partial update
    suspend fun updateLocationOrder(locations: List<SavedLocationEntity>) // Room will update only those that have changed

    @Query("SELECT COUNT(*) FROM saved_locations")
    suspend fun getLocationsCount(): Int

    // Method for getting location by coordinates (to avoid duplicates)
    @Query("SELECT * FROM saved_locations WHERE latitude = :latitude AND longitude = :longitude LIMIT 1")
    suspend fun getLocationByCoordinates(latitude: Double, longitude: Double): SavedLocationEntity?
}