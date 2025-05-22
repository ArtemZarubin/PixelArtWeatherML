package com.artemzarubin.weatherml.data.preferences

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
    // Тут можна буде додати інші ключі для інших налаштувань
}

enum class TemperatureUnit {
    CELSIUS, FAHRENHEIT
}

data class UserPreferences(
    val temperatureUnit: TemperatureUnit
)

@Singleton
class UserPreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) {

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                Log.e("UserPrefsRepo", "Error reading preferences.", exception)
                emit(emptyPreferences()) // Випромінюємо порожні налаштування у випадку помилки
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapUserPreferences(preferences)
        }

    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        val isFahrenheit =
            preferences[PreferencesKeys.IS_FAHRENHEIT] ?: false // За замовчуванням Цельсій
        return UserPreferences(
            temperatureUnit = if (isFahrenheit) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
        )
    }

    suspend fun updateTemperatureUnit(unit: TemperatureUnit) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_FAHRENHEIT] = (unit == TemperatureUnit.FAHRENHEIT)
        }
        Log.d("UserPrefsRepo", "Temperature unit updated to: $unit")
    }
}