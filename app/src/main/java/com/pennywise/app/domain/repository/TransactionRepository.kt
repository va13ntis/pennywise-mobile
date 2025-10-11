package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import java.util.Date

/**
 * Repository interface for transaction data operations.
 * All operations implicitly use the currently authenticated user.
 */
interface TransactionRepository {
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTransactionById(id: Long): Transaction?
    
    // User-specific operations (userId now implicit from authentication)
    fun getTransactions(): Flow<List<Transaction>>
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>>
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    fun getRecurringTransactions(): Flow<List<Transaction>>
    
    // Monthly operations
    fun getTransactionsByMonth(month: YearMonth): Flow<List<Transaction>>
    
    // Aggregation operations
    suspend fun getTotalIncome(startDate: Date, endDate: Date): Double
    suspend fun getTotalExpense(startDate: Date, endDate: Date): Double
    suspend fun getBalance(): Double
    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Double
    
    // Count operations
    suspend fun getTransactionCount(): Int
    suspend fun getTransactionCountByDateRange(startDate: Date, endDate: Date): Int
    
    // Merchant suggestions
    suspend fun getFrequentMerchantsByCategory(category: String, limit: Int = 10): List<String>
}

