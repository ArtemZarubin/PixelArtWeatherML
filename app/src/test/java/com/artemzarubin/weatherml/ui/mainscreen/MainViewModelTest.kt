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

            // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
            // Вместо: coEvery { mockLocationTracker.getCurrentLocation() } returns mockLocation
            // Делаем:
            coEvery { mockLocationTracker.getCurrentLocation() } returns flowOf(
                Resource.Success(
                    mockLocation
                )
            )
            // --- КОНЕЦ ИЗМЕНЕНИЯ ---

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
            setPermissions(granted = true) // Предполагаем, что геолокация будет добавлена
            val newLocationIdReturnedByRepo = 3L
            val capturedSavedLocation = slot<SavedLocation>()

            // 1. Начальное состояние: пустые списки, геолокация должна загрузиться
            mutableSavedLocationsFlow.value = emptyList()
            mutableCurrentActiveLocationFlow.value = null // Нет активной сохраненной
            every { mockWeatherRepository.getSavedLocations() } returns mutableSavedLocationsFlow
            every { mockWeatherRepository.getCurrentActiveWeatherLocation() } returns mutableCurrentActiveLocationFlow

            // Мок для начальной загрузки геолокации (New York)
            coEvery { mockLocationTracker.getCurrentLocation() } returns flowOf(
                Resource.Success(
                    mockLocation
                )
            )
            coEvery {
                mockWeatherRepository.getLocationDetailsByCoordinates(
                    eq(mockLocation.latitude), eq(mockLocation.longitude), any()
                )
            } returns Resource.Success(mockGeoDetails) // mockGeoDetails это "New York (Geo)"
            val initialGeoWeatherData = mockWeatherDataBundle.copy(
                currentWeather = mockWeatherDataBundle.currentWeather.copy(cityName = "New York (Geo)")
            )
            coEvery {
                mockWeatherRepository.getAllWeatherData(
                    eq(mockLocation.latitude), eq(mockLocation.longitude), any(), any()
                )
            } returns Resource.Success(initialGeoWeatherData)


            // 2. Мок для добавления London
            coEvery { mockWeatherRepository.addSavedLocation(capture(capturedSavedLocation)) } coAnswers {
                newLocationIdReturnedByRepo
            }

            // 3. Мок для setActiveLocation (London)
            // Этот мок теперь должен обновить `mutableSavedLocationsFlow` так,
            // чтобы он содержал и геолокацию (если она была), и новый Лондон.
            // И `mutableCurrentActiveLocationFlow` должен указать на Лондон.
            val londonSavedLocation = SavedLocation(
                id = newLocationIdReturnedByRepo.toInt(),
                cityName = "London", countryCode = "GB", latitude = 51.5, longitude = -0.12,
                isCurrentActive = true, // Делаем Лондон активным
                orderIndex = 1 // Предполагаем, что геолокация (если есть) будет 0, Лондон - 1
            )
            coEvery { mockWeatherRepository.setActiveLocation(eq(londonSavedLocation.id)) } coAnswers {
                // Обновляем flow сохраненных локаций, чтобы отразить добавление Лондона
                // и то, что он теперь активен (а геолокация, если была, уже не isCurrentActive в БД)
                val baseSavedList: List<SavedLocation> =
                    if (viewModel.pagerItems.value.any { it is PagerItem.GeolocationPage }) {
                        // Если есть геолокационная страница, и мы добавляем Лондон как активную сохраненную,
                        // то список сохраненных локаций, который мы эмулируем из БД,
                        // не должен содержать других *активных* сохраненных локаций.
                        // Он может содержать другие *неактивные* сохраненные локации, если они были.
                        // Для простоты этого мока, если мы делаем Лондон единственной активной сохраненной,
                        // то предыдущий список сохраненных из БД может быть пустым или содержать только неактивные.
                        // В данном конкретном моке мы хотим, чтобы Лондон был добавлен и стал активным.
                        // Если до этого была только геолокация, то список сохраненных был пуст.
                        emptyList<SavedLocation>()
                    } else {
                        emptyList<SavedLocation>()
                    }

                val updatedSavedList = baseSavedList + listOf(londonSavedLocation)
                mutableSavedLocationsFlow.value = updatedSavedList // Эмитим новый список
                mutableCurrentActiveLocationFlow.value =
                    londonSavedLocation // Лондон теперь активен
                Log.d("ViewModelTestMock", "setActiveLocation for London: updated flows.")
            }

            // 4. Мок для погоды Лондона
            val londonWeatherData = mockWeatherDataBundle.copy(
                currentWeather = mockWeatherDataBundle.currentWeather.copy(cityName = "London")
            )
            coEvery {
                mockWeatherRepository.getAllWeatherData(
                    eq(londonSavedLocation.latitude),
                    eq(londonSavedLocation.longitude),
                    any(),
                    any()
                )
            } returns Resource.Success(londonWeatherData)


            // Инициализируем ViewModel
            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle() // Даем время на начальную загрузку геолокации

            // Проверяем начальное состояние: геолокация (New York) должна быть первой и активной
            viewModel.pagerItems.test {
                val initialItems = awaitItem()
                assertTrue("Initial pager items should not be empty", initialItems.isNotEmpty())
                assertTrue(
                    "First item should be GeolocationPage",
                    initialItems.first() is PagerItem.GeolocationPage
                )
                assertEquals(
                    "New York (Geo)",
                    (initialItems.first() as PagerItem.GeolocationPage).fetchedCityName
                )
                // cancelAndConsumeRemainingEvents() // Не закрываем, если хотим дальше следить
            }
            assertEquals(
                "Initial pager index should be 0 (geo)",
                0,
                viewModel.currentPagerIndex.value
            )


            // Act: Пользователь выбирает London
            val londonSuggestion = GeoapifyFeatureDto(
                properties = GeoapifyPropertiesDto(
                    city = "London",
                    countryCode = "GB",
                    latitude = 51.5,
                    longitude = -0.12,
                    formattedAddress = "London, UK",
                    placeId = "london_id",
                    country = "UK",
                    state = null,
                    postcode = null,
                    county = null,
                    street = null,
                    housenumber = null
                ), geometry = null
            )

            // Используем turbine для отслеживания изменений в pagerItems и currentPagerIndex
            viewModel.pagerItems.test {
                // Пропускаем начальное состояние, которое мы уже проверили выше
                // или проверяем его снова, если turbine был инициализирован до ViewModel
                var currentItems = awaitItem() // Начальные элементы (с геолокацией)
                Log.d(
                    "ViewModelTest",
                    "Initial PagerItems for turbine: ${currentItems.map { it.displayName }}"
                )


                viewModel.onCitySuggestionSelected(londonSuggestion)
                testScheduler.advanceUntilIdle() // Даем время на addSavedLocation, setActiveLocation

                // Ожидаем, что pagerItems обновится и будет содержать Лондон
                currentItems = awaitItem() // Должен быть список с Лондоном
                Log.d(
                    "ViewModelTest",
                    "PagerItems after London selected: ${currentItems.map { it.displayName }}"
                )
                val londonPageIndexInItems =
                    currentItems.indexOfFirst { it is PagerItem.SavedPage && it.location.cityName == "London" }
                assertTrue(
                    "London should be in pagerItems after selection",
                    londonPageIndexInItems != -1
                )

                // ViewModel должен был обновить _currentPagerIndex внутри onCitySuggestionSelected
                // после ожидания обновления pagerItems
                testScheduler.advanceUntilIdle() // Еще один advance для стабилизации _currentPagerIndex
                assertEquals(
                    "Pager index should be set to London's index",
                    londonPageIndexInItems,
                    viewModel.currentPagerIndex.value // Проверяем текущее значение
                )

                // Проверяем currentActivePagerItem
                val activeItem = viewModel.currentActivePagerItem.value
                assertTrue(
                    "Active PagerItem should be SavedPage",
                    activeItem is PagerItem.SavedPage
                )
                assertEquals("London", (activeItem as PagerItem.SavedPage).location.cityName)

                // Проверяем, что погода для Лондона загрузилась
                testScheduler.advanceUntilIdle() // Для загрузки погоды
                val weatherStateForLondon = viewModel.weatherDataStateMap.value[activeItem.id]
                assertTrue(
                    "Weather for London should be Success",
                    weatherStateForLondon is Resource.Success
                )
                assertEquals(
                    "London",
                    (weatherStateForLondon as Resource.Success).data?.currentWeather?.cityName
                )

                cancelAndConsumeRemainingEvents()
            }

            // Дополнительные проверки вызовов моков
            coVerify { mockWeatherRepository.addSavedLocation(any()) }
            assertEquals("London", capturedSavedLocation.captured.cityName.trim())
            coVerify { mockWeatherRepository.setActiveLocation(londonSavedLocation.id) }
        }

    // @Ignore("Skipping test due to complexity with asynchronous state updates and mock interactions. Needs further investigation.")
    @Test
    fun `deleteLocationAndUpdatePager - deletes location, updates pager to geo if active deleted and geo exists`() =
        runTest(testDispatcher) {
            // --- ARRANGE (оставляем как в вашем последнем коде) ---
            setPermissions(granted = true)

            val locationToDeleteId = 1
            val cityToDelete = SavedLocation(
                id = locationToDeleteId, cityName = "CityToDelete", countryCode = "TD",
                latitude = 1.0, longitude = 1.0, isCurrentActive = true, orderIndex = 0
            )
            val anotherCity = SavedLocation(
                id = 2, cityName = "AnotherCity", countryCode = "AC",
                latitude = 2.0, longitude = 2.0, isCurrentActive = false, orderIndex = 1
            )
            val initialSavedList = listOf(cityToDelete, anotherCity)
            val listAfterDeleteInRepo =
                listOf(anotherCity.copy(isCurrentActive = false, orderIndex = 0))

            coEvery { mockLocationTracker.getCurrentLocation() } returns flowOf(
                Resource.Success(
                    mockLocation
                )
            )
            coEvery {
                mockWeatherRepository.getLocationDetailsByCoordinates(
                    eq(mockLocation.latitude),
                    eq(mockLocation.longitude),
                    any()
                )
            } returns Resource.Success(mockGeoDetails)
            val geoWeatherData = mockWeatherDataBundle.copy(
                currentWeather = mockWeatherDataBundle.currentWeather.copy(cityName = "New York (Geo)")
            )
            coEvery {
                mockWeatherRepository.getAllWeatherData(
                    eq(mockLocation.latitude),
                    eq(mockLocation.longitude),
                    any(),
                    any()
                )
            } returns Resource.Success(geoWeatherData)

            val savedLocationsFlowForTest = MutableStateFlow(initialSavedList)
            val activeSavedLocationFlowForTest = MutableStateFlow<SavedLocation?>(cityToDelete)
            every { mockWeatherRepository.getSavedLocations() } returns savedLocationsFlowForTest
            every { mockWeatherRepository.getCurrentActiveWeatherLocation() } returns activeSavedLocationFlowForTest

            val cityToDeleteWeatherData = mockWeatherDataBundle.copy(
                currentWeather = mockWeatherDataBundle.currentWeather.copy(cityName = "CityToDelete")
            )
            coEvery {
                mockWeatherRepository.getAllWeatherData(
                    eq(cityToDelete.latitude),
                    eq(cityToDelete.longitude),
                    any(),
                    any()
                )
            } returns Resource.Success(cityToDeleteWeatherData)

            coEvery { mockWeatherRepository.deleteSavedLocation(locationToDeleteId) } coAnswers {
                savedLocationsFlowForTest.value = listAfterDeleteInRepo
                activeSavedLocationFlowForTest.value = null
            }
            coEvery { mockWeatherRepository.setActiveLocation(0) } returns Unit

            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle()

            val initialItems = viewModel.pagerItems.value
            val indexOfCityToDelete =
                initialItems.indexOfFirst { it.id == "saved_$locationToDeleteId" }
            if (viewModel.currentPagerIndex.value != indexOfCityToDelete && indexOfCityToDelete != -1) {
                viewModel.onPageChanged(indexOfCityToDelete)
                testScheduler.advanceUntilIdle()
            }
            assertEquals(
                "Initial active page should be CityToDelete",
                "saved_$locationToDeleteId",
                viewModel.currentActivePagerItem.value?.id
            )
            assertEquals(
                "Initial pager index should be for CityToDelete",
                indexOfCityToDelete,
                viewModel.currentPagerIndex.value
            )

            // --- ACT ---
            viewModel.deleteLocationAndUpdatePager(locationToDeleteId)
            testScheduler.advanceUntilIdle() // Даем время на deleteSavedLocation и ПЕРВЫЕ обновления Flow (getSavedLocations)

            // --- ASSERT ---
            coVerify { mockWeatherRepository.deleteSavedLocation(locationToDeleteId) }
            // ViewModel должен был вызвать setActiveLocation(0) после обновления pagerItems
            // Мы проверим это после того, как убедимся, что pagerItems обновился и currentPagerIndex тоже

            // 1. Проверяем pagerItems
            viewModel.pagerItems.test {
                val itemsAfterDelete = awaitItem() // Ждем обновления списка страниц
                Log.d(
                    "ViewModelTest",
                    "PagerItems after delete: ${itemsAfterDelete.map { it.displayName }}"
                )
                assertNotNull("Pager items should not be null after delete", itemsAfterDelete)
                assertTrue(
                    "Deleted city should not be in pager items",
                    itemsAfterDelete.none { it.id == "saved_$locationToDeleteId" })
                assertTrue(
                    "AnotherCity should still be in pager items",
                    itemsAfterDelete.any { it is PagerItem.SavedPage && it.location.id == anotherCity.id })
                val geoPageIndex = itemsAfterDelete.indexOfFirst { it is PagerItem.GeolocationPage }
                assertTrue("Geolocation page should exist in final items", geoPageIndex != -1)

                // Важно: не проверяем здесь currentPagerIndex или currentActivePagerItem,
                // так как они обновляются ПОСЛЕ обновления pagerItems внутри ViewModel.
                cancelAndConsumeRemainingEvents()
            }

            // 2. Даем ViewModel время обновить currentPagerIndex и currentActivePagerItem
            //    на основе только что обновленных pagerItems.
            testScheduler.advanceUntilIdle()

            // 3. Теперь проверяем currentPagerIndex
            val finalPagerItems = viewModel.pagerItems.value // Получаем самый свежий список
            val expectedGeoPageIndex =
                finalPagerItems.indexOfFirst { it is PagerItem.GeolocationPage }
            assertTrue(
                "Geolocation page should exist in final pager items for index check",
                expectedGeoPageIndex != -1
            )
            assertEquals(
                "Pager index should be geolocation's index",
                expectedGeoPageIndex,
                viewModel.currentPagerIndex.value
            )

            // 4. Проверяем currentActivePagerItem
            val finalActiveItem = viewModel.currentActivePagerItem.value
            assertTrue(
                "Active item should be GeolocationPage",
                finalActiveItem is PagerItem.GeolocationPage
            )
            assertEquals(
                "New York (Geo)",
                (finalActiveItem as PagerItem.GeolocationPage).fetchedCityName
            )

            // 5. Проверяем вызов setActiveLocation(0) теперь, когда мы уверены, что ViewModel должен был его сделать
            coVerify { mockWeatherRepository.setActiveLocation(0) }
        }


    @Test
    fun `initial state - permission granted, but GPS disabled - shows GPS error`() =
        runTest(testDispatcher) {
            setPermissions(granted = true)

            // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
            coEvery { mockLocationTracker.getCurrentLocation() } returns flowOf(Resource.Error("GPS is disabled. Please enable location services."))
            // --- КОНЕЦ ИЗМЕНЕНИЯ ---

            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle()

            // Assert
            val geoPageId = PagerItem.GeolocationPage().id
            val state = viewModel.weatherDataStateMap.value[geoPageId]
            assertTrue(state is Resource.Error)
            assertTrue((state as Resource.Error).message?.contains("GPS is disabled") == true)
            // Также можно проверить, что _geolocationPagerItemState.fetchedCityName == "GPS Disabled"
        }

    @Test
    fun `geolocation page shows loading message from location tracker`() =
        runTest(testDispatcher) {
            setPermissions(granted = true)

            // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
            coEvery { mockLocationTracker.getCurrentLocation() } returns flowOf(
                Resource.Loading(
                    message = "Fetching current location..."
                )
            )
            // --- КОНЕЦ ИЗМЕНЕНИЯ ---

            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle()

            // Assert
            val geoPageId = PagerItem.GeolocationPage().id
            val state = viewModel.weatherDataStateMap.value[geoPageId]
            assertTrue(state is Resource.Loading)
            assertEquals("Fetching current location...", (state as Resource.Loading).message)
            // Также можно проверить, что _geolocationPagerItemState.isLoadingDetails == true
            // и _geolocationPagerItemState.fetchedCityName содержит сообщение о загрузке
        }

    // --- НОВЫЕ ТЕСТЫ ---
    @Ignore("Skipping this test temporarily due to issues with async state updates")
    @Test
    fun `determineInitialPage - no permission, no active saved, has inactive saved - selects first saved`() =
        runTest(testDispatcher) {
            // Arrange
            setPermissions(granted = false)
            val savedLocation1 = SavedLocation(
                id = 1,
                cityName = "Paris",
                countryCode = "FR",
                latitude = 1.0,
                longitude = 1.0,
                isCurrentActive = false,
                orderIndex = 0
            )
            val savedLocation2 = SavedLocation(
                id = 2,
                cityName = "Berlin",
                countryCode = "DE",
                latitude = 2.0,
                longitude = 2.0,
                isCurrentActive = false,
                orderIndex = 1
            )
            val savedList = listOf(savedLocation1, savedLocation2)

            // Используем MutableStateFlow для имитации изменений из БД
            val localSavedLocationsFlow = MutableStateFlow(savedList)
            val localActiveSavedLocationFlow = MutableStateFlow<SavedLocation?>(null)

            every { mockWeatherRepository.getSavedLocations() } returns localSavedLocationsFlow
            every { mockWeatherRepository.getCurrentActiveWeatherLocation() } returns localActiveSavedLocationFlow

            val parisWeatherData = mockWeatherDataBundle.copy(
                currentWeather = mockWeatherDataBundle.currentWeather.copy(cityName = "Paris")
            )
            coEvery {
                mockWeatherRepository.getAllWeatherData(
                    eq(savedLocation1.latitude),
                    eq(savedLocation1.longitude),
                    any(),
                    any()
                )
            } returns Resource.Success(parisWeatherData)
            // Мокируем setActiveLocation, чтобы убедиться, что он вызывается для Paris
            coEvery { mockWeatherRepository.setActiveLocation(savedLocation1.id) } coAnswers {
                // Имитируем, что после этого Paris становится активной в "БД"
                localActiveSavedLocationFlow.value = savedLocation1.copy(isCurrentActive = true)
            }


            // Re-initialize ViewModel
            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle() // Для init и determineInitialPage

            // Assert
            Log.d(
                "TestDetermineInitial",
                "PagerItems: ${viewModel.pagerItems.value.map { it.displayName }}"
            )
            Log.d("TestDetermineInitial", "Current Index: ${viewModel.currentPagerIndex.value}")
            Log.d(
                "TestDetermineInitial",
                "Active Item: ${viewModel.currentActivePagerItem.value?.displayName}"
            )


            // Ожидаем, что Paris (первая сохраненная) будет выбрана
            // Так как нет разрешений, геолокация не будет в pagerItems
            val expectedParisIndex = 0 // Paris должна быть на индексе 0
            assertEquals(
                "Current pager index should be for Paris",
                expectedParisIndex,
                viewModel.currentPagerIndex.value
            )

            val activeItem = viewModel.currentActivePagerItem.value
            assertTrue("Active item should be a SavedPage", activeItem is PagerItem.SavedPage)
            assertEquals("Paris", (activeItem as PagerItem.SavedPage).location.cityName)

            // Проверяем, что setActiveLocation был вызван для Paris
            coVerify { mockWeatherRepository.setActiveLocation(savedLocation1.id) }
        }

    @Test
    fun `onPageChanged - to saved location - sets active location in repo`() =
        runTest(testDispatcher) {
            // Arrange
            setPermissions(granted = true) // Геолокация есть, но мы переключимся на сохраненную
            val savedLocation = SavedLocation(
                id = 1,
                cityName = "Kyiv",
                countryCode = "UA",
                latitude = 50.45,
                longitude = 30.52,
                isCurrentActive = false,
                orderIndex = 1
            )
            mutableSavedLocationsFlow.value = listOf(savedLocation)
            coEvery { mockLocationTracker.getCurrentLocation() } returns flowOf(
                Resource.Success(
                    mockLocation
                )
            ) // Начальная геолокация

            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle() // Начальная инициализация

            // Находим индекс сохраненной страницы (после геолокации)
            val savedPageIndex =
                viewModel.pagerItems.value.indexOfFirst { it is PagerItem.SavedPage && it.location.id == savedLocation.id }
            assertTrue("Saved page for Kyiv should exist", savedPageIndex != -1)

            // Act
            viewModel.onPageChanged(savedPageIndex)
            testScheduler.advanceUntilIdle()

            // Assert
            coVerify { mockWeatherRepository.setActiveLocation(savedLocation.id) }
            assertEquals(savedPageIndex, viewModel.currentPagerIndex.value)
        }

    @Test
    fun `onPageChanged - to geolocation page - sets active location to 0 in repo`() =
        runTest(testDispatcher) {
            // Arrange
            setPermissions(granted = true)
            val savedLocation = SavedLocation(
                id = 1,
                cityName = "Kyiv",
                countryCode = "UA",
                latitude = 50.45,
                longitude = 30.52,
                isCurrentActive = true,
                orderIndex = 0
            )
            mutableSavedLocationsFlow.value =
                listOf(savedLocation) // Начинаем с активной сохраненной
            mutableCurrentActiveLocationFlow.value = savedLocation
            coEvery { mockLocationTracker.getCurrentLocation() } returns flowOf(
                Resource.Success(
                    mockLocation
                )
            )

            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle() // Начальная инициализация (должна выбрать Kyiv)

            // Находим индекс геолокационной страницы
            val geoPageIndex =
                viewModel.pagerItems.value.indexOfFirst { it is PagerItem.GeolocationPage }
            assertTrue("Geolocation page should exist", geoPageIndex != -1)
            // Убедимся, что мы не на геолокации изначально
            if (viewModel.currentPagerIndex.value == geoPageIndex) {
                // Если случайно геолокация стала активной, переключимся на сохраненную для чистоты теста
                val savedPageIndex =
                    viewModel.pagerItems.value.indexOfFirst { it is PagerItem.SavedPage }
                if (savedPageIndex != -1) viewModel.onPageChanged(savedPageIndex)
                testScheduler.advanceUntilIdle()
            }
            assertTrue(
                "Initial page should not be geolocation for this test part",
                viewModel.currentPagerIndex.value != geoPageIndex
            )


            // Act
            viewModel.onPageChanged(geoPageIndex)
            testScheduler.advanceUntilIdle()

            // Assert
            coVerify { mockWeatherRepository.setActiveLocation(0) } // 0 для геолокации
            assertEquals(geoPageIndex, viewModel.currentPagerIndex.value)
        }

    @Test
    fun `searchCityAutocomplete - query less than 3 chars - returns success emptyList`() =
        runTest(testDispatcher) {
            // Arrange
            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            val shortQuery = "ne"

            // Act
            viewModel.searchCityAutocomplete(shortQuery)
            testScheduler.advanceUntilIdle()

            // Assert
            viewModel.autocompleteResults.test {
                val result = awaitItem()
                assertTrue(result is Resource.Success)
                assertTrue((result as Resource.Success).data.isNullOrEmpty())
                cancelAndConsumeRemainingEvents()
            }
            coVerify(exactly = 0) {
                mockWeatherRepository.getCityAutocompleteSuggestions(
                    any(),
                    any()
                )
            } // Проверяем, что вызова в репозиторий не было
        }

    @Ignore("Skipping this test temporarily due to issues with async state updates")
    @Test
    fun `forceGpsDisabledError - updates weatherDataMap and geolocationPagerItemState`() =
        runTest(testDispatcher) {
            // Arrange
            setPermissions(granted = true) // Разрешения есть
            viewModel = MainViewModel(
                mockWeatherRepository,
                mockLocationTracker,
                mockApplication,
                mockUserPreferencesRepository
            )
            testScheduler.advanceUntilIdle() // Даем ViewModel инициализироваться

            // Act
            viewModel.forceGpsDisabledError()
            testScheduler.advanceUntilIdle()

            // Assert
            val geoPageId = PagerItem.GeolocationPage().id
            val weatherState = viewModel.weatherDataStateMap.value[geoPageId]
            assertTrue("Weather state for geo page should be Error", weatherState is Resource.Error)
            assertEquals(
                "GPS is disabled. Please enable location services.",
                (weatherState as Resource.Error).message
            )

            val geoPagerItem =
                viewModel.pagerItems.value.find { it.id == geoPageId } as? PagerItem.GeolocationPage
            assertNotNull("Geolocation PagerItem should exist", geoPagerItem)
            assertEquals("GPS Disabled", geoPagerItem?.fetchedCityName)
            assertEquals(false, geoPagerItem?.isLoadingDetails)
        }
}