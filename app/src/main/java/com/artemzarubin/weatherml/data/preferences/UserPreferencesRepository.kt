package com.artemzarubin.weatherml.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Визначаємо DataStore на рівні файлу (рекомендовано)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

// Ключі для наших налаштувань
private object PreferencesKeys {
    val IS_FAHRENHEIT = booleanPreferencesKey("is_fahrenheit_selected")
    val APP_THEME = stringPreferencesKey("app_theme_preference") // <--- НОВИЙ КЛЮЧ ДЛЯ ТЕМИ
}

enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT
}

// --- НОВИЙ ENUM ДЛЯ ТЕМИ ---
enum class AppTheme {
    SYSTEM, LIGHT, DARK
}

data class UserPreferences(
    val temperatureUnit: TemperatureUnit,
    val appTheme: AppTheme // <--- НОВЕ ПОЛЕ
)

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("UserPrefsRepo", "Error reading preferences.", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapUserPreferences(preferences)
        }

    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        val isFahrenheit = preferences[PreferencesKeys.IS_FAHRENHEIT] ?: false

        // Отримуємо збережену тему, за замовчуванням - SYSTEM
        val themeName = preferences[PreferencesKeys.APP_THEME] ?: AppTheme.SYSTEM.name
        val appTheme = try {
            AppTheme.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppTheme.SYSTEM // Fallback, якщо збережено невалідну назву
        }

        return UserPreferences(
            temperatureUnit = if (isFahrenheit) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS,
            appTheme = appTheme // <--- ВИКОРИСТОВУЄМО ОТРИМАНУ ТЕМУ
        )
    }

    suspend fun updateTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FAHRENHEIT] = (unit == TemperatureUnit.FAHRENHEIT)
        }
        Log.d("UserPrefsRepo", "Temperature unit updated to: $unit")
    }

    // --- НОВИЙ МЕТОД ДЛЯ ОНОВЛЕННЯ ТЕМИ ---
    suspend fun updateAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_THEME] = theme.name // Зберігаємо назву enum як String
        }
        Log.d("UserPrefsRepo", "App theme updated to: $theme")
    }
    // --- КІНЕЦЬ НОВОГО МЕТОДУ ---
}