// File: com/artemzarubin/weatherml/ui/SettingsScreen.kt
package com.artemzarubin.weatherml.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.artemzarubin.weatherml.R
import com.artemzarubin.weatherml.data.preferences.AppTheme
import com.artemzarubin.weatherml.data.preferences.TemperatureUnit
import com.artemzarubin.weatherml.ui.mainscreen.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit, // <--- НОВИЙ ПАРАМЕТР
    viewModel: MainViewModel = hiltViewModel()
) {
    val userPreferences by viewModel.userPreferencesFlow.collectAsState()
    val context = LocalContext.current

    val versionName = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName
    } catch (e: Exception) {
        Log.e("SettingsScreen", "Could not get package info", e)
        "N/A"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_left),
                            contentDescription = "Back",
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
        // Box теперь содержит и Column с настройками, и Text с версией
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // Применяем padding от Scaffold к Box
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize() // Column занимает все пространство Box (кроме места для Text версии)
                    .padding(16.dp), // Внутренний padding для содержимого Column
            ) {
                Text(
                    "Temperature Units",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateTemperatureUnit(TemperatureUnit.CELSIUS) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSelected = userPreferences.temperatureUnit == TemperatureUnit.CELSIUS
                    Image(
                        painter = painterResource(
                            id = if (isSelected) R.drawable.radio_checked else R.drawable.radio_unchecked
                        ),
                        contentDescription = "Celsius radio button",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Celsius (°C)",
                        style = MaterialTheme.typography.bodyLarge,
                        // modifier = Modifier.align(Alignment.CenterVertically) // Это не нужно, т.к. Row уже verticalAlignment = Alignment.CenterVertically
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateTemperatureUnit(TemperatureUnit.FAHRENHEIT) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSelected = userPreferences.temperatureUnit == TemperatureUnit.FAHRENHEIT
                    Image(
                        painter = painterResource(
                            id = if (isSelected) R.drawable.radio_checked else R.drawable.radio_unchecked
                        ),
                        contentDescription = "Fahrenheit radio button",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fahrenheit (°F)",
                        style = MaterialTheme.typography.bodyLarge,
                        // modifier = Modifier.align(Alignment.CenterVertically) // Аналогично
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                Text("App Theme", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(10.dp))

                val appThemes = listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK)
                appThemes.forEach { themeOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateAppTheme(themeOption) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (userPreferences.appTheme == themeOption) R.drawable.radio_checked
                                else R.drawable.radio_unchecked
                            ),
                            contentDescription = "${themeOption.name} theme radio button",
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = themeOption.name.lowercase()
                                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            style = MaterialTheme.typography.bodyLarge,
                            // modifier = Modifier.align(Alignment.CenterVertically) // Аналогично
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                // --- ПУНКТ "ПРО ЗАСТОСУНОК" ---
                Text(
                    "About",
                    style = MaterialTheme.typography.headlineSmall
                ) // Заголовок для секції
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToAbout() } // <--- ВИКЛИКАЄМО ЛЯМБДУ
                        .padding(vertical = 4.dp), // Відступ як у інших опцій
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.question), // Твоя піксельна іконка "info"
                        contentDescription = "About Pixel Weather",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "About Pixel Weather",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                // --- КІНЕЦЬ ПУНКТУ "ПРО ЗАСТОСУНОК" ---

            } // Конец Column

            // Текст версии теперь прямой дочерний элемент Box
            Text(
                text = "App Version: $versionName",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter) // Теперь это выравнивает Text по низу Box
                    .padding(bottom = 16.dp) // Можно настроить отступ от низа
            )
        } // Конец Box
    } // Конец Scaffold
}