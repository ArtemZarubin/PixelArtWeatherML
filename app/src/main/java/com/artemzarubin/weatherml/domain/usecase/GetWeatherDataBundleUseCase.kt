package com.artemzarubin.weatherml.domain.usecase

import com.artemzarubin.weatherml.BuildConfig
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle // Return Domain Model
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import javax.inject.Inject

class GetWeatherDataBundleUseCase @Inject constructor(
    private val repository: WeatherRepository
) {
    suspend operator fun invoke(
        lat: Double,
        lon: Double,
        units: String
    ): Resource<WeatherDataBundle> { // <--- ДОДАНО units
        return repository.getAllWeatherData(
            lat = lat,
            lon = lon,
            apiKey = BuildConfig.OPEN_WEATHER_API_KEY, // Або передавай ключ як параметр
            units = units // <--- ПЕРЕДАЄМО units
        )
    }
}