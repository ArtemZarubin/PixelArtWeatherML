package com.artemzarubin.weatherml.ui

// import androidx.compose.material.icons.filled.CheckCircle // Ти використовуєш кастомні
// import androidx.compose.material.icons.outlined.RadioButtonUnchecked // Ти використовуєш кастомні
// import androidx.compose.material.icons.filled.Delete // Ти використовуєш кастомну
// import androidx.compose.ui.tooling.data.EmptyGroup.location // <--- ВИДАЛЕНО ЦЕЙ НЕПРАВИЛЬНИЙ ІМПОРТ
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.ColorFilter
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
import com.artemzarubin.weatherml.ui.mainscreen.PagerItem
import com.artemzarubin.weatherml.util.Resource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCitiesScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val pagerItemsList by viewModel.pagerItems.collectAsState()
    val savedLocationPages = pagerItemsList.filterIsInstance<PagerItem.SavedPage>()
    val currentActivePagerItem by viewModel.currentActivePagerItem.collectAsState()
    val autocompleteResultsState by viewModel.autocompleteResults.collectAsState()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Locations", style = MaterialTheme.typography.labelLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_left),
                            contentDescription = "Back to Weather",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
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
                    if (newText.length >= 3) viewModel.searchCityAutocomplete(newText)
                    else if (newText.isBlank()) viewModel.clearGeocodingResults()
                },
                label = { Text("Enter city name", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (searchQuery.isNotBlank()) viewModel.searchCityAutocomplete(searchQuery)
                    keyboardController?.hide(); focusManager.clearFocus()
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
                            LazyColumn {
                                items(
                                    locations,
                                    key = { it.properties?.placeId ?: it.hashCode() }) { feature ->
                                    LocationSearchResultItem(feature = feature) {
                                        viewModel.onCitySuggestionSelected(feature)
                                        searchQuery = ""
                                        viewModel.clearGeocodingResults()
                                        keyboardController?.hide(); focusManager.clearFocus()
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
            if (savedLocationPages.isEmpty()) {
                Text("No locations saved yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(
                        savedLocationPages,
                        key = { savedPageItem -> savedPageItem.id }) { savedPageItem ->
                        SavedLocationRow(
                            location = savedPageItem.location,
                            // isActive = currentActivePagerItem?.id == savedPageItem.id, // <--- ЦЕЙ РЯДОК ВИДАЛЯЄМО
                            onSelect = {
                                Log.d(
                                    "ManageCitiesScreen",
                                    "Location selected: ${savedPageItem.location.cityName}"
                                )
                                viewModel.setCurrentPagerItemToSavedLocation(savedPageItem.location) // Викликаємо метод ViewModel
                                onNavigateBack()
                            },
                            onDelete = {
                                viewModel.deleteLocationAndUpdatePager(savedPageItem.location.id)
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
    location: SavedLocation, // Тип параметра - SavedLocation
    // isActive: Boolean, // БІЛЬШЕ НЕ ПОТРІБЕН
    onSelect: () -> Unit,    // Клік по рядку все ще вибирає локацію і переходить на неї
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect) // Клік по всьому рядку для вибору
            .padding(
                vertical = 12.dp,
                horizontal = 16.dp
            ), // Збільшив горизонтальний відступ, оскільки немає іконки зліва
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Іконку чекбокса (IconButton з activeIconResId) ВИДАЛЕНО
        // Spacer(modifier = Modifier.width(16.dp)) // Цей Spacer теж можна прибрати або зменшити

        Text(
            text = "${location.cityName}${location.countryCode?.let { ", $it" } ?: ""}",
            style = MaterialTheme.typography.bodyLarge, // Або твій стиль
            modifier = Modifier.weight(1f), // Займає доступний простір
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Залишаємо тільки іконку видалення
        IconButton(onClick = onDelete) {
            Image(
                painter = painterResource(id = R.drawable.trash_bin), // Твоя піксельна іконка смітника
                contentDescription = "Delete ${location.cityName}",
                modifier = Modifier.size(32.dp), // Або 24.dp, залежно від твого дизайну
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground) // Або інший колір, якщо потрібно
            )
        }
    }
}

// LocationSearchResultItem - переконайся, що він визначений тут або правильно імпортований
@Composable
fun LocationSearchResultItem(
    feature: GeoapifyFeatureDto,
    onClick: () -> Unit
) {
    val properties = feature.properties
    val displayName = listOfNotNull(
        properties?.city,
        properties?.state,
        properties?.county,
        properties?.country
    ).distinct().joinToString(", ").ifBlank { properties?.formattedAddress ?: "Unknown location" }

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