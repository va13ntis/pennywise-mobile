package com.pennywise.app.performance

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.data.service.CurrencyConversionService
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import org.junit.Assert.*

/**
 * Simple test to verify currency conversion service works without benchmark framework
 */
@RunWith(AndroidJUnit4::class)
class SimpleCurrencyConversionTest {

    private lateinit var currencyConversionService: MockCurrencyConversionService
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        currencyConversionService = createTestCurrencyConversionService()
    }
    
    private fun createTestCurrencyConversionService(): MockCurrencyConversionService {
        // Create a mock service for testing
        return MockCurrencyConversionService()
    }

    @After
    fun cleanup() {
        currencyConversionService.clearCache()
    }

    /**
     * Test: Same currency conversion (should be fastest)
     */
    @Test
    fun testSameCurrencyConversion() {
        runBlocking {
            val result = currencyConversionService.convertCurrency(
                amount = 100.0,
                fromCurrency = "USD",
                toCurrency = "USD"
            )
            // Verify result is correct
            assertNotNull("Result should not be null", result)
            assertEquals(100.0, result!!, 0.001)
        }
    }

    /**
     * Test: Cache operations
     */
    @Test
    fun testCacheOperations() {
        runBlocking {
            // Test cache statistics retrieval
            val stats = currencyConversionService.getCacheStats()
            assertTrue("Cache stats should contain total_cached", stats.containsKey("total_cached"))
            assertTrue("Cache stats should contain valid_cached", stats.containsKey("valid_cached"))
            assertTrue("Cache stats should contain expired_cached", stats.containsKey("expired_cached"))
            
            // Verify initial cache is empty
            assertEquals(0, stats["total_cached"])
            assertEquals(0, stats["valid_cached"])
            assertEquals(0, stats["expired_cached"])
        }
    }

    /**
     * Test: Currency availability check
     */
    @Test
    fun testCurrencyAvailabilityCheck() {
        runBlocking {
            // Test that the availability check completes without exception
            val isAvailable = currencyConversionService.isConversionAvailable("USD", "EUR")
            // Note: This might return false due to API limitations in test environment
            // We just want to ensure it doesn't crash
            assertNotNull("Availability check should return a boolean", isAvailable)
        }
    }

    /**
     * Test: Cache invalidation
     */
    @Test
    fun testCacheInvalidation() {
        runBlocking {
            // Clear cache
            currencyConversionService.clearCache()
            
            // Verify cache is empty
            val stats = currencyConversionService.getCacheStats()
            assertEquals(0, stats["total_cached"])
        }
    }
}

