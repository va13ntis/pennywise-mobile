package com.pennywise.app.presentation.util

import android.content.Context
import android.text.TextUtils
import android.view.View
import java.text.NumberFormat
import java.util.*

/**
 * Enhanced utility class for currency formatting with proper locale support
 * Handles RTL languages, currency-specific decimal places, and symbol positioning
 * 
 * This class replaces manual string formatting with Android's built-in NumberFormat
 * to ensure proper locale-aware currency display according to international standards.
 */
object CurrencyFormatter {
    
    /**
     * Format amount with currency using system locale-aware formatting
     * 
     * @param amount The amount to format
     * @param currencyCode The ISO 4217 currency code (e.g., "USD", "EUR")
     * @param context Context used to determine locale if not provided
     * @param locale Optional specific locale to use for formatting
     * @param forceRTL Whether to force RTL formatting regardless of locale
     * @return Properly formatted currency string
     */
    fun formatAmount(
        amount: Double, 
        currencyCode: String, 
        context: Context,
        locale: Locale? = null,
        forceRTL: Boolean = false
    ): String {
        val formattingLocale = locale ?: context.resources.configuration.locales[0]
        val format = NumberFormat.getCurrencyInstance(formattingLocale)
        
        try {
            format.currency = Currency.getInstance(currencyCode.uppercase())
        } catch (e: IllegalArgumentException) {
            // Fall back to USD if currency code is invalid
            format.currency = Currency.getInstance("USD")
        }
        
        // Apply bidirectional markers if needed for mixed-direction text
        val formattedAmount = format.format(amount)
        val isRTL = forceRTL || TextUtils.getLayoutDirectionFromLocale(formattingLocale) == View.LAYOUT_DIRECTION_RTL
        
        return if (isRTL) {
            "\u200F$formattedAmount\u200F" // RLM markers
        } else {
            formattedAmount
        }
    }
    
    /**
     * Format amount with original and converted currency
     * 
     * @param originalAmount The original amount
     * @param convertedAmount The converted amount
     * @param originalCurrency The original currency code
     * @param targetCurrency The target currency code
     * @param context Context used for locale
     * @param showRate Whether to show the conversion rate
     * @param conversionRate The conversion rate (optional)
     * @return Formatted string with both currencies
     */
    fun formatAmountWithConversion(
        originalAmount: Double,
        convertedAmount: Double,
        originalCurrency: String,
        targetCurrency: String,
        context: Context,
        showRate: Boolean = false,
        conversionRate: Double = 0.0
    ): String {
        val locale = context.resources.configuration.locales[0]
        val originalFormatted = formatAmount(originalAmount, originalCurrency, context)
        val convertedFormatted = formatAmount(convertedAmount, targetCurrency, context)
        
        return if (showRate && conversionRate > 0) {
            val rateFormat = NumberFormat.getNumberInstance(locale)
            rateFormat.minimumFractionDigits = 4
            rateFormat.maximumFractionDigits = 4
            "$originalFormatted ($convertedFormatted, ${rateFormat.format(conversionRate)})"
        } else {
            "$originalFormatted ($convertedFormatted)"
        }
    }
    
    /**
     * Get currency symbol for a specific currency code
     * 
     * @param currencyCode The ISO 4217 currency code
     * @param locale Optional locale to determine symbol variant
     * @return The currency symbol
     */
    fun getCurrencySymbol(currencyCode: String, locale: Locale? = null): String {
        return try {
            val currency = Currency.getInstance(currencyCode.uppercase())
            if (locale != null) {
                currency.getSymbol(locale)
            } else {
                currency.symbol
            }
        } catch (e: IllegalArgumentException) {
            "$" // Default to $ if currency code is invalid
        }
    }
    
    /**
     * Format amount without currency symbol
     * Useful when displaying the symbol separately
     * 
     * @param amount The amount to format
     * @param currencyCode The currency code to determine decimal places
     * @param locale The locale for formatting
     * @return Formatted amount without currency symbol
     */
    fun formatAmountWithoutSymbol(
        amount: Double,
        currencyCode: String,
        locale: Locale
    ): String {
        val format = NumberFormat.getNumberInstance(locale)
        
        try {
            val currency = Currency.getInstance(currencyCode.uppercase())
            format.minimumFractionDigits = currency.defaultFractionDigits
            format.maximumFractionDigits = currency.defaultFractionDigits
        } catch (e: IllegalArgumentException) {
            // Default to 2 decimal places
            format.minimumFractionDigits = 2
            format.maximumFractionDigits = 2
        }
        
        return format.format(amount)
    }
    
    /**
     * Format amount with currency symbol positioned separately
     * This allows for custom symbol placement in UI components
     * 
     * @param amount The amount to format
     * @param currencyCode The currency code
     * @param context Context for locale
     * @return Pair of (formatted amount, currency symbol)
     */
    fun formatAmountWithSeparateSymbol(
        amount: Double,
        currencyCode: String,
        context: Context
    ): Pair<String, String> {
        val locale = context.resources.configuration.locales[0]
        val symbol = getCurrencySymbol(currencyCode, locale)
        val formattedAmount = formatAmountWithoutSymbol(amount, currencyCode, locale)
        
        return Pair(formattedAmount, symbol)
    }
    
    /**
     * Check if a currency code is valid
     * 
     * @param currencyCode The currency code to validate
     * @return true if the currency code is valid, false otherwise
     */
    fun isValidCurrencyCode(currencyCode: String): Boolean {
        return try {
            Currency.getInstance(currencyCode.uppercase())
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
    
    /**
     * Get the default fraction digits for a currency
     * 
     * @param currencyCode The currency code
     * @return Number of default fraction digits (e.g., 0 for JPY, 2 for USD)
     */
    fun getDefaultFractionDigits(currencyCode: String): Int {
        return try {
            val currency = Currency.getInstance(currencyCode.uppercase())
            currency.defaultFractionDigits
        } catch (e: IllegalArgumentException) {
            2 // Default to 2 decimal places
        }
    }
    
    /**
     * Format large amounts with appropriate abbreviations (K, M, B)
     * 
     * @param amount The amount to format
     * @param currencyCode The currency code
     * @param context Context for locale
     * @param useAbbreviations Whether to use abbreviations for large numbers
     * @return Formatted string with or without abbreviations
     */
    fun formatLargeAmount(
        amount: Double,
        currencyCode: String,
        context: Context,
        useAbbreviations: Boolean = false
    ): String {
        if (!useAbbreviations) {
            return formatAmount(amount, currencyCode, context)
        }
        
        val locale = context.resources.configuration.locales[0]
        val symbol = getCurrencySymbol(currencyCode, locale)
        
        return when {
            amount >= 1_000_000_000 -> {
                val format = NumberFormat.getNumberInstance(locale)
                format.maximumFractionDigits = 1
                "$symbol${format.format(amount / 1_000_000_000)}B"
            }
            amount >= 1_000_000 -> {
                val format = NumberFormat.getNumberInstance(locale)
                format.maximumFractionDigits = 1
                "$symbol${format.format(amount / 1_000_000)}M"
            }
            amount >= 1_000 -> {
                val format = NumberFormat.getNumberInstance(locale)
                format.maximumFractionDigits = 1
                "$symbol${format.format(amount / 1_000)}K"
            }
            else -> formatAmount(amount, currencyCode, context)
        }
    }
    
    /**
     * Format zero amount with special handling
     * 
     * @param currencyCode The currency code
     * @param context Context for locale
     * @param showAsFree Whether to show zero as "Free" instead of "0.00"
     * @return Formatted zero amount
     */
    fun formatZeroAmount(
        currencyCode: String,
        context: Context,
        showAsFree: Boolean = false
    ): String {
        return if (showAsFree) {
            "Free"
        } else {
            formatAmount(0.0, currencyCode, context)
        }
    }
    
    /**
     * Format amount specifically for RTL languages with enhanced bidirectional support
     * 
     * @param amount The amount to format
     * @param currencyCode The currency code
     * @param context Context for locale
     * @param locale Optional specific locale to use
     * @return Formatted currency string optimized for RTL display
     */
    fun formatAmountForRTL(
        amount: Double,
        currencyCode: String,
        context: Context,
        locale: Locale? = null
    ): String {
        val formattingLocale = locale ?: context.resources.configuration.locales[0]
        val format = NumberFormat.getCurrencyInstance(formattingLocale)
        
        try {
            format.currency = Currency.getInstance(currencyCode.uppercase())
        } catch (e: IllegalArgumentException) {
            format.currency = Currency.getInstance("USD")
        }
        
        val formattedAmount = format.format(amount)
        
        // Enhanced RTL support with proper bidirectional markers
        return "\u200F$formattedAmount\u200F" // Always apply RLM markers for RTL
    }
    
    /**
     * Check if a locale is RTL
     * 
     * @param locale The locale to check
     * @return true if the locale is RTL, false otherwise
     */
    fun isRTLLocale(locale: Locale): Boolean {
        return TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    }
    
    /**
     * Get the appropriate text direction for a locale
     * 
     * @param locale The locale to check
     * @return "rtl" for RTL locales, "ltr" for LTR locales
     */
    fun getTextDirection(locale: Locale): String {
        return if (isRTLLocale(locale)) "rtl" else "ltr"
    }
}
