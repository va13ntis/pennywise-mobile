package com.pennywise.app.domain.model

import java.util.Date

/**
 * Domain model representing a financial transaction
 */
data class Transaction(
    val id: Long = 0,
    val userId: Long, // Reference to the user who owns this transaction
    val amount: Double,
    val currency: String = "USD",
    val description: String,
    val category: String,
    val type: TransactionType,
    val date: Date,
    val isRecurring: Boolean = false,
    val recurringPeriod: RecurringPeriod? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val installments: Int? = null, // Only used for split payments
    val installmentAmount: Double? = null, // Calculated monthly payment amount
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)

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

