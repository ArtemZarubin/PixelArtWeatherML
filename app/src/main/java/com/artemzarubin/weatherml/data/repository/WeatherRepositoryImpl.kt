package com.artemzarubin.weatherml.data.repository

// Импортируем правильный DTO для компонентов загрязнителей
import android.util.Log
import com.artemzarubin.weatherml.data.mapper.mapToWeatherDataBundle
import com.artemzarubin.weatherml.data.remote.ApiService
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.domain.ml.ModelInput
import com.artemzarubin.weatherml.domain.ml.WeatherModelInterpreter
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.artemzarubin.weatherml.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val modelInterpreter: WeatherModelInterpreter
) : WeatherRepository {

    private val featureOrderFromTraining: List<String> = listOf(
        "num__Temperature (C)", "num__Humidity", "num__Wind Speed (km/h)",
        "num__Visibility (km)", "num__Pressure (millibars)",
        "cat__Precip Type_rain", "cat__Precip Type_snow",
        "remainder__HourSin", "remainder__HourCos", "remainder__MonthSin",
        "remainder__MonthCos", "remainder__DayOfYearSin", "remainder__DayOfYearCos",
        "remainder__WindBearingSin", "remainder__WindBearingCos"
    )

    // Допоміжна функція для отримання локальної години, місяця та дня року
    private fun getLocalTimeFeatures(
        dateTimeMillisUTC: Long,
        timezoneOffsetSeconds: Int
    ): Triple<Int, Int, Int> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = dateTimeMillisUTC
        calendar.add(Calendar.SECOND, timezoneOffsetSeconds)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val month = calendar.get(Calendar.MONTH) + 1
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        return Triple(hour, month, dayOfYear)
    }

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

    private fun getLocalHourAndMonth(
        dateTimeMillisUTC: Long,
        timezoneOffsetSeconds: Int
    ): Pair<Int, Int> {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.timeInMillis = dateTimeMillisUTC
        calendar.add(
            Calendar.SECOND,
            timezoneOffsetSeconds
        ) // Convert to local time of the location
        val hour = calendar.get(Calendar.HOUR_OF_DAY) // 0-23
        val month =
            calendar.get(Calendar.MONTH) + 1    // Month is 0-indexed (0-11), so add 1 for (1-12)
        return Pair(hour, month)
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
}