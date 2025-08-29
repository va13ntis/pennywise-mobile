package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import java.util.Date

/**
 * Repository interface for transaction data operations
 */
interface TransactionRepository {
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
    suspend fun getTransactionById(id: Long): Transaction?
    
    // User-specific operations
    fun getTransactionsByUser(userId: Long): Flow<List<Transaction>>
    fun getTransactionsByUserAndDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<Transaction>>
    fun getTransactionsByUserAndCategory(userId: Long, category: String): Flow<List<Transaction>>
    fun getTransactionsByUserAndType(userId: Long, type: TransactionType): Flow<List<Transaction>>
    fun getRecurringTransactionsByUser(userId: Long): Flow<List<Transaction>>
    
    // Monthly operations
    fun getTransactionsByMonth(userId: Long, month: YearMonth): Flow<List<Transaction>>
    
    // Aggregation operations
    suspend fun getTotalIncomeByUser(userId: Long, startDate: Date, endDate: Date): Double
    suspend fun getTotalExpenseByUser(userId: Long, startDate: Date, endDate: Date): Double
    suspend fun getBalanceByUser(userId: Long): Double
    suspend fun getTotalByTypeAndDateRange(userId: Long, type: TransactionType, startDate: Date, endDate: Date): Double
    
    // Count operations
    suspend fun getTransactionCountByUser(userId: Long): Int
    suspend fun getTransactionCountByUserAndDateRange(userId: Long, startDate: Date, endDate: Date): Int
    
    // Legacy methods for backward compatibility (deprecated)
    @Deprecated("Use getTransactionsByUser instead")
    fun getAllTransactions(): Flow<List<Transaction>>
    @Deprecated("Use getTransactionsByUserAndDateRange instead")
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>>
    @Deprecated("Use getTransactionsByUserAndCategory instead")
    fun getTransactionsByCategory(category: String): Flow<List<Transaction>>
    @Deprecated("Use getTransactionsByUserAndType instead")
    fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>>
    @Deprecated("Use getTotalIncomeByUser instead")
    suspend fun getTotalIncome(startDate: Date, endDate: Date): Double
    @Deprecated("Use getTotalExpenseByUser instead")
    suspend fun getTotalExpense(startDate: Date, endDate: Date): Double
    @Deprecated("Use getBalanceByUser instead")
    suspend fun getBalance(): Double
}

