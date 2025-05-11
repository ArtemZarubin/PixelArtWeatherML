package com.artemzarubin.weatherml.data.remote

import com.artemzarubin.weatherml.data.remote.dto.CurrentWeatherResponseDto
import com.artemzarubin.weatherml.data.remote.dto.ForecastResponseDto
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
}