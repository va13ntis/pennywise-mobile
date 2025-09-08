package com.pennywise.app.domain.validation

import com.pennywise.app.domain.model.Currency
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CurrencyValidatorTest {
    
    private lateinit var currencyValidator: CurrencyValidator
    
    @BeforeEach
    fun setUp() {
        currencyValidator = CurrencyValidator()
    }
    
    @Test
    fun `validateCurrencyCode should return success for valid currency codes`() {
        // Given
        val validCodes = listOf("USD", "EUR", "GBP", "JPY", "CAD")
        
        // When & Then
        validCodes.forEach { code ->
            val result = currencyValidator.validateCurrencyCode(code)
            assertTrue(result is ValidationResult.Success, "Validation should succeed for $code")
        }
    }
    
    @Test
    fun `validateCurrencyCode should return error for empty code`() {
        // Given
        val emptyCode = ""
        
        // When
        val result = currencyValidator.validateCurrencyCode(emptyCode)
        
        // Then
        assertTrue(result is ValidationResult.Error, "Should return error")
        assertEquals("Should return correct error message", 
            "Currency code cannot be empty", 
            (result as ValidationResult.Error).message)
    }
    
    @Test
    fun `validateCurrencyCode should return error for blank code`() {
        // Given
        val blankCode = "   "
        
        // When
        val result = currencyValidator.validateCurrencyCode(blankCode)
        
        // Then
        assertTrue(result is ValidationResult.Error, "Should return error")
        assertEquals("Should return correct error message", 
            "Currency code cannot be empty", 
            (result as ValidationResult.Error).message)
    }
    
    @Test
    fun `validateCurrencyCode should return error for invalid length`() {
        // Given
        val invalidLengthCodes = listOf("US", "USDD", "A", "ABCD")
        
        // When & Then
        invalidLengthCodes.forEach { code ->
            val result = currencyValidator.validateCurrencyCode(code)
            assertTrue(result is ValidationResult.Error, "Should return error for $code")
            assertEquals("Should return correct error message for $code", 
                "Currency code must be exactly 3 characters", 
                (result as ValidationResult.Error).message)
        }
    }
    
    @Test
    fun `validateCurrencyCode should return error for unsupported currency`() {
        // Given
        val unsupportedCodes = listOf("XXX", "ZZZ", "ABC", "123")
        
        // When & Then
        unsupportedCodes.forEach { code ->
            val result = currencyValidator.validateCurrencyCode(code)
            assertTrue(result is ValidationResult.Error, "Should return error for $code")
            assertEquals("Should return correct error message for $code", 
                "Unsupported currency code: $code", 
                (result as ValidationResult.Error).message)
        }
    }
    
    @Test
    fun `getValidCurrencyCodeOrFallback should return original code for valid currency`() {
        // Given
        val validCodes = listOf("USD", "EUR", "GBP", "JPY")
        
        // When & Then
        validCodes.forEach { code ->
            val result = currencyValidator.getValidCurrencyCodeOrFallback(code)
            assertEquals(code, result, "Should return original code for $code")
        }
    }
    
    @Test
    fun `getValidCurrencyCodeOrFallback should return USD for invalid currency`() {
        // Given
        val invalidCodes = listOf("", "XXX", "US", "USDD")
        
        // When & Then
        invalidCodes.forEach { code ->
            val result = currencyValidator.getValidCurrencyCodeOrFallback(code)
            assertEquals("USD", result, "Should return USD for invalid code $code")
        }
    }
    
    @Test
    fun `getValidCurrencyOrFallback should return correct Currency enum for valid code`() {
        // Given
        val validCodes = mapOf(
            "USD" to Currency.USD,
            "EUR" to Currency.EUR,
            "GBP" to Currency.GBP,
            "JPY" to Currency.JPY
        )
        
        // When & Then
        validCodes.forEach { (code, expectedCurrency) ->
            val result = currencyValidator.getValidCurrencyOrFallback(code)
            assertEquals(expectedCurrency, result, "Should return correct currency for $code")
        }
    }
    
    @Test
    fun `getValidCurrencyOrFallback should return USD for invalid code`() {
        // Given
        val invalidCodes = listOf("", "XXX", "US", "USDD")
        
        // When & Then
        invalidCodes.forEach { code ->
            val result = currencyValidator.getValidCurrencyOrFallback(code)
            assertEquals(Currency.USD, result, "Should return USD for invalid code $code")
        }
    }
    
    @Test
    fun `validateAmountForCurrency should return success for valid amounts`() {
        // Given
        val validAmounts = listOf(0.0, 1.0, 100.0, 999.99, 1000000.0)
        val currency = Currency.USD
        
        // When & Then
        validAmounts.forEach { amount ->
            val result = currencyValidator.validateAmountForCurrency(amount, currency)
            assertTrue(result is ValidationResult.Success, "Validation should succeed for amount $amount")
        }
    }
    
    @Test
    fun `validateAmountForCurrency should return error for negative amounts`() {
        // Given
        val negativeAmounts = listOf(-1.0, -100.0, -0.01)
        val currency = Currency.USD
        
        // When & Then
        negativeAmounts.forEach { amount ->
            val result = currencyValidator.validateAmountForCurrency(amount, currency)
            assertTrue(result is ValidationResult.Error, "Should return error for negative amount $amount")
            assertEquals("Should return correct error message for $amount", 
                "Amount cannot be negative", 
                (result as ValidationResult.Error).message)
        }
    }
    
    @Test
    fun `validateAmountForCurrency should return error for NaN amounts`() {
        // Given
        val currency = Currency.USD
        
        // When
        val result = currencyValidator.validateAmountForCurrency(Double.NaN, currency)
        
        // Then
        assertTrue(result is ValidationResult.Error, "Should return error for NaN")
        assertEquals("Should return correct error message", 
            "Invalid amount", 
            (result as ValidationResult.Error).message)
    }
    
    @Test
    fun `validateAmountForCurrency should return error for infinite amounts`() {
        // Given
        val currency = Currency.USD
        
        // When
        val result = currencyValidator.validateAmountForCurrency(Double.POSITIVE_INFINITY, currency)
        
        // Then
        assertTrue(result is ValidationResult.Error, "Should return error for infinite amount")
        assertEquals("Should return correct error message", 
            "Invalid amount", 
            (result as ValidationResult.Error).message)
    }
    
    @Test
    fun `validateAmountForCurrency should return error for decimal places in JPY`() {
        // Given
        val decimalAmounts = listOf(1.5, 100.99, 0.01)
        val currency = Currency.JPY // JPY has 0 decimal places
        
        // When & Then
        decimalAmounts.forEach { amount ->
            val result = currencyValidator.validateAmountForCurrency(amount, currency)
            assertTrue(result is ValidationResult.Error, "Should return error for decimal amount $amount in JPY")
            assertEquals("Should return correct error message for $amount", 
                "Japanese Yen does not support decimal places", 
                (result as ValidationResult.Error).message)
        }
    }
    
    @Test
    fun `validateAmountForCurrency should return success for integer amounts in JPY`() {
        // Given
        val integerAmounts = listOf(0.0, 1.0, 100.0, 1000.0)
        val currency = Currency.JPY
        
        // When & Then
        integerAmounts.forEach { amount ->
            val result = currencyValidator.validateAmountForCurrency(amount, currency)
            assertTrue(result is ValidationResult.Success, "Validation should succeed for integer amount $amount in JPY")
        }
    }
    
    @Test
    fun `validateAmountForCurrency should return success for decimal amounts in USD`() {
        // Given
        val decimalAmounts = listOf(1.5, 100.99, 0.01, 999.99)
        val currency = Currency.USD
        
        // When & Then
        decimalAmounts.forEach { amount ->
            val result = currencyValidator.validateAmountForCurrency(amount, currency)
            assertTrue(result is ValidationResult.Success, "Validation should succeed for decimal amount $amount in USD")
        }
    }
    
    @Test
    fun `formatAmountWithValidation should format valid amounts correctly`() {
        // Given
        val testCases = mapOf(
            "USD" to mapOf(100.0 to "$100.00", 1.5 to "$1.50", 0.0 to "$0.00"),
            "EUR" to mapOf(100.0 to "€100.00", 1.5 to "€1.50", 0.0 to "€0.00"),
            "JPY" to mapOf(100.0 to "¥100", 1.0 to "¥1", 0.0 to "¥0")
        )
        
        // When & Then
        testCases.forEach { (currencyCode, amounts) ->
            amounts.forEach { (amount, expectedFormat) ->
                val result = currencyValidator.formatAmountWithValidation(amount, currencyCode)
                assertEquals(expectedFormat, result, "Should format $amount in $currencyCode correctly")
            }
        }
    }
    
    @Test
    fun `formatAmountWithValidation should handle invalid currency codes gracefully`() {
        // Given
        val invalidCurrencyCode = "XXX"
        val amount = 100.0
        
        // When
        val result = currencyValidator.formatAmountWithValidation(amount, invalidCurrencyCode)
        
        // Then
        assertEquals("$0", result, "Should return safe fallback format")
    }
    
    @Test
    fun `validateCurrencyCodeWithDetails should return success for valid codes`() {
        // Given
        val validCode = "USD"
        
        // When
        val result = currencyValidator.validateCurrencyCodeWithDetails(validCode)
        
        // Then
        assertTrue(result is DetailedValidationResult.Success, "Should return success")
        assertEquals(Currency.USD, (result as DetailedValidationResult.Success).currency, "Should return correct currency")
    }
    
    @Test
    fun `validateCurrencyCodeWithDetails should return detailed error for empty code`() {
        // Given
        val emptyCode = ""
        
        // When
        val result = currencyValidator.validateCurrencyCodeWithDetails(emptyCode)
        
        // Then
        assertTrue(result is DetailedValidationResult.Error, "Should return error")
        val error = result as DetailedValidationResult.Error
        assertEquals(CurrencyErrorType.EMPTY_CODE, error.errorType, "Should have correct error type")
        assertEquals("Currency code cannot be empty", error.message, "Should have correct message")
        assertTrue(error.suggestedFix.isNotEmpty(), "Should have suggestion")
    }
    
    @Test
    fun `validateCurrencyCodeWithDetails should return detailed error for invalid length`() {
        // Given
        val invalidCode = "US"
        
        // When
        val result = currencyValidator.validateCurrencyCodeWithDetails(invalidCode)
        
        // Then
        assertTrue(result is DetailedValidationResult.Error, "Should return error")
        val error = result as DetailedValidationResult.Error
        assertEquals(CurrencyErrorType.INVALID_LENGTH, error.errorType, "Should have correct error type")
        assertEquals("Currency code must be exactly 3 characters", error.message, "Should have correct message")
        assertTrue(error.suggestedFix.isNotEmpty(), "Should have suggestion")
    }
    
    @Test
    fun `validateCurrencyCodeWithDetails should return detailed error for unsupported code`() {
        // Given
        val unsupportedCode = "XXX"
        
        // When
        val result = currencyValidator.validateCurrencyCodeWithDetails(unsupportedCode)
        
        // Then
        assertTrue(result is DetailedValidationResult.Error, "Should return error")
        val error = result as DetailedValidationResult.Error
        assertEquals(CurrencyErrorType.UNSUPPORTED_CODE, error.errorType, "Should have correct error type")
        assertEquals("Unsupported currency code: XXX", error.message, "Should have correct message")
        assertTrue(error.suggestedFix.isNotEmpty(), "Should have suggestion")
    }
}
