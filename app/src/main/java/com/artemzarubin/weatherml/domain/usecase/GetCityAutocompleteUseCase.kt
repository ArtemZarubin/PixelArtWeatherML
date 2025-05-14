package com.artemzarubin.weatherml.domain.usecase

import com.artemzarubin.weatherml.BuildConfig
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto // Use Geoapify DTO
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import javax.inject.Inject

// import android.util.Log

class GetCityAutocompleteUseCase @Inject constructor(
    private val weatherRepository: WeatherRepository
) {
    suspend operator fun invoke(query: String, limit: Int = 7): Resource<List<GeoapifyFeatureDto>> {
        val apiKey = BuildConfig.GEOAPIFY_API_KEY // Use Geoapify API key
        // Log.d("GetCityAutocompleteUseCase", "Fetching suggestions for: $query with key: $apiKey")

        if (apiKey.isBlank()) {
            return Resource.Error("Geoapify API key is missing.")
        }
        if (query.isBlank()) {
            return Resource.Success(emptyList()) // Return empty success if query is blank
        }
        // Consider adding type="city" directly here or ensure it's passed if needed
        return weatherRepository.getCityAutocompleteSuggestions(
            query = query,
            apiKey = apiKey,
            limit = limit/*, type = "city"*/
        )
    }
}