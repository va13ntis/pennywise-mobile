package com.pennywise.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar
import java.util.Date

@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {
    private lateinit var database: PennyWiseDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var userDao: UserDao
    private lateinit var testUser: UserEntity

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = database.transactionDao()
        userDao = database.userDao()
        
        // Create a test user
        testUser = UserEntity(username = "testuser", passwordHash = "hash")
        userDao.insertUser(testUser)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertTransaction_shouldReturnTransactionId() = runTest {
        // Given
        val transaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )

        // When
        val transactionId = transactionDao.insertTransaction(transaction)

        // Then
        assertTrue(transactionId > 0)
    }

    @Test
    fun getTransactionById_shouldReturnTransaction() = runTest {
        // Given
        val transaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val transactionId = transactionDao.insertTransaction(transaction)

        // When
        val retrievedTransaction = transactionDao.getTransactionById(transactionId)

        // Then
        assertNotNull(retrievedTransaction)
        assertEquals(transaction.amount, retrievedTransaction!!.amount)
        assertEquals(transaction.description, retrievedTransaction.description)
        assertEquals(transaction.category, retrievedTransaction.category)
        assertEquals(transaction.type, retrievedTransaction.type)
    }

    @Test
    fun getTransactionsByUser_shouldReturnUserTransactions() = runTest {
        // Given
        val transaction1 = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Transaction 1",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val transaction2 = TransactionEntity(
            userId = testUser.id,
            amount = 200.0,
            description = "Transaction 2",
            category = "Transport",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2)

        // When
        val transactions = transactionDao.getTransactionsByUser(testUser.id).first()

        // Then
        assertEquals(2, transactions.size)
        assertTrue(transactions.any { it.description == "Transaction 1" })
        assertTrue(transactions.any { it.description == "Transaction 2" })
    }

    @Test
    fun getTransactionsByDateRange_shouldReturnFilteredTransactions() = runTest {
        // Given
        val calendar = Calendar.getInstance()
        val startDate = calendar.time
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val middleDate = calendar.time
        
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endDate = calendar.time
        
        val transaction1 = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Transaction 1",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = startDate
        )
        val transaction2 = TransactionEntity(
            userId = testUser.id,
            amount = 200.0,
            description = "Transaction 2",
            category = "Transport",
            type = TransactionType.EXPENSE,
            date = middleDate
        )
        val transaction3 = TransactionEntity(
            userId = testUser.id,
            amount = 300.0,
            description = "Transaction 3",
            category = "Entertainment",
            type = TransactionType.EXPENSE,
            date = endDate
        )
        
        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2)
        transactionDao.insertTransaction(transaction3)

        // When
        val transactions = transactionDao.getTransactionsByDateRange(testUser.id, startDate, endDate).first()

        // Then
        assertEquals(3, transactions.size)
    }

    @Test
    fun getTransactionsByCategory_shouldReturnFilteredTransactions() = runTest {
        // Given
        val transaction1 = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Food transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val transaction2 = TransactionEntity(
            userId = testUser.id,
            amount = 200.0,
            description = "Transport transaction",
            category = "Transport",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2)

        // When
        val foodTransactions = transactionDao.getTransactionsByCategory(testUser.id, "Food").first()

        // Then
        assertEquals(1, foodTransactions.size)
        assertEquals("Food", foodTransactions[0].category)
    }

    @Test
    fun getTransactionsByType_shouldReturnFilteredTransactions() = runTest {
        // Given
        val expenseTransaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Expense",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val incomeTransaction = TransactionEntity(
            userId = testUser.id,
            amount = 1000.0,
            description = "Income",
            category = "Salary",
            type = TransactionType.INCOME,
            date = Date()
        )
        transactionDao.insertTransaction(expenseTransaction)
        transactionDao.insertTransaction(incomeTransaction)

        // When
        val expenseTransactions = transactionDao.getTransactionsByType(testUser.id, TransactionType.EXPENSE).first()

        // Then
        assertEquals(1, expenseTransactions.size)
        assertEquals(TransactionType.EXPENSE, expenseTransactions[0].type)
    }

    @Test
    fun getRecurringTransactions_shouldReturnRecurringTransactions() = runTest {
        // Given
        val recurringTransaction = TransactionEntity(
            userId = testUser.id,
            amount = 50.0,
            description = "Monthly subscription",
            category = "Subscription",
            type = TransactionType.EXPENSE,
            date = Date(),
            isRecurring = true,
            recurringPeriod = RecurringPeriod.MONTHLY
        )
        val nonRecurringTransaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "One-time purchase",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            isRecurring = false
        )
        transactionDao.insertTransaction(recurringTransaction)
        transactionDao.insertTransaction(nonRecurringTransaction)

        // When
        val recurringTransactions = transactionDao.getRecurringTransactions(testUser.id).first()

        // Then
        assertEquals(1, recurringTransactions.size)
        assertTrue(recurringTransactions[0].isRecurring)
    }

    @Test
    fun getTotalIncome_shouldReturnCorrectSum() = runTest {
        // Given
        val calendar = Calendar.getInstance()
        val startDate = calendar.time
        calendar.add(Calendar.MONTH, 1)
        val endDate = calendar.time
        
        val income1 = TransactionEntity(
            userId = testUser.id,
            amount = 1000.0,
            description = "Salary",
            category = "Salary",
            type = TransactionType.INCOME,
            date = Date()
        )
        val income2 = TransactionEntity(
            userId = testUser.id,
            amount = 500.0,
            description = "Bonus",
            category = "Bonus",
            type = TransactionType.INCOME,
            date = Date()
        )
        val expense = TransactionEntity(
            userId = testUser.id,
            amount = 200.0,
            description = "Expense",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        transactionDao.insertTransaction(income1)
        transactionDao.insertTransaction(income2)
        transactionDao.insertTransaction(expense)

        // When
        val totalIncome = transactionDao.getTotalIncome(testUser.id, startDate, endDate)

        // Then
        assertEquals(1500.0, totalIncome, 0.01)
    }

    @Test
    fun getTotalExpense_shouldReturnCorrectSum() = runTest {
        // Given
        val calendar = Calendar.getInstance()
        val startDate = calendar.time
        calendar.add(Calendar.MONTH, 1)
        val endDate = calendar.time
        
        val expense1 = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Food",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val expense2 = TransactionEntity(
            userId = testUser.id,
            amount = 200.0,
            description = "Transport",
            category = "Transport",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val income = TransactionEntity(
            userId = testUser.id,
            amount = 1000.0,
            description = "Income",
            category = "Salary",
            type = TransactionType.INCOME,
            date = Date()
        )
        transactionDao.insertTransaction(expense1)
        transactionDao.insertTransaction(expense2)
        transactionDao.insertTransaction(income)

        // When
        val totalExpense = transactionDao.getTotalExpense(testUser.id, startDate, endDate)

        // Then
        assertEquals(300.0, totalExpense, 0.01)
    }

    @Test
    fun getBalance_shouldReturnCorrectBalance() = runTest {
        // Given
        val income = TransactionEntity(
            userId = testUser.id,
            amount = 1000.0,
            description = "Income",
            category = "Salary",
            type = TransactionType.INCOME,
            date = Date()
        )
        val expense = TransactionEntity(
            userId = testUser.id,
            amount = 300.0,
            description = "Expense",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        transactionDao.insertTransaction(income)
        transactionDao.insertTransaction(expense)

        // When
        val balance = transactionDao.getBalance(testUser.id)

        // Then
        assertEquals(700.0, balance, 0.01) // 1000 - 300
    }

    @Test
    fun getTotalByTypeAndDateRange_shouldReturnCorrectSum() = runTest {
        // Given
        val calendar = Calendar.getInstance()
        val startDate = calendar.time
        calendar.add(Calendar.MONTH, 1)
        val endDate = calendar.time
        
        val expense1 = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Food 1",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val expense2 = TransactionEntity(
            userId = testUser.id,
            amount = 150.0,
            description = "Food 2",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        transactionDao.insertTransaction(expense1)
        transactionDao.insertTransaction(expense2)

        // When
        val totalExpense = transactionDao.getTotalByTypeAndDateRange(testUser.id, TransactionType.EXPENSE, startDate, endDate)

        // Then
        assertEquals(250.0, totalExpense, 0.01)
    }

    @Test
    fun getTransactionCount_shouldReturnCorrectCount() = runTest {
        // Given
        val transaction1 = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Transaction 1",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val transaction2 = TransactionEntity(
            userId = testUser.id,
            amount = 200.0,
            description = "Transaction 2",
            category = "Transport",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2)

        // When
        val count = transactionDao.getTransactionCount(testUser.id)

        // Then
        assertEquals(2, count)
    }

    @Test
    fun updateTransaction_shouldUpdateTransaction() = runTest {
        // Given
        val transaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Original description",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val transactionId = transactionDao.insertTransaction(transaction)
        val originalTransaction = transactionDao.getTransactionById(transactionId)!!

        // When
        val updatedTransaction = originalTransaction.copy(
            description = "Updated description",
            amount = 150.0
        )
        transactionDao.updateTransaction(updatedTransaction)

        // Then
        val retrievedTransaction = transactionDao.getTransactionById(transactionId)
        assertEquals("Updated description", retrievedTransaction!!.description)
        assertEquals(150.0, retrievedTransaction.amount, 0.01)
    }

    @Test
    fun deleteTransaction_shouldRemoveTransaction() = runTest {
        // Given
        val transaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val transactionId = transactionDao.insertTransaction(transaction)
        assertNotNull(transactionDao.getTransactionById(transactionId))

        // When
        val transactionToDelete = transactionDao.getTransactionById(transactionId)!!
        transactionDao.deleteTransaction(transactionToDelete)

        // Then
        assertNull(transactionDao.getTransactionById(transactionId))
    }
}
