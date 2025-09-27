package com.pennywise.app.data.service

import android.content.Context
import android.content.SharedPreferences
import com.pennywise.app.data.api.CurrencyApi
import com.pennywise.app.data.model.CachedExchangeRate
import com.pennywise.app.data.model.ExchangeRateResponse
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import retrofit2.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.TimeUnit

/**
 * Integration tests for CurrencyConversionService
 * Tests the service's integration with real dependencies and end-to-end scenarios
 * 
 * This test focuses on integration aspects rather than pure unit testing:
 * - Real SharedPreferences integration
 * - API integration with mock responses
 * - Cache persistence and retrieval
 * - Error handling and fallback mechanisms
 * - Performance characteristics
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Currency Conversion Service Integration Tests")
class CurrencyConversionServiceIntegrationTest {

    private lateinit var service: CurrencyConversionService
    private lateinit var mockContext: Context
    private lateinit var realSharedPreferences: SharedPreferences
    private lateinit var mockCurrencyApi: CurrencyApi
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        // Create real SharedPreferences for integration testing
        mockContext = mockk<Context>(relaxed = true)
        realSharedPreferences = mockk<SharedPreferences>(relaxed = true)
        mockCurrencyApi = mockk<CurrencyApi>(relaxed = true)

        // Setup real SharedPreferences behavior
        every { mockContext.getSharedPreferences(any(), any()) } returns realSharedPreferences
        
        // Create service with real dependencies
        service = CurrencyConversionService(mockContext, mockCurrencyApi)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Nested
    @DisplayName("API Integration Tests")
    inner class ApiIntegrationTests {

        @Test
        @DisplayName("Should integrate with real API and cache response")
        fun `should integrate with real API and cache response`() = runTest {
            // Setup API response
            val apiResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            // Mock API call
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            // Mock SharedPreferences for caching
            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            every { realSharedPreferences.edit() } returns mockEditor
            every { mockEditor.putString(any(), any()) } returns mockEditor
            every { mockEditor.apply() } just Runs
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            // Execute conversion
            val result = service.convertCurrency(100.0, "USD", "EUR")
            
            // Verify result
            assertEquals(85.0, result)
            
            // Verify API was called
            coVerify { mockCurrencyApi.getExchangeRate("USD", "EUR") }
            
            // Verify cache was updated
            verify { mockEditor.putString("exchange_rate_USD_EUR", any()) }
            verify { mockEditor.apply() }
        }

        @Test
        @DisplayName("Should handle API errors gracefully with fallback")
        fun `should handle API errors gracefully with fallback`() = runTest {
            // Setup expired cache as fallback
            val expiredCache = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.80,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            )
            
            // Mock SharedPreferences to return expired cache
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.80,"lastUpdateTime":${expiredCache.lastUpdateTime}}"""
            
            // Mock API to throw exception
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws Exception("API Error")
            
            // Execute conversion
            val result = service.convertCurrency(100.0, "USD", "EUR")
            
            // Verify fallback to expired cache
            assertEquals(80.0, result)
            
            // Verify API was attempted
            coVerify { mockCurrencyApi.getExchangeRate("USD", "EUR") }
        }

        @Test
        @DisplayName("Should handle network timeout with proper error handling")
        fun `should handle network timeout with proper error handling`() = runTest {
            // Mock no cache available
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            // Mock API timeout
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws 
                java.net.SocketTimeoutException("Connection timeout")
            
            // Execute conversion
            val result = service.convertCurrency(100.0, "USD", "EUR")
            
            // Verify null result for timeout
            assertNull(result)
            
            // Verify API was attempted
            coVerify { mockCurrencyApi.getExchangeRate("USD", "EUR") }
        }

        @Test
        @DisplayName("Should handle HTTP error responses")
        fun `should handle HTTP error responses`() = runTest {
            // Mock no cache available
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            // Mock HTTP error response
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws 
                retrofit2.HttpException(Response.error<Any>(400, "Bad Request".toResponseBody("application/json".toMediaType())))
            
            // Execute conversion
            val result = service.convertCurrency(100.0, "USD", "EUR")
            
            // Verify null result for HTTP error
            assertNull(result)
        }

        @ParameterizedTest
        @CsvSource(
            "USD, EUR, 0.85",
            "EUR, GBP, 0.86", 
            "GBP, JPY, 150.0",
            "USD, CAD, 1.25",
            "EUR, USD, 1.18"
        )
        @DisplayName("Should handle multiple currency pairs correctly")
        fun `should handle multiple currency pairs correctly`(
            fromCurrency: String,
            toCurrency: String,
            expectedRate: Double
        ) = runTest {
            // Setup API response
            val apiResponse = ExchangeRateResponse(
                result = "success",
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = expectedRate,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            // Mock API call
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // Mock SharedPreferences
            every { realSharedPreferences.getString("exchange_rate_${fromCurrency}_$toCurrency", null) } returns null
            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            every { realSharedPreferences.edit() } returns mockEditor
            every { mockEditor.putString(any(), any()) } returns mockEditor
            every { mockEditor.apply() } just Runs
            
            // Execute conversion
            val result = service.convertCurrency(100.0, fromCurrency, toCurrency)
            
            // Verify result
            assertEquals(100.0 * expectedRate, result)
            
            // Verify API was called with correct parameters
            coVerify { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) }
        }
    }

    @Nested
    @DisplayName("Cache Integration Tests")
    inner class CacheIntegrationTests {

        @Test
        @DisplayName("Should persist and retrieve cache correctly")
        fun `should persist and retrieve cache correctly`() = runTest {
            // Setup valid cache
            val validCache = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            )
            
            // Mock SharedPreferences to return valid cache
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":${validCache.lastUpdateTime}}"""
            
            // Execute conversion
            val result = service.convertCurrency(100.0, "USD", "EUR")
            
            // Verify result from cache
            assertEquals(85.0, result)
            
            // Verify API was not called (cache hit)
            coVerify { mockCurrencyApi wasNot Called }
        }

        @Test
        @DisplayName("Should handle cache corruption gracefully")
        fun `should handle cache corruption gracefully`() = runTest {
            // Mock corrupted cache data
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns "invalid_json_data"
            
            // Setup API response for fallback
            val apiResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            // Mock cache update
            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            every { realSharedPreferences.edit() } returns mockEditor
            every { mockEditor.putString(any(), any()) } returns mockEditor
            every { mockEditor.apply() } just Runs
            
            // Execute conversion
            val result = service.convertCurrency(100.0, "USD", "EUR")
            
            // Verify result from API (cache was corrupted)
            assertEquals(85.0, result)
            
            // Verify API was called due to cache corruption
            coVerify { mockCurrencyApi.getExchangeRate("USD", "EUR") }
        }

        @Test
        @DisplayName("Should clear cache correctly")
        fun `should clear cache correctly`() {
            // Mock SharedPreferences with cache entries
            every { realSharedPreferences.all } returns mapOf(
                "exchange_rate_USD_EUR" to "cached_data",
                "exchange_rate_EUR_GBP" to "cached_data",
                "timestamp_USD_EUR" to "timestamp_data",
                "other_key" to "other_data"
            )
            
            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            every { realSharedPreferences.edit() } returns mockEditor
            every { mockEditor.remove(any()) } returns mockEditor
            every { mockEditor.apply() } just Runs
            
            // Execute cache clear
            service.clearCache()
            
            // Verify cache entries were removed
            verify { mockEditor.remove("exchange_rate_USD_EUR") }
            verify { mockEditor.remove("exchange_rate_EUR_GBP") }
            verify { mockEditor.remove("timestamp_USD_EUR") }
            verify { mockEditor.remove("other_key") wasNot Called }
            verify { mockEditor.apply() }
        }

        @Test
        @DisplayName("Should provide accurate cache statistics")
        fun `should provide accurate cache statistics`() {
            // Setup cache with valid and expired entries
            val validTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            val expiredTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            
            every { realSharedPreferences.all } returns mapOf(
                "exchange_rate_USD_EUR" to """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":$validTime}""",
                "exchange_rate_EUR_GBP" to """{"baseCode":"EUR","targetCode":"GBP","conversionRate":0.86,"lastUpdateTime":$expiredTime}""",
                "other_key" to "other_data"
            )
            
            // Mock individual getString calls
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":$validTime}"""
            every { realSharedPreferences.getString("exchange_rate_EUR_GBP", null) } returns 
                """{"baseCode":"EUR","targetCode":"GBP","conversionRate":0.86,"lastUpdateTime":$expiredTime}"""
            
            // Execute cache stats
            val stats = service.getCacheStats()
            
            // Verify statistics
            assertEquals(2, stats["total_cached"])
            assertEquals(1, stats["valid_cached"])
            assertEquals(1, stats["expired_cached"])
        }
    }

    @Nested
    @DisplayName("Service Integration Scenarios")
    inner class ServiceIntegrationScenarios {

        @Test
        @DisplayName("Should handle complete conversion workflow")
        fun `should handle complete conversion workflow`() = runTest {
            // Step 1: First conversion (no cache, API call)
            val apiResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            every { realSharedPreferences.edit() } returns mockEditor
            every { mockEditor.putString(any(), any()) } returns mockEditor
            every { mockEditor.apply() } just Runs
            
            val firstResult = service.convertCurrency(100.0, "USD", "EUR")
            assertEquals(85.0, firstResult)
            
            // Step 2: Second conversion (cache hit, no API call)
            val validCache = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            )
            
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":${validCache.lastUpdateTime}}"""
            
            val secondResult = service.convertCurrency(200.0, "USD", "EUR")
            assertEquals(170.0, secondResult)
            
            // Verify API was called only once
            coVerify(exactly = 1) { mockCurrencyApi.getExchangeRate("USD", "EUR") }
        }

        @Test
        @DisplayName("Should handle conversion availability check")
        fun `should handle conversion availability check`() = runTest {
            // Test with valid cache
            val validCache = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            )
            
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":${validCache.lastUpdateTime}}"""
            
            val availableWithCache = service.isConversionAvailable("USD", "EUR")
            assertTrue(availableWithCache)
            
            // Test with no cache but API available
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            val apiResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            val availableWithApi = service.isConversionAvailable("USD", "EUR")
            assertTrue(availableWithApi)
            
            // Test with no cache and API failure
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws Exception("API Error")
            
            val notAvailable = service.isConversionAvailable("USD", "EUR")
            assertFalse(notAvailable)
        }

        @Test
        @DisplayName("Should handle same currency conversion efficiently")
        fun `should handle same currency conversion efficiently`() = runTest {
            // Test same currency (case sensitive)
            val result1 = service.convertCurrency(100.0, "USD", "USD")
            assertEquals(100.0, result1)
            
            // Test same currency (case insensitive)
            val result2 = service.convertCurrency(100.0, "USD", "usd")
            assertEquals(100.0, result2)
            
            // Verify no API calls were made
            coVerify { mockCurrencyApi wasNot Called }
            
            // Verify no cache operations
            verify { realSharedPreferences wasNot Called }
        }
    }

    @Nested
    @DisplayName("Performance Integration Tests")
    inner class PerformanceIntegrationTests {

        @Test
        @DisplayName("Should handle multiple concurrent conversions efficiently")
        fun `should handle multiple concurrent conversions efficiently`() = runTest {
            val startTime = System.currentTimeMillis()
            
            // Setup API responses for different currency pairs
            val usdEurResponse = ExchangeRateResponse(
                result = "success", baseCode = "USD", targetCode = "EUR", conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z", nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            val eurGbpResponse = ExchangeRateResponse(
                result = "success", baseCode = "EUR", targetCode = "GBP", conversionRate = 0.86,
                lastUpdateTime = "2023-01-01T00:00:00Z", nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns usdEurResponse
            coEvery { mockCurrencyApi.getExchangeRate("EUR", "GBP") } returns eurGbpResponse
            
            every { realSharedPreferences.getString(any(), null) } returns null
            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            every { realSharedPreferences.edit() } returns mockEditor
            every { mockEditor.putString(any(), any()) } returns mockEditor
            every { mockEditor.apply() } just Runs
            
            // Execute multiple conversions concurrently
            val results = listOf(
                async { service.convertCurrency(100.0, "USD", "EUR") },
                async { service.convertCurrency(200.0, "EUR", "GBP") },
                async { service.convertCurrency(50.0, "USD", "USD") }
            ).map { deferred -> deferred.await() }
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            // Verify results
            assertEquals(85.0, results[0])
            assertEquals(172.0, results[1])
            assertEquals(50.0, results[2])
            
            // Verify performance (should complete within reasonable time)
            assertTrue(duration < 5000, "Concurrent conversions took too long: ${duration}ms")
        }

        @Test
        @DisplayName("Should handle large batch conversions efficiently")
        fun `should handle large batch conversions efficiently`() = runTest {
            val startTime = System.currentTimeMillis()
            
            // Setup API response
            val apiResponse = ExchangeRateResponse(
                result = "success", baseCode = "USD", targetCode = "EUR", conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z", nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            every { realSharedPreferences.edit() } returns mockEditor
            every { mockEditor.putString(any(), any()) } returns mockEditor
            every { mockEditor.apply() } just Runs
            
            // Execute batch of conversions
            val amounts = listOf(10.0, 25.0, 50.0, 100.0, 250.0, 500.0, 1000.0)
            val results = amounts.map { amount ->
                service.convertCurrency(amount, "USD", "EUR")
            }
            
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            // Verify all results
            val expectedResults = amounts.map { it * 0.85 }
            results.forEachIndexed { index, result ->
                assertEquals(expectedResults[index], result ?: 0.0, 0.01)
            }
            
            // Verify performance
            assertTrue(duration < 3000, "Batch conversions took too long: ${duration}ms")
        }
    }

    @Nested
    @DisplayName("Error Recovery Integration Tests")
    inner class ErrorRecoveryIntegrationTests {

        @Test
        @DisplayName("Should recover from temporary API failures")
        fun `should recover from temporary API failures`() = runTest {
            // First call fails, second succeeds
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws Exception("Temporary failure") andThen
                ExchangeRateResponse(
                    result = "success", baseCode = "USD", targetCode = "EUR", conversionRate = 0.85,
                    lastUpdateTime = "2023-01-01T00:00:00Z", nextUpdateTime = "2023-01-02T00:00:00Z"
                )
            
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            every { realSharedPreferences.edit() } returns mockEditor
            every { mockEditor.putString(any(), any()) } returns mockEditor
            every { mockEditor.apply() } just Runs
            
            // First conversion fails
            val firstResult = service.convertCurrency(100.0, "USD", "EUR")
            assertNull(firstResult)
            
            // Second conversion succeeds
            val secondResult = service.convertCurrency(100.0, "USD", "EUR")
            assertEquals(85.0, secondResult)
        }

        @Test
        @DisplayName("Should handle cache corruption and rebuild")
        fun `should handle cache corruption and rebuild`() = runTest {
            // Setup corrupted cache
            every { realSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns "corrupted_json"
            
            // Setup API response for cache rebuild
            val apiResponse = ExchangeRateResponse(
                result = "success", baseCode = "USD", targetCode = "EUR", conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z", nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            val mockEditor = mockk<SharedPreferences.Editor>(relaxed = true)
            every { realSharedPreferences.edit() } returns mockEditor
            every { mockEditor.putString(any(), any()) } returns mockEditor
            every { mockEditor.apply() } just Runs
            
            // Execute conversion (should rebuild cache)
            val result = service.convertCurrency(100.0, "USD", "EUR")
            assertEquals(85.0, result)
            
            // Verify cache was rebuilt
            verify { mockEditor.putString("exchange_rate_USD_EUR", any()) }
        }
    }
}