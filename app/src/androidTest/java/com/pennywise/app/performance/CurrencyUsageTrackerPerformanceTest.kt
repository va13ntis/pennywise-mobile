package com.pennywise.app.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.UserEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import java.util.Date
import java.util.Calendar

/**
 * Performance tests for currency usage tracker operations
 * Tests the performance of currency usage tracking functionality including:
 * - Currency usage increment operations
 * - Currency usage queries and sorting
 * - Top currencies retrieval
 * - Batch currency usage operations
 * - Concurrent currency usage tracking
 * - Large dataset currency usage operations
 */
@RunWith(AndroidJUnit4::class)
class CurrencyUsageTrackerPerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var database: PennyWiseDatabase
    private lateinit var currencyUsageDao: CurrencyUsageDao
    private lateinit var userDao: UserDao
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            PennyWiseDatabase::class.java
        ).build()
        
        currencyUsageDao = database.currencyUsageDao()
        userDao = database.userDao()
        
        // Setup test data
        setupTestData()
    }

    @After
    fun cleanup() {
        database.close()
    }

    private fun setupTestData() {
        runBlocking {
            // Create test users
            val testUsers = listOf(
                UserEntity(
                    defaultCurrency = "USD",
                    locale = "en",
                    deviceAuthEnabled = false,
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                UserEntity(
                    defaultCurrency = "EUR",
                    locale = "en",
                    deviceAuthEnabled = false,
                    createdAt = Date(),
                    updatedAt = Date()
                )
            )
            
            testUsers.forEach { user ->
                userDao.insertUser(user)
            }
            
            // Create test currency usage data for multiple users
            val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "SEK", "NOK")
            val userIds = listOf(1L, 2L)
            
            userIds.forEach { userId ->
                currencies.forEachIndexed { index, currency ->
                    val currencyUsage = CurrencyUsageEntity(
                        userId = userId,
                        currency = currency,
                        usageCount = (100 - index * 5),
                        lastUsed = Date(System.currentTimeMillis() - index * 86400000L), // Days ago
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    currencyUsageDao.insertCurrencyUsage(currencyUsage)
                }
            }
        }
    }

    /**
     * Benchmark: Single currency usage increment
     * This tests the performance of incrementing currency usage for a single currency
     */
    @Test
    fun benchmarkSingleCurrencyUsageIncrement() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val now = Date()
                currencyUsageDao.incrementCurrencyUsage(1L, "USD", now, now)
            }
        }
    }

    /**
     * Benchmark: Insert or increment currency usage
     * This tests the performance of the insert or increment operation
     */
    @Test
    fun benchmarkInsertOrIncrementCurrencyUsage() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val now = Date()
                currencyUsageDao.insertOrIncrementCurrencyUsage(1L, "USD", now, now, now)
            }
        }
    }

    /**
     * Benchmark: Currency usage query by user
     * This tests the performance of querying currency usage by user
     */
    @Test
    fun benchmarkCurrencyUsageQueryByUser() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val usage = currencyUsageDao.getCurrencyUsageByUser(1L).first()
                assert(usage.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Top currencies query
     * This tests the performance of querying top currencies by usage count
     */
    @Test
    fun benchmarkTopCurrenciesQuery() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(1L, 5).first()
                assert(topCurrencies.size <= 5)
            }
        }
    }

    /**
     * Benchmark: Currency usage sorted by usage
     * This tests the performance of querying currencies sorted by usage count
     */
    @Test
    fun benchmarkCurrencyUsageSortedByUsage() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val sortedUsage = currencyUsageDao.getUserCurrenciesSortedByUsage(1L).first()
                assert(sortedUsage.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Currency usage query by user and currency
     * This tests the performance of querying specific currency usage
     */
    @Test
    fun benchmarkCurrencyUsageQueryByUserAndCurrency() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val usage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(1L, "USD")
                assert(usage != null)
            }
        }
    }

    /**
     * Benchmark: Currency usage count query
     * This tests the performance of counting currency usage entries
     */
    @Test
    fun benchmarkCurrencyUsageCountQuery() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val count = currencyUsageDao.getCurrencyUsageCountForUser(1L)
                assert(count > 0)
            }
        }
    }

    /**
     * Benchmark: Batch currency usage increment
     * This tests the performance of incrementing multiple currencies
     */
    @Test
    fun benchmarkBatchCurrencyUsageIncrement() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD")
                val now = Date()
                
                currencies.forEach { currency ->
                    currencyUsageDao.incrementCurrencyUsage(1L, currency, now, now)
                }
            }
        }
    }

    /**
     * Benchmark: Multiple users currency usage operations
     * This tests the performance of currency usage operations across multiple users
     */
    @Test
    fun benchmarkMultipleUsersCurrencyUsageOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val userIds = listOf(1L, 2L)
                val currencies = listOf("USD", "EUR", "GBP")
                val now = Date()
                
                userIds.forEach { userId ->
                    currencies.forEach { currency ->
                        currencyUsageDao.incrementCurrencyUsage(userId, currency, now, now)
                    }
                }
            }
        }
    }

    /**
     * Benchmark: Concurrent currency usage operations
     * This tests the performance under concurrent currency usage tracking
     */
    @Test
    fun benchmarkConcurrentCurrencyUsageOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val operations = listOf(
                    async { currencyUsageDao.getCurrencyUsageByUser(1L).first() },
                    async { currencyUsageDao.getTopCurrenciesByUser(1L, 3).first() },
                    async { currencyUsageDao.getCurrencyUsageByUser(2L).first() },
                    async { currencyUsageDao.getTopCurrenciesByUser(2L, 3).first() },
                    async { 
                        val now = Date()
                        currencyUsageDao.incrementCurrencyUsage(1L, "USD", now, now)
                    }
                )
                
                val results = operations.map { it.await() }
                assert(results.size == 5)
            }
        }
    }

    /**
     * Benchmark: Large dataset currency usage operations
     * This tests the performance with a larger dataset of currency usage
     */
    @Test
    fun benchmarkLargeDatasetCurrencyUsageOperations() {
        // Create additional test data
        runBlocking {
            val additionalCurrencies = listOf("BRL", "MXN", "INR", "KRW", "SGD", "HKD", "NZD", "ZAR", "TRY", "RUB")
            val userIds = listOf(1L, 2L)
            
            userIds.forEach { userId ->
                additionalCurrencies.forEachIndexed { index, currency ->
                    val currencyUsage = CurrencyUsageEntity(
                        userId = userId,
                        currency = currency,
                        usageCount = (50 - index * 2),
                        lastUsed = Date(System.currentTimeMillis() - index * 3600000L), // Hours ago
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    currencyUsageDao.insertCurrencyUsage(currencyUsage)
                }
            }
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val allUsage = currencyUsageDao.getCurrencyUsageByUser(1L).first()
                val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(1L, 10).first()
                val count = currencyUsageDao.getCurrencyUsageCountForUser(1L)
                
                assert(allUsage.size >= 20) // Original 10 + additional 10
                assert(topCurrencies.size <= 10)
                assert(count >= 20)
            }
        }
    }

    /**
     * Benchmark: Currency usage update operations
     * This tests the performance of updating currency usage entries
     */
    @Test
    fun benchmarkCurrencyUsageUpdateOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val existingUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(1L, "USD")
                if (existingUsage != null) {
                    val updatedUsage = existingUsage.copy(
                        usageCount = existingUsage.usageCount + 1,
                        lastUsed = Date(),
                        updatedAt = Date()
                    )
                    currencyUsageDao.updateCurrencyUsage(updatedUsage)
                }
            }
        }
    }

    /**
     * Benchmark: Currency usage deletion operations
     * This tests the performance of deleting currency usage entries
     */
    @Test
    fun benchmarkCurrencyUsageDeletionOperations() {
        // Create a temporary currency usage entry for deletion testing
        runBlocking {
            val tempUsage = CurrencyUsageEntity(
                userId = 1L,
                currency = "TEMP",
                usageCount = 1,
                lastUsed = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
            currencyUsageDao.insertCurrencyUsage(tempUsage)
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val tempUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(1L, "TEMP")
                if (tempUsage != null) {
                    currencyUsageDao.deleteCurrencyUsage(tempUsage)
                }
            }
        }
    }

    /**
     * Benchmark: Currency usage operations with date filtering
     * This tests the performance of currency usage operations with date-based filtering
     */
    @Test
    fun benchmarkCurrencyUsageOperationsWithDateFiltering() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val calendar = Calendar.getInstance()
                val now = Date()
                calendar.time = now
                calendar.add(Calendar.DAY_OF_MONTH, -7)
                val weekAgo = calendar.time
                
                // Get all currency usage and filter by date in memory
                val allUsage = currencyUsageDao.getCurrencyUsageByUser(1L).first()
                val recentUsage = allUsage.filter { it.lastUsed.after(weekAgo) }
                
                assert(recentUsage.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Currency usage statistics calculation
     * This tests the performance of calculating currency usage statistics
     */
    @Test
    fun benchmarkCurrencyUsageStatisticsCalculation() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val allUsage = currencyUsageDao.getCurrencyUsageByUser(1L).first()
                
                // Calculate statistics
                val totalUsage = allUsage.sumOf { it.usageCount }
                val averageUsage = if (allUsage.isNotEmpty()) totalUsage / allUsage.size else 0
                val maxUsage = allUsage.maxOfOrNull { it.usageCount } ?: 0
                val minUsage = allUsage.minOfOrNull { it.usageCount } ?: 0
                val uniqueCurrencies = allUsage.map { it.currency }.distinct().size
                
                assert(totalUsage > 0)
                assert(averageUsage > 0)
                assert(maxUsage > 0)
                assert(minUsage >= 0)
                assert(uniqueCurrencies > 0)
            }
        }
    }

    /**
     * Benchmark: Currency usage cleanup operations
     * This tests the performance of cleaning up currency usage data
     */
    @Test
    fun benchmarkCurrencyUsageCleanupOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Test cleanup for a specific user
                val countBefore = currencyUsageDao.getCurrencyUsageCountForUser(2L)
                currencyUsageDao.deleteAllCurrencyUsageForUser(2L)
                val countAfter = currencyUsageDao.getCurrencyUsageCountForUser(2L)
                
                assert(countBefore > 0)
                assert(countAfter == 0)
                
                // Restore data for next iteration
                setupTestData()
            }
        }
    }
}
