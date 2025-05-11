package com.artemzarubin.weatherml.data.mapper

// Add these imports for Date, SimpleDateFormat, and Locale
// Make sure all your DTOs and Domain Models are correctly imported
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
        dateTimeMillis = this.dateTime ?: 0L,
        sunriseMillis = this.sys?.sunrise ?: 0L,
        sunsetMillis = this.sys?.sunset ?: 0L,
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
        dateTimeMillis = this.dateTime ?: 0L,
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
        pressureHpa = this.main?.pressure ?: 0
    )
}

// This is a simplified mapper for demonstrating the concept.
// Real daily forecast aggregation from 3-hourly data would be more complex.
fun ForecastItemDto.toDomainDailyPlaceholder(): DailyForecast {
    val weatherCond = this.weather?.firstOrNull()
    return DailyForecast(
        dateTimeMillis = this.dateTime ?: 0L,
        sunriseMillis = 0L, // Placeholder - not directly available in ForecastItemDto
        sunsetMillis = 0L,  // Placeholder - not directly available in ForecastItemDto
        tempMinCelsius = this.main?.tempMin ?: this.main?.temp ?: 0.0, // Approximation
        tempMaxCelsius = this.main?.tempMax ?: this.main?.temp ?: 0.0, // Approximation
        tempDayCelsius = this.main?.temp ?: 0.0,
        tempNightCelsius = this.main?.temp ?: 0.0, // Placeholder - needs night temp data
        feelsLikeDayCelsius = this.main?.feelsLike ?: 0.0,
        feelsLikeNightCelsius = this.main?.feelsLike ?: 0.0, // Placeholder
        probabilityOfPrecipitation = this.probabilityOfPrecipitation ?: 0.0,
        weatherConditionId = weatherCond?.id ?: 0,
        weatherCondition = weatherCond?.main ?: "Unknown",
        weatherDescription = weatherCond?.description ?: "No description",
        weatherIconId = weatherCond?.icon ?: "",
        windSpeedMps = this.wind?.speed ?: 0.0,
        windDirectionDegrees = this.wind?.deg ?: 0,
        humidityPercent = this.main?.humidity ?: 0,
        pressureHpa = this.main?.pressure ?: 0,
        uvi = 0.0 // Placeholder - UVI not in /forecast list items
    )
}

fun ForecastResponseDto.toDomainHourlyList(): List<HourlyForecast> {
    return this.list?.map { it.toDomainHourly() } ?: emptyList()
}

// Placeholder function for creating a list of daily forecasts.
// This requires a strategy to select or aggregate data from the 3-hourly forecast items.
fun ForecastResponseDto.toDomainDailyListPlaceholder(): List<DailyForecast> {
    val dailyForecastsOutput = mutableListOf<DailyForecast>()
    this.list?.groupBy { forecastItem ->
        // Group by day using SimpleDateFormat
        val date = Date((forecastItem.dateTime ?: 0L) * 1000L) // Ensure multiplication is Long
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
    }?.forEach { (_, hourlyItemsForDay) ->
        // For simplicity, take an item around midday (e.g., the 4th 3-hour block if available)
        // A more robust approach would aggregate min/max temperatures, dominant weather, etc.
        hourlyItemsForDay.getOrNull(4)?.let { representativeItem ->
            dailyForecastsOutput.add(representativeItem.toDomainDailyPlaceholder())
        }
    }
    return dailyForecastsOutput.take(7) // Limit to a week for consistency
}


fun mapToWeatherDataBundle(
    currentWeatherDto: CurrentWeatherResponseDto,
    forecastResponseDto: ForecastResponseDto,
    lat: Double,
    lon: Double
): WeatherDataBundle {
    val currentWeatherDomain = currentWeatherDto.toDomain()
    // Take up to 8 hourly forecasts (next 24 hours)
    val hourlyForecastsDomain = forecastResponseDto.toDomainHourlyList().take(8)
    // Use the placeholder logic for daily forecasts
    val dailyForecastsDomain = forecastResponseDto.toDomainDailyListPlaceholder()

    return WeatherDataBundle(
        latitude = lat,
        longitude = lon,
        // Using city name from forecast if available, otherwise from current weather
        timezone = forecastResponseDto.city?.name ?: currentWeatherDto.cityName
        ?: "Unknown Location",
        currentWeather = currentWeatherDomain,
        hourlyForecasts = hourlyForecastsDomain,
        dailyForecasts = dailyForecastsDomain
    )
}