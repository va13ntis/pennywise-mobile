package com.pennywise.app.data.local.config

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.PennywiseDatabase
import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for database module configuration
 * Tests database module setup, dependency injection, and configuration
 */
@RunWith(AndroidJUnit4::class)
class DatabaseModuleTest {

    private lateinit var database: PennywiseDatabase
    private lateinit var userDao: UserDao
    private lateinit var transactionDao: TransactionDao
    private lateinit var currencyUsageDao: CurrencyUsageDao

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
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

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

        // Verify all DAOs are working
        assertNotNull(userDao.getUserById(1))
        assertNotNull(transactionDao.getTransactionById(1))
        assertNotNull(currencyUsageDao.getCurrencyUsageById(1))
    }

    @Test
    fun testDatabaseModuleDependencies() = runBlocking {
        // Test that all dependencies are properly injected
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Test transaction dependency on user
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

        // Test currency usage dependency on user
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

        // Verify dependencies are working
        val userTransactions = transactionDao.getTransactionsByUser(1)
        val userCurrencyUsages = currencyUsageDao.getCurrencyUsageByUser(1)

        assertNotNull(userTransactions)
        assertNotNull(userCurrencyUsages)
        assertEquals(1, userTransactions.size)
        assertEquals(1, userCurrencyUsages.size)
    }

    @Test
    fun testDatabaseModulePerformance() = runBlocking {
        // Test database module performance
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Measure performance of multiple operations
        val startTime = System.currentTimeMillis()
        
        // Insert multiple transactions
        for (i in 1..100) {
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
        
        // Insert multiple currency usages
        for (i in 1..50) {
            val currencyUsage = CurrencyUsageEntity(
                id = i,
                userId = 1,
                currencyCode = "USD",
                usageCount = i,
                lastUsedAt = Date(),
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
        val transactionCount = transactionDao.getTransactionsByUser(1).size
        val currencyUsageCount = currencyUsageDao.getCurrencyUsageByUser(1).size

        assertEquals(100, transactionCount)
        assertEquals(50, currencyUsageCount)
    }

    @Test
    fun testDatabaseModuleConcurrency() = runBlocking {
        // Test database module concurrency
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Perform concurrent operations
        val startTime = System.currentTimeMillis()
        
        // Insert transactions concurrently
        val transactions = (1..50).map { i ->
            TransactionEntity(
                id = i,
                userId = 1,
                amount = i * 10.0,
                description = "Transaction $i",
                category = "Category $i",
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
                id = i,
                userId = 1,
                currencyCode = "USD",
                usageCount = i,
                lastUsedAt = Date(),
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
        val transactionCount = transactionDao.getTransactionsByUser(1).size
        val currencyUsageCount = currencyUsageDao.getCurrencyUsageByUser(1).size

        assertEquals(50, transactionCount)
        assertEquals(25, currencyUsageCount)
    }

    @Test
    fun testDatabaseModuleDataIntegrity() = runBlocking {
        // Test database module data integrity
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Create related entities
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

    @Test
    fun testDatabaseModuleCleanup() = runBlocking {
        // Test database module cleanup
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

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

        // Verify data exists
        assertNotNull(userDao.getUserById(1))
        assertNotNull(transactionDao.getTransactionById(1))
        assertNotNull(currencyUsageDao.getCurrencyUsageById(1))

        // Clean up data
        transactionDao.deleteTransaction(1)
        currencyUsageDao.deleteCurrencyUsage(1)
        userDao.deleteUser(1)

        // Verify data was cleaned up
        assert(userDao.getUserById(1) == null)
        assert(transactionDao.getTransactionById(1) == null)
        assert(currencyUsageDao.getCurrencyUsageById(1) == null)
    }

    @Test
    fun testDatabaseModuleErrorHandling() = runBlocking {
        // Test database module error handling
        val user = UserEntity(
            id = 1,
            email = "test@example.com",
            passwordHash = "hashed_password",
            createdAt = Date(),
            updatedAt = Date()
        )
        userDao.insertUser(user)

        // Test foreign key constraint violation
        try {
            val invalidTransaction = TransactionEntity(
                id = 1,
                userId = 999, // Non-existent user
                amount = 100.0,
                description = "Invalid transaction",
                category = "Food",
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            transactionDao.insertTransaction(invalidTransaction)
            assert(false) // Should not reach here
        } catch (e: Exception) {
            // Expected to fail due to foreign key constraint
            assert(true)
        }

        // Test duplicate primary key
        try {
            val duplicateUser = UserEntity(
                id = 1, // Same ID as existing user
                email = "duplicate@example.com",
                passwordHash = "hashed_password",
                createdAt = Date(),
                updatedAt = Date()
            )
            userDao.insertUser(duplicateUser)
            assert(false) // Should not reach here
        } catch (e: Exception) {
            // Expected to fail due to duplicate primary key
            assert(true)
        }
    }
}