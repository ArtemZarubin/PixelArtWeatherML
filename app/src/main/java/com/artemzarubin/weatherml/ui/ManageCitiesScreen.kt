// File: com/artemzarubin/weatherml/ui/ManageCitiesScreen.kt
package com.artemzarubin.weatherml.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.artemzarubin.weatherml.R
import com.artemzarubin.weatherml.data.remote.dto.GeoapifyFeatureDto
import com.artemzarubin.weatherml.domain.model.SavedLocation
import com.artemzarubin.weatherml.ui.mainscreen.MainViewModel
import com.artemzarubin.weatherml.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCitiesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val savedLocations by viewModel.savedLocations.collectAsState()
    val activeLocation by viewModel.activeWeatherLocation.collectAsState()
    val autocompleteResultsState by viewModel.autocompleteResults.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Manage Locations",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_left),
                            contentDescription = "Back to Weather",
                            modifier = Modifier.size(24.dp),
                            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                                MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Search Section ---
            Text("Add New Location", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = searchQuery,
                onValueChange = { newText ->
                    searchQuery = newText
                    if (newText.length >= 3) {
                        viewModel.searchCityAutocomplete(newText)
                    } else if (newText.isBlank()) {
                        viewModel.clearGeocodingResults()
                    }
                },
                label = { Text("Enter city name", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (searchQuery.isNotBlank()) viewModel.searchCityAutocomplete(searchQuery)
                    keyboardController?.hide()
                    focusManager.clearFocus()
                })
            )
            Spacer(modifier = Modifier.height(8.dp))

            // --- Autocomplete Results ---
            val currentAutocompleteState = autocompleteResultsState
            if (searchQuery.length >= 3) {
                when (currentAutocompleteState) {
                    is Resource.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(vertical = 8.dp)
                        )
                    }

                    is Resource.Success -> {
                        val locations = currentAutocompleteState.data
                        if (!locations.isNullOrEmpty()) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp) // Обмеження висоти списку результатів
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                            ) {
                                items(locations) { feature ->
                                    LocationSearchResultItem(feature = feature) {
                                        viewModel.onCitySuggestionSelected(feature) // Це додасть і зробить активним
                                        searchQuery = "" // Очищаємо пошук
                                        viewModel.clearGeocodingResults()
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        // Можна автоматично повернутися назад: onNavigateBack()
                                    }
                                    if (locations.last() != feature) HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outline.copy(
                                            alpha = 0.5f
                                        )
                                    )
                                }
                            }
                        } else if (currentAutocompleteState.data != null) { // Порожній успішний результат
                            Text(
                                "No cities found for \"$searchQuery\"",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    is Resource.Error -> {
                        currentAutocompleteState.message?.let {
                            if (it != "City name cannot be empty.") { // Не показуємо цю специфічну помилку тут
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
            Spacer(modifier = Modifier.height(16.dp))

            // --- Saved Locations List ---
            Text("Saved Locations", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (savedLocations.isEmpty()) {
                Text("No locations saved yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(savedLocations, key = { it.id }) { location ->
                        SavedLocationRow(
                            location = location,
                            isActive = location.id == activeLocation?.id,
                            onSelect = {
                                viewModel.selectActiveLocation(location.id)
                                onNavigateBack() // Повертаємося на головний екран після вибору
                            },
                            onDelete = {
                                viewModel.deleteLocation(location.id)
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
fun SavedLocationRow(
    location: SavedLocation,
    isActive: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Іконка активності (чекбокс)
        val activeIconResId = if (isActive) R.drawable.add_button else R.drawable.alert_symbol
        val activeIconDescription = if (isActive) "Active location" else "Set as active"
        IconButton(onClick = onSelect) {
            Image(
                painter = painterResource(id = activeIconResId),
                contentDescription = activeIconDescription,
                modifier = Modifier.size(32.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = "${location.cityName}${location.countryCode?.let { ", $it" } ?: ""}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        IconButton(onClick = onDelete) {
            Image(
                painter = painterResource(id = R.drawable.trash_bin),
                contentDescription = "Delete ${location.cityName}",
                modifier = Modifier.size(32.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
            )
        }
    }
}

// LocationSearchResultItem - якщо ти його ще не створив як окремий Composable,
// ось приклад, який ти використовував раніше. Переконайся, що він доступний тут.
@Composable
fun LocationSearchResultItem(
    feature: GeoapifyFeatureDto,
    onClick: () -> Unit
) {
    val properties = feature.properties
    // Формуємо більш повну назву
    val displayName = listOfNotNull(
        properties?.city,
        properties?.state,
        properties?.county,
        properties?.country // Використовуємо повну назву країни
    ).distinct().joinToString(", ").ifBlank { properties?.formattedAddress ?: "Unknown location" }
    // distinct() - щоб уникнути повторень, якщо name і city однакові

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