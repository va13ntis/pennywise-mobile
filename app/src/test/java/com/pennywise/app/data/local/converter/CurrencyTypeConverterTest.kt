package com.pennywise.app.data.local.converter

import com.pennywise.app.domain.model.Currency
import com.pennywise.app.data.local.converter.CurrencyTypeConverter
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import io.mockk.junit5.MockKExtension

/**
 * Unit tests for Currency type converters
 * Tests conversion between Currency enum and database string representation
 */
@ExtendWith(MockKExtension::class)
@DisplayName("Currency Type Converter Tests")
class CurrencyTypeConverterTest {

    private lateinit var converter: CurrencyTypeConverter

    @BeforeEach
    fun setUp() {
        converter = CurrencyTypeConverter()
    }

    @Nested
    @DisplayName("fromCurrency Method")
    inner class FromCurrencyMethod {

        @Test
        @DisplayName("Should convert USD currency to string")
        fun `should convert USD currency to string`() {
            // When
            val result = converter.fromCurrency(Currency.USD)
            
            // Then
            assertEquals("USD", result)
        }

        @Test
        @DisplayName("Should convert EUR currency to string")
        fun `should convert EUR currency to string`() {
            // When
            val result = converter.fromCurrency(Currency.EUR)
            
            // Then
            assertEquals("EUR", result)
        }

        @Test
        @DisplayName("Should convert JPY currency to string")
        fun `should convert JPY currency to string`() {
            // When
            val result = converter.fromCurrency(Currency.JPY)
            
            // Then
            assertEquals("JPY", result)
        }

        @Test
        @DisplayName("Should convert all currency enums to their codes")
        fun `should convert all currency enums to their codes`() {
            // When & Then
            Currency.values().forEach { currency ->
                val result = converter.fromCurrency(currency)
                assertEquals(currency.code, result)
            }
        }

        @Test
        @DisplayName("Should handle null currency")
        fun `should handle null currency`() {
            // When
            val result = converter.fromCurrency(null)
            
            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("toCurrency Method")
    inner class ToCurrencyMethod {

        @ParameterizedTest
        @ValueSource(strings = ["USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY"])
        @DisplayName("Should convert valid currency codes to Currency enum")
        fun `should convert valid currency codes to Currency enum`(currencyCode: String) {
            // When
            val result = converter.toCurrency(currencyCode)
            
            // Then
            assertNotNull(result)
            assertEquals(currencyCode, result?.code)
        }

        @Test
        @DisplayName("Should return null for invalid currency code")
        fun `should return null for invalid currency code`() {
            // When
            val result = converter.toCurrency("INVALID")
            
            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should return null for empty string")
        fun `should return null for empty string`() {
            // When
            val result = converter.toCurrency("")
            
            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should return null for null input")
        fun `should return null for null input`() {
            // When
            val result = converter.toCurrency(null)
            
            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should be case sensitive")
        fun `should be case sensitive`() {
            // When
            val result = converter.toCurrency("usd")
            
            // Then
            assertNull(result) // Should be case sensitive
        }
    }

    @Nested
    @DisplayName("Round Trip Conversion")
    inner class RoundTripConversion {

        @Test
        @DisplayName("Should maintain consistency in round trip conversion")
        fun `should maintain consistency in round trip conversion`() {
            // Given
            val originalCurrency = Currency.USD
            
            // When
            val stringValue = converter.fromCurrency(originalCurrency)
            val convertedBack = converter.toCurrency(stringValue)
            
            // Then
            assertEquals(originalCurrency, convertedBack)
        }

        @Test
        @DisplayName("Should maintain consistency for all currencies")
        fun `should maintain consistency for all currencies`() {
            // When & Then
            Currency.values().forEach { originalCurrency ->
                val stringValue = converter.fromCurrency(originalCurrency)
                val convertedBack = converter.toCurrency(stringValue)
                assertEquals(originalCurrency, convertedBack)
            }
        }

        @Test
        @DisplayName("Should handle null round trip")
        fun `should handle null round trip`() {
            // When
            val stringValue = converter.fromCurrency(null)
            val convertedBack = converter.toCurrency(stringValue)
            
            // Then
            assertNull(stringValue)
            assertNull(convertedBack)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle special characters in currency codes")
        fun `should handle special characters in currency codes`() {
            // When
            val result = converter.toCurrency("US$")
            
            // Then
            assertNull(result) // Should not match any valid currency
        }

        @Test
        @DisplayName("Should handle very long strings")
        fun `should handle very long strings`() {
            // When
            val result = converter.toCurrency("USD".repeat(100))
            
            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle numeric strings")
        fun `should handle numeric strings`() {
            // When
            val result = converter.toCurrency("123")
            
            // Then
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle whitespace strings")
        fun `should handle whitespace strings`() {
            // When
            val result = converter.toCurrency("   ")
            
            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should handle multiple conversions efficiently")
        fun `should handle multiple conversions efficiently`() {
            // Given
            val currencies = Currency.values()
            val startTime = System.currentTimeMillis()
            
            // When
            repeat(1000) {
                currencies.forEach { currency ->
                    val stringValue = converter.fromCurrency(currency)
                    val convertedBack = converter.toCurrency(stringValue)
                    assertEquals(currency, convertedBack)
                }
            }
            
            // Then
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime
            
            // Should complete within reasonable time (less than 1 second)
            assertTrue(duration < 1000, "Conversion should be fast, took ${duration}ms")
        }
    }

    @Nested
    @DisplayName("Integration with Currency Enum")
    inner class IntegrationWithCurrencyEnum {

        @Test
        @DisplayName("Should work with all currency enum values")
        fun `should work with all currency enum values`() {
            // When & Then
            Currency.values().forEach { currency ->
                // Test conversion to string
                val stringValue = converter.fromCurrency(currency)
                assertNotNull(stringValue)
                assertEquals(currency.code, stringValue)
                
                // Test conversion back to enum
                val convertedBack = converter.toCurrency(stringValue)
                assertNotNull(convertedBack)
                assertEquals(currency, convertedBack)
            }
        }

        @Test
        @DisplayName("Should maintain enum properties after conversion")
        fun `should maintain enum properties after conversion`() {
            // Given
            val originalCurrency = Currency.EUR
            
            // When
            val stringValue = converter.fromCurrency(originalCurrency)
            val convertedBack = converter.toCurrency(stringValue)
            
            // Then
            assertEquals(originalCurrency.code, convertedBack?.code)
            assertEquals(originalCurrency.symbol, convertedBack?.symbol)
            assertEquals(originalCurrency.displayName, convertedBack?.displayName)
            assertEquals(originalCurrency.popularity, convertedBack?.popularity)
            assertEquals(originalCurrency.decimalPlaces, convertedBack?.decimalPlaces)
        }
    }
}
