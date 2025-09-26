package com.pennywise.app.testutils

import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.User
import java.util.Date

/**
 * Test fixtures for currency-related UI tests
 */
object CurrencyTestFixtures {
    
    /**
     * Create a test user with specific currency preferences
     */
    fun createTestUser(
        id: Long = 1L,
        defaultCurrency: String = "USD"
    ): User {
        return User(
            id = id,
            defaultCurrency = defaultCurrency,
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
    }
    
    /**
     * Create test transactions with different currencies
     */
    fun createTestTransactions(): List<Transaction> {
        val baseDate = Date()
        return listOf(
            Transaction(
                id = 1L,
                userId = 1L,
                amount = 100.0,
                currency = "USD",
                description = "Test USD Transaction",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = baseDate,
                isRecurring = false
            ),
            Transaction(
                id = 2L,
                userId = 1L,
                amount = 85.50,
                currency = "EUR",
                description = "Test EUR Transaction",
                category = "Transport",
                type = TransactionType.EXPENSE,
                date = baseDate,
                isRecurring = false
            ),
            Transaction(
                id = 3L,
                userId = 1L,
                amount = 15000.0,
                currency = "JPY",
                description = "Test JPY Transaction",
                category = "Entertainment",
                type = TransactionType.EXPENSE,
                date = baseDate,
                isRecurring = true
            ),
            Transaction(
                id = 4L,
                userId = 1L,
                amount = 75.25,
                currency = "GBP",
                description = "Test GBP Transaction",
                category = "Shopping",
                type = TransactionType.EXPENSE,
                date = baseDate,
                isRecurring = false
            )
        )
    }
    
    /**
     * Get popular currencies for testing
     */
    fun getPopularCurrencies(): List<Currency> {
        return Currency.getMostPopular()
    }
    
    /**
     * Get all currencies for testing
     */
    fun getAllCurrencies(): List<Currency> {
        return Currency.values().toList()
    }
    
    /**
     * Get currencies with specific decimal places for testing
     */
    fun getCurrenciesWithDecimalPlaces(decimalPlaces: Int): List<Currency> {
        return Currency.values().filter { it.decimalPlaces == decimalPlaces }
    }
    
    /**
     * Get currencies without decimal places (like JPY, KRW)
     */
    fun getCurrenciesWithoutDecimals(): List<Currency> {
        return getCurrenciesWithDecimalPlaces(0)
    }
    
    /**
     * Get currencies with 2 decimal places (most common)
     */
    fun getCurrenciesWithTwoDecimals(): List<Currency> {
        return getCurrenciesWithDecimalPlaces(2)
    }
    
    /**
     * Create test data for currency conversion settings
     */
    fun createCurrencyConversionTestData(): Map<String, Any> {
        return mapOf(
            "enabled" to true,
            "originalCurrency" to "USD",
            "supportedCurrencies" to listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD")
        )
    }
    
    /**
     * Get test amounts for different currency types
     */
    fun getTestAmounts(): Map<String, List<Double>> {
        return mapOf(
            "USD" to listOf(10.50, 100.00, 999.99, 0.01),
            "EUR" to listOf(15.75, 250.00, 1500.50, 0.05),
            "JPY" to listOf(100.0, 1000.0, 10000.0, 1.0), // No decimals
            "GBP" to listOf(25.99, 500.00, 2000.25, 0.10)
        )
    }
    
    /**
     * Get invalid test amounts for validation testing
     */
    fun getInvalidTestAmounts(): List<String> {
        return listOf(
            "", // Empty
            "abc", // Non-numeric
            "-100", // Negative
            "0", // Zero
            "100.999", // Too many decimals for JPY
            "100.12345" // Too many decimals for most currencies
        )
    }
    
    /**
     * Get test currency codes for validation
     */
    fun getTestCurrencyCodes(): Map<String, Boolean> {
        return mapOf(
            "USD" to true,
            "EUR" to true,
            "GBP" to true,
            "JPY" to true,
            "usd" to true, // Case insensitive
            "eur" to true,
            "XXX" to false, // Invalid
            "ABC" to false, // Invalid
            "" to false, // Empty
            "US" to false // Too short
        )
    }
}
