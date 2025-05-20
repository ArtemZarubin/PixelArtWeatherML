package com.artemzarubin.weatherml.ui.mainscreen

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.artemzarubin.weatherml.BuildConfig
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.domain.location.LocationTracker
import com.artemzarubin.weatherml.domain.model.SavedLocation
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationTracker: LocationTracker,
    private val application: Application
) : ViewModel() {

    private val _weatherDataState =
        MutableStateFlow<Resource<WeatherDataBundle>>(Resource.Loading(message = "Initializing..."))
    val weatherDataState: StateFlow<Resource<WeatherDataBundle>> = _weatherDataState.asStateFlow()

    private val _autocompleteResults =
        MutableStateFlow<Resource<List<GeoapifyFeatureDto>>>(Resource.Success(emptyList()))
    val autocompleteResults: StateFlow<Resource<List<GeoapifyFeatureDto>>> =
        _autocompleteResults.asStateFlow()

    private val _savedLocations = MutableStateFlow<List<SavedLocation>>(emptyList())
    val savedLocations: StateFlow<List<SavedLocation>> = _savedLocations.asStateFlow()

    private val _activeWeatherLocation = MutableStateFlow<SavedLocation?>(null)
    val activeWeatherLocation: StateFlow<SavedLocation?> = _activeWeatherLocation.asStateFlow()

    private var fetchWeatherJob: Job? = null
    private var autocompleteJob: Job? = null
    private var initialSetupDone = false // Прапорець для уникнення повторної ініціалізації

    init {
        Log.d("MainViewModel", "ViewModel initialized. Launching initial setup.")
        // Спочатку підписуємося на активну локацію, щоб реагувати на її зміни
        observeCurrentActiveLocationAndFetchWeather()
        // Потім підписуємося на список збережених, який може ініціювати встановлення активної
        observeSavedLocations()
    }

    private fun observeSavedLocations() {
        weatherRepository.getSavedLocations()
            .onEach { locations ->
                _savedLocations.value = locations
                Log.d("MainViewModel", "Saved locations updated: ${locations.size} items.")

                // Логіка встановлення початкової активної локації або запиту геолокації
                // виконується тільки один раз при старті ViewModel
                if (!initialSetupDone) {
                    initialSetupDone = true
                    val currentActiveFromDb = locations.find { it.isCurrentActive }
                    if (currentActiveFromDb != null) {
                        Log.d(
                            "MainViewModel",
                            "Initial: Found active location in DB: ${currentActiveFromDb.cityName}"
                        )
                        // _activeWeatherLocation.value = currentActiveFromDb // Це викличе observeCurrentActiveLocation...
                        // Краще напряму викликати setActiveLocation, щоб оновити і в БД, і Flow
                        viewModelScope.launch {
                            weatherRepository.setActiveLocation(
                                currentActiveFromDb.id
                            )
                        }
                    } else if (locations.isNotEmpty()) {
                        Log.d(
                            "MainViewModel",
                            "Initial: No active in DB, setting first saved as active: ${locations.first().cityName}"
                        )
                        viewModelScope.launch { weatherRepository.setActiveLocation(locations.first().id) }
                    } else {
                        Log.d(
                            "MainViewModel",
                            "Initial: No saved locations. Initiating geolocation fetch."
                        )
                        initiateWeatherFetch(isInitialAttempt = true)
                    }
                }
            }
            .catch { e -> Log.e("MainViewModel", "Error observing saved locations", e) }
            .launchIn(viewModelScope)
    }

    private fun observeCurrentActiveLocationAndFetchWeather() {
        weatherRepository.getCurrentActiveWeatherLocation() // Цей Flow має емітити нове значення, коли setActiveLocation викликається
            .distinctUntilChanged() // Важливо, щоб не було зайвих запитів
            .onEach { activeLocation ->
                Log.d("MainViewModel", "Active location Flow emitted: ${activeLocation?.cityName}")
                _activeWeatherLocation.value = activeLocation // Оновлюємо наш StateFlow
                if (activeLocation != null) {
                    // Перевіряємо, чи не завантажуємо вже для цієї локації
                    val isLoadingForThis = (_weatherDataState.value is Resource.Loading &&
                            (_weatherDataState.value as Resource.Loading).message?.contains(
                                activeLocation.cityName
                            ) == true)
                    val isAlreadySuccessForThis = (_weatherDataState.value is Resource.Success &&
                            (_weatherDataState.value as Resource.Success<WeatherDataBundle>).data?.currentWeather?.cityName == activeLocation.cityName &&
                            (_weatherDataState.value as Resource.Success<WeatherDataBundle>).data?.latitude == activeLocation.latitude &&
                            (_weatherDataState.value as Resource.Success<WeatherDataBundle>).data?.longitude == activeLocation.longitude)


                    if (!isLoadingForThis && !isAlreadySuccessForThis) {
                        Log.i(
                            "MainViewModel",
                            "Active location changed to: ${activeLocation.cityName}. Fetching weather."
                        )
                        fetchWeatherData(
                            activeLocation.latitude,
                            activeLocation.longitude,
                            activeLocation.cityName
                        )
                    } else {
                        Log.d(
                            "MainViewModel",
                            "Weather for ${activeLocation.cityName} is already loading or loaded."
                        )
                    }
                }
                // Якщо activeLocation == null і initialSetupDone == true і _savedLocations порожній,
                // це означає, що всі локації видалено. Можна спробувати геолокацію.
                else if (initialSetupDone && _savedLocations.value.isEmpty()) {
                    Log.d(
                        "MainViewModel",
                        "Active location is null, no saved locations. Initiating geolocation."
                    )
                    initiateWeatherFetch(isInitialAttempt = false) // isInitialAttempt = false, щоб не створювати нову локацію, якщо геолокація не вдасться
                }
            }
            .catch { e ->
                Log.e("MainViewModel", "Error observing active location from repository", e)
                _weatherDataState.value =
                    Resource.Error("Could not load active location preference.")
            }
            .launchIn(viewModelScope)
    }

    fun initiateWeatherFetch(isInitialAttempt: Boolean = false) {
        if (_activeWeatherLocation.value != null && _savedLocations.value.any { it.id == _activeWeatherLocation.value!!.id && it.isCurrentActive } && weatherDataState.value is Resource.Success) {
            Log.d(
                "MainViewModel",
                "initiateWeatherFetch: Active location already set and weather loaded. Skipping redundant geolocation fetch unless forced."
            )
            // Якщо потрібна кнопка "оновити геолокацію", вона має викликати це з іншим прапорцем
            return
        }

        fetchWeatherJob?.cancel()
        fetchWeatherJob = viewModelScope.launch {
            if (!hasLocationPermission()) {
                if (_activeWeatherLocation.value == null) {
                    _weatherDataState.value = Resource.Error("Location permission needed.")
                }
                return@launch
            }
            // Встановлюємо Loading тільки якщо ми дійсно починаємо новий процес отримання геолокації
            // і ще немає активної локації, для якої вже може йти завантаження
            if (_activeWeatherLocation.value == null) {
                _weatherDataState.value = Resource.Loading(message = "Fetching current location...")
            }

            try {
                val locationData: Location? = locationTracker.getCurrentLocation()
                if (locationData != null) {
                    Log.d(
                        "MainViewModel",
                        "Geolocation received: Lat=${locationData.latitude}, Lon=${locationData.longitude}"
                    )
                    if (isInitialAttempt && _savedLocations.value.isEmpty()) {
                        val newLocation = SavedLocation(
                            cityName = "Current Location (GPS)", // TODO: Reverse geocode
                            countryCode = null,
                            latitude = locationData.latitude,
                            longitude = locationData.longitude,
                            isCurrentActive = false, // Додамо як неактивну
                            orderIndex = 0
                        )
                        // Додаємо і одразу робимо активною, що викличе fetchWeatherData через observeCurrentActiveLocation
                        addLocationAndSetAsActive(newLocation)
                    } else if (_activeWeatherLocation.value == null) {
                        // Якщо немає активної, але є збережені, просто показуємо погоду для геолокації
                        // НЕ зберігаючи її і НЕ роблячи активною.
                        // Це для випадку, коли користувач просто хоче подивитися погоду "тут і зараз"
                        // не змінюючи свою активну збережену локацію.
                        // Або, якщо це isInitialAttempt=false (наприклад, після видалення всіх),
                        // то ми теж просто завантажуємо, а не зберігаємо.
                        fetchWeatherData(
                            locationData.latitude,
                            locationData.longitude,
                            "Current Geolocation"
                        )
                    }
                } else {
                    if (_activeWeatherLocation.value == null) {
                        _weatherDataState.value =
                            Resource.Error("Could not get current location (null from tracker).")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Error in initiateWeatherFetch's locationTracker", e)
                if (_activeWeatherLocation.value == null) {
                    _weatherDataState.value =
                        Resource.Error("Could not get current location: ${e.message}")
                }
            }
        }
    }

    // Новий метод для чистоти
    private fun addLocationAndSetAsActive(location: SavedLocation) {
        viewModelScope.launch {
            val newId =
                weatherRepository.addSavedLocation(location) // Додаємо з isCurrentActive = false
            if (newId > 0L) {
                weatherRepository.setActiveLocation(newId.toInt())
            } else if (newId == -2L) { // Вже існує
                val existing =
                    _savedLocations.value.find { it.latitude == location.latitude && it.longitude == location.longitude }
                existing?.let { weatherRepository.setActiveLocation(it.id) }
            }
        }
    }

    private fun fetchWeatherData(
        lat: Double,
        lon: Double,
        locationNameHint: String = "Selected Location"
    ) {
        viewModelScope.launch {
            _weatherDataState.value =
                Resource.Loading(message = "Fetching weather for $locationNameHint...")
            val result = weatherRepository.getAllWeatherData(
                lat = lat,
                lon = lon,
                apiKey = BuildConfig.OPEN_WEATHER_API_KEY
            )
            _weatherDataState.value = result
            if (result is Resource.Success) {
                Log.i(
                    "MainViewModel",
                    "Weather data fetched for $locationNameHint: ${result.data?.currentWeather?.cityName}"
                )
            } else if (result is Resource.Error) {
                Log.e(
                    "MainViewModel",
                    "Error fetching weather for $locationNameHint: ${result.message}"
                )
            }
        }
    }

    @OptIn(FlowPreview::class)
    fun searchCityAutocomplete(query: String) {
        autocompleteJob?.cancel()
        if (query.length < 3) {
            _autocompleteResults.value = Resource.Success(emptyList()); return
        }

        viewModelScope.launch {
            kotlinx.coroutines.flow.flowOf(query) // Використовуємо flowOf для одного значення
                .debounce(300L)
                .distinctUntilChanged()
                .flatMapLatest { debouncedQuery ->
                    Log.d("MainViewModel", "Debounced search for: $debouncedQuery")
                    // weatherRepository.getCityAutocompleteSuggestions повертає Resource, який є Flow-like, але не Flow.
                    // Якщо він повертає Resource напряму, то flatMapLatest не потрібен, або потрібен інший підхід.
                    // Припустимо, getCityAutocompleteSuggestions - це suspend функція, що повертає Resource
                    try {
                        val resourceResult = weatherRepository.getCityAutocompleteSuggestions(
                            query = debouncedQuery,
                            apiKey = BuildConfig.GEOAPIFY_API_KEY
                        )
                        if (resourceResult is Resource.Success && resourceResult.data.isNullOrEmpty() && debouncedQuery.isNotBlank()) {
                            kotlinx.coroutines.flow.flowOf(Resource.Error("No cities found for \"$debouncedQuery\""))
                        } else {
                            kotlinx.coroutines.flow.flowOf(resourceResult)
                        }
                    } catch (e: Exception) {
                        kotlinx.coroutines.flow.flowOf(Resource.Error("Autocomplete search failed: ${e.message}"))
                    }
                }
                .catch { e ->
                    _autocompleteResults.value =
                        Resource.Error("Autocomplete search failed: ${e.message}")
                } // emit не потрібен тут
                .collectLatest { results -> _autocompleteResults.value = results }
        }
    }

    fun clearGeocodingResults() {
        _autocompleteResults.value = Resource.Success(emptyList())
        autocompleteJob?.cancel()
    }

    fun onCitySuggestionSelected(cityGeoData: GeoapifyFeatureDto) {
        val properties = cityGeoData.properties
        val lat = properties?.latitude
        val lon = properties?.longitude

        // Формуємо більш повну назву для збереження
        val fullCityName = listOfNotNull(
            properties?.city,
            properties?.state,
            properties?.county
        ).distinct().joinToString(", ")
            .ifBlank { properties?.formattedAddress ?: "Unknown Location" }

        if (lat != null && lon != null) {
            val newLocation = SavedLocation(
                cityName = fullCityName.trim(), // Зберігаємо повну назву
                countryCode = properties.countryCode?.uppercase(),
                latitude = lat,
                longitude = lon,
                isCurrentActive = false,
                orderIndex = _savedLocations.value.size
            )
            addLocationAndSetAsActive(newLocation)
        } else {
            Log.e(
                "MainViewModel",
                "Selected city suggestion has no coordinates or essential properties: $cityGeoData"
            )
        }
    }

    fun addLocation(location: SavedLocation) {
        viewModelScope.launch {
            weatherRepository.addSavedLocation(location)
        }
    }

    fun deleteLocation(locationId: Int) {
        viewModelScope.launch {
            val locationToDelete = _savedLocations.value.find { it.id == locationId }
            weatherRepository.deleteSavedLocation(locationId)
            Log.d("MainViewModel", "Location with ID $locationId deleted.")

            if (locationToDelete?.isCurrentActive == true) {
                val remainingLocations = _savedLocations.value.filterNot { it.id == locationId }
                if (remainingLocations.isNotEmpty()) {
                    Log.d(
                        "MainViewModel",
                        "Deleted active location. Setting ${remainingLocations.first().cityName} as new active."
                    )
                    weatherRepository.setActiveLocation(remainingLocations.first().id)
                } else {
                    Log.d(
                        "MainViewModel",
                        "No saved locations left after deletion. Clearing active and attempting geolocation."
                    )
                    _activeWeatherLocation.value =
                        null // Це має викликати initiateWeatherFetch через observeCurrentActiveLocation...
                }
            }
        }
    }

    fun selectActiveLocation(locationId: Int) {
        viewModelScope.launch {
            weatherRepository.setActiveLocation(locationId)
        }
    }

    fun updateLocationsOrder(locations: List<SavedLocation>) {
        viewModelScope.launch {
            weatherRepository.updateSavedLocationsOrder(locations)
            // _savedLocations оновиться автоматично через Flow
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun setPermissionError(message: String) {
        // Встановлюємо помилку тільки якщо зараз не успішне завантаження для активної локації
        if (_activeWeatherLocation.value == null || _weatherDataState.value !is Resource.Success) {
            _weatherDataState.value = Resource.Error(message = message)
        }
        Log.e("MainViewModel", "Permission error set by UI: $message")
    }
}