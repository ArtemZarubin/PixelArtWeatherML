package com.artemzarubin.weatherml.data.repository

import com.artemzarubin.weatherml.data.remote.ApiService
import com.artemzarubin.weatherml.data.remote.dto.OneCallResponseDto // Using DTO directly for now
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject // Hilt annotation for constructor injection

// Hilt will know how to provide ApiService because we configured it in NetworkModule
class WeatherRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : WeatherRepository {

    override suspend fun getWeather(
        lat: Double,
        lon: Double,
        apiKey: String
    ): Resource<OneCallResponseDto> {
        // Perform the network call on the IO dispatcher for background execution
        return withContext(Dispatchers.IO) {
            try {
                // Call the suspend function from our ApiService
                val response = apiService.getWeatherOneCall(
                    latitude = lat,
                    longitude = lon,
                    apiKey = apiKey,
                    // excludeParts, units, language will use default values from ApiService definition
                    // or you can pass them as parameters to getWeather if you want more control here
                )
                // If the call is successful, wrap the response in Resource.Success
                Resource.Success(data = response)
            } catch (e: Exception) {
                // If an exception occurs (e.g., network error, parsing error),
                // wrap the error message in Resource.Error
                // It's good practice to log the exception here as well
                // Log.e("WeatherRepositoryImpl", "Error fetching weather data", e) // Example logging
                Resource.Error(message = e.message ?: "An unknown error occurred")
            }
        }
    }
}