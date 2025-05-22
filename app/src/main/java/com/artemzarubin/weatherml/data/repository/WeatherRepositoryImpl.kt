package com.artemzarubin.weatherml.data.repository

// We import the correct DTO for pollutant components
import android.util.Log
import com.artemzarubin.weatherml.data.local.SavedLocationDao
import com.artemzarubin.weatherml.data.mapper.mapToWeatherDataBundle
import com.artemzarubin.weatherml.data.mapper.toDomainModel
import com.artemzarubin.weatherml.data.mapper.toDomainModelList
import com.artemzarubin.weatherml.data.mapper.toEntity
import com.artemzarubin.weatherml.data.mapper.toEntityList
import com.artemzarubin.weatherml.data.remote.ApiService
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.domain.ml.ModelInput
import com.artemzarubin.weatherml.domain.ml.WeatherModelInterpreter
import com.artemzarubin.weatherml.domain.model.SavedLocation
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val modelInterpreter: WeatherModelInterpreter,
    private val savedLocationDao: SavedLocationDao
) : WeatherRepository {

    override suspend fun getAllWeatherData(
        lat: Double,
        lon: Double,
        apiKey: String
    ): Resource<WeatherDataBundle> {
        return withContext(Dispatchers.IO) {
            try {
                val currentWeatherDeferred = async {
                    apiService.getCurrentWeather(
                        latitude = lat,
                        longitude = lon,
                        apiKey = apiKey
                    )
                }
                val forecastDeferred = async {
                    apiService.getForecast(
                        latitude = lat,
                        longitude = lon,
                        apiKey = apiKey,
                        count = 40
                    )
                }
                // val airPollutionDeferred = async { apiService.getAirPollutionData(latitude = lat, longitude = lon, apiKey = apiKey) } // Поки що не використовуємо

                val currentWeatherResponseDto = currentWeatherDeferred.await()
                val forecastResponseDto = forecastDeferred.await()
                // val airPollutionResponseDto = airPollutionDeferred.await()

                var weatherDataBundle = mapToWeatherDataBundle(
                    currentWeatherDto = currentWeatherResponseDto,
                    forecastResponseDto = forecastResponseDto,
                    lat = lat,
                    lon = lon
                )

                if (modelInterpreter.initialize()) {
                    // Вызываем метод у экземпляра modelInterpreter
                    val modelInputFeatures =
                        modelInterpreter.prepareAndScaleFeatures(weatherDataBundle.currentWeather)

                    if (modelInputFeatures != null) {
                        val predictionInput = ModelInput(features = modelInputFeatures)
                        val mlOutput = modelInterpreter.getPrediction(predictionInput)
                        if (mlOutput != null) {
                            Log.i(
                                "WeatherRepositoryImpl",
                                "ML 'Feels Like' Prediction: ${mlOutput.predictedFeelsLikeTemp}"
                            )
                            val updatedCurrentWeather = weatherDataBundle.currentWeather.copy(
                                mlFeelsLikeCelsius = mlOutput.predictedFeelsLikeTemp // Оновлюємо mlFeelsLikeCelsius
                            )
                            weatherDataBundle =
                                weatherDataBundle.copy(currentWeather = updatedCurrentWeather)
                        } else {
                            Log.w(
                                "WeatherRepositoryImpl",
                                "ML 'Feels Like' model returned null prediction."
                            )
                        }
                    } else {
                        Log.w(
                            "WeatherRepositoryImpl",
                            "Could not prepare features for 'Feels Like' model input."
                        )
                    }
                } else {
                    Log.e(
                        "WeatherRepositoryImpl",
                        "ML 'Feels Like' Interpreter failed to initialize."
                    )
                }

                Resource.Success(data = weatherDataBundle)
            } catch (e: Exception) {
                Log.e("WeatherRepositoryImpl", "Error in getAllWeatherData: ${e.message}", e)
                Resource.Error(message = e.message ?: "An unknown error occurred")
            }
        }
    }

    override suspend fun getCityAutocompleteSuggestions(
        query: String,
        apiKey: String,
        limit: Int
    ): Resource<List<GeoapifyFeatureDto>> {
        return withContext(Dispatchers.IO) {
            try {
                val response =
                    apiService.getCityAutocomplete(text = query, apiKey = apiKey, limit = limit)
                Resource.Success(data = response.features ?: emptyList())
            } catch (e: Exception) {
                Log.e(
                    "WeatherRepositoryImpl",
                    "Error fetching city suggestions: ${e.message}",
                    e
                )
                Resource.Error(message = e.message ?: "Error fetching suggestions")
            }
        }
    }

    // --- IMPLEMENTATION OF NEW METHODS ---

    override fun getSavedLocations(): Flow<List<SavedLocation>> {
        return savedLocationDao.getAllSavedLocations().map { entities ->
            entities.toDomainModelList() // Let's map the Entity to the Domain Model
        }
    }

    override fun getCurrentActiveWeatherLocation(): Flow<SavedLocation?> {
        return savedLocationDao.getCurrentActiveLocation().map { entity ->
            entity?.toDomainModel() // Map Entity to Domain Model if not null
        }
    }

    override suspend fun addSavedLocation(location: SavedLocation): Long {
        // Check if a location with these coordinates already exists
        val existingLocation =
            savedLocationDao.getLocationByCoordinates(location.latitude, location.longitude)
        if (existingLocation != null) {
            Log.w(
                "WeatherRepositoryImpl",
                "Location already exists with id ${existingLocation.id}. Not adding duplicate."
            )
            // Might be worth updating isCurrentLocation to an existing one if needed
            // setActiveLocation(existingLocation.id)
            return -2L // Or other code that means "already exists"
        }

        // If this is the first location, make it active
        val count = savedLocationDao.getLocationsCount()
        val entityToInsert = if (count == 0) {
            location.toEntity().copy(isCurrentLocation = true, orderIndex = 0)
        } else {
            location.toEntity()
                .copy(orderIndex = count) // New locations are added to the end of the list
        }
        return savedLocationDao.insertLocation(entityToInsert)
    }

    override suspend fun setActiveLocation(locationId: Int) {
        savedLocationDao.setCurrentActiveLocation(locationId)
    }

    override suspend fun deleteSavedLocation(locationId: Int) {
        savedLocationDao.deleteLocationById(locationId)
        // After deletion, if there are no more active locations, you can make the first one in the list active (if the list is not empty)
        // Or this logic should be in the ViewModel
    }

    override suspend fun updateSavedLocationsOrder(locations: List<SavedLocation>) {
        savedLocationDao.updateLocationOrder(locations.toEntityList())
    }

    override suspend fun hasSavedLocations(): Boolean {
        return savedLocationDao.getLocationsCount() > 0
    }

    override suspend fun getSavedLocationById(locationId: Int): SavedLocation? {
        return savedLocationDao.getLocationById(locationId)?.toDomainModel()
    }

    override suspend fun getLocationDetailsByCoordinates(
        lat: Double,
        lon: Double,
        apiKey: String
    ): Resource<GeoapifyFeatureDto?> {
        return try {
            val response = apiService.reverseGeocode(
                latitude = lat,
                longitude = lon,
                apiKey = apiKey
                // type = "city" and lang = "en" are already set by default in ApiService
            )
            // Reverse geocoding usually returns one or more nearest addresses/places.
            // Let's take the first significant one.
            val firstFeature = response.features?.firstOrNull()
            if (firstFeature != null) {
                Log.d(
                    "WeatherRepositoryImpl",
                    "Reverse geocoding success: ${firstFeature.properties?.formattedAddress}"
                )
                Resource.Success(data = firstFeature)
            } else {
                Log.w(
                    "WeatherRepositoryImpl",
                    "Reverse geocoding returned no features for $lat, $lon"
                )
                Resource.Error(message = "Could not determine location name from coordinates.")
            }
        } catch (e: Exception) {
            Log.e("WeatherRepositoryImpl", "Error during reverse geocoding: ${e.message}", e)
            Resource.Error(message = e.message ?: "Reverse geocoding failed")
        }
    }
}
