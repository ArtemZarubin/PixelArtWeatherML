package com.artemzarubin.weatherml.ui.mainscreen

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// BuildConfig is not needed here if the UseCase itself handles the key
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle // Use the Domain Model
import com.artemzarubin.weatherml.domain.usecase.GetWeatherDataBundleUseCase // New UseCase
import com.artemzarubin.weatherml.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getWeatherDataBundleUseCase: GetWeatherDataBundleUseCase // Inject a new UseCase
) : ViewModel() {

    // One StateFlow for all weather data
    private val _weatherDataState =
        MutableStateFlow<Resource<WeatherDataBundle>>(Resource.Loading())
    val weatherDataState: StateFlow<Resource<WeatherDataBundle>> = _weatherDataState.asStateFlow()

    /**
     * Fetches all weather data (current and forecast) for the given latitude and longitude.
     */
    fun fetchAllWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _weatherDataState.value = Resource.Loading()
            Log.d("MainViewModel", "Fetching all weather data for lat: $latitude, lon: $longitude")

            val result = getWeatherDataBundleUseCase(lat = latitude, lon = longitude)
            _weatherDataState.value = result

            when (result) {
                is Resource.Success -> Log.d(
                    "MainViewModel",
                    "All weather data fetched. City: ${result.data?.currentWeather?.cityName}, Hourly items: ${result.data?.hourlyForecasts?.size}"
                )

                is Resource.Error -> Log.e(
                    "MainViewModel",
                    "Error fetching all weather data: ${result.message}"
                )

                is Resource.Loading -> {}
            }
        }
    }
}