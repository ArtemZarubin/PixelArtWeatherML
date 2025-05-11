package com.artemzarubin.weatherml.data.repository

// DTO imports are still needed here as ApiService returns DTOs
import com.artemzarubin.weatherml.data.mapper.mapToWeatherDataBundle
import com.artemzarubin.weatherml.data.remote.ApiService
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import javax.inject.Inject

// import android.util.Log

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: ApiService
) : WeatherRepository {

    // We are removing the old getCurrentWeather that returned DTOs,
    // or you can keep it if it's used elsewhere, but the interface now has getAllWeatherData.

    override suspend fun getAllWeatherData(
        lat: Double,
        lon: Double,
        apiKey: String
    ): Resource<WeatherDataBundle> {
        // Perform network calls on the IO dispatcher
        return withContext(Dispatchers.IO) {
            try {
                // Launch both API calls concurrently using async for better performance
                val currentWeatherDeferred = async {
                    apiService.getCurrentWeather(
                        latitude = lat,
                        longitude = lon,
                        apiKey = apiKey
                        // Default units and lang from ApiService
                    )
                }
                val forecastDeferred = async {
                    apiService.getForecast(
                        latitude = lat,
                        longitude = lon,
                        apiKey = apiKey,
                        count = 40 // Request full 5-day forecast (8 * 5 = 40 intervals)
                        // We will then take what we need in the mapper or ViewModel
                    )
                }

                // Await for both calls to complete
                val currentWeatherResponseDto = currentWeatherDeferred.await()
                val forecastResponseDto = forecastDeferred.await()

                // Map DTOs to Domain Model WeatherDataBundle
                val weatherDataBundle = mapToWeatherDataBundle(
                    currentWeatherDto = currentWeatherResponseDto,
                    forecastResponseDto = forecastResponseDto,
                    lat = lat, // Pass lat and lon if you want them in WeatherDataBundle
                    lon = lon
                )
                Resource.Success(data = weatherDataBundle)

            } catch (e: Exception) {
                // Log.e("WeatherRepositoryImpl", "Error fetching all weather data: ${e.message}", e)
                Resource.Error(
                    message = e.message ?: "An unknown error occurred fetching weather data"
                )
            }
        }
    }
}