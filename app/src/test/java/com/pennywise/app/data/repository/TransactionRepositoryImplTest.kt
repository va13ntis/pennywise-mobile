package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.RecurringPeriod
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.time.YearMonth
import java.util.Date

/**
 * Unit tests for TransactionRepositoryImpl
 */
class TransactionRepositoryImplTest {
    
    private lateinit var transactionRepository: TransactionRepositoryImpl
    private lateinit var mockTransactionDao: MockTransactionDao
    
    @Before
    fun setUp() {
        mockTransactionDao = MockTransactionDao()
        transactionRepository = TransactionRepositoryImpl(mockTransactionDao)
    }
    
    @Test
    fun `insertTransaction should return transaction ID`() = runTest {
        // Given
        val transaction = Transaction(
            id = 0,
            userId = 1L,
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            isRecurring = false
        )
        mockTransactionDao.shouldReturnInsertId = 1L
        
        // When
        val result = transactionRepository.insertTransaction(transaction)
        
        // Then
        assertEquals("Should return correct transaction ID", 1L, result)
        assertNotNull("Should call DAO with entity", mockTransactionDao.lastInsertedTransaction)
        assertEquals("Should have correct user ID", 1L, mockTransactionDao.lastInsertedTransaction?.userId)
        assertEquals("Should have correct amount", 100.0, mockTransactionDao.lastInsertedTransaction?.amount ?: 0.0, 0.01)
    }
    
    @Test
    fun `getTransactionById should return transaction when found`() = runTest {
        // Given
        val transactionId = 1L
        val transactionEntity = TransactionEntity(
            id = transactionId,
            userId = 1L,
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            isRecurring = false
        )
        mockTransactionDao.shouldReturnTransaction = transactionEntity
        
        // When
        val result = transactionRepository.getTransactionById(transactionId)
        
        // Then
        assertNotNull("Should return transaction", result)
        assertEquals("Should return correct ID", transactionId, result?.id)
        assertEquals("Should return correct amount", 100.0, result?.amount ?: 0.0, 0.01)
        assertEquals("Should return correct description", "Test transaction", result?.description)
    }
    
    @Test
    fun `getTransactionById should return null when not found`() = runTest {
        // Given
        val transactionId = 999L
        mockTransactionDao.shouldReturnTransaction = null
        
        // When
        val result = transactionRepository.getTransactionById(transactionId)
        
        // Then
        assertNull("Should return null", result)
    }
    
    @Test
    fun `getTransactionsByUser should return user transactions`() = runTest {
        // Given
        val userId = 1L
        val transactions = listOf(
            TransactionEntity(id = 1, userId = userId, amount = 100.0, description = "Transaction 1", category = "Food", type = TransactionType.EXPENSE, date = Date()),
            TransactionEntity(id = 2, userId = userId, amount = 200.0, description = "Transaction 2", category = "Transport", type = TransactionType.EXPENSE, date = Date())
        )
        mockTransactionDao.shouldReturnTransactions = transactions
        
        // When
        val result = transactionRepository.getTransactionsByUser(userId)
        
        // Then
        val resultList = mutableListOf<Transaction>()
        result.collect { resultList.addAll(it) }
        assertEquals("Should return correct number of transactions", 2, resultList.size)
        assertEquals("Should return correct first transaction", "Transaction 1", resultList[0].description)
        assertEquals("Should return correct second transaction", "Transaction 2", resultList[1].description)
    }
    
    @Test
    fun `getTransactionsByMonth should return transactions for specific month`() = runTest {
        // Given
        val userId = 1L
        val month = YearMonth.of(2024, 1) // January 2024
        val transactions = listOf(
            TransactionEntity(id = 1, userId = userId, amount = 100.0, description = "January transaction", category = "Food", type = TransactionType.EXPENSE, date = Date())
        )
        mockTransactionDao.shouldReturnTransactions = transactions
        
        // When
        val result = transactionRepository.getTransactionsByMonth(userId, month)
        
        // Then
        val resultList = mutableListOf<Transaction>()
        result.collect { resultList.addAll(it) }
        assertEquals("Should return correct number of transactions", 1, resultList.size)
        assertEquals("Should return correct transaction", "January transaction", resultList[0].description)
    }
    
    @Test
    fun `getRecurringTransactionsByUser should return recurring transactions`() = runTest {
        // Given
        val userId = 1L
        val transactions = listOf(
            TransactionEntity(id = 1, userId = userId, amount = 50.0, description = "Monthly subscription", category = "Subscription", type = TransactionType.EXPENSE, date = Date(), isRecurring = true, recurringPeriod = RecurringPeriod.MONTHLY)
        )
        mockTransactionDao.shouldReturnTransactions = transactions
        
        // When
        val result = transactionRepository.getRecurringTransactionsByUser(userId)
        
        // Then
        val resultList = mutableListOf<Transaction>()
        result.collect { resultList.addAll(it) }
        assertEquals("Should return correct number of recurring transactions", 1, resultList.size)
        assertTrue("Should be recurring transaction", resultList[0].isRecurring)
        assertEquals("Should have correct recurring period", RecurringPeriod.MONTHLY, resultList[0].recurringPeriod)
    }
    
    @Test
    fun `getTotalIncomeByUser should return correct total`() = runTest {
        // Given
        val userId = 1L
        val startDate = Date()
        val endDate = Date()
        val expectedTotal = 1000.0
        mockTransactionDao.shouldReturnTotal = expectedTotal
        
        // When
        val result = transactionRepository.getTotalIncomeByUser(userId, startDate, endDate)
        
        // Then
        assertEquals("Should return correct total income", expectedTotal, result, 0.01)
    }
    
    @Test
    fun `getTotalExpenseByUser should return correct total`() = runTest {
        // Given
        val userId = 1L
        val startDate = Date()
        val endDate = Date()
        val expectedTotal = 500.0
        mockTransactionDao.shouldReturnTotal = expectedTotal
        
        // When
        val result = transactionRepository.getTotalExpenseByUser(userId, startDate, endDate)
        
        // Then
        assertEquals("Should return correct total expense", expectedTotal, result, 0.01)
    }
    
    @Test
    fun `getBalanceByUser should return correct balance`() = runTest {
        // Given
        val userId = 1L
        val expectedBalance = 250.0
        mockTransactionDao.shouldReturnTotal = expectedBalance
        
        // When
        val result = transactionRepository.getBalanceByUser(userId)
        
        // Then
        assertEquals("Should return correct balance", expectedBalance, result, 0.01)
    }
    
    @Test
    fun `getTransactionCountByUser should return correct count`() = runTest {
        // Given
        val userId = 1L
        val expectedCount = 5
        mockTransactionDao.shouldReturnCount = expectedCount
        
        // When
        val result = transactionRepository.getTransactionCountByUser(userId)
        
        // Then
        assertEquals("Should return correct transaction count", expectedCount, result)
    }
    
    // Mock implementation of TransactionDao for testing
    private class MockTransactionDao : TransactionDao {
        var shouldReturnTransaction: TransactionEntity? = null
        var shouldReturnTransactions: List<TransactionEntity> = emptyList()
        var shouldReturnInsertId: Long = 0L
        var shouldReturnTotal: Double = 0.0
        var shouldReturnCount: Int = 0
        var lastInsertedTransaction: TransactionEntity? = null
        
        override suspend fun insertTransaction(transaction: TransactionEntity): Long {
            lastInsertedTransaction = transaction
            return shouldReturnInsertId
        }
        
        override suspend fun updateTransaction(transaction: TransactionEntity) {
            // Mock implementation
        }
        
        override suspend fun deleteTransaction(transaction: TransactionEntity) {
            // Mock implementation
        }
        
        override suspend fun deleteAllTransactions() {
            // Mock implementation
        }
        
        override suspend fun getRecentTransactionsByUser(userId: Long): List<TransactionEntity> {
            return shouldReturnTransactions
        }
        
        override suspend fun insertTransactions(transactions: List<TransactionEntity>) {
            // Mock implementation
        }
        
        override suspend fun getTransactionById(id: Long): TransactionEntity? {
            return shouldReturnTransaction
        }
        
        override fun getTransactionsByUser(userId: Long) = flowOf(shouldReturnTransactions)
        
        override fun getTransactionsByDateRange(userId: Long, startDate: Date, endDate: Date) = flowOf(shouldReturnTransactions)
        
        override fun getTransactionsByCategory(userId: Long, category: String) = flowOf(shouldReturnTransactions)
        
        override fun getTransactionsByType(userId: Long, type: TransactionType) = flowOf(shouldReturnTransactions)
        
        override fun getRecurringTransactions(userId: Long) = flowOf(shouldReturnTransactions)
        
        override suspend fun getTotalIncome(userId: Long, startDate: Date, endDate: Date): Double {
            return shouldReturnTotal
        }
        
        override suspend fun getTotalExpense(userId: Long, startDate: Date, endDate: Date): Double {
            return shouldReturnTotal
        }
        
        override suspend fun getBalance(userId: Long): Double {
            return shouldReturnTotal
        }
        
        override suspend fun getTotalByTypeAndDateRange(userId: Long, type: TransactionType, startDate: Date, endDate: Date): Double {
            return shouldReturnTotal
        }
        
        override suspend fun getTransactionCount(userId: Long): Int {
            return shouldReturnCount
        }
        
        override suspend fun getTransactionCountByDateRange(userId: Long, startDate: Date, endDate: Date): Int {
            return shouldReturnCount
        }
    }
}
