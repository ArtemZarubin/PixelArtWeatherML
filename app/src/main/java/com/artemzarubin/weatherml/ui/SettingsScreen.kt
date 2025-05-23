// File: com/artemzarubin/weatherml/ui/SettingsScreen.kt
package com.artemzarubin.weatherml.ui

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
    viewModel: MainViewModel = hiltViewModel() // Отримуємо ViewModel
) {
    val userPreferences by viewModel.userPreferencesFlow.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.labelLarge
                    )
                }, // Твій стиль
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Image(
                            painter = painterResource(id = R.drawable.arrow_left), // Твоя іконка
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            Text("Temperature Units", style = MaterialTheme.typography.headlineSmall) // Твій стиль
            Spacer(modifier = Modifier.height(10.dp)) // Збільшив відступ після заголовка

            // --- Опція Цельсій ---
            Row(
                modifier = Modifier
                    .fillMaxWidth() // Щоб клікабельна область була на всю ширину
                    .clickable { viewModel.updateTemperatureUnit(TemperatureUnit.CELSIUS) }
                    .padding(vertical = 4.dp), // Менший вертикальний відступ для рядка
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isSelected =
                    userPreferences.temperatureUnit == TemperatureUnit.CELSIUS // Або FAHRENHEIT
                val iconResId = if (isSelected) R.drawable.radio_checked
                else R.drawable.radio_unchecked
                val iconSize = if (isSelected) 24.dp else 18.dp

                Image(
                    painter = painterResource(
                        id = iconResId
                    ),
                    contentDescription = "Celsius radio button",
                    modifier = Modifier.size(24.dp), // Або 32.dp, як у тебе було для інших іконок
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface) // Адаптація до теми
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Celsius (°C)",
                    style = MaterialTheme.typography.bodyLarge, // Стиль для тексту опції
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            // Spacer(modifier = Modifier.height(2.dp)) // Дуже маленький відступ між опціями, або взагалі без нього

            // --- Опція Фаренгейт ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.updateTemperatureUnit(TemperatureUnit.FAHRENHEIT) }
                    .padding(vertical = 4.dp), // Менший вертикальний відступ для рядка
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isSelected = userPreferences.temperatureUnit == TemperatureUnit.FAHRENHEIT
                val iconResId = if (isSelected) R.drawable.radio_checked
                else R.drawable.radio_unchecked
                val iconSize = if (isSelected) 24.dp else 18.dp

                Image(
                    painter = painterResource(
                        id = iconResId
                    ),
                    contentDescription = "Fahrenheit radio button",
                    modifier = Modifier.size(24.dp), // Або .size(24.dp), якщо співвідношення сторін 1:1 і це виглядає добре,
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Fahrenheit (°F)",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            // --- РОЗДІЛЬНИК ---
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp), // Залишаємо відступи
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )

            // --- СЕКЦІЯ ВИБОРУ ТЕМИ ---
            Text("App Theme", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(10.dp))

            val appThemes = listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK)
            appThemes.forEach { themeOption ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateAppTheme(themeOption) } // Викликаємо метод ViewModel
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(
                            id = if (userPreferences.appTheme == themeOption) R.drawable.radio_checked // Твоя іконка "вибрано"
                            else R.drawable.radio_unchecked // Твоя іконка "не вибрано"
                        ),
                        contentDescription = "${themeOption.name} theme radio button",
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = themeOption.name.lowercase()
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }, // Робимо першу літеру великою
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
            // --- КІНЕЦЬ СЕКЦІЇ ВИБОРУ ТЕМИ ---
        }
    }
}