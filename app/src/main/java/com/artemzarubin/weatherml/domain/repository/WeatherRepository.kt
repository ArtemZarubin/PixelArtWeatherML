package com.artemzarubin.weatherml.domain.repository

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
}