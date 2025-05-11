package com.artemzarubin.weatherml.domain.repository

// For a cleaner architecture, repository should return Domain Models.
import com.artemzarubin.weatherml.data.remote.dto.CurrentWeatherResponseDto
import com.artemzarubin.weatherml.data.remote.dto.ForecastResponseDto
import com.artemzarubin.weatherml.util.Resource

interface WeatherRepository {

    /**
     * Fetches weather data for the given latitude and longitude.
     * This function will interact with the ApiService to get data from the network.
     *
     * @param lat Latitude.
     * @param lon Longitude.
     * @param apiKey The API key for accessing the weather service.
     * @return A Resource wrapper containing either the CurrentWeatherResponseDto on success or an error message.
     *         Ideally, this should return a Domain Model, not a DTO.
     */
    suspend fun getCurrentWeather(
        lat: Double,
        lon: Double,
        apiKey: String
    ): Resource<CurrentWeatherResponseDto>

    /**
     * Fetches 5 day / 3 hour forecast data for the given latitude and longitude.
     *
     * @param lat Latitude.
     * @param lon Longitude.
     * @param apiKey The API key for accessing the weather service.
     * @return A Resource wrapper containing either the ForecastResponseDto on success or an error message.
     *         Ideally, this should return Domain Models mapped from DTOs.
     */
    suspend fun getForecast( // New method for forecast
        lat: Double,
        lon: Double,
        apiKey: String
        // count: Int? = null
    ): Resource<ForecastResponseDto> // Return type is now ForecastResponseDto
}