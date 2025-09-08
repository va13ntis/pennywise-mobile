package com.pennywise.app.domain.validation

import com.pennywise.app.domain.model.Currency
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Integration test for currency validation flow
 */
class CurrencyValidationIntegrationTest {
    
    private lateinit var currencyValidator: CurrencyValidator
    private lateinit var currencyErrorHandler: CurrencyErrorHandler
    
    @Before
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
        assertTrue("Currency validation should succeed", currencyValidation is ValidationResult.Success)
        assertEquals("Should return correct currency", Currency.USD, currency)
        assertTrue("Amount validation should succeed", amountValidation is ValidationResult.Success)
        assertEquals("Should format amount correctly", "$100.00", formattedAmount)
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
        assertTrue("Currency validation should fail", currencyValidation is ValidationResult.Error)
        assertEquals("Should fallback to USD", Currency.USD, currency)
        assertTrue("Amount validation should succeed with fallback currency", amountValidation is ValidationResult.Success)
        assertEquals("Should format amount with fallback currency", "$100.00", formattedAmount)
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
        assertEquals("Should return JPY currency", Currency.JPY, currency)
        assertTrue("Valid amount should succeed", validAmountValidation is ValidationResult.Success)
        assertTrue("Invalid amount should fail", invalidAmountValidation is ValidationResult.Error)
        assertEquals("Should format valid amount correctly", "¥100", formattedValidAmount)
        assertEquals("Should format invalid amount with fallback", "¥0", formattedInvalidAmount)
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
        assertEquals("Should provide correct message for empty code", "Please select a currency", emptyCodeMessage)
        assertEquals("Should provide correct message for invalid length", "Currency code must be exactly 3 characters", invalidLengthMessage)
        assertEquals("Should provide correct message for unsupported code", "Currency 'XXX' is not supported", unsupportedCodeMessage)
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
        assertTrue("Should include base message", messageWithSuggestions.contains("Currency 'US' is not supported"))
        assertTrue("Should include suggestions", messageWithSuggestions.contains("USD, EUR, GBP"))
    }
    
    @Test
    fun `detailed validation should provide comprehensive error information`() {
        // Given
        val invalidCode = "US"
        
        // When
        val detailedResult = currencyValidator.validateCurrencyCodeWithDetails(invalidCode)
        
        // Then
        assertTrue("Should return detailed error", detailedResult is DetailedValidationResult.Error)
        val error = detailedResult as DetailedValidationResult.Error
        assertEquals("Should have correct error type", CurrencyErrorType.INVALID_LENGTH, error.errorType)
        assertEquals("Should have correct message", "Currency code must be exactly 3 characters", error.message)
        assertTrue("Should have suggestion", error.suggestedFix.isNotEmpty())
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
            assertTrue("Validation should handle $description", result is ValidationResult)
            
            val fallback = currencyValidator.getValidCurrencyCodeOrFallback(code)
            assertTrue("Fallback should always return valid code", Currency.isValidCode(fallback))
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
                assertTrue("Amount $amount should be valid", result is ValidationResult.Success)
            } else {
                assertTrue("Amount $amount should be invalid", result is ValidationResult.Error)
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
                assertEquals("Should format $amount in $currencyCode correctly", expectedFormat, result)
            }
        }
    }
}
