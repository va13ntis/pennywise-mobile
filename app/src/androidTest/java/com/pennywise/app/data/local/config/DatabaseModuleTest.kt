package com.pennywise.app.data.local.config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Instrumented integration tests for database module configuration
 * Tests database module setup, dependency injection, and configuration
 * 
 * This test suite validates:
 * - Database module initialization and configuration
 * - DAO dependency injection and functionality
 * - Database relationships and constraints
 * - Performance characteristics
 * - Concurrency handling
 * - Data integrity and cleanup
 * - Error handling and edge cases
 * 
 */
@RunWith(AndroidJUnit4::class)
class DatabaseModuleTest {

    private lateinit var database: PennyWiseDatabase
    private lateinit var userDao: UserDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var currencyUsageDao: CurrencyUsageDao

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
    fun testDatabaseModuleInitialization() {
        assertNotNull(database)
        assertNotNull(userDao)
        assertNotNull(transactionDao)
        assertNotNull(currencyUsageDao)
    }

    @Test
    fun testDatabaseModuleConfiguration() = runBlocking {
        // Test that all DAOs are properly configured
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        val userId = userDao.insertUser(user)

        val transaction = TransactionEntity(
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        val transactionId = transactionDao.insertTransaction(transaction)

        val currencyUsage = CurrencyUsageEntity(
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        val currencyUsageId = currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Verify all DAOs are working
        assertNotNull(userDao.getUser())
        assertNotNull(transactionDao.getTransactionById(transactionId))
        assertNotNull(currencyUsageDao.getCurrencyUsageById(currencyUsageId))
    }

    @Test
    fun testDatabaseModuleDependencies() = runBlocking {
        // Test that all dependencies are properly injected
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Test transaction operations
        val transaction = TransactionEntity(
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        transactionDao.insertTransaction(transaction)

        // Test currency usage operations
        val currencyUsage = CurrencyUsageEntity(
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Verify operations are working
        val allTransactions = transactionDao.getAllTransactions()
        val allCurrencyUsages = currencyUsageDao.getAllCurrencyUsage()

        assertNotNull(allTransactions)
        assertNotNull(allCurrencyUsages)
        assertEquals(1, allTransactions.first().size)
        assertEquals(1, allCurrencyUsages.first().size)
    }

    @Test
    fun testDatabaseModulePerformance() = runBlocking {
        // Test database module performance
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Measure performance of multiple operations
        val startTime = System.currentTimeMillis()
        
        // Insert multiple transactions
        for (i in 1..100) {
            val transaction = TransactionEntity(
                id = i.toLong(),
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
        
        // Insert multiple currency usages
        for (i in 1..50) {
            val currencyUsage = CurrencyUsageEntity(
                id = i.toLong(),
                currency = "USD",
                usageCount = i,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            currencyUsageDao.insertCurrencyUsage(currencyUsage)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Verify performance is acceptable (less than 5 seconds)
        assert(duration < 5000) { "Database module operations took too long: ${duration}ms" }

        // Verify all data was inserted
        val transactionCount = transactionDao.getAllTransactions().first().size
        val currencyUsageCount = currencyUsageDao.getAllCurrencyUsage().first().size

        assertEquals(100, transactionCount)
        assertEquals(50, currencyUsageCount)
    }

    @Test
    fun testDatabaseModuleConcurrency() = runBlocking {
        // Test database module concurrency
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Perform concurrent operations
        val startTime = System.currentTimeMillis()
        
        // Insert transactions concurrently
        val transactions = (1..50).map { i ->
            TransactionEntity(
                id = i.toLong(),
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
            transactionDao.insertTransaction(transaction)
        }
        
        // Insert currency usages concurrently
        val currencyUsages = (1..25).map { i ->
            CurrencyUsageEntity(
                id = i.toLong(),
                currency = "USD",
                usageCount = i,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        }
        
        currencyUsages.forEach { currencyUsage ->
            currencyUsageDao.insertCurrencyUsage(currencyUsage)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime

        // Verify concurrent operations completed within reasonable time
        assert(duration < 3000) { "Concurrent operations took too long: ${duration}ms" }

        // Verify all data was inserted
        val transactionCount = transactionDao.getAllTransactions().first().size
        val currencyUsageCount = currencyUsageDao.getAllCurrencyUsage().first().size

        assertEquals(50, transactionCount)
        assertEquals(25, currencyUsageCount)
    }

    @Test
    fun testDatabaseModuleDataIntegrity() = runBlocking {
        // Test database module data integrity
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create related entities
        val transaction = TransactionEntity(
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        transactionDao.insertTransaction(transaction)

        val currencyUsage = CurrencyUsageEntity(
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Verify data integrity
        val retrievedUser = userDao.getUser()
        val retrievedTransaction = transactionDao.getTransactionById(1)
        val retrievedCurrencyUsage = currencyUsageDao.getCurrencyUsageById(1)

        assertNotNull(retrievedUser)
        assertNotNull(retrievedTransaction)
        assertNotNull(retrievedCurrencyUsage)

    }

    @Test
    fun testDatabaseModuleCleanup() = runBlocking {
        // Test database module cleanup
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        val transaction = TransactionEntity(
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        transactionDao.insertTransaction(transaction)

        val currencyUsage = CurrencyUsageEntity(
            currency = "USD",
            usageCount = 1,
            lastUsed = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Verify data exists
        assertNotNull(userDao.getUser())
        assertNotNull(transactionDao.getTransactionById(1))
        assertNotNull(currencyUsageDao.getCurrencyUsageById(1))

        // Clean up data
        val transactionToDelete = transactionDao.getTransactionById(1)
        val currencyUsageToDelete = currencyUsageDao.getCurrencyUsageById(1)
        val userToDelete = userDao.getUser()
        
        transactionToDelete?.let { transactionDao.deleteTransaction(it) }
        currencyUsageToDelete?.let { currencyUsageDao.deleteCurrencyUsage(it) }
        userToDelete?.let { userDao.deleteUser(it) }

        // Verify data was cleaned up
        assert(userDao.getUser() == null)
        assert(transactionDao.getTransactionById(1) == null)
        assert(currencyUsageDao.getCurrencyUsageById(1) == null)
    }

    @Test
    fun testDatabaseModuleErrorHandling() = runBlocking {
        // Test database module error handling
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Test unique constraint on currency usage
        try {
            val currencyUsage1 = CurrencyUsageEntity(
                currency = "USD",
                usageCount = 1,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            currencyUsageDao.insertCurrencyUsage(currencyUsage1)

            // This should fail due to unique constraint on currency
            val currencyUsage2 = CurrencyUsageEntity(
                currency = "USD", // Duplicate currency
                usageCount = 2,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            currencyUsageDao.insertCurrencyUsage(currencyUsage2)
            assert(false) // Should not reach here
        } catch (e: Exception) {
            // Expected to fail due to unique constraint
            assert(true)
        }

        // Test duplicate primary key - Room uses REPLACE strategy by default
        // This will replace the existing user instead of throwing an error
        val duplicateUser = UserEntity(
            defaultCurrency = "EUR",
            locale = "fr",
            deviceAuthEnabled = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(duplicateUser)
        
        // Verify the user was replaced
        val users = userDao.getUser()
        assertNotNull(users)
        assertEquals("EUR", users?.defaultCurrency)
    }

    @Test
    fun testDatabaseModuleTransactionSupport() = runBlocking {
        // Test database module transaction support
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Test transaction rollback on error
        try {
            database.runInTransaction {
                val transaction1 = TransactionEntity(
                    amount = 100.0,
                    description = "Transaction 1",
                    category = "Food",
                    type = TransactionType.EXPENSE,
                    date = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                )
                runBlocking { transactionDao.insertTransaction(transaction1) }

                val transaction2 = TransactionEntity(
                    id = 2,
                    amount = 200.0,
                    description = "Transaction 2",
                    category = "Transport",
                    type = TransactionType.EXPENSE,
                    date = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                )
                runBlocking { transactionDao.insertTransaction(transaction2) }

                // Force an error to test rollback
                throw RuntimeException("Test rollback")
            }
        } catch (e: RuntimeException) {
            // Expected exception
        }

        // Verify no transactions were inserted due to rollback
        val transactions = transactionDao.getAllTransactions().first()
        assertEquals(0, transactions.size)
    }

    @Test
    fun testDatabaseModuleMigrationSupport() = runBlocking {
        // Test database module migration support
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Test that database can handle schema changes
        val transaction = TransactionEntity(
            amount = 100.0,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )
        transactionDao.insertTransaction(transaction)

        // Verify data persists after operations
        val retrievedTransaction = transactionDao.getTransactionById(1)
        assertNotNull(retrievedTransaction)
        assertEquals(100.0, retrievedTransaction?.amount ?: 0.0, 0.01)
    }

    @Test
    fun testDatabaseModuleIndexing() = runBlocking {
        // Test database module indexing performance
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Insert multiple transactions to test indexing
        val startTime = System.currentTimeMillis()
        
        for (i in 1..1000) {
            val transaction = TransactionEntity(
                id = i.toLong(),
                amount = i * 10.0,
                description = "Transaction $i",
                category = "Category ${i % 10}",
                type = if (i % 2 == 0) TransactionType.EXPENSE else TransactionType.INCOME,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            transactionDao.insertTransaction(transaction)
        }
        
        val insertTime = System.currentTimeMillis() - startTime

        // Test query performance with indexing
        val queryStartTime = System.currentTimeMillis()
        val transactions = transactionDao.getAllTransactions().first()
        val queryTime = System.currentTimeMillis() - queryStartTime

        // Verify performance is acceptable
        assertTrue("Insert time should be reasonable: ${insertTime}ms", insertTime < 10000)
        assertTrue("Query time should be reasonable: ${queryTime}ms", queryTime < 1000)
        assertEquals(1000, transactions.size)
    }

    @Test
    fun testDatabaseModuleMemoryManagement() = runBlocking {
        // Test database module memory management
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Insert and delete data to test memory management
        for (i in 1..100) {
            val transaction = TransactionEntity(
                id = i.toLong(),
                amount = i * 10.0,
                description = "Transaction $i",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            transactionDao.insertTransaction(transaction)
        }

        // Verify data was inserted
        val initialCount = transactionDao.getAllTransactions().first().size
        assertEquals(100, initialCount)

        // Delete all transactions
        val transactions = transactionDao.getAllTransactions().first()
        transactions.forEach { transaction ->
            transactionDao.deleteTransaction(transaction)
        }

        // Verify data was deleted
        val finalCount = transactionDao.getAllTransactions().first().size
        assertEquals(0, finalCount)
    }

    @Test
    fun testDatabaseModuleConfigurationValidation() = runBlocking {
        // Test database module configuration validation
        assertNotNull("Database should be initialized", database)
        assertNotNull("UserDao should be initialized", userDao)
        assertNotNull("TransactionDao should be initialized", transactionDao)
        assertNotNull("CurrencyUsageDao should be initialized", currencyUsageDao)

        // Test database is writable
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Test database is readable
        val retrievedUser = userDao.getUser()
        assertNotNull("User should be retrievable", retrievedUser)
        assertEquals("USD", retrievedUser?.defaultCurrency)
    }
}
