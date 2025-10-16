package com.pennywise.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.PennyWiseDatabase
import org.junit.Assert.*
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Instrumented integration tests for PennywiseDatabase
 * Tests database configuration, entity relationships, and data integrity
 */
@RunWith(AndroidJUnit4::class)
class PennywiseDatabaseTest {

    private lateinit var database: PennyWiseDatabase
    private lateinit var userDao: com.pennywise.app.data.local.dao.UserDao
    private lateinit var transactionDao: com.pennywise.app.data.local.dao.TransactionDao
    private lateinit var currencyUsageDao: com.pennywise.app.data.local.dao.CurrencyUsageDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PennyWiseDatabase::class.java
        ).allowMainThreadQueries().build()

        userDao = database.userDao()
        transactionDao = database.transactionDao()
        currencyUsageDao = database.currencyUsageDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDatabaseCreation() {
        assertNotNull(database)
        assertNotNull(userDao)
        assertNotNull(transactionDao)
        assertNotNull(currencyUsageDao)
    }

    @Test
    fun testUserEntityOperations() = runBlocking {
        // Create test user (ID will be auto-generated)
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )

        // Insert user and get the generated ID
        val userId = userDao.insertUser(user)
        assertTrue(userId > 0)

        // Retrieve user
        val retrievedUser = userDao.getUser()
        assertNotNull(retrievedUser)
        assertEquals(user.defaultCurrency, retrievedUser?.defaultCurrency)
        assertEquals(user.locale, retrievedUser?.locale)

        // Update user
        val updatedUser = retrievedUser!!.copy(defaultCurrency = "EUR")
        userDao.updateUser(updatedUser)

        val retrievedUpdatedUser = userDao.getUser()
        assertNotNull(retrievedUpdatedUser)
        assertEquals("EUR", retrievedUpdatedUser?.defaultCurrency)

        // Delete user
        userDao.deleteUser(updatedUser)
        val deletedUser = userDao.getUser()
        assertNull(deletedUser)
    }

    @Test
    fun testTransactionEntityOperations() = runBlocking {
        val transaction = TransactionEntity(
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = com.pennywise.app.domain.model.TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Insert transaction and get the generated ID
        val transactionId = transactionDao.insertTransaction(transaction)
        assertTrue(transactionId > 0)

        // Retrieve transaction
        val retrievedTransaction = transactionDao.getTransactionById(transactionId)
        assertNotNull(retrievedTransaction)
        assertEquals(transaction.amount, retrievedTransaction?.amount)
        assertEquals(transaction.description, retrievedTransaction?.description)

        // Update transaction
        val updatedTransaction = retrievedTransaction!!.copy(amount = 150.0)
        transactionDao.updateTransaction(updatedTransaction)

        val retrievedUpdatedTransaction = transactionDao.getTransactionById(transactionId)
        assertNotNull(retrievedUpdatedTransaction)
        assertEquals(150.0, retrievedUpdatedTransaction?.amount)

        // Delete transaction
        transactionDao.deleteTransaction(updatedTransaction)
        val deletedTransaction = transactionDao.getTransactionById(transactionId)
        assertNull(deletedTransaction)
    }

    @Test
    fun testCurrencyUsageEntityOperations() = runBlocking {
        val currencyUsage = CurrencyUsageEntity(
            currency = "USD",
            usageCount = 5,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Insert currency usage and get the generated ID
        val currencyUsageId = currencyUsageDao.insertCurrencyUsage(currencyUsage)
        assertTrue(currencyUsageId > 0)

        // Retrieve currency usage
        val retrievedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(currencyUsageId)
        assertNotNull(retrievedCurrencyUsage)
        assertEquals(currencyUsage.currency, retrievedCurrencyUsage?.currency)
        assertEquals(currencyUsage.usageCount, retrievedCurrencyUsage?.usageCount)

        // Update currency usage
        val updatedCurrencyUsage = retrievedCurrencyUsage!!.copy(usageCount = 10)
        currencyUsageDao.updateCurrencyUsage(updatedCurrencyUsage)

        val retrievedUpdatedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(currencyUsageId)
        assertNotNull(retrievedUpdatedCurrencyUsage)
        assertEquals(10, retrievedUpdatedCurrencyUsage?.usageCount)

        // Delete currency usage
        currencyUsageDao.deleteCurrencyUsage(updatedCurrencyUsage)
        val deletedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(currencyUsageId)
        assertNull(deletedCurrencyUsage)
    }

    @Test
    fun testCurrencyUsageIncrement() = runBlocking {
        val now = Date()
        
        // Increment usage (will create if not exists)
        currencyUsageDao.insertOrIncrementCurrencyUsage("USD", now, now, now)

        // Verify creation
        val currencyUsage = currencyUsageDao.getCurrencyUsageByCurrency("USD")
        assertNotNull(currencyUsage)
        assertEquals(1, currencyUsage?.usageCount)
        
        // Increment again
        currencyUsageDao.insertOrIncrementCurrencyUsage("USD", now, now, now)
        
        // Verify increment
        val updatedCurrencyUsage = currencyUsageDao.getCurrencyUsageByCurrency("USD")
        assertNotNull(updatedCurrencyUsage)
        assertEquals(2, updatedCurrencyUsage?.usageCount)
    }

    @Test
    fun testAllCurrencyUsage() = runBlocking {
        val now = Date()
        val currencyUsages = listOf(
            CurrencyUsageEntity(
                currency = "USD",
                usageCount = 5,
                lastUsed = now,
                createdAt = now,
                updatedAt = now
            ),
            CurrencyUsageEntity(
                currency = "EUR",
                usageCount = 3,
                lastUsed = now,
                createdAt = now,
                updatedAt = now
            ),
            CurrencyUsageEntity(
                currency = "GBP",
                usageCount = 2,
                lastUsed = now,
                createdAt = now,
                updatedAt = now
            )
        )

        currencyUsages.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // Retrieve all currency usage
        val allCurrencyUsages = currencyUsageDao.getAllCurrencyUsage().first()
        assertEquals(3, allCurrencyUsages.size)

        // Verify currencies are present
        val currencyCodes = allCurrencyUsages.map { it.currency }
        assertTrue(currencyCodes.contains("USD"))
        assertTrue(currencyCodes.contains("EUR"))
        assertTrue(currencyCodes.contains("GBP"))
    }

    @Test
    fun testTopCurrencies() = runBlocking {
        val now = Date()
        val currencyUsages = listOf(
            CurrencyUsageEntity(
                currency = "USD",
                usageCount = 10,
                lastUsed = now,
                createdAt = now,
                updatedAt = now
            ),
            CurrencyUsageEntity(
                currency = "EUR",
                usageCount = 5,
                lastUsed = now,
                createdAt = now,
                updatedAt = now
            ),
            CurrencyUsageEntity(
                currency = "GBP",
                usageCount = 15,
                lastUsed = now,
                createdAt = now,
                updatedAt = now
            )
        )

        currencyUsages.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // Retrieve top currencies (should be ordered by usage count)
        val topCurrencies = currencyUsageDao.getTopCurrencies(2).first()
        assertEquals(2, topCurrencies.size)

        // Verify ordering (GBP should be first with 15, USD second with 10)
        assertEquals("GBP", topCurrencies[0].currency)
        assertEquals(15, topCurrencies[0].usageCount)
        assertEquals("USD", topCurrencies[1].currency)
        assertEquals(10, topCurrencies[1].usageCount)
    }

    @Test
    fun testAllTransactions() = runBlocking {
        val now = Date()
        val transactions = listOf(
            TransactionEntity(
                amount = 100.0,
                description = "Transaction 1",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = now,
                createdAt = now,
                updatedAt = now
            ),
            TransactionEntity(
                amount = 200.0,
                description = "Transaction 2",
                category = "Transport",
                type = TransactionType.EXPENSE,
                date = now,
                createdAt = now,
                updatedAt = now
            ),
            TransactionEntity(
                amount = 300.0,
                description = "Transaction 3",
                category = "Entertainment",
                type = TransactionType.EXPENSE,
                date = now,
                createdAt = now,
                updatedAt = now
            )
        )

        val transactionIds = transactions.map { transactionDao.insertTransaction(it) }

        // Retrieve all transactions
        val allTransactions = transactionDao.getAllTransactions().first()
        assertEquals(3, allTransactions.size)

        // Verify all transactions are present
        val retrievedTransactionIds = allTransactions.map { it.id }
        assertTrue(retrievedTransactionIds.containsAll(transactionIds))
    }

    @Test
    fun testDatabasePerformance() = runBlocking {
        val isCI = System.getenv("CI") == "true"
        val maxAllowed = if (isCI) 120000 else 10000
        
        val startTime = System.currentTimeMillis()
        val now = Date()
        
        // Insert 1000 transactions
        for (i in 1..1000) {
            val transaction = TransactionEntity(
                amount = i * 10.0,
                description = "Transaction $i",
                category = "Category ${i % 10}",
                type = TransactionType.EXPENSE,
                date = now,
                createdAt = now,
                updatedAt = now
            )
            transactionDao.insertTransaction(transaction)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Verify insertion completed within reasonable time (relaxed for CI)
        assert(duration < maxAllowed) { "Insertion took too long: ${duration}ms (max: ${maxAllowed}ms)" }

        // Verify all transactions were inserted
        val transactionCount = transactionDao.getTransactionCount()
        assertEquals(1000, transactionCount)
    }

    @Test
    fun testDatabaseIntegrity() = runBlocking {
        val now = Date()
        
        // Create transaction
        val transaction = TransactionEntity(
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = now,
            createdAt = now,
            updatedAt = now
        )
        val transactionId = transactionDao.insertTransaction(transaction)

        // Create currency usage
        val currencyUsage = CurrencyUsageEntity(
            currency = "USD",
            usageCount = 1,
            lastUsed = now,
            createdAt = now,
            updatedAt = now
        )
        val currencyUsageId = currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Verify data integrity
        val retrievedTransaction = transactionDao.getTransactionById(transactionId)
        val retrievedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(currencyUsageId)

        assertNotNull(retrievedTransaction)
        assertNotNull(retrievedCurrencyUsage)

        // Verify data consistency
        assertEquals("USD", retrievedTransaction?.currency)
        assertEquals("USD", retrievedCurrencyUsage?.currency)
    }

    @Test
    fun testTransactionsByCategory() = runBlocking {
        val now = Date()
        val transactions = listOf(
            TransactionEntity(
                amount = 100.0,
                description = "Groceries",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = now,
                createdAt = now,
                updatedAt = now
            ),
            TransactionEntity(
                amount = 200.0,
                description = "Restaurant",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = now,
                createdAt = now,
                updatedAt = now
            ),
            TransactionEntity(
                amount = 50.0,
                description = "Bus ticket",
                category = "Transport",
                type = TransactionType.EXPENSE,
                date = now,
                createdAt = now,
                updatedAt = now
            )
        )

        transactions.forEach { transactionDao.insertTransaction(it) }

        // Get transactions by category
        val foodTransactions = transactionDao.getTransactionsByCategory("Food").first()
        assertEquals(2, foodTransactions.size)
        
        val transportTransactions = transactionDao.getTransactionsByCategory("Transport").first()
        assertEquals(1, transportTransactions.size)
    }

    @Test
    fun testTransactionsByType() = runBlocking {
        val now = Date()
        val transactions = listOf(
            TransactionEntity(
                amount = 100.0,
                description = "Expense 1",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = now,
                createdAt = now,
                updatedAt = now
            ),
            TransactionEntity(
                amount = 200.0,
                description = "Income 1",
                category = "Salary",
                type = TransactionType.INCOME,
                date = now,
                createdAt = now,
                updatedAt = now
            ),
            TransactionEntity(
                amount = 50.0,
                description = "Expense 2",
                category = "Transport",
                type = TransactionType.EXPENSE,
                date = now,
                createdAt = now,
                updatedAt = now
            )
        )

        transactions.forEach { transactionDao.insertTransaction(it) }

        // Get transactions by type
        val expenses = transactionDao.getTransactionsByType(TransactionType.EXPENSE).first()
        assertEquals(2, expenses.size)
        
        val income = transactionDao.getTransactionsByType(TransactionType.INCOME).first()
        assertEquals(1, income.size)
    }
}
