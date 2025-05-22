// File: com/artemzarubin/weatherml/ui/Navigation.kt
package com.artemzarubin.weatherml.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.artemzarubin.weatherml.ui.mainscreen.MainViewModel
import com.artemzarubin.weatherml.ui.mainscreen.WeatherScreen
import com.artemzarubin.weatherml.ui.ManageCitiesScreen // Переконайся, що цей імпорт правильний
import com.artemzarubin.weatherml.ui.SettingsScreen   // Імпорт нового екрану

object AppDestinations {
    const val WEATHER_SCREEN_ROUTE = "weather"
    const val MANAGE_CITIES_ROUTE = "manage_cities"
    const val SETTINGS_SCREEN_ROUTE = "settings"
}

@Composable
fun AppNavHost(navController: NavHostController, mainViewModel: MainViewModel = hiltViewModel()) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.WEATHER_SCREEN_ROUTE
    ) {
        composable(AppDestinations.WEATHER_SCREEN_ROUTE) {
            WeatherScreen(
                viewModel = mainViewModel,
                onNavigateToManageCities = {
                    navController.navigate(AppDestinations.MANAGE_CITIES_ROUTE)
                },
                onNavigateToSettings = { // <--- ДОДАНО ПЕРЕДАЧУ ЦІЄЇ ЛЯМБДИ
                    navController.navigate(AppDestinations.SETTINGS_SCREEN_ROUTE)
                }
            )
        }
        composable(AppDestinations.MANAGE_CITIES_ROUTE) {
            ManageCitiesScreen(
                viewModel = mainViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        // --- НОВИЙ COMPOSABLE ДЛЯ ЕКРАНУ НАЛАШТУВАНЬ ---
        composable(AppDestinations.SETTINGS_SCREEN_ROUTE) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                viewModel = mainViewModel // <--- ПЕРЕДАЄМО mainViewModel
            )
        }
        // --- КІНЕЦЬ НОВОГО COMPOSABLE ---
    }
}