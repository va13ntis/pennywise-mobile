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
import com.pennywise.app.data.repository.CurrencyUsageRepositoryImpl
import com.pennywise.app.data.repository.UserRepositoryImpl
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.usecase.CurrencySortingService
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Performance tests for currency sorting operations
 * Tests the performance of currency sorting with different scenarios:
 * - Basic sorting by popularity
 * - Sorting with usage data
 * - Large dataset sorting
 * - Cache operations performance
 * - Concurrent sorting operations
 * - Memory efficiency during sorting
 */
@RunWith(AndroidJUnit4::class)
class CurrencySortingPerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var database: PennyWiseDatabase
    private lateinit var currencyUsageDao: CurrencyUsageDao
    private lateinit var userDao: UserDao
    private lateinit var currencyUsageRepository: CurrencyUsageRepositoryImpl
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var currencySortingService: CurrencySortingService
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testUserId = 1L

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        
        currencyUsageDao = database.currencyUsageDao()
        userDao = database.userDao()
        
        // Create repository implementations
        currencyUsageRepository = CurrencyUsageRepositoryImpl(currencyUsageDao)
        userRepository = UserRepositoryImpl(userDao)
        
        // Create sorting service
        currencySortingService = CurrencySortingService(currencyUsageRepository, userRepository)
        
        // Setup test data
        setupTestData()
    }

    @After
    fun cleanup() {
        runBlocking {
            currencySortingService.invalidateAllCache()
        }
        database.close()
    }

    private fun setupTestData() {
        runBlocking {
            // Create a test user
            val testUser = UserEntity(
                id = testUserId,
                defaultCurrency = "USD",
                locale = "en_US",
                deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            userDao.insertUser(testUser)
        }
    }

    /**
     * Benchmark: Basic currency sorting by popularity
     * This tests the performance of sorting all currencies by their popularity ranking
     */
    @Test
    fun benchmarkBasicCurrencySorting() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val sortedCurrencies = Currency.getSortedByPopularity()
                assert(sortedCurrencies.isNotEmpty())
                assert(sortedCurrencies.first() == Currency.USD) // USD should be first
                assert(sortedCurrencies.last().popularity > sortedCurrencies.first().popularity)
            }
        }
    }

    /**
     * Benchmark: Currency sorting with usage data
     * This tests the performance of sorting currencies with user usage patterns
     */
    @Test
    fun benchmarkCurrencySortingWithUsage() {
        // Setup real usage data in database
        runBlocking {
            val usageEntities = listOf(
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "EUR",
                    usageCount = 10,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "GBP",
                    usageCount = 5,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "JPY",
                    usageCount = 3,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                )
            )
            
            usageEntities.forEach { entity ->
                currencyUsageDao.insertCurrencyUsage(entity)
            }
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(testUserId)
                assert(sortedCurrencies.isNotEmpty())
                // EUR should be first due to highest usage count
                assert(sortedCurrencies.first().code == "EUR")
            }
        }
    }

    /**
     * Benchmark: Large dataset currency sorting
     * This tests the performance of sorting with a large number of currency usage records
     */
    @Test
    fun benchmarkLargeDatasetCurrencySorting() {
        // Create a large dataset of currency usage in database
        runBlocking {
            val largeUsageEntities = Currency.values().mapIndexed { index, currency ->
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = currency.code,
                    usageCount = (index + 1) * 10,
                    lastUsed = Date(System.currentTimeMillis() - (index * 1000L)),
                    createdAt = Date(),
                    updatedAt = Date()
                )
            }
            
            largeUsageEntities.forEach { entity ->
                currencyUsageDao.insertCurrencyUsage(entity)
            }
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(testUserId)
                assert(sortedCurrencies.size == Currency.values().size)
                // Verify sorting is correct (highest usage first)
                assert(sortedCurrencies.first().code == "ZAR") // Last currency should have highest usage
            }
        }
    }

    /**
     * Benchmark: Cache operations performance
     * This tests the performance of cache read/write operations
     */
    @Test
    fun benchmarkCacheOperations() {
        // Setup real usage data in database
        runBlocking {
            val usageEntities = listOf(
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "USD",
                    usageCount = 15,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "EUR",
                    usageCount = 8,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                )
            )
            
            usageEntities.forEach { entity ->
                currencyUsageDao.insertCurrencyUsage(entity)
            }
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                // First call - cache miss
                val sortedCurrencies1 = currencySortingService.getSortedCurrenciesSuspend(testUserId)
                
                // Second call - cache hit
                val sortedCurrencies2 = currencySortingService.getSortedCurrenciesSuspend(testUserId)
                
                assert(sortedCurrencies1 == sortedCurrencies2)
                
                // Test cache invalidation
                currencySortingService.invalidateCache(testUserId)
                
                // Third call - cache miss again
                val sortedCurrencies3 = currencySortingService.getSortedCurrenciesSuspend(testUserId)
                assert(sortedCurrencies3.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Concurrent currency sorting operations
     * This tests the performance under concurrent load
     */
    @Test
    fun benchmarkConcurrentCurrencySorting() {
        // Setup real usage data in database
        runBlocking {
            val usageEntities = listOf(
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "USD",
                    usageCount = 20,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "EUR",
                    usageCount = 15,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "GBP",
                    usageCount = 10,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                )
            )
            
            usageEntities.forEach { entity ->
                currencyUsageDao.insertCurrencyUsage(entity)
            }
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val concurrentOperations = (1..5).map {
                    async {
                        currencySortingService.getSortedCurrenciesSuspend(testUserId)
                    }
                }
                
                val results = concurrentOperations.awaitAll()
                
                // Verify all results are the same
                results.forEach { result ->
                    assert(result.isNotEmpty())
                    assert(result.first().code == "USD") // USD should be first due to highest usage
                }
            }
        }
    }

    /**
     * Benchmark: Top currencies retrieval performance
     * This tests the performance of getting top N currencies
     */
    @Test
    fun benchmarkTopCurrenciesRetrieval() {
        // Setup real usage data in database
        runBlocking {
            val usageEntities = Currency.values().mapIndexed { index, currency ->
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = currency.code,
                    usageCount = (Currency.values().size - index) * 5,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                )
            }
            
            usageEntities.forEach { entity ->
                currencyUsageDao.insertCurrencyUsage(entity)
            }
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val topCurrencies = currencySortingService.getTopCurrencies(testUserId, 10).first()
                assert(topCurrencies.size == 10)
                assert(topCurrencies.first().code == "ZAR") // Should be the most used
            }
        }
    }

    /**
     * Benchmark: Used currencies filtering performance
     * This tests the performance of filtering only used currencies
     */
    @Test
    fun benchmarkUsedCurrenciesFiltering() {
        // Setup real usage data in database
        runBlocking {
            val usageEntities = listOf(
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "USD",
                    usageCount = 25,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "EUR",
                    usageCount = 18,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "GBP",
                    usageCount = 12,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                ),
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = "JPY",
                    usageCount = 8,
                    lastUsed = Date(),
                    createdAt = Date(),
                    updatedAt = Date()
                )
            )
            
            usageEntities.forEach { entity ->
                currencyUsageDao.insertCurrencyUsage(entity)
            }
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val usedCurrencies = currencySortingService.getUsedCurrencies(testUserId).first()
                assert(usedCurrencies.size == 4)
                assert(usedCurrencies.map { it.code }.containsAll(listOf("USD", "EUR", "GBP", "JPY")))
            }
        }
    }

    /**
     * Benchmark: Memory efficiency during sorting
     * This tests memory usage patterns during sorting operations
     */
    @Test
    fun benchmarkMemoryEfficiencyDuringSorting() {
        // Create large dataset in database
        runBlocking {
            val largeUsageEntities = (1..1000).map { index ->
                val currency = Currency.values()[index % Currency.values().size]
                CurrencyUsageEntity(
                    userId = testUserId,
                    currency = currency.code,
                    usageCount = index,
                    lastUsed = Date(System.currentTimeMillis() - (index * 1000L)),
                    createdAt = Date(),
                    updatedAt = Date()
                )
            }
            
            largeUsageEntities.forEach { entity ->
                currencyUsageDao.insertCurrencyUsage(entity)
            }
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                // Perform multiple sorting operations to test memory efficiency
                repeat(5) {
                    val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(testUserId)
                    assert(sortedCurrencies.isNotEmpty())
                }
                
                // Test cache statistics
                val cacheStats = currencySortingService.getCacheStats()
                assert(cacheStats.isNotEmpty())
                assert(cacheStats.containsKey("sortedCurrenciesCacheSize"))
            }
        }
    }
}
