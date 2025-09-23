package com.pennywise.app.testutils

import com.pennywise.app.data.model.CachedExchangeRate
import com.pennywise.app.data.model.ExchangeRateResponse
import com.pennywise.app.domain.model.Currency
import java.util.concurrent.TimeUnit

/**
 * Test fixtures for currency-related tests
 * Provides standardized test data for currency tests to ensure consistency across test cases
 */
object CurrencyTestFixtures {

    // Common test amounts
    object Amounts {
        const val ZERO = 0.0
        const val SMALL = 0.01
        const val STANDARD = 100.0
        const val LARGE = 9999999.99
        const val NEGATIVE = -50.0
        val NAN = Double.NaN
        val POSITIVE_INFINITY = Double.POSITIVE_INFINITY
        val NEGATIVE_INFINITY = Double.NEGATIVE_INFINITY
    }

    // Common currency pairs
    object CurrencyPairs {
        val USD_EUR = Pair("USD", "EUR")
        val EUR_GBP = Pair("EUR", "GBP")
        val GBP_JPY = Pair("GBP", "JPY")
        val JPY_USD = Pair("JPY", "USD")
        val USD_USD = Pair("USD", "USD")
        val EUR_EUR = Pair("EUR", "EUR")
    }

    // Common exchange rates
    object ExchangeRates {
        const val USD_TO_EUR = 0.85
        const val EUR_TO_USD = 1.18
        const val EUR_TO_GBP = 0.86
        const val GBP_TO_EUR = 1.16
        const val GBP_TO_JPY = 150.0
        const val JPY_TO_GBP = 0.0067
        const val USD_TO_JPY = 130.0
        const val JPY_TO_USD = 0.0077
    }

    // Create a valid cached exchange rate
    fun createValidCachedRate(
        baseCode: String,
        targetCode: String,
        rate: Double,
        hoursOld: Long = 1
    ): CachedExchangeRate {
        return CachedExchangeRate(
            baseCode = baseCode,
            targetCode = targetCode,
            conversionRate = rate,
            lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hoursOld)
        )
    }

    // Create an expired cached exchange rate
    fun createExpiredCachedRate(
        baseCode: String,
        targetCode: String,
        rate: Double,
        hoursOld: Long = 25
    ): CachedExchangeRate {
        return CachedExchangeRate(
            baseCode = baseCode,
            targetCode = targetCode,
            conversionRate = rate,
            lastUpdateTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hoursOld)
        )
    }

    // Create an exchange rate API response
    fun createExchangeRateResponse(
        baseCode: String,
        targetCode: String,
        rate: Double
    ): ExchangeRateResponse {
        return ExchangeRateResponse(
            result = "success",
            baseCode = baseCode,
            targetCode = targetCode,
            conversionRate = rate,
            lastUpdateTime = "2023-01-01 00:00:00",
            nextUpdateTime = "2023-01-02 00:00:00"
        )
    }

    // Common test scenarios for currency conversion
    object ConversionScenarios {
        // Standard conversion (e.g., $100 to €85)
        val STANDARD_USD_TO_EUR = Triple(Amounts.STANDARD, CurrencyPairs.USD_EUR, ExchangeRates.USD_TO_EUR)
        
        // Large amount conversion
        val LARGE_USD_TO_EUR = Triple(Amounts.LARGE, CurrencyPairs.USD_EUR, ExchangeRates.USD_TO_EUR)
        
        // Small amount conversion
        val SMALL_USD_TO_EUR = Triple(Amounts.SMALL, CurrencyPairs.USD_EUR, ExchangeRates.USD_TO_EUR)
        
        // Zero amount conversion
        val ZERO_USD_TO_EUR = Triple(Amounts.ZERO, CurrencyPairs.USD_EUR, ExchangeRates.USD_TO_EUR)
        
        // Negative amount conversion
        val NEGATIVE_USD_TO_EUR = Triple(Amounts.NEGATIVE, CurrencyPairs.USD_EUR, ExchangeRates.USD_TO_EUR)
        
        // Same currency conversion
        val SAME_CURRENCY_USD = Triple(Amounts.STANDARD, CurrencyPairs.USD_USD, 1.0)
    }

    // Serialized cache entries for testing
    object SerializedCache {
        fun validUsdEurCache(rate: Double = ExchangeRates.USD_TO_EUR): String {
            val validTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
            return """{"baseCode":"USD","targetCode":"EUR","conversionRate":$rate,"lastUpdateTime":$validTime}"""
        }

        fun expiredUsdEurCache(rate: Double = ExchangeRates.USD_TO_EUR): String {
            val expiredTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(25)
            return """{"baseCode":"USD","targetCode":"EUR","conversionRate":$rate,"lastUpdateTime":$expiredTime}"""
        }

        const val INVALID_CACHE = "invalid_json_data"
    }

    // Common Currency objects for testing
    object TestCurrencies {
        // Helper to create standardized test result strings
        fun formattedAmount(amount: Double, currency: Currency): String {
            val formattedAmount = when (currency.decimalPlaces) {
                0 -> amount.toInt().toString()
                else -> String.format("%.${currency.decimalPlaces}f", amount)
            }
            return "${currency.symbol}$formattedAmount"
        }

        // Common test results for formatting
        object ExpectedFormats {
            // USD formats ($100.00)
            fun usdStandard() = formattedAmount(Amounts.STANDARD, Currency.USD)
            fun usdSmall() = formattedAmount(Amounts.SMALL, Currency.USD)
            fun usdLarge() = formattedAmount(Amounts.LARGE, Currency.USD)
            fun usdNegative() = formattedAmount(Amounts.NEGATIVE, Currency.USD)
            
            // EUR formats (€100.00)
            fun eurStandard() = formattedAmount(Amounts.STANDARD, Currency.EUR)
            fun eurSmall() = formattedAmount(Amounts.SMALL, Currency.EUR)
            fun eurLarge() = formattedAmount(Amounts.LARGE, Currency.EUR)
            fun eurNegative() = formattedAmount(Amounts.NEGATIVE, Currency.EUR)
            
            // JPY formats (¥100)
            fun jpyStandard() = formattedAmount(Amounts.STANDARD, Currency.JPY)
            fun jpySmall() = formattedAmount(Amounts.SMALL, Currency.JPY)
            fun jpyLarge() = formattedAmount(Amounts.LARGE, Currency.JPY)
            fun jpyNegative() = formattedAmount(Amounts.NEGATIVE, Currency.JPY)
        }
    }
    
    // Test data for currency validation tests
    object ValidationTestData {
        val VALID_CODES = listOf("USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF")
        val INVALID_CODES = listOf("", "ABC", "USDD", "123", "US", "XX")
        val CASE_VARIATIONS = mapOf(
            "USD" to "USD",
            "usd" to "USD",
            "Usd" to "USD",
            "eUr" to "EUR"
        )
    }
}

