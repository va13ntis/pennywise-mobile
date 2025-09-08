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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import retrofit2.Response
import java.util.concurrent.TimeUnit

/**
 * Unit tests for CurrencyConversionService
 * Tests currency conversion logic, caching, API integration, and error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Currency Conversion Service Tests")
class CurrencyConversionServiceTest {

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
    @DisplayName("convertCurrency Method")
    inner class ConvertCurrencyMethod {

        @Test
        @DisplayName("Should return same amount when converting to same currency")
        fun `should return same amount when converting to same currency`() = runTest {
            val result = service.convertCurrency(100.0, "USD", "USD")
            assertEquals(100.0, result)
        }

        @Test
        @DisplayName("Should return same amount when converting to same currency (case insensitive)")
        fun `should return same amount when converting to same currency case insensitive`() = runTest {
            val result = service.convertCurrency(100.0, "USD", "usd")
            assertEquals(100.0, result)
        }

        @Test
        @DisplayName("Should use cached rate when available and not expired")
        fun `should use cached rate when available and not expired`() = runTest {
            // Setup cached rate
            val cachedRate = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1) // 1 hour ago
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":${cachedRate.lastUpdateTime}}"""
            
            val result = service.convertCurrency(100.0, "USD", "EUR")
            assertEquals(85.0, result)
            
            // Verify API was not called
            verify { mockCurrencyApi wasNot Called }
        }

        @Test
        @DisplayName("Should fetch from API when cache is expired")
        fun `should fetch from API when cache is expired`() = runTest {
            // Setup expired cached rate
            val expiredRate = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.80, // Old rate
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25) // 25 hours ago (expired)
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.80,"lastUpdateTime":${expiredRate.lastUpdateTime}}"""
            
            // Setup API response
            val apiResponse = ExchangeRateResponse(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            val result = service.convertCurrency(100.0, "USD", "EUR")
            assertEquals(85.0, result)
            
            // Verify API was called
            coVerify { mockCurrencyApi.getExchangeRate("USD", "EUR") }
        }

        @Test
        @DisplayName("Should use expired cache as fallback when API fails")
        fun `should use expired cache as fallback when API fails`() = runTest {
            // Setup expired cached rate
            val expiredRate = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.80,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            )
            
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.80,"lastUpdateTime":${expiredRate.lastUpdateTime}}"""
            
            // Setup API to throw exception
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws Exception("API Error")
            
            val result = service.convertCurrency(100.0, "USD", "EUR")
            assertEquals(80.0, result) // Should use expired cache
        }

        @Test
        @DisplayName("Should return null when no cache and API fails")
        fun `should return null when no cache and API fails`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws Exception("API Error")
            
            val result = service.convertCurrency(100.0, "USD", "EUR")
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle API response correctly")
        fun `should handle API response correctly`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            val apiResponse = ExchangeRateResponse(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            val result = service.convertCurrency(100.0, "USD", "EUR")
            assertEquals(85.0, result)
            
            // Verify cache was updated
            verify { mockSharedPreferencesEditor.putString("exchange_rate_USD_EUR", any()) }
            verify { mockSharedPreferencesEditor.apply() }
        }

        @ParameterizedTest
        @CsvSource(
            "100.0, USD, EUR, 0.85, 85.0",
            "50.0, EUR, GBP, 0.86, 43.0",
            "200.0, GBP, JPY, 150.0, 30000.0",
            "0.0, USD, EUR, 0.85, 0.0"
        )
        @DisplayName("Should convert amounts correctly with different rates")
        fun `should convert amounts correctly with different rates`(
            amount: Double,
            fromCurrency: String,
            toCurrency: String,
            rate: Double,
            expected: Double
        ) = runTest {
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            val apiResponse = ExchangeRateResponse(
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = rate
            )
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            val result = service.convertCurrency(amount, fromCurrency, toCurrency)
            assertEquals(expected, result, 0.01)
        }
    }

    @Nested
    @DisplayName("Cache Management")
    inner class CacheManagement {

        @Test
        @DisplayName("Should clear cache correctly")
        fun `should clear cache correctly`() {
            every { mockSharedPreferences.all } returns mapOf(
                "exchange_rate_USD_EUR" to "cached_data",
                "timestamp_USD_EUR" to "timestamp_data",
                "other_key" to "other_data"
            )
            
            service.clearCache()
            
            verify { mockSharedPreferencesEditor.remove("exchange_rate_USD_EUR") }
            verify { mockSharedPreferencesEditor.remove("timestamp_USD_EUR") }
            verify { mockSharedPreferencesEditor.remove("other_key") wasNot Called }
            verify { mockSharedPreferencesEditor.apply() }
        }

        @Test
        @DisplayName("Should get cache statistics correctly")
        fun `should get cache statistics correctly`() {
            // Setup cache with valid and expired entries
            val validTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            val expiredTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            
            every { mockSharedPreferences.all } returns mapOf(
                "exchange_rate_USD_EUR" to """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":$validTime}""",
                "exchange_rate_EUR_GBP" to """{"baseCode":"EUR","targetCode":"GBP","conversionRate":0.86,"lastUpdateTime":$expiredTime}""",
                "other_key" to "other_data"
            )
            
            val stats = service.getCacheStats()
            
            assertEquals(2, stats["total_cached"])
            assertEquals(1, stats["valid_cached"])
            assertEquals(1, stats["expired_cached"])
        }

        @Test
        @DisplayName("Should handle malformed cache entries")
        fun `should handle malformed cache entries`() {
            every { mockSharedPreferences.all } returns mapOf(
                "exchange_rate_USD_EUR" to "invalid_json",
                "exchange_rate_EUR_GBP" to """{"baseCode":"EUR","targetCode":"GBP","conversionRate":0.86,"lastUpdateTime":${System.currentTimeMillis()}}"""
            )
            
            val stats = service.getCacheStats()
            
            assertEquals(2, stats["total_cached"])
            assertEquals(1, stats["valid_cached"])
            assertEquals(1, stats["expired_cached"])
        }
    }

    @Nested
    @DisplayName("isConversionAvailable Method")
    inner class IsConversionAvailableMethod {

        @Test
        @DisplayName("Should return true for same currency")
        fun `should return true for same currency`() = runTest {
            val result = service.isConversionAvailable("USD", "USD")
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return true when valid cache exists")
        fun `should return true when valid cache exists`() = runTest {
            val validTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":$validTime}"""
            
            val result = service.isConversionAvailable("USD", "EUR")
            assertTrue(result)
        }

        @Test
        @DisplayName("Should return false when no cache and API fails")
        fun `should return false when no cache and API fails`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws Exception("API Error")
            
            val result = service.isConversionAvailable("USD", "EUR")
            assertFalse(result)
        }

        @Test
        @DisplayName("Should return true when API succeeds")
        fun `should return true when API succeeds`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            val apiResponse = ExchangeRateResponse(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            val result = service.isConversionAvailable("USD", "EUR")
            assertTrue(result)
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("Should handle API timeout gracefully")
        fun `should handle API timeout gracefully`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws 
                java.net.SocketTimeoutException("Timeout")
            
            val result = service.convertCurrency(100.0, "USD", "EUR")
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle network error gracefully")
        fun `should handle network error gracefully`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws 
                java.net.UnknownHostException("Network error")
            
            val result = service.convertCurrency(100.0, "USD", "EUR")
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle malformed API response gracefully")
        fun `should handle malformed API response gracefully`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } throws 
                com.google.gson.JsonSyntaxException("Malformed JSON")
            
            val result = service.convertCurrency(100.0, "USD", "EUR")
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle invalid currency codes gracefully")
        fun `should handle invalid currency codes gracefully`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_XXX_YYY", null) } returns null
            coEvery { mockCurrencyApi.getExchangeRate("XXX", "YYY") } throws 
                retrofit2.HttpException(Response.error<Any>(400, okhttp3.ResponseBody.create(null, "Invalid currency")))
            
            val result = service.convertCurrency(100.0, "XXX", "YYY")
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle zero amount")
        fun `should handle zero amount`() = runTest {
            val result = service.convertCurrency(0.0, "USD", "EUR")
            assertEquals(0.0, result)
        }

        @Test
        @DisplayName("Should handle very large amounts")
        fun `should handle very large amounts`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            val apiResponse = ExchangeRateResponse(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            val result = service.convertCurrency(999999999.99, "USD", "EUR")
            assertEquals(849999999.99, result, 0.01)
        }

        @Test
        @DisplayName("Should handle very small amounts")
        fun `should handle very small amounts`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            val apiResponse = ExchangeRateResponse(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            val result = service.convertCurrency(0.01, "USD", "EUR")
            assertEquals(0.0085, result, 0.0001)
        }

        @Test
        @DisplayName("Should handle negative amounts")
        fun `should handle negative amounts`() = runTest {
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            val apiResponse = ExchangeRateResponse(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns apiResponse
            
            val result = service.convertCurrency(-100.0, "USD", "EUR")
            assertEquals(-85.0, result)
        }
    }

    @Nested
    @DisplayName("CachedExchangeRate Model")
    inner class CachedExchangeRateModel {

        @Test
        @DisplayName("Should correctly identify expired cache")
        fun `should correctly identify expired cache`() {
            val expiredRate = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            )
            
            assertTrue(expiredRate.isExpired())
        }

        @Test
        @DisplayName("Should correctly identify valid cache")
        fun `should correctly identify valid cache`() {
            val validRate = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            )
            
            assertFalse(validRate.isExpired())
        }

        @Test
        @DisplayName("Should handle edge case of exactly 24 hours")
        fun `should handle edge case of exactly 24 hours`() {
            val edgeRate = CachedExchangeRate(
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)
            )
            
            assertTrue(edgeRate.isExpired())
        }
    }
}
