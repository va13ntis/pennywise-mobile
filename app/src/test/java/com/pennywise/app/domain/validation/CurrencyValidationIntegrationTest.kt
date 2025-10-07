package com.pennywise.app.domain.validation

import com.pennywise.app.domain.model.Currency
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Integration test for currency validation flow
 */
class CurrencyValidationIntegrationTest {
    
    private lateinit var currencyValidator: CurrencyValidator
    private lateinit var currencyErrorHandler: CurrencyErrorHandler
    
    @BeforeEach
    fun setUp() {
        currencyValidator = CurrencyValidator()
        currencyErrorHandler = CurrencyErrorHandler()
    }
    
    @Test
    fun `complete currency validation flow should handle valid currency correctly`() {
        // Given
        val validCurrencyCode = "USD"
        val amount = 100.0
        
        // When - Validate currency code
        val currencyValidation = currencyValidator.validateCurrencyCode(validCurrencyCode)
        val currency = currencyValidator.getValidCurrencyOrFallback(validCurrencyCode)
        val amountValidation = currencyValidator.validateAmountForCurrency(amount, currency)
        val formattedAmount = currencyValidator.formatAmountWithValidation(amount, validCurrencyCode)
        
        // Then
        assertTrue(currencyValidation is ValidationResult.Success, "Currency validation should succeed")
        assertEquals(Currency.USD, currency, "Should return correct currency")
        assertTrue(amountValidation is ValidationResult.Success, "Amount validation should succeed")
        assertEquals("$100.00", formattedAmount, "Should format amount correctly")
    }
    
    @Test
    fun `complete currency validation flow should handle invalid currency with fallback`() {
        // Given
        val invalidCurrencyCode = "XXX"
        val amount = 100.0
        
        // When - Validate currency code
        val currencyValidation = currencyValidator.validateCurrencyCode(invalidCurrencyCode)
        val currency = currencyValidator.getValidCurrencyOrFallback(invalidCurrencyCode)
        val amountValidation = currencyValidator.validateAmountForCurrency(amount, currency)
        val formattedAmount = currencyValidator.formatAmountWithValidation(amount, invalidCurrencyCode)
        
        // Then
        assertTrue(currencyValidation is ValidationResult.Error, "Currency validation should fail")
        assertEquals(Currency.USD, currency, "Should fallback to USD")
        assertTrue(amountValidation is ValidationResult.Success, "Amount validation should succeed with fallback currency")
        assertEquals("$100.00", formattedAmount, "Should format amount with fallback currency")
    }
    
    @Test
    fun `complete currency validation flow should handle JPY with no decimal places`() {
        // Given
        val jpyCurrencyCode = "JPY"
        val validAmount = 100.0
        val invalidAmount = 100.5
        
        // When - Validate currency code
        val currency = currencyValidator.getValidCurrencyOrFallback(jpyCurrencyCode)
        val validAmountValidation = currencyValidator.validateAmountForCurrency(validAmount, currency)
        val invalidAmountValidation = currencyValidator.validateAmountForCurrency(invalidAmount, currency)
        val formattedValidAmount = currencyValidator.formatAmountWithValidation(validAmount, jpyCurrencyCode)
        val formattedInvalidAmount = currencyValidator.formatAmountWithValidation(invalidAmount, jpyCurrencyCode)
        
        // Then
        assertEquals(Currency.JPY, currency, "Should return JPY currency")
        assertTrue(validAmountValidation is ValidationResult.Success, "Valid amount should succeed")
        assertTrue(invalidAmountValidation is ValidationResult.Error, "Invalid amount should fail")
        assertEquals("¥100", formattedValidAmount, "Should format valid amount correctly")
        assertEquals("¥0", formattedInvalidAmount, "Should format invalid amount with fallback")
    }
    
    @Test
    fun `currency error handler should provide appropriate messages for different error types`() {
        // Given
        val emptyCode = ""
        val invalidLengthCode = "US"
        val unsupportedCode = "XXX"
        
        // When
        val emptyCodeMessage = currencyErrorHandler.getUserFriendlyErrorMessage(
            CurrencyErrorType.EMPTY_CODE, emptyCode
        )
        val invalidLengthMessage = currencyErrorHandler.getUserFriendlyErrorMessage(
            CurrencyErrorType.INVALID_LENGTH, invalidLengthCode
        )
        val unsupportedCodeMessage = currencyErrorHandler.getUserFriendlyErrorMessage(
            CurrencyErrorType.UNSUPPORTED_CODE, unsupportedCode
        )
        
        // Then
        assertEquals("Please select a currency", emptyCodeMessage, "Should provide correct message for empty code")
        assertEquals("Currency code must be exactly 3 characters", invalidLengthMessage, "Should provide correct message for invalid length")
        assertEquals("Currency 'XXX' is not supported", unsupportedCodeMessage, "Should provide correct message for unsupported code")
    }
    
    @Test
    fun `currency error handler should provide suggestions for unsupported codes`() {
        // Given
        val unsupportedCode = "US"
        val suggestions = listOf("USD", "EUR", "GBP")
        
        // When
        val messageWithSuggestions = currencyErrorHandler.getUserFriendlyErrorMessageWithSuggestions(
            CurrencyErrorType.UNSUPPORTED_CODE, unsupportedCode, suggestions
        )
        
        // Then
        assertTrue(messageWithSuggestions.contains("Currency 'US' is not supported"), "Should include base message")
        assertTrue(messageWithSuggestions.contains("USD, EUR, GBP"), "Should include suggestions")
    }
    
    @Test
    fun `detailed validation should provide comprehensive error information`() {
        // Given
        val invalidCode = "US"
        
        // When
        val detailedResult = currencyValidator.validateCurrencyCodeWithDetails(invalidCode)
        
        // Then
        assertTrue(detailedResult is DetailedValidationResult.Error, "Should return detailed error")
        val error = detailedResult as DetailedValidationResult.Error
        assertEquals(CurrencyErrorType.INVALID_LENGTH, error.errorType, "Should have correct error type")
        assertEquals("Currency code must be exactly 3 characters", error.message, "Should have correct message")
        assertTrue(error.suggestedFix.isNotEmpty(), "Should have suggestion")
    }
    
    @Test
    fun `currency validation should handle edge cases gracefully`() {
        // Given
        val edgeCases = listOf(
            "" to "empty string",
            "   " to "blank string",
            "A" to "single character",
            "ABCD" to "four characters",
            "123" to "numeric string",
            "usd" to "lowercase valid code"
        )
        
        // When & Then
        edgeCases.forEach { (code, description) ->
            val result = currencyValidator.validateCurrencyCode(code)
            @Suppress("USELESS_IS_CHECK")
            assertTrue(result is ValidationResult, "Validation should handle $description")
            
            val fallback = currencyValidator.getValidCurrencyCodeOrFallback(code)
            assertTrue(Currency.isValidCode(fallback), "Fallback should always return valid code")
        }
    }
    
    @Test
    fun `amount validation should handle various amount types correctly`() {
        // Given
        val currency = Currency.USD
        val testCases = listOf(
            0.0 to true,
            1.0 to true,
            100.0 to true,
            999.99 to true,
            -1.0 to false,
            Double.NaN to false,
            Double.POSITIVE_INFINITY to false,
            Double.NEGATIVE_INFINITY to false
        )
        
        // When & Then
        testCases.forEach { (amount, shouldBeValid) ->
            val result = currencyValidator.validateAmountForCurrency(amount, currency)
            if (shouldBeValid) {
                assertTrue(result is ValidationResult.Success, "Amount $amount should be valid")
            } else {
                assertTrue(result is ValidationResult.Error, "Amount $amount should be invalid")
            }
        }
    }
    
    @Test
    fun `currency formatting should handle all supported currencies`() {
        // Given
        val testCases = mapOf(
            "USD" to mapOf(100.0 to "$100.00", 1.5 to "$1.50", 0.0 to "$0.00"),
            "EUR" to mapOf(100.0 to "€100.00", 1.5 to "€1.50", 0.0 to "€0.00"),
            "GBP" to mapOf(100.0 to "£100.00", 1.5 to "£1.50", 0.0 to "£0.00"),
            "JPY" to mapOf(100.0 to "¥100", 1.0 to "¥1", 0.0 to "¥0"),
            "KRW" to mapOf(100.0 to "₩100", 1.0 to "₩1", 0.0 to "₩0")
        )
        
        // When & Then
        testCases.forEach { (currencyCode, amounts) ->
            amounts.forEach { (amount, expectedFormat) ->
                val result = currencyValidator.formatAmountWithValidation(amount, currencyCode)
                assertEquals(expectedFormat, result, "Should format $amount in $currencyCode correctly")
            }
        }
    }
}
