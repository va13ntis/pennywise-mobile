package com.pennywise.app.performance

import android.Manifest
import android.os.Build
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
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import java.util.Date
import java.util.Calendar
import org.junit.Assert.assertTrue

/**
 * Performance tests for currency usage tracker operations
 * Tests the performance of currency usage tracking functionality including:
 * - Currency usage increment operations
 * - Currency usage queries and sorting
 * - Top currencies retrieval
 * - Batch currency usage operations
 * - Concurrent currency usage tracking
 * - Large dataset currency usage operations
 * 
 */
@RunWith(AndroidJUnit4::class)
class CurrencyUsageTrackerPerformanceTest {

    private lateinit var database: PennyWiseDatabase
    private lateinit var currencyUsageDao: CurrencyUsageDao
    private lateinit var userDao: UserDao
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        // Grant permissions for CI environment
        grantPermissions()
        
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
    
    /**
     * Grant runtime permissions for CI environment
     * This prevents "Failed to grant permissions" errors on emulator
     */
    private fun grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val uiAutomation = instrumentation.uiAutomation
            
            // Grant all permissions declared in the manifest
            val permissions = listOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            
            // Grant all permissions using UiAutomation
            permissions.forEach { permission ->
                try {
                    uiAutomation.grantRuntimePermission(
                        context.packageName,
                        permission
                    )
                } catch (e: Exception) {
                    // Permission may not be declared in manifest or already granted
                    // This is expected and safe to ignore
                }
            }
        }
    }

    @After
    fun cleanup() {
        database.close()
    }

    private fun setupTestData() {
        runBlocking {
            // Create single test user
            val testUser = UserEntity(
                defaultCurrency = "USD",
                locale = "en",
                deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            userDao.insertUser(testUser)
            
            // Create test currency usage data
            val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "SEK", "NOK")
            val now = Date()
            
            currencies.forEachIndexed { index, currency ->
                val currencyUsage = CurrencyUsageEntity(
                    currency = currency,
                    usageCount = (100 - index * 5),
                    lastUsed = Date(System.currentTimeMillis() - index * 86400000L), // Days ago
                    createdAt = now,
                    updatedAt = now
                )
                currencyUsageDao.insertCurrencyUsage(currencyUsage)
            }
        }
    }

    /**
     * Benchmark: Single currency usage increment
     * This tests the performance of incrementing currency usage for a single currency
     */
    @Test
    fun benchmarkSingleCurrencyUsageIncrement() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val now = Date()
                currencyUsageDao.insertOrIncrementCurrencyUsage("USD", now, now, now)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 50ms, got ${avgTime}ms", avgTime < 50)
    }

    /**
     * Benchmark: All currency usage query
     * This tests the performance of querying all currency usage
     */
    @Test
    fun benchmarkAllCurrencyUsageQuery() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val usage = currencyUsageDao.getAllCurrencyUsage().first()
                assertTrue("Usage should not be empty", usage.isNotEmpty())
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 50ms, got ${avgTime}ms", avgTime < 50)
    }

    /**
     * Benchmark: Top currencies query
     * This tests the performance of querying top currencies by usage count
     */
    @Test
    fun benchmarkTopCurrenciesQuery() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val topCurrencies = currencyUsageDao.getTopCurrencies(5).first()
                assertTrue("Top currencies size should be <= 5", topCurrencies.size <= 5)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 50ms, got ${avgTime}ms", avgTime < 50)
    }

    /**
     * Benchmark: Currency usage sorted by usage
     * This tests the performance of querying currencies sorted by usage count
     */
    @Test
    fun benchmarkCurrencyUsageSortedByUsage() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val sortedUsage = currencyUsageDao.getCurrencyUsageSortedByUsage().first()
                assertTrue("Sorted usage should not be empty", sortedUsage.isNotEmpty())
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 50ms, got ${avgTime}ms", avgTime < 50)
    }

    /**
     * Benchmark: Currency usage query by currency
     * This tests the performance of querying specific currency usage
     */
    @Test
    fun benchmarkCurrencyUsageQueryByCurrency() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val usage = currencyUsageDao.getCurrencyUsageByCurrency("USD")
                assertTrue("USD usage should not be null", usage != null)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 50ms, got ${avgTime}ms", avgTime < 50)
    }

    /**
     * Benchmark: Currency usage count query
     * This tests the performance of counting currency usage entries
     */
    @Test
    fun benchmarkCurrencyUsageCountQuery() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val count = currencyUsageDao.getCurrencyUsageCount()
                assertTrue("Count should be > 0", count > 0)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 50ms, got ${avgTime}ms", avgTime < 50)
    }

    /**
     * Benchmark: Batch currency usage increment
     * This tests the performance of incrementing multiple currencies
     */
    @Test
    fun benchmarkBatchCurrencyUsageIncrement() {
        val iterations = 50
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD")
                val now = Date()
                
                currencies.forEach { currency ->
                    currencyUsageDao.insertOrIncrementCurrencyUsage(currency, now, now, now)
                }
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 200ms, got ${avgTime}ms", avgTime < 200)
    }

    /**
     * Benchmark: Concurrent currency usage operations
     * This tests the performance under concurrent currency usage tracking
     */
    @Test
    fun benchmarkConcurrentCurrencyUsageOperations() {
        val iterations = 50
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val operations = listOf(
                    async { currencyUsageDao.getAllCurrencyUsage().first() },
                    async { currencyUsageDao.getTopCurrencies(5).first() },
                    async { currencyUsageDao.getCurrencyUsageByCurrency("USD") },
                    async { currencyUsageDao.getCurrencyUsageCount() },
                    async { currencyUsageDao.getCurrencyUsageSortedByUsage().first() }
                )
                
                val results = operations.map { it.await() }
                assertTrue("Results size should be 5", results.size == 5)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 200ms, got ${avgTime}ms", avgTime < 200)
    }

    /**
     * Benchmark: Large dataset currency usage operations
     * This tests the performance with a larger set of currencies
     */
    @Test
    fun benchmarkLargeDatasetCurrencyUsageOperations() {
        // Create additional currency usage data
        runBlocking {
            val now = Date()
            repeat(100) { index ->
                val currencyUsage = CurrencyUsageEntity(
                    currency = "CUR$index",
                    usageCount = index,
                    lastUsed = now,
                    createdAt = now,
                    updatedAt = now
                )
                currencyUsageDao.insertCurrencyUsage(currencyUsage)
            }
        }

        val iterations = 50
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val allUsage = currencyUsageDao.getAllCurrencyUsage().first()
                assertTrue("All usage size should be >= 100", allUsage.size >= 100)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 200ms, got ${avgTime}ms", avgTime < 200)
    }

    /**
     * Benchmark: Currency usage deletion
     * This tests the performance of deleting all currency usage
     */
    @Test
    fun benchmarkCurrencyUsageDeletion() {
        val iterations = 20
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                // Setup some test data first
                val now = Date()
                repeat(10) { index ->
                    currencyUsageDao.insertOrIncrementCurrencyUsage("TEST$index", now, now, now)
                }
                
                // Delete all currency usage
                currencyUsageDao.deleteAllCurrencyUsage()
                
                // Verify deletion
                val count = currencyUsageDao.getCurrencyUsageCount()
                assertTrue("Count should be 0", count == 0)
                
                // Restore test data for next iteration
                setupTestData()
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 500ms, got ${avgTime}ms", avgTime < 500)
    }

    /**
     * Benchmark: Sequential currency usage updates
     * This tests the performance of sequential increment operations
     */
    @Test
    fun benchmarkSequentialCurrencyUsageUpdates() {
        val iterations = 50
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val now = Date()
                repeat(10) { _ ->
                    currencyUsageDao.insertOrIncrementCurrencyUsage("USD", now, now, now)
                }
                
                val usage = currencyUsageDao.getCurrencyUsageByCurrency("USD")
                assertTrue("USD usage should not be null", usage != null)
                assertTrue("Usage count should be > 0", usage!!.usageCount > 0)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 200ms, got ${avgTime}ms", avgTime < 200)
    }

    /**
     * Benchmark: Currency usage with time-based queries
     * This tests the performance of querying currencies by last used time
     */
    @Test
    fun benchmarkTimeBasedCurrencyQueries() {
        val iterations = 50
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                // Get all currencies sorted by usage (which considers last used)
                val sortedUsage = currencyUsageDao.getCurrencyUsageSortedByUsage().first()
                assertTrue("Sorted usage should not be empty", sortedUsage.isNotEmpty())
                
                // Verify they're properly sorted by usage count
                for (i in 0 until sortedUsage.size - 1) {
                    assertTrue("Usage should be sorted", sortedUsage[i].usageCount >= sortedUsage[i + 1].usageCount)
                }
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 200ms, got ${avgTime}ms", avgTime < 200)
    }

    /**
     * Benchmark: Mixed currency operations
     * This tests the performance of mixed read/write operations
     */
    @Test
    fun benchmarkMixedCurrencyOperations() {
        val iterations = 50
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val now = Date()
                
                // Insert/increment
                currencyUsageDao.insertOrIncrementCurrencyUsage("USD", now, now, now)
                
                // Read specific
                val usdUsage = currencyUsageDao.getCurrencyUsageByCurrency("USD")
                assertTrue("USD usage should not be null", usdUsage != null)
                
                // Read top
                val topCurrencies = currencyUsageDao.getTopCurrencies(3).first()
                assertTrue("Top currencies should not be empty", topCurrencies.isNotEmpty())
                
                // Read count
                val count = currencyUsageDao.getCurrencyUsageCount()
                assertTrue("Count should be > 0", count > 0)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        assertTrue("Average time should be < 200ms, got ${avgTime}ms", avgTime < 200)
    }
}
