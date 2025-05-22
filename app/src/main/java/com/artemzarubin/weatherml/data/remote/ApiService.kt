package com.artemzarubin.weatherml.data.remote

import com.artemzarubin.weatherml.data.remote.dto.CurrentWeatherResponseDto
import com.artemzarubin.weatherml.data.remote.dto.ForecastResponseDto
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyAutocompleteResponseDto
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyReverseGeocodeResponseDto
import com.artemzarubin.weatherml.data.remote.dto.GeocodingResponseItemDto
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    /**
     * Fetches current weather data for a specific location.
     * Uses the /weather endpoint of OpenWeatherMap API 2.5.
     *
     * @param latitude Geographical coordinate, latitude.
     * @param longitude Geographical coordinate, longitude.
     * @param apiKey Your unique API key for OpenWeatherMap.
     * @param units Optional. Units of measurement. "metric" for Celsius, "imperial" for Fahrenheit. Default is "metric".
     * @param language Optional. Language for the output of weather descriptions. Default is "en" (English).
     * @return [CurrentWeatherResponseDto] The DTO containing the parsed current weather data.
     */

    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") language: String = "en"
    ): CurrentWeatherResponseDto

    /**
     * Fetches 5 day / 3 hour forecast data for a specific location.
     * Uses the /forecast endpoint of OpenWeatherMap API 2.5.
     *
     * @param latitude Geographical coordinate, latitude.
     * @param longitude Geographical coordinate, longitude.
     * @param apiKey Your unique API key for OpenWeatherMap.
     * @param units Optional. Units of measurement. "metric" for Celsius, "imperial" for Fahrenheit. Default is "metric".
     * @param language Optional. Language for the output of weather descriptions. Default is "en" (English).
     * @param count Optional. A number of timestamps, which will be returned in the API response (up to 40 for 3-hour forecast).
     * @return [ForecastResponseDto] The DTO containing the parsed forecast data.
     */
    @GET("forecast") // Endpoint for 5 day / 3 hour forecast
    suspend fun getForecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") language: String = "en",
        @Query("cnt") count: Int? = null // Optional: number of timestamps to return (max 40)
    ): ForecastResponseDto

    /**
     * Fetches geographic coordinates for a given location name.
     * Uses the OpenWeatherMap Geocoding API.
     *
     * @param cityName The name of the city (and optionally state code, country code).
     * @param limit The maximum number of locations in the API response (default 1, max 5).
     * @param apiKey Your unique API key.
     * @return A list of [GeocodingResponseItemDto] containing location data.
     */
    @GET("https://api.openweathermap.org/geo/1.0/direct") // FULL URL instead of /geo/...
    suspend fun getCoordinatesByCityName(
        @Query("q") cityName: String,
        @Query("limit") limit: Int = 5, // Request up to 5
        @Query("appid") apiKey: String
    ): List<GeocodingResponseItemDto> // API returns a JSON array

    /**
     * Fetches place autocomplete suggestions from Geoapify API.
     *
     * @param text The input text to search for.
     * @param apiKey Your unique API key for Geoapify.
     * @param limit The maximum number of suggestions to return.
     * @param lang Language of the results.
     * @param filter Filter to search only for cities (example: filter=countrycode:us&type=city).
     *               For global city search, might be just type=city or no filter if results are good.
     * @return [GeoapifyAutocompleteResponseDto] containing a list of features.
     */
    @GET("https://api.geoapify.com/v1/geocode/autocomplete") // Full URL for Geoapify
    suspend fun getCityAutocomplete(
        @Query("type") type: String = "city",
        @Query("text") text: String,
        @Query("apiKey") apiKey: String,
        @Query("limit") limit: Int = 10, // Request a few suggestions
        @Query("lang") lang: String = "en",
        // @Query("type") type: String = "city" // Filter by city type
        // @Query("filter") filter: String? = null // More advanced filtering if needed
    ): GeoapifyAutocompleteResponseDto // New DTO for Geoapify

    @GET("https://api.geoapify.com/v1/geocode/reverse") // Повний URL, оскільки BASE_URL інший
    suspend fun reverseGeocode(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("type") type: String = "city", // Опціонально, для отримання результатів на рівні міста
        @Query("lang") language: String = "en", // Мова результатів
        @Query("apiKey") apiKey: String
    ): GeoapifyReverseGeocodeResponseDto // Новий DTO для відповіді
}