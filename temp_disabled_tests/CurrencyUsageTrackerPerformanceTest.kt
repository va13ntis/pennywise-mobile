package com.pennywise.app.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.domain.usecase.CurrencyUsageTracker
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import java.util.Date

/**
 * Performance tests for currency usage tracking operations
 * Tests the performance of currency usage tracking with different scenarios:
 * - Single currency usage tracking
 * - Multiple currency usage tracking
 * - Statistics calculation
 * - Trend analysis
 * - Summary generation
 */
@RunWith(AndroidJUnit4::class)
class CurrencyUsageTrackerPerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Mock
    private lateinit var currencyUsageRepository: CurrencyUsageRepository

    private lateinit var currencyUsageTracker: CurrencyUsageTracker
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        currencyUsageTracker = CurrencyUsageTracker(currencyUsageRepository, Dispatchers.IO)
        
        // Setup mock data
        setupMockData()
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
        whenever(currencyUsageRepository.getCurrencyUsageByUser(1L))
            .thenReturn(flowOf(mockCurrencyUsage))
        
        whenever(currencyUsageRepository.getTopCurrenciesByUser(1L, 10))
            .thenReturn(flowOf(mockCurrencyUsage.take(10)))
        
        whenever(currencyUsageRepository.getTopCurrenciesByUser(1L, 5))
            .thenReturn(flowOf(mockCurrencyUsage.take(5)))
        
        whenever(currencyUsageRepository.getTopCurrenciesByUser(1L, 3))
            .thenReturn(flowOf(mockCurrencyUsage.take(3)))
    }

    /**
     * Benchmark: Single currency usage tracking
     * This tests the performance of tracking a single currency usage
     */
    @Test
    fun benchmarkSingleCurrencyUsageTracking() {
        benchmarkRule.measureRepeated {
            runBlocking {
                currencyUsageTracker.trackCurrencyUsage(1L, "USD")
                // Verify tracking was successful (would be tested in integration tests)
            }
        }
    }

    /**
     * Benchmark: Multiple currency usage tracking
     * This tests the performance of tracking multiple currency usages
     */
    @Test
    fun benchmarkMultipleCurrencyUsageTracking() {
        val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "SEK", "NOK", "DKK")
        
        benchmarkRule.measureRepeated {
            runBlocking {
                currencies.forEach { currency ->
                    currencyUsageTracker.trackCurrencyUsage(1L, currency)
                }
            }
        }
    }

    /**
     * Benchmark: Currency popularity retrieval
     * This tests the performance of getting currencies sorted by popularity
     */
    @Test
    fun benchmarkCurrencyPopularityRetrieval() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val popularCurrencies = currencyUsageTracker.getUserCurrenciesByPopularity(1L)
                assert(popularCurrencies.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Top currencies retrieval
     * This tests the performance of getting top currencies with limit
     */
    @Test
    fun benchmarkTopCurrenciesRetrieval() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val topCurrencies = currencyUsageTracker.getTopCurrenciesForUser(1L, 5)
                assert(topCurrencies.size <= 5)
            }
        }
    }

    /**
     * Benchmark: Currency usage statistics calculation
     * This tests the performance of calculating usage statistics
     */
    @Test
    fun benchmarkCurrencyUsageStatisticsCalculation() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val stats = currencyUsageTracker.getCurrencyUsageStats(1L)
                assert(stats.totalUsage > 0)
                assert(stats.uniqueCurrencies > 0)
                assert(stats.mostUsedCurrency != null)
            }
        }
    }

    /**
     * Benchmark: Most used currencies retrieval
     * This tests the performance of getting most used currencies with percentages
     */
    @Test
    fun benchmarkMostUsedCurrenciesRetrieval() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val mostUsed = currencyUsageTracker.getMostUsedCurrencies(1L, 5)
                assert(mostUsed.isNotEmpty())
                assert(mostUsed.all { it.percentage >= 0.0 })
            }
        }
    }

    /**
     * Benchmark: Least used currencies retrieval
     * This tests the performance of getting least used currencies
     */
    @Test
    fun benchmarkLeastUsedCurrenciesRetrieval() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val leastUsed = currencyUsageTracker.getLeastUsedCurrencies(1L, 5)
                assert(leastUsed.isNotEmpty())
                assert(leastUsed.all { it.percentage >= 0.0 })
            }
        }
    }

    /**
     * Benchmark: Currency usage trend analysis
     * This tests the performance of trend analysis calculations
     */
    @Test
    fun benchmarkCurrencyUsageTrendAnalysis() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val trend = currencyUsageTracker.getCurrencyUsageTrend(1L, 30)
                assert(trend.totalCurrencies > 0)
                assert(trend.activeCurrencies >= 0)
            }
        }
    }

    /**
     * Benchmark: Currency usage summary generation
     * This tests the performance of generating comprehensive usage summaries
     */
    @Test
    fun benchmarkCurrencyUsageSummaryGeneration() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val summary = currencyUsageTracker.getCurrencyUsageSummary(1L)
                assert(summary.totalTransactions > 0)
                assert(summary.uniqueCurrencies > 0)
                assert(summary.topCurrencies.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Concurrent usage tracking operations
     * This tests the performance under concurrent load simulation
     */
    @Test
    fun benchmarkConcurrentUsageTrackingOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val operations = listOf(
                    async { currencyUsageTracker.trackCurrencyUsage(1L, "USD") },
                    async { currencyUsageTracker.getUserCurrenciesByPopularity(1L) },
                    async { currencyUsageTracker.getTopCurrenciesForUser(1L, 3) },
                    async { currencyUsageTracker.getCurrencyUsageStats(1L) },
                    async { currencyUsageTracker.getMostUsedCurrencies(1L, 3) },
                    async { currencyUsageTracker.getCurrencyUsageTrend(1L, 30) },
                    async { currencyUsageTracker.getCurrencyUsageSummary(1L) }
                )
                
                val results = operations.map { it.await() }
                assert(results.size == 7)
            }
        }
    }

    /**
     * Benchmark: Large dataset statistics calculation
     * This tests the performance with a larger simulated dataset
     */
    @Test
    fun benchmarkLargeDatasetStatisticsCalculation() {
        // Create a larger mock dataset
        val largeMockCurrencyUsage = (1..100).map { index ->
            CurrencyUsage(
                id = index.toLong(),
                userId = 1L,
                currency = "CURRENCY_$index",
                usageCount = (1000 - index),
                lastUsed = Date()
            )
        }

        whenever(currencyUsageRepository.getCurrencyUsageByUser(1L))
            .thenReturn(flowOf(largeMockCurrencyUsage))

        benchmarkRule.measureRepeated {
            runBlocking {
                val stats = currencyUsageTracker.getCurrencyUsageStats(1L)
                assert(stats.totalUsage > 0)
                assert(stats.uniqueCurrencies == 100)
            }
        }
    }

    /**
     * Benchmark: Memory usage during statistics calculation
     * This tests memory efficiency during complex calculations
     */
    @Test
    fun benchmarkMemoryUsageDuringStatisticsCalculation() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Perform multiple statistics calculations to test memory usage
                repeat(10) {
                    val stats = currencyUsageTracker.getCurrencyUsageStats(1L)
                    val mostUsed = currencyUsageTracker.getMostUsedCurrencies(1L, 5)
                    val trend = currencyUsageTracker.getCurrencyUsageTrend(1L, 30)
                    val summary = currencyUsageTracker.getCurrencyUsageSummary(1L)
                    
                    assert(stats.totalUsage > 0)
                    assert(mostUsed.isNotEmpty())
                    assert(trend.totalCurrencies > 0)
                    assert(summary.totalTransactions > 0)
                }
            }
        }
    }

    /**
     * Benchmark: Percentage calculation performance
     * This tests the performance of percentage calculations in usage statistics
     */
    @Test
    fun benchmarkPercentageCalculationPerformance() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val mostUsed = currencyUsageTracker.getMostUsedCurrencies(1L, 10)
                val leastUsed = currencyUsageTracker.getLeastUsedCurrencies(1L, 10)
                
                // Verify percentage calculations are correct
                val totalPercentage = mostUsed.sumOf { it.percentage }
                assert(totalPercentage <= 100.0) // Should not exceed 100%
                
                assert(mostUsed.all { it.percentage >= 0.0 })
                assert(leastUsed.all { it.percentage >= 0.0 })
            }
        }
    }

    /**
     * Benchmark: Date-based trend analysis performance
     * This tests the performance of date-based filtering and analysis
     */
    @Test
    fun benchmarkDateBasedTrendAnalysisPerformance() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val trend30Days = currencyUsageTracker.getCurrencyUsageTrend(1L, 30)
                val trend7Days = currencyUsageTracker.getCurrencyUsageTrend(1L, 7)
                val trend90Days = currencyUsageTracker.getCurrencyUsageTrend(1L, 90)
                
                assert(trend30Days.totalCurrencies > 0)
                assert(trend7Days.totalCurrencies > 0)
                assert(trend90Days.totalCurrencies > 0)
            }
        }
    }
}
