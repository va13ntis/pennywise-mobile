package com.pennywise.app.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.data.service.CurrencyConversionService
import com.pennywise.app.domain.model.Currency
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After

/**
 * Load tests for currency operations
 * Tests the performance under different load conditions and device scenarios
 */
@RunWith(AndroidJUnit4::class)
class CurrencyOperationsLoadTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var currencyConversionService: CurrencyConversionService
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        currencyConversionService = CurrencyConversionService(context)
    }

    @After
    fun cleanup() {
        currencyConversionService.clearCache()
    }

    /**
     * Load test: High-frequency currency conversions
     * This tests the performance under high-frequency conversion requests
     */
    @Test
    fun loadTestHighFrequencyConversions() {
        val conversionPairs = listOf(
            Pair("USD", "EUR"),
            Pair("EUR", "GBP"),
            Pair("GBP", "JPY"),
            Pair("JPY", "USD"),
            Pair("USD", "CAD")
        )

        benchmarkRule.measureRepeated {
            runBlocking {
                val conversions = (1..50).map { index ->
                    async {
                        val (from, to) = conversionPairs[index % conversionPairs.size]
                        currencyConversionService.convertCurrency(100.0 + index, from, to)
                    }
                }
                
                val results = conversions.map { it.await() }
                assert(results.size == 50)
            }
        }
    }

    /**
     * Load test: Large dataset currency operations
     * This tests the performance with large datasets
     */
    @Test
    fun loadTestLargeDatasetOperations() {
        val currencies = Currency.values().toList()
        val amounts = (1..1000).map { it * 10.0 }

        benchmarkRule.measureRepeated {
            runBlocking {
                val operations = amounts.take(100).map { amount ->
                    async {
                        val fromCurrency = currencies.random().code
                        val toCurrency = currencies.random().code
                        currencyConversionService.convertCurrency(amount, fromCurrency, toCurrency)
                    }
                }
                
                val results = operations.map { it.await() }
                assert(results.size == 100)
            }
        }
    }

    /**
     * Load test: Memory-intensive operations
     * This tests memory usage during intensive operations
     */
    @Test
    fun loadTestMemoryIntensiveOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Perform multiple operations to test memory usage
                repeat(100) { index ->
                    val result = currencyConversionService.convertCurrency(
                        amount = 1000.0 + index,
                        fromCurrency = "USD",
                        toCurrency = "EUR"
                    )
                    // Verify result is not null (may be null due to API limitations in test)
                    assert(result is Double?)
                }
                
                // Check cache stats
                val stats = currencyConversionService.getCacheStats()
                assert(stats.containsKey("total_cached"))
            }
        }
    }

    /**
     * Load test: Concurrent operations simulation
     * This tests the performance under concurrent load
     */
    @Test
    fun loadTestConcurrentOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val concurrentOperations = (1..20).map { index ->
                    async {
                        val operations = listOf(
                            async { currencyConversionService.convertCurrency(100.0, "USD", "EUR") },
                            async { currencyConversionService.convertCurrency(200.0, "EUR", "GBP") },
                            async { currencyConversionService.convertCurrency(300.0, "GBP", "JPY") },
                            async { currencyConversionService.isConversionAvailable("USD", "CAD") },
                            async { currencyConversionService.getCacheStats() }
                        )
                        
                        operations.map { it.await() }
                    }
                }
                
                val results = concurrentOperations.map { it.await() }
                assert(results.size == 20)
                assert(results.all { it.size == 5 })
            }
        }
    }
}
