// File: com/artemzarubin/weatherml/ui/Navigation.kt
package com.artemzarubin.weatherml.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.artemzarubin.weatherml.ui.mainscreen.MainViewModel
import com.artemzarubin.weatherml.ui.mainscreen.WeatherScreen

object AppDestinations {
    const val WEATHER_SCREEN_ROUTE = "weather"
    const val MANAGE_CITIES_ROUTE = "manage_cities"
    const val SETTINGS_SCREEN_ROUTE = "settings"
    const val ABOUT_SCREEN_ROUTE = "about"
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

        composable(AppDestinations.SETTINGS_SCREEN_ROUTE) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                // Додаємо лямбду для переходу на екран "Про застосунок"
                onNavigateToAbout = { navController.navigate(AppDestinations.ABOUT_SCREEN_ROUTE) }, // <--- НОВЕ
                viewModel = mainViewModel // <--- ДОДАНО ПЕРЕДАЧУ VIEWMODEL
            )
        }


        // --- НОВИЙ COMPOSABLE ДЛЯ ЕКРАНУ "ПРО ЗАСТОСУНОК" ---
        composable(AppDestinations.ABOUT_SCREEN_ROUTE) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}