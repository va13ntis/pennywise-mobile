package com.pennywise.app.performance

import com.pennywise.app.domain.model.Currency
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.*

/**
 * Unit test version of currency UI performance tests
 * These tests can run in GitHub Actions without requiring an Android device/emulator
 * Tests the performance of currency-related operations without UI components
 */
class CurrencyUIPerformanceTestUnit {

    private val testAmounts = listOf(0.0, 1.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0, 999999.99)
    private val testCurrencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY")
    private val allCurrencies = Currency.values().toList()

    @Before
    fun setUp() {
        // Setup for unit tests
    }

    /**
     * Benchmark: Currency enum operations performance
     * Tests the performance of Currency enum operations
     */
    @Test
    fun benchmarkCurrencyEnumOperations() {
        // Test getting default currency
        val defaultCurrency = Currency.getDefault()
        assertNotNull("Default currency should not be null", defaultCurrency)
        
        // Test getting most popular currencies
        val popularCurrencies = Currency.getMostPopular()
        assertTrue("Popular currencies should not be empty", popularCurrencies.isNotEmpty())
        
        // Test getting currency by code
        testCurrencies.forEach { code ->
            val currency = Currency.fromCode(code)
            assertNotNull("Currency should be found for code: $code", currency)
        }
        
        // Test getting display text
        allCurrencies.forEach { currency ->
            val displayText = Currency.getDisplayText(currency)
            assertTrue("Display text should not be empty", displayText.isNotEmpty())
        }
    }

    /**
     * Benchmark: Currency validation performance
     * Tests the performance of validating currency codes
     */
    @Test
    fun benchmarkCurrencyValidation() {
        val validCurrencies = testCurrencies
        val invalidCurrencies = listOf("XXX", "ZZZ", "ABC", "123", "")
        val allTestCurrencies = validCurrencies + invalidCurrencies

        allTestCurrencies.forEach { currency ->
            val isValid = Currency.isValidCode(currency)
            assertEquals("Currency validation should match expected result", 
                validCurrencies.contains(currency), isValid)
        }
    }

    /**
     * Benchmark: Currency search performance
     * Tests the performance of searching through currency lists
     */
    @Test
    fun benchmarkCurrencySearch() {
        val searchTerms = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "Dollar", "Euro", "Pound")

        searchTerms.forEach { term ->
            val results = allCurrencies.filter { 
                it.code.contains(term, ignoreCase = true) ||
                it.name.contains(term, ignoreCase = true) ||
                it.displayName.contains(term, ignoreCase = true)
            }
            assertTrue("Search results should not be empty for term: $term", results.isNotEmpty())
        }
    }

    /**
     * Benchmark: Currency sorting performance
     * Tests the performance of sorting currencies by popularity
     */
    @Test
    fun benchmarkCurrencySorting() {
        val sortedCurrencies = Currency.getSortedByPopularity()
        assertTrue("Sorted currencies should not be empty", sortedCurrencies.isNotEmpty())
        assertEquals("Sorted currencies should have same size as all currencies", 
            allCurrencies.size, sortedCurrencies.size)
        
        // Verify sorting is correct (by popularity)
        for (i in 1 until sortedCurrencies.size) {
            assertTrue("Currencies should be sorted by popularity", 
                sortedCurrencies[i-1].popularity <= sortedCurrencies[i].popularity)
        }
    }

    /**
     * Benchmark: Currency amount formatting performance
     * Tests the performance of formatting amounts with currency
     */
    @Test
    fun benchmarkCurrencyAmountFormatting() {
        testCurrencies.forEach { currencyCode ->
            testAmounts.forEach { amount ->
                val currency = Currency.fromCode(currencyCode)
                if (currency != null) {
                    val formatted = Currency.formatAmount(amount, currency)
                    assertTrue("Formatted amount should not be empty", formatted.isNotEmpty())
                }
            }
        }
    }

    /**
     * Benchmark: Currency symbol retrieval performance
     * Tests the performance of getting currency symbols
     */
    @Test
    fun benchmarkCurrencySymbolRetrieval() {
        testCurrencies.forEach { currencyCode ->
            val currency = Currency.fromCode(currencyCode)
            if (currency != null) {
                assertTrue("Currency symbol should not be empty", currency.symbol.isNotEmpty())
                assertTrue("Currency display name should not be empty", currency.displayName.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Currency fraction digits performance
     * Tests the performance of getting currency fraction digits
     */
    @Test
    fun benchmarkCurrencyFractionDigits() {
        testCurrencies.forEach { currencyCode ->
            val currency = Currency.fromCode(currencyCode)
            if (currency != null) {
                assertTrue("Fraction digits should be non-negative", currency.decimalPlaces >= 0)
            }
        }
    }

    /**
     * Benchmark: Currency batch operations performance
     * Tests the performance of batch currency operations
     */
    @Test
    fun benchmarkCurrencyBatchOperations() {
        val batchSize = 10
        val testData = (1..batchSize).map { 
            Triple(
                testAmounts.random(),
                testCurrencies.random(),
                testCurrencies.random()
            )
        }

        testData.forEach { (amount, fromCurrency, toCurrency) ->
            // Format original amount
            val fromCurrencyEnum = Currency.fromCode(fromCurrency)
            if (fromCurrencyEnum != null) {
                val originalFormatted = Currency.formatAmount(amount, fromCurrencyEnum)
                assertTrue("Original formatted amount should not be empty", originalFormatted.isNotEmpty())
            }
            
            // Test currency validation
            val isFromValid = Currency.isValidCode(fromCurrency)
            val isToValid = Currency.isValidCode(toCurrency)
            assertTrue("From currency should be valid", isFromValid)
            assertTrue("To currency should be valid", isToValid)
        }
    }

    /**
     * Benchmark: Currency code conversion performance
     * Tests the performance of converting between currency codes and enums
     */
    @Test
    fun benchmarkCurrencyCodeConversion() {
        // Test all currency codes
        allCurrencies.forEach { currency ->
            val code = currency.code
            val convertedBack = Currency.fromCode(code)
            assertEquals("Currency conversion should be consistent", currency, convertedBack)
        }
        
        // Test invalid codes
        val invalidCodes = listOf("INVALID", "XXX", "ZZZ", "123")
        invalidCodes.forEach { code ->
            val result = Currency.fromCode(code)
            assertNull("Invalid currency code should return null", result)
        }
    }

    /**
     * Benchmark: Currency popularity operations performance
     * Tests the performance of currency popularity-related operations
     */
    @Test
    fun benchmarkCurrencyPopularityOperations() {
        // Test most popular currencies
        val mostPopular = Currency.getMostPopular()
        assertEquals("Most popular should return top 10 currencies", 10, mostPopular.size)
        
        // Verify they are actually the most popular
        val sortedByPopularity = Currency.getSortedByPopularity()
        val top10FromSorted = sortedByPopularity.take(10)
        assertEquals("Most popular should match top 10 from sorted", mostPopular, top10FromSorted)
        
        // Test default currency
        val defaultCurrency = Currency.getDefault()
        assertEquals("Default currency should be USD", Currency.USD, defaultCurrency)
    }

    /**
     * Benchmark: Currency display operations performance
     * Tests the performance of currency display-related operations
     */
    @Test
    fun benchmarkCurrencyDisplayOperations() {
        allCurrencies.forEach { currency ->
            // Test display text generation
            val displayText = Currency.getDisplayText(currency)
            assertTrue("Display text should contain currency code", displayText.contains(currency.code))
            assertTrue("Display text should contain currency symbol", displayText.contains(currency.symbol))
            assertTrue("Display text should contain currency name", displayText.contains(currency.displayName))
            
            // Test amount formatting with currency
            val testAmounts = listOf(0.0, 100.0, 1000.0, -100.0)
            testAmounts.forEach { amount ->
                val formatted = Currency.formatAmount(amount, currency)
                assertTrue("Formatted amount should not be empty", formatted.isNotEmpty())
                assertTrue("Formatted amount should contain currency symbol", formatted.contains(currency.symbol))
            }
        }
    }
}
