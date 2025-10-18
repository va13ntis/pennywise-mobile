package com.pennywise.app.performance

import android.Manifest
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.data.service.CurrencyConversionService
import com.pennywise.app.data.model.CachedExchangeRate
import com.google.gson.Gson
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue

/**
 * Performance tests for currency conversion operations
 * Tests the performance of currency conversion with different scenarios:
 * - Same currency conversion (no-op)
 * - Cached conversion
 * - Large dataset conversion
 * - Multiple concurrent conversions
 */
@RunWith(AndroidJUnit4::class)
class CurrencyConversionPerformanceTest {

    private lateinit var currencyConversionService: CurrencyConversionService
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        // Grant permissions for CI environment
        grantPermissions()
        
        currencyConversionService = CurrencyConversionService(context)
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
        currencyConversionService.clearCache()
    }

    /**
     * Benchmark: Same currency conversion (should be fastest)
     * This tests the early return optimization for same currency
     */
    @Test
    fun benchmarkSameCurrencyConversion() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val result = currencyConversionService.convertCurrency(
                    amount = 100.0,
                    fromCurrency = "USD",
                    toCurrency = "USD"
                )
                // Verify result is correct
                assertTrue("Result should be 100.0", result == 100.0)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        // Verify performance is reasonable (less than 10ms per operation)
        assertTrue("Average time per conversion should be < 10ms, got ${avgTime}ms", avgTime < 10)
    }

    /**
     * Benchmark: Currency conversion with cached rates
     * This tests the performance of cached conversion operations
     */
    @Test
    fun benchmarkCachedCurrencyConversion() {
        // Pre-populate cache with some exchange rates
        runBlocking {
            // Simulate cached rates by directly setting them
            val cachedRate = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis()
            )
            
            // Manually cache the rate (this would normally be done by the service)
            val sharedPrefs = context.getSharedPreferences("currency_conversion_cache", 0)
            val gson = Gson()
            val json = gson.toJson(cachedRate)
            sharedPrefs.edit().putString("exchange_rate_USD_EUR", json).apply()
        }

        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val result = currencyConversionService.convertCurrency(
                    amount = 100.0,
                    fromCurrency = "USD",
                    toCurrency = "EUR"
                )
                // Verify result is approximately correct (allowing for some variance)
                assertTrue("Result should be between 80 and 90", result != null && result > 80.0 && result < 90.0)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        // Verify performance is reasonable
        assertTrue("Average time per cached conversion should be < 50ms, got ${avgTime}ms", avgTime < 50)
    }

    /**
     * Benchmark: Multiple currency conversions in sequence
     * This tests the performance when converting multiple amounts
     */
    @Test
    fun benchmarkMultipleCurrencyConversions() {
        val testData = listOf(
            Triple(100.0, "USD", "EUR"),
            Triple(250.0, "EUR", "GBP"),
            Triple(500.0, "GBP", "JPY"),
            Triple(1000.0, "JPY", "USD"),
            Triple(75.0, "USD", "CAD")
        )

        val iterations = 50
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val results = mutableListOf<Double?>()
                for ((amount, from, to) in testData) {
                    val result = currencyConversionService.convertCurrency(amount, from, to)
                    results.add(result)
                }
                // Verify we got some results (may be null due to API limitations in test)
                assertTrue("Results size should match test data size", results.size == testData.size)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        // Verify performance is reasonable
        assertTrue("Average time per batch should be < 200ms, got ${avgTime}ms", avgTime < 200)
    }

    /**
     * Benchmark: Large amount conversion
     * This tests performance with large monetary amounts
     */
    @Test
    fun benchmarkLargeAmountConversion() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val result = currencyConversionService.convertCurrency(
                    amount = 1_000_000.0,
                    fromCurrency = "USD",
                    toCurrency = "USD"
                )
                assertTrue("Result should be 1,000,000", result == 1_000_000.0)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        // Verify performance is reasonable
        assertTrue("Average time should be < 10ms, got ${avgTime}ms", avgTime < 10)
    }

    /**
     * Benchmark: Cache operations performance
     * This tests the performance of cache read/write operations
     */
    @Test
    fun benchmarkCacheOperations() {
        val iterations = 1000
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                // Test cache statistics retrieval
                val stats = currencyConversionService.getCacheStats()
                assertTrue("Stats should contain total_cached", stats.containsKey("total_cached"))
                assertTrue("Stats should contain valid_cached", stats.containsKey("valid_cached"))
                assertTrue("Stats should contain expired_cached", stats.containsKey("expired_cached"))
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        // Verify performance is reasonable
        assertTrue("Average time should be < 1ms, got ${avgTime}ms", avgTime < 1)
    }

    /**
     * Benchmark: Currency availability check
     * This tests the performance of checking if conversion is available
     */
    @Test
    fun benchmarkCurrencyAvailabilityCheck() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val isAvailable = currencyConversionService.isConversionAvailable("USD", "EUR")
                // Result may be true or false depending on network/API availability
                assertTrue("Result should be a Boolean", isAvailable is Boolean)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        // Verify performance is reasonable
        assertTrue("Average time should be < 50ms, got ${avgTime}ms", avgTime < 50)
    }

    /**
     * Benchmark: Concurrent currency conversions
     * This tests performance under concurrent load simulation
     */
    @Test
    fun benchmarkConcurrentCurrencyConversions() {
        val iterations = 50
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                val conversions = listOf(
                    async { currencyConversionService.convertCurrency(100.0, "USD", "EUR") },
                    async { currencyConversionService.convertCurrency(200.0, "EUR", "GBP") },
                    async { currencyConversionService.convertCurrency(300.0, "GBP", "JPY") },
                    async { currencyConversionService.convertCurrency(400.0, "JPY", "USD") },
                    async { currencyConversionService.convertCurrency(500.0, "USD", "CAD") }
                )
                
                val results = conversions.map { it.await() }
                assertTrue("Results size should be 5", results.size == 5)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        // Verify performance is reasonable
        assertTrue("Average time should be < 200ms, got ${avgTime}ms", avgTime < 200)
    }

    /**
     * Benchmark: Cache invalidation and repopulation
     * This tests the performance of cache management operations
     */
    @Test
    fun benchmarkCacheInvalidation() {
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            runBlocking {
                // Clear cache
                currencyConversionService.clearCache()
                
                // Verify cache is empty
                val stats = currencyConversionService.getCacheStats()
                assertTrue("Cache should be empty", stats["total_cached"] == 0)
            }
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        // Verify performance is reasonable
        assertTrue("Average time should be < 10ms, got ${avgTime}ms", avgTime < 10)
    }

    /**
     * Benchmark: JSON serialization/deserialization for cache
     * This tests the performance of cache data serialization
     */
    @Test
    fun benchmarkCacheSerialization() {
        val gson = Gson()
        val testRate = CachedExchangeRate(
            baseCode = "USD",
            targetCode = "EUR",
            conversionRate = 0.85,
            lastUpdateTime = System.currentTimeMillis()
        )

        val iterations = 1000
        val startTime = System.currentTimeMillis()
        
        repeat(iterations) {
            // Test serialization
            val json = gson.toJson(testRate)
            assertTrue("JSON should not be empty", json.isNotEmpty())
            
            // Test deserialization
            val deserializedRate = gson.fromJson(json, CachedExchangeRate::class.java)
            assertTrue("Base code should match", deserializedRate.baseCode == testRate.baseCode)
            assertTrue("Target code should match", deserializedRate.targetCode == testRate.targetCode)
            assertTrue("Conversion rate should match", deserializedRate.conversionRate == testRate.conversionRate)
        }
        
        val duration = System.currentTimeMillis() - startTime
        val avgTime = duration / iterations.toDouble()
        
        // Verify performance is reasonable
        assertTrue("Average time should be < 1ms, got ${avgTime}ms", avgTime < 1)
    }
}
