package com.pennywise.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Integration tests for transaction operations that involve currencies
 * Tests the relationship between transactions and currency usage tracking
 */
@RunWith(AndroidJUnit4::class)
class TransactionCurrencyIntegrationTest {
    private lateinit var database: PennyWiseDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var currencyUsageDao: CurrencyUsageDao
    private lateinit var userDao: UserDao
    private lateinit var testUser: UserEntity

    @Before
    fun createDb() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = database.transactionDao()
        currencyUsageDao = database.currencyUsageDao()
        userDao = database.userDao()
        
        // Create a test user
        testUser = UserEntity(defaultCurrency = "USD", locale = "en", deviceAuthEnabled = false)
        userDao.insertUser(testUser)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun createTransactionWithCurrency_shouldTrackCurrencyUsage() = runTest {
        // Given
        val currency = "EUR"
        val transaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            currency = currency,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )

        // When
        transactionDao.insertTransaction(transaction)
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, currency, now, now, now)

        // Then
        val currencyUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, currency)
        assertNotNull(currencyUsage)
        assertEquals(1, currencyUsage!!.usageCount)
        assertEquals(currency, currencyUsage.currency)
    }

    @Test
    fun multipleTransactionsWithSameCurrency_shouldIncrementUsageCount() = runTest {
        // Given
        val currency = "USD"
        val transaction1 = TransactionEntity(
            userId = testUser.id,
            amount = 50.0,
            currency = currency,
            description = "Transaction 1",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val transaction2 = TransactionEntity(
            userId = testUser.id,
            amount = 75.0,
            currency = currency,
            description = "Transaction 2",
            category = "Transport",
            type = TransactionType.EXPENSE,
            date = Date()
        )

        // When
        transactionDao.insertTransaction(transaction1)
        transactionDao.insertTransaction(transaction2)
        
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, currency, now, now, now)
        currencyUsageDao.incrementCurrencyUsage(testUser.id, currency, now, now)

        // Then
        val currencyUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, currency)
        assertNotNull(currencyUsage)
        assertEquals(2, currencyUsage!!.usageCount)
    }

    @Test
    fun transactionsWithDifferentCurrencies_shouldTrackSeparately() = runTest {
        // Given
        val eurTransaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            currency = "EUR",
            description = "EUR transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val usdTransaction = TransactionEntity(
            userId = testUser.id,
            amount = 120.0,
            currency = "USD",
            description = "USD transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )

        // When
        transactionDao.insertTransaction(eurTransaction)
        transactionDao.insertTransaction(usdTransaction)
        
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, "EUR", now, now, now)
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, "USD", now, now, now)

        // Then
        val eurUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")
        val usdUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "USD")
        
        assertNotNull(eurUsage)
        assertNotNull(usdUsage)
        assertEquals(1, eurUsage!!.usageCount)
        assertEquals(1, usdUsage!!.usageCount)
        assertEquals("EUR", eurUsage.currency)
        assertEquals("USD", usdUsage.currency)
    }

    @Test
    fun getTransactionsByCurrency_shouldReturnCorrectTransactions() = runTest {
        // Given
        val eurTransaction1 = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            currency = "EUR",
            description = "EUR transaction 1",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val eurTransaction2 = TransactionEntity(
            userId = testUser.id,
            amount = 50.0,
            currency = "EUR",
            description = "EUR transaction 2",
            category = "Transport",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        val usdTransaction = TransactionEntity(
            userId = testUser.id,
            amount = 120.0,
            currency = "USD",
            description = "USD transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )

        // When
        transactionDao.insertTransaction(eurTransaction1)
        transactionDao.insertTransaction(eurTransaction2)
        transactionDao.insertTransaction(usdTransaction)

        // Then - Get all transactions and filter by currency
        val allTransactions = transactionDao.getTransactionsByUser(testUser.id).first()
        val eurTransactions = allTransactions.filter { it.currency == "EUR" }
        val usdTransactions = allTransactions.filter { it.currency == "USD" }
        
        assertEquals(2, eurTransactions.size)
        assertEquals(1, usdTransactions.size)
        assertTrue(eurTransactions.all { it.currency == "EUR" })
        assertTrue(usdTransactions.all { it.currency == "USD" })
    }

    @Test
    fun currencyUsageTracking_shouldReflectTransactionPatterns() = runTest {
        // Given - Create transactions with different currency usage patterns
        val currencies = listOf("EUR", "USD", "GBP", "EUR", "USD", "EUR") // EUR used 3 times, USD 2 times, GBP 1 time
        
        currencies.forEachIndexed { index, currency ->
            val transaction = TransactionEntity(
                userId = testUser.id,
                amount = (index + 1) * 10.0,
                currency = currency,
                description = "Transaction $index",
                category = "Test",
                type = TransactionType.EXPENSE,
                date = Date()
            )
            transactionDao.insertTransaction(transaction)
            
            // Track currency usage
            val now = Date()
            currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, currency, now, now, now)
        }

        // When
        val currencyUsages = currencyUsageDao.getCurrencyUsageByUser(testUser.id).first()

        // Then
        assertEquals(3, currencyUsages.size) // Should have 3 unique currencies
        
        val eurUsage = currencyUsages.find { it.currency == "EUR" }
        val usdUsage = currencyUsages.find { it.currency == "USD" }
        val gbpUsage = currencyUsages.find { it.currency == "GBP" }
        
        assertNotNull(eurUsage)
        assertNotNull(usdUsage)
        assertNotNull(gbpUsage)
        
        assertEquals(3, eurUsage!!.usageCount) // EUR used 3 times
        assertEquals(2, usdUsage!!.usageCount) // USD used 2 times
        assertEquals(1, gbpUsage!!.usageCount) // GBP used 1 time
    }

    @Test
    fun topCurrencies_shouldMatchTransactionFrequency() = runTest {
        // Given - Create transactions with specific currency usage patterns
        val currencyPatterns = mapOf(
            "EUR" to 5, // Most used
            "USD" to 3, // Second most used
            "GBP" to 1  // Least used
        )
        
        currencyPatterns.forEach { (currency, count) ->
            repeat(count) { index ->
                val transaction = TransactionEntity(
                    userId = testUser.id,
                    amount = (index + 1) * 10.0,
                    currency = currency,
                    description = "$currency transaction $index",
                    category = "Test",
                    type = TransactionType.EXPENSE,
                    date = Date()
                )
                transactionDao.insertTransaction(transaction)
                
                // Track currency usage
                val now = Date()
                currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, currency, now, now, now)
            }
        }

        // When
        val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(testUser.id, 3).first()

        // Then
        assertEquals(3, topCurrencies.size)
        assertEquals("EUR", topCurrencies[0].currency) // Most used
        assertEquals(5, topCurrencies[0].usageCount)
        assertEquals("USD", topCurrencies[1].currency) // Second most used
        assertEquals(3, topCurrencies[1].usageCount)
        assertEquals("GBP", topCurrencies[2].currency) // Least used
        assertEquals(1, topCurrencies[2].usageCount)
    }

    @Test
    fun deleteTransaction_shouldNotAffectCurrencyUsage() = runTest {
        // Given
        val currency = "EUR"
        val transaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            currency = currency,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        
        val transactionId = transactionDao.insertTransaction(transaction)
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, currency, now, now, now)

        // When
        val transactionToDelete = transactionDao.getTransactionById(transactionId)!!
        transactionDao.deleteTransaction(transactionToDelete)

        // Then
        val currencyUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, currency)
        assertNotNull(currencyUsage) // Currency usage should still exist
        assertEquals(1, currencyUsage!!.usageCount)
        
        val deletedTransaction = transactionDao.getTransactionById(transactionId)
        assertNull(deletedTransaction) // Transaction should be deleted
    }

    @Test
    fun userDeletion_shouldCascadeToTransactionsAndCurrencyUsage() = runTest {
        // Given
        val currency = "EUR"
        val transaction = TransactionEntity(
            userId = testUser.id,
            amount = 100.0,
            currency = currency,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        
        transactionDao.insertTransaction(transaction)
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, currency, now, now, now)

        // When
        userDao.deleteUser(testUser)

        // Then
        val userTransactions = transactionDao.getTransactionsByUser(testUser.id).first()
        val userCurrencyUsages = currencyUsageDao.getCurrencyUsageByUser(testUser.id).first()
        
        assertTrue(userTransactions.isEmpty()) // Transactions should be deleted
        assertTrue(userCurrencyUsages.isEmpty()) // Currency usage should be deleted
    }

    @Test
    fun transactionAmounts_shouldBeTrackedByCurrency() = runTest {
        // Given
        val eurTransactions = listOf(
            TransactionEntity(
                userId = testUser.id,
                amount = 100.0,
                currency = "EUR",
                description = "EUR transaction 1",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = Date()
            ),
            TransactionEntity(
                userId = testUser.id,
                amount = 50.0,
                currency = "EUR",
                description = "EUR transaction 2",
                category = "Transport",
                type = TransactionType.EXPENSE,
                date = Date()
            )
        )
        
        val usdTransaction = TransactionEntity(
            userId = testUser.id,
            amount = 120.0,
            currency = "USD",
            description = "USD transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )

        // When
        eurTransactions.forEach { transactionDao.insertTransaction(it) }
        transactionDao.insertTransaction(usdTransaction)
        
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, "EUR", now, now, now)
        currencyUsageDao.incrementCurrencyUsage(testUser.id, "EUR", now, now)
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, "USD", now, now, now)

        // Then
        val allTransactions = transactionDao.getTransactionsByUser(testUser.id).first()
        val eurTransactionsFromDb = allTransactions.filter { it.currency == "EUR" }
        val usdTransactionsFromDb = allTransactions.filter { it.currency == "USD" }
        
        val eurTotal = eurTransactionsFromDb.sumOf { it.amount }
        val usdTotal = usdTransactionsFromDb.sumOf { it.amount }
        
        assertEquals(150.0, eurTotal, 0.01) // 100 + 50
        assertEquals(120.0, usdTotal, 0.01) // 120
        
        val eurUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")
        val usdUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "USD")
        
        assertEquals(2, eurUsage!!.usageCount) // EUR used twice
        assertEquals(1, usdUsage!!.usageCount) // USD used once
    }
}
