package com.pennywise.app.data.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.pennywise.app.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized DataStore for all app settings to prevent multiple DataStore instances
 */
@Singleton
class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationContext private val context: Context
) {
    
    private val themeKey = stringPreferencesKey("theme_mode")
    private val languageKey = stringPreferencesKey("language")
    private val currencyKey = stringPreferencesKey("currency")
    
    /**
     * Flow of the current theme mode from DataStore
     */
    val themeMode: Flow<SettingsViewModel.ThemeMode> = dataStore.data
        .map { preferences ->
            val themeModeString = preferences[themeKey] ?: SettingsViewModel.ThemeMode.SYSTEM.name
            try {
                // Handle both uppercase enum names and lowercase strings
                when (themeModeString.lowercase()) {
                    "light" -> SettingsViewModel.ThemeMode.LIGHT
                    "dark" -> SettingsViewModel.ThemeMode.DARK
                    "system" -> SettingsViewModel.ThemeMode.SYSTEM
                    else -> SettingsViewModel.ThemeMode.valueOf(themeModeString)
                }
            } catch (e: IllegalArgumentException) {
                SettingsViewModel.ThemeMode.SYSTEM
            }
        }
    
    /**
     * Get the current theme mode as a string
     */
    suspend fun getThemeMode(): String {
        val preferences = dataStore.data.first()
        return preferences[themeKey] ?: "system"
    }
    
    /**
     * Flow of the current language from DataStore
     */
    val language: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[languageKey] ?: ""
        }
    
    /**
     * Get the current language
     */
    suspend fun getLanguage(): String {
        val preferences = dataStore.data.first()
        return preferences[languageKey] ?: ""
    }
    
    /**
     * Flow of the current currency from DataStore
     */
    val currency: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[currencyKey] ?: ""
        }
    
    /**
     * Get the current currency
     */
    suspend fun getCurrency(): String {
        val preferences = dataStore.data.first()
        return preferences[currencyKey] ?: ""
    }
    
    /**
     * Set the theme mode and save it to DataStore
     */
    suspend fun setThemeMode(themeMode: SettingsViewModel.ThemeMode) {
        dataStore.edit { preferences ->
            preferences[themeKey] = themeMode.name
        }
    }
    
    /**
     * Set the theme mode as a string and save it to DataStore
     */
    suspend fun setThemeMode(themeMode: String) {
        dataStore.edit { preferences ->
            preferences[themeKey] = themeMode
        }
    }
    
    /**
     * Set the language and save it to DataStore
     */
    suspend fun setLanguage(languageCode: String) {
        dataStore.edit { preferences ->
            preferences[languageKey] = languageCode
        }
    }
    
    /**
     * Set the currency and save it to DataStore
     */
    suspend fun setCurrency(currencyCode: String) {
        dataStore.edit { preferences ->
            preferences[currencyKey] = currencyCode
        }
    }
    
}
