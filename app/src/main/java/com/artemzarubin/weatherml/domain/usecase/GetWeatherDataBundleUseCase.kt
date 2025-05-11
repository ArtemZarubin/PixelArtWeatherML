package com.artemzarubin.weatherml.domain.usecase

import com.artemzarubin.weatherml.BuildConfig
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle // Return Domain Model
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import javax.inject.Inject

class GetWeatherDataBundleUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(lat: Double, lon: Double): Resource<WeatherDataBundle> {
        val apiKey = BuildConfig.OPEN_WEATHER_API_KEY

        if (apiKey.isBlank()) {
            return Resource.Error("API key is missing.")
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            return Resource.Error("Invalid geographical coordinates provided.")
        }
        // Return method
        return weatherRepository.getAllWeatherData(lat = lat, lon = lon, apiKey = apiKey)
    }
}