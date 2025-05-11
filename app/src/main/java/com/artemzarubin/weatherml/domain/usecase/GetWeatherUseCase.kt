package com.artemzarubin.weatherml.domain.usecase

import com.artemzarubin.weatherml.BuildConfig // To access the API key
import com.artemzarubin.weatherml.data.remote.dto.OneCallResponseDto // Using DTO for now
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import javax.inject.Inject

/**
 * Use case for fetching weather data for a specific location.
 * This class encapsulates the business logic for this particular operation.
 */
class GetWeatherUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository // Hilt will provide the WeatherRepositoryImpl instance
) {

    /**
     * Executes the use case to get weather data.
     *
     * @param lat Latitude of the location.
     * @param lon Longitude of the location.
     * @return A [Resource] wrapping the [OneCallResponseDto] or an error.
     *         Ideally, this should return a Domain Model mapped from the DTO.
     */
    suspend operator fun invoke(lat: Double, lon: Double): Resource<OneCallResponseDto> {
        // The API key is accessed from BuildConfig, which is a secure way to store it.
        val apiKey = BuildConfig.OPEN_WEATHER_API_KEY

        // Basic validation (can be expanded)
        if (apiKey.isBlank()) {
            return Resource.Error("API key is missing. Please check your configuration.")
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return Resource.Error("Invalid geographical coordinates provided.")
        }

        return weatherRepository.getWeather(lat = lat, lon = lon, apiKey = apiKey)
    }
}