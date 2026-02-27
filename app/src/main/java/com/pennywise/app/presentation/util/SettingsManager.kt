package com.pennywise.app.presentation.util

import com.pennywise.app.data.util.SettingsDataStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling application settings and preferences
 * This class delegates to SettingsDataStore to avoid duplication
 */
@Singleton
class SettingsManager @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    
    /**
     * Save the user's language preference
     */
    suspend fun saveLanguagePreference(languageCode: String) {
        
        // SettingsDataStore handles DataStore + startup SharedPreferences sync.
        settingsDataStore.setLanguage(languageCode)
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
        
        // SettingsDataStore handles DataStore + startup SharedPreferences sync.
        settingsDataStore.setCurrency(currencyCode)
    }
    
    /**
     * Get the stored currency preference
     */
    fun getCurrencyPreference(): Flow<String> {
        return settingsDataStore.currency
    }
}
