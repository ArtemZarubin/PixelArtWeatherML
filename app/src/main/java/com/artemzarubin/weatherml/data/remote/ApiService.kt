package com.artemzarubin.weatherml.data.remote

import com.artemzarubin.weatherml.data.remote.dto.OneCallResponseDto // Ensure this DTO is correctly defined and imported
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    /**
     * Fetches comprehensive weather data (current, minutely, hourly, daily) for a specific location
     * using the OpenWeatherMap One Call API 2.5.
     *
     * @param latitude Geographical coordinate, latitude.
     * @param longitude Geographical coordinate, longitude.
     * @param apiKey Your unique API key for OpenWeatherMap.
     * @param excludeParts Optional. Parts of the weather data to exclude from the API response.
     *                     Comma-delimited list (without spaces). Available values: current, minutely, hourly, daily, alerts.
     *                     Default is to exclude "minutely" and "alerts" for brevity.
     * @param units Optional. Units of measurement. "metric" for Celsius, "imperial" for Fahrenheit. Default is "metric".
     * @param language Optional. Language for the output of weather descriptions. Default is "en" (English).
     * @return [OneCallResponseDto] The DTO containing the parsed weather data.
     */
    @GET("onecall") // Endpoint for the One Call API 2.5
    suspend fun getWeatherOneCall(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("appid") apiKey: String, // This will be BuildConfig.OPEN_WEATHER_API_KEY
        @Query("exclude") excludeParts: String? = "minutely,alerts", // Default to exclude minutely and alerts
        @Query("units") units: String = "metric",
        @Query("lang") language: String = "en" // Default to English
    ): OneCallResponseDto
}