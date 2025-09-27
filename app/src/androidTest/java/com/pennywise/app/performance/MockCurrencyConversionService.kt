package com.pennywise.app.performance

/**
 * Mock implementation of CurrencyConversionService for testing
 */
class MockCurrencyConversionService {
    
    private val mockRates = mapOf(
        "USD_EUR" to 0.85,
        "EUR_GBP" to 0.73,
        "GBP_JPY" to 110.0,
        "JPY_USD" to 0.009,
        "USD_CAD" to 1.25,
        "EUR_CHF" to 0.92,
        "GBP_AUD" to 1.84,
        "CAD_JPY" to 88.0,
        "EUR_USD" to 1.18,
        "GBP_EUR" to 1.37,
        "JPY_GBP" to 0.009,
        "CAD_USD" to 0.80,
        "CHF_EUR" to 1.09,
        "AUD_GBP" to 0.54,
        "JPY_CAD" to 0.011
    )
    
    suspend fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String): Double? {
        return try {
            when {
                fromCurrency.uppercase() == toCurrency.uppercase() -> amount
                else -> {
                    val rate = mockRates["${fromCurrency}_${toCurrency}"] ?: 1.0
                    amount * rate
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun isConversionAvailable(@Suppress("UNUSED_PARAMETER") fromCurrency: String, @Suppress("UNUSED_PARAMETER") toCurrency: String): Boolean {
        return true // Mock as always available for testing
    }
    
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "total_cached" to 0,
            "valid_cached" to 0,
            "expired_cached" to 0
        )
    }
    
    fun clearCache() {
        // Mock implementation - no-op
    }
}
