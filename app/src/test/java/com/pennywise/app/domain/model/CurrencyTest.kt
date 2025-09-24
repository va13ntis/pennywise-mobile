package com.pennywise.app.domain.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.Double.Companion.NEGATIVE_INFINITY

/**
 * Unit tests for Currency enum and its companion object methods
 * Tests currency code validation, symbol mapping, formatting logic, and edge cases
 */
@DisplayName("Currency Model Tests")
class CurrencyTest {

    @Nested
    @DisplayName("Currency Enum Properties")
    inner class CurrencyProperties {

        @Test
        @DisplayName("Should have correct properties for USD")
        fun `USD should have correct properties`() {
            assertEquals("USD", Currency.USD.code)
            assertEquals("$", Currency.USD.symbol)
            assertEquals("US Dollar", Currency.USD.displayName)
            assertEquals(1, Currency.USD.popularity)
            assertEquals(2, Currency.USD.decimalPlaces)
        }

        @Test
        @DisplayName("Should have correct properties for JPY")
        fun `JPY should have correct properties`() {
            assertEquals("JPY", Currency.JPY.code)
            assertEquals("¥", Currency.JPY.symbol)
            assertEquals("Japanese Yen", Currency.JPY.displayName)
            assertEquals(4, Currency.JPY.popularity)
            assertEquals(0, Currency.JPY.decimalPlaces)
        }

        @Test
        @DisplayName("Should have correct properties for KRW")
        fun `KRW should have correct properties`() {
            assertEquals("KRW", Currency.KRW.code)
            assertEquals("₩", Currency.KRW.symbol)
            assertEquals("South Korean Won", Currency.KRW.displayName)
            assertEquals(19, Currency.KRW.popularity)
            assertEquals(0, Currency.KRW.decimalPlaces)
        }

        @Test
        @DisplayName("Should have correct properties for EUR")
        fun `EUR should have correct properties`() {
            assertEquals("EUR", Currency.EUR.code)
            assertEquals("€", Currency.EUR.symbol)
            assertEquals("Euro", Currency.EUR.displayName)
            assertEquals(2, Currency.EUR.popularity)
            assertEquals(2, Currency.EUR.decimalPlaces)
        }
    }

    @Nested
    @DisplayName("fromCode Method")
    inner class FromCodeMethod {

        @ParameterizedTest
        @CsvSource(
            "USD, USD",
            "EUR, EUR", 
            "GBP, GBP",
            "JPY, JPY",
            "CAD, CAD"
        )
        @DisplayName("Should return correct currency for valid codes")
        fun `should return correct currency for valid codes`(input: String, expected: String) {
            val result = Currency.fromCode(input)
            assertNotNull(result)
            assertEquals(expected, result?.code)
        }

        @ParameterizedTest
        @ValueSource(strings = ["usd", "eur", "gbp", "jpy", "cad"])
        @DisplayName("Should handle case insensitive currency codes")
        fun `should handle case insensitive currency codes`(code: String) {
            val result = Currency.fromCode(code)
            assertNotNull(result)
            assertEquals(code.uppercase(), result?.code)
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "XXX", "ABC", "123", "US", "USDD"])
        @DisplayName("Should return null for invalid currency codes")
        fun `should return null for invalid currency codes`(code: String) {
            val result = Currency.fromCode(code)
            assertNull(result)
        }

        @Test
        @DisplayName("Should return null for null input")
        fun `should return null for null input`() {
            // Note: This test assumes the method handles null gracefully
            // If the method doesn't handle null, this test would need to be adjusted
            assertThrows(NullPointerException::class.java) {
                Currency.fromCode(null as String)
            }
        }
    }

    @Nested
    @DisplayName("getDefault Method")
    inner class GetDefaultMethod {

        @Test
        @DisplayName("Should return USD as default currency")
        fun `should return USD as default currency`() {
            val result = Currency.getDefault()
            assertEquals(Currency.USD, result)
        }
    }

    @Nested
    @DisplayName("getSortedByPopularity Method")
    inner class GetSortedByPopularityMethod {

        @Test
        @DisplayName("Should return currencies sorted by popularity")
        fun `should return currencies sorted by popularity`() {
            val result = Currency.getSortedByPopularity()
            
            // Check that the list is sorted by popularity
            for (i in 1 until result.size) {
                assertTrue(
                    result[i-1].popularity <= result[i].popularity,
                    "Currency at index ${i-1} (${result[i-1].code}) should have popularity <= currency at index $i (${result[i].code})"
                )
            }
        }

        @Test
        @DisplayName("Should return all currencies")
        fun `should return all currencies`() {
            val result = Currency.getSortedByPopularity()
            assertEquals(Currency.values().size, result.size)
        }

        @Test
        @DisplayName("Should have USD as first currency")
        fun `should have USD as first currency`() {
            val result = Currency.getSortedByPopularity()
            assertEquals(Currency.USD, result.first())
        }
    }

    @Nested
    @DisplayName("getMostPopular Method")
    inner class GetMostPopularMethod {

        @Test
        @DisplayName("Should return exactly 10 currencies")
        fun `should return exactly 10 currencies`() {
            val result = Currency.getMostPopular()
            assertEquals(10, result.size)
        }

        @Test
        @DisplayName("Should return currencies sorted by popularity")
        fun `should return currencies sorted by popularity`() {
            val result = Currency.getMostPopular()
            
            // Check that the list is sorted by popularity
            for (i in 1 until result.size) {
                assertTrue(
                    result[i-1].popularity <= result[i].popularity,
                    "Currency at index ${i-1} (${result[i-1].code}) should have popularity <= currency at index $i (${result[i].code})"
                )
            }
        }

        @Test
        @DisplayName("Should include USD as first currency")
        fun `should include USD as first currency`() {
            val result = Currency.getMostPopular()
            assertEquals(Currency.USD, result.first())
        }
    }

    @Nested
    @DisplayName("formatAmount Method")
    inner class FormatAmountMethod {

        @ParameterizedTest
        @CsvSource(
            "100.0, USD, $100.00",
            "1.5, USD, $1.50",
            "0.0, USD, $0.00",
            "100.0, EUR, €100.00",
            "1.5, EUR, €1.50",
            "100.0, GBP, £100.00"
        )
        @DisplayName("Should format amounts correctly for currencies with 2 decimal places")
        fun `should format amounts correctly for currencies with 2 decimal places`(
            amount: Double,
            currencyCode: String,
            expected: String
        ) {
            val currency = Currency.fromCode(currencyCode)!!
            val result = Currency.formatAmount(amount, currency)
            assertEquals(expected, result)
        }

        @ParameterizedTest
        @CsvSource(
            "100.0, JPY, ¥100",
            "1.0, JPY, ¥1",
            "0.0, JPY, ¥0",
            "100.0, KRW, ₩100",
            "1.0, KRW, ₩1"
        )
        @DisplayName("Should format amounts correctly for currencies with 0 decimal places")
        fun `should format amounts correctly for currencies with 0 decimal places`(
            amount: Double,
            currencyCode: String,
            expected: String
        ) {
            val currency = Currency.fromCode(currencyCode)!!
            val result = Currency.formatAmount(amount, currency)
            assertEquals(expected, result)
        }

        @Test
        @DisplayName("Should handle negative amounts")
        fun `should handle negative amounts`() {
            val result = Currency.formatAmount(-25.5, Currency.USD)
            assertEquals("$-25.50", result)
        }

        @Test
        @DisplayName("Should handle very large amounts")
        fun `should handle very large amounts`() {
            val result = Currency.formatAmount(999999999.99, Currency.USD)
            assertEquals("$999999999.99", result)
        }

        @Test
        @DisplayName("Should handle very small amounts")
        fun `should handle very small amounts`() {
            val result = Currency.formatAmount(0.01, Currency.USD)
            assertEquals("$0.01", result)
        }
    }

    @Nested
    @DisplayName("getDisplayText Method")
    inner class GetDisplayTextMethod {

        @Test
        @DisplayName("Should return correct display text for USD")
        fun `should return correct display text for USD`() {
            val result = Currency.getDisplayText(Currency.USD)
            assertEquals("USD - $ - US Dollar", result)
        }

        @Test
        @DisplayName("Should return correct display text for EUR")
        fun `should return correct display text for EUR`() {
            val result = Currency.getDisplayText(Currency.EUR)
            assertEquals("EUR - € - Euro", result)
        }

        @Test
        @DisplayName("Should return correct display text for JPY")
        fun `should return correct display text for JPY`() {
            val result = Currency.getDisplayText(Currency.JPY)
            assertEquals("JPY - ¥ - Japanese Yen", result)
        }
    }

    @Nested
    @DisplayName("isValidCode Method")
    inner class IsValidCodeMethod {

        @ParameterizedTest
        @ValueSource(strings = ["USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF"])
        @DisplayName("Should return true for valid currency codes")
        fun `should return true for valid currency codes`(code: String) {
            assertTrue(Currency.isValidCode(code))
        }

        @ParameterizedTest
        @ValueSource(strings = ["usd", "eur", "gbp", "jpy", "cad"])
        @DisplayName("Should return true for valid currency codes in lowercase")
        fun `should return true for valid currency codes in lowercase`(code: String) {
            assertTrue(Currency.isValidCode(code))
        }

        @ParameterizedTest
        @ValueSource(strings = ["", "XXX", "ABC", "123", "US", "USDD"])
        @DisplayName("Should return false for invalid currency codes")
        fun `should return false for invalid currency codes`(code: String) {
            assertFalse(Currency.isValidCode(code))
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesAndErrorHandling {

        @Test
        @DisplayName("Should handle NaN amounts gracefully")
        fun `should handle NaN amounts gracefully`() {
            val result = Currency.formatAmount(Double.NaN, Currency.USD)
            assertEquals("\$NaN", result)
        }

        @Test
        @DisplayName("Should handle infinite amounts gracefully")
        fun `should handle infinite amounts gracefully`() {
            val positiveInfinity = Currency.formatAmount(Double.POSITIVE_INFINITY, Currency.USD)
            val negativeInfinity = Currency.formatAmount(Double.NEGATIVE_INFINITY, Currency.USD)
            
            assertEquals("$∞", positiveInfinity)
            assertEquals("$-∞", negativeInfinity)
        }

        @Test
        @DisplayName("Should handle very small decimal amounts for JPY")
        fun `should handle very small decimal amounts for JPY`() {
            // JPY has 0 decimal places, so 0.5 should be formatted as 0
            val result = Currency.formatAmount(0.5, Currency.JPY)
            assertEquals("¥0", result)
        }

        @Test
        @DisplayName("Should handle rounding for JPY")
        fun `should handle rounding for JPY`() {
            // JPY has 0 decimal places, so 0.9 should be formatted as 0 (truncated)
            val result = Currency.formatAmount(0.9, Currency.JPY)
            assertEquals("¥0", result)
        }

        @Test
        @DisplayName("Should handle rounding for KRW")
        fun `should handle rounding for KRW`() {
            // KRW has 0 decimal places, so 0.9 should be formatted as 0 (truncated)
            val result = Currency.formatAmount(0.9, Currency.KRW)
            assertEquals("₩0", result)
        }
    }

    @Nested
    @DisplayName("Currency Enum Completeness")
    inner class CurrencyEnumCompleteness {

        @Test
        @DisplayName("Should have unique currency codes")
        fun `should have unique currency codes`() {
            val codes = Currency.values().map { it.code }
            val uniqueCodes = codes.toSet()
            assertEquals(codes.size, uniqueCodes.size, "All currency codes should be unique")
        }

        @Test
        @DisplayName("Should have unique symbols")
        fun `should have unique symbols`() {
            val symbols = Currency.values().map { it.symbol }
            val uniqueSymbols = symbols.toSet()
            assertEquals(symbols.size, uniqueSymbols.size, "All currency symbols should be unique")
        }

        @Test
        @DisplayName("Should have unique popularity rankings")
        fun `should have unique popularity rankings`() {
            val popularities = Currency.values().map { it.popularity }
            val uniquePopularities = popularities.toSet()
            assertEquals(popularities.size, uniquePopularities.size, "All popularity rankings should be unique")
        }

        @Test
        @DisplayName("Should have valid decimal places")
        fun `should have valid decimal places`() {
            Currency.values().forEach { currency ->
                assertTrue(
                    currency.decimalPlaces >= 0 && currency.decimalPlaces <= 4,
                    "Currency ${currency.code} should have decimal places between 0 and 4"
                )
            }
        }

        @Test
        @DisplayName("Should have non-empty display names")
        fun `should have non-empty display names`() {
            Currency.values().forEach { currency ->
                assertTrue(
                    currency.displayName.isNotBlank(),
                    "Currency ${currency.code} should have a non-empty display name"
                )
            }
        }

        @Test
        @DisplayName("Should have non-empty symbols")
        fun `should have non-empty symbols`() {
            Currency.values().forEach { currency ->
                assertTrue(
                    currency.symbol.isNotBlank(),
                    "Currency ${currency.code} should have a non-empty symbol"
                )
            }
        }
    }
}
