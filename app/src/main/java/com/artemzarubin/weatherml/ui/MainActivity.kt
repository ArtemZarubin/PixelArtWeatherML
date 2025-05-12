package com.artemzarubin.weatherml.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artemzarubin.weatherml.R
import com.artemzarubin.weatherml.domain.model.CurrentWeather
import com.artemzarubin.weatherml.domain.model.DailyForecast
import com.artemzarubin.weatherml.domain.model.HourlyForecast
import com.artemzarubin.weatherml.ui.mainscreen.MainViewModel
import com.artemzarubin.weatherml.ui.theme.WeatherMLTheme
import com.artemzarubin.weatherml.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherMLTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeatherScreen()
                }
            }
        }
    }
}

@Composable
fun WeatherScreen(viewModel: MainViewModel = hiltViewModel()) {
    val weatherBundleState by viewModel.weatherDataState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        Log.d("WeatherScreen", "LaunchedEffect triggered. Fetching all weather data...")
        // Using New York coordinates for testing, replace with actual location logic later
        viewModel.fetchAllWeatherData(latitude = 41.639412, longitude = 41.628371)
    }

    when (val state = weatherBundleState) {
        is Resource.Loading<*> -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is Resource.Success<*> -> {
            val bundle = state.data
            if (bundle != null) {
                val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = systemBarsPadding.calculateTopPadding() + 16.dp, // Added 16.dp general top padding
                            bottom = systemBarsPadding.calculateBottomPadding() + 16.dp // Added 16.dp general bottom padding
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    CurrentWeatherSection(currentWeather = bundle.currentWeather)

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp)) // Reduced space before title

                    Text(
                        "Hourly Forecast (Next 24 Hours)",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (bundle.hourlyForecasts.isEmpty()) {
                        Text(
                            "No hourly forecast data available.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { // Reduced spacing
                            items(bundle.hourlyForecasts) { hourlyItem ->
                                SimpleHourlyForecastItemView(hourlyItem)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp)) // Reduced space before title

                    Text(
                        "Daily Forecast (Next ${bundle.dailyForecasts.size} Days)", // Dynamic day count
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (bundle.dailyForecasts.isEmpty()) {
                        Text(
                            "No daily forecast data available.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // Reduced spacing
                            bundle.dailyForecasts.forEach { dailyItem ->
                                DailyForecastItemView(dailyItem)
                            }
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Weather data is currently unavailable.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        is Resource.Error<*> -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Log.e("WeatherScreen", "Displaying Error UI. Message: ${state.message}")
                Text("Error: ${state.message}", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
fun CurrentWeatherSection(currentWeather: CurrentWeather) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${currentWeather.cityName}${currentWeather.countryCode?.let { ", $it" } ?: ""}",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Last update: ${
                formatUnixTimestampToDateTime(
                    currentWeather.dateTimeMillis,
                    currentWeather.timezoneOffsetSeconds
                )
            }",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(16.dp))

        val largeIconResId = getWeatherIconResourceId(
            iconId = currentWeather.weatherIconId,
            iconPrefix = "icon_128_" // Prefix for 128x128 icons
        )
        Image(
            painter = painterResource(id = largeIconResId),
            contentDescription = currentWeather.weatherDescription,
            modifier = Modifier.size(128.dp),
            contentScale = ContentScale.Fit,
            // filterQuality = FilterQuality.None,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground) // Tinting the icon
        )

        Text(
            text = "${currentWeather.temperatureCelsius.toInt()}°C",
            style = MaterialTheme.typography.titleMedium // Using style for main temperature
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
            text = "Feels like: ${currentWeather.feelsLikeCelsius.toInt()}°C",
            style = MaterialTheme.typography.titleSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(16.dp))
        WeatherDetailsGrid(currentWeather = currentWeather)
    }
}

@Composable
fun WeatherDetailsGrid(currentWeather: CurrentWeather) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WeatherDetailItem(
                label = "Wind",
                value = "${currentWeather.windSpeedMps} m/s, ${
                    degreesToCardinalDirection(currentWeather.windDirectionDegrees)
                }"
            )
            WeatherDetailItem(label = "Humidity", value = "${currentWeather.humidityPercent}%")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WeatherDetailItem(label = "Pressure", value = "${currentWeather.pressureHpa} hPa")
            WeatherDetailItem(
                label = "Visibility",
                value = "${currentWeather.visibilityMeters / 1000} km"
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            WeatherDetailItem(
                label = "Sunrise",
                value = formatUnixTimestampToTime(
                    currentWeather.sunriseMillis,
                    currentWeather.timezoneOffsetSeconds
                )
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

fun formatUnixTimestampToDateTime(timestampMillis: Long, timezoneOffsetSeconds: Int): String {
    if (timestampMillis == 0L) return "N/A"
    val date = Date(timestampMillis)
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    val customTimeZone = TimeZone.getTimeZone("GMT")
    customTimeZone.rawOffset = timezoneOffsetSeconds * 1000
    sdf.timeZone = customTimeZone
    return sdf.format(date)
}

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

@Composable
fun SimpleHourlyForecastItemView(hourlyForecast: HourlyForecast) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp) // Added some padding
    ) {
        Text(
            SimpleDateFormat("EEE HH:mm", Locale.getDefault())
                .format(Date(hourlyForecast.dateTimeMillis)),
            style = MaterialTheme.typography.labelSmall
        )
        Spacer(modifier = Modifier.height(4.dp))

        val hourlyIconResId = getWeatherIconResourceId(
            iconId = hourlyForecast.weatherIconId,
            iconPrefix = "icon_" // Standard prefix for 24x24 icons
        )
        Image(
            painter = painterResource(id = hourlyIconResId),
            contentDescription = hourlyForecast.weatherDescription,
            modifier = Modifier.size(24.dp),
            contentScale = ContentScale.Fit,
            // filterQuality = FilterQuality.None,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
        )
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            "${hourlyForecast.temperatureCelsius.toInt()}°C",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            "POP: ${(hourlyForecast.probabilityOfPrecipitation * 100).toInt()}%",
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun DailyForecastItemView(dailyForecast: DailyForecast) {
    fun formatUnixTimestampToDay(timestampMillis: Long): String {
        if (timestampMillis == 0L) return "N/A"
        val date = Date(timestampMillis)
        val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        return sdf.format(date)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Increased vertical padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatUnixTimestampToDay(dailyForecast.dateTimeMillis),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f) // Adjusted weight
        )
        val dailyIconResId = getWeatherIconResourceId(
            iconId = dailyForecast.weatherIconId,
            iconPrefix = "icon_" // Standard prefix for 24x24 icons
        )
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(id = dailyIconResId),
                contentDescription = dailyForecast.weatherDescription,
                modifier = Modifier.size(24.dp),
                contentScale = ContentScale.Fit,
                // filterQuality = FilterQuality.Low,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }
        Text(
            text = dailyForecast.weatherCondition,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1.5f), // Adjusted weight
            maxLines = 1
        )
        Text(
            text = "${dailyForecast.tempMaxCelsius.toInt()}°/${dailyForecast.tempMinCelsius.toInt()}°",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
}

// Helper function to get drawable resource ID from icon string
@Composable
fun getWeatherIconResourceId(iconId: String?, iconPrefix: String = "icon_"): Int {
    if (iconId.isNullOrBlank()) {
        return R.drawable.ic_weather_placeholder // Your placeholder drawable
    }
    val context = LocalContext.current
    val resourceName = iconPrefix + iconId.lowercase()
    val resourceId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
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