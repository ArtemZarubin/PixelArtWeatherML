package com.artemzarubin.weatherml.ui.mainscreen

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
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
import kotlinx.coroutines.flow.asSharedFlow
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

    private val _userMessages = MutableSharedFlow<String>() // Для общих сообщений пользователю
    val userMessages = _userMessages.asSharedFlow()

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
                // Существующая логика distinctUntilChanged
                val oldGeoLoading = (old as? PagerItem.GeolocationPage)?.isLoadingDetails
                val newGeoLoading = (new as? PagerItem.GeolocationPage)?.isLoadingDetails
                // Добавим проверку на изменение координат, если это геолокационная страница,
                // чтобы перезагрузить погоду, если координаты обновились, даже если isLoadingDetails не менялся.
                val coordinatesChanged =
                    if (old is PagerItem.GeolocationPage && new is PagerItem.GeolocationPage) {
                        old.latitude != new.latitude || old.longitude != new.longitude
                    } else false
                (old.id == new.id && oldGeoLoading == newGeoLoading && !coordinatesChanged)
            }
            .onEach { pagerItem ->
                Log.i(
                    "MainViewModel_Observer",
                    "Current Pager Item to evaluate for fetch: ${pagerItem.displayName} (ID: ${pagerItem.id}), isLoadingDetails: ${(pagerItem as? PagerItem.GeolocationPage)?.isLoadingDetails}, Lat: ${pagerItem.latitude}, Lon: ${pagerItem.longitude}"
                )

                if (pagerItem is PagerItem.GeolocationPage) {
                    if (pagerItem.isLoadingDetails) {
                        Log.d(
                            "MainViewModel_Observer",
                            "Geolocation page details are still loading (${pagerItem.fetchedCityName}). Weather fetch will wait."
                        )
                        return@onEach
                    }
                    // --- НОВАЯ ПРОВЕРКА ---
                    // Не запускать загрузку погоды для геолокации, если текущее состояние - ошибка "GPS is disabled"
                    val currentGeoState = _weatherDataStateMap.value[pagerItem.id]
                    if (currentGeoState is Resource.Error && currentGeoState.message?.contains(
                            "GPS is disabled",
                            ignoreCase = true
                        ) == true
                    ) {
                        Log.w(
                            "MainViewModel_Observer",
                            "Skipping weather fetch for GeolocationPage because GPS is currently reported as disabled. State: $currentGeoState"
                        )
                        return@onEach
                    }
                    // --- КОНЕЦ НОВОЙ ПРОВЕРКИ ---
                }

                // Проверяем, нужно ли вообще загружать погоду (например, если она уже есть и успешна)
                // Это можно оптимизировать, но для начала оставим как есть, чтобы не сломать другую логику.
                // Важно, чтобы fetchWeatherDataForPagerItem вызывался, если данных нет или они не Resource.Success.
                val weatherState = _weatherDataStateMap.value[pagerItem.id]
                if (weatherState is Resource.Success && pagerItem !is PagerItem.GeolocationPage) { // Для сохраненных можно не перезагружать, если уже есть
                    // Однако, если это геолокация и координаты могли обновиться, то стоит перезагрузить.
                    // Эта логика может быть сложной, пока оставим так.
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
        val geoPageId = PagerItem.GeolocationPage().id
        // Отменяем предыдущую активную работу, если она есть, чтобы избежать гонок состояний
        fetchWeatherJobs[geoPageId]?.cancel() // Отмена предыдущего Job для этого ID

        fetchWeatherJobs[geoPageId] = viewModelScope.launch {
            // Убедимся, что PagerItem обновлен и isLoadingDetails = true
            if (!_geolocationPagerItemState.value.isLoadingDetails) {
                _geolocationPagerItemState.update {
                    it.copy(
                        isLoadingDetails = true,
                        fetchedCityName = "Loading location..."
                    )
                }
            }
            // Устанавливаем состояние загрузки в карте состояний
            _weatherDataStateMap.update { currentMap ->
                currentMap.toMutableMap().apply {
                    this[geoPageId] =
                        Resource.Loading(message = "Fetching geolocation details...")
                }.toMap()
            }

            locationTracker.getCurrentLocation().collectLatest { locationResource ->
                // val geoPageId = PagerItem.GeolocationPage().id // Уже определен выше
                when (locationResource) {
                    is Resource.Success -> {
                        val locationData = locationResource.data
                        if (locationData != null) {
                            Log.i(
                                "MainViewModel",
                                "Geo Details: Got locationData: Lat=${locationData.latitude}, Lon=${locationData.longitude}"
                            )
                            val details = weatherRepository.getLocationDetailsByCoordinates(
                                locationData.latitude,
                                locationData.longitude,
                                BuildConfig.GEOAPIFY_API_KEY
                            )
                            var city = "Current Location"
                            var country: String? = null
                            if (details is Resource.Success && details.data?.properties != null) {
                                city = details.data.properties.city
                                    ?: details.data.properties.formattedAddress ?: city
                                country = details.data.properties.countryCode?.uppercase()
                            } else if (details is Resource.Error) {
                                city = "Details Error"
                            }
                            _geolocationPagerItemState.update {
                                it.copy(
                                    lat = locationData.latitude,
                                    lon = locationData.longitude,
                                    fetchedCityName = city,
                                    fetchedCountryCode = country,
                                    isLoadingDetails = false // <--- ВАЖНО: сбросить после получения деталей
                                )
                            }
                            // После обновления _geolocationPagerItemState, observeActivePagerItemToFetchWeather
                            // должен среагировать и вызвать fetchWeatherDataForPagerItem, если это активная страница.
                            // Если _weatherDataStateMap[geoPageId] все еще Loading, то fetchWeatherDataForPagerItem перезапишет его.
                        } else {
                            _geolocationPagerItemState.update {
                                it.copy(
                                    isLoadingDetails = false,
                                    fetchedCityName = "Location Unknown"
                                )
                            }
                            _weatherDataStateMap.update { currentMap ->
                                currentMap.toMutableMap().apply {
                                    this[geoPageId] =
                                        Resource.Error("Could not get current geolocation (null data).")
                                }.toMap()
                            }
                        }
                    }

                    is Resource.Error -> {
                        val errorMessage = locationResource.message ?: "Failed to get location."
                        Log.w("MainViewModel", "Error from locationTracker: $errorMessage")
                        _geolocationPagerItemState.update {
                            it.copy(
                                isLoadingDetails = false,
                                fetchedCityName = if (errorMessage.contains(
                                        "GPS is disabled",
                                        ignoreCase = true
                                    )
                                ) "GPS Disabled" else "Location Error",
                                // Важно не сбрасывать lat/lon, если они уже были, но здесь это не тот случай
                                lat = 0.0, // Сбрасываем, так как геолокация не удалась
                                lon = 0.0
                            )
                        }
                        _weatherDataStateMap.update { currentMap ->
                            currentMap.toMutableMap().apply {
                                this[geoPageId] = Resource.Error(message = errorMessage)
                            }.toMap()
                        }
                    }

                    is Resource.Loading -> {
                        Log.d("MainViewModel", "Geo Details: LocationTracker is Loading...")
                        _geolocationPagerItemState.update {
                            it.copy(
                                isLoadingDetails = true, // Убедимся, что флаг установлен
                                fetchedCityName = locationResource.message ?: "Fetching location..."
                            )
                        }
                        // Также обновим _weatherDataStateMap, чтобы UI показывал сообщение от LocationTracker
                        _weatherDataStateMap.update { currentMap ->
                            currentMap.toMutableMap().apply {
                                this[geoPageId] = Resource.Loading(
                                    message = locationResource.message
                                        ?: "Fetching location details..."
                                )
                            }.toMap()
                        }
                    }
                }
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

        val currentSavedPagesCount = pagerItems.value.filterIsInstance<PagerItem.SavedPage>().size
        if (currentSavedPagesCount >= MAX_SAVED_LOCATIONS) {
            Log.w(
                "MainViewModel",
                "Cannot add new location. Limit of $MAX_SAVED_LOCATIONS saved locations reached."
            )
            viewModelScope.launch {
                _userMessages.emit("You have reached the maximum number of saved locations (${MAX_SAVED_LOCATIONS}).")
            }
            return
        }

        if (lat != null && lon != null) {
            viewModelScope.launch {
                val currentSavedInDb = _savedLocationsFromDbFlow.first()

                val newSavedLocation = SavedLocation(
                    cityName = cityName.trim(),
                    countryCode = properties.countryCode?.uppercase(),
                    latitude = lat,
                    longitude = lon,
                    isCurrentActive = false,
                    orderIndex = currentSavedInDb.size
                )
                val newId = weatherRepository.addSavedLocation(newSavedLocation)

                if (newId > 0L) { // Успешно добавлено новое местоположение
                    weatherRepository.setActiveLocation(newId.toInt())
                    val targetPageId = "saved_$newId"
                    try {
                        // Ожидаем обновления pagerItems и затем устанавливаем индекс
                        pagerItems.first { items -> items.any { it.id == targetPageId } }
                        val newPageIndex = pagerItems.value.indexOfFirst { it.id == targetPageId }
                        if (newPageIndex != -1) {
                            _currentPagerIndex.value = newPageIndex
                            Log.d(
                                "MainViewModel",
                                "Set current pager index to $newPageIndex for new city."
                            )
                        }
                        _userMessages.emit("${newSavedLocation.cityName} added and set as active.") // Сообщение об успешном добавлении
                    } catch (e: NoSuchElementException) {
                        Log.e(
                            "MainViewModel",
                            "New page $targetPageId not found in pagerItems after add."
                        )
                    }
                } else if (newId == -2L) { // Местоположение уже существует
                    val existing =
                        currentSavedInDb.find { it.latitude == lat && it.longitude == lon }
                    val existingCityName = existing?.cityName
                        ?: cityName // Используем имя из БД, если есть, или из DTO
                    Log.w("MainViewModel", "$existingCityName already exists in saved locations.")
                    _userMessages.emit("$existingCityName is already in your saved locations.") // Отправляем сообщение в UI

                    existing?.let {
                        // Если город уже существует, делаем его активным и переключаемся на него
                        weatherRepository.setActiveLocation(it.id)
                        val existingPageIndex =
                            pagerItems.value.indexOfFirst { pgItem -> pgItem is PagerItem.SavedPage && pgItem.location.id == it.id }
                        if (existingPageIndex != -1) {
                            if (_currentPagerIndex.value != existingPageIndex) {
                                _currentPagerIndex.value = existingPageIndex
                                Log.d(
                                    "MainViewModel",
                                    "Set current pager index to $existingPageIndex for existing city."
                                )
                            } else {
                                // Если уже на этой странице, можно просто обновить погоду (на всякий случай)
                                fetchWeatherDataForPagerItem(pagerItems.value[existingPageIndex])
                            }
                        }
                    }
                } else { // Другая ошибка при добавлении
                    Log.e("MainViewModel", "Failed to add location, repository returned: $newId")
                    _userMessages.emit("Could not add ${cityName}. Please try again.")
                }
            }
        } else {
            viewModelScope.launch {
                _userMessages.emit("Could not add location: Invalid coordinates.")
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

    // Вызывается, когда разрешения есть И GPS включен (например, из ON_START или после запроса разрешений)
    fun handlePermissionAndGpsGranted() {
        Log.d("MainViewModel", "handlePermissionAndGpsGranted called.")
        val geoPageId = PagerItem.GeolocationPage().id

        _weatherDataStateMap.update { currentMap ->
            val mutableMap = currentMap.toMutableMap()
            // Всегда устанавливаем Loading, так как инициируем новую последовательность загрузки
            Log.d("MainViewModel", "handlePermissionAndGpsGranted: Setting geoPageId to Loading.")
            mutableMap[geoPageId] = Resource.Loading(message = "Fetching location details...")
            mutableMap.toMap()
        }

        _geolocationPagerItemState.update {
            Log.d(
                "MainViewModel",
                "Resetting _geolocationPagerItemState to initial loading state for handlePermissionAndGpsGranted."
            )
            PagerItem.GeolocationPage(
                isLoadingDetails = true,
                fetchedCityName = "Loading location...", // Начальный текст
                lat = 0.0, // Сбрасываем координаты
                lon = 0.0,
                fetchedCountryCode = null
            )
        }

        viewModelScope.launch {
            Log.d(
                "MainViewModel",
                "Emitting to _permissionCheckTrigger from handlePermissionAndGpsGranted"
            )
            _permissionCheckTrigger.tryEmit(Unit)
        }
        fetchDetailsForGeolocationPage() // Этот метод должен использовать обновленный LocationTracker
    }

    // Новый метод, чтобы принудительно установить ошибку "GPS is disabled"
    // Это нужно, если разрешения есть, но GPS выключен (например, при запуске или возврате из настроек)
    fun forceGpsDisabledError() {
        val geoPageId = PagerItem.GeolocationPage().id
        val errorMessage = "GPS is disabled. Please enable location services."
        Log.d("MainViewModel", "forceGpsDisabledError: Setting GPS disabled error for $geoPageId")

        _weatherDataStateMap.update { currentMap ->
            currentMap.toMutableMap().apply {
                this[geoPageId] = Resource.Error(message = errorMessage)
            }.toMap()
        }
        // Также обновить состояние PagerItem.GeolocationPage, если это необходимо для UI
        _geolocationPagerItemState.update {
            it.copy(
                isLoadingDetails = false, // Загрузка деталей не идет, так как GPS выключен
                fetchedCityName = "GPS Disabled" // Или другое соответствующее имя
            )
        }
        // Триггер для обновления списка pagerItems, если геолокационная страница должна удалиться/обновиться
        viewModelScope.launch {
            _permissionCheckTrigger.tryEmit(Unit)
        }
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