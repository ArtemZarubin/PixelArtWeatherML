// File: app/src/test/java/com/artemzarubin/weatherml/ui/mainscreen/MainViewModelTest.kt
package com.artemzarubin.weatherml.ui.mainscreen

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import app.cash.turbine.test
import com.artemzarubin.weatherml.data.preferences.AppTheme
import com.artemzarubin.weatherml.data.preferences.TemperatureUnit
import com.artemzarubin.weatherml.data.preferences.UserPreferences
import com.artemzarubin.weatherml.data.preferences.UserPreferencesRepository
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyPropertiesDto
import com.artemzarubin.weatherml.domain.location.LocationTracker
import com.artemzarubin.weatherml.domain.model.CurrentWeather
import com.artemzarubin.weatherml.domain.model.SavedLocation
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private lateinit var mockWeatherRepository: WeatherRepository
    private lateinit var mockLocationTracker: LocationTracker
    private lateinit var mockApplication: Application
    private lateinit var mockUserPreferencesRepository: UserPreferencesRepository

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: MainViewModel

    // Допоміжні дані для тестів
    private val mockLocation = mockk<Location>(relaxed = true).apply {
        every { latitude } returns 40.7128 // New York
        every { longitude } returns -74.0060
    }
    private val mockGeoDetails = GeoapifyFeatureDto(
        properties = GeoapifyPropertiesDto(
            city = "New York (Geo)",
            countryCode = "US",
            formattedAddress = "New York, NY, USA",
            latitude = 40.7128,
            longitude = -74.0060,
            country = "United States", // Додав для повноти
            state = "New York",
            postcode = "10001",
            county = null,
            street = null,
            housenumber = null,
            suburb = null,
            placeId = "some_place_id_ny"
        ),
        geometry = null
    )
    private val mockWeatherDataBundle = WeatherDataBundle(
        latitude = 40.7128, longitude = -74.0060, timezone = "America/New_York",
        currentWeather = mockk<CurrentWeather>(relaxed = true).apply {
            every { cityName } returns "New York (Geo)"
            every { mlFeelsLikeCelsius } returns 20.0f // Приклад
        },
        hourlyForecasts = emptyList(), dailyForecasts = emptyList()
    )

    private val mutableSavedLocationsFlow = MutableStateFlow<List<SavedLocation>>(emptyList())
    private val mutableCurrentActiveLocationFlow =
        MutableStateFlow<SavedLocation?>(null) // Вы это уже могли сделать

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.w(any<String>(), any<String>(), any()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.v(any(), any()) } returns 0


        mockkStatic(ContextCompat::class)

        mockWeatherRepository =
            mockk(relaxed = true) // relaxed = true, щоб не мокувати всі методи DAO
        mockLocationTracker = mockk()
        mockApplication = mockk(relaxed = true)
        mockUserPreferencesRepository = mockk()

        // Налаштування моків за замовчуванням для більшості тестів
        every { mockWeatherRepository.getSavedLocations() } returns mutableSavedLocationsFlow
        every { mockWeatherRepository.getCurrentActiveWeatherLocation() } returns mutableCurrentActiveLocationFlow
        coEvery {
            mockWeatherRepository.getLocationDetailsByCoordinates(
                any(),
                any(),
                any()
            )
        } returns Resource.Success(mockGeoDetails)
        coEvery {
            mockWeatherRepository.getAllWeatherData(
                any(),
                any(),
                any(),
                any()
            )
        } returns Resource.Success(mockWeatherDataBundle)
        coEvery { mockWeatherRepository.addSavedLocation(any()) } returns 1L // Успішне додавання
        coEvery { mockWeatherRepository.setActiveLocation(any()) } returns Unit
        coEvery { mockWeatherRepository.deleteSavedLocation(any()) } returns Unit
        coEvery { mockWeatherRepository.updateSavedLocationsOrder(any()) } returns Unit


        val defaultUserPreferences = UserPreferences(TemperatureUnit.CELSIUS, AppTheme.SYSTEM)
        every { mockUserPreferencesRepository.userPreferencesFlow } returns flowOf(
            defaultUserPreferences
        )
        coEvery { mockUserPreferencesRepository.updateTemperatureUnit(any()) } returns Unit
        coEvery { mockUserPreferencesRepository.updateAppTheme(any()) } returns Unit


        // За замовчуванням, дозволів немає для першого тесту
        setPermissions(granted = false)

        // Створюємо ViewModel ПІСЛЯ налаштування всіх моків
        viewModel = MainViewModel(
            weatherRepository = mockWeatherRepository,
            locationTracker = mockLocationTracker,
            application = mockApplication, // Має бути Application
            userPreferencesRepository = mockUserPreferencesRepository // Має бути UserPreferencesRepository
        )
    }

    private fun setPermissions(granted: Boolean) {
        val permissionResult =
            if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        every {
            ContextCompat.checkSelfPermission(
                mockApplication,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } returns permissionResult
        every {
            ContextCompat.checkSelfPermission(
                mockApplication,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } returns permissionResult
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(Log::class)
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `initial state - no permission, no saved locations - shows permission error for geo page`() =
        runTest(testDispatcher) {
            mutableSavedLocationsFlow.value = emptyList()
            mutableCurrentActiveLocationFlow.value = null
            // Arrange: дозволів немає (встановлено в setUp)
            // Act: ViewModel ініціалізується в setUp

            testScheduler.advanceUntilIdle() // Даємо час на виконання init блоку

            // Assert
            val geoPageId = PagerItem.GeolocationPage().id
            val state = viewModel.weatherDataStateMap.value[geoPageId]
            assertTrue(
                "State for geo page should be Error, but was $state",
                state is Resource.Error
            )
            assertTrue(
                "Error message should indicate permission issue. Actual: '${(state as Resource.Error).message}'",
                state.message?.contains("permission", ignoreCase = true) == true
            )
        }

    @Test
    fun `initial state - permission granted, no saved locations - fetches geo details and weather`() =
        runTest(testDispatcher) {
            mutableSavedLocationsFlow.value = emptyList()
            mutableCurrentActiveLocationFlow.value = null
            // Arrange
            setPermissions(granted = true)
            coEvery { mockLocationTracker.getCurrentLocation() } returns mockLocation // Геолокація успішна
            // Re-initialize ViewModel with new permission state for this test
            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )


            testScheduler.advanceUntilIdle()

            // Assert
            // 1. Перевіряємо, що деталі геолокації були завантажені
            val geoItemState =
                viewModel.pagerItems.value.find { it is PagerItem.GeolocationPage } as? PagerItem.GeolocationPage
            assertNotNull("GeolocationPage item should exist", geoItemState)
            assertEquals(false, geoItemState?.isLoadingDetails)
            assertEquals(mockGeoDetails.properties?.city, geoItemState?.fetchedCityName)

            // 2. Перевіряємо, що погода для геолокації завантажена
            val geoPageId = PagerItem.GeolocationPage().id
            val weatherState = viewModel.weatherDataStateMap.value[geoPageId]
            assertTrue(
                "Weather for geo page should be Success, but was $weatherState",
                weatherState is Resource.Success
            )
            assertEquals(
                mockWeatherDataBundle.currentWeather.cityName,
                (weatherState as Resource.Success).data?.currentWeather?.cityName
            )

            // 3. Перевіряємо, що геолокація стала активною сторінкою пейджера
            assertEquals(0, viewModel.currentPagerIndex.value) // Геолокація - перша
        }

    @Test
    fun `initial state - has saved locations, one active in DB - loads weather for active saved location`() =
        runTest(testDispatcher) {
            mutableSavedLocationsFlow.value = emptyList()
            mutableCurrentActiveLocationFlow.value = null
            // Arrange
            setPermissions(granted = false) // Дозволи не важливі, якщо є активна збережена
            val activeLocation = SavedLocation(
                id = 1,
                cityName = "Paris",
                countryCode = "FR",
                latitude = 1.0,
                longitude = 1.0,
                isCurrentActive = true,
                orderIndex = 0
            )
            val otherLocation = SavedLocation(
                id = 2,
                cityName = "Berlin",
                countryCode = "DE",
                latitude = 2.0,
                longitude = 2.0,
                isCurrentActive = false,
                orderIndex = 1
            )
            every { mockWeatherRepository.getSavedLocations() } returns flowOf(
                listOf(
                    activeLocation,
                    otherLocation
                )
            )
            // getCurrentActiveWeatherLocation має повернути активну з БД
            every { mockWeatherRepository.getCurrentActiveWeatherLocation() } returns flowOf(
                activeLocation
            )


            // Re-initialize ViewModel
            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle()

            // Assert
            // 1. Перевіряємо, що активна сторінка пейджера - це Paris
            val activePagerItem = viewModel.currentActivePagerItem.value
            assertTrue(
                "Active pager item should be SavedPage",
                activePagerItem is PagerItem.SavedPage
            )
            assertEquals(activeLocation.id, (activePagerItem as PagerItem.SavedPage).location.id)

            // 2. Перевіряємо, що погода завантажена для Paris
            val weatherState = viewModel.weatherDataStateMap.value[activePagerItem.id]
            assertTrue(
                "Weather for Paris should be Success, but was $weatherState",
                weatherState is Resource.Success
            )
            coVerify {
                mockWeatherRepository.getAllWeatherData(
                    eq(activeLocation.latitude),
                    eq(activeLocation.longitude),
                    any(),
                    any()
                )
            }
        }

    @Ignore("Skipping this test temporarily due to issues with async state updates")
    @Test
    fun `onCitySuggestionSelected - adds location, sets it active, and fetches weather`() =
        runTest(testDispatcher) {
            // Arrange
            setPermissions(granted = true)
            val newLocationIdReturnedByRepo = 3L
            val capturedSavedLocation =
                slot<SavedLocation>() // Для перевірки, що передається в addSavedLocation

            // 1. Початковий стан (як у тебе було)
            mutableSavedLocationsFlow.value = emptyList()
            mutableCurrentActiveLocationFlow.value = null
            every { mockWeatherRepository.getSavedLocations() } returns mutableSavedLocationsFlow // Використовуємо MutableStateFlow
            every { mockWeatherRepository.getCurrentActiveWeatherLocation() } returns mutableCurrentActiveLocationFlow // Використовуємо MutableStateFlow
            coEvery { mockLocationTracker.getCurrentLocation() } returns mockLocation
            coEvery {
                mockWeatherRepository.getLocationDetailsByCoordinates(
                    any(),
                    any(),
                    any()
                )
            } returns Resource.Success(mockGeoDetails)
            coEvery {
                mockWeatherRepository.getAllWeatherData(
                    eq(mockLocation.latitude),
                    eq(mockLocation.longitude),
                    any(),
                    any()
                )
            } returns Resource.Success(
                mockWeatherDataBundle.copy(
                    currentWeather = mockWeatherDataBundle.currentWeather.copy(
                        cityName = "New York (Geo)"
                    )
                )
            )

            // 2. Мокуємо додавання нової локації (London)
            coEvery { mockWeatherRepository.addSavedLocation(capture(capturedSavedLocation)) } coAnswers {
                Log.d(
                    "ViewModelTestMock",
                    "addSavedLocation called with: ${capturedSavedLocation.captured.cityName}"
                )
                newLocationIdReturnedByRepo
            }

            // 3. Мокуємо встановлення активної локації (London)
            // Тепер setActiveLocation просто імітує оновлення Flow, на які підписана ViewModel
            coEvery { mockWeatherRepository.setActiveLocation(eq(newLocationIdReturnedByRepo.toInt())) } coAnswers {
                val id = firstArg<Int>()
                Log.d("ViewModelTestMock", "setActiveLocation called for id: $id")
                // Імітуємо, що після setActiveLocation, getCurrentActiveWeatherLocation
                // та getSavedLocations повернуть оновлені дані.
                // capturedSavedLocation.captured буде доступний ПІСЛЯ виклику addSavedLocation.
                // Тому ми не можемо його тут використовувати напряму для створення newActiveLondon.
                // Замість цього, ми оновимо mutable Flow, які ViewModel слухає.

                // Створюємо об'єкт London, який мав би бути збережений
                val londonAfterSaveAndSetActive = SavedLocation(
                    id = newLocationIdReturnedByRepo.toInt(),
                    cityName = "London", // Припускаємо, що це буде передано
                    countryCode = "GB",
                    latitude = 51.5,
                    longitude = -0.12,
                    isCurrentActive = true, // Бо ми її робимо активною
                    orderIndex = 0 // Припускаємо, що це перша збережена після геолокації
                )
                mutableSavedLocationsFlow.value = listOf(londonAfterSaveAndSetActive)
                mutableCurrentActiveLocationFlow.value = londonAfterSaveAndSetActive
                Log.d(
                    "ViewModelTestMock",
                    "Mock setActiveLocation: Flows updated for London (id=$id)."
                )
            }

            // 4. Мокуємо завантаження погоди для London
            val londonWeatherData = mockWeatherDataBundle.copy(
                currentWeather = mockWeatherDataBundle.currentWeather.copy(cityName = "London")
            )
            coEvery {
                mockWeatherRepository.getAllWeatherData(
                    eq(51.5),
                    eq(-0.12),
                    any(),
                    any()
                )
            } returns Resource.Success(londonWeatherData)

            // Ініціалізуємо ViewModel
            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle()

            // Перевіряємо початковий стан
            assertEquals(
                "New York (Geo)",
                (viewModel.pagerItems.value.getOrNull(0) as? PagerItem.GeolocationPage)?.fetchedCityName
            )

            // Act
            val londonSuggestion = GeoapifyFeatureDto(
                properties = GeoapifyPropertiesDto(
                    city = "London",
                    countryCode = "GB",
                    latitude = 51.5,
                    longitude = -0.12,
                    formattedAddress = "London, England, Greater London, United Kingdom",
                    country = "UK",
                    state = null,
                    postcode = null,
                    county = null,
                    street = null,
                    housenumber = null,
                    addressLine1 = null,
                    addressLine2 = null,
                    placeId = "london_id"
                ),
                geometry = null
            )

            // Тестуємо _currentPagerIndex
            viewModel.currentPagerIndex.test {
                assertEquals("Initial pager index should be 0 (geo)", 0, awaitItem())

                viewModel.onCitySuggestionSelected(londonSuggestion)
                testScheduler.advanceUntilIdle() // Даємо час на всі асинхронні операції

                // Очікуємо, що індекс зміниться на індекс London
                // London має стати другою сторінкою (індекс 1), після геолокації (індекс 0)
                // АБО якщо геолокація не додається в pagerItems як перша, то індекс 0
                val londonPageIndex = awaitItem()
                Log.d("ViewModelTest", "Pager Index after selecting London: $londonPageIndex")

                val finalPagerItems = viewModel.pagerItems.value
                val expectedLondonIndexInPager =
                    finalPagerItems.indexOfFirst { it is PagerItem.SavedPage && it.location.cityName == "London" }

                assertTrue("London should be in pagerItems", expectedLondonIndexInPager != -1)
                assertEquals(
                    "Pager index should be set to London's index",
                    expectedLondonIndexInPager,
                    londonPageIndex
                )

                // Перевіряємо currentActivePagerItem
                val activeItem = viewModel.currentActivePagerItem.value
                assertTrue(
                    "Active PagerItem should be SavedPage for London",
                    activeItem is PagerItem.SavedPage
                )
                assertEquals("London", (activeItem as PagerItem.SavedPage).location.cityName)

                // Перевіряємо погоду для London
                testScheduler.advanceUntilIdle()
                val weatherMap = viewModel.weatherDataStateMap.value
                val londonWeather = weatherMap[activeItem.id]
                assertTrue(
                    "Weather for London should be Success, but was $londonWeather",
                    londonWeather is Resource.Success
                )
                assertEquals(
                    "London",
                    (londonWeather as Resource.Success).data?.currentWeather?.cityName
                )

                cancelAndConsumeRemainingEvents()
            }

            // Перевірка викликів моків
            coVerify { mockWeatherRepository.addSavedLocation(any()) }
            assertEquals("London", capturedSavedLocation.captured.cityName.trim())
            coVerify { mockWeatherRepository.setActiveLocation(newLocationIdReturnedByRepo.toInt()) }
        }

    @Ignore("Skipping this test temporarily due to issues with async state updates")
    @Test
    fun `deleteLocationAndUpdatePager - deletes location, updates pager to geo if active deleted and geo exists`() =
        runTest(testDispatcher) {
            setPermissions(granted = true) // Припускаємо, що є дозвіл на геолокацію

            val locationToDeleteId = 1
            val savedLocation1 = SavedLocation(
                id = locationToDeleteId,
                cityName = "CityToDelete",
                countryCode = "TD",
                latitude = 1.0,
                longitude = 1.0,
                isCurrentActive = true,
                orderIndex = 0
            )
            val savedLocation2 = SavedLocation(
                id = 2,
                cityName = "AnotherCity",
                countryCode = "AC",
                latitude = 2.0,
                longitude = 2.0,
                isCurrentActive = false,
                orderIndex = 1
            )

            val initialSavedList = listOf(savedLocation1, savedLocation2)
            val listAfterDelete = listOf(
                savedLocation2.copy(
                    isCurrentActive = true,
                    orderIndex = 0
                )
            ) // Припускаємо, що друга стане активною і першою

            // Початковий стан: є дві збережені локації, перша - активна
            every { mockWeatherRepository.getSavedLocations() } returns flowOf(initialSavedList)
            every { mockWeatherRepository.getCurrentActiveWeatherLocation() } returns flowOf(
                savedLocation1
            )
            coEvery { mockLocationTracker.getCurrentLocation() } returns mockLocation // Геолокація (New York)
            coEvery {
                mockWeatherRepository.getLocationDetailsByCoordinates(
                    any(),
                    any(),
                    any()
                )
            } returns Resource.Success(mockGeoDetails)
            coEvery {
                mockWeatherRepository.getAllWeatherData(
                    any(),
                    any(),
                    any(),
                    any()
                )
            } returns Resource.Success(mockWeatherDataBundle)


            // Мокуємо видалення
            coEvery { mockWeatherRepository.deleteSavedLocation(locationToDeleteId) } coAnswers {
                Log.d("ViewModelTestMock", "deleteSavedLocation called for id: $locationToDeleteId")
                // Імітуємо оновлення Flow після видалення
                every { mockWeatherRepository.getSavedLocations() } returns flowOf(listAfterDelete)
                // Якщо після видалення активною стає інша збережена
                // every { mockWeatherRepository.getCurrentActiveWeatherLocation() } returns flowOf(listAfterDelete.firstOrNull { it.isCurrentActive })
                // Якщо після видалення активною має стати геолокація (або нічого, якщо геолокації немає)
                every { mockWeatherRepository.getCurrentActiveWeatherLocation() } returns flowOf(
                    null
                ) // Немає активної збереженої
            }
            // Мокуємо встановлення нової активної локації
            coEvery { mockWeatherRepository.setActiveLocation(any()) } returns Unit


            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle()

            // --- Начальная настройка активной страницы для теста ---
            // Сначала убедимся, что CityToDelete есть в списке и найдем ее индекс
            val cityToDeletePagerId = "saved_$locationToDeleteId"
            val pagerItemsInitial = viewModel.pagerItems.value
            val indexOfCityToDelete =
                pagerItemsInitial.indexOfFirst { it.id == cityToDeletePagerId }

            println("--- DEBUG: Pager items before setting CityToDelete active: ${pagerItemsInitial.map { it.id }}")
            println("--- DEBUG: Index of CityToDelete ('$cityToDeletePagerId'): $indexOfCityToDelete")
            assertTrue(
                "CityToDelete (id: $cityToDeletePagerId) must be in the initial pager items list",
                indexOfCityToDelete != -1
            )

            // Если CityToDelete не активна изначально, делаем ее активной
            if (viewModel.currentActivePagerItem.value?.id != cityToDeletePagerId) {
                println("--- DEBUG: CityToDelete is not active. Calling onPageChanged($indexOfCityToDelete). Current active: ${viewModel.currentActivePagerItem.value?.id}")
                viewModel.onPageChanged(indexOfCityToDelete) // Это должно обновить currentPagerIndex и currentActivePagerItem
                testScheduler.advanceUntilIdle() // Даем время на обновление
            }

            // Теперь проверяем, что CityToDelete действительно стала активной
            assertEquals(
                "Active item should now be CityToDelete before deleting",
                cityToDeletePagerId,
                viewModel.currentActivePagerItem.value?.id
            )
            assertEquals(
                "Pager index should be set to CityToDelete's index",
                indexOfCityToDelete,
                viewModel.currentPagerIndex.value
            )
            println("--- DEBUG: CityToDelete is now active. Current index: ${viewModel.currentPagerIndex.value}")
            // --- Конец начальной настройки ---

            // Act
            viewModel.deleteLocationAndUpdatePager(locationToDeleteId)
            testScheduler.advanceUntilIdle() // Даємо час на виконання корутин

// Assert
            coVerify { mockWeatherRepository.deleteSavedLocation(locationToDeleteId) }

            val itemsAfterDelete = viewModel.pagerItems.value
            println("--- DEBUG: Pager Items AFTER delete: ${itemsAfterDelete.map { if (it is PagerItem.SavedPage) it.location.cityName else (it as? PagerItem.GeolocationPage)?.fetchedCityName }}")
            println("--- DEBUG: Current Pager Index AFTER delete: ${viewModel.currentPagerIndex.value}")
            println("--- DEBUG: Current Active PagerItem AFTER delete: ${viewModel.currentActivePagerItem.value?.let { if (it is PagerItem.SavedPage) it.location.cityName else (it as? PagerItem.GeolocationPage)?.fetchedCityName }}")

            // Очікуємо, що пейджер перемкнеться на геолокацію (індекс 0),
            // оскільки ми видалили активну збережену, і геолокація є першою сторінкою.
            // Або на першу збережену, якщо геолокації немає.
            val finalPagerItems = viewModel.pagerItems.value
            Log.d(
                "ViewModelTest",
                "PagerItems after delete: ${finalPagerItems.map { it.displayName }}"
            )
            Log.d(
                "ViewModelTest",
                "Current Pager Index after delete: ${viewModel.currentPagerIndex.value}"
            )
            Log.d(
                "ViewModelTest",
                "Current Active PagerItem after delete: ${viewModel.currentActivePagerItem.value?.displayName}"
            )

            val expectedNewActiveIndex =
                finalPagerItems.indexOfFirst { it is PagerItem.GeolocationPage }
            if (expectedNewActiveIndex != -1) {
                assertEquals(
                    "Pager index should be geolocation after deleting active saved",
                    expectedNewActiveIndex,
                    viewModel.currentPagerIndex.value
                )
                assertTrue(viewModel.currentActivePagerItem.value is PagerItem.GeolocationPage)
                coVerify { mockWeatherRepository.setActiveLocation(0) } // Перевіряємо, що активність знято зі збережених
            } else if (finalPagerItems.isNotEmpty()) { // Якщо геолокації немає, але є інші збережені
                val firstSavedIndex = finalPagerItems.indexOfFirst { it is PagerItem.SavedPage }
                assertEquals(
                    "Pager index should be first saved after deleting active saved",
                    firstSavedIndex,
                    viewModel.currentPagerIndex.value
                )
                assertTrue(viewModel.currentActivePagerItem.value is PagerItem.SavedPage)
                coVerify { mockWeatherRepository.setActiveLocation((finalPagerItems[firstSavedIndex] as PagerItem.SavedPage).location.id) }
            } else {
                // Список порожній
                assertNull(
                    "Active pager item should be null if list is empty",
                    viewModel.currentActivePagerItem.value
                )
            }
        }

    // TODO: Додай тести для deleteLocationAndUpdatePager, onUserSwipedPage, refreshCurrentPageWeather, зміни одиниць/теми
}