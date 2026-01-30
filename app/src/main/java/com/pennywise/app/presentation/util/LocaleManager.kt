package com.pennywise.app.presentation.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for handling locale changes and providing utility functions
 * for updating the app's locale throughout the application
 */
@Singleton
class LocaleManager @Inject constructor() {
    
    /**
     * Update the app's locale based on the provided language code
     * @param context The application context
     * @param languageCode The language code (e.g., "en", "iw", "ru")
     * @return The updated context with the new locale
     */
    fun updateLocale(context: Context, languageCode: String): Context {
        val locale = when (languageCode) {
            "en" -> Locale("en")
            "iw" -> Locale("iw")
            "ru" -> Locale("ru")
            else -> Locale.getDefault()
        }
        
        return updateResources(context, locale)
    }
    
    /**
     * Update the app's locale to the system default
     * @param context The application context
     * @return The updated context with the system locale
     */
    fun updateLocaleToSystem(context: Context): Context {
        return updateResources(context, Locale.getDefault())
    }
    
    /**
     * Get the current locale from the context
     * @param context The application context
     * @return The current locale
     */
    fun getCurrentLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
    
    /**
     * Update the resources configuration with the new locale
     * @param context The application context
     * @param locale The new locale to apply
     * @return The updated context
     */
    private fun updateResources(context: Context, locale: Locale): Context {
        // Set the default locale for the entire app
        Locale.setDefault(locale)
        
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
        
        // Create a new context with the updated configuration
        val updatedContext = context.createConfigurationContext(configuration)
        
        // Also update the resources configuration directly
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
        
        return updatedContext
    }
    
    /**
     * Check if the provided language code is supported
     * @param languageCode The language code to check
     * @return True if the language is supported, false otherwise
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return languageCode in listOf("en", "iw", "ru")
    }
    
    /**
     * Get the display name for a language code
     * @param languageCode The language code
     * @return The display name of the language
     */
    fun getLanguageDisplayName(languageCode: String): String {
        return when (languageCode) {
            "en" -> "English"
            "iw" -> "עברית"
            "ru" -> "Русский"
            else -> "System Default"
        }
    }
    
    /**
     * Detect the device's system locale and return the best supported language code
     * @param context The application context
     * @return The detected locale code that is supported by the app
     */
    fun detectDeviceLocale(context: Context): String {
        val systemLocale = getCurrentLocale(context)
        val systemLanguage = systemLocale.language
        
        return when {
            isLanguageSupported(systemLanguage) -> systemLanguage
            systemLanguage.startsWith("he") -> "iw" // Hebrew variants
            systemLanguage.startsWith("ru") -> "ru" // Russian variants
            systemLanguage.startsWith("en") -> "en" // English variants
            else -> "en" // Default to English
        }
    }
    
    /**
     * Get all supported locales with their display names
     * @return Map of locale codes to display names
     */
    fun getSupportedLocales(): Map<String, String> {
        return mapOf(
            "en" to "English",
            "iw" to "עברית",
            "ru" to "Русский"
        )
    }
}
