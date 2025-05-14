package com.artemzarubin.weatherml.domain.repository

import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.data.remote.dto.GeocodingResponseItemDto
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.util.Resource

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
}