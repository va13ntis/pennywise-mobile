package com.pennywise.app

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.pennywise.app.presentation.util.LocaleManager
import com.pennywise.app.presentation.util.SettingsManager
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
    lateinit var localeManager: LocaleManager
    
    @Inject
    lateinit var settingsManager: SettingsManager
    
    @Inject
    lateinit var dataStore: DataStore<Preferences>
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Timber.plant(Timber.DebugTree())
        }
        Timber.d("PennyWise Application initialized")
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applyPreferredLocale()
    }
    
    override fun attachBaseContext(base: Context) {
        // Apply saved locale before creating the base context
        val contextWithLocale = applySavedLocale(base)
        super.attachBaseContext(contextWithLocale)
    }
    
    private fun applyPreferredLocale() {
        try {
            // Re-apply saved locale when configuration changes
            val languageCode = runBlocking { settingsManager.getLanguagePreference().first() }
            Timber.d("Re-applying saved locale: $languageCode")
            val resources = resources
            val configuration = Configuration(resources.configuration)
            
            val locale = resolveAppLocale(languageCode, this)
            
            Locale.setDefault(locale)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.setLocales(android.os.LocaleList(locale))
            } else {
                @Suppress("DEPRECATION")
                configuration.locale = locale
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                configuration.setLayoutDirection(locale)
            }
            
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply preferred locale")
        }
    }
    
    private fun applySavedLocale(context: Context): Context {
        return try {
            // Use SharedPreferences for initial locale detection to avoid DataStore conflicts
            val sharedPrefs = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
            val languageCode = sharedPrefs.getString("language", "")
            
            Timber.d("Applying saved locale at app startup: $languageCode")
            
            if (!languageCode.isNullOrEmpty()) {
                // Apply the saved locale
                val locale = resolveAppLocale(languageCode, context)
                
                // Update the configuration
                val configuration = Configuration(context.resources.configuration)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    configuration.setLocales(android.os.LocaleList(locale))
                } else {
                    @Suppress("DEPRECATION")
                    configuration.locale = locale
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    configuration.setLayoutDirection(locale)
                }
                
                // Set default locale
                Locale.setDefault(locale)
                
                // Create context with new configuration
                context.createConfigurationContext(configuration)
            } else {
                context
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply saved locale: ${e.message}")
            context
        }
    }

    /**
     * Resolves the app locale from a language code and guarantees we only use
     * locales supported by the app to avoid accidental RTL layout flips.
     */
    private fun resolveAppLocale(languageCode: String?, context: Context? = null): Locale {
        return when {
            languageCode.equals("en", ignoreCase = true) -> Locale("en")
            languageCode.equals("iw", ignoreCase = true) || languageCode.equals("he", ignoreCase = true) -> Locale("iw")
            languageCode.equals("ru", ignoreCase = true) -> Locale("ru")
            else -> mapSystemLocaleToSupportedLocale(context)
        }
    }

    /**
     * Maps the device locale to a supported app locale.
     * App startup should not depend on device locale for layout direction.
     * Unsupported or missing language settings default to English (LTR).
     */
    private fun mapSystemLocaleToSupportedLocale(context: Context?): Locale {
        val systemLanguage = context?.let { safeContext ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                safeContext.resources.configuration.locales[0]?.language
            } else {
                @Suppress("DEPRECATION")
                safeContext.resources.configuration.locale?.language
            }
        } ?: Locale.getDefault().language

        return when {
            systemLanguage.equals("ru", ignoreCase = true) -> Locale("ru")
            systemLanguage.equals("he", ignoreCase = true) ||
                systemLanguage.equals("iw", ignoreCase = true) -> Locale("iw")
            systemLanguage.equals("en", ignoreCase = true) -> Locale("en")
            else -> Locale("en")
        }
    }
}
