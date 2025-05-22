package com.artemzarubin.weatherml.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.artemzarubin.weatherml.R
import com.artemzarubin.weatherml.data.preferences.TemperatureUnit
import com.artemzarubin.weatherml.domain.model.CurrentWeather
import com.artemzarubin.weatherml.domain.model.DailyForecast
import com.artemzarubin.weatherml.domain.model.HourlyForecast
import com.artemzarubin.weatherml.ui.theme.WeatherMLTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun PixelatedSunLoader(modifier: Modifier = Modifier) {
    val frames = listOf(
        R.drawable.block_frame_1,
        R.drawable.block_frame_2,
        R.drawable.block_frame_3,
        R.drawable.block_frame_4,
        R.drawable.block_frame_5,
        R.drawable.block_frame_6,
        R.drawable.block_frame_7,
        R.drawable.block_frame_8,
        R.drawable.block_frame_9,
        R.drawable.block_frame_10,
        R.drawable.block_frame_11,
        R.drawable.block_frame_12,
        R.drawable.block_frame_13,
        R.drawable.block_frame_14,
        R.drawable.block_frame_15,
        R.drawable.block_frame_16,
        R.drawable.block_frame_17,
        R.drawable.block_frame_18,
        R.drawable.block_frame_19,
        R.drawable.block_frame_20,
        R.drawable.block_frame_21,
        R.drawable.block_frame_22,
        R.drawable.block_frame_23,
        R.drawable.block_frame_24,
        R.drawable.block_frame_25,
        R.drawable.block_frame_26,
        R.drawable.block_frame_27,
        R.drawable.block_frame_28,
        R.drawable.block_frame_29,
        R.drawable.block_frame_30,
        R.drawable.block_frame_31,
        R.drawable.block_frame_32,
        R.drawable.block_frame_33,
        R.drawable.block_frame_34,
        R.drawable.block_frame_35,
        R.drawable.block_frame_36,
        R.drawable.block_frame_37,
        R.drawable.block_frame_38,
        R.drawable.block_frame_39,
        R.drawable.block_frame_40,
        R.drawable.block_frame_41,
        R.drawable.block_frame_42,
        R.drawable.block_frame_43,
        R.drawable.block_frame_44,
        R.drawable.block_frame_45,
        R.drawable.block_frame_46
    )
    var currentFrameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(28L) // Preferred delay
            currentFrameIndex = (currentFrameIndex + 1) % frames.size
        }
    }

    Image(
        painter = painterResource(id = frames[currentFrameIndex]),
        contentDescription = "Loading animation",
        modifier = modifier.size(100.dp), // Preferred size
        contentScale = ContentScale.Fit
        // colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary) // Optional tint
    )
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherMLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}

// Update CurrentWeatherMainSection to NOT include city name and last update
@Composable
fun CurrentWeatherMainSection(
    currentWeather: CurrentWeather,
    temperatureUnit: TemperatureUnit
) { // <--- ДОДАНО ПАРАМЕТР
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // City name and last update are now in CityHeaderSection or directly in WeatherScreen
        /*Text(
            text = "Last update: ${
                formatUnixTimestampToDateTime(
                    currentWeather.dateTimeMillis,
                    currentWeather.timezoneOffsetSeconds
                )
            }",
            style = MaterialTheme.typography.bodySmall
        )*/
        // Spacer(modifier = Modifier.height(1.dp))
        val largeIconResId = getWeatherIconResourceId(
            iconId = currentWeather.weatherIconId,
            iconPrefix = "icon_512_"
        )
        Image(
            painter = painterResource(id = largeIconResId),
            contentDescription = currentWeather.weatherDescription,
            modifier = Modifier.size(128.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
        )
        Text(
            text = "${currentWeather.temperatureCelsius.toInt()}°${if (temperatureUnit == TemperatureUnit.CELSIUS) "C" else "F"}", // <--- ДИНАМІЧНА ОДИНИЦЯ
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = currentWeather.weatherDescription.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            },
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Feels like: ${currentWeather.feelsLikeCelsius.toInt()}°${if (temperatureUnit == TemperatureUnit.CELSIUS) "C" else "F"}",
            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        currentWeather.mlFeelsLikeCelsius?.let { mlFeels ->
            Text(
                text = "Feels like (ML): ${mlFeels.toInt()}°${if (temperatureUnit == TemperatureUnit.CELSIUS) "C" else "F"}",
                style = MaterialTheme.typography.titleSmall.copy(
                    color = MaterialTheme.colorScheme.secondary // Інший колір
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun CurrentWeatherDetailsSection(
    currentWeather: CurrentWeather,
    temperatureUnit: TemperatureUnit
) { // <--- ДОДАНО ПАРАМЕТР
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround // This will space out the two Columns
    ) {
        // Left Column for details
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f) // First column takes half the space
        ) {
            WeatherDetailItem(
                label = "Wind",
                value = "${currentWeather.windSpeedMps} ${if (temperatureUnit == TemperatureUnit.CELSIUS) "m/s" else "mph"}, ${
                    degreesToCardinalDirection(
                        currentWeather.windDirectionDegrees
                    )
                }"
            )
            WeatherDetailItem(
                label = "Pressure",
                value = "${currentWeather.pressureHpa} hPa"
            )
            WeatherDetailItem(
                label = "Sunrise",
                value = formatUnixTimestampToTime(
                    currentWeather.sunriseMillis,
                    currentWeather.timezoneOffsetSeconds
                )
            )
        }
        // Right Column for details
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f) // Second column takes the other half
        ) {
            WeatherDetailItem(
                label = "Humidity",
                value = "${currentWeather.humidityPercent}%"
            )
            WeatherDetailItem(
                label = "Visibility",
                value = "${currentWeather.visibilityMeters / 1000} km"
            )
            WeatherDetailItem(
                label = "Sunset",
                value = formatUnixTimestampToTime(
                    currentWeather.sunsetMillis,
                    currentWeather.timezoneOffsetSeconds
                )
            )
        }
    }
}

@Composable
fun WeatherDetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun SimpleHourlyForecastItemView(
    hourlyForecast: HourlyForecast,
    temperatureUnit: TemperatureUnit
) { // <--- ДОДАНО ПАРАМЕТР
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            SimpleDateFormat(
                "EEE HH:mm",
                Locale.getDefault()
            ).format(Date(hourlyForecast.dateTimeMillis)),
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(4.dp))
        val hourlyIconResId =
            getWeatherIconResourceId(
                iconId = hourlyForecast.weatherIconId,
                iconPrefix = "icon_128_"
            )
        Image(
            painter = painterResource(id = hourlyIconResId),
            contentDescription = hourlyForecast.weatherDescription,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${hourlyForecast.temperatureCelsius.toInt()}°${if (temperatureUnit == TemperatureUnit.CELSIUS) "C" else "F"}",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "POP: ${(hourlyForecast.probabilityOfPrecipitation * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

// Renamed from DailyForecastItemView to SimplifiedDailyForecastItemView
@Composable
fun SimplifiedDailyForecastItemView(
    dailyForecast: DailyForecast,
    temperatureUnit: TemperatureUnit
) {
    fun formatUnixTimestampToDay(timestampMillis: Long): String {
        if (timestampMillis == 0L) return "N/A"
        val forecastDate = Date(timestampMillis)
        val todayCalendar = Calendar.getInstance()
        val tomorrowCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        val forecastCalendar = Calendar.getInstance().apply { time = forecastDate }

        return when {
            todayCalendar.get(Calendar.YEAR) == forecastCalendar.get(Calendar.YEAR) &&
                    todayCalendar.get(Calendar.DAY_OF_YEAR) == forecastCalendar.get(Calendar.DAY_OF_YEAR) -> "Today"

            tomorrowCalendar.get(Calendar.YEAR) == forecastCalendar.get(Calendar.YEAR) &&
                    tomorrowCalendar.get(Calendar.DAY_OF_YEAR) == forecastCalendar.get(Calendar.DAY_OF_YEAR) -> "Tomorrow"

            else -> SimpleDateFormat("EEE, d/MM", Locale.getDefault()).format(forecastDate)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp), // Adjusted padding
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date
        Text(
            text = formatUnixTimestampToDay(dailyForecast.dateTimeMillis),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2.2f), // Weight for date
            textAlign = TextAlign.Start,
            maxLines = 2 // Allow date to wrap if needed
        )

        // Icon
        Box(
            modifier = Modifier.weight(1.0f), // Weight for icon
            contentAlignment = Alignment.Center
        ) {
            val dailyIconResId = getWeatherIconResourceId(
                iconId = dailyForecast.weatherIconId,
                iconPrefix = "icon_128_"
            )
            Image(
                painter = painterResource(id = dailyIconResId),
                contentDescription = dailyForecast.weatherDescription,
                modifier = Modifier.size(30.dp), // Slightly adjusted icon size
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }

        // Condition
        Text(
            text = dailyForecast.weatherCondition,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.8f), // Weight for condition
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )

        // Column for Temperatures, API POP, and ML Prediction - Aligned to End
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.weight(2.0f) // Weight for this column
        ) {
            Text(
                text = "${dailyForecast.tempMaxCelsius.toInt()}°/${dailyForecast.tempMinCelsius.toInt()}°${if (temperatureUnit == TemperatureUnit.CELSIUS) "C" else "F"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
            // API POP
            Text(
                text = "POP: ${(dailyForecast.probabilityOfPrecipitation * 100).toInt()}%", // Changed from "API POP"
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
    // HorizontalDivider is handled by the parent
}

/*// --- Helper Functions ---
fun formatUnixTimestampToDateTime(timestampMillis: Long, timezoneOffsetSeconds: Int): String {
    if (timestampMillis == 0L) return "N/A"
    val date = Date(timestampMillis)
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val customTimeZone = TimeZone.getTimeZone("GMT")
    customTimeZone.rawOffset = timezoneOffsetSeconds * 1000
    sdf.timeZone = customTimeZone
    return sdf.format(date)
}*/

fun formatUnixTimestampToTime(timestampMillis: Long, timezoneOffsetSeconds: Int): String {
    if (timestampMillis == 0L) return "N/A"
    val date = Date(timestampMillis)
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val tz = TimeZone.getTimeZone("GMT")
    tz.rawOffset = timezoneOffsetSeconds * 1000
    sdf.timeZone = tz
    return sdf.format(date)
}

fun degreesToCardinalDirection(degrees: Int): String {
    val directions = arrayOf(
        "N",
        "NNE",
        "NE",
        "ENE",
        "E",
        "ESE",
        "SE",
        "SSE",
        "S",
        "SSW",
        "SW",
        "WSW",
        "W",
        "WNW",
        "NW",
        "NNW"
    )
    return directions[(degrees / 22.5).toInt() % 16]
}

fun getAqiInterpretation(aqiValue: Float?): String {
    if (aqiValue == null) return "N/A"
    // Round to nearest integer for category mapping, or use ranges
    val roundedAqi = Math.round(aqiValue)
    return when (roundedAqi) {
        1 -> "Good"
        2 -> "Moderate"
        3 -> "Unhealthy for Sensitive" // Groups
        4 -> "Unhealthy"
        5 -> "Very Unhealthy"
        6 -> "Hazardous"
        else -> { // Handle values outside 1-6, though model should output within this if trained on it
            if (aqiValue < 1) "Good"
            else if (aqiValue > 6) "Hazardous"
            else "Unknown"
        }
    }
}

@Composable
fun getWeatherIconResourceId(iconId: String?, iconPrefix: String = "icon_"): Int {
    if (iconId.isNullOrBlank()) {
        return R.drawable.ic_weather_placeholder
    }
    val context = LocalContext.current
    val resourceName = iconPrefix + iconId.lowercase()
    val resourceId =
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    return if (resourceId != 0) resourceId else R.drawable.ic_weather_placeholder
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    WeatherMLTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Weather App Preview Placeholder", style = MaterialTheme.typography.bodyLarge)
        }
    }
}