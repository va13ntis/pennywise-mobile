package com.pennywise.app.domain.model

import java.time.LocalDate
import java.time.YearMonth

/**
 * Finds the billing cycle that contains [date] for this card (statement boundaries from [getBillingCycles]).
 */
fun PaymentMethodConfig.billingCycleContaining(date: LocalDate): BillingCycle? {
    if (!isCreditCard() || withdrawDay == null) return null
    val cycles = getBillingCycles(24)
    return cycles.find { !date.isBefore(it.startDate) && !date.isAfter(it.endDate) }
}

/**
 * First calendar day of the billing cycle after the one containing [date].
 * Used when scheduling a recurring amount change to take effect from the "next" card cycle.
 */
fun PaymentMethodConfig.firstDayOfNextBillingCycleAfter(date: LocalDate): LocalDate {
    val current = billingCycleContaining(date)
    return if (current != null) {
        current.endDate.plusDays(1)
    } else {
        YearMonth.from(date).plusMonths(1).atDay(1)
    }
}
