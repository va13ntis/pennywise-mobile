package com.pennywise.app.data.local.dao

import androidx.room.*
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for transaction operations
 */
@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)
    
    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)
    
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?
    
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY date DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE isRecurring = 1 ORDER BY date DESC")
    fun getRecurringTransactions(): Flow<List<TransactionEntity>>
    
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'INCOME' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalIncome(startDate: Date, endDate: Date): Double
    
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'EXPENSE' AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalExpense(startDate: Date, endDate: Date): Double
    
    @Query("SELECT COALESCE(SUM(CASE WHEN type = 'INCOME' THEN amount ELSE -amount END), 0) FROM transactions")
    suspend fun getBalance(): Double
    
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Double
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
    
    @Query("SELECT COUNT(*) FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTransactionCountByDateRange(startDate: Date, endDate: Date): Int
    
    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()
    
    // Debug query to get recent transactions (for debugging)
    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT 10")
    suspend fun getRecentTransactions(): List<TransactionEntity>
    
    // Get frequent merchants by category for suggestions
    @Query("""
        SELECT description, COUNT(*) as usage_count 
        FROM transactions 
        WHERE type = 'EXPENSE' AND category = :category AND description != ''
        GROUP BY LOWER(TRIM(description))
        ORDER BY usage_count DESC, MAX(date) DESC
        LIMIT :limit
    """)
    suspend fun getFrequentMerchantsByCategory(category: String, limit: Int): List<MerchantSuggestion>
    
    /**
     * Data class for merchant suggestion query result
     */
    data class MerchantSuggestion(
        val description: String,
        val usage_count: Int
    )
}