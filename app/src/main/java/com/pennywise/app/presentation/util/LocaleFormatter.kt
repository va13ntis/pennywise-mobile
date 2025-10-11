package com.pennywise.app.presentation.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.text.DateFormat
import java.util.*

/**
 * Utility class for locale-aware formatting of currency and dates
 * Handles RTL languages properly with appropriate currency symbols and date formats
 * Supports currency conversion with original and converted amounts
 */
object LocaleFormatter {
    
    /**
     * Format currency amount based on selected currency preference
     * Falls back to language-based currency if no preference is set
     */
    fun formatCurrency(amount: Double, @Suppress("UNUSED_PARAMETER") context: Context, selectedCurrency: String = ""): String {
        // If a specific currency is selected, use it
        if (selectedCurrency.isNotEmpty()) {
            return formatCurrencyByCode(amount, selectedCurrency)
        }
        
        // Otherwise, fall back to language-based currency
        val locale = Locale.getDefault()
        return when (locale.language) {
            "iw" -> formatHebrewCurrency(amount)
            "ru" -> formatRussianCurrency(amount)
            else -> formatEnglishCurrency(amount)
        }
    }
    
    /**
     * Format currency by currency code
     */
    private fun formatCurrencyByCode(amount: Double, currencyCode: String): String {
        return when (currencyCode.uppercase()) {
            "USD" -> "$${String.format("%.2f", kotlin.math.abs(amount))}"
            "EUR" -> "€${String.format("%.2f", kotlin.math.abs(amount))}"
            "GBP" -> "£${String.format("%.2f", kotlin.math.abs(amount))}"
            "ILS" -> "₪${String.format("%.2f", kotlin.math.abs(amount))}"
            "RUB" -> "₽${String.format("%.2f", kotlin.math.abs(amount))}"
            "JPY" -> "¥${String.format("%.0f", kotlin.math.abs(amount))}" // No decimals for JPY
            "CAD" -> "C$${String.format("%.2f", kotlin.math.abs(amount))}"
            "AUD" -> "A$${String.format("%.2f", kotlin.math.abs(amount))}"
            "CHF" -> "CHF ${String.format("%.2f", kotlin.math.abs(amount))}"
            "CNY" -> "¥${String.format("%.2f", kotlin.math.abs(amount))}"
            "INR" -> "₹${String.format("%.2f", kotlin.math.abs(amount))}"
            else -> "$${String.format("%.2f", kotlin.math.abs(amount))}" // Default to USD
        }
    }
    
    /**
     * Format currency for Hebrew locale with ₪ symbol
     */
    private fun formatHebrewCurrency(amount: Double): String {
        return "₪${String.format("%.2f", kotlin.math.abs(amount))}"
    }
    
    /**
     * Format currency for Russian locale with ₽ symbol
     */
    private fun formatRussianCurrency(amount: Double): String {
        return "₽${String.format("%.2f", kotlin.math.abs(amount))}"
    }
    
    /**
     * Format currency for English locale with $ symbol
     */
    private fun formatEnglishCurrency(amount: Double): String {
        return "$${String.format("%.2f", kotlin.math.abs(amount))}"
    }
    
    /**
     * Format transaction date based on system date format preference
     * Uses multiple methods to detect the system's actual date format setting
     */
    fun formatTransactionDate(date: Date, context: Context): String {
        val locale = getSystemLocale(context)
        val dateFormat = getSystemDateFormat(context, locale)
        
        val formattedDate = dateFormat.format(date)
        
        return formattedDate
    }
    
    /**
     * Get the system locale with proper detection for different Android versions
     */
    private fun getSystemLocale(context: Context): Locale {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // For Android 7.0+ (API 24+), use the primary locale from configuration
            context.resources.configuration.locales[0]
        } else {
            // For older versions, use the configuration locale
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
    }
    
    /**
     * Get the system date format with proper region detection
     */
    private fun getSystemDateFormat(@Suppress("UNUSED_PARAMETER") context: Context, locale: Locale): DateFormat {
        // Try to get the system's date format preference
        return try {
            // Method 1: Use system default (should respect user's date format preference)
            val systemFormat = DateFormat.getDateInstance(DateFormat.SHORT)
            
            // Method 2: Use locale-specific format as fallback
            @Suppress("UNUSED_VARIABLE")
            val localeFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale)
            
            // Method 3: Try to detect region-specific format
            val regionFormat = getRegionSpecificDateFormat(locale)
            
            
            // For specific regions like Israel, prioritize region-specific format
            // since system format might not always respect region settings
            when {
                locale.country == "IL" || locale.language == "iw" -> regionFormat
                locale.country == "GB" -> regionFormat
                locale.country == "US" -> regionFormat
                else -> systemFormat
            }
        } catch (e: Exception) {
            DateFormat.getDateInstance(DateFormat.SHORT, locale)
        }
    }
    
    /**
     * Get region-specific date format based on locale
     */
    private fun getRegionSpecificDateFormat(locale: Locale): DateFormat {
        return when {
            // US format: MM/dd/yyyy
            locale.country == "US" -> SimpleDateFormat("MM/dd/yyyy", locale)
            // UK format: dd/MM/yyyy
            locale.country == "GB" -> SimpleDateFormat("dd/MM/yyyy", locale)
            // Israel format: dd/MM/yyyy (same as UK)
            locale.country == "IL" || locale.language == "iw" -> SimpleDateFormat("dd/MM/yyyy", locale)
            // European format: dd.MM.yyyy
            locale.country in listOf("DE", "AT", "CH", "RU", "PL", "CZ", "SK", "HU", "RO", "BG", "HR", "SI") -> 
                SimpleDateFormat("dd.MM.yyyy", locale)
            // ISO format: yyyy-MM-dd
            locale.country in listOf("SE", "NO", "DK", "FI", "IS") -> 
                SimpleDateFormat("yyyy-MM-dd", locale)
            // Default to locale's default format
            else -> DateFormat.getDateInstance(DateFormat.SHORT, locale)
        }
    }
    
    /**
     * Debug utility to log system date format information
     * This can be called to verify system date format detection is working
     */
    fun logSystemDateFormatInfo(@Suppress("UNUSED_PARAMETER") context: Context) {
        val systemLocale = Locale.getDefault()
        val testDate = Date()
        
        android.util.Log.d("LocaleFormatter", "=== System Date Format Debug ===")
        android.util.Log.d("LocaleFormatter", "System Locale: ${systemLocale.language}_${systemLocale.country}")
        android.util.Log.d("LocaleFormatter", "System Locale Display: ${systemLocale.displayName}")
        
        // Test date formatting with system default (respects user preference)
        val systemFormatted = DateFormat.getDateInstance(DateFormat.SHORT).format(testDate)
        
        // Test date formatting with explicit locale (might not respect user preference)
        val localeFormatted = DateFormat.getDateInstance(DateFormat.SHORT, systemLocale).format(testDate)
        
        android.util.Log.d("LocaleFormatter", "System Date Format (user preference): $systemFormatted")
        android.util.Log.d("LocaleFormatter", "Locale Date Format (locale default): $localeFormatted")
        android.util.Log.d("LocaleFormatter", "=== End Date Format Debug ===")
    }
    
    /**
     * Test function to verify system region and date format detection
     * Call this from your UI to test if system region detection is working
     */
    fun testSystemRegionDetection(context: Context): String {
        val systemLocale = getSystemLocale(context)
        val defaultLocale = Locale.getDefault()
        val testDate = Date()
        
        // Test different formatting methods
        val systemFormatted = DateFormat.getDateInstance(DateFormat.SHORT).format(testDate)
        val localeFormatted = DateFormat.getDateInstance(DateFormat.SHORT, systemLocale).format(testDate)
        val regionFormatted = getRegionSpecificDateFormat(systemLocale).format(testDate)
        
        return buildString {
            appendLine("System Locale: ${systemLocale.language}_${systemLocale.country}")
            appendLine("Default Locale: ${defaultLocale.language}_${defaultLocale.country}")
            appendLine("System Format: $systemFormatted")
            appendLine("Locale Format: $localeFormatted")
            appendLine("Region Format: $regionFormatted")
            appendLine("Country: ${systemLocale.country}")
            appendLine("Display Country: ${systemLocale.displayCountry}")
        }
    }
    
    /**
     * Debug function to check if system region settings are being detected
     * This helps identify if the issue is with region detection or date formatting
     */
    fun debugRegionDetection(context: Context): String {
        val systemLocale = getSystemLocale(context)
        val defaultLocale = Locale.getDefault()
        val config = context.resources.configuration
        
        return buildString {
            appendLine("=== Region Detection Debug ===")
            appendLine("Android Version: ${Build.VERSION.SDK_INT}")
            appendLine("System Locale: ${systemLocale.language}_${systemLocale.country}")
            appendLine("Default Locale: ${defaultLocale.language}_${defaultLocale.country}")
            appendLine("Config Locales Count: ${config.locales.size()}")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                for (i in 0 until config.locales.size()) {
                    val locale = config.locales[i]
                    appendLine("Config Locale $i: ${locale.language}_${locale.country}")
                }
            } else {
                @Suppress("DEPRECATION")
                appendLine("Config Locale (deprecated): ${config.locale.language}_${config.locale.country}")
            }
            
            appendLine("Country: ${systemLocale.country}")
            appendLine("Display Country: ${systemLocale.displayCountry}")
            appendLine("Variant: ${systemLocale.variant}")
            appendLine("=== End Debug ===")
        }
    }
    
    /**
     * Test function specifically for Israel region detection
     * This helps verify that Israel region is properly detected and formatted
     */
    fun testIsraelRegionDetection(context: Context): String {
        val systemLocale = getSystemLocale(context)
        val testDate = Date()
        
        return buildString {
            appendLine("=== Israel Region Test ===")
            appendLine("System Locale: ${systemLocale.language}_${systemLocale.country}")
            appendLine("Is Israel Country: ${systemLocale.country == "IL"}")
            appendLine("Is Hebrew Language: ${systemLocale.language == "iw"}")
            appendLine("Display Country: ${systemLocale.displayCountry}")
            
            // Test different formatting methods
            val systemFormat = DateFormat.getDateInstance(DateFormat.SHORT).format(testDate)
            val localeFormat = DateFormat.getDateInstance(DateFormat.SHORT, systemLocale).format(testDate)
            val regionFormat = getRegionSpecificDateFormat(systemLocale).format(testDate)
            val israelFormat = SimpleDateFormat("dd/MM/yyyy", systemLocale).format(testDate)
            
            appendLine("System Format: $systemFormat")
            appendLine("Locale Format: $localeFormat")
            appendLine("Region Format: $regionFormat")
            appendLine("Israel Format (dd/MM/yyyy): $israelFormat")
            
            // Show which format will be used
            val finalFormat = getSystemDateFormat(context, systemLocale).format(testDate)
            appendLine("Final Format Used: $finalFormat")
            appendLine("=== End Israel Test ===")
        }
    }
    
    /**
     * Get a date formatter instance for consistent use across the app
     * This ensures all date formatting uses the same region detection logic
     */
    fun getDateFormatter(context: Context): DateFormat {
        val locale = getSystemLocale(context)
        return getSystemDateFormat(context, locale)
    }
    
    /**
     * Get currency symbol for current locale or selected currency
     */
    fun getCurrencySymbol(@Suppress("UNUSED_PARAMETER") context: Context, selectedCurrency: String = ""): String {
        // If a specific currency is selected, use its symbol
        if (selectedCurrency.isNotEmpty()) {
            return getCurrencySymbolByCode(selectedCurrency)
        }
        
        // Otherwise, fall back to language-based currency
        val locale = Locale.getDefault()
        return when (locale.language) {
            "iw" -> "₪"
            "ru" -> "₽"
            else -> "$"
        }
    }
    
    /**
     * Get currency symbol by currency code
     */
    private fun getCurrencySymbolByCode(currencyCode: String): String {
        return when (currencyCode.uppercase()) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "ILS" -> "₪"
            "RUB" -> "₽"
            "JPY" -> "¥"
            "CAD" -> "C$"
            "AUD" -> "A$"
            "CHF" -> "CHF"
            "CNY" -> "¥"
            "INR" -> "₹"
            else -> "$" // Default to USD
        }
    }
    
    /**
     * Format currency amount with conversion information
     * @param originalAmount The original amount in original currency
     * @param convertedAmount The converted amount in target currency
     * @param context The context for locale information
     * @param originalCurrency The original currency code
     * @param targetCurrency The target currency code
     * @param conversionRate The conversion rate used
     * @return Formatted string showing both original and converted amounts
     */
    fun formatCurrencyWithConversion(
        originalAmount: Double,
        convertedAmount: Double,
        context: Context,
        originalCurrency: String,
        targetCurrency: String,
        conversionRate: Double
    ): String {
        val originalSymbol = getCurrencySymbol(context, originalCurrency)
        val targetSymbol = getCurrencySymbol(context, targetCurrency)
        
        val originalFormatted = String.format("%.2f", originalAmount)
        val convertedFormatted = String.format("%.2f", convertedAmount)
        val rateFormatted = String.format("%.4f", conversionRate)
        
        return "$originalSymbol$originalFormatted ($targetSymbol$convertedFormatted, $rateFormatted)"
    }
    
    /**
     * Format currency amount with conversion information (simplified)
     * @param originalAmount The original amount in original currency
     * @param convertedAmount The converted amount in target currency
     * @param context The context for locale information
     * @param originalCurrency The original currency code
     * @param targetCurrency The target currency code
     * @return Formatted string showing both original and converted amounts
     */
    fun formatCurrencyWithConversionSimple(
        originalAmount: Double,
        convertedAmount: Double,
        context: Context,
        originalCurrency: String,
        targetCurrency: String
    ): String {
        val originalSymbol = getCurrencySymbol(context, originalCurrency)
        val targetSymbol = getCurrencySymbol(context, targetCurrency)
        
        val originalFormatted = String.format("%.2f", originalAmount)
        val convertedFormatted = String.format("%.2f", convertedAmount)
        
        return "$originalSymbol$originalFormatted ($targetSymbol$convertedFormatted)"
    }
    
    /**
     * Format month and year using nominative case for month names
     * This is especially important for Russian where DateTimeFormatter uses genitive case by default
     * @param month The month number (1-12)
     * @param year The year
     * @param context The context for accessing string resources
     * @return Formatted string like "Октябрь 2025" (nominative) instead of "октября 2025" (genitive)
     */
    fun formatMonthYear(month: Int, year: Int, context: Context): String {
        val monthStringId = when (month) {
            1 -> com.pennywise.app.R.string.month_january
            2 -> com.pennywise.app.R.string.month_february
            3 -> com.pennywise.app.R.string.month_march
            4 -> com.pennywise.app.R.string.month_april
            5 -> com.pennywise.app.R.string.month_may
            6 -> com.pennywise.app.R.string.month_june
            7 -> com.pennywise.app.R.string.month_july
            8 -> com.pennywise.app.R.string.month_august
            9 -> com.pennywise.app.R.string.month_september
            10 -> com.pennywise.app.R.string.month_october
            11 -> com.pennywise.app.R.string.month_november
            12 -> com.pennywise.app.R.string.month_december
            else -> throw IllegalArgumentException("Invalid month: $month")
        }
        
        val monthName = context.getString(monthStringId)
        return "$monthName $year"
    }
}
