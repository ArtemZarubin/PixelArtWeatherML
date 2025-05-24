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
import com.artemzarubin.weatherml.data.preferences.AppTheme
import com.artemzarubin.weatherml.data.preferences.TemperatureUnit
import com.artemzarubin.weatherml.data.preferences.UserPreferences
import com.artemzarubin.weatherml.data.preferences.UserPreferencesRepository
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.domain.location.LocationTracker
import com.artemzarubin.weatherml.domain.model.SavedLocation
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// PagerItem визначено тут (без змін)
sealed class PagerItem {
    abstract val id: String
    abstract val displayName: String
    abstract val latitude: Double
    abstract val longitude: Double
    abstract val countryCode: String?

    data class GeolocationPage(
        var lat: Double = 0.0,
        var lon: Double = 0.0,
        var fetchedCityName: String = "My Location",
        var fetchedCountryCode: String? = null,
        var isLoadingDetails: Boolean = true
    ) : PagerItem() {
        override val id: String = "geolocation_page_id"
        override val displayName: String get() = fetchedCityName
        override val latitude: Double get() = lat
        override val longitude: Double get() = lon
        override val countryCode: String? get() = fetchedCountryCode
    }

    data class SavedPage(val location: SavedLocation) : PagerItem() {
        override val id: String = "saved_${location.id}"
        override val displayName: String get() = location.cityName
        override val latitude: Double get() = location.latitude
        override val longitude: Double get() = location.longitude
        override val countryCode: String? get() = location.countryCode
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
open class MainViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationTracker: LocationTracker,
    private val application: Application, // <--- ТРЕТІЙ АРГУМЕНТ
    private val userPreferencesRepository: UserPreferencesRepository // <--- ЧЕТВЕРТИЙ АРГУМЕНТ (якщо ти його додав)
) : ViewModel() {

    companion object {
        const val MAX_SAVED_LOCATIONS = 10 // Максимальна кількість ЗБЕРЕЖЕНИХ локацій
    }

    private val _weatherDataStateMap =
        MutableStateFlow<Map<String, Resource<WeatherDataBundle>>>(emptyMap())
    val weatherDataStateMap: StateFlow<Map<String, Resource<WeatherDataBundle>>> =
        _weatherDataStateMap.asStateFlow()

    private val _autocompleteResults =
        MutableStateFlow<Resource<List<GeoapifyFeatureDto>>>(Resource.Success(emptyList()))
    val autocompleteResults: StateFlow<Resource<List<GeoapifyFeatureDto>>> =
        _autocompleteResults.asStateFlow()

    private val _savedLocationsFromDbFlow =
        weatherRepository.getSavedLocations().distinctUntilChanged()
    private val _geolocationPagerItemState =
        MutableStateFlow(PagerItem.GeolocationPage(isLoadingDetails = false)) // isLoadingDetails = false спочатку

    private val _currentPagerIndex = MutableStateFlow(0)
    val currentPagerIndex: StateFlow<Int> = _currentPagerIndex.asStateFlow()

    private val _permissionCheckTrigger =
        MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    val pagerItems: StateFlow<List<PagerItem>> =
        combine(
            _savedLocationsFromDbFlow,
            _geolocationPagerItemState,
            _permissionCheckTrigger.onStart { emit(Unit) } // Пытаемся эмитить при старте, чтобы combine сработал с актуальным разрешением
                .flatMapLatest { // Каждый раз, когда триггер срабатывает
                    Log.d(
                        "MainViewModel",
                        "PermissionCheckTrigger fired. Re-evaluating hasLocationPermission()."
                    )
                    flowOf(hasLocationPermission()) // Получаем актуальное состояние разрешения
                }
                .distinctUntilChanged()
        ) { savedDbList, geoPageState, permissionGranted ->
            val list = mutableListOf<PagerItem>()
            // Твои детальные логи здесь остаются
            Log.d(
                "MainViewModel",
                "pagerItems combining... PermissionGranted: $permissionGranted, GeoPageCity: ${geoPageState.fetchedCityName}, GeoPageLoading: ${geoPageState.isLoadingDetails}, GeoPageLat: ${geoPageState.lat}, GeoPageLon: ${geoPageState.lon}, SavedDBList size: ${savedDbList.size}"
            )

            if (permissionGranted) {
                list.add(geoPageState)
                Log.d(
                    "MainViewModel",
                    "pagerItems: Added GeolocationPage to list. Current geoPageState: $geoPageState"
                )
            } else {
                Log.d(
                    "MainViewModel",
                    "pagerItems: Permission NOT granted. GeolocationPage NOT added."
                )
            }
            list.addAll(savedDbList.map { PagerItem.SavedPage(it) })
            Log.d(
                "MainViewModel",
                "pagerItems combined. Final list size before distinct: ${list.size}. List content: $list"
            )
            val distinctList = list.distinctBy { it.id }
            if (distinctList.size != list.size) {
                Log.w(
                    "MainViewModel",
                    "pagerItems: distinctBy removed duplicates. Original size: ${list.size}, Distinct size: ${distinctList.size}"
                )
            }
            distinctList
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentActivePagerItem: StateFlow<PagerItem?> =
        combine(pagerItems, _currentPagerIndex) { items, index ->
            items.getOrNull(index)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val canAddNewLocation: StateFlow<Boolean> =
        _savedLocationsFromDbFlow // Використовуємо оригінальний Flow з БД
            .map { it.size < MAX_SAVED_LOCATIONS }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private var fetchWeatherJobs: MutableMap<String, Job> = mutableMapOf()
    private var autocompleteJob: Job? = null
    private var initialSetupFlowCompleted = false // Для керування початковим налаштуванням

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val userPreferencesFlow: StateFlow<UserPreferences> =
        userPreferencesRepository.userPreferencesFlow
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = UserPreferences(
                    TemperatureUnit.CELSIUS,
                    AppTheme.SYSTEM
                ) // <--- ОНОВЛЕНО initialValue
            )

    init {
        Log.d("MainViewModel", "ViewModel initialized.")
        observeActivePagerItemToFetchWeather() // Запускаем наблюдателя за погодой

        viewModelScope.launch {
            pagerItems.first { items ->
                val permissionGranted = hasLocationPermission()
                val geolocationPageIsCurrentlyLoadingDetails =
                    _geolocationPagerItemState.value.isLoadingDetails

                if (permissionGranted) {
                    val hasGeolocationPage = items.any { it is PagerItem.GeolocationPage }
                    val hasSavedPage = items.any { it is PagerItem.SavedPage }

                    if (hasGeolocationPage || hasSavedPage) {
                        true
                    } else {
                        !geolocationPageIsCurrentlyLoadingDetails
                    }
                } else {
                    items.any { it is PagerItem.SavedPage } || items.isEmpty()
                }
            }.let { initialItems ->
                if (!initialSetupFlowCompleted) {
                    initialSetupFlowCompleted = true
                    Log.d(
                        "MainViewModel",
                        "Initial pagerItems collected (size: ${initialItems.size}), determining initial page."
                    )
                    determineInitialPage(initialItems)
                } else {
                    Log.d(
                        "MainViewModel",
                        "Initial setup already completed. Skipping determineInitialPage for subsequent pagerItems emission."
                    )
                }
            }
        }
    }

    private suspend fun determineInitialPage(currentPagerItems: List<PagerItem>) {
        Log.d(
            "MainViewModel",
            "determineInitialPage called with ${currentPagerItems.size} items. HasPerm: ${hasLocationPermission()}"
        )
        var targetIndex = -1
        var newActiveLocationIdToSetInDb: Int? = null

        if (hasLocationPermission()) {
            val geoPageIndex = currentPagerItems.indexOfFirst { it is PagerItem.GeolocationPage }
            if (geoPageIndex != -1) {
                targetIndex = geoPageIndex
                newActiveLocationIdToSetInDb = 0
                Log.i(
                    "MainViewModel",
                    "Initial: Geolocation is available. Setting as initial page (Index: $targetIndex). DB active ID will be 0."
                )

                val geoPage = currentPagerItems[targetIndex] as PagerItem.GeolocationPage
                if (geoPage.isLoadingDetails || (geoPage.lat == 0.0 && geoPage.lon == 0.0 && geoPage.fetchedCityName == "My Location")) {
                    Log.d(
                        "MainViewModel",
                        "Initial: Geolocation page ($geoPage) needs details or is loading. Fetching..."
                    )
                    fetchDetailsForGeolocationPage()
                }
            } else {
                Log.w(
                    "MainViewModel",
                    "Initial: Has permission, but GeolocationPage not found in currentPagerItems. Attempting to fetch details anyway."
                )
                if (!_geolocationPagerItemState.value.isLoadingDetails) {
                    fetchDetailsForGeolocationPage()
                }
            }
        }

        if (targetIndex == -1) {
            val activeSavedInDb =
                (_savedLocationsFromDbFlow.firstOrNull() ?: emptyList()).find { it.isCurrentActive }
            if (activeSavedInDb != null) {
                val activeSavedIndexInPager =
                    currentPagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.id == activeSavedInDb.id }
                if (activeSavedIndexInPager != -1) {
                    targetIndex = activeSavedIndexInPager
                    newActiveLocationIdToSetInDb = activeSavedInDb.id
                    Log.i(
                        "MainViewModel",
                        "Initial: No geolocation. Found active saved in DB: ${activeSavedInDb.cityName} (Index: $targetIndex). DB active ID will be ${activeSavedInDb.id}."
                    )
                } else {
                    Log.w(
                        "MainViewModel",
                        "Initial: Active saved in DB (${activeSavedInDb.cityName}) not found in current pager items. Will try first saved if any."
                    )
                }
            }

            if (targetIndex == -1) {
                val firstSavedPageIndex =
                    currentPagerItems.indexOfFirst { it is PagerItem.SavedPage }
                if (firstSavedPageIndex != -1) {
                    targetIndex = firstSavedPageIndex
                    val firstSavedPageItem =
                        currentPagerItems[firstSavedPageIndex] as PagerItem.SavedPage
                    newActiveLocationIdToSetInDb = firstSavedPageItem.location.id
                    Log.i(
                        "MainViewModel",
                        "Initial: No geo/active. Using first saved page: ${firstSavedPageItem.displayName} (Index: $targetIndex). DB active ID will be ${firstSavedPageItem.location.id}."
                    )
                }
            }
        }

        if (targetIndex != -1) {
            if (_currentPagerIndex.value != targetIndex) {
                _currentPagerIndex.value = targetIndex
                Log.d("MainViewModel", "Initial: _currentPagerIndex set to $targetIndex.")
            } else {
                Log.d(
                    "MainViewModel",
                    "Initial: _currentPagerIndex already $targetIndex. No change."
                )
            }

            newActiveLocationIdToSetInDb?.let { activeId ->
                val currentActiveInDb = (_savedLocationsFromDbFlow.firstOrNull()
                    ?: emptyList()).find { it.isCurrentActive }
                val currentDbActiveId = currentActiveInDb?.id ?: 0
                if (activeId == 0 && currentActiveInDb == null) {
                    // Ничего не делаем
                } else if (currentDbActiveId != activeId) {
                    weatherRepository.setActiveLocation(activeId)
                    Log.d("MainViewModel", "Initial: setActiveLocation($activeId) called in DB.")
                }
            }

            val finalSelectedPagerItem = currentPagerItems.getOrNull(targetIndex)
            if (finalSelectedPagerItem != null &&
                _weatherDataStateMap.value[finalSelectedPagerItem.id] !is Resource.Success &&
                _weatherDataStateMap.value[finalSelectedPagerItem.id] !is Resource.Loading
            ) {

                val isLoadingDetails =
                    (finalSelectedPagerItem as? PagerItem.GeolocationPage)?.isLoadingDetails
                        ?: false
                if (!isLoadingDetails) {
                    Log.d(
                        "MainViewModel",
                        "Initial: Page $targetIndex (${finalSelectedPagerItem.displayName}) selected, weather not loaded/loading. Fetching weather."
                    )
                    fetchWeatherDataForPagerItem(finalSelectedPagerItem)
                }
            }

        } else {
            Log.w(
                "MainViewModel",
                "Initial: Could not determine any initial page. PagerItems size: ${currentPagerItems.size}, HasPerm: ${hasLocationPermission()}"
            )
            if (currentPagerItems.isEmpty() && !hasLocationPermission()) {
                val geoPageId =
                    PagerItem.GeolocationPage().id
                if (!(_weatherDataStateMap.value[geoPageId] is Resource.Error &&
                            _weatherDataStateMap.value[geoPageId]?.message?.contains(
                                "permission",
                                ignoreCase = true
                            ) == true)
                ) {
                    setPermissionError("Location permission needed or add a city.")
                }
            }
        }
    }

    private fun observeActivePagerItemToFetchWeather() {
        currentActivePagerItem
            .filterNotNull()
            .distinctUntilChanged { old, new ->
                val oldGeoLoading = (old as? PagerItem.GeolocationPage)?.isLoadingDetails
                val newGeoLoading = (new as? PagerItem.GeolocationPage)?.isLoadingDetails
                (old.id == new.id && oldGeoLoading == newGeoLoading)
            }
            .onEach { pagerItem ->
                Log.i(
                    "MainViewModel_Observer",
                    "Current Pager Item to fetch for: ${pagerItem.displayName} (ID: ${pagerItem.id}), isLoadingDetails: ${(pagerItem as? PagerItem.GeolocationPage)?.isLoadingDetails}"
                )
                if (pagerItem is PagerItem.GeolocationPage && pagerItem.isLoadingDetails) {
                    Log.d(
                        "MainViewModel_Observer",
                        "Geolocation page details are still loading (${pagerItem.fetchedCityName}). Weather fetch will wait."
                    )
                    return@onEach
                }
                Log.d(
                    "MainViewModel_Observer",
                    "Proceeding to fetchWeatherDataForPagerItem for ${pagerItem.displayName}"
                )
                fetchWeatherDataForPagerItem(pagerItem)
            }
            .launchIn(viewModelScope)
    }

    fun onPageChanged(pageIndex: Int) {
        viewModelScope.launch {
            val items = pagerItems.value
            if (pageIndex >= 0 && pageIndex < items.size) {
                val selectedPagerItem = items[pageIndex]
                Log.d(
                    "MainViewModel",
                    "onPageChanged: User scrolled/navigated to page $pageIndex, item: ${selectedPagerItem.displayName}"
                )

                if (_currentPagerIndex.value != pageIndex) {
                    _currentPagerIndex.value = pageIndex
                }

                if (selectedPagerItem is PagerItem.SavedPage) {
                    weatherRepository.setActiveLocation(selectedPagerItem.location.id)
                    Log.d(
                        "MainViewModel",
                        "onPageChanged: Set ${selectedPagerItem.displayName} (ID: ${selectedPagerItem.location.id}) as active in DB."
                    )
                } else if (selectedPagerItem is PagerItem.GeolocationPage) {
                    weatherRepository.setActiveLocation(0)
                    Log.d(
                        "MainViewModel",
                        "onPageChanged: Geolocation page selected. Set active ID to 0 in DB."
                    )
                }
            } else {
                Log.w(
                    "MainViewModel",
                    "onPageChanged: Invalid pageIndex: $pageIndex, items size: ${items.size}"
                )
            }
        }
    }

    private fun fetchDetailsForGeolocationPage() {
        if (!hasLocationPermission()) {
            Log.w(
                "MainViewModel",
                "fetchDetailsForGeolocationPage: Called without location permission!"
            )
            setPermissionError("Location permission needed for geolocation.")
            _geolocationPagerItemState.update {
                it.copy(
                    isLoadingDetails = false,
                    fetchedCityName = "Permission Denied"
                )
            }
            return
        }

        val geoPageId = PagerItem.GeolocationPage().id
        if (fetchWeatherJobs[geoPageId]?.isActive == true) {
            Log.d(
                "MainViewModel",
                "fetchDetailsForGeolocationPage: Job for $geoPageId is already active. isLoadingDetails: ${_geolocationPagerItemState.value.isLoadingDetails}"
            )
            return
        }
        if (!_geolocationPagerItemState.value.isLoadingDetails && _geolocationPagerItemState.value.fetchedCityName != "My Location" && _geolocationPagerItemState.value.fetchedCityName != "Loading location...") {
            Log.d(
                "MainViewModel",
                "fetchDetailsForGeolocationPage: Details already loaded for ${_geolocationPagerItemState.value.fetchedCityName}. isLoadingDetails: false. Triggering weather fetch if current."
            )
            if (currentActivePagerItem.value?.id == geoPageId && _weatherDataStateMap.value[geoPageId] !is Resource.Success) {
                fetchWeatherDataForPagerItem(_geolocationPagerItemState.value)
            }
            return
        }

        fetchWeatherJobs[geoPageId] = viewModelScope.launch {
            Log.d(
                "MainViewModel",
                "fetchDetailsForGeolocationPage: COROUTINE STARTED for $geoPageId."
            )
            if (!_geolocationPagerItemState.value.isLoadingDetails || _geolocationPagerItemState.value.fetchedCityName == "My Location") {
                _geolocationPagerItemState.update {
                    Log.d(
                        "MainViewModel",
                        "fetchDetailsForGeolocationPage: Updating geoPagerItemState to LOADING DETAILS."
                    )
                    PagerItem.GeolocationPage(
                        isLoadingDetails = true,
                        fetchedCityName = "Loading location..."
                    )
                }
            }

            try {
                Log.d(
                    "MainViewModel",
                    "fetchDetailsForGeolocationPage: Attempting to get current location from locationTracker..."
                )
                val locationData: Location? =
                    locationTracker.getCurrentLocation()

                if (locationData != null) {
                    Log.i(
                        "MainViewModel",
                        "fetchDetailsForGeolocationPage: Got locationData: Lat=${locationData.latitude}, Lon=${locationData.longitude}"
                    )
                    Log.d(
                        "MainViewModel",
                        "fetchDetailsForGeolocationPage: Attempting to get location details from repository..."
                    )
                    val details = weatherRepository.getLocationDetailsByCoordinates(
                        locationData.latitude, locationData.longitude, BuildConfig.GEOAPIFY_API_KEY
                    )
                    Log.d(
                        "MainViewModel",
                        "fetchDetailsForGeolocationPage: Got details from repository: $details"
                    )

                    var city = "Current Location"
                    var country: String? = null
                    if (details is Resource.Success && details.data?.properties != null) {
                        city =
                            details.data.properties.city ?: details.data.properties.formattedAddress
                                    ?: city
                        country = details.data.properties.countryCode?.uppercase()
                        Log.i(
                            "MainViewModel",
                            "fetchDetailsForGeolocationPage: Reverse geocoded to $city, $country"
                        )
                    } else if (details is Resource.Error) {
                        Log.w(
                            "MainViewModel",
                            "fetchDetailsForGeolocationPage: Failed to get location details: ${details.message}"
                        )
                        city = "Details Error"
                    }

                    _geolocationPagerItemState.update {
                        Log.i(
                            "MainViewModel",
                            "fetchDetailsForGeolocationPage: Updating geoPagerItemState with SUCCESSFUL details. City: $city, isLoadingDetails: false"
                        )
                        PagerItem.GeolocationPage(
                            lat = locationData.latitude,
                            lon = locationData.longitude,
                            fetchedCityName = city,
                            fetchedCountryCode = country,
                            isLoadingDetails = false
                        )
                    }
                } else {
                    Log.w("MainViewModel", "fetchDetailsForGeolocationPage: locationData is NULL.")
                    _geolocationPagerItemState.update {
                        Log.w(
                            "MainViewModel",
                            "fetchDetailsForGeolocationPage: Updating geoPagerItemState with LOCATION UNKNOWN. isLoadingDetails: false"
                        )
                        it.copy(
                            isLoadingDetails = false,
                            fetchedCityName = "Location Unknown"
                        )
                    }
                    _weatherDataStateMap.update { currentMap ->
                        Log.w(
                            "MainViewModel",
                            "fetchDetailsForGeolocationPage: Setting weatherDataStateMap to ERROR for $geoPageId due to null locationData."
                        )
                        currentMap.toMutableMap().apply {
                            this[geoPageId] =
                                Resource.Error("Could not get current geolocation (null from tracker).")
                        }.toMap()
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    "MainViewModel",
                    "fetchDetailsForGeolocationPage: CRITICAL ERROR in coroutine",
                    e
                )
                _geolocationPagerItemState.update {
                    Log.e(
                        "MainViewModel",
                        "fetchDetailsForGeolocationPage: Updating geoPagerItemState with LOCATION ERROR due to exception. isLoadingDetails: false"
                    )
                    it.copy(
                        isLoadingDetails = false,
                        fetchedCityName = "Location Error"
                    )
                }
                _weatherDataStateMap.update { currentMap ->
                    Log.e(
                        "MainViewModel",
                        "fetchDetailsForGeolocationPage: Setting weatherDataStateMap to ERROR for $geoPageId due to exception."
                    )
                    currentMap.toMutableMap().apply {
                        this[geoPageId] =
                            Resource.Error("Error fetching geolocation details: ${e.message}")
                    }.toMap()
                }
            } finally {
                Log.d(
                    "MainViewModel",
                    "fetchDetailsForGeolocationPage: COROUTINE FINISHED for $geoPageId."
                )
            }
        }
    }

    private fun fetchWeatherDataForPagerItem(pagerItem: PagerItem) {
        val itemId = pagerItem.id
        Log.d(
            "MainViewModel",
            "Attempting to fetch weather for ${pagerItem.displayName} (ID: $itemId). Current map state: ${_weatherDataStateMap.value[itemId]}"
        )

        fetchWeatherJobs[itemId]?.cancel()
        Log.d("MainViewModel", "Previous fetch job for $itemId cancelled (if existed).")

        fetchWeatherJobs[itemId] = viewModelScope.launch {
            Log.d(
                "MainViewModel",
                "fetchWeatherDataForPagerItem: COROUTINE STARTED for ${pagerItem.displayName}"
            )

            _weatherDataStateMap.update { currentMap ->
                Log.d(
                    "MainViewModel",
                    "fetchWeatherDataForPagerItem: Updating _weatherDataStateMap to Loading for ${pagerItem.displayName}"
                )
                currentMap.toMutableMap().apply {
                    this[itemId] =
                        Resource.Loading(message = "Fetching weather for ${pagerItem.displayName}...")
                }.toMap()
            }

            val currentUnits =
                userPreferencesFlow.value.temperatureUnit
            val unitsQueryParam =
                if (currentUnits == TemperatureUnit.FAHRENHEIT) "imperial" else "metric"

            val result = weatherRepository.getAllWeatherData(
                lat = pagerItem.latitude,
                lon = pagerItem.longitude,
                apiKey = BuildConfig.OPEN_WEATHER_API_KEY,
                units = unitsQueryParam
            )


            Log.d(
                "MainViewModel",
                "fetchWeatherDataForPagerItem: Got result for ${pagerItem.displayName}: $result"
            )

            _weatherDataStateMap.update { currentMap ->
                currentMap.toMutableMap().apply {
                    this[itemId] = result
                }.toMap()
            }

            if (result is Resource.Success) {
                Log.i(
                    "MainViewModel",
                    "Weather data fetched for ${pagerItem.displayName}: ${result.data?.currentWeather?.cityName}"
                )
            } else if (result is Resource.Error) {
                Log.e(
                    "MainViewModel",
                    "Error fetching weather for ${pagerItem.displayName}: ${result.message}"
                )
            }
            fetchWeatherJobs.remove(itemId)
            Log.d(
                "MainViewModel",
                "fetchWeatherDataForPagerItem: COROUTINE FINISHED for ${pagerItem.displayName}"
            )
        }
    }

    fun searchCityAutocomplete(query: String) {
        autocompleteJob?.cancel()
        if (query.length < 3) {
            _autocompleteResults.value = Resource.Success(emptyList()); return
        }

        viewModelScope.launch {
            flowOf(query)
                .flatMapLatest { debouncedQuery ->
                    Log.d("MainViewModel", "Debounced search for: $debouncedQuery")
                    try {
                        val resourceResult = weatherRepository.getCityAutocompleteSuggestions(
                            query = debouncedQuery,
                            apiKey = BuildConfig.GEOAPIFY_API_KEY
                        )
                        if (resourceResult is Resource.Success && resourceResult.data.isNullOrEmpty() && debouncedQuery.isNotBlank()) {
                            flowOf(Resource.Error("No cities found for \"$debouncedQuery\""))
                        } else {
                            flowOf(resourceResult)
                        }
                    } catch (e: Exception) {
                        flowOf(Resource.Error("Autocomplete search failed: ${e.message}"))
                    }
                }
                .catch { e ->
                    _autocompleteResults.value =
                        Resource.Error("Autocomplete search failed: ${e.message}")
                }
                .collectLatest { results -> _autocompleteResults.value = results }
        }
    }

    fun clearGeocodingResults() {
        _autocompleteResults.value = Resource.Success(emptyList())
        autocompleteJob?.cancel()
    }

    internal open fun onCitySuggestionSelected(cityGeoData: GeoapifyFeatureDto) {
        val properties = cityGeoData.properties
        val lat = properties?.latitude
        val lon = properties?.longitude
        val cityName = properties?.city ?: properties?.formattedAddress ?: "Unknown Location"

        // Перевіряємо ліміт, використовуючи поточне значення з StateFlow, який вже є
        // Або, якщо canAddNewLocation вже є, можна було б перевіряти його,
        // але для негайної реакції в цій функції краще взяти актуальний розмір.
        // Для цього нам потрібен StateFlow, який містить List<SavedLocation>.
        // _savedLocationsFromDbFlow - це Flow, але ми можемо створити з нього StateFlow для цього.
        // Або, простіше, використовувати кількість PagerItem.SavedPage з pagerItems.

        val currentSavedPagesCount = pagerItems.value.filterIsInstance<PagerItem.SavedPage>().size
        if (currentSavedPagesCount >= MAX_SAVED_LOCATIONS) {
            Log.w(
                "MainViewModel",
                "Cannot add new location. Limit of $MAX_SAVED_LOCATIONS saved locations reached. Current count: $currentSavedPagesCount"
            )
            // TODO: Встановити StateFlow для показу повідомлення користувачеві в UI (наприклад, SnackBar)
            // _showLimitReachedMessage.value = true (потім скинути)
            return
        }

        if (lat != null && lon != null) {
            viewModelScope.launch {
                // Отримуємо поточний розмір списку збережених з БД для orderIndex
                val currentSavedInDb =
                    _savedLocationsFromDbFlow.first() // Отримуємо останній список з БД

                val newSavedLocation = SavedLocation(
                    cityName = cityName.trim(),
                    countryCode = properties.countryCode?.uppercase(),
                    latitude = lat,
                    longitude = lon,
                    isCurrentActive = false, // Нова локація стає активною через setActiveLocation
                    orderIndex = currentSavedInDb.size // Новий порядок
                )
                val newId = weatherRepository.addSavedLocation(newSavedLocation)
                if (newId > 0L) {
                    weatherRepository.setActiveLocation(newId.toInt())
                    // Чекаємо, поки pagerItems оновиться, щоб включити нову локацію
                    val targetPageId = "saved_$newId"
                    try {
                        pagerItems.first { items -> items.any { it.id == targetPageId } }
                        val newPageIndex = pagerItems.value.indexOfFirst { it.id == targetPageId }
                        if (newPageIndex != -1) {
                            _currentPagerIndex.value = newPageIndex
                        }
                    } catch (e: NoSuchElementException) {
                        Log.e(
                            "MainViewModel",
                            "New page $targetPageId not found in pagerItems after add."
                        )
                    }
                } else if (newId == -2L) { // Вже існує
                    val existing =
                        currentSavedInDb.find { it.latitude == lat && it.longitude == lon }
                    existing?.let {
                        weatherRepository.setActiveLocation(it.id)
                        val existingPageIndex =
                            pagerItems.value.indexOfFirst { pgItem -> pgItem is PagerItem.SavedPage && pgItem.location.id == it.id }
                        if (existingPageIndex != -1) {
                            _currentPagerIndex.value = existingPageIndex
                        }
                    }
                }
            }
        }
    }

    internal open fun deleteLocationAndUpdatePager(locationId: Int) {
        viewModelScope.launch {
            val currentItemBeforeDelete = currentActivePagerItem.value
            val pageIndexOfDeleted =
                pagerItems.value.indexOfFirst { it is PagerItem.SavedPage && it.location.id == locationId }

            weatherRepository.deleteSavedLocation(locationId)

            val updatedPagerItems = pagerItems.first { currentItems ->
                currentItems.none { it is PagerItem.SavedPage && it.location.id == locationId } || currentItems.isEmpty()
            }

            if (currentItemBeforeDelete is PagerItem.SavedPage && currentItemBeforeDelete.location.id == locationId) {
                if (updatedPagerItems.isNotEmpty()) {
                    Log.d("MainViewModel", "Deleted active page. Setting pager to index 0.")
                    _currentPagerIndex.value = 0
                    (updatedPagerItems.firstOrNull() as? PagerItem.SavedPage)?.let {
                        weatherRepository.setActiveLocation(it.location.id)
                    }
                        ?: weatherRepository.setActiveLocation(0)
                } else {
                    Log.d(
                        "MainViewModel",
                        "All locations deleted. Pager index to 0 (or handle empty state)."
                    )
                    _currentPagerIndex.value = 0
                    _weatherDataStateMap.value =
                        mapOf("empty_list_error" to Resource.Error("No locations. Add a city."))
                }
            } else if (pageIndexOfDeleted != -1 && pageIndexOfDeleted < _currentPagerIndex.value) {
                _currentPagerIndex.value = (_currentPagerIndex.value - 1).coerceAtLeast(0)
                Log.d(
                    "MainViewModel",
                    "Deleted a page before current. Adjusted pager index to ${_currentPagerIndex.value}"
                )
            } else {
                if (_currentPagerIndex.value >= updatedPagerItems.size && updatedPagerItems.isNotEmpty()) {
                    _currentPagerIndex.value = updatedPagerItems.size - 1
                }
                Log.d(
                    "MainViewModel",
                    "Deleted non-active page. Current pager index: ${_currentPagerIndex.value}"
                )
            }
        }
    }

    internal open fun hasLocationPermission(): Boolean {
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
        val errorKey = currentActivePagerItem.value?.id
            ?: PagerItem.GeolocationPage().id

        _weatherDataStateMap.update { currentMap ->
            currentMap.toMutableMap().apply {
                this[errorKey] = Resource.Error(message = message)
            }.toMap()
        }

        if (errorKey == PagerItem.GeolocationPage().id && message.contains(
                "permission",
                ignoreCase = true
            )
        ) {
            _geolocationPagerItemState.update {
                it.copy(
                    isLoadingDetails = false,
                    fetchedCityName = if (message.contains("permanently denied")) "Permission Denied (Settings)" else "Permission Denied"
                )
            }
        }
        Log.e("MainViewModel", "Permission error set for $errorKey: $message")
    }

    fun handlePermissionGranted() {
        Log.d("MainViewModel", "handlePermissionGranted called by UI.")
        val geoPageId = PagerItem.GeolocationPage().id

        // Очистим предыдущую ошибку разрешений для geoPageId, если она была.
        // Это позволит избежать показа старой ошибки, если разрешения были даны из настроек.
        _weatherDataStateMap.update { currentMap ->
            val mutableMap = currentMap.toMutableMap()
            val currentResource = mutableMap[geoPageId]
            if (currentResource is Resource.Error && currentResource.message?.contains(
                    "permission",
                    ignoreCase = true
                ) == true
            ) {
                Log.d(
                    "MainViewModel",
                    "Clearing previous permission error for $geoPageId as permissions are now granted."
                )
                // Можно установить временное состояние загрузки или просто удалить,
                // чтобы fetchDetailsForGeolocationPage установил свое актуальное состояние.
                // Если просто удалить, то не будет промежуточного Loading(message="Permissions granted...")
                mutableMap.remove(geoPageId)
                // Либо, если хотите явный индикатор, но без специфичного сообщения "Permissions granted":
                // mutableMap[geoPageId] = Resource.Loading()
            }
            mutableMap.toMap()
        }

        // Сбрасываем состояние геолокационной страницы в начальное состояние загрузки.
        // Это важно, чтобы данные (координаты, название города) были запрошены заново.
        _geolocationPagerItemState.update {
            Log.d(
                "MainViewModel",
                "handlePermissionGranted: Updating _geolocationPagerItemState to initial loading state (isLoadingDetails=true)."
            )
            PagerItem.GeolocationPage( // Сброс в полностью начальное состояние
                isLoadingDetails = true,
                fetchedCityName = "Loading location...", // Начальный текст загрузки
                lat = 0.0,
                lon = 0.0,
                fetchedCountryCode = null
            )
        }
        Log.d(
            "MainViewModel",
            "Updated _geolocationPagerItemState to initial loading state due to permission grant."
        )

        // Триггер для обновления списка pagerItems, если геолокационная страница должна добавиться/удалиться
        viewModelScope.launch {
            Log.d(
                "MainViewModel",
                "Emitting to _permissionCheckTrigger from handlePermissionGranted"
            )
            _permissionCheckTrigger.tryEmit(Unit)
        }

        // Запускаем процесс получения деталей геолокации (координат и названия города).
        // Эта функция, в свою очередь, инициирует загрузку погоды, если это активная страница.
        fetchDetailsForGeolocationPage()
    }

    fun setCurrentPagerItemToSavedLocation(savedLocation: SavedLocation) {
        viewModelScope.launch {
            Log.d(
                "MainViewModel",
                "User selected saved location: ${savedLocation.cityName} from list. Setting active in DB."
            )
            weatherRepository.setActiveLocation(savedLocation.id)
            try {
                val updatedPagerItems = pagerItems.first { items ->
                    items.any { it is PagerItem.SavedPage && it.location.id == savedLocation.id }
                }

                val newPageIndex =
                    updatedPagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.id == savedLocation.id }

                if (newPageIndex != -1) {
                    Log.d(
                        "MainViewModel",
                        "Setting pager index to $newPageIndex for ${savedLocation.cityName} after selection from list."
                    )
                    if (_currentPagerIndex.value != newPageIndex) {
                        _currentPagerIndex.value = newPageIndex
                    } else {
                        fetchWeatherDataForPagerItem(updatedPagerItems[newPageIndex])
                    }
                } else {
                    Log.w(
                        "MainViewModel",
                        "Could not find PagerItem index for newly selected saved location: ${savedLocation.cityName} even after waiting for pagerItems update."
                    )
                }
            } catch (e: NoSuchElementException) {
                Log.e(
                    "MainViewModel",
                    "Timeout or error waiting for pagerItems to update for ${savedLocation.cityName}",
                    e
                )
            }
        }
    }

    // Метод, який викликається при Pull-to-Refresh
    fun refreshCurrentPageWeather() {
        viewModelScope.launch {
            val currentItem = currentActivePagerItem.value ?: run {
                if (_isRefreshing.value) _isRefreshing.value = false
                Log.w("MainViewModel", "Refresh: currentActivePagerItem is null, exiting.")
                return@launch
            }

            Log.i(
                "MainViewModel",
                "Pull-to-refresh triggered for: ${currentItem.displayName} (ID: ${currentItem.id})"
            )
            _isRefreshing.value = true

            val itemId = currentItem.id

            try {
                // <<< НАЧАЛО ИЗМЕНЕНИЯ >>>
                // Устанавливаем состояние Loading для ЛЮБОЙ страницы перед началом обновления.
                // Это гарантирует, что .first { !isLoading } будет ждать завершения НОВОГО запроса.
                _weatherDataStateMap.update { currentMap ->
                    currentMap.toMutableMap().apply {
                        this[itemId] =
                            Resource.Loading(message = "Refreshing weather for ${currentItem.displayName}...")
                    }.toMap()
                }
                Log.d("MainViewModel", "Refresh: Set _weatherDataStateMap to Loading for $itemId.")
                // <<< КОНЕЦ ИЗМЕНЕНИЯ >>>

                if (currentItem is PagerItem.GeolocationPage) {
                    Log.d(
                        "MainViewModel",
                        "Refresh: Handling GeolocationPage. Calling fetchDetailsForGeolocationPage for $itemId."
                    )
                    // Для геолокации, fetchDetailsForGeolocationPage инициирует цепочку,
                    // которая в итоге вызовет fetchWeatherDataForPagerItem,
                    // которая снова установит Loading, а затем Success/Error.
                    // Предыдущая установка Loading здесь служит для немедленного UI отклика.
                    fetchDetailsForGeolocationPage()
                } else { // Для сохраненной страницы (PagerItem.SavedPage)
                    Log.d(
                        "MainViewModel",
                        "Refresh: Handling SavedPage. Calling fetchWeatherDataForPagerItem for ${itemId}."
                    )
                    // fetchWeatherDataForPagerItem сама установит Resource.Loading (перезапишет наше),
                    // а затем Success/Error.
                    fetchWeatherDataForPagerItem(currentItem)
                }

                Log.d(
                    "MainViewModel",
                    "Refresh: Waiting for weather data for $itemId to finish loading (i.e., not be in Loading state). Current state before wait: ${_weatherDataStateMap.value[itemId]?.javaClass?.simpleName}"
                )

                _weatherDataStateMap.first { stateMap ->
                    val weatherResource = stateMap[itemId]
                    val isLoading = weatherResource is Resource.Loading
                    // Добавим более подробный лог внутри .first для отладки
                    Log.v(
                        "MainViewModel_RefreshWait",
                        "Checking state for $itemId: ${weatherResource?.javaClass?.simpleName}. IsLoading: $isLoading. Message: ${(weatherResource as? Resource.Loading)?.message ?: (weatherResource as? Resource.Error)?.message}"
                    )
                    weatherResource != null && !isLoading // Ждем, пока состояние не перестанет быть Loading
                }

                val finalState = _weatherDataStateMap.value[itemId]
                Log.i(
                    "MainViewModel",
                    "Refresh: Weather data loading finished for $itemId. Final state: ${finalState?.javaClass?.simpleName}"
                )

            } catch (e: Exception) {
                Log.e(
                    "MainViewModel",
                    "Exception during pull-to-refresh waiting logic for $itemId",
                    e
                )
                // Убедимся, что _isRefreshing сбрасывается даже при ошибке в ожидании
                if (_isRefreshing.value) _isRefreshing.value = false
            } finally {
                // _isRefreshing должен быть сброшен после того, как все операции завершены
                // или если произошла ошибка, которая не была поймана выше.
                // .first{} блокирует корутину, так что finally выполнится после ее завершения.
                if (_isRefreshing.value) { // Дополнительная проверка, если вдруг уже false
                    _isRefreshing.value = false
                }
                Log.i(
                    "MainViewModel",
                    "Pull-to-refresh finalized. _isRefreshing set to false for: ${currentItem.displayName}"
                )
            }
        }
    }

    fun updateTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch {
            userPreferencesRepository.updateTemperatureUnit(unit)
            currentActivePagerItem.value?.let {
                Log.d(
                    "MainViewModel",
                    "Temperature unit changed, refreshing weather for ${it.displayName}"
                )
                _weatherDataStateMap.update { currentMap ->
                    currentMap.toMutableMap().apply {
                        this[it.id] = Resource.Loading(message = "Updating units...")
                    }.toMap()
                }
                fetchWeatherDataForPagerItem(it)
            }
        }
    }

    fun moveSavedLocation(fromIndexInFilteredList: Int, toIndexInFilteredList: Int) {
        viewModelScope.launch {
            val currentSavedPages = pagerItems.value.filterIsInstance<PagerItem.SavedPage>()

            if (fromIndexInFilteredList < 0 || fromIndexInFilteredList >= currentSavedPages.size ||
                toIndexInFilteredList < 0 || toIndexInFilteredList >= currentSavedPages.size
            ) {
                Log.w(
                    "MainViewModel",
                    "moveSavedLocation: Invalid indices. From: $fromIndexInFilteredList, To: $toIndexInFilteredList, Size: ${currentSavedPages.size}"
                )
                return@launch
            }

            val listToReorder = currentSavedPages.map { it.location }.toMutableList()

            val itemToMove = listToReorder.removeAt(fromIndexInFilteredList)
            listToReorder.add(toIndexInFilteredList, itemToMove)

            val updatedOrderLocations = listToReorder.mapIndexed { newOrderIndex, savedLocation ->
                savedLocation.copy(orderIndex = newOrderIndex)
            }

            weatherRepository.updateSavedLocationsOrder(updatedOrderLocations)

            val currentActiveItem = currentActivePagerItem.value
            if (currentActiveItem is PagerItem.SavedPage) {
                // Ожидаем обновления pagerItems после изменения порядка в БД
                // Это нужно, чтобы получить правильный новый индекс активного элемента
                val updatedPagerItemsAfterReorder = pagerItems.first { items ->
                    // Проверяем, что порядок в pagerItems соответствует новому порядку updatedOrderLocations
                    // Это упрощенная проверка; в идеале, нужно убедиться, что все элементы на своих местах.
                    // Для простоты, можно просто подождать, пока ID активного элемента появится в списке.
                    items.filterIsInstance<PagerItem.SavedPage>()
                        .map { it.location.id } == updatedOrderLocations.map { it.id } ||
                            items.any { it.id == currentActiveItem.id } // Убедимся, что активный элемент все еще там
                }

                val newIndexOfActive =
                    updatedPagerItemsAfterReorder.indexOfFirst { it.id == currentActiveItem.id }
                if (newIndexOfActive != -1 && _currentPagerIndex.value != newIndexOfActive) {
                    _currentPagerIndex.value = newIndexOfActive
                    Log.d(
                        "MainViewModel",
                        "Pager index updated to $newIndexOfActive after reorder due to active item shift."
                    )
                }
            }
            Log.d("MainViewModel", "Saved locations reordered in DB.")
        }
    }

    fun updateAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            userPreferencesRepository.updateAppTheme(theme)
            // Перезавантаження погоди не потрібне, тема змінюється на рівні UI
        }
    }
}