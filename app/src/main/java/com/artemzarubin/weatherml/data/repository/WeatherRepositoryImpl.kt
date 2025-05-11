package com.artemzarubin.weatherml.data.repository

import com.artemzarubin.weatherml.data.remote.ApiService
import com.artemzarubin.weatherml.data.remote.dto.CurrentWeatherResponseDto
import com.artemzarubin.weatherml.data.remote.dto.ForecastResponseDto
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject // Hilt annotation for constructor injection

// Hilt will know how to provide ApiService because we configured it in NetworkModule
class WeatherRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : WeatherRepository {

    override suspend fun getCurrentWeather(
        lat: Double,
        lon: Double,
        apiKey: String
    ): Resource<CurrentWeatherResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCurrentWeather(
                    latitude = lat,
                    longitude = lon,
                    apiKey = apiKey
                )
                Resource.Success(data = response)
            } catch (e: Exception) {
                // Log.e("WeatherRepositoryImpl", "Error fetching current weather: ${e.message}", e)
                Resource.Error(
                    message = e.message ?: "An unknown error occurred fetching current weather"
                )
            }
        }
    }

    // New implementation for fetching forecast data
    override suspend fun getForecast(
        lat: Double,
        lon: Double,
        apiKey: String
        // count: Int? = null // Add if you added it to the interface
    ): Resource<ForecastResponseDto> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getForecast( // Call the new method in ApiService
                    latitude = lat,
                    longitude = lon,
                    apiKey = apiKey
                    // units, lang, count will use default values from ApiService definition
                    // or pass 'count' here if you added it to the parameters
                )
                Resource.Success(data = response)
            } catch (e: Exception) {
                // Log.e("WeatherRepositoryImpl", "Error fetching forecast: ${e.message}", e)
                Resource.Error(message = e.message ?: "An unknown error occurred fetching forecast")
            }
        }
    }
}