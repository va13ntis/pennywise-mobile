package com.pennywise.app.domain.model

import java.util.Date

/**
 * Domain model representing a single installment of a split payment
 */
data class SplitPaymentInstallment(
    val id: Long = 0,
    val parentTransactionId: Long, // Reference to the original transaction
    val userId: Long, // Reference to the user who owns this installment
    val amount: Double, // The installment amount
    val currency: String = "USD",
    val description: String, // Description with installment info
    val category: String,
    val type: TransactionType,
    val dueDate: Date, // When this installment is due
    val installmentNumber: Int, // Which installment this is (1, 2, 3, etc.)
    val totalInstallments: Int, // Total number of installments
    val isPaid: Boolean = false, // Whether this installment has been paid
    val paidDate: Date? = null, // When this installment was paid (if paid)
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    /**
     * Get a formatted description for the installment
     */
    fun getFormattedDescription(): String {
        return "$description (${installmentNumber}/${totalInstallments})"
    }
    
    /**
     * Check if this installment is overdue
     */
    fun isOverdue(): Boolean {
        return !isPaid && dueDate.before(Date())
    }
    
    /**
     * Check if this installment is due in the current month
     */
    fun isDueInMonth(year: Int, month: Int): Boolean {
        val calendar = java.util.Calendar.getInstance()
        calendar.time = dueDate
        return calendar.get(java.util.Calendar.YEAR) == year && 
               calendar.get(java.util.Calendar.MONTH) == month
    }
}
