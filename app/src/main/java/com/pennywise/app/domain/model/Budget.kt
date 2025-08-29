package com.pennywise.app.domain.model

import java.util.Date

/**
 * Domain model representing a budget category
 */
data class Budget(
    val id: Long = 0,
    val category: String,
    val amount: Double,
    val spent: Double = 0.0,
    val period: BudgetPeriod,
    val startDate: Date,
    val endDate: Date,
    val isActive: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    val remaining: Double
        get() = amount - spent
    
    val percentageUsed: Double
        get() = if (amount > 0) (spent / amount) * 100 else 0.0
    
    val isOverBudget: Boolean
        get() = spent > amount
}

enum class BudgetPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}

