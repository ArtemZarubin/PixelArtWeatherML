package com.artemzarubin.weatherml.ui.mainscreen

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.artemzarubin.weatherml.R
import com.artemzarubin.weatherml.domain.model.WeatherDataBundle
import com.artemzarubin.weatherml.ui.CurrentWeatherDetailsSection
import com.artemzarubin.weatherml.ui.CurrentWeatherMainSection
import com.artemzarubin.weatherml.ui.PixelatedSunLoader
import com.artemzarubin.weatherml.ui.SimpleHourlyForecastItemView
import com.artemzarubin.weatherml.ui.SimplifiedDailyForecastItemView
import com.artemzarubin.weatherml.ui.common.PixelArtCard
import com.artemzarubin.weatherml.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    viewModel: MainViewModel = hiltViewModel(),
    onNavigateToManageCities: () -> Unit
) {
    val weatherBundleState by viewModel.weatherDataState.collectAsState()
    val autocompleteResultsState by viewModel.autocompleteResults.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    var permissionsRequestedThisSession by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            permissionsRequestedThisSession = true
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
            if (fineLocationGranted || coarseLocationGranted) {
                showPermissionRationale = false; viewModel.initiateWeatherFetch()
            } else {
                val activity = context as? ComponentActivity
                val userPermanentlyDenied = locationPermissions.any { perm ->
                    ContextCompat.checkSelfPermission(
                        context,
                        perm
                    ) == PackageManager.PERMISSION_DENIED && activity?.shouldShowRequestPermissionRationale(
                        perm
                    ) == false
                }
                viewModel.setPermissionError(if (userPermanentlyDenied) "Location permission permanently denied. Please enable it in app settings." else "Location permission denied.")
                showPermissionRationale = true
            }
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                val allGranted = locationPermissions.all {
                    ContextCompat.checkSelfPermission(
                        context,
                        it
                    ) == PackageManager.PERMISSION_GRANTED
                }
                if (allGranted) {
                    showPermissionRationale = false; viewModel.initiateWeatherFetch()
                } else if (!permissionsRequestedThisSession) {
                    locationPermissionLauncher.launch(locationPermissions)
                } else {
                    showPermissionRationale = true
                    if (weatherBundleState.message?.contains("permanently denied") != true && weatherBundleState.message?.contains(
                            "permission denied",
                            ignoreCase = true
                        ) != true
                    ) {
                        viewModel.setPermissionError("Location permission is needed.")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            if (weatherBundleState is Resource.Success<*>) {
                (weatherBundleState as Resource.Success<WeatherDataBundle>).data?.let { bundle ->
                    Surface(
                        shadowElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                    ) {
                        TopAppBar(
                            title = {
                                val displayTitle = bundle.currentWeather.let {
                                    "${it.cityName}${it.countryCode?.let { code -> ", $code" } ?: ""}"
                                } // Тепер беремо з bundle.currentWeather, оскільки ми в Success стані
                                Text(
                                    text = displayTitle,
                                    style = MaterialTheme.typography.headlineSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            actions = {
                                IconButton(onClick = onNavigateToManageCities) {
                                    Image( // <--- ЗАМІНА ICON НА IMAGE
                                        painter = painterResource(id = R.drawable.list_option),
                                        contentDescription = "Manage Locations",
                                        modifier = Modifier.size(32.dp),
                                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                            MaterialTheme.colorScheme.onBackground
                                        )
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { scaffoldPaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPaddingValues)
        ) {
            // --- UI Display Logic based on weatherBundleState ---
            when (val currentContentState = weatherBundleState) {
                is Resource.Loading<*> -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            PixelatedSunLoader()
                            Spacer(modifier = Modifier.height(16.dp))
                            currentContentState.message?.let { messageText -> // Дамо змінній ім'я, щоб було зрозуміліше
                                Text(
                                    text = messageText, // Використовуємо message з Resource.Loading
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Justify, // <--- ВСТАНОВЛЮЄМО ЦЕНТРУВАННЯ
                                    modifier = Modifier.padding(horizontal = 32.dp) // Додаємо горизонтальні відступи, щоб довгий текст переносився і центрувався
                                )
                            }
                        }
                    }
                }

                is Resource.Error<*> -> {
                    if (!(showPermissionRationale && currentContentState.message?.contains(
                            "permission",
                            ignoreCase = true
                        ) == true)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Error: ${currentContentState.message ?: "Unknown error"}",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Justify,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    // Permission error UI with button is handled by the overlay Box at the end
                }

                is Resource.Success<*> -> {
                    val bundle = currentContentState.data
                    if (bundle != null) { // Show weather content only if not in search mode
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                ), // Adjusted vertical padding
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // CityHeaderSection is effectively replaced by TopAppBar content
                            // --- Current Weather Main Info Section ---
                            CurrentWeatherMainSection(currentWeather = bundle.currentWeather) // This will no longer contain city name

                            // --- Hourly Forecast Section ---
                            PixelArtCard(
                                modifier = Modifier.fillMaxWidth(),
                                internalPadding = 8.dp,
                                borderWidth = 2.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Hourly Forecast (Next 24 Hours)",
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                                    )
                                    if (bundle.hourlyForecasts.isEmpty()) {
                                        Text(
                                            "Hourly forecast data is currently unavailable.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            items(bundle.hourlyForecasts) {
                                                SimpleHourlyForecastItemView(
                                                    it
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // --- Daily Forecast Section ---
                            PixelArtCard(
                                modifier = Modifier.fillMaxWidth(),
                                internalPadding = 8.dp,
                                borderWidth = 2.dp
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "Daily Forecast (Next ${bundle.dailyForecasts.size} Days)",
                                        style = MaterialTheme.typography.headlineSmall,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                                    )
                                    if (bundle.dailyForecasts.isEmpty()) {
                                        Text(
                                            "Daily forecast data is currently unavailable.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                            bundle.dailyForecasts.forEachIndexed { index, dailyItem ->
                                                SimplifiedDailyForecastItemView(
                                                    dailyItem
                                                ); if (index < bundle.dailyForecasts.size - 1) {
                                                HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outline.copy(
                                                        alpha = 0.5f
                                                    )
                                                )
                                            }
                                            }
                                        }
                                    }
                                }
                            }

                            // --- Current Weather Details Section ---
                            PixelArtCard(
                                modifier = Modifier.fillMaxWidth(),
                                internalPadding = 16.dp,
                                borderWidth = 2.dp
                            ) {
                                CurrentWeatherDetailsSection(currentWeather = bundle.currentWeather)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Weather data is currently unavailable.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // --- Permission Error UI Overlay (should be the topmost if active and search is not active) ---
            if (weatherBundleState is Resource.Error &&
                weatherBundleState.message?.contains("permission", ignoreCase = true) == true &&
                showPermissionRationale
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f)) // Напівпрозорий фон
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Error: ${weatherBundleState.message}",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Justify,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val isPermanentlyDenied =
                            weatherBundleState.message!!.contains("permanently denied")
                        Button(
                            onClick = {
                                if (isPermanentlyDenied) {
                                    // Open app settings
                                    val intent =
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    val uri = Uri.fromParts("package", context.packageName, null)
                                    intent.data = uri
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Log.e("WeatherScreen", "Could not open app settings", e)
                                    }
                                } else {
                                    // Retry permission request
                                    locationPermissionLauncher.launch(locationPermissions)
                                }
                                showPermissionRationale = false // Сховуємо це UI після дії
                            },
                            shape = RoundedCornerShape(4.dp), // Твій піксельний стиль
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(
                                if (isPermanentlyDenied) "Open Settings" else "Grant Permission", // Змінено текст кнопки
                                style = MaterialTheme.typography.headlineSmall // Або інший відповідний стиль
                            )
                        }
                    }
                }
            } // End of main screen Box (content of Scaffold)
        } // End of Scaffold
    }
}