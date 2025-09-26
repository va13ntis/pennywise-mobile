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
        val retrievedUser = userDao.getUserById(userId)
        assertNotNull(retrievedUser)
        assertEquals(user.defaultCurrency, retrievedUser?.defaultCurrency)
        assertEquals(user.locale, retrievedUser?.locale)

        // Update user
        val updatedUser = retrievedUser!!.copy(defaultCurrency = "EUR")
        userDao.updateUser(updatedUser)

        val retrievedUpdatedUser = userDao.getUserById(userId)
        assertNotNull(retrievedUpdatedUser)
        assertEquals("EUR", retrievedUpdatedUser?.defaultCurrency)

        // Delete user
        userDao.deleteUser(updatedUser)
        val deletedUser = userDao.getUserById(userId)
        assertNull(deletedUser)
    }

    @Test
    fun testTransactionEntityOperations() = runBlocking {
        // Create test user first
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        val userId = userDao.insertUser(user)

        // Create test transaction
        val transaction = TransactionEntity(
            userId = userId,
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
        // Create test user first
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        val userId = userDao.insertUser(user)

        // Create test currency usage
        val currencyUsage = CurrencyUsageEntity(
            userId = userId,
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
        // Create test user first
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        val userId = userDao.insertUser(user)

        // Create initial currency usage
        val currencyUsage = CurrencyUsageEntity(
            userId = userId,
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        val currencyUsageId = currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Increment usage
        currencyUsageDao.incrementCurrencyUsage(userId, "USD", Date(), Date())

        // Verify increment
        val updatedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(currencyUsageId)
        assertNotNull(updatedCurrencyUsage)
        assertEquals(2, updatedCurrencyUsage?.usageCount)
    }

    @Test
    fun testCurrencyUsageByUser() = runBlocking {
        // Create test user first
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        val userId = userDao.insertUser(user)

        // Create multiple currency usage records
        val currencyUsages = listOf(
            CurrencyUsageEntity(
                userId = userId,
                currency = "USD",
                usageCount = 5,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                userId = userId,
                currency = "EUR",
                usageCount = 3,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                userId = userId,
                currency = "GBP",
                usageCount = 2,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        currencyUsages.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // Retrieve currency usage by user
        val userCurrencyUsages = currencyUsageDao.getCurrencyUsageByUser(userId).first()
        assertEquals(3, userCurrencyUsages.size)

        // Verify currencies are present
        val currencyCodes = userCurrencyUsages.map { it.currency }
        assertTrue(currencyCodes.contains("USD"))
        assertTrue(currencyCodes.contains("EUR"))
        assertTrue(currencyCodes.contains("GBP"))
    }

    @Test
    fun testTopCurrenciesByUser() = runBlocking {
        // Create test user first
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        val userId = userDao.insertUser(user)

        // Create currency usage records with different usage counts
        val currencyUsages = listOf(
            CurrencyUsageEntity(
                userId = userId,
                currency = "USD",
                usageCount = 10,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                userId = userId,
                currency = "EUR",
                usageCount = 5,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                userId = userId,
                currency = "GBP",
                usageCount = 15,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        currencyUsages.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // Retrieve top currencies (should be ordered by usage count)
        val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(userId, 2).first()
        assertEquals(2, topCurrencies.size)

        // Verify ordering (GBP should be first with 15, USD second with 10)
        assertEquals("GBP", topCurrencies[0].currency)
        assertEquals(15, topCurrencies[0].usageCount)
        assertEquals("USD", topCurrencies[1].currency)
        assertEquals(10, topCurrencies[1].usageCount)
    }

    @Test
    fun testTransactionByUser() = runBlocking {
        // Create test user first
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        val userId = userDao.insertUser(user)

        // Create multiple transactions
        val transactions = listOf(
            TransactionEntity(
                userId = userId,
                amount = 100.0,
                description = "Transaction 1",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            TransactionEntity(
                userId = userId,
                amount = 200.0,
                description = "Transaction 2",
                category = "Transport",
                type = TransactionType.EXPENSE,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            TransactionEntity(
                userId = userId,
                amount = 300.0,
                description = "Transaction 3",
                category = "Entertainment",
                type = TransactionType.EXPENSE,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        val transactionIds = transactions.map { transactionDao.insertTransaction(it) }

        // Retrieve transactions by user
        val userTransactions = transactionDao.getTransactionsByUser(userId).first()
        assertEquals(3, userTransactions.size)

        // Verify all transactions are present
        val retrievedTransactionIds = userTransactions.map { it.id }
        assertTrue(retrievedTransactionIds.containsAll(transactionIds))
    }

    @Test
    fun testDatabaseConstraints() = runBlocking {
        // Test foreign key constraint - should fail when inserting transaction with non-existent user
        val transaction = TransactionEntity(
            userId = 999, // Non-existent user
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )

        try {
            transactionDao.insertTransaction(transaction)
            assert(false) // Should not reach here
        } catch (e: Exception) {
            // Expected to fail due to foreign key constraint
            assert(true)
        }
    }

    @Test
    fun testDatabasePerformance() = runBlocking {
        // Create test user first
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        val userId = userDao.insertUser(user)

        // Measure insertion performance
        val startTime = System.currentTimeMillis()
        
        // Insert 1000 transactions
        for (i in 1..1000) {
            val transaction = TransactionEntity(
                userId = userId,
                amount = i * 10.0,
                description = "Transaction $i",
                category = "Category $i",
                type = TransactionType.EXPENSE,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            transactionDao.insertTransaction(transaction)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Verify insertion completed within reasonable time (less than 10 seconds)
        assert(duration < 10000) { "Insertion took too long: ${duration}ms" }

        // Verify all transactions were inserted
        val transactionCount = transactionDao.getTransactionsByUser(userId).first().size
        assertEquals(1000, transactionCount)
    }

    @Test
    fun testDatabaseIntegrity() = runBlocking {
        // Create test user
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        val userId = userDao.insertUser(user)

        // Create transaction
        val transaction = TransactionEntity(
            userId = userId,
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        val transactionId = transactionDao.insertTransaction(transaction)

        // Create currency usage
        val currencyUsage = CurrencyUsageEntity(
            userId = userId,
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        val currencyUsageId = currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Verify data integrity
        val retrievedUser = userDao.getUserById(userId)
        val retrievedTransaction = transactionDao.getTransactionById(transactionId)
        val retrievedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(currencyUsageId)

        assertNotNull(retrievedUser)
        assertNotNull(retrievedTransaction)
        assertNotNull(retrievedCurrencyUsage)

        // Verify relationships
        assertEquals(retrievedUser?.id, retrievedTransaction?.userId)
        assertEquals(retrievedUser?.id, retrievedCurrencyUsage?.userId)
    }
}
