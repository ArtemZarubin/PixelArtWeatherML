package com.artemzarubin.weatherml.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.artemzarubin.weatherml.ui.mainscreen.MainViewModel
import com.artemzarubin.weatherml.ui.theme.WeatherMLTheme
import com.artemzarubin.weatherml.util.Resource
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val weatherBundleState by viewModel.weatherDataState.collectAsState() // New StateFlow

    LaunchedEffect(key1 = Unit) {
        Log.d("WeatherScreen", "LaunchedEffect triggered. Fetching all weather data...")
        viewModel.fetchAllWeatherData(
            latitude = 41.643414,
            longitude = 41.639900
        ) // Calling a new method
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        when (val state = weatherBundleState) {
            is Resource.Loading<*> -> {
                Log.d("WeatherScreen", "Displaying Loading UI")
                CircularProgressIndicator()
            }

            is Resource.Success<*> -> {
                val bundle = state.data
                Log.d("WeatherScreen", "Displaying Success UI. Bundle: $bundle")
                if (bundle != null) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Displaying the current weather with bundle.currentWeather
                        Text("Current Weather", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("City: ${bundle.currentWeather.cityName}")
                        Text("Temp: ${bundle.currentWeather.temperatureCelsius}°C")
                        Text("Condition: ${bundle.currentWeather.weatherCondition} (${bundle.currentWeather.weatherDescription})")

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        // Displaying hourly forecasts with bundle.hourlyForecasts
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
                                    // Create or adapt HourlyForecastItemView for HourlyForecast (Domain Model)
                                    SimpleHourlyForecastItemView(hourlyItem)
                                }
                            }
                        }
                        // TODO: Add daily forecast display with bundle.dailyForecasts
                    }
                } else {
                    Text("Weather data is currently unavailable.")
                }
            }

            is Resource.Error<*> -> {
                Log.e("WeatherScreen", "Displaying Error UI. Message: ${state.message}")
                Text("Error: ${state.message}")
            }
        }
    }
}

// Simple Composable for displaying hourly forecast (Domain Model)
@Composable
fun SimpleHourlyForecastItemView(hourlyForecast: com.artemzarubin.weatherml.domain.model.HourlyForecast) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
        Text(
            SimpleDateFormat(
                "HH:mm",
                Locale.getDefault()
            ).format(Date(hourlyForecast.dateTimeMillis * 1000L))
        )
        Text("${hourlyForecast.temperatureCelsius}°C")
        Text(hourlyForecast.weatherCondition, fontSize = 12.sp)
        Text("POP: ${(hourlyForecast.probabilityOfPrecipitation * 100).toInt()}%", fontSize = 12.sp)
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    WeatherMLTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Weather App Preview Placeholder")
        }
    }
}