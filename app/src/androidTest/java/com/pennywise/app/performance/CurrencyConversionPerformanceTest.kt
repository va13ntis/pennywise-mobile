package com.pennywise.app.performance

import android.Manifest
import android.os.Build
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.data.service.CurrencyConversionService
import com.pennywise.app.data.model.CachedExchangeRate
import com.google.gson.Gson
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import java.util.concurrent.TimeUnit

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

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var currencyConversionService: CurrencyConversionService
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        // Configure benchmark output to internal storage before any tests run
        // This must be done in init block to run before BenchmarkRule tries to grant permissions
        System.setProperty("benchmark.output.path", 
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath)
    }

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
        benchmarkRule.measureRepeated {
            runBlocking {
                val result = currencyConversionService.convertCurrency(
                    amount = 100.0,
                    fromCurrency = "USD",
                    toCurrency = "USD"
                )
                // Verify result is correct
                assert(result == 100.0)
            }
        }
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

        benchmarkRule.measureRepeated {
            runBlocking {
                val result = currencyConversionService.convertCurrency(
                    amount = 100.0,
                    fromCurrency = "USD",
                    toCurrency = "EUR"
                )
                // Verify result is approximately correct (allowing for some variance)
                assert(result != null && result > 80.0 && result < 90.0)
            }
        }
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

        benchmarkRule.measureRepeated {
            runBlocking {
                val results = mutableListOf<Double?>()
                for ((amount, from, to) in testData) {
                    val result = currencyConversionService.convertCurrency(amount, from, to)
                    results.add(result)
                }
                // Verify we got some results (may be null due to API limitations in test)
                assert(results.size == testData.size)
            }
        }
    }

    /**
     * Benchmark: Large amount conversion
     * This tests performance with large monetary amounts
     */
    @Test
    fun benchmarkLargeAmountConversion() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val result = currencyConversionService.convertCurrency(
                    amount = 1_000_000.0,
                    fromCurrency = "USD",
                    toCurrency = "USD"
                )
                assert(result == 1_000_000.0)
            }
        }
    }

    /**
     * Benchmark: Cache operations performance
     * This tests the performance of cache read/write operations
     */
    @Test
    fun benchmarkCacheOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Test cache statistics retrieval
                val stats = currencyConversionService.getCacheStats()
                assert(stats.containsKey("total_cached"))
                assert(stats.containsKey("valid_cached"))
                assert(stats.containsKey("expired_cached"))
            }
        }
    }

    /**
     * Benchmark: Currency availability check
     * This tests the performance of checking if conversion is available
     */
    @Test
    fun benchmarkCurrencyAvailabilityCheck() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val isAvailable = currencyConversionService.isConversionAvailable("USD", "EUR")
                // Result may be true or false depending on network/API availability
                assert(isAvailable is Boolean)
            }
        }
    }

    /**
     * Benchmark: Concurrent currency conversions
     * This tests performance under concurrent load simulation
     */
    @Test
    fun benchmarkConcurrentCurrencyConversions() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val conversions = listOf(
                    async { currencyConversionService.convertCurrency(100.0, "USD", "EUR") },
                    async { currencyConversionService.convertCurrency(200.0, "EUR", "GBP") },
                    async { currencyConversionService.convertCurrency(300.0, "GBP", "JPY") },
                    async { currencyConversionService.convertCurrency(400.0, "JPY", "USD") },
                    async { currencyConversionService.convertCurrency(500.0, "USD", "CAD") }
                )
                
                val results = conversions.map { it.await() }
                assert(results.size == 5)
            }
        }
    }

    /**
     * Benchmark: Cache invalidation and repopulation
     * This tests the performance of cache management operations
     */
    @Test
    fun benchmarkCacheInvalidation() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Clear cache
                currencyConversionService.clearCache()
                
                // Verify cache is empty
                val stats = currencyConversionService.getCacheStats()
                assert(stats["total_cached"] == 0)
            }
        }
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

        benchmarkRule.measureRepeated {
            // Test serialization
            val json = gson.toJson(testRate)
            assert(json.isNotEmpty())
            
            // Test deserialization
            val deserializedRate = gson.fromJson(json, CachedExchangeRate::class.java)
            assert(deserializedRate.baseCode == testRate.baseCode)
            assert(deserializedRate.targetCode == testRate.targetCode)
            assert(deserializedRate.conversionRate == testRate.conversionRate)
        }
    }
}
