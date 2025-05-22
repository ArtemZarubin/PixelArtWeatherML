package com.artemzarubin.weatherml.domain.repository

import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.data.remote.dto.GeocodingResponseItemDto
import com.artemzarubin.weatherml.domain.model.SavedLocation
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.util.Resource
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {

    /**
     * Fetches all relevant weather data (current, hourly forecast, daily forecast)
     * for the given coordinates and returns it as a [WeatherDataBundle].
     *
     * @param lat Latitude.
     * @param lon Longitude.
     * @param apiKey The API key.
     * @return A [Resource] wrapping the [WeatherDataBundle] on success, or an error.
     */
    suspend fun getAllWeatherData(
        lat: Double,
        lon: Double,
        apiKey: String // API key is needed for both calls
    ): Resource<WeatherDataBundle>

    /**
     * Fetches geographic coordinates for a given city name.
     *
     * @param cityName The name of the city to search for.
     * @param apiKey The API key for the geocoding service.
     * @param limit The maximum number of results to return.
     * @return A Resource wrapper containing a list of [GeocodingResponseItemDto] on success, or an error.
     *         Ideally, this should also return Domain Models for locations.
     */
    /**
     * Fetches city autocomplete suggestions from Geoapify.
     */
    suspend fun getCityAutocompleteSuggestions(
        query: String,
        apiKey: String,
        limit: Int = 7
        // type: String = "city"
    ): Resource<List<GeoapifyFeatureDto>> // Returns a list of features

    /**
     * Retrieves all saved locations from the local database, ordered by user preference.
     * @return A Flow emitting a list of [SavedLocation] domain models.
     */
    fun getSavedLocations(): Flow<List<SavedLocation>>

    /**
     * Retrieves the currently active location for which weather should be displayed.
     * @return A Flow emitting the current [SavedLocation] or null if none is active.
     */
    fun getCurrentActiveWeatherLocation(): Flow<SavedLocation?>

    /**
     * Adds a new location to the saved list.
     * It will also check if a location with the same coordinates already exists.
     * If it's the first location added, it might be set as current.
     * @param location The [SavedLocation] domain model to add.
     * @return The ID of the newly inserted location, or -1L if insertion failed or location already exists.
     */
    suspend fun addSavedLocation(location: SavedLocation): Long

    /**
     * Sets a given location as the current active one.
     * @param locationId The ID of the location to set as active.
     */
    suspend fun setActiveLocation(locationId: Int)

    /**
     * Deletes a location from the saved list.
     * @param locationId The ID of the location to delete.
     */
    suspend fun deleteSavedLocation(locationId: Int)

    /**
     * Updates the order of saved locations.
     * @param locations The list of [SavedLocation] domain models with updated orderIndex.
     */
    suspend fun updateSavedLocationsOrder(locations: List<SavedLocation>)

    /**
     * Checks if any locations are saved.
     */
    suspend fun hasSavedLocations(): Boolean

    /**
     * Gets a specific saved location by its ID.
     */
    suspend fun getSavedLocationById(locationId: Int): SavedLocation?

    /**
     * Fetches address/location details (like city name, country) for given coordinates.
     * @param lat Latitude.
     * @param lon Longitude.
     * @param apiKey The API key for the reverse geocoding service.
     * @return A Resource wrapper containing the primary [GeoapifyFeatureDto] on success, or an error.
     */
    suspend fun getLocationDetailsByCoordinates(
        lat: Double,
        lon: Double,
        apiKey: String
    ): Resource<GeoapifyFeatureDto?>
}