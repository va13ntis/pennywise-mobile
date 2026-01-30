package com.pennywise.app.domain.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Data class representing a billing cycle for a credit card
 */
data class BillingCycle(
    val id: Long,
    val cardId: Long,
    val cardName: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dueDate: LocalDate
) {
    /**
     * Get the date range for this billing cycle
     */
    fun getDateRange(): DateRange = DateRange(startDate, endDate)
    
    /**
     * Computed display name for UI
     */
    val displayName: String
        get() {
            val startFormatted = startDate.format(DateTimeFormatter.ofPattern("MMM d"))
            val endFormatted = endDate.format(DateTimeFormatter.ofPattern("MMM d"))
            return "$cardName ($startFormatted - $endFormatted)"
        }
}

/**
 * Data class representing a date range
 */
data class DateRange(
    val start: LocalDate,
    val end: LocalDate
)

