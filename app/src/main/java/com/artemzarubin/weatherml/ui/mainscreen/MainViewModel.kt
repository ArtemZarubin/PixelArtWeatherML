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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.artemzarubin.weatherml.data.preferences.UserPreferencesRepository
import com.artemzarubin.weatherml.data.preferences.TemperatureUnit
import com.artemzarubin.weatherml.data.preferences.UserPreferences

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

@HiltViewModel
class MainViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val locationTracker: LocationTracker,
    private val userPreferencesRepository: UserPreferencesRepository, // <--- НОВА ЗАЛЕЖНІСТЬ
    private val application: Application
) : ViewModel() {

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
    val _geolocationPagerItemState =
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
                initialValue = UserPreferences(TemperatureUnit.CELSIUS) // Початкове значення
            )

    init {
        Log.d("MainViewModel", "ViewModel initialized.")
        observeActivePagerItemToFetchWeather() // Запускаем наблюдателя за погодой

        viewModelScope.launch {
            // Ждем первого стабильного состояния pagerItems
            pagerItems.first { items ->
                val permissionGranted = hasLocationPermission()
                // Получаем текущее состояние isLoadingDetails для геолокационной страницы
                val geolocationPageIsCurrentlyLoadingDetails =
                    _geolocationPagerItemState.value.isLoadingDetails

                if (permissionGranted) {
                    // Если разрешение есть, считаем список стабильным, если:
                    // 1. В списке уже есть GeolocationPage (неважно, грузятся ли для нее детали,
                    //    determineInitialPage вызовет fetchDetailsForGeolocationPage если нужно).
                    // ИЛИ
                    // 2. В списке есть хотя бы одна SavedPage.
                    // ИЛИ
                    // 3. GeolocationPage ЕЩЕ НЕТ в списке, НО ее детали УЖЕ НЕ грузятся
                    //    (т.е. isLoadingDetails стало false - загрузка завершилась успехом/ошибкой, или не начиналась).
                    //    Это позволяет продолжить, если геолокация не определилась, но мы не хотим ждать вечно.
                    val hasGeolocationPage = items.any { it is PagerItem.GeolocationPage }
                    val hasSavedPage = items.any { it is PagerItem.SavedPage }

                    if (hasGeolocationPage || hasSavedPage) {
                        true // Либо геолокация, либо сохраненная страница уже в списке
                    } else {
                        // Если ни того, ни другого еще нет, но разрешение есть,
                        // мы ждем, только если детали геолокации все еще активно грузятся.
                        // Если они НЕ грузятся, значит, либо они загружены (и скоро страница появится),
                        // либо была ошибка, либо геолокация не будет доступна. В этом случае можно продолжать.
                        !geolocationPageIsCurrentlyLoadingDetails
                    }
                } else {
                    // Если разрешения нет:
                    // Список стабилен, если в нем есть хотя бы одна SavedPage,
                    // или если он пуст (determineInitialPage покажет ошибку или ничего).
                    items.any { it is PagerItem.SavedPage } || items.isEmpty()
                }
            }.let { initialItems ->
                // Убедимся, что выполняем только один раз, даже если pagerItems эмитит снова быстро
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
        // Остальная часть init, если есть (например, tryInitialSetupOrLoadActive, если он все еще нужен и делает что-то другое)
        // Если tryInitialSetupOrLoadActive дублирует логику determineInitialPage, его можно удалить или рефакторить.
        // tryInitialSetupOrLoadActive() // <-- Проверь, нужен ли этот вызов здесь
    }

    private suspend fun determineInitialPage(currentPagerItems: List<PagerItem>) {
        Log.d(
            "MainViewModel",
            "determineInitialPage called with ${currentPagerItems.size} items. HasPerm: ${hasLocationPermission()}"
        )
        var targetIndex = -1
        var newActiveLocationIdToSetInDb: Int? = null

        // 1. ПРИОРИТЕТ: Геолокация при запуске, если есть разрешение
        if (hasLocationPermission()) {
            val geoPageIndex = currentPagerItems.indexOfFirst { it is PagerItem.GeolocationPage }
            if (geoPageIndex != -1) {
                targetIndex = geoPageIndex
                newActiveLocationIdToSetInDb =
                    0 // 0 означает, что активна геолокация (нет активного *сохраненного*)
                Log.i(
                    "MainViewModel",
                    "Initial: Geolocation is available. Setting as initial page (Index: $targetIndex). DB active ID will be 0."
                )

                val geoPage = currentPagerItems[targetIndex] as PagerItem.GeolocationPage
                // Если детали геолокации еще не загружены (например, lat/lon = 0.0 или имя дефолтное)
                // или если она явно в состоянии загрузки деталей.
                if (geoPage.isLoadingDetails || (geoPage.lat == 0.0 && geoPage.lon == 0.0 && geoPage.fetchedCityName == "My Location")) {
                    Log.d(
                        "MainViewModel",
                        "Initial: Geolocation page ($geoPage) needs details or is loading. Fetching..."
                    )
                    fetchDetailsForGeolocationPage() // Запрашиваем/обновляем детали
                }
            } else {
                // Это может случиться, если разрешение есть, но GeolocationPage еще не успела добавиться в pagerItems
                // (например, из-за асинхронности Flow).
                Log.w(
                    "MainViewModel",
                    "Initial: Has permission, but GeolocationPage not found in currentPagerItems. Attempting to fetch details anyway."
                )
                // Попытаемся запустить загрузку деталей геолокации, если она еще не идет.
                // Это может помочь ей появиться в pagerItems при следующем обновлении.
                if (!_geolocationPagerItemState.value.isLoadingDetails) {
                    fetchDetailsForGeolocationPage()
                }
            }
        }

        // 2. ЗАПАСНОЙ ВАРИАНТ: Если геолокация не была установлена (нет разрешения, или не нашлась сразу)
        if (targetIndex == -1) {
            // Ищем последний активный *сохраненный* город в БД
            val activeSavedInDb =
                (_savedLocationsFromDbFlow.firstOrNull() ?: emptyList()).find { it.isCurrentActive }
            if (activeSavedInDb != null) {
                val activeSavedIndexInPager =
                    currentPagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.id == activeSavedInDb.id }
                if (activeSavedIndexInPager != -1) {
                    targetIndex = activeSavedIndexInPager
                    newActiveLocationIdToSetInDb =
                        activeSavedInDb.id // Этот сохраненный город будет активным
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

            // Если все еще нет targetIndex (не было геолокации, не было активного в БД или он не нашелся в пейджере)
            // Берем первую *сохраненную* страницу, если таковые имеются
            if (targetIndex == -1) {
                val firstSavedPageIndex =
                    currentPagerItems.indexOfFirst { it is PagerItem.SavedPage }
                if (firstSavedPageIndex != -1) {
                    targetIndex = firstSavedPageIndex
                    val firstSavedPageItem =
                        currentPagerItems[firstSavedPageIndex] as PagerItem.SavedPage
                    newActiveLocationIdToSetInDb =
                        firstSavedPageItem.location.id // Первый сохраненный становится активным
                    Log.i(
                        "MainViewModel",
                        "Initial: No geo/active. Using first saved page: ${firstSavedPageItem.displayName} (Index: $targetIndex). DB active ID will be ${firstSavedPageItem.location.id}."
                    )
                }
            }
        }

        // Устанавливаем вычисленный индекс и обновляем активный город в БД
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

            // Обновляем запись об активном городе в БД
            newActiveLocationIdToSetInDb?.let { activeId ->
                // Проверяем, нужно ли вообще обновлять БД, чтобы избежать лишних записей,
                // если состояние в БД уже соответствует (хотя setActiveLocation обычно сама это проверяет)
                val currentActiveInDb = (_savedLocationsFromDbFlow.firstOrNull()
                    ?: emptyList()).find { it.isCurrentActive }
                val currentDbActiveId = if (currentActiveInDb != null) currentActiveInDb.id else 0
                if (activeId == 0 && currentActiveInDb == null) { // Хотим геолокацию, и в БД уже нет активного сохраненного
                    // Ничего не делаем
                } else if (currentDbActiveId != activeId) {
                    weatherRepository.setActiveLocation(activeId)
                    Log.d("MainViewModel", "Initial: setActiveLocation($activeId) called in DB.")
                }
            }

            // Дополнительная проверка: если выбранная страница - геолокация, но погода для нее еще не загружена
            // (и детали не грузятся), то инициируем загрузку погоды.
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
            // Если список страниц пуст и нет разрешения на геолокацию, показываем ошибку.
            if (currentPagerItems.isEmpty() && !hasLocationPermission()) {
                val geoPageId =
                    PagerItem.GeolocationPage().id // Используем ID геолокационной страницы для ошибки разрешений
                // Устанавливаем ошибку, только если ее еще нет или она не связана с разрешениями
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

    private fun tryFirstSavedOrGeo(
        currentPagerItems: List<PagerItem>,
        savedFromDb: List<SavedLocation>
    ) {
        if (savedFromDb.isNotEmpty()) {
            val firstSavedIndex =
                currentPagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.id == savedFromDb.first().id }
            if (firstSavedIndex != -1) _currentPagerIndex.value = firstSavedIndex
        } else if (hasLocationPermission()) {
            val geoIndex = currentPagerItems.indexOfFirst { it is PagerItem.GeolocationPage }
            if (geoIndex != -1) _currentPagerIndex.value = geoIndex
        }
    }


    private fun observeActivePagerItemToFetchWeather() {
        currentActivePagerItem
            .filterNotNull()
            .distinctUntilChanged { old, new ->
                val oldGeoLoading = (old as? PagerItem.GeolocationPage)?.isLoadingDetails
                val newGeoLoading = (new as? PagerItem.GeolocationPage)?.isLoadingDetails
                // Логируем для отладки distinctUntilChanged
                // Log.d("MainViewModel_Observer", "distinctUntilChanged: oldID=${old.id}, newID=${new.id}, oldGeoLoading=$oldGeoLoading, newGeoLoading=$newGeoLoading")
                (old.id == new.id && oldGeoLoading == newGeoLoading) // Упрощенное условие, если id одинаковый и isLoadingDetails для гео не изменился
            }
            .onEach { pagerItem ->
                Log.i(
                    "MainViewModel_Observer", // Изменен тег для легкого поиска
                    "Current Pager Item to fetch for: ${pagerItem.displayName} (ID: ${pagerItem.id}), isLoadingDetails: ${(pagerItem as? PagerItem.GeolocationPage)?.isLoadingDetails}"
                )
                if (pagerItem is PagerItem.GeolocationPage && pagerItem.isLoadingDetails) {
                    Log.d(
                        "MainViewModel_Observer",
                        "Geolocation page details are still loading (${pagerItem.fetchedCityName}). Weather fetch will wait."
                    )
                    return@onEach // Ждем, пока isLoadingDetails станет false
                }
                // Если мы здесь, значит либо это SavedPage, либо GeolocationPage с isLoadingDetails = false
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

                // Обновляем активный город в БД в зависимости от выбранной страницы
                if (selectedPagerItem is PagerItem.SavedPage) {
                    weatherRepository.setActiveLocation(selectedPagerItem.location.id)
                    Log.d(
                        "MainViewModel",
                        "onPageChanged: Set ${selectedPagerItem.displayName} (ID: ${selectedPagerItem.location.id}) as active in DB."
                    )
                } else if (selectedPagerItem is PagerItem.GeolocationPage) {
                    // Если пользователь выбрал страницу геолокации,
                    // это означает, что никакой *сохраненный* город не является активным.
                    // Используем 0 или специальный ID для этого состояния в БД.
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

    fun fetchDetailsForGeolocationPage() {
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
        // Проверяем, не выполняется ли уже активная работа для этой страницы
        if (fetchWeatherJobs[geoPageId]?.isActive == true) {
            Log.d(
                "MainViewModel",
                "fetchDetailsForGeolocationPage: Job for $geoPageId is already active. isLoadingDetails: ${_geolocationPagerItemState.value.isLoadingDetails}"
            )
            return
        }
        // Проверяем, если детали УЖЕ успешно загружены (isLoadingDetails = false и имя города не дефолтное)
        if (!_geolocationPagerItemState.value.isLoadingDetails && _geolocationPagerItemState.value.fetchedCityName != "My Location" && _geolocationPagerItemState.value.fetchedCityName != "Loading location...") {
            Log.d(
                "MainViewModel",
                "fetchDetailsForGeolocationPage: Details already loaded for ${_geolocationPagerItemState.value.fetchedCityName}. isLoadingDetails: false. Triggering weather fetch if current."
            )
            // Если это текущая активная страница, и погода для нее еще не загружена или была ошибка,
            // observeActivePagerItemToFetchWeather должен это обработать.
            // Можно дополнительно проверить и запустить, если нужно принудительно:
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
            // Устанавливаем состояние загрузки деталей, если еще не установлено
            if (_geolocationPagerItemState.value.isLoadingDetails == false || _geolocationPagerItemState.value.fetchedCityName == "My Location") {
                _geolocationPagerItemState.update {
                    Log.d(
                        "MainViewModel",
                        "fetchDetailsForGeolocationPage: Updating geoPagerItemState to LOADING DETAILS."
                    )
                    PagerItem.GeolocationPage( // Используем конструктор, чтобы сбросить lat/lon если нужно
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
                    locationTracker.getCurrentLocation() // Эта функция suspend? Если нет, обернуть в withContext(Dispatchers.IO) если она блокирующая

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
                        city = "Details Error" // Указываем на ошибку получения деталей
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
                            isLoadingDetails = false // <--- КЛЮЧЕВОЙ МОМЕНТ
                        )
                    }
                } else { // locationData == null
                    Log.w("MainViewModel", "fetchDetailsForGeolocationPage: locationData is NULL.")
                    _geolocationPagerItemState.update {
                        Log.w(
                            "MainViewModel",
                            "fetchDetailsForGeolocationPage: Updating geoPagerItemState with LOCATION UNKNOWN. isLoadingDetails: false"
                        )
                        it.copy( // Сохраняем предыдущие lat/lon, если они были, или оставляем 0.0
                            isLoadingDetails = false, // <--- КЛЮЧЕВОЙ МОМЕНТ
                            fetchedCityName = "Location Unknown"
                        )
                    }
                    // Также обновляем _weatherDataStateMap, чтобы показать ошибку, если геолокация не найдена
                    _weatherDataStateMap.update { currentMap ->
                        Log.w(
                            "MainViewModel",
                            "fetchDetailsForGeolocationPage: Setting weatherDataStateMap to ERROR for $geoPageId due to null locationData."
                        )
                        currentMap.toMutableMap().apply {
                            this[geoPageId] =
                                Resource.Error<WeatherDataBundle>("Could not get current geolocation (null from tracker).")
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
                        isLoadingDetails = false, // <--- КЛЮЧЕВОЙ МОМЕНТ
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
                            Resource.Error<WeatherDataBundle>("Error fetching geolocation details: ${e.message}")
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

        // Отменяем предыдущий job, если он был
        fetchWeatherJobs[itemId]?.cancel()
        Log.d("MainViewModel", "Previous fetch job for $itemId cancelled (if existed).")

        fetchWeatherJobs[itemId] = viewModelScope.launch {
            Log.d(
                "MainViewModel",
                "fetchWeatherDataForPagerItem: COROUTINE STARTED for ${pagerItem.displayName}"
            )

            // ... (встановлення Loading) ...
            val currentUnits =
                userPreferencesFlow.value.temperatureUnit // Отримуємо поточні одиниці
            val unitsQueryParam =
                if (currentUnits == TemperatureUnit.FAHRENHEIT) "imperial" else "metric"

            val result = weatherRepository.getAllWeatherData(
                lat = pagerItem.latitude,
                lon = pagerItem.longitude,
                apiKey = BuildConfig.OPEN_WEATHER_API_KEY,
                units = unitsQueryParam // <--- ПЕРЕДАЄМО ОДИНИЦІ
            )

            // Немедленно устанавливаем состояние загрузки с правильным сообщением
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
            flowOf(query) // Використовуємо flowOf для одного значення
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
        val cityName = properties?.city ?: properties?.formattedAddress ?: "Unknown Location"
        if (lat != null && lon != null) {
            viewModelScope.launch {
                val newSavedLocation = SavedLocation(
                    cityName = cityName.trim(),
                    countryCode = properties.countryCode?.uppercase(),
                    latitude = lat,
                    longitude = lon,
                    isCurrentActive = false, // Нова локація стає активною через setActiveLocation
                    orderIndex = (_savedLocationsFromDbFlow.firstOrNull()?.size
                        ?: 0) // Порядок для нової локації
                )
                val newId = weatherRepository.addSavedLocation(newSavedLocation)
                if (newId > 0L) {
                    weatherRepository.setActiveLocation(newId.toInt()) // Встановлюємо активною в БД
                    // Пейджер має оновитися через _savedLocationsFromDbFlow -> pagerItems
                    // і потім _currentPagerIndex має встановитися на цю нову сторінку
                    // Це може потребувати очікування оновлення pagerItems
                    val targetPageId = "saved_$newId"
                    pagerItems.first { items -> items.any { it.id == targetPageId } } // Чекаємо, поки з'явиться
                    val newPageIndex = pagerItems.value.indexOfFirst { it.id == targetPageId }
                    if (newPageIndex != -1) _currentPagerIndex.value = newPageIndex
                } else if (newId == -2L) { // Вже існує
                    val existing = (_savedLocationsFromDbFlow.firstOrNull()
                        ?: emptyList()).find { it.latitude == lat && it.longitude == lon }
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

    fun deleteLocationAndUpdatePager(locationId: Int) {
        viewModelScope.launch {
            val currentItemBeforeDelete = currentActivePagerItem.value
            val pageIndexOfDeleted =
                pagerItems.value.indexOfFirst { it is PagerItem.SavedPage && it.location.id == locationId }

            weatherRepository.deleteSavedLocation(locationId) // _savedLocationsFromDbFlow оновиться -> pagerItems оновиться

            // Логіка вибору нової активної сторінки після видалення
            // Чекаємо оновлення pagerItems
            val updatedPagerItems = pagerItems.first { currentItems ->
                // Чекаємо, поки видалена локація зникне зі списку PagerItems
                currentItems.none { it is PagerItem.SavedPage && it.location.id == locationId } || currentItems.isEmpty()
            }

            if (currentItemBeforeDelete is PagerItem.SavedPage && currentItemBeforeDelete.location.id == locationId) {
                // Якщо видалили поточну активну збережену сторінку
                if (updatedPagerItems.isNotEmpty()) {
                    // Встановлюємо першу сторінку (ймовірно, геолокація або перша збережена) як активну
                    Log.d("MainViewModel", "Deleted active page. Setting pager to index 0.")
                    _currentPagerIndex.value = 0
                    // Якщо перша сторінка - збережена, оновимо її isCurrentActive в БД
                    (updatedPagerItems.firstOrNull() as? PagerItem.SavedPage)?.let {
                        weatherRepository.setActiveLocation(it.location.id)
                    }
                        ?: weatherRepository.setActiveLocation(0) // Якщо геолокація, знімаємо активність з усіх
                } else {
                    // Список порожній
                    Log.d(
                        "MainViewModel",
                        "All locations deleted. Pager index to 0 (or handle empty state)."
                    )
                    _currentPagerIndex.value = 0
                    _weatherDataStateMap.value =
                        mapOf("empty_list_error" to Resource.Error<WeatherDataBundle>("No locations. Add a city."))
                }
            } else if (pageIndexOfDeleted != -1 && pageIndexOfDeleted < _currentPagerIndex.value) {
                // Якщо видалили сторінку, що була ПЕРЕД поточною активною, зсуваємо індекс
                _currentPagerIndex.value = (_currentPagerIndex.value - 1).coerceAtLeast(0)
                Log.d(
                    "MainViewModel",
                    "Deleted a page before current. Adjusted pager index to ${_currentPagerIndex.value}"
                )
            } else {
                // Видалили неактивну сторінку, поточний індекс може бути валідним
                // або потрібно перевірити, чи він не виходить за межі оновленого списку
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

    private fun hasLocationPermissionFlow(): Flow<Boolean> = flow {
        // Этот flow будет эмитить значение каждый раз, когда кто-то на него подписывается
        // или когда значение потенциально меняется. Для большей реактивности можно
        // заставить его пере-эмитить при определенных событиях, но пока оставим так.
        // Для простоты, можно сделать так, чтобы он эмитил значение при каждом вызове handlePermissionGranted
        // или при изменении состояния разрешений.
        // Но для combine, он должен эмитить последнее известное состояние.
        emit(hasLocationPermission())
    }.distinctUntilChanged() // distinctUntilChanged здесь важен

    private fun hasLocationPermission(): Boolean { // Эта функция остается
        return ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    // Оновлений setPermissionError
    fun setPermissionError(message: String) {
        val errorKey = currentActivePagerItem.value?.id
            ?: PagerItem.GeolocationPage().id // Використовуємо ID поточної сторінки або геолокації

        _weatherDataStateMap.update { currentMap ->
            currentMap.toMutableMap().apply {
                this[errorKey] = Resource.Error<WeatherDataBundle>(message = message)
            }.toMap()
        }

        // Якщо це помилка для геолокаційної сторінки, оновимо її стан
        if (errorKey == PagerItem.GeolocationPage().id && message.contains(
                "permission",
                ignoreCase = true
            )
        ) {
            _geolocationPagerItemState.update {
                it.copy(
                    isLoadingDetails = false, // Важливо скинути isLoadingDetails при помилці дозволів
                    fetchedCityName = if (message.contains("permanently denied")) "Permission Denied (Settings)" else "Permission Denied"
                )
            }
        }
        // Блок для _hasLocationPermissionState.value = false удален, так как _hasLocationPermissionState больше нет.
        // hasLocationPermissionFlow() должен сам эмитить false, если разрешения нет.
        Log.e("MainViewModel", "Permission error set for $errorKey: $message")
    }

    // Оновлений handlePermissionGranted
    fun handlePermissionGranted() {
        Log.d("MainViewModel", "handlePermissionGranted called by UI.")

        val geoPageId = PagerItem.GeolocationPage().id

        // 1. СРАЗУ обновить состояние геолокационной страницы на "загрузка деталей".
        // Это критически важно сделать ДО того, как _permissionCheckTrigger заставит pagerItems
        // пересобраться и currentActivePagerItem среагирует.
        _geolocationPagerItemState.update {
            Log.d(
                "MainViewModel",
                "handlePermissionGranted: Updating _geolocationPagerItemState to initial loading state (isLoadingDetails=true)."
            )
            // Используем конструктор PagerItem.GeolocationPage, чтобы сбросить все поля к начальным для загрузки
            PagerItem.GeolocationPage(
                isLoadingDetails = true,
                fetchedCityName = "Loading location...",
                lat = 0.0, // Сбрасываем, т.к. будем получать новые
                lon = 0.0, // Сбрасываем
                fetchedCountryCode = null
            )
        }
        Log.d(
            "MainViewModel",
            "Updated _geolocationPagerItemState to initial loading state due to permission grant."
        )

        // 2. "Пинаем" триггер, чтобы combine (pagerItems) пересчитался с новым состоянием разрешения
        // и с УЖЕ обновленным _geolocationPagerItemState (в котором isLoadingDetails=true).
        viewModelScope.launch {
            Log.d(
                "MainViewModel",
                "Emitting to _permissionCheckTrigger from handlePermissionGranted"
            )
            _permissionCheckTrigger.tryEmit(Unit)
        }

        // 3. Очищаем предыдущую ошибку (если была) и устанавливаем состояние загрузки в _weatherDataStateMap.
        // Это гарантирует, что WeatherScreen не подхватит старое сообщение "permission denied"
        // и покажет индикатор загрузки.
        _weatherDataStateMap.update { currentMap ->
            val mutableMap = currentMap.toMutableMap()
            Log.d(
                "MainViewModel",
                "Setting _weatherDataStateMap for $geoPageId to Loading (permissions granted)."
            )
            // Устанавливаем состояние загрузки, чтобы UI немедленно отреагировал
            mutableMap[geoPageId] =
                Resource.Loading(message = "Permissions granted. Fetching location data...")
            mutableMap.toMap()
        }
        Log.d("MainViewModel", "_weatherDataStateMap for $geoPageId set to Loading.")

        // 4. Теперь (пере)запрашиваем детали для геолокационной страницы.
        // Эта функция асинхронно получит lat/lon, обновит _geolocationPagerItemState (isLoadingDetails станет false),
        // что в свою очередь триггернет observeActivePagerItemToFetchWeather для загрузки погоды
        // уже с правильными координатами и состоянием.
        fetchDetailsForGeolocationPage()
    }

    private fun tryInitialSetupOrLoadActive() {
        viewModelScope.launch {
            Log.d("MainViewModel", "tryInitialSetupOrLoadActive called.")
            // Также "пинаем" триггер при инициализации
            Log.d(
                "MainViewModel",
                "Emitting to _permissionCheckTrigger from tryInitialSetupOrLoadActive"
            )
            _permissionCheckTrigger.tryEmit(Unit)

            val initialPermission = hasLocationPermission()
            Log.d(
                "MainViewModel",
                "tryInitialSetupOrLoadActive: Initial permission state: $initialPermission"
            )

            val stablePagerItems = pagerItems.first { items ->
                val permissionGrantedNow = hasLocationPermission()
                if (permissionGrantedNow) {
                    items.any { it is PagerItem.GeolocationPage } || items.any { it is PagerItem.SavedPage }
                } else {
                    true
                }
            }
            // val stablePagerItems = pagerItems.value // Або просто беремо поточне значення після невеликої затримки, якщо .first {} занадто блокує

            val savedFromDb =
                _savedLocationsFromDbFlow.first() // Це має бути досить швидко, оскільки StateFlow

            Log.d(
                "MainViewModel",
                "InitialSetup: Saved in DB: ${savedFromDb.size}, Stable PagerItems: ${stablePagerItems.size}, HasPermission: ${hasLocationPermission()}"
            )

            val activeInDb = savedFromDb.find { it.isCurrentActive }

            var targetIndexToSet: Int? = null

            if (hasLocationPermission()) {
                val geoPageIndex = stablePagerItems.indexOfFirst { it is PagerItem.GeolocationPage }
                if (geoPageIndex != -1) {
                    Log.d(
                        "MainViewModel",
                        "InitialSetup: Has permission. Setting Geolocation (index $geoPageIndex) as current page."
                    )
                    targetIndexToSet = geoPageIndex
                    val geoPageState = _geolocationPagerItemState.value
                    if (geoPageState.isLoadingDetails || geoPageState.fetchedCityName == "My Location" || geoPageState.fetchedCityName == "Loading location...") {
                        fetchDetailsForGeolocationPage()
                    }
                } else {
                    Log.w(
                        "MainViewModel",
                        "InitialSetup: Has permission, but GeolocationPage not found in stablePagerItems. Fallback."
                    )
                    if (activeInDb != null) {
                        val activeIndex =
                            stablePagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.id == activeInDb.id }
                        if (activeIndex != -1) targetIndexToSet = activeIndex
                    }
                    if (targetIndexToSet == null && savedFromDb.isNotEmpty()) {
                        val firstSavedIndex =
                            stablePagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.id == savedFromDb.first().id }
                        if (firstSavedIndex != -1) {
                            targetIndexToSet = firstSavedIndex
                            // Встановлюємо першу збережену активною в БД, якщо не було активної і геолокація не завантажилась
                            if (activeInDb == null) weatherRepository.setActiveLocation(savedFromDb.first().id)
                        }
                    }
                }
            } else if (activeInDb != null) {
                val activeIndex =
                    stablePagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.id == activeInDb.id }
                if (activeIndex != -1) {
                    Log.d(
                        "MainViewModel",
                        "InitialSetup: No permission, using active from DB: ${activeInDb.cityName} at index $activeIndex."
                    )
                    targetIndexToSet = activeIndex
                } else {
                    Log.w(
                        "MainViewModel",
                        "InitialSetup: Active from DB not found in stablePagerItems. Fallback to first saved."
                    )
                    if (savedFromDb.isNotEmpty()) {
                        val firstSavedIndex =
                            stablePagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.id == savedFromDb.first().id }
                        if (firstSavedIndex != -1) targetIndexToSet = firstSavedIndex
                    }
                }
            } else if (savedFromDb.isNotEmpty()) {
                Log.d(
                    "MainViewModel",
                    "InitialSetup: No permission, no active in DB, using first saved: ${savedFromDb.first().cityName}"
                )
                val firstSavedIndex =
                    stablePagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.id == savedFromDb.first().id }
                if (firstSavedIndex != -1) {
                    targetIndexToSet = firstSavedIndex
                    weatherRepository.setActiveLocation(savedFromDb.first().id)
                }
            }

            if (targetIndexToSet != null) {
                if (_currentPagerIndex.value != targetIndexToSet) {
                    _currentPagerIndex.value = targetIndexToSet
                }
            } else if (!hasLocationPermission() && savedFromDb.isEmpty()) {
                Log.d(
                    "MainViewModel",
                    "InitialSetup: No saved locations and no permission. Setting permission error."
                )
                // Перевіряємо, чи вже є помилка, щоб не перезаписувати
                val geoPageId = PagerItem.GeolocationPage().id
                if (_weatherDataStateMap.value[geoPageId] !is Resource.Error || _weatherDataStateMap.value[geoPageId]?.message?.contains(
                        "permission"
                    ) != true
                ) {
                    setPermissionError("Location permission is required to show weather. Please grant permission or add a city.")
                }
            } else {
                Log.w(
                    "MainViewModel",
                    "InitialSetup: Could not determine a target page index. Current list size: ${stablePagerItems.size}"
                )
                // Якщо список не порожній, але індекс не встановлено, можливо, встановити 0 за замовчуванням, якщо є елементи.
                if (stablePagerItems.isNotEmpty() && _currentPagerIndex.value >= stablePagerItems.size) {
                    _currentPagerIndex.value = 0
                }
            }
        }
    }

    // ЦЕЙ МЕТОД МАЄ БУТИ PUBLIC (без private)
    fun setCurrentPagerItemToSavedLocation(savedLocation: SavedLocation) {
        viewModelScope.launch {
            Log.d(
                "MainViewModel",
                "User selected saved location: ${savedLocation.cityName} from list. Setting active in DB."
            )
            // Этот метод вызывается из ManageCitiesScreen. Он должен сделать город активным.
            // При следующем запуске determineInitialPage все равно отдаст приоритет геолокации.
            weatherRepository.setActiveLocation(savedLocation.id)

            // Ожидаем обновления pagerItems и находим индекс новой активной страницы
            try {
                // Ждем, пока pagerItems обновится и будет содержать выбранную локацию,
                // и она будет помечена как isCurrentActive в данных из БД (если это отражается в PagerItem)
                // или просто ждем появления элемента с нужным ID.
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
                        // Если индекс уже тот, но погода не загружена (маловероятно после добавления/выбора)
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
                if (currentItem is PagerItem.GeolocationPage) {
                    Log.d(
                        "MainViewModel",
                        "Refresh: Handling GeolocationPage. Setting weather state to Loading for $itemId before fetching details."
                    )
                    // <<< ИЗМЕНЕНИЕ ЗДЕСЬ >>>
                    // Принудительно устанавливаем состояние Resource.Loading для геолокационной страницы.
                    // Это гарантирует, что последующее ожидание в .first { ... } будет ждать
                    // завершения НОВОГО цикла загрузки погоды, инициированного fetchDetailsForGeolocationPage -> observeActivePagerItemToFetchWeather.
                    _weatherDataStateMap.update { currentMap ->
                        currentMap.toMutableMap().apply {
                            this[itemId] =
                                Resource.Loading(message = "Refreshing location and weather...")
                        }.toMap()
                    }
                    // Теперь вызываем fetchDetailsForGeolocationPage.
                    // Она обновит детали, а изменение isLoadingDetails в _geolocationPagerItemState
                    // должно через observeActivePagerItemToFetchWeather запустить fetchWeatherDataForPagerItem,
                    // которая снова установит Loading, а затем Success/Error.
                    fetchDetailsForGeolocationPage()
                } else { // Для сохраненной страницы (PagerItem.SavedPage)
                    Log.d(
                        "MainViewModel",
                        "Refresh: Handling SavedPage. Calling fetchWeatherDataForPagerItem for ${itemId}."
                    )
                    // fetchWeatherDataForPagerItem сама установит Resource.Loading в начале своей работы.
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
            } finally {
                _isRefreshing.value = false
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
            // Після зміни одиниць, потрібно перезавантажити погоду для поточної сторінки пейджера,
            // оскільки API має повернути дані в нових одиницях (або ми маємо їх конвертувати)
            currentActivePagerItem.value?.let {
                Log.d(
                    "MainViewModel",
                    "Temperature unit changed, refreshing weather for ${it.displayName}"
                )
                // Встановлюємо стан завантаження для поточної сторінки, щоб показати індикатор
                _weatherDataStateMap.update { currentMap ->
                    currentMap.toMutableMap().apply {
                        this[it.id] = Resource.Loading(message = "Updating units...")
                    }.toMap()
                }
                // Повторний запит погоди
                fetchWeatherDataForPagerItem(it) // Цей метод має враховувати поточні одиниці
            }
        }
    }
}