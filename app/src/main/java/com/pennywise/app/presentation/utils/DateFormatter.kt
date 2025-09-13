package com.pennywise.app.presentation.utils

import android.content.Context
import java.time.YearMonth
import java.util.Locale

/**
 * Utility class for formatting dates with proper localization
 */
object DateFormatter {
    
    /**
     * Format a YearMonth using custom month names from string resources
     * This ensures proper nominative case for Russian months
     */
    fun formatMonthYear(context: Context, yearMonth: YearMonth): String {
        val locale = Locale.getDefault()
        val monthValue = yearMonth.monthValue
        val year = yearMonth.year
        
        return when (locale.language) {
            "ru" -> {
                val monthName = getRussianMonthName(context, monthValue)
                "$monthName $year"
            }
            "iw" -> {
                val monthName = getHebrewMonthName(context, monthValue)
                "$monthName $year"
            }
            else -> {
                // For English and other languages, use the default formatter
                val monthName = getEnglishMonthName(context, monthValue)
                "$monthName $year"
            }
        }
    }
    
    private fun getRussianMonthName(context: Context, monthValue: Int): String {
        return when (monthValue) {
            1 -> context.getString(com.pennywise.app.R.string.month_january)
            2 -> context.getString(com.pennywise.app.R.string.month_february)
            3 -> context.getString(com.pennywise.app.R.string.month_march)
            4 -> context.getString(com.pennywise.app.R.string.month_april)
            5 -> context.getString(com.pennywise.app.R.string.month_may)
            6 -> context.getString(com.pennywise.app.R.string.month_june)
            7 -> context.getString(com.pennywise.app.R.string.month_july)
            8 -> context.getString(com.pennywise.app.R.string.month_august)
            9 -> context.getString(com.pennywise.app.R.string.month_september)
            10 -> context.getString(com.pennywise.app.R.string.month_october)
            11 -> context.getString(com.pennywise.app.R.string.month_november)
            12 -> context.getString(com.pennywise.app.R.string.month_december)
            else -> ""
        }
    }
    
    private fun getHebrewMonthName(context: Context, monthValue: Int): String {
        return when (monthValue) {
            1 -> context.getString(com.pennywise.app.R.string.month_january)
            2 -> context.getString(com.pennywise.app.R.string.month_february)
            3 -> context.getString(com.pennywise.app.R.string.month_march)
            4 -> context.getString(com.pennywise.app.R.string.month_april)
            5 -> context.getString(com.pennywise.app.R.string.month_may)
            6 -> context.getString(com.pennywise.app.R.string.month_june)
            7 -> context.getString(com.pennywise.app.R.string.month_july)
            8 -> context.getString(com.pennywise.app.R.string.month_august)
            9 -> context.getString(com.pennywise.app.R.string.month_september)
            10 -> context.getString(com.pennywise.app.R.string.month_october)
            11 -> context.getString(com.pennywise.app.R.string.month_november)
            12 -> context.getString(com.pennywise.app.R.string.month_december)
            else -> ""
        }
    }
    
    private fun getEnglishMonthName(context: Context, monthValue: Int): String {
        return when (monthValue) {
            1 -> context.getString(com.pennywise.app.R.string.month_january)
            2 -> context.getString(com.pennywise.app.R.string.month_february)
            3 -> context.getString(com.pennywise.app.R.string.month_march)
            4 -> context.getString(com.pennywise.app.R.string.month_april)
            5 -> context.getString(com.pennywise.app.R.string.month_may)
            6 -> context.getString(com.pennywise.app.R.string.month_june)
            7 -> context.getString(com.pennywise.app.R.string.month_july)
            8 -> context.getString(com.pennywise.app.R.string.month_august)
            9 -> context.getString(com.pennywise.app.R.string.month_september)
            10 -> context.getString(com.pennywise.app.R.string.month_october)
            11 -> context.getString(com.pennywise.app.R.string.month_november)
            12 -> context.getString(com.pennywise.app.R.string.month_december)
            else -> ""
        }
    }
}
