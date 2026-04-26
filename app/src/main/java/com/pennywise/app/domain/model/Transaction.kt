package com.pennywise.app.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date

/**
 * Domain model representing a financial transaction
 */
data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val currency: String = "USD",
    val description: String,
    val category: String,
    val type: TransactionType,
    val date: Date,
    val isRecurring: Boolean = false,
    val recurringPeriod: RecurringPeriod? = null,
    /** Last day (inclusive, ISO yyyy-MM-dd) of the last card billing cycle (or calendar month for cash) where this recurring still appears. */
    val recurringEndsInclusiveDate: String? = null,
    /** When set with [recurringAmountEffectiveOn], replaces [amount] for display from that cycle onward. */
    val recurringAmountPending: Double? = null,
    /** First day (ISO yyyy-MM-dd) of the billing cycle when [recurringAmountPending] applies; null if no pending amount change. */
    val recurringAmountEffectiveOn: String? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val paymentMethodConfigId: Long? = null, // Link to specific PaymentMethodConfig (e.g., which credit card)
    val installments: Int? = null, // Only used for split payments
    val installmentAmount: Double? = null, // Calculated monthly payment amount
    val billingDelayDays: Int = 0, // Billing delay in days for credit card payments (0=immediate/שוטף, 30/60/90=delayed)
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    /**
     * Amount for this recurring in a given calendar month (pending amount applies from [recurringAmountEffectiveOn] onward).
     */
    fun recurringDisplayAmount(forMonth: YearMonth): Double {
        val pending = recurringAmountPending
        val effectiveOn = recurringAmountEffectiveOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (pending != null && effectiveOn != null && !forMonth.atDay(1).isBefore(effectiveOn)) {
            return pending
        }
        return amount
    }

    /** Amount for "today" (edit form). */
    fun recurringDisplayAmount(referenceDate: LocalDate): Double {
        val pending = recurringAmountPending
        val effectiveOn = recurringAmountEffectiveOn?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (pending != null && effectiveOn != null && !referenceDate.isBefore(effectiveOn)) {
            return pending
        }
        return amount
    }

    /**
     * Whether this recurring template should appear for the given calendar month (not yet past scheduled end).
     */
    fun isRecurringActiveInMonth(month: YearMonth): Boolean {
        if (!isRecurring) return false
        val ends = recurringEndsInclusiveDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return true
        return !month.atDay(1).isAfter(ends)
    }

    /**
     * Calculate the actual billing date for this transaction
     * For delayed credit card payments, this adds billingDelayDays to the transaction date
     * For immediate payments, returns the transaction date
     */
    fun getBillingDate(): Date {
        if (billingDelayDays == 0) {
            return date
        }
        
        // Convert to LocalDate, add days, convert back to Date
        val instant = Instant.ofEpochMilli(date.time)
        val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
        val billingLocalDate = localDate.plusDays(billingDelayDays.toLong())
        return Date.from(billingLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }
    
    /**
     * Check if this transaction has delayed billing
     */
    fun hasDelayedBilling(): Boolean {
        return paymentMethod == PaymentMethod.CREDIT_CARD && billingDelayDays > 0
    }
}

enum class TransactionType {
    INCOME,
    EXPENSE
}

enum class RecurringPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

