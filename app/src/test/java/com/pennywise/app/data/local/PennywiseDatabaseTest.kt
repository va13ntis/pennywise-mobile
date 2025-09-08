package com.pennywise.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.Currency
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for PennywiseDatabase
 * Tests database configuration, entity relationships, and data integrity
 */
@RunWith(AndroidJUnit4::class)
class PennywiseDatabaseTest {

    private lateinit var database: PennywiseDatabase
    private lateinit var userDao: com.pennywise.app.data.local.dao.UserDao
    private lateinit var transactionDao: com.pennywise.app.data.local.dao.TransactionDao
    private lateinit var currencyUsageDao: com.pennywise.app.data.local.dao.CurrencyUsageDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PennywiseDatabase::class.java
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
        // Create test user
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )

        // Insert user
        userDao.insertUser(user)

        // Retrieve user
        val retrievedUser = userDao.getUserById(1)
        assertNotNull(retrievedUser)
        assertEquals(user.email, retrievedUser.email)
        assertEquals(user.passwordHash, retrievedUser.passwordHash)

        // Update user
        val updatedUser = user.copy(email = "updated@example.com")
        userDao.updateUser(updatedUser)

        val retrievedUpdatedUser = userDao.getUserById(1)
        assertNotNull(retrievedUpdatedUser)
        assertEquals("updated@example.com", retrievedUpdatedUser.email)

        // Delete user
        userDao.deleteUser(1)
        val deletedUser = userDao.getUserById(1)
        assertNull(deletedUser)
    }

    @Test
    fun testTransactionEntityOperations() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
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
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Insert transaction
        transactionDao.insertTransaction(transaction)

        // Retrieve transaction
        val retrievedTransaction = transactionDao.getTransactionById(1)
        assertNotNull(retrievedTransaction)
        assertEquals(transaction.amount, retrievedTransaction.amount)
        assertEquals(transaction.description, retrievedTransaction.description)

        // Update transaction
        val updatedTransaction = transaction.copy(amount = 150.0)
        transactionDao.updateTransaction(updatedTransaction)

        val retrievedUpdatedTransaction = transactionDao.getTransactionById(1)
        assertNotNull(retrievedUpdatedTransaction)
        assertEquals(150.0, retrievedUpdatedTransaction.amount)

        // Delete transaction
        transactionDao.deleteTransaction(1)
        val deletedTransaction = transactionDao.getTransactionById(1)
        assertNull(deletedTransaction)
    }

    @Test
    fun testCurrencyUsageEntityOperations() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create test currency usage
        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currencyCode = "USD",
            usageCount = 5,
            lastUsedAt = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )

        // Insert currency usage
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Retrieve currency usage
        val retrievedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)
        assertNotNull(retrievedCurrencyUsage)
        assertEquals(currencyUsage.currencyCode, retrievedCurrencyUsage.currencyCode)
        assertEquals(currencyUsage.usageCount, retrievedCurrencyUsage.usageCount)

        // Update currency usage
        val updatedCurrencyUsage = currencyUsage.copy(usageCount = 10)
        currencyUsageDao.updateCurrencyUsage(updatedCurrencyUsage)

        val retrievedUpdatedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)
        assertNotNull(retrievedUpdatedCurrencyUsage)
        assertEquals(10, retrievedUpdatedCurrencyUsage.usageCount)

        // Delete currency usage
        currencyUsageDao.deleteCurrencyUsage(1)
        val deletedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)
        assertNull(deletedCurrencyUsage)
    }

    @Test
    fun testCurrencyUsageIncrement() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create initial currency usage
        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currencyCode = "USD",
            usageCount = 1,
            lastUsedAt = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Increment usage
        currencyUsageDao.incrementUsage(1, "USD")

        // Verify increment
        val updatedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)
        assertNotNull(updatedCurrencyUsage)
        assertEquals(2, updatedCurrencyUsage.usageCount)
    }

    @Test
    fun testCurrencyUsageByUser() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create multiple currency usage records
        val currencyUsages = listOf(
            CurrencyUsageEntity(
                id = 1,
                userId = 1,
                currencyCode = "USD",
                usageCount = 5,
                lastUsedAt = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                id = 2,
                userId = 1,
                currencyCode = "EUR",
                usageCount = 3,
                lastUsedAt = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                id = 3,
                userId = 1,
                currencyCode = "GBP",
                usageCount = 2,
                lastUsedAt = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        currencyUsages.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // Retrieve currency usage by user
        val userCurrencyUsages = currencyUsageDao.getCurrencyUsageByUser(1)
        assertEquals(3, userCurrencyUsages.size)

        // Verify currencies are present
        val currencyCodes = userCurrencyUsages.map { it.currencyCode }
        assertTrue(currencyCodes.contains("USD"))
        assertTrue(currencyCodes.contains("EUR"))
        assertTrue(currencyCodes.contains("GBP"))
    }

    @Test
    fun testTopCurrenciesByUser() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create currency usage records with different usage counts
        val currencyUsages = listOf(
            CurrencyUsageEntity(
                id = 1,
                userId = 1,
                currencyCode = "USD",
                usageCount = 10,
                lastUsedAt = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                id = 2,
                userId = 1,
                currencyCode = "EUR",
                usageCount = 5,
                lastUsedAt = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            CurrencyUsageEntity(
                id = 3,
                userId = 1,
                currencyCode = "GBP",
                usageCount = 15,
                lastUsedAt = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        currencyUsages.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // Retrieve top currencies (should be ordered by usage count)
        val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(1, 2)
        assertEquals(2, topCurrencies.size)

        // Verify ordering (GBP should be first with 15, USD second with 10)
        assertEquals("GBP", topCurrencies[0].currencyCode)
        assertEquals(15, topCurrencies[0].usageCount)
        assertEquals("USD", topCurrencies[1].currencyCode)
        assertEquals(10, topCurrencies[1].usageCount)
    }

    @Test
    fun testTransactionByUser() = runBlocking {
        // Create test user first
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
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
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        transactions.forEach { transactionDao.insertTransaction(it) }

        // Retrieve transactions by user
        val userTransactions = transactionDao.getTransactionsByUser(1)
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
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Measure insertion performance
        val startTime = System.currentTimeMillis()
        
        // Insert 1000 transactions
        for (i in 1..1000) {
            val transaction = TransactionEntity(
                id = i,
                userId = 1,
                amount = i * 10.0,
                description = "Transaction $i",
                category = "Category $i",
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
        val transactionCount = transactionDao.getTransactionsByUser(1).size
        assertEquals(1000, transactionCount)
    }

    @Test
    fun testDatabaseIntegrity() = runBlocking {
        // Create test user
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
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
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        transactionDao.insertTransaction(transaction)

        // Create currency usage
        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currencyCode = "USD",
            usageCount = 1,
            lastUsedAt = Date(),
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
        assertEquals(retrievedUser.id, retrievedTransaction.userId)
        assertEquals(retrievedUser.id, retrievedCurrencyUsage.userId)
    }
}
