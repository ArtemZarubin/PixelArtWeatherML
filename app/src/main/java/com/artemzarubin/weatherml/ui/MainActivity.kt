package com.artemzarubin.weatherml.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.artemzarubin.weatherml.R
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.domain.model.CurrentWeather
import com.artemzarubin.weatherml.domain.model.DailyForecast
import com.artemzarubin.weatherml.domain.model.HourlyForecast
import com.artemzarubin.weatherml.ui.common.PixelArtCard
import com.artemzarubin.weatherml.ui.mainscreen.MainViewModel
import com.artemzarubin.weatherml.ui.theme.WeatherMLTheme
import com.artemzarubin.weatherml.util.Resource
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
        // enableEdgeToEdge() // Kept commented out, Scaffold will handle insets
        setContent {
            WeatherMLTheme {
                Surface( // Surface is now inside WeatherMLTheme, which is good
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background // Main background for the app
                ) {
                    WeatherScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: MainViewModel = hiltViewModel()) {
    val weatherBundleState by viewModel.weatherDataState.collectAsState()
    val autocompleteResultsState by viewModel.autocompleteResults.collectAsState()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    var showPermissionRationale by rememberSaveable { mutableStateOf(false) }
    var permissionsRequestedThisSession by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showSearchUi by rememberSaveable { mutableStateOf(false) }

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
            // TopAppBar is shown only when not in search mode AND weather data is successfully loaded (or no error related to permissions)
            val canShowTopBar = !showSearchUi &&
                    (weatherBundleState is Resource.Success<*> ||
                            (weatherBundleState is Resource.Error<*> && weatherBundleState.message?.contains(
                                "permission",
                                ignoreCase = true
                            ) != true))

            if (canShowTopBar) {
                Surface( // Wrap TopAppBar in Surface to apply shadowElevation
                    shadowElevation = 3.dp, // Your desired shadow
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // Background for TopAppBar area
                ) {
                    TopAppBar(
                        title = {
                            val currentWeatherData =
                                (weatherBundleState as? Resource.Success)?.data?.currentWeather
                            // Use showSearchUi here, which is defined in WeatherScreen's scope
                            val displayTitle = if (showSearchUi) { // <--- USE showSearchUi HERE
                                "Search Location"
                            } else {
                                currentWeatherData?.let {
                                    "${it.cityName}${it.countryCode?.let { code -> ", $code" } ?: ""}"
                                } ?: "WeatherML"
                            }
                            Text(
                                text = displayTitle,
                                style = MaterialTheme.typography.headlineSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        actions = {
                            IconButton(onClick = { showSearchUi = true }) {
                                Icon(
                                    Icons.Filled.Add,
                                    "Search City",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                ) // Adjusted tint
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent // Make TopAppBar itself transparent
                        )
                    )
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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PixelatedSunLoader(); Spacer(modifier = Modifier.height(16.dp))
                            currentContentState.message?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodyMedium
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
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    // Permission error UI with button is handled by the overlay Box at the end
                }

                is Resource.Success<*> -> {
                    val bundle = currentContentState.data
                    if (bundle != null && !showSearchUi) { // Show weather content only if not in search mode
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
                                    if (bundle.hourlyForecasts.isEmpty()) { /* ... */
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
                                    if (bundle.dailyForecasts.isEmpty()) { /* ... */
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
                    } else if (bundle == null && !showSearchUi) {
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

            // --- Search UI Overlay ---
            if (showSearchUi) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(WindowInsets.systemBars.asPaddingValues()) // Insets for edge-to-edge inside search
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Search Location", style = MaterialTheme.typography.headlineSmall)
                            IconButton(onClick = {
                                showSearchUi = false; searchQuery =
                                ""; viewModel.clearGeocodingResults(); keyboardController?.hide(); focusManager.clearFocus()
                            }) {
                                Icon(
                                    Icons.Filled.Close,
                                    "Close Search",
                                    tint = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { newText ->
                                searchQuery =
                                    newText; if (newText.length >= 3) viewModel.searchCityAutocomplete(
                                newText
                            ) else if (newText.isBlank()) viewModel.clearGeocodingResults()
                            },
                            label = {
                                Text(
                                    "Enter city name",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyLarge,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                if (searchQuery.isNotBlank()) viewModel.searchCityAutocomplete(
                                    searchQuery
                                ); keyboardController?.hide(); focusManager.clearFocus()
                            })
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        val currentAutocompleteState = autocompleteResultsState
                        if (searchQuery.length >= 3) {
                            when (currentAutocompleteState) {
                                is Resource.Loading -> {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .padding(vertical = 8.dp)
                                    )
                                }

                                is Resource.Success -> {
                                    val locations = currentAutocompleteState.data
                                    if (!locations.isNullOrEmpty()) {
                                        LazyColumn(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 150.dp)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                )
                                        ) { // Added weight(1f) and min height
                                            items(locations) { feature ->
                                                LocationSearchResultItem(feature = feature) {
                                                    viewModel.onCitySuggestionSelected(feature)
                                                    searchQuery = ""
                                                    showSearchUi = false // Close search UI
                                                    keyboardController?.hide(); focusManager.clearFocus()
                                                }
                                                if (locations.last() != feature) HorizontalDivider(
                                                    color = MaterialTheme.colorScheme.outline.copy(
                                                        alpha = 0.5f
                                                    )
                                                )
                                            }
                                        }
                                    } else if (currentAutocompleteState.data != null) {
                                        Text(
                                            "No cities found for \"$searchQuery\"",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }

                                is Resource.Error -> {
                                    currentAutocompleteState.message?.let {
                                        if (it != "City name cannot be empty.") {
                                            Text(
                                                "Search Error: $it",
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (!(searchQuery.length >= 3 && autocompleteResultsState is Resource.Success && !(autocompleteResultsState as Resource.Success<List<GeoapifyFeatureDto>>).data.isNullOrEmpty())) {
                            Spacer(modifier = Modifier.weight(1f)) // Pushes content up if list is short or not present
                        }
                    }
                }
            }

            // --- Permission Error UI Overlay (should be the topmost if active and search is not active) ---
            if (!showSearchUi && weatherBundleState is Resource.Error && weatherBundleState.message?.contains(
                    "permission",
                    ignoreCase = true
                ) == true && showPermissionRationale
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.98f))
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
                                    locationPermissionLauncher.launch(locationPermissions)
                                }
                                showPermissionRationale = false
                            },
                            shape = RoundedCornerShape(4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(
                                if (isPermanentlyDenied) "Open Settings" else "Retry Permissions",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }
            }
        } // End of main screen Box (content of Scaffold)
    } // End of Scaffold
}

@Composable
fun LocationSearchResultItem(
    feature: GeoapifyFeatureDto,
    onClick: () -> Unit
) {
    val properties = feature.properties
    val displayName = listOfNotNull(
        properties?.city,
        properties?.state,
        properties?.countryCode?.uppercase()
    ).joinToString(", ").ifBlank { properties?.formattedAddress ?: "Unknown location" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Update CurrentWeatherMainSection to NOT include city name and last update
@Composable
fun CurrentWeatherMainSection(currentWeather: CurrentWeather) {
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
            text = "${currentWeather.temperatureCelsius.toInt()}°C",
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
            text = "Feels like: ${currentWeather.feelsLikeCelsius.toInt()}°C",
            style = MaterialTheme.typography.titleSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
fun CurrentWeatherDetailsSection(currentWeather: CurrentWeather) {
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
                value = "${currentWeather.windSpeedMps} m/s, ${
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
fun SimpleHourlyForecastItemView(hourlyForecast: HourlyForecast) {
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
            "${hourlyForecast.temperatureCelsius.toInt()}°C",
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
fun SimplifiedDailyForecastItemView(dailyForecast: DailyForecast) {
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
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatUnixTimestampToDay(dailyForecast.dateTimeMillis),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2.5f),
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.width(8.dp))

        Box(modifier = Modifier.weight(1.2f), contentAlignment = Alignment.Center) {
            val dailyIconResId = getWeatherIconResourceId(
                iconId = dailyForecast.weatherIconId,
                iconPrefix = "icon_128_"
            )
            Image(
                painter = painterResource(id = dailyIconResId),
                contentDescription = dailyForecast.weatherDescription,
                modifier = Modifier.size(32.dp),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = dailyForecast.weatherCondition,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(2f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start
        )
        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${dailyForecast.tempMaxCelsius.toInt()}°/${dailyForecast.tempMinCelsius.toInt()}°",
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.weight(2f),
            textAlign = TextAlign.End
        )
    }
    // HorizontalDivider is now handled by the parent Column's forEachIndexed
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