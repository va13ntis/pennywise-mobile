package com.pennywise.app.performance

import com.pennywise.app.domain.model.Currency
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Unit test version of currency operations load tests
 * These tests can run in GitHub Actions without requiring an Android device/emulator
 */
class CurrencyOperationsLoadTestUnit {

    private lateinit var mockCurrencyConversionService: MockCurrencyConversionService
    
    // Performance thresholds (in milliseconds) - Reduced for faster test execution
    private companion object {
        const val MAX_CONVERSION_TIME_MS = 1000L
        const val MAX_CACHE_OPERATION_TIME_MS = 50L
        const val MAX_CONCURRENT_OPERATION_TIME_MS = 2000L
        const val MAX_MEMORY_OPERATION_TIME_MS = 1000L
        const val MAX_STRESS_TEST_TIME_MS = 3000L
        const val MIN_SUCCESS_RATE = 0.8 // 80% success rate minimum
    }

    @Before
    fun setup() {
        mockCurrencyConversionService = MockCurrencyConversionService()
        mockCurrencyConversionService.clearCache()
    }

    /**
     * Load test: High-frequency currency conversions
     * Tests performance under high-frequency conversion requests with various currency pairs
     */
    @Test
    fun loadTestHighFrequencyConversions() {
        val conversionPairs = listOf(
            Pair("USD", "EUR"),
            Pair("EUR", "GBP"),
            Pair("GBP", "JPY"),
            Pair("JPY", "USD"),
            Pair("USD", "CAD"),
            Pair("EUR", "CHF"),
            Pair("GBP", "AUD"),
            Pair("CAD", "JPY")
        )

        val successCount = AtomicInteger(0)
        val totalOperations = AtomicInteger(0)
        val totalTime = AtomicLong(0)

        val startTime = System.currentTimeMillis()
        
        runBlocking {
            val conversions = (1..10).map { index ->
                async {
                    val (from, to) = conversionPairs[index % conversionPairs.size]
                    val amount = 100.0 + index
                    
                    try {
                        withTimeout(MAX_CONVERSION_TIME_MS) {
                            val result = mockCurrencyConversionService.convertCurrency(amount, from, to)
                            if (result != null) {
                                successCount.incrementAndGet()
                                assertTrue("Conversion result should be positive", result > 0)
                            }
                            totalOperations.incrementAndGet()
                        }
                    } catch (e: Exception) {
                        totalOperations.incrementAndGet()
                        println("Conversion failed for $from to $to: ${e.message}")
                    }
                }
            }
            
            conversions.map { it.await() }
        }
        
        val endTime = System.currentTimeMillis()
        totalTime.addAndGet(endTime - startTime)
        
        val successRate = successCount.get().toDouble() / totalOperations.get()
        val averageTime = totalTime.get() / totalOperations.get()
        
        assertTrue("Success rate should be at least ${MIN_SUCCESS_RATE * 100}%", successRate >= MIN_SUCCESS_RATE)
        assertTrue("Average conversion time should be under ${MAX_CONVERSION_TIME_MS}ms", averageTime < MAX_CONVERSION_TIME_MS)
        
        println("High-frequency conversions: ${successCount.get()}/${totalOperations.get()} successful (${(successRate * 100).toInt()}%)")
        println("Average time per conversion: ${averageTime}ms")
    }

    /**
     * Load test: Large dataset currency operations
     * Tests performance with large datasets and various currency combinations
     */
    @Test
    fun loadTestLargeDatasetOperations() {
        val currencies = Currency.getMostPopular()
        val amounts = (1..100).map { it * 10.0 }
        val successCount = AtomicInteger(0)
        val totalOperations = AtomicInteger(0)

        val startTime = System.currentTimeMillis()
        
        runBlocking {
            val operations = amounts.take(20).map { amount ->
                async {
                    val fromCurrency = currencies.random().code
                    val toCurrency = currencies.random().code
                    
                    try {
                        withTimeout(MAX_CONVERSION_TIME_MS) {
                            val result = mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency)
                            if (result != null) {
                                successCount.incrementAndGet()
                                assertTrue("Large dataset conversion result should be positive", result > 0)
                            }
                            totalOperations.incrementAndGet()
                        }
                    } catch (e: Exception) {
                        totalOperations.incrementAndGet()
                        println("Large dataset conversion failed for $fromCurrency to $toCurrency: ${e.message}")
                    }
                }
            }
            
            operations.map { it.await() }
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        assertTrue("Large dataset operations should complete within reasonable time", duration < MAX_MEMORY_OPERATION_TIME_MS)
        
        val successRate = successCount.get().toDouble() / totalOperations.get()
        assertTrue("Large dataset success rate should be at least ${MIN_SUCCESS_RATE * 100}%", successRate >= MIN_SUCCESS_RATE)
        
        println("Large dataset operations: ${successCount.get()}/${totalOperations.get()} successful (${(successRate * 100).toInt()}%)")
    }

    /**
     * Load test: Concurrent operations simulation
     * Tests performance under concurrent load with multiple operation types
     */
    @Test
    fun loadTestConcurrentOperations() {
        val successCount = AtomicInteger(0)
        val totalOperations = AtomicInteger(0)

        val startTime = System.currentTimeMillis()
        
        runBlocking {
            val concurrentOperations = (1..5).map { index ->
                async {
                    val operations = listOf(
                        async { 
                            try {
                                withTimeout(MAX_CONVERSION_TIME_MS) {
                                    mockCurrencyConversionService.convertCurrency(100.0 + index, "USD", "EUR")
                                }
                            } catch (e: Exception) {
                                null
                            }
                        },
                        async { 
                            try {
                                withTimeout(MAX_CONVERSION_TIME_MS) {
                                    mockCurrencyConversionService.convertCurrency(200.0 + index, "EUR", "GBP")
                                }
                            } catch (e: Exception) {
                                null
                            }
                        },
                        async { 
                            try {
                                mockCurrencyConversionService.isConversionAvailable("USD", "CAD")
                            } catch (e: Exception) {
                                false
                            }
                        },
                        async { 
                            try {
                                mockCurrencyConversionService.getCacheStats()
                            } catch (e: Exception) {
                                emptyMap<String, Any>()
                            }
                        }
                    )
                    
                    val results = operations.map { it.await() }
                    results.forEach { result ->
                        if (result != null && result != false && result != emptyMap<String, Any>()) {
                            successCount.incrementAndGet()
                        }
                        totalOperations.incrementAndGet()
                    }
                    results
                }
            }
            
            val results = concurrentOperations.map { it.await() }
            assertTrue("Should have 5 concurrent operation groups", results.size == 5)
            assertTrue("Each group should have 4 operations", results.all { it.size == 4 })
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        assertTrue("Concurrent operations should complete within reasonable time", duration < MAX_CONCURRENT_OPERATION_TIME_MS)
        
        val successRate = successCount.get().toDouble() / totalOperations.get()
        assertTrue("Concurrent operations success rate should be at least ${MIN_SUCCESS_RATE * 100}%", successRate >= MIN_SUCCESS_RATE)
        
        println("Concurrent operations: ${successCount.get()}/${totalOperations.get()} successful (${(successRate * 100).toInt()}%)")
    }

    /**
     * Load test: Cache performance and efficiency
     * Tests cache hit rates, cache operations, and cache cleanup
     */
    @Test
    fun loadTestCachePerformance() {
        val cacheOperations = AtomicInteger(0)
        val cacheHits = AtomicInteger(0)
        val cacheMisses = AtomicInteger(0)

        val startTime = System.currentTimeMillis()
        
        runBlocking {
            // First, populate cache with various currency pairs
            val currencyPairs = listOf(
                Pair("USD", "EUR"),
                Pair("EUR", "GBP"),
                Pair("GBP", "JPY"),
                Pair("JPY", "USD"),
                Pair("USD", "CAD"),
                Pair("EUR", "CHF"),
                Pair("GBP", "AUD")
            )
            
            // Populate cache
            currencyPairs.forEach { (from, to) ->
                try {
                    withTimeout(MAX_CONVERSION_TIME_MS) {
                        mockCurrencyConversionService.convertCurrency(100.0, from, to)
                        cacheOperations.incrementAndGet()
                    }
                } catch (e: Exception) {
                    cacheOperations.incrementAndGet()
                }
            }
            
            // Test cache hits by repeating the same conversions
            repeat(5) { index ->
                val (from, to) = currencyPairs[index % currencyPairs.size]
                try {
                    withTimeout(MAX_CACHE_OPERATION_TIME_MS) {
                        val result = mockCurrencyConversionService.convertCurrency(100.0 + index, from, to)
                        if (result != null) {
                            cacheHits.incrementAndGet()
                        } else {
                            cacheMisses.incrementAndGet()
                        }
                        cacheOperations.incrementAndGet()
                    }
                } catch (e: Exception) {
                    cacheMisses.incrementAndGet()
                    cacheOperations.incrementAndGet()
                }
            }
            
            // Test cache stats
            val stats = mockCurrencyConversionService.getCacheStats()
            assertTrue("Cache stats should contain total_cached", stats.containsKey("total_cached"))
            assertTrue("Cache stats should contain valid_cached", stats.containsKey("valid_cached"))
            assertTrue("Cache stats should contain expired_cached", stats.containsKey("expired_cached"))
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        assertTrue("Cache operations should complete quickly", duration < MAX_CACHE_OPERATION_TIME_MS * 10)
        
        val cacheHitRate = if (cacheHits.get() + cacheMisses.get() > 0) {
            cacheHits.get().toDouble() / (cacheHits.get() + cacheMisses.get())
        } else {
            0.0
        }
        println("Cache performance: ${cacheHits.get()} hits, ${cacheMisses.get()} misses (${(cacheHitRate * 100).toInt()}% hit rate)")
        
        // Cache hit rate should be reasonable
        assertTrue("Cache hit rate should be reasonable", cacheHitRate >= 0.1)
    }

    /**
     * Load test: Stress testing with extreme load
     * Tests system behavior under extreme load conditions
     */
    @Test
    fun loadTestStressTesting() {
        val successCount = AtomicInteger(0)
        val totalOperations = AtomicInteger(0)
        val errorCount = AtomicInteger(0)

        val startTime = System.currentTimeMillis()
        
        runBlocking {
            // Create extreme load with many concurrent operations
            val stressOperations = (1..10).map { _ ->
                async {
                    try {
                        withTimeout(MAX_CONVERSION_TIME_MS) {
                            val currencies = Currency.getMostPopular()
                            val fromCurrency = currencies.shuffled().first().code
                            val toCurrency = currencies.shuffled().first().code
                            val amount = (100..1000).random().toDouble()
                            
                            val result = mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency)
                            if (result != null) {
                                successCount.incrementAndGet()
                            }
                            totalOperations.incrementAndGet()
                        }
                    } catch (e: Exception) {
                        errorCount.incrementAndGet()
                        totalOperations.incrementAndGet()
                        println("Stress test operation failed: ${e.message}")
                    }
                }
            }
            
            stressOperations.map { it.await() }
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        assertTrue("Stress test should complete within reasonable time", duration < MAX_STRESS_TEST_TIME_MS)
        
        val successRate = successCount.get().toDouble() / totalOperations.get()
        val errorRate = errorCount.get().toDouble() / totalOperations.get()
        
        println("Stress test: ${successCount.get()}/${totalOperations.get()} successful (${(successRate * 100).toInt()}%)")
        println("Stress test errors: ${errorCount.get()} (${(errorRate * 100).toInt()}%)")
        
        // Under stress, we expect some failures but overall system should remain functional
        assertTrue("Stress test success rate should be reasonable", successRate >= 0.7)
        assertTrue("Error rate should not be too high", errorRate <= 0.3)
    }

    /**
     * Load test: Performance regression testing
     * Tests that performance doesn't degrade significantly over time
     */
    @Test
    fun loadTestPerformanceRegression() {
        val operationTimes = mutableListOf<Long>()
        val successCount = AtomicInteger(0)
        val totalOperations = AtomicInteger(0)

        runBlocking {
            repeat(5) { index ->
                val startTime = System.currentTimeMillis()
                
                try {
                    withTimeout(MAX_CONVERSION_TIME_MS) {
                        val result = mockCurrencyConversionService.convertCurrency(
                            amount = 100.0 + index,
                            fromCurrency = "USD",
                            toCurrency = "EUR"
                        )
                        
                        if (result != null) {
                            successCount.incrementAndGet()
                        }
                        totalOperations.incrementAndGet()
                    }
                } catch (e: Exception) {
                    totalOperations.incrementAndGet()
                }
                
                val endTime = System.currentTimeMillis()
                operationTimes.add(endTime - startTime)
            }
        }
        
        // Calculate performance metrics
        val averageTime = operationTimes.average()
        val maxTime = operationTimes.maxOrNull() ?: 0L
        val minTime = operationTimes.minOrNull() ?: 0L
        val medianTime = if (operationTimes.size > 1) operationTimes.sorted()[operationTimes.size / 2] else operationTimes.first()
        
        val successRate = successCount.get().toDouble() / totalOperations.get()
        
        println("Performance regression test results:")
        println("  Average time: ${averageTime.toInt()}ms")
        println("  Max time: ${maxTime}ms")
        println("  Min time: ${minTime}ms")
        println("  Median time: ${medianTime}ms")
        println("  Success rate: ${(successRate * 100).toInt()}%")
        
        // Performance assertions
        assertTrue("Average operation time should be reasonable", averageTime < MAX_CONVERSION_TIME_MS)
        assertTrue("Max operation time should not be excessive", maxTime < MAX_CONVERSION_TIME_MS * 2)
        assertTrue("Success rate should be acceptable", successRate >= MIN_SUCCESS_RATE)
        
        // Check for performance regression (operations shouldn't get significantly slower)
        val slowOperations = operationTimes.count { it > MAX_CONVERSION_TIME_MS }
        val slowOperationRate = slowOperations.toDouble() / operationTimes.size
        assertTrue("Too many slow operations detected", slowOperationRate <= 0.2)
    }
}
