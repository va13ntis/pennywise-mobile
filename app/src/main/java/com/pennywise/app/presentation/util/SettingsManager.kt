package com.pennywise.app.presentation.util

import android.content.Context
import com.pennywise.app.data.util.SettingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling application settings and preferences
 * This class delegates to SettingsDataStore to avoid duplication
 */
@Singleton
class SettingsManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) {
    
    /**
     * Save the user's language preference
     */
    suspend fun saveLanguagePreference(languageCode: String) {
        
        // Save to DataStore via SettingsDataStore
        settingsDataStore.setLanguage(languageCode)
        
        // Also save to SharedPreferences for app startup compatibility
        val sharedPrefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("language", languageCode).apply()
    }
    
    /**
     * Get the stored language preference
     */
    fun getLanguagePreference(): Flow<String> {
        return settingsDataStore.language
    }
    
    /**
     * Save the user's currency preference
     */
    suspend fun saveCurrencyPreference(currencyCode: String) {
        
        // Save to DataStore via SettingsDataStore
        settingsDataStore.setCurrency(currencyCode)
        
        // Also save to SharedPreferences for app startup compatibility
        val sharedPrefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("currency", currencyCode).apply()
    }
    
    /**
     * Get the stored currency preference
     */
    fun getCurrencyPreference(): Flow<String> {
        return settingsDataStore.currency
    }
}
