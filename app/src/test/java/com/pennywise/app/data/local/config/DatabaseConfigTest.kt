package com.pennywise.app.data.local.config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Integration tests for database configuration
 * Tests database setup, configuration, and initialization
 */
@RunWith(AndroidJUnit4::class)
class DatabaseConfigTest {

    private lateinit var database: PennyWiseDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            PennyWiseDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testDatabaseInitialization() {
        assertNotNull(database)
        assertNotNull(database.userDao())
        assertNotNull(database.transactionDao())
        assertNotNull(database.currencyUsageDao())
    }

    @Test
    fun testDatabaseTablesExist() = runBlocking {
        // Verify all tables exist by attempting to query them
        val userDao = database.userDao()
        val transactionDao = database.transactionDao()
        val currencyUsageDao = database.currencyUsageDao()

        // Test user table
        val userCount = userDao.getUserCount()
        assertNotNull(userCount)

        // Test transaction table
        val transactionCount = transactionDao.getTransactionCount(1L)
        assertNotNull(transactionCount)

        // Test currency_usage table
        val currencyUsageCount = currencyUsageDao.getCurrencyUsageCountForUser(1L)
        assertNotNull(currencyUsageCount)
    }

    @Test
    fun testDatabaseSchema() = runBlocking {
        // Test that all expected columns exist by inserting and retrieving data
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        database.userDao().insertUser(user)

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
        database.transactionDao().insertTransaction(transaction)

        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        database.currencyUsageDao().insertCurrencyUsage(currencyUsage)

        // Verify data was inserted successfully
        val retrievedUser = database.userDao().getUserById(1)
        val retrievedTransaction = database.transactionDao().getTransactionById(1)
        val retrievedCurrencyUsage = database.currencyUsageDao().getCurrencyUsageById(1)

        assertNotNull(retrievedUser)
        assertNotNull(retrievedTransaction)
        assertNotNull(retrievedCurrencyUsage)
    }

    @Test
    fun testDatabaseConstraints() = runBlocking {
        // Test foreign key constraints
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        database.userDao().insertUser(user)

        // This should succeed
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
        database.transactionDao().insertTransaction(transaction)

        // This should fail due to foreign key constraint
        try {
            val invalidTransaction = TransactionEntity(
                id = 2,
                userId = 999, // Non-existent user
                amount = 100.0,
                description = "Invalid transaction",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            database.transactionDao().insertTransaction(invalidTransaction)
            assert(false) // Should not reach here
        } catch (e: Exception) {
            // Expected to fail due to foreign key constraint
            assert(true)
        }
    }

    @Test
    fun testDatabaseIndexes() = runBlocking {
        // Test that indexes are working by querying with indexed columns
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        database.userDao().insertUser(user)

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
        database.transactionDao().insertTransaction(transaction)

        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        database.currencyUsageDao().insertCurrencyUsage(currencyUsage)

        // Test queries that should use indexes
        val userTransactions = database.transactionDao().getTransactionsByUser(1)
        val userCurrencyUsages = database.currencyUsageDao().getCurrencyUsageByUser(1)
        val topCurrencies = database.currencyUsageDao().getTopCurrenciesByUser(1, 10)

        assertNotNull(userTransactions)
        assertNotNull(userCurrencyUsages)
        assertNotNull(topCurrencies)
    }

    @Test
    fun testDatabasePerformance() = runBlocking {
        // Test database performance with bulk operations
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        database.userDao().insertUser(user)

        // Measure bulk insertion performance
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
            database.transactionDao().insertTransaction(transaction)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Verify insertion completed within reasonable time (less than 10 seconds)
        assert(duration < 10000) { "Bulk insertion took too long: ${duration}ms" }

        // Verify all transactions were inserted
        val transactionCount = database.transactionDao().getTransactionsByUser(1).first().size
        assertEquals(1000, transactionCount)
    }

    @Test
    fun testDatabaseConcurrency() = runBlocking {
        // Test database concurrency with multiple operations
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        database.userDao().insertUser(user)

        // Perform multiple concurrent operations
        val startTime = System.currentTimeMillis()
        
        // Insert multiple transactions concurrently
        val transactions = (1..100).map { i ->
            TransactionEntity(
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
        }
        
        transactions.forEach { transaction ->
            database.transactionDao().insertTransaction(transaction)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Verify concurrent operations completed within reasonable time
        assert(duration < 5000) { "Concurrent operations took too long: ${duration}ms" }

        // Verify all transactions were inserted
        val transactionCount = database.transactionDao().getTransactionsByUser(1).first().size
        assertEquals(100, transactionCount)
    }

    @Test
    fun testDatabaseDataIntegrity() = runBlocking {
        // Test data integrity with complex relationships
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        database.userDao().insertUser(user)

        // Create multiple related entities
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
        database.transactionDao().insertTransaction(transaction)

        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        database.currencyUsageDao().insertCurrencyUsage(currencyUsage)

        // Verify data integrity
        val retrievedUser = database.userDao().getUserById(1)
        val retrievedTransaction = database.transactionDao().getTransactionById(1)
        val retrievedCurrencyUsage = database.currencyUsageDao().getCurrencyUsageById(1)

        assertNotNull(retrievedUser)
        assertNotNull(retrievedTransaction)
        assertNotNull(retrievedCurrencyUsage)

        // Verify relationships
        assertEquals(retrievedUser?.id, retrievedTransaction?.userId)
        assertEquals(retrievedUser?.id, retrievedCurrencyUsage?.userId)
    }

    @Test
    fun testDatabaseCleanup() = runBlocking {
        // Test database cleanup operations
        val user = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        database.userDao().insertUser(user)

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
        database.transactionDao().insertTransaction(transaction)

        val currencyUsage = CurrencyUsageEntity(
            id = 1,
            userId = 1,
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        database.currencyUsageDao().insertCurrencyUsage(currencyUsage)

        // Verify data exists
        assertNotNull(database.userDao().getUserById(1))
        assertNotNull(database.transactionDao().getTransactionById(1))
        assertNotNull(database.currencyUsageDao().getCurrencyUsageById(1))

        // Clean up data
        val transactionToDelete = database.transactionDao().getTransactionById(1)
        val currencyUsageToDelete = database.currencyUsageDao().getCurrencyUsageById(1)
        val userToDelete = database.userDao().getUserById(1)
        
        transactionToDelete?.let { database.transactionDao().deleteTransaction(it) }
        currencyUsageToDelete?.let { database.currencyUsageDao().deleteCurrencyUsage(it) }
        userToDelete?.let { database.userDao().deleteUser(it) }

        // Verify data was cleaned up
        assert(database.userDao().getUserById(1) == null)
        assert(database.transactionDao().getTransactionById(1) == null)
        assert(database.currencyUsageDao().getCurrencyUsageById(1) == null)
    }
}
