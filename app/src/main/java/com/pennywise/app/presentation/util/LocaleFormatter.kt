package com.pennywise.app.presentation.util

import android.content.Context
import java.text.NumberFormat
import java.text.SimpleDateFormat
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
    fun formatCurrency(amount: Double, context: Context, selectedCurrency: String = ""): String {
        // If a specific currency is selected, use it
        if (selectedCurrency.isNotEmpty()) {
            return formatCurrencyByCode(amount, selectedCurrency)
        }
        
        // Otherwise, fall back to language-based currency
        val locale = context.resources.configuration.locales[0]
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
     * Format transaction date based on current locale
     * Uses appropriate date format for the language
     */
    fun formatTransactionDate(date: Date, context: Context): String {
        val locale = context.resources.configuration.locales[0]
        
        return when (locale.language) {
            "iw" -> formatHebrewDate(date)
            "ru" -> formatRussianDate(date)
            else -> formatEnglishDate(date)
        }
    }
    
    /**
     * Format date for Hebrew locale (dd/MM/yyyy)
     */
    private fun formatHebrewDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        
        return String.format("%02d/%02d/%d", day, month, year)
    }
    
    /**
     * Format date for Russian locale (dd.MM.yyyy)
     */
    private fun formatRussianDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        
        return String.format("%02d.%02d.%d", day, month, year)
    }
    
    /**
     * Format date for English locale (MM/dd/yyyy)
     */
    private fun formatEnglishDate(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1
        val year = calendar.get(Calendar.YEAR)
        
        return String.format("%02d/%02d/%d", month, day, year)
    }
    
    /**
     * Get currency symbol for current locale or selected currency
     */
    fun getCurrencySymbol(context: Context, selectedCurrency: String = ""): String {
        // If a specific currency is selected, use its symbol
        if (selectedCurrency.isNotEmpty()) {
            return getCurrencySymbolByCode(selectedCurrency)
        }
        
        // Otherwise, fall back to language-based currency
        val locale = context.resources.configuration.locales[0]
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
}
