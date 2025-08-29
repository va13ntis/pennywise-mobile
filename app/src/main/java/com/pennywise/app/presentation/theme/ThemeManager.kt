package com.pennywise.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pennywise.app.data.util.SettingsDataStore
import com.pennywise.app.presentation.viewmodel.SettingsViewModel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling theme changes throughout the app
 */
@Singleton
class ThemeManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    
    /**
     * Flow of the current theme mode from DataStore
     */
    val themeMode = settingsDataStore.themeMode
    
    /**
     * Composable function that applies the current theme based on user preferences
     */
    @Composable
    fun ApplyTheme(content: @Composable () -> Unit) {
        val themeMode by themeMode.collectAsState(initial = SettingsViewModel.ThemeMode.SYSTEM)
        val systemIsDark = isSystemInDarkTheme()
        
        val isDarkTheme = when (themeMode) {
            SettingsViewModel.ThemeMode.LIGHT -> false
            SettingsViewModel.ThemeMode.DARK -> true
            SettingsViewModel.ThemeMode.SYSTEM -> systemIsDark
        }
        
        MaterialTheme(
            colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
