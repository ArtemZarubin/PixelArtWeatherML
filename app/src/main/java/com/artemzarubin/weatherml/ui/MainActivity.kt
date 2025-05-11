package com.artemzarubin.weatherml.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artemzarubin.weatherml.ui.mainscreen.MainViewModel
import com.artemzarubin.weatherml.ui.theme.WeatherMLTheme
import com.artemzarubin.weatherml.util.Resource
import dagger.hilt.android.AndroidEntryPoint

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
    // Use the correct StateFlow name from ViewModel
    val weatherState by viewModel.currentWeatherState.collectAsState()

    LaunchedEffect(key1 = Unit) {
        Log.d("WeatherScreen", "LaunchedEffect triggered. Fetching weather...")
        // Use the correct function name and parameter names
        viewModel.fetchCurrentWeather(
            latitude = 41.639433,
            longitude = 41.628576
        ) // Coordinates for Batumi as an example
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (val state = weatherState) {
            is Resource.Loading<*> -> { // Add <*> for star projection
                Log.d("WeatherScreen", "Displaying Loading UI")
                CircularProgressIndicator()
            }

            is Resource.Success<*> -> { // Add <*> for star projection
                val weatherData = state.data // data is of type CurrentWeatherResponseDto?
                Log.d("WeatherScreen", "Displaying Success UI. Data: $weatherData")

                weatherData?.let { data -> // Safe call for weatherData
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        data.cityName?.let { city -> // Safe call for cityName
                            Text("City: $city")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        data.main?.temp?.let { temp -> // Safe call for main and temp
                            Text("Temp: $temp°C")
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        data.weather?.firstOrNull()
                            ?.let { condition -> // Safe call for weather list and its first element
                                Text("Condition: ${condition.main} (${condition.description})")
                            }
                    }
                } ?: run {
                    // This block executes if weatherData is null, even if state is Success
                    // (e.g. if Resource.Success(null) was emitted, though less likely with current setup)
                    Log.d("WeatherScreen", "Success state, but weather data is null or incomplete.")
                    Text("Weather data is currently unavailable.")
                }
            }

            is Resource.Error<*> -> { // Add <*> for star projection
                Log.e("WeatherScreen", "Displaying Error UI. Message: ${state.message}")
                Text("Error: ${state.message}")
            }
        }
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