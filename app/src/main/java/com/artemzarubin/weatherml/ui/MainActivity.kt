package com.artemzarubin.weatherml.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artemzarubin.weatherml.domain.model.CurrentWeather
import com.artemzarubin.weatherml.domain.model.DailyForecast
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
        viewModel.fetchAllWeatherData(latitude = 41.643414, longitude = 41.639900)
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
                // Get insets for both status bar and navigation bar
                val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            // Add padding for the status bar at the top
                            top = systemBarsPadding.calculateTopPadding(),
                            // Add padding for the navigation bar at the bottom
                            bottom = systemBarsPadding.calculateBottomPadding()
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top // Keep content aligned to the top after padding
                ) {
                    CurrentWeatherSection(currentWeather = bundle.currentWeather)

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Hourly Forecast (Next 24 Hours)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (bundle.hourlyForecasts.isEmpty()) {
                        Text("No hourly forecast data available.")
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(bundle.hourlyForecasts) { hourlyItem ->
                                SimpleHourlyForecastItemView(hourlyItem)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Daily Forecast (Next 6 Days)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (bundle.dailyForecasts.isEmpty()) {
                        Text("No daily forecast data available.")
                    } else {
                        // The LazyColumn here already has its own scrolling, but if it's inside a
                        // non-scrollable Column, it won't help with the overall scrolling.
                        // Now that the parent Column is scrollable, everything will be fine.
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Replaced LazyColumn with Column for simplicity if dailyForecasts is not many
                            bundle.dailyForecasts.forEach { dailyItem ->
                                DailyForecastItemView(dailyItem)
                            }
                        }
                    }
                }
            } else {
                Box( // Center the error message
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Weather data is currently unavailable.")
                }
            }
        }

        is Resource.Error<*> -> {
            Box( // Center the error message
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Log.e("WeatherScreen", "Displaying Error UI. Message: ${state.message}")
                Text("Error: ${state.message}")
            }
        }
    }
}

// New Composable function for displaying the current weather section
@Composable
fun CurrentWeatherSection(currentWeather: CurrentWeather) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp) // Space between elements in this section
    ) {
        // City and last update time (optional)
        Text(
            text = "${currentWeather.cityName}${currentWeather.countryCode?.let { ", $it" } ?: ""}",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Last update: ${
                formatUnixTimestampToDateTime(
                    currentWeather.dateTimeMillis,
                    currentWeather.timezoneOffsetSeconds
                )
            }",
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Main weather info: Icon (placeholder), Temperature, Description
        // TODO: Replace Text(currentWeather.weatherIconId) with actual Pixel Art Image
        Text(
            "Icon: ${currentWeather.weatherIconId}",
            fontSize = 48.sp
        ) // Placeholder for weather icon

        Text(
            text = "${currentWeather.temperatureCelsius.toInt()}°C", // Display temp as Int for now
            fontSize = 72.sp,
            fontWeight = FontWeight.Light // Using Light fontWeight for large text
        )
        Text(
            text = currentWeather.weatherDescription.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            }, // Capitalize first letter
            fontSize = 18.sp
        )
        Text(
            text = "Feels like: ${currentWeather.feelsLikeCelsius.toInt()}°C",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant // Slightly dimmer color
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider() // Separator before details
        Spacer(modifier = Modifier.height(16.dp))

        // Detailed weather information
        WeatherDetailsGrid(currentWeather = currentWeather)
    }
}

// New Composable for displaying weather details in a grid-like manner
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
        // TODO: Add Cloudiness, UVI (from daily forecast for today) if needed
    }
}

// Helper Composable for a single detail item
@Composable
fun WeatherDetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}


// Helper function to format Unix timestamp to a readable Date and Time string
// Needs to consider the timezoneOffset from the API for accurate local time.
fun formatUnixTimestampToDateTime(timestampMillis: Long, timezoneOffsetSeconds: Int): String {
    if (timestampMillis == 0L) return "N/A"
    val date = Date(timestampMillis) // timestampMillis must be UTC in milliseconds
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) // e.g., May 12, 15:45

    // Create a TimeZone based on an offset from UTC
    val customTimeZone = TimeZone.getTimeZone("GMT")
    customTimeZone.rawOffset = timezoneOffsetSeconds * 1000 // Set the offset in milliseconds
    sdf.timeZone = customTimeZone // Set this timezone for SimpleDateFormat

    return sdf.format(date)
}

// Helper function to format Unix timestamp to a readable Time string
fun formatUnixTimestampToTime(timestampMillis: Long, timezoneOffsetSeconds: Int): String {
    if (timestampMillis == 0L) return "N/A"
    // timestampMillis MUST ALREADY BE in milliseconds (i.e., DTO.dt * 1000L)
    // timezoneOffsetSeconds is the offset of local time relative to UTC in seconds
    // To get the local time, we don't need to add timezoneOffset to the UTC time,
    // if SimpleDateFormat is already set to the device's local timezone.
    // Or, if we want to display the time for that location,
    // we need to create a TimeZone with that offset.

    val date = Date(timestampMillis) // timestampMillis should already be in UTC milliseconds
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    // Set the time zone for offset-based formatting
    // This is a more reliable way than just adding seconds to the timestamp
    val tz = TimeZone.getTimeZone("GMT") // Start with GMT
    tz.rawOffset = timezoneOffsetSeconds * 1000 // Set the offset in milliseconds
    sdf.timeZone = tz
    return sdf.format(date)
}

// Helper function to convert wind degrees to cardinal direction
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


// SimpleHourlyForecastItemView remains the same for now
@Composable
fun SimpleHourlyForecastItemView(hourlyForecast: com.artemzarubin.weatherml.domain.model.HourlyForecast) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Text(
            SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(Date(hourlyForecast.dateTimeMillis))
        )
        Text("${hourlyForecast.temperatureCelsius.toInt()}°C")
        Text(hourlyForecast.weatherCondition, fontSize = 12.sp)
        Text("POP: ${(hourlyForecast.probabilityOfPrecipitation * 100).toInt()}%", fontSize = 12.sp)
    }
}

// --- START: New Composable for Daily Forecast Item ---
@Composable
fun DailyForecastItemView(dailyForecast: DailyForecast) {
    // Helper function to format Unix timestamp to a readable day of the week or date
    fun formatUnixTimestampToDay(timestampMillis: Long): String {
        if (timestampMillis == 0L) return "N/A"
        val date = Date(timestampMillis)
        // You can choose your desired format, e.g., "EEE" for "Mon", "EEEE" for "Monday", "MMM d" for "May 12"
        val sdf = SimpleDateFormat("EEE, MMM d", Locale.getDefault()) // Example: "Mon, May 12"
        return sdf.format(date)
    }

    Row( // Display daily forecast items in a Row for better layout
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // Fill width and add some padding
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Distribute space
    ) {
        Text(
            text = formatUnixTimestampToDay(dailyForecast.dateTimeMillis),
            modifier = Modifier.weight(1.5f), // Give more weight to the date
            fontSize = 16.sp
        )
        // TODO: Add Pixel Art Icon here based on dailyForecast.weatherIconId
        Text(
            text = "Icon: ${dailyForecast.weatherIconId}", // Placeholder for icon
            modifier = Modifier.weight(1f),
            fontSize = 14.sp
        )
        Text(
            text = dailyForecast.weatherCondition,
            modifier = Modifier.weight(2f), // More weight for description
            fontSize = 14.sp,
            maxLines = 1 // Ensure description doesn't wrap excessively
        )
        Text(
            text = "${dailyForecast.tempMaxCelsius.toInt()}°/${dailyForecast.tempMinCelsius.toInt()}°",
            modifier = Modifier.weight(1f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
    HorizontalDivider() // Add a divider after each daily item
}
// --- END: New Composable for Daily Forecast Item ---

// DefaultPreview remains the same
@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    WeatherMLTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Weather App Preview Placeholder")
        }
    }
}