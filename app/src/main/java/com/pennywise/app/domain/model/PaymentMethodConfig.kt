package com.pennywise.app.domain.model

import java.time.LocalDate
import java.time.YearMonth
import java.util.Date
import java.time.ZoneId

/**
 * Data class representing a payment method configuration
 */
data class PaymentMethodConfig(
    val id: Long = 0,
    val paymentMethod: PaymentMethod,
    val alias: String, // e.g., "Personal Visa", "Corporate Card", "Main Credit Card"
    val isDefault: Boolean = false,
    val withdrawDay: Int? = null, // Day of month for credit card payment (1-31, null for non-credit cards)
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    
    /**
     * Get display name combining payment method and alias
     */
    fun getDisplayName(): String {
        return if (alias.isBlank()) {
            paymentMethod.displayName
        } else {
            "$alias (${paymentMethod.displayName})"
        }
    }
    
    /**
     * Check if this is a credit card that needs withdraw day configuration
     */
    fun isCreditCard(): Boolean {
        return paymentMethod.isCreditCard && withdrawDay != null
    }
    
    /**
     * Validate withdraw day for credit cards
     */
    fun isValidWithdrawDay(): Boolean {
        return withdrawDay == null || (withdrawDay in 1..31)
    }
    
    companion object {
        /**
         * Create a default payment method config
         */
        fun createDefault(paymentMethod: PaymentMethod, alias: String = ""): PaymentMethodConfig {
            return PaymentMethodConfig(
                paymentMethod = paymentMethod,
                alias = alias,
                isDefault = false,
                withdrawDay = if (paymentMethod.isCreditCard) 15 else null // Default to 15th for credit cards
            )
        }
    }
}

/**
 * Extension function to generate billing cycles for a payment method configuration (Card)
 * 
 * Calculates billing cycles based on the card's statement date (withdrawDay) and returns
 * the specified number of most recent cycles going backwards from the current date.
 * 
 * @param count The number of billing cycles to generate (default: 6)
 * @return List of BillingCycle objects in chronological order (oldest to newest)
 */
fun PaymentMethodConfig.getBillingCycles(count: Int = 6): List<BillingCycle> {
    // Return empty list if not a credit card or no withdraw day configured
    if (!isCreditCard() || withdrawDay == null) {
        return emptyList()
    }
    
    val cycles = mutableListOf<BillingCycle>()
    val today = LocalDate.now()
    val currentYearMonth = YearMonth.from(today)
    
    // Calculate the current cycle end date
    // If today is on or before withdrawDay, the current cycle ends this month
    // Otherwise, the current cycle ends next month
    val currentCycleEndYearMonth = if (today.dayOfMonth <= withdrawDay) {
        currentYearMonth
    } else {
        currentYearMonth.plusMonths(1)
    }
    
    // Generate cycles going backwards
    for (i in 0 until count) {
        val cycleEndYearMonth = currentCycleEndYearMonth.minusMonths(i.toLong())
        val cycleEndDay = minOf(withdrawDay, cycleEndYearMonth.lengthOfMonth())
        val cycleEndDate = cycleEndYearMonth.atDay(cycleEndDay)
        
        // Calculate cycle start date (day after previous month's withdraw day)
        val cycleStartYearMonth = cycleEndYearMonth.minusMonths(1)
        val cycleStartDay = minOf(withdrawDay, cycleStartYearMonth.lengthOfMonth()) + 1
        val cycleStartDate = if (cycleStartDay > cycleStartYearMonth.lengthOfMonth()) {
            // If start day exceeds month length, start from 1st of cycle end month
            cycleEndYearMonth.atDay(1)
        } else {
            cycleStartYearMonth.atDay(cycleStartDay)
        }
        
        // Calculate due date (typically 21 days after cycle end date, or use withdrawDay of next month)
        // For simplicity, we'll use 21 days after cycle end date as a standard grace period
        val dueDate = cycleEndDate.plusDays(21)
        
        // Create BillingCycle (id will be assigned after reversing to match final order)
        val cycle = BillingCycle(
            id = 0L, // Temporary, will be updated after reversing
            cardId = id,
            cardName = getDisplayName(),
            startDate = cycleStartDate,
            endDate = cycleEndDate,
            dueDate = dueDate
        )
        
        cycles.add(cycle)
    }
    
    // Reverse to chronological order (oldest to newest) and assign proper IDs
    val reversedCycles = cycles.reversed()
    return reversedCycles.mapIndexed { index, cycle ->
        cycle.copy(id = index.toLong())
    }
}
