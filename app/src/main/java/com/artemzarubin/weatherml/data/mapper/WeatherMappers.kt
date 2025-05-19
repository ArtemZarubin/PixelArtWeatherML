// File: com/artemzarubin/weatherml/data/mapper/WeatherMappers.kt
package com.artemzarubin.weatherml.data.mapper

import android.util.Log
import com.artemzarubin.weatherml.data.remote.dto.CurrentWeatherResponseDto
import com.artemzarubin.weatherml.data.remote.dto.ForecastItemDto
import com.artemzarubin.weatherml.data.remote.dto.ForecastResponseDto
import com.artemzarubin.weatherml.domain.model.CurrentWeather
import com.artemzarubin.weatherml.domain.model.DailyForecast
import com.artemzarubin.weatherml.domain.model.HourlyForecast
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- Mapper for Current Weather (from /weather endpoint DTO) ---
fun CurrentWeatherResponseDto.toDomain(): CurrentWeather {
    val weatherCond = this.weather?.firstOrNull()
    return CurrentWeather(
        dateTimeMillis = (this.dateTime ?: 0L) * 1000L,
        sunriseMillis = (this.sys?.sunrise ?: 0L) * 1000L,
        sunsetMillis = (this.sys?.sunset ?: 0L) * 1000L,
        temperatureCelsius = this.main?.temp ?: 0.0,
        feelsLikeCelsius = this.main?.feelsLike ?: 0.0,
        pressureHpa = this.main?.pressure ?: 0,
        humidityPercent = this.main?.humidity ?: 0,
        cloudinessPercent = this.clouds?.all ?: 0,
        visibilityMeters = this.visibility ?: 0,
        windSpeedMps = this.wind?.speed ?: 0.0,
        windDirectionDegrees = this.wind?.deg ?: 0,
        weatherConditionId = weatherCond?.id ?: 0,
        weatherCondition = weatherCond?.main ?: "Unknown",
        weatherDescription = weatherCond?.description ?: "No description",
        weatherIconId = weatherCond?.icon ?: "",
        cityName = this.cityName ?: "Unknown City",
        countryCode = this.sys?.country,
        timezoneOffsetSeconds = this.timezone ?: 0
    )
}

// --- Mappers for Forecast (from /forecast endpoint DTOs) ---
fun ForecastItemDto.toDomainHourly(): HourlyForecast {
    val weatherCond = this.weather?.firstOrNull()
    return HourlyForecast(
        dateTimeMillis = (this.dateTime ?: 0L) * 1000L,
        temperatureCelsius = this.main?.temp ?: 0.0,
        feelsLikeCelsius = this.main?.feelsLike ?: 0.0,
        probabilityOfPrecipitation = this.probabilityOfPrecipitation ?: 0.0,
        weatherConditionId = weatherCond?.id ?: 0,
        weatherCondition = weatherCond?.main ?: "Unknown",
        weatherDescription = weatherCond?.description ?: "No description",
        weatherIconId = weatherCond?.icon ?: "",
        windSpeedMps = this.wind?.speed ?: 0.0,
        windDirectionDegrees = this.wind?.deg ?: 0,
        humidityPercent = this.main?.humidity ?: 0,
        pressureHpa = this.main?.pressure ?: 0,
        cloudinessPercent = this.clouds?.all // <--- ADDED: Getting cloudiness
    )
}

fun ForecastResponseDto.toDomainHourlyList(): List<HourlyForecast> {
    return this.list?.map { it.toDomainHourly() } ?: emptyList()
}

fun mapToWeatherDataBundle(
    currentWeatherDto: CurrentWeatherResponseDto,
    forecastResponseDto: ForecastResponseDto,
    lat: Double,
    lon: Double
): WeatherDataBundle {
    val currentWeatherDomain = currentWeatherDto.toDomain()
    val hourlyForecastsDomain = forecastResponseDto.toDomainHourlyList().take(8) // Next 24 hours

    val dailyForecastsDomain = mutableListOf<DailyForecast>()
    val groupedByDay = forecastResponseDto.list?.groupBy {
        val date = Date((it.dateTime ?: 0L) * 1000L)
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(date)
    }

    groupedByDay?.values?.take(7)?.forEach { dailyItems -> // Take up to 7 days
        // Aggregate data for the day
        var minTemp: Double? = null
        var maxTemp: Double? = null
        val tempsForDay = mutableListOf<Double>()
        val feelsLikeForDay = mutableListOf<Double>()
        val pops = mutableListOf<Double>()
        val weatherConditionsCounts =
            mutableMapOf<Pair<String?, String?>, Int>() // Pair of (main, icon) to count

        dailyItems.forEach { item ->
            item.main?.temp?.let { temp ->
                tempsForDay.add(temp)
                if (minTemp == null || temp < minTemp!!) minTemp = temp
                if (maxTemp == null || temp > maxTemp!!) maxTemp = temp
            }
            item.main?.feelsLike?.let { feelsLikeForDay.add(it) }
            item.probabilityOfPrecipitation?.let { pops.add(it) }
            item.weather?.firstOrNull()?.let { cond ->
                val key = Pair(cond.main, cond.icon)
                weatherConditionsCounts[key] = (weatherConditionsCounts[key] ?: 0) + 1
            }
        }

        // Select a representative item (e.g., first item of the day for dateTime, or midday item)
        val representativeItem = dailyItems.firstOrNull() // Or find midday item

        if (representativeItem != null) {
            val dominantWeather = weatherConditionsCounts.maxByOrNull { it.value }?.key
            dailyForecastsDomain.add(
                DailyForecast(
                    dateTimeMillis = (representativeItem.dateTime ?: 0L) * 1000L,
                    sunriseMillis = (forecastResponseDto.city?.sunrise ?: 0L) * 1000L,
                    sunsetMillis = (forecastResponseDto.city?.sunset ?: 0L) * 1000L,
                    tempMinCelsius = minTemp ?: representativeItem.main?.temp ?: 0.0,
                    tempMaxCelsius = maxTemp ?: representativeItem.main?.temp ?: 0.0,
                    tempDayCelsius = if (tempsForDay.isNotEmpty()) tempsForDay.average() else representativeItem.main?.temp
                        ?: 0.0,
                    tempNightCelsius = 0.0, // Needs specific night data or better aggregation
                    feelsLikeDayCelsius = if (feelsLikeForDay.isNotEmpty()) feelsLikeForDay.average() else representativeItem.main?.feelsLike
                        ?: 0.0,
                    feelsLikeNightCelsius = 0.0, // Needs specific night data
                    probabilityOfPrecipitation = if (pops.isNotEmpty()) pops.maxOrNull()
                        ?: 0.0 else 0.0, // Max POP for the day
                    weatherConditionId = dominantWeather?.first?.hashCode()
                        ?: representativeItem.weather?.firstOrNull()?.id ?: 0, // Simplification
                    weatherCondition = dominantWeather?.first
                        ?: representativeItem.weather?.firstOrNull()?.main ?: "Unknown",
                    weatherDescription = representativeItem.weather?.firstOrNull()?.description
                        ?: "No description", // Could take from dominant
                    weatherIconId = dominantWeather?.second
                        ?: representativeItem.weather?.firstOrNull()?.icon ?: "",
                    windSpeedMps = representativeItem.wind?.speed ?: 0.0, // Could average wind
                    windDirectionDegrees = representativeItem.wind?.deg
                        ?: 0, // Could find dominant direction
                    humidityPercent = representativeItem.main?.humidity
                        ?: 0, // Could average humidity
                    pressureHpa = representativeItem.main?.pressure
                        ?: 0,     // Could average pressure
                    uvi = 0.0 // UVI is not in /forecast list items, typically in OneCall's daily
                )
            )
        }
    }
    Log.d("WeatherMapper", "Number of daily forecasts mapped: ${dailyForecastsDomain.size}")

    return WeatherDataBundle(
        latitude = lat,
        longitude = lon,
        timezone = forecastResponseDto.city?.name ?: currentWeatherDto.cityName
        ?: "Unknown Location",
        currentWeather = currentWeatherDomain,
        hourlyForecasts = hourlyForecastsDomain,
        dailyForecasts = dailyForecastsDomain // This will now have more aggregated daily data
    )
}