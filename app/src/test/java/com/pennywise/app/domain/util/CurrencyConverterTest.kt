package com.pennywise.app.domain.util

import android.content.Context
import android.content.SharedPreferences
import com.pennywise.app.data.api.CurrencyApi
import com.pennywise.app.data.model.CachedExchangeRate
import com.pennywise.app.data.model.ExchangeRateResponse
import com.pennywise.app.data.service.CurrencyConversionService
import com.pennywise.app.domain.model.Currency
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
import java.util.concurrent.TimeUnit

/**
 * Comprehensive unit tests for currency conversion functionality
 * Tests both direct currency conversion methods and utility functions
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Currency Converter Tests")
class CurrencyConverterTest {

    private lateinit var conversionService: CurrencyConversionService
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
        mockCurrencyApi = mockk<CurrencyApi>()

        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.putString(any(), any()) } returns mockSharedPreferencesEditor
        every { mockSharedPreferencesEditor.apply() } just Runs

        conversionService = CurrencyConversionService(mockContext)
        
        // Use reflection to inject the mock API for testing
        val apiField = conversionService.javaClass.getDeclaredField("currencyApi")
        apiField.isAccessible = true
        apiField.set(conversionService, mockCurrencyApi)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Nested
    @DisplayName("Currency Pair Conversion Tests")
    inner class CurrencyPairConversionTests {

        @ParameterizedTest
        @CsvSource(
            "100.0, USD, EUR, 0.85, 85.0",
            "50.0, EUR, GBP, 0.86, 43.0",
            "200.0, GBP, JPY, 150.0, 30000.0",
            "0.0, USD, EUR, 0.85, 0.0"
        )
        @DisplayName("Should convert amounts correctly between currency pairs")
        fun `should convert amounts correctly between currency pairs`(
            amount: Double,
            fromCurrency: String,
            toCurrency: String,
            rate: Double,
            expected: Double
        ) = runTest {
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            val apiResponse = ExchangeRateResponse(
                result = "success",
                baseCode = fromCurrency,
                targetCode = toCurrency,
                conversionRate = rate,
                lastUpdateTime = "2023-01-01 00:00:00",
                nextUpdateTime = "2023-01-02 00:00:00"
            )
            coEvery { mockCurrencyApi.getExchangeRate(fromCurrency, toCurrency) } returns apiResponse
            
            // When
            val result = conversionService.convertCurrency(amount, fromCurrency, toCurrency)
            
            // Then
            assertEquals(expected, result ?: 0.0, 0.01)
        }
        
        @Test
        @DisplayName("Should handle reciprocal rates correctly")
        fun `should handle reciprocal rates correctly`() = runTest {
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            // EUR to USD (1.18)
            val euroToUsdResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "EUR",
                targetCode = "USD",
                conversionRate = 1.18,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            // USD to EUR (0.85)
            val usdToEuroResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            coEvery { mockCurrencyApi.getExchangeRate("EUR", "USD") } returns euroToUsdResponse
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns usdToEuroResponse
            
            // When - Convert EUR to USD
            val euroToUsd = conversionService.convertCurrency(100.0, "EUR", "USD")
            
            // Then - Should be around 118 USD
            assertEquals(118.0, euroToUsd ?: 0.0, 0.01)
            
            // When - Convert back USD to EUR
            val usdToEuro = conversionService.convertCurrency(118.0, "USD", "EUR")
            
            // Then - Should be around 100 EUR
            assertEquals(100.3, usdToEuro ?: 0.0, 0.1)
        }
    }

    @Nested
    @DisplayName("Multi-Currency Conversion Tests")
    inner class MultiCurrencyConversionTests {

        @Test
        @DisplayName("Should handle triangular currency conversion")
        fun `should handle triangular currency conversion`() = runTest {
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            // USD -> EUR at 0.85
            val usdToEurResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            // EUR -> GBP at 0.86
            val eurToGbpResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "EUR",
                targetCode = "GBP",
                conversionRate = 0.86,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns usdToEurResponse
            coEvery { mockCurrencyApi.getExchangeRate("EUR", "GBP") } returns eurToGbpResponse
            
            // When - Convert USD to EUR
            val usdToEur = conversionService.convertCurrency(100.0, "USD", "EUR")
            
            // Then - Should be 85 EUR
            assertEquals(85.0, usdToEur ?: 0.0, 0.01)
            
            // When - Convert the resulting EUR to GBP
            val eurToGbp = conversionService.convertCurrency(usdToEur!!, "EUR", "GBP")
            
            // Then - Should be 73.1 GBP (85 * 0.86)
            assertEquals(73.1, eurToGbp ?: 0.0, 0.01)
            
            // The effective USD to GBP rate should be approximately 0.731 (0.85 * 0.86)
            val effectiveRate = eurToGbp!! / 100.0
            assertEquals(0.731, effectiveRate, 0.001)
        }

        @Test
        @DisplayName("Should convert between multiple currencies in sequence")
        fun `should convert between multiple currencies in sequence`() = runTest {
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            // Define several exchange rates
            val rates = mapOf(
                "USD_EUR" to 0.85,
                "EUR_GBP" to 0.86,
                "GBP_JPY" to 150.0,
                "JPY_AUD" to 0.012
            )
            
            // Setup mock responses for each pair
            rates.forEach { (pair, rate) ->
                val (from, to) = pair.split("_")
                val response = ExchangeRateResponse(
                    result = "success",
                    baseCode = from,
                    targetCode = to,
                    conversionRate = rate,
                    lastUpdateTime = "2023-01-01T00:00:00Z",
                    nextUpdateTime = "2023-01-02T00:00:00Z"
                )
                coEvery { mockCurrencyApi.getExchangeRate(from, to) } returns response
            }
            
            // When/Then - Convert through a chain of currencies
            // Start with 1000 USD
            var amount = 1000.0
            
            // USD to EUR
            amount = conversionService.convertCurrency(amount, "USD", "EUR")!!
            assertEquals(850.0, amount, 0.01) // 1000 * 0.85
            
            // EUR to GBP
            amount = conversionService.convertCurrency(amount, "EUR", "GBP")!!
            assertEquals(731.0, amount, 0.01) // 850 * 0.86
            
            // GBP to JPY
            amount = conversionService.convertCurrency(amount, "GBP", "JPY")!!
            assertEquals(109650.0, amount, 0.01) // 731 * 150
            
            // JPY to AUD
            amount = conversionService.convertCurrency(amount, "JPY", "AUD")!!
            assertEquals(1315.8, amount, 0.1) // 109650 * 0.012
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle fractional exchange rates")
        fun `should handle fractional exchange rates`() = runTest {
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            // Very small exchange rate (e.g., VND to USD)
            val smallRateResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "VND",
                targetCode = "USD",
                conversionRate = 0.000043,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            coEvery { mockCurrencyApi.getExchangeRate("VND", "USD") } returns smallRateResponse
            
            // When - Convert 1,000,000 VND to USD
            val result = conversionService.convertCurrency(1000000.0, "VND", "USD")
            
            // Then - Should be around 43 USD
            assertEquals(43.0, result ?: 0.0, 0.01)
        }
        
        @Test
        @DisplayName("Should handle very large exchange rates")
        fun `should handle very large exchange rates`() = runTest {
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            // Very large exchange rate (e.g., USD to VND)
            val largeRateResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "VND",
                conversionRate = 23255.814,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "VND") } returns largeRateResponse
            
            // When - Convert 100 USD to VND
            val result = conversionService.convertCurrency(100.0, "USD", "VND")
            
            // Then - Should be around 2,325,581.4 VND
            assertEquals(2325581.4, result ?: 0.0, 0.1)
        }
        
        @Test
        @DisplayName("Should handle extreme input values")
        fun `should handle extreme input values`() = runTest {
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            val rateResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns rateResponse
            
            // When/Then - Test with extremely large value
            val largeResult = conversionService.convertCurrency(1e15, "USD", "EUR")
            assertEquals(8.5e14, largeResult ?: 0.0, 0.1e14)
            
            // When/Then - Test with extremely small positive value
            val smallResult = conversionService.convertCurrency(1e-10, "USD", "EUR")
            assertEquals(8.5e-11, smallResult ?: 0.0, 0.1e-11)
        }
    }

    @Nested
    @DisplayName("Rate Stability and Consistency")
    inner class RateStabilityAndConsistency {

        @Test
        @DisplayName("Should maintain consistent rates during a session")
        fun `should maintain consistent rates during a session`() = runTest {
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            val rateResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns rateResponse
            
            // When - Convert multiple times
            val results = List(5) { 
                conversionService.convertCurrency(100.0, "USD", "EUR") 
            }
            
            // Then - All results should be the same
            results.forEach { result ->
                assertEquals(85.0, result ?: 0.0, 0.01)
            }
            
            // API should be called only once due to caching
            coVerify(exactly = 1) { mockCurrencyApi.getExchangeRate("USD", "EUR") }
        }

        @Test
        @DisplayName("Should handle rate changes appropriately")
        fun `should handle rate changes appropriately`() = runTest {
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            // First rate response
            val initialRateResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            // Changed rate response (after cache expiry)
            val updatedRateResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.88,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            // First call returns initial rate
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns initialRateResponse
            
            // When/Then - First conversion
            val initialResult = conversionService.convertCurrency(100.0, "USD", "EUR")
            assertEquals(85.0, initialResult ?: 0.0, 0.01)
            
            // Given - Now mock the cached rate as expired
            val expiredTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns 
                """{"baseCode":"USD","targetCode":"EUR","conversionRate":0.85,"lastUpdateTime":$expiredTime}"""
            
            // And API now returns updated rate
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns updatedRateResponse
            
            // When - Convert again after cache expiry
            val updatedResult = conversionService.convertCurrency(100.0, "USD", "EUR")
            
            // Then - Should use the new rate
            assertEquals(88.0, updatedResult ?: 0.0, 0.01)
        }
    }

    @Nested
    @DisplayName("Currency Cross-Rate Tests")
    inner class CurrencyCrossRateTests {

        @Test
        @DisplayName("Should accurately calculate cross rates")
        fun `should accurately calculate cross rates`() = runTest {
            // This test verifies that converting through an intermediate currency
            // produces results that are approximately equivalent to a direct conversion
            
            // Given
            every { mockSharedPreferences.getString(any(), null) } returns null
            
            // Define direct rate for USD->GBP
            val directRateResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "GBP",
                conversionRate = 0.73,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            // Define rates for USD->EUR and EUR->GBP
            val usdToEurResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            val eurToGbpResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "EUR",
                targetCode = "GBP",
                conversionRate = 0.86,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            
            // Setup mock responses
            coEvery { mockCurrencyApi.getExchangeRate("USD", "GBP") } returns directRateResponse
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns usdToEurResponse
            coEvery { mockCurrencyApi.getExchangeRate("EUR", "GBP") } returns eurToGbpResponse
            
            // When - Convert directly
            val directResult = conversionService.convertCurrency(100.0, "USD", "GBP")
            
            // Then - Should be 73 GBP
            assertEquals(73.0, directResult ?: 0.0, 0.01)
            
            // When - Convert through EUR
            val intermediateResult = conversionService.convertCurrency(100.0, "USD", "EUR")!!
            val crossRateResult = conversionService.convertCurrency(intermediateResult, "EUR", "GBP")
            
            // Then - Cross rate result (100 USD -> 85 EUR -> 73.1 GBP) should be close to direct result
            // The implied rate is 0.85 * 0.86 = 0.731, which is very close to the direct rate of 0.73
            assertEquals(73.1, crossRateResult ?: 0.0, 0.1)
            assertEquals(directResult!!, crossRateResult!!, 0.5) // Allow small difference due to cross-rate calculation
        }
    }

    @Nested
    @DisplayName("Currency Conversion Performance")
    inner class CurrencyConversionPerformance {

        @Test
        @DisplayName("Should handle batch conversions efficiently")
        fun `should handle batch conversions efficiently`() = runTest {
            // This test verifies that multiple conversions for the same currency pair
            // are handled efficiently using caching
            
            // Given
            every { mockSharedPreferences.getString("exchange_rate_USD_EUR", null) } returns null
            
            val rateResponse = ExchangeRateResponse(
                result = "success",
                baseCode = "USD",
                targetCode = "EUR",
                conversionRate = 0.85,
                lastUpdateTime = "2023-01-01T00:00:00Z",
                nextUpdateTime = "2023-01-02T00:00:00Z"
            )
            coEvery { mockCurrencyApi.getExchangeRate("USD", "EUR") } returns rateResponse
            
            // When - Convert multiple amounts in sequence
            val amounts = listOf(100.0, 200.0, 500.0, 1000.0, 5000.0)
            val results = amounts.map { 
                conversionService.convertCurrency(it, "USD", "EUR") 
            }
            
            // Then - Results should match expected conversions
            val expectedResults = amounts.map { it * 0.85 }
            for (i in results.indices) {
                assertEquals(expectedResults[i], results[i] ?: 0.0, 0.01)
            }
            
            // API should be called only once due to caching
            coVerify(exactly = 1) { mockCurrencyApi.getExchangeRate("USD", "EUR") }
        }
    }
}

