package com.artemzarubin.weatherml.ui.mainscreen

import android.location.Location
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.domain.location.LocationTracker
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.domain.usecase.GetCityAutocompleteUseCase
import com.artemzarubin.weatherml.domain.usecase.GetWeatherDataBundleUseCase
import com.artemzarubin.weatherml.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getWeatherDataBundleUseCase: GetWeatherDataBundleUseCase,
    private val locationTracker: LocationTracker,
    private val getCityAutocompleteUseCase: GetCityAutocompleteUseCase // Inject new UseCase
) : ViewModel() {

    // State for Weather Data (current, hourly, daily)
    private val _weatherDataState =
        MutableStateFlow<Resource<WeatherDataBundle>>(Resource.Loading())
    val weatherDataState: StateFlow<Resource<WeatherDataBundle>> = _weatherDataState.asStateFlow()

    // State for Autocomplete Search Results
    private val _autocompleteResults =
        MutableStateFlow<Resource<List<GeoapifyFeatureDto>>>(Resource.Success(emptyList())) // Initial empty success
    val autocompleteResults: StateFlow<Resource<List<GeoapifyFeatureDto>>> =
        _autocompleteResults.asStateFlow()

    /**
     * Initiates the process of fetching weather data.
     * It first tries to get the current location, and if successful, fetches weather for it.
     * If location cannot be obtained (e.g., services off, or an error), it sets an error state.
     * This function should be called when location permissions are confirmed.
     */
    fun initiateWeatherFetch() {
        Log.d("MainViewModel", "initiateWeatherFetch called.")
        viewModelScope.launch {
            _weatherDataState.value = Resource.Loading(message = "Fetching location...")
            val location: Location? =
                locationTracker.getCurrentLocation() // Getting the real location

            if (location != null) {
                Log.d(
                    "MainViewModel",
                    "Location fetched: Lat=${location.latitude}, Lon=${location.longitude}"
                )
                // USING REAL COORDINATES
                fetchAllWeatherData(latitude = location.latitude, longitude = location.longitude)
            } else {
                Log.e("MainViewModel", "Failed to get current location for weather fetch.")
                // Check if a previous successful data exists, if not, show error.
                // This prevents overwriting successful data with a location error if weather was already loaded for a default.
                if (_weatherDataState.value !is Resource.Success) {
                    _weatherDataState.value = Resource.Error(
                        "Unable to retrieve current location. Please ensure location services are enabled."
                    )
                } else {
                    Log.w(
                        "MainViewModel",
                        "Failed to get new location, keeping existing weather data."
                    )
                }
            }
        }
    }

    /**
     * Fetches all weather data (current and forecast) for the given latitude and longitude.
     * This is a more direct function if coordinates are already known.
     */
    private fun fetchAllWeatherData(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            // Set loading state only if not already loading (e.g. from initiateWeatherFetch)
            if (_weatherDataState.value !is Resource.Loading || (_weatherDataState.value as? Resource.Loading)?.data == null) {
                _weatherDataState.value = Resource.Loading(message = "Fetching weather data...")
            }
            Log.d("MainViewModel", "Fetching all weather data for lat: $latitude, lon: $longitude")

            val result = getWeatherDataBundleUseCase(lat = latitude, lon = longitude)
            _weatherDataState.value = result

            when (result) {
                is Resource.Success -> Log.d(
                    "MainViewModel",
                    "All weather data fetched. City: ${result.data?.currentWeather?.cityName}"
                )

                is Resource.Error -> Log.e(
                    "MainViewModel",
                    "Error fetching all weather data: ${result.message}"
                )

                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Sets an error state, typically used when permissions are denied.
     */
    fun setPermissionError(message: String) {
        _weatherDataState.value = Resource.Error(message)
        Log.e("MainViewModel", "Permission error set: $message")
    }

    /**
     * Searches for city autocomplete suggestions and updates the autocompleteResults StateFlow.
     */
    fun searchCityAutocomplete(query: String) {
        if (query.length < 3) { // Start search only if query is reasonably long
            _autocompleteResults.value =
                Resource.Success(emptyList()) // Clear results for short queries
            return
        }
        viewModelScope.launch {
            // Don't show full screen loading, maybe a subtle indicator in UI if needed
            // _autocompleteResults.value = Resource.Loading()
            Log.d("MainViewModel", "Searching autocomplete for: $query")
            val result = getCityAutocompleteUseCase(query = query)
            _autocompleteResults.value = result

            when (result) {
                is Resource.Success -> Log.d(
                    "MainViewModel",
                    "Autocomplete success: ${result.data?.size} suggestions found."
                )

                is Resource.Error -> Log.e("MainViewModel", "Autocomplete error: ${result.message}")
                is Resource.Loading -> {}
            }
        }
    }

    /**
     * Called when a city suggestion is selected from the autocomplete results.
     * Fetches weather data for the selected city's coordinates.
     */
    fun onCitySuggestionSelected(selectedFeature: GeoapifyFeatureDto) {
        // Geoapify returns coordinates as [longitude, latitude] in geometry
        // or directly as lat, lon in properties. Prioritize properties if available.
        val lat = selectedFeature.properties?.latitude
        val lon = selectedFeature.properties?.longitude
        // Fallback to geometry if properties lat/lon are null
        // val lonFromGeom = selectedFeature.geometry?.coordinates?.getOrNull(0)
        // val latFromGeom = selectedFeature.geometry?.coordinates?.getOrNull(1)

        if (lat != null && lon != null) {
            Log.d(
                "MainViewModel",
                "City suggestion selected: ${selectedFeature.properties.formattedAddress}. Fetching weather..."
            )
            fetchAllWeatherData(latitude = lat, longitude = lon)
            _autocompleteResults.value = Resource.Success(emptyList()) // Clear
        } else {
            Log.e("MainViewModel", "Selected suggestion has invalid coordinates: $selectedFeature")
            // Optionally, set an error state for autocomplete results
            // _autocompleteResults.value = Resource.Error("Selected location has invalid coordinates.")
        }
    }

    fun clearGeocodingResults() { // Renamed to clearAutocompleteResults
        _autocompleteResults.value = Resource.Success(emptyList())
    }
}