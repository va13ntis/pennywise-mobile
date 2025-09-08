package com.pennywise.app.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.domain.usecase.CurrencySortingService
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.repository.UserRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance tests for currency sorting operations
 * Tests the performance of currency sorting with different scenarios:
 * - Small dataset sorting
 * - Large dataset sorting
 * - Cache hit scenarios
 * - Cache miss scenarios
 * - Concurrent sorting operations
 */
@RunWith(AndroidJUnit4::class)
class CurrencySortingPerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Mock
    private lateinit var currencyUsageRepository: CurrencyUsageRepository

    @Mock
    private lateinit var userRepository: UserRepository

    private lateinit var currencySortingService: CurrencySortingService
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        currencySortingService = CurrencySortingService(currencyUsageRepository, userRepository)
        
        // Setup mock data
        setupMockData()
    }

    @After
    fun cleanup() {
        runBlocking {
            currencySortingService.invalidateAllCache()
        }
    }

    private fun setupMockData() {
        // Create mock currency usage data
        val mockCurrencyUsage = listOf(
            CurrencyUsage(
                id = 1,
                userId = 1L,
                currency = "USD",
                usageCount = 100,
                lastUsed = Date()
            ),
            CurrencyUsage(
                id = 2,
                userId = 1L,
                currency = "EUR",
                usageCount = 75,
                lastUsed = Date()
            ),
            CurrencyUsage(
                id = 3,
                userId = 1L,
                currency = "GBP",
                usageCount = 50,
                lastUsed = Date()
            ),
            CurrencyUsage(
                id = 4,
                userId = 1L,
                currency = "JPY",
                usageCount = 25,
                lastUsed = Date()
            ),
            CurrencyUsage(
                id = 5,
                userId = 1L,
                currency = "CAD",
                usageCount = 10,
                lastUsed = Date()
            )
        )

        // Mock repository responses
        whenever(currencyUsageRepository.getUserCurrenciesSortedByUsage(1L))
            .thenReturn(flowOf(mockCurrencyUsage))
    }

    /**
     * Benchmark: Currency sorting with small dataset
     * This tests the performance of sorting a small number of currencies
     */
    @Test
    fun benchmarkSmallDatasetSorting() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(1L)
                assert(sortedCurrencies.isNotEmpty())
                assert(sortedCurrencies.size == Currency.values().size)
            }
        }
    }

    /**
     * Benchmark: Currency sorting with cache hit
     * This tests the performance when data is already cached
     */
    @Test
    fun benchmarkCachedSorting() {
        // Pre-populate cache
        runBlocking {
            currencySortingService.getSortedCurrenciesSuspend(1L)
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(1L)
                assert(sortedCurrencies.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Top currencies retrieval
     * This tests the performance of getting top N currencies
     */
    @Test
    fun benchmarkTopCurrenciesRetrieval() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val topCurrencies = currencySortingService.getTopCurrencies(1L, 5).first()
                assert(topCurrencies.size <= 5)
            }
        }
    }

    /**
     * Benchmark: Used currencies retrieval
     * This tests the performance of getting only used currencies
     */
    @Test
    fun benchmarkUsedCurrenciesRetrieval() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val usedCurrencies = currencySortingService.getUsedCurrencies(1L).first()
                assert(usedCurrencies.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Currency usage tracking
     * This tests the performance of tracking currency usage
     */
    @Test
    fun benchmarkCurrencyUsageTracking() {
        benchmarkRule.measureRepeated {
            runBlocking {
                currencySortingService.trackCurrencyUsage(1L, "USD")
                // Verify cache was invalidated (this would be tested in integration tests)
            }
        }
    }

    /**
     * Benchmark: Cache statistics retrieval
     * This tests the performance of getting cache statistics
     */
    @Test
    fun benchmarkCacheStatisticsRetrieval() {
        benchmarkRule.measureRepeated {
            val stats = currencySortingService.getCacheStats()
            assert(stats.containsKey("sortedCurrenciesCacheSize"))
            assert(stats.containsKey("currencyUsageCacheSize"))
            assert(stats.containsKey("cacheTimestampsSize"))
            assert(stats.containsKey("cacheExpirationTimeMs"))
        }
    }

    /**
     * Benchmark: Cache invalidation
     * This tests the performance of cache invalidation operations
     */
    @Test
    fun benchmarkCacheInvalidation() {
        benchmarkRule.measureRepeated {
            runBlocking {
                currencySortingService.invalidateCache(1L)
                // Verify cache is cleared
                val stats = currencySortingService.getCacheStats()
                assert(stats["sortedCurrenciesCacheSize"] == 0)
            }
        }
    }

    /**
     * Benchmark: Reactive currency sorting
     * This tests the performance of reactive currency sorting with Flow
     */
    @Test
    fun benchmarkReactiveCurrencySorting() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val sortedCurrencies = currencySortingService.getSortedCurrencies(1L).first()
                assert(sortedCurrencies.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Enhanced reactive currency sorting
     * This tests the performance of enhanced reactive sorting with multiple data sources
     */
    @Test
    fun benchmarkEnhancedReactiveSorting() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val sortedCurrencies = currencySortingService.getSortedCurrenciesReactive(1L).first()
                assert(sortedCurrencies.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Concurrent sorting operations
     * This tests the performance under concurrent load simulation
     */
    @Test
    fun benchmarkConcurrentSortingOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val operations = listOf(
                    async { currencySortingService.getSortedCurrenciesSuspend(1L) },
                    async { currencySortingService.getTopCurrencies(1L, 3).first() },
                    async { currencySortingService.getUsedCurrencies(1L).first() },
                    async { currencySortingService.getSortedCurrencies(1L).first() },
                    async { currencySortingService.getSortedCurrenciesReactive(1L).first() }
                )
                
                val results = operations.map { it.await() }
                assert(results.size == 5)
                assert(results.all { it.isNotEmpty() })
            }
        }
    }

    /**
     * Benchmark: Large dataset sorting simulation
     * This tests the performance with a larger simulated dataset
     */
    @Test
    fun benchmarkLargeDatasetSorting() {
        // Create a larger mock dataset
        val largeMockCurrencyUsage = (1..50).map { index ->
            CurrencyUsage(
                id = index.toLong(),
                userId = 1L,
                currency = "CURRENCY_$index",
                usageCount = (100 - index),
                lastUsed = Date()
            )
        }

        whenever(currencyUsageRepository.getUserCurrenciesSortedByUsage(1L))
            .thenReturn(flowOf(largeMockCurrencyUsage))

        benchmarkRule.measureRepeated {
            runBlocking {
                val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(1L)
                assert(sortedCurrencies.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Memory usage during sorting operations
     * This tests memory efficiency during sorting operations
     */
    @Test
    fun benchmarkMemoryUsageDuringSorting() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Perform multiple sorting operations to test memory usage
                repeat(10) {
                    val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(1L)
                    assert(sortedCurrencies.isNotEmpty())
                }
                
                // Check cache stats to monitor memory usage
                val stats = currencySortingService.getCacheStats()
                assert(stats["sortedCurrenciesCacheSize"] is Number)
            }
        }
    }
}
