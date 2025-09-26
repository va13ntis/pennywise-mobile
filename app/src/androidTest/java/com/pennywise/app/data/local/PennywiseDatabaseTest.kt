package com.pennywise.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.PennyWiseDatabase
import org.junit.jupiter.api.Assertions.*
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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

    @BeforeEach
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PennyWiseDatabase::class.java
        ).allowMainThreadQueries().build()

        userDao = database.userDao()
        transactionDao = database.transactionDao()
        currencyUsageDao = database.currencyUsageDao()
    }

    @AfterEach
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
        // Create test user
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )

        // Insert user
        userDao.insertUser(user)

        // Retrieve user
        val retrievedUser = userDao.getUserById(1)
        assertNotNull(retrievedUser)
        assertEquals(user.defaultCurrency, retrievedUser?.defaultCurrency)
        assertEquals(user.locale, retrievedUser?.locale)

        // Update user
        val updatedUser = user.copy(defaultCurrency = "EUR")
        userDao.updateUser(updatedUser)

        val retrievedUpdatedUser = userDao.getUserById(1)
        assertNotNull(retrievedUpdatedUser)
        assertEquals("EUR", retrievedUpdatedUser?.defaultCurrency)

        // Delete user
        val userToDelete = userDao.getUserById(1)
        userToDelete?.let { userDao.deleteUser(it) }
        val deletedUser = userDao.getUserById(1)
        assertNull(deletedUser)
    }

    @Test
    fun testTransactionEntityOperations() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create test transaction
        val transaction = TransactionEntity(
            id = 1,
            userId = 1,
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = com.pennywise.app.domain.model.TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Insert transaction
        transactionDao.insertTransaction(transaction)

        // Retrieve transaction
        val retrievedTransaction = transactionDao.getTransactionById(1)
        assertNotNull(retrievedTransaction)
        assertEquals(transaction.amount, retrievedTransaction?.amount)
        assertEquals(transaction.description, retrievedTransaction?.description)

        // Update transaction
        val updatedTransaction = transaction.copy(amount = 150.0)
        transactionDao.updateTransaction(updatedTransaction)

        val retrievedUpdatedTransaction = transactionDao.getTransactionById(1)
        assertNotNull(retrievedUpdatedTransaction)
        assertEquals(150.0, retrievedUpdatedTransaction?.amount)

        // Delete transaction
        val transactionToDelete = transactionDao.getTransactionById(1)
        transactionToDelete?.let { transactionDao.deleteTransaction(it) }
        val deletedTransaction = transactionDao.getTransactionById(1)
        assertNull(deletedTransaction)
    }

    @Test
    fun testCurrencyUsageEntityOperations() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create test currency usage
        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currency = "USD",
            usageCount = 5,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Insert currency usage
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Retrieve currency usage
        val retrievedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)
        assertNotNull(retrievedCurrencyUsage)
        assertEquals(currencyUsage.currency, retrievedCurrencyUsage?.currency)
        assertEquals(currencyUsage.usageCount, retrievedCurrencyUsage?.usageCount)

        // Update currency usage
        val updatedCurrencyUsage = currencyUsage.copy(usageCount = 10)
        currencyUsageDao.updateCurrencyUsage(updatedCurrencyUsage)

        val retrievedUpdatedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)
        assertNotNull(retrievedUpdatedCurrencyUsage)
        assertEquals(10, retrievedUpdatedCurrencyUsage?.usageCount)

        // Delete currency usage
        val currencyUsageToDelete = currencyUsageDao.getCurrencyUsageById(1)
        currencyUsageToDelete?.let { currencyUsageDao.deleteCurrencyUsage(it) }
        val deletedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)
        assertNull(deletedCurrencyUsage)
    }

    @Test
    fun testCurrencyUsageIncrement() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create initial currency usage
        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Increment usage
        currencyUsageDao.incrementCurrencyUsage(1, "USD", Date(), Date())

        // Verify increment
        val updatedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)
        assertNotNull(updatedCurrencyUsage)
        assertEquals(2, updatedCurrencyUsage?.usageCount)
    }

    @Test
    fun testCurrencyUsageByUser() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create multiple currency usage records
        val currencyUsages = listOf(
            CurrencyUsageEntity(
                id = 1,
                userId = 1,
                currency = "USD",
                usageCount = 5,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                id = 2,
                userId = 1,
                currency = "EUR",
                usageCount = 3,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                id = 3,
                userId = 1,
                currency = "GBP",
                usageCount = 2,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        currencyUsages.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // Retrieve currency usage by user
        val userCurrencyUsages = currencyUsageDao.getCurrencyUsageByUser(1).first()
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
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create currency usage records with different usage counts
        val currencyUsages = listOf(
            CurrencyUsageEntity(
                id = 1,
                userId = 1,
                currency = "USD",
                usageCount = 10,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                id = 2,
                userId = 1,
                currency = "EUR",
                usageCount = 5,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                id = 3,
                userId = 1,
                currency = "GBP",
                usageCount = 15,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        currencyUsages.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // Retrieve top currencies (should be ordered by usage count)
        val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(1, 2).first()
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
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create multiple transactions
        val transactions = listOf(
            TransactionEntity(
                id = 1,
                userId = 1,
                amount = 100.0,
                description = "Transaction 1",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            TransactionEntity(
                id = 2,
                userId = 1,
                amount = 200.0,
                description = "Transaction 2",
                category = "Transport",
                type = TransactionType.EXPENSE,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            TransactionEntity(
                id = 3,
                userId = 1,
                amount = 300.0,
                description = "Transaction 3",
                category = "Entertainment",
                type = TransactionType.EXPENSE,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        transactions.forEach { transactionDao.insertTransaction(it) }

        // Retrieve transactions by user
        val userTransactions = transactionDao.getTransactionsByUser(1).first()
        assertEquals(3, userTransactions.size)

        // Verify all transactions are present
        val transactionIds = userTransactions.map { it.id }
        assertTrue(transactionIds.contains(1))
        assertTrue(transactionIds.contains(2))
        assertTrue(transactionIds.contains(3))
    }

    @Test
    fun testDatabaseConstraints() = runBlocking {
        // Test foreign key constraint - should fail when inserting transaction with non-existent user
        val transaction = TransactionEntity(
            id = 1,
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
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Measure insertion performance
        val startTime = System.currentTimeMillis()
        
        // Insert 1000 transactions
        for (i in 1..1000) {
            val transaction = TransactionEntity(
                id = i.toLong(),
                userId = 1,
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
        val transactionCount = transactionDao.getTransactionsByUser(1).first().size
        assertEquals(1000, transactionCount)
    }

    @Test
    fun testDatabaseIntegrity() = runBlocking {
        // Create test user
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create transaction
        val transaction = TransactionEntity(
            id = 1,
            userId = 1,
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        transactionDao.insertTransaction(transaction)

        // Create currency usage
        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Verify data integrity
        val retrievedUser = userDao.getUserById(1)
        val retrievedTransaction = transactionDao.getTransactionById(1)
        val retrievedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)

        assertNotNull(retrievedUser)
        assertNotNull(retrievedTransaction)
        assertNotNull(retrievedCurrencyUsage)

        // Verify relationships
        assertEquals(retrievedUser?.id, retrievedTransaction?.userId)
        assertEquals(retrievedUser?.id, retrievedCurrencyUsage?.userId)
    }
}
