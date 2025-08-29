package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.data.util.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing app settings including theme preferences
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {
    
    /**
     * Enum representing the available theme modes
     */
    enum class ThemeMode {
        LIGHT, DARK, SYSTEM
    }
    
    /**
     * Flow of the current theme mode from DataStore
     */
    val themeMode: Flow<ThemeMode> = settingsDataStore.themeMode
    
    /**
     * Set the theme mode and save it to DataStore
     */
    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(themeMode)
        }
    }
}
