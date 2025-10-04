package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.validation.AuthenticationValidator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

/**
 * Implementation of TransactionRepository using Room database with authentication validation.
 * All database operations require user authentication.
 */
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    authValidator: AuthenticationValidator
) : TransactionRepository, BaseAuthenticatedRepository(authValidator) {
    
    override suspend fun insertTransaction(transaction: Transaction): Long = withAuthentication {
        transactionDao.insertTransaction(TransactionEntity.fromDomainModel(transaction))
    }
    
    override suspend fun updateTransaction(transaction: Transaction) = withAuthentication {
        transactionDao.updateTransaction(TransactionEntity.fromDomainModel(transaction))
    }
    
    override suspend fun deleteTransaction(transaction: Transaction) = withAuthentication {
        transactionDao.deleteTransaction(TransactionEntity.fromDomainModel(transaction))
    }
    
    override suspend fun getTransactionById(id: Long): Transaction? = withAuthentication {
        transactionDao.getTransactionById(id)?.toDomainModel()
    }
    
    // User-specific operations
    override fun getTransactions(): Flow<List<Transaction>> = 
        kotlinx.coroutines.flow.flow {
            withAuthentication {
                transactionDao.getAllTransactions().collect { entities ->
                    emit(entities.map { it.toDomainModel() })
                }
            }
        }
    
    override fun getTransactionsByDateRange(startDate: Date, endDate: Date): Flow<List<Transaction>> = 
        kotlinx.coroutines.flow.flow {
            withAuthentication {
                transactionDao.getTransactionsByDateRange(startDate, endDate).collect { entities ->
                    emit(entities.map { it.toDomainModel() })
                }
            }
        }
    
    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = 
        kotlinx.coroutines.flow.flow {
            withAuthentication {
                transactionDao.getTransactionsByCategory(category).collect { entities ->
                    emit(entities.map { it.toDomainModel() })
                }
            }
        }
    
    override fun getTransactionsByType(type: TransactionType): Flow<List<Transaction>> = 
        kotlinx.coroutines.flow.flow {
            withAuthentication {
                transactionDao.getTransactionsByType(type).collect { entities ->
                    emit(entities.map { it.toDomainModel() })
                }
            }
        }
    
    override fun getRecurringTransactions(): Flow<List<Transaction>> = 
        kotlinx.coroutines.flow.flow {
            withAuthentication {
                transactionDao.getRecurringTransactions().collect { entities ->
                    emit(entities.map { it.toDomainModel() })
                }
            }
        }
    
    // Monthly operations
    override fun getTransactionsByMonth(month: YearMonth): Flow<List<Transaction>> = 
        kotlinx.coroutines.flow.flow {
            withAuthentication {
                val startDate = month.atDay(1).toDate()
                val endDate = month.atEndOfMonth().toDate()
                
                println("🔍 TransactionRepository: Querying transactions for month $month")
                println("🔍 TransactionRepository: Date range: $startDate to $endDate")
                
                transactionDao.getTransactionsByDateRange(startDate, endDate).collect { entities ->
                    val transactions = entities.map { it.toDomainModel() }
                    println("🔍 TransactionRepository: Found ${transactions.size} transactions for $month")
                    
                    // Debug: Show recent transactions
                    val recentTransactions = transactionDao.getRecentTransactions()
                    println("🔍 TransactionRepository: Recent transactions:")
                    recentTransactions.forEachIndexed { index, transaction ->
                        println("  ${index + 1}. ${transaction.description} - $${transaction.amount} (${transaction.type}) - ${transaction.date}")
                    }
                    
                    val countInRange = transactionDao.getTransactionCountByDateRange(startDate, endDate)
                    println("🔍 TransactionRepository: Count of transactions in date range: $countInRange")
                    
                    transactions.forEachIndexed { index, transaction ->
                        println("  ${index + 1}. ${transaction.description} - $${transaction.amount} (${transaction.type}) - ${transaction.date}")
                    }
                    
                    emit(transactions)
                }
            }
        }
    
    // Aggregation operations
    override suspend fun getTotalIncome(startDate: Date, endDate: Date): Double = withAuthentication {
        transactionDao.getTotalIncome(startDate, endDate)
    }
    
    override suspend fun getTotalExpense(startDate: Date, endDate: Date): Double = withAuthentication {
        transactionDao.getTotalExpense(startDate, endDate)
    }
    
    override suspend fun getBalance(): Double = withAuthentication {
        transactionDao.getBalance()
    }
    
    override suspend fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Date, endDate: Date): Double = withAuthentication {
        transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)
    }
    
    // Count operations
    override suspend fun getTransactionCount(): Int = withAuthentication {
        transactionDao.getTransactionCount()
    }
    
    override suspend fun getTransactionCountByDateRange(startDate: Date, endDate: Date): Int = withAuthentication {
        transactionDao.getTransactionCountByDateRange(startDate, endDate)
    }
}

// Extension function to convert LocalDate to Date
private fun LocalDate.toDate(): Date {
    return Date.from(this.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

