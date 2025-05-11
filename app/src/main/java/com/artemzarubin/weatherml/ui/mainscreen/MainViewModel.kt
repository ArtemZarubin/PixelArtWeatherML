package com.artemzarubin.weatherml.ui.mainscreen

import android.util.Log // For logging, if you keep the logs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemzarubin.weatherml.BuildConfig
import com.artemzarubin.weatherml.data.remote.dto.CurrentWeatherResponseDto
import com.artemzarubin.weatherml.domain.usecase.GetWeatherUseCase
import com.artemzarubin.weatherml.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase
) : ViewModel() {
    private val _currentWeatherState =
        MutableStateFlow<Resource<CurrentWeatherResponseDto>>(Resource.Loading()) // Type StateFlow
    val currentWeatherState: StateFlow<Resource<CurrentWeatherResponseDto>> =
        _currentWeatherState.asStateFlow() // Type StateFlow

    /**
     * Fetches weather data for the given latitude and longitude.
     * Updates the weatherState StateFlow with the result.
     */
    fun fetchCurrentWeather(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            // Set state to Loading before making the network call
            _currentWeatherState.value = Resource.Loading()
            Log.d("MainViewModel", "Fetching weather for lat: $latitude, lon: $longitude")

            // The API key is accessed from BuildConfig
            val apiKey = BuildConfig.OPEN_WEATHER_API_KEY
            if (apiKey.isBlank()) {
                Log.e("MainViewModel", "API Key is blank!")
                _currentWeatherState.value = Resource.Error("API key is missing.")
                return@launch
            }

            // Call the use case
            val result = getWeatherUseCase(
                lat = latitude,
                lon = longitude
            ) // apiKey is handled by use case now

            // Update the state with the result
            _currentWeatherState.value = result

            // Log the result for debugging
            when (result) {
                is Resource.Success -> Log.d(
                    "MainViewModel",
                    "Weather data fetched successfully: ${result.data}"
                )

                is Resource.Error -> Log.e(
                    "MainViewModel",
                    "Error fetching weather data: ${result.message}"
                )

                is Resource.Loading -> { /* This case should ideally not be emitted by the use case directly after a call */
                }
            }
        }
    }

    // Example: Call this function when the ViewModel is created to load initial data
    // Or, it can be called from the UI when a specific event occurs (e.g., button click, location update)
    // For now, we will call it from the UI (MainActivity/WeatherScreen) for testing.
    // init {
    //     // Example: Zaporizhzhia coordinates
    //     // fetchWeatherData(47.8388, 35.1396)
    // }
}