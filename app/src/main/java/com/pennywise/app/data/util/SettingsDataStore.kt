package com.pennywise.app.data.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pennywise.app.presentation.viewmodel.SettingsViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized DataStore for all app settings to prevent multiple DataStore instances
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val themeKey = stringPreferencesKey("theme_mode")
    
    /**
     * Flow of the current theme mode from DataStore
     */
    val themeMode: Flow<SettingsViewModel.ThemeMode> = context.settingsDataStore.data
        .map { preferences ->
            val themeModeString = preferences[themeKey] ?: SettingsViewModel.ThemeMode.SYSTEM.name
            try {
                SettingsViewModel.ThemeMode.valueOf(themeModeString)
            } catch (e: IllegalArgumentException) {
                SettingsViewModel.ThemeMode.SYSTEM
            }
        }
    
    /**
     * Set the theme mode and save it to DataStore
     */
    suspend fun setThemeMode(themeMode: SettingsViewModel.ThemeMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[themeKey] = themeMode.name
        }
    }
    
    companion object {
        private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_preferences")
    }
}
