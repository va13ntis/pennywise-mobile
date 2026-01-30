package com.pennywise.app.domain.util

import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig
import com.pennywise.app.domain.model.Transaction
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

/**
 * Utility functions for handling billing cycle calculations
 */
object BillingCycleUtils {
    
    /**
     * Calculate the billing cycle (start and end dates) for a transaction based on its date and withdraw day.
     * 
     * For a transaction with withdrawDay = 10:
     * - Transaction on Sep-15: cycle is Sep-10 to Oct-9
     * - Transaction on Oct-28: cycle is Oct-10 to Nov-9
     * - Transaction on Nov-5: cycle is Oct-10 to Nov-9 (belongs to previous cycle)
     * 
     * @param transactionDate The date when the transaction was made
     * @param withdrawDay The day of month when the billing cycle ends (1-31)
     * @return Pair of cycle start date to cycle end date
     */
    fun calculateBillingCycle(transactionDate: Date, withdrawDay: Int): Pair<Date, Date> {
        val transactionLocalDate = transactionDate.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        
        val transactionYearMonth = YearMonth.from(transactionLocalDate)
        
        // Determine which cycle this transaction belongs to
        val cycleEndYearMonth: YearMonth
        val cycleEndDay: Int
        
        if (transactionLocalDate.dayOfMonth <= withdrawDay) {
            // Transaction is before or on withdraw day, so it belongs to current month's cycle
            cycleEndYearMonth = transactionYearMonth
            cycleEndDay = minOf(withdrawDay, transactionYearMonth.lengthOfMonth())
        } else {
            // Transaction is after withdraw day, so it belongs to next month's cycle
            cycleEndYearMonth = transactionYearMonth.plusMonths(1)
            cycleEndDay = minOf(withdrawDay, cycleEndYearMonth.lengthOfMonth())
        }
        
        // Calculate cycle end date
        val cycleEndDate = cycleEndYearMonth.atDay(cycleEndDay)
        
        // Calculate cycle start date (day after previous month's withdraw day)
        val cycleStartYearMonth = cycleEndYearMonth.minusMonths(1)
        val cycleStartDay = minOf(withdrawDay, cycleStartYearMonth.lengthOfMonth()) + 1
        val cycleStartDate = if (cycleStartDay > cycleStartYearMonth.lengthOfMonth()) {
            // If start day exceeds month length, start from 1st of next month
            cycleEndYearMonth.atDay(1)
        } else {
            cycleStartYearMonth.atDay(cycleStartDay)
        }
        
        return Pair(
            Date.from(cycleStartDate.atStartOfDay(ZoneId.systemDefault()).toInstant()),
            Date.from(cycleEndDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant())
        )
    }
    
    /**
     * Check if the current date falls within the billing cycle of a transaction.
     * 
     * @param transactionDate The date when the transaction was made
     * @param withdrawDay The day of month when the billing cycle ends
     * @param currentDate The current date to check against
     * @return true if current date is within the transaction's billing cycle
     */
    fun isCurrentDateInBillingCycle(
        transactionDate: Date, 
        withdrawDay: Int, 
        currentDate: Date
    ): Boolean {
        val (cycleStart, cycleEnd) = calculateBillingCycle(transactionDate, withdrawDay)
        
        val currentLocalDateTime = currentDate.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val cycleStartDateTime = cycleStart.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        val cycleEndDateTime = cycleEnd.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime()
        
        return !currentLocalDateTime.isBefore(cycleStartDateTime) && 
               !currentLocalDateTime.isAfter(cycleEndDateTime)
    }
    
    /**
     * Get the billing cycle for a specific transaction based on its payment method configuration.
     * 
     * @param transaction The transaction to calculate cycle for
     * @param paymentMethodConfigs List of payment method configurations
     * @return Pair of cycle start and end dates, or null if no cycle applies (e.g., cash transactions)
     */
    fun getTransactionBillingCycle(
        transaction: Transaction, 
        paymentMethodConfigs: List<PaymentMethodConfig>
    ): Pair<Date, Date>? {
        // Only credit card transactions have billing cycles
        if (transaction.paymentMethod != PaymentMethod.CREDIT_CARD) {
            return null
        }
        
        // Find the payment method config for this transaction
        val config = transaction.paymentMethodConfigId?.let { configId ->
            paymentMethodConfigs.find { it.id == configId }
        }
        
        // If no config or no withdraw day, return null (immediate billing)
        val withdrawDay = config?.withdrawDay ?: return null
        
        return calculateBillingCycle(transaction.date, withdrawDay)
    }
    
    /**
     * Check if a transaction should be included in the current month's totals
     * based on whether the current date falls within its billing cycle.
     * 
     * @param transaction The transaction to check
     * @param paymentMethodConfigs List of payment method configurations  
     * @param currentDate The current date
     * @return true if transaction should be included in current totals
     */
    fun shouldIncludeTransactionInCurrentTotals(
        transaction: Transaction,
        paymentMethodConfigs: List<PaymentMethodConfig>,
        currentDate: Date
    ): Boolean {
        // For non-credit card transactions, use the existing billing date logic
        if (transaction.paymentMethod != PaymentMethod.CREDIT_CARD) {
            val currentLocalDate = currentDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val currentYearMonth = YearMonth.from(currentLocalDate)
            
            val billingDate = transaction.getBillingDate()
            val billingLocalDate = billingDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val billingYearMonth = YearMonth.from(billingLocalDate)
            
            return billingYearMonth == currentYearMonth
        }
        
        // For credit card transactions, check if current date is in billing cycle
        val config = transaction.paymentMethodConfigId?.let { configId ->
            paymentMethodConfigs.find { it.id == configId }
        }
        
        val withdrawDay = config?.withdrawDay
        
        // If no withdraw day configured, fall back to billing date logic
        if (withdrawDay == null) {
            val currentLocalDate = currentDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val currentYearMonth = YearMonth.from(currentLocalDate)
            
            val billingDate = transaction.getBillingDate()
            val billingLocalDate = billingDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            val billingYearMonth = YearMonth.from(billingLocalDate)
            
            return billingYearMonth == currentYearMonth
        }
        
        // Check if current date falls within transaction's billing cycle
        return isCurrentDateInBillingCycle(transaction.date, withdrawDay, currentDate)
    }
    
    /**
     * Get the effective billing date for a transaction, used for month/week grouping.
     * 
     * For credit cards with billing cycles: returns the billing cycle end date (statement month).
     * For cash/cheque: returns the purchase date (same as getBillingDate).
     * For delayed payments: returns getBillingDate (which handles billingDelayDays).
     * 
     * This is the date that determines which month/week the transaction appears in on the Home screen.
     * 
     * @param transaction The transaction to get effective billing date for
     * @param paymentMethodConfigs List of payment method configurations
     * @return The effective billing date (statement month for credit cards, purchase date for others)
     */
    fun getEffectiveBillingDate(
        transaction: Transaction,
        paymentMethodConfigs: List<PaymentMethodConfig>
    ): Date {
        // For non-credit card transactions, use the standard billing date
        if (transaction.paymentMethod != PaymentMethod.CREDIT_CARD) {
            return transaction.getBillingDate()
        }
        
        // For credit card transactions, check if they have a billing cycle
        val config = transaction.paymentMethodConfigId?.let { configId ->
            paymentMethodConfigs.find { it.id == configId }
        }
        
        val withdrawDay = config?.withdrawDay
        
        // If no withdraw day configured, fall back to standard billing date logic
        if (withdrawDay == null) {
            return transaction.getBillingDate()
        }
        
        // For credit cards with billing cycles, use the billing cycle end date (statement month)
        // This is when the expense appears on the statement
        val (_, cycleEndDate) = calculateBillingCycle(transaction.date, withdrawDay)
        return cycleEndDate
    }
}







