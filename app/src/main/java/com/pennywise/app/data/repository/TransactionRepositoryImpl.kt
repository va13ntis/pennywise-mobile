package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/**
 * Implementation of TransactionRepository using Room database
 */
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {
    
    override suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(TransactionEntity.fromDomainModel(transaction))
    }
    
    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(TransactionEntity.fromDomainModel(transaction))
    }
    
    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(TransactionEntity.fromDomainModel(transaction))
    }
    
    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomainModel()
    }
    
    // User-specific operations
    override fun getTransactionsByUser(userId: Long): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByUser(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getTransactionsByUserAndDateRange(userId: Long, startDate: Date, endDate: Date): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(userId, startDate, endDate).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getTransactionsByUserAndCategory(userId: Long, category: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(userId, category).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getTransactionsByUserAndType(userId: Long, type: TransactionType): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByType(userId, type).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getRecurringTransactionsByUser(userId: Long): Flow<List<Transaction>> {
        return transactionDao.getRecurringTransactions(userId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    // Monthly operations
    override fun getTransactionsByMonth(userId: Long, month: YearMonth): Flow<List<Transaction>> {
        val startDate = month.atDay(1).toDate()
        val endDate = month.atEndOfMonth().toDate()
        return getTransactionsByUserAndDateRange(userId, startDate, endDate)
    }
    
    // Aggregation operations
    override suspend fun getTotalIncomeByUser(userId: Long, startDate: Date, endDate: Date): Double {
        return transactionDao.getTotalIncome(userId, startDate, endDate)
    }
    
    override suspend fun getTotalExpenseByUser(userId: Long, startDate: Date, endDate: Date): Double {
        return transactionDao.getTotalExpense(userId, startDate, endDate)
    }
    
    override suspend fun getBalanceByUser(userId: Long): Double {
        return transactionDao.getBalance(userId)
    }
    
    override suspend fun getTotalByTypeAndDateRange(userId: Long, type: TransactionType, startDate: Date, endDate: Date): Double {
        return transactionDao.getTotalByTypeAndDateRange(userId, type, startDate, endDate)
    }
    
    // Count operations
    override suspend fun getTransactionCountByUser(userId: Long): Int {
        return transactionDao.getTransactionCount(userId)
    }
    
    override suspend fun getTransactionCountByUserAndDateRange(userId: Long, startDate: Date, endDate: Date): Int {
        return transactionDao.getTransactionCountByDateRange(userId, startDate, endDate)
    }
    
    // Legacy methods for backward compatibility (deprecated)
    @Deprecated("Use getTransactionsByUser instead")
    override fun getAllTransactions(): Flow<List<Transaction>> {
        // This method is deprecated and should not be used in new code
        // For now, we'll return an empty list since we need a userId
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    @Deprecated("Use getTransactionsByUserAndDateRange instead")
    override fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>> {
        // This method is deprecated and should not be used in new code
        // For now, we'll return an empty list since we need a userId
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    @Deprecated("Use getTransactionsByUserAndCategory instead")
    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        // This method is deprecated and should not be used in new code
        // For now, we'll return an empty list since we need a userId
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    @Deprecated("Use getTransactionsByUserAndType instead")
    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> {
        // This method is deprecated and should not be used in new code
        // For now, we'll return an empty list since we need a userId
        return kotlinx.coroutines.flow.flowOf(emptyList())
    }
    
    @Deprecated("Use getTotalIncomeByUser instead")
    override suspend fun getTotalIncome(startDate: Date, endDate: Date): Double {
        // This method is deprecated and should not be used in new code
        // For now, we'll return 0.0 since we need a userId
        return 0.0
    }
    
    @Deprecated("Use getTotalExpenseByUser instead")
    override suspend fun getTotalExpense(startDate: Date, endDate: Date): Double {
        // This method is deprecated and should not be used in new code
        // For now, we'll return 0.0 since we need a userId
        return 0.0
    }
    
    @Deprecated("Use getBalanceByUser instead")
    override suspend fun getBalance(): Double {
        // This method is deprecated and should not be used in new code
        // For now, we'll return 0.0 since we need a userId
        return 0.0
    }
}

// Extension function to convert LocalDate to Date
private fun LocalDate.toDate(): Date {
    return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

