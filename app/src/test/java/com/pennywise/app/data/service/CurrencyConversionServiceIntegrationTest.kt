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
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import retrofit2.Response
import java.util.concurrent.TimeUnit

/**
 * Integration tests for CurrencyConversionService
 * Tests service integration with caching, API, and error handling scenarios
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Currency Conversion Service Integration Tests")
class CurrencyConversionServiceIntegrationTest {

    private lateinit var service: CurrencyConversionService
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockSharedPreferencesEditor: SharedPreferences.Editor
    private lateinit var mockCurrencyApi: CurrencyApi
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        mockContext = mockk<Context>(relaxed = true)
        mockSharedPreferences = mockk<SharedPreferences>(relaxed = true)
        mockSharedPreferencesEditor = mockk<SharedPreferences.Editor>(relaxed = true)
        mockCurrencyApi = mockk<CurrencyApi>(relaxed = true)

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putString(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.apply() } just Runs

        service = CurrencyConversionService(mockContext)
        
        // Use reflection to inject the mock API for testing
        val apiField = service.javaClass.getDeclaredField("currencyApi")
        apiField.isAccessible = true
        apiField.set(service, mockCurrencyApi)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Nested
    @DisplayName("Currency Conversion Flow Integration")
    inner class CurrencyConversionFlowIntegration {

        @Test
        @DisplayName("Should handle complete conversion flow with caching")
        fun `should handle complete conversion flow with caching`() = runTest {
            // Given
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val apiResponse = ExchangeRateResponse(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85
            )
            
            // No cache initially
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // When
            val result = service.convertCurrency(amount, fromCurrency, toCurrency)
            
            // Then
            assertEquals(85.0, result)
            
            // Verify API was called
            coVerify { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) }
            
            // Verify cache was updated
            verify { mockSharedPreferencesEditor.putString("exchange_rate_USD_EUR", any()) }
            verify { mockSharedPreferencesEditor.apply() }
        }

        @Test
        @DisplayName("Should use cached data on subsequent calls")
        fun `should use cached data on subsequent calls`() = runTest {
            // Given
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val cachedRate = CachedExchangeRate(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":${cachedRate.lastUpdateTime}}"""
            
            // When
            val result = service.convertCurrency(amount, fromCurrency, toCurrency)
            
            // Then
            assertEquals(85.0, result)
            
            // Verify API was NOT called
            coVerify { mockCurrencyApi wasNot Called }
        }

        @Test
        @DisplayName("Should handle API failure with expired cache fallback")
        fun `should handle API failure with expired cache fallback`() = runTest {
            // Given
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val expiredRate = CachedExchangeRate(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.80, // Old rate
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.80,"lastUpdateTime":${expiredRate.lastUpdateTime}}"""
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } throws 
                Exception("API Error")
            
            // When
            val result = service.convertCurrency(amount, fromCurrency, toCurrency)
            
            // Then
            assertEquals(80.0, result) // Should use expired cache as fallback
        }

        @Test
        @DisplayName("Should handle complete failure scenario")
        fun `should handle complete failure scenario`() = runTest {
            // Given
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } throws 
                Exception("API Error")
            
            // When
            val result = service.convertCurrency(amount, fromCurrency, toCurrency)
            
            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("Cache Management Integration")
    inner class CacheManagementIntegration {

        @Test
        @DisplayName("Should manage cache lifecycle correctly")
        fun `should manage cache lifecycle correctly`() = runTest {
            // Given
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val apiResponse = ExchangeRateResponse(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85
            )
            
            // First call - no cache
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // When - First conversion
            val result1 = service.convertCurrency(100.0, fromCurrency, toCurrency)
            
            // Then
            assertEquals(85.0, result1)
            verify { mockSharedPreferencesEditor.putString("exchange_rate_USD_EUR", any()) }
            
            // Second call - use cache
            val cachedRate = CachedExchangeRate(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            )
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":${cachedRate.lastUpdateTime}}"""
            
            // When - Second conversion
            val result2 = service.convertCurrency(200.0, fromCurrency, toCurrency)
            
            // Then
            assertEquals(170.0, result2)
            coVerify(exactly = 1) { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) }
        }

        @Test
        @DisplayName("Should clear cache correctly")
        fun `should clear cache correctly`() = runTest {
            // Given
            every { mockSharedPreferences.all } returns mapOf(
                "exchange_rate_USD_EUR" to "cached_data",
                "exchange_rate_EUR_GBP" to "cached_data",
                "other_key" to "other_data"
            )
            
            // When
            service.clearCache()
            
            // Then
            verify { mockSharedPreferencesEditor.remove("exchange_rate_USD_EUR") }
            verify { mockSharedPreferencesEditor.remove("exchange_rate_EUR_GBP") }
            verify { mockSharedPreferencesEditor.remove("other_key") wasNot Called }
            verify { mockSharedPreferencesEditor.apply() }
        }

        @Test
        @DisplayName("Should provide accurate cache statistics")
        fun `should provide accurate cache statistics`() = runTest {
            // Given
            val validTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            val expiredTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            
            every { mockSharedPreferences.all } returns mapOf(
                "exchange_rate_USD_EUR" to """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":$validTime}""",
                "exchange_rate_EUR_GBP" to """{"baseCode":"EUR","targetCode":"GBP","conversionRate":0.86,"lastUpdateTime":$expiredTime}""",
                "exchange_rate_GBP_JPY" to "invalid_json",
                "other_key" to "other_data"
            )
            
            // When
            val stats = service.getCacheStats()
            
            // Then
            assertEquals(3, stats["total_cached"])
            assertEquals(1, stats["valid_cached"])
            assertEquals(2, stats["expired_cached"])
        }
    }

    @Nested
    @DisplayName("API Integration Scenarios")
    inner class ApiIntegrationScenarios {

        @Test
        @DisplayName("Should handle successful API response")
        fun `should handle successful API response`() = runTest {
            // Given
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val apiResponse = ExchangeRateResponse(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // When
            val result = service.convertCurrency(100.0, fromCurrency, toCurrency)
            
            // Then
            assertEquals(85.0, result)
            coVerify { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) }
        }

        @Test
        @DisplayName("Should handle API timeout")
        fun `should handle API timeout`() = runTest {
            // Given
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } throws 
                java.net.SocketTimeoutException("Timeout")
            
            // When
            val result = service.convertCurrency(100.0, fromCurrency, toCurrency)
            
            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle network error")
        fun `should handle network error`() = runTest {
            // Given
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } throws 
                java.net.UnknownHostException("Network error")
            
            // When
            val result = service.convertCurrency(100.0, fromCurrency, toCurrency)
            
            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle invalid currency codes")
        fun `should handle invalid currency codes`() = runTest {
            // Given
            val fromCurrency = "XXX"
            val toCurrency = "YYY"
            
            every { mockSharedPreferences.getString("exchange_rate_XXX_YYY", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } throws 
                retrofit2.HttpException(Response.error<Any>(400, okhttp3.ResponseBody.create(null, "Invalid currency")))
            
            // When
            val result = service.convertCurrency(100.0, fromCurrency, toCurrency)
            
            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("Availability Check Integration")
    inner class AvailabilityCheckIntegration {

        @Test
        @DisplayName("Should check availability with valid cache")
        fun `should check availability with valid cache`() = runTest {
            // Given
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val validTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":$validTime}"""
            
            // When
            val isAvailable = service.isConversionAvailable(fromCurrency, toCurrency)
            
            // Then
            assertTrue(isAvailable)
            coVerify { mockCurrencyApi wasNot Called }
        }

        @Test
        @DisplayName("Should check availability with API when no cache")
        fun `should check availability with API when no cache`() = runTest {
            // Given
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val apiResponse = ExchangeRateResponse(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // When
            val isAvailable = service.isConversionAvailable(fromCurrency, toCurrency)
            
            // Then
            assertTrue(isAvailable)
            coVerify { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) }
        }

        @Test
        @DisplayName("Should return false when API fails and no cache")
        fun `should return false when API fails and no cache`() = runTest {
            // Given
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } throws 
                Exception("API Error")
            
            // When
            val isAvailable = service.isConversionAvailable(fromCurrency, toCurrency)
            
            // Then
            assertFalse(isAvailable)
        }
    }

    @Nested
    @DisplayName("Edge Cases Integration")
    inner class EdgeCasesIntegration {

        @Test
        @DisplayName("Should handle same currency conversion")
        fun `should handle same currency conversion`() = runTest {
            // When
            val result = service.convertCurrency(100.0, "USD", "USD")
            
            // Then
            assertEquals(100.0, result)
            coVerify { mockCurrencyApi wasNot Called }
            verify { mockSharedPreferences wasNot Called }
        }

        @Test
        @DisplayName("Should handle zero amount")
        fun `should handle zero amount`() = runTest {
            // Given
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val apiResponse = ExchangeRateResponse(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // When
            val result = service.convertCurrency(0.0, fromCurrency, toCurrency)
            
            // Then
            assertEquals(0.0, result)
        }

        @Test
        @DisplayName("Should handle very large amounts")
        fun `should handle very large amounts`() = runTest {
            // Given
            val amount = 999999999.99
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val apiResponse = ExchangeRateResponse(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // When
            val result = service.convertCurrency(amount, fromCurrency, toCurrency)
            
            // Then
            assertEquals(849999999.99, result, 0.01)
        }

        @Test
        @DisplayName("Should handle negative amounts")
        fun `should handle negative amounts`() = runTest {
            // Given
            val amount = -100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val apiResponse = ExchangeRateResponse(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // When
            val result = service.convertCurrency(amount, fromCurrency, toCurrency)
            
            // Then
            assertEquals(-85.0, result)
        }
    }

    @Nested
    @DisplayName("Performance Integration")
    inner class PerformanceIntegration {

        @Test
        @DisplayName("Should handle multiple concurrent conversions")
        fun `should handle multiple concurrent conversions`() = runTest {
            // Given
            val apiResponse = ExchangeRateResponse(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            // When
            val results = (1..10).map { amount ->
                service.convertCurrency(amount.toDouble(), "USD", "EUR")
            }
            
            // Then
            assertEquals(10, results.size)
            results.forEachIndexed { index, result ->
                assertEquals((index + 1) * 0.85, result, 0.01)
            }
            
            // API should be called only once due to caching
            coVerify(exactly = 1) { mockCurrencyApi.getExchangeRate("USD", "EUR") }
        }

        @Test
        @DisplayName("Should handle cache expiration efficiently")
        fun `should handle cache expiration efficiently`() = runTest {
            // Given
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val expiredTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            val apiResponse = ExchangeRateResponse(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = 0.85
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.80,"lastUpdateTime":$expiredTime}"""
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // When
            val result = service.convertCurrency(100.0, fromCurrency, toCurrency)
            
            // Then
            assertEquals(85.0, result) // Should use new API rate, not expired cache
            coVerify { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) }
        }
    }
}
