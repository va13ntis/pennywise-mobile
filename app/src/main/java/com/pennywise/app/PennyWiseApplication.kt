package com.pennywise.app

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Build
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

/**
 * Main Application class for PennyWise
 */
@HiltAndroidApp
class PennyWiseApplication : Application() {
    
    @Inject
    lateinit var localeManager: com.pennywise.app.presentation.util.LocaleManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("PennyWise Application initialized")
    }
    
    override fun attachBaseContext(base: Context) {
        // Apply saved locale before creating the base context
        val contextWithLocale = applySavedLocale(base)
        super.attachBaseContext(contextWithLocale)
    }
    
    private fun applySavedLocale(context: Context): Context {
        return try {
            // Get saved language preference
            val preferences = context.settingsDataStore.data
            val languageCode = runBlocking { preferences.first()[stringPreferencesKey("language")] ?: "" }
            
            if (languageCode.isNotEmpty()) {
                // Apply the saved locale
                val locale = when (languageCode) {
                    "en" -> Locale("en")
                    "iw" -> Locale("iw")
                    "ru" -> Locale("ru")
                    else -> Locale.getDefault()
                }
                
                // Update the configuration
                val configuration = Configuration(context.resources.configuration)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    configuration.setLocales(android.os.LocaleList(locale))
                } else {
                    @Suppress("DEPRECATION")
                    configuration.locale = locale
                }
                
                // Set default locale
                Locale.setDefault(locale)
                
                // Create context with new configuration
                context.createConfigurationContext(configuration)
            } else {
                context
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply saved locale")
            context
        }
    }
    
    companion object {
        private val Context.settingsDataStore: androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore(name = "settings_preferences")
    }
}
