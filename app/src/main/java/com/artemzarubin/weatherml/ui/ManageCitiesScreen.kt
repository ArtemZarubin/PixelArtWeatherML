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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
    val canAddLocation by viewModel.canAddNewLocation.collectAsState()

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
                enabled = canAddLocation, // <--- ДЕАКТИВУЄМО ПОЛЕ
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (searchQuery.isNotBlank()) viewModel.searchCityAutocomplete(searchQuery)
                    keyboardController?.hide(); focusManager.clearFocus()
                })
            )
            if (!canAddLocation) {
                Text(
                    "You have reached the maximum number of saved locations (${MainViewModel.MAX_SAVED_LOCATIONS}).",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
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
                LazyColumn(modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)) {
                    // ВИКОРИСТОВУЄМО itemsIndexed
                    itemsIndexed(
                        items = savedLocationPages,
                        key = { _, savedPageItem -> savedPageItem.id } // Ключ тепер приймає (index, item)
                    ) { index, savedPageItem -> // Тепер index та savedPageItem доступні
                        SavedLocationRow(
                            location = savedPageItem.location,
                            onMoveUp = {
                                if (index > 0) {
                                    viewModel.moveSavedLocation(index, index - 1)
                                }
                            },
                            onMoveDown = {
                                if (index < savedLocationPages.size - 1) {
                                    viewModel.moveSavedLocation(index, index + 1)
                                }
                            },
                            onSelect = {
                                Log.d(
                                    "ManageCitiesScreen",
                                    "Location selected: ${savedPageItem.location.cityName}"
                                )
                                viewModel.setCurrentPagerItemToSavedLocation(savedPageItem.location)
                                onNavigateBack()
                            },
                            onDelete = {
                                viewModel.deleteLocationAndUpdatePager(savedPageItem.location.id)
                            },
                            canMoveUp = (index > 0),
                            canMoveDown = (index < savedLocationPages.size - 1)
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
    onMoveUp: () -> Unit,    // Нова лямбда
    onMoveDown: () -> Unit,  // Нова лямбда
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    canMoveUp: Boolean,      // Чи можна рухати вгору
    canMoveDown: Boolean     // Чи можна рухати вниз
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // .clickable(onClick = onSelect) // Клік по всьому рядку тепер не потрібен, якщо є кнопки
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Кнопки для зміни порядку ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = onMoveUp,
                enabled = canMoveUp,
                modifier = Modifier.size(32.dp) // Однаковий розмір для кнопок
            ) {
                Image(
                    painter = painterResource(id = R.drawable.arrow_up), // Твоя іконка "Вгору"
                    contentDescription = "Move Up",
                    colorFilter = ColorFilter.tint(
                        if (canMoveUp) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                )
            }
            IconButton(
                onClick = onMoveDown,
                enabled = canMoveDown,
                modifier = Modifier.size(32.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.arrow_down), // Твоя іконка "Вниз"
                    contentDescription = "Move Down",
                    colorFilter = ColorFilter.tint(
                        if (canMoveDown) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))

        // --- Назва міста (клікабельна для переходу) ---
        Text(
            text = "${location.cityName}${location.countryCode?.let { ", $it" } ?: ""}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSelect) // Клік по тексту для вибору
                .padding(vertical = 8.dp, horizontal = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        // Spacer(modifier = Modifier.width(8.dp)) // Можна прибрати, якщо Text займає weight(1f)

        // --- Кнопка видалення ---
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Image(
                painter = painterResource(id = R.drawable.trash_bin), // Твоя іконка
                contentDescription = "Delete ${location.cityName}",
                modifier = Modifier.size(24.dp), // Зробимо трохи меншою, ніж кнопки порядку
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground)
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