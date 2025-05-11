package com.artemzarubin.weatherml.domain.repository

// We will define Domain Models later, for now, let's assume ApiService returns DTOs
// and the repository will handle mapping or pass DTOs further up if needed initially.
// For a cleaner architecture, repository should return Domain Models.
import com.artemzarubin.weatherml.data.remote.dto.OneCallResponseDto // Temporary: Using DTO directly
import com.artemzarubin.weatherml.util.Resource // We will create this utility class later for handling results

interface WeatherRepository {

    /**
     * Fetches weather data for the given latitude and longitude.
     * This function will interact with the ApiService to get data from the network.
     *
     * @param lat Latitude.
     * @param lon Longitude.
     * @param apiKey The API key for accessing the weather service.
     * @return A Resource wrapper containing either the OneCallResponseDto on success or an error message.
     *         Ideally, this should return a Domain Model, not a DTO.
     */
    suspend fun getWeather(
        lat: Double,
        lon: Double,
        apiKey: String
    ): Resource<OneCallResponseDto> // TODO: Replace OneCallResponseDto with a Domain Model later
}