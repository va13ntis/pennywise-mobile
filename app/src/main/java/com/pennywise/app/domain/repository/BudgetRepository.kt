package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Repository interface for budget data operations
 */
interface BudgetRepository {
    suspend fun insertBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
    suspend fun getBudgetById(id: Long): Budget?
    fun getAllBudgets(): Flow<List<Budget>>
    fun getActiveBudgets(): Flow<List<Budget>>
    fun getBudgetsByPeriod(period: com.pennywise.app.domain.model.BudgetPeriod): Flow<List<Budget>>
    fun getBudgetsByDateRange(startDate: Date, endDate: Date): Flow<List<Budget>>
    suspend fun updateBudgetSpent(budgetId: Long, spent: Double)
    suspend fun getTotalBudgetAmount(): Double
    suspend fun getTotalBudgetSpent(): Double
}

