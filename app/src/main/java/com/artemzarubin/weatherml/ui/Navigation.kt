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
}

@Composable
fun AppNavHost(navController: NavHostController, mainViewModel: MainViewModel = hiltViewModel()) {
    NavHost(
        navController = navController,
        startDestination = AppDestinations.WEATHER_SCREEN_ROUTE // Починаємо з головного екрану
    ) {
        composable(AppDestinations.WEATHER_SCREEN_ROUTE) {
            WeatherScreen(
                viewModel = mainViewModel, // Передаємо ViewModel
                onNavigateToManageCities = {
                    navController.navigate(AppDestinations.MANAGE_CITIES_ROUTE)
                }
            )
        }
        composable(AppDestinations.MANAGE_CITIES_ROUTE) {
            // Тут буде ManageCitiesScreen, поки що заглушка
            ManageCitiesScreen(
                viewModel = mainViewModel, // Передаємо ту саму ViewModel
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

