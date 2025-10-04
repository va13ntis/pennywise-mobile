package com.pennywise.app.domain.validation

import android.content.Context
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CurrencyErrorHandlerTest {
    
    private lateinit var currencyErrorHandler: CurrencyErrorHandler
    private lateinit var mockContext: Context
    
    @BeforeEach
    fun setUp() {
        currencyErrorHandler = CurrencyErrorHandler()
        mockContext = mockk<Context>()
    }
    
    @Test
    fun `getUserFriendlyErrorMessage should return correct message for empty code`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessage(errorType)
        
        // Then
        assertEquals("Please select a currency", result, "Should return correct message")
    }
    
    @Test
    fun `getUserFriendlyErrorMessage should return correct message for invalid length`() {
        // Given
        val errorType = CurrencyErrorType.INVALID_LENGTH
        val invalidValue = "US"
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessage(errorType, invalidValue)
        
        // Then
        assertEquals("Currency code must be exactly 3 characters", result, "Should return correct message")
    }
    
    @Test
    fun `getUserFriendlyErrorMessage should return correct message for unsupported code`() {
        // Given
        val errorType = CurrencyErrorType.UNSUPPORTED_CODE
        val invalidValue = "XXX"
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessage(errorType, invalidValue)
        
        // Then
        assertEquals("Currency 'XXX' is not supported", result, "Should return correct message")
    }
    
    @Test
    fun `getUserFriendlyErrorMessage should return correct message for invalid amount`() {
        // Given
        val errorType = CurrencyErrorType.INVALID_AMOUNT
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessage(errorType)
        
        // Then
        assertEquals("Please enter a valid amount", result, "Should return correct message")
    }
    
    @Test
    fun `getUserFriendlyErrorMessage should use context strings when available`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        every { mockContext.getString(R.string.currency_error_empty_code) } returns "Custom empty code message"
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessage(errorType, context = mockContext)
        
        // Then
        assertEquals("Custom empty code message", result, "Should use context string")
    }
    
    @Test
    fun `getUserFriendlyErrorMessageWithSuggestions should include suggestions for unsupported code`() {
        // Given
        val errorType = CurrencyErrorType.UNSUPPORTED_CODE
        val invalidValue = "US"
        val suggestions = listOf("USD", "EUR", "GBP")
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessageWithSuggestions(
            errorType, invalidValue, suggestions
        )
        
        // Then
        assertTrue(result.contains("USD, EUR, GBP"), "Should include suggestions")
        assertTrue(result.contains("Currency 'US' is not supported"), "Should include base message")
    }
    
    @Test
    fun `getUserFriendlyErrorMessageWithSuggestions should not include suggestions for other error types`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        val suggestions = listOf("USD", "EUR", "GBP")
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessageWithSuggestions(
            errorType, suggestions = suggestions
        )
        
        // Then
        assertEquals("Please select a currency", result, "Should not include suggestions")
    }
    
    @Test
    fun `getUserFriendlyErrorMessageWithSuggestions should use context strings when available`() {
        // Given
        val errorType = CurrencyErrorType.UNSUPPORTED_CODE
        val invalidValue = "US"
        val suggestions = listOf("USD", "EUR")
        every { mockContext.getString(R.string.currency_error_unsupported_code, invalidValue) } returns "Custom unsupported message"
        every { mockContext.getString(R.string.currency_error_with_suggestions) } returns "Custom message with %s, %s"
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessageWithSuggestions(
            errorType, invalidValue, suggestions, mockContext
        )
        
        // Then
        assertEquals("Custom message with Custom unsupported message, USD, EUR", result, "Should use context string")
    }
    
    @Test
    fun `getRecoverySuggestion should return correct suggestion for empty code`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType)
        
        // Then
        assertEquals("Please select a currency from the dropdown", result, "Should return correct suggestion")
    }
    
    @Test
    fun `getRecoverySuggestion should return correct suggestion for invalid length`() {
        // Given
        val errorType = CurrencyErrorType.INVALID_LENGTH
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType)
        
        // Then
        assertEquals("Please enter a 3-letter currency code", result, "Should return correct suggestion")
    }
    
    @Test
    fun `getRecoverySuggestion should return correct suggestion for unsupported code`() {
        // Given
        val errorType = CurrencyErrorType.UNSUPPORTED_CODE
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType)
        
        // Then
        assertEquals("Please select from the supported currencies", result, "Should return correct suggestion")
    }
    
    @Test
    fun `getRecoverySuggestion should return correct suggestion for invalid amount`() {
        // Given
        val errorType = CurrencyErrorType.INVALID_AMOUNT
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType)
        
        // Then
        assertEquals("Please enter a valid positive number", result, "Should return correct suggestion")
    }
    
    @Test
    fun `getRecoverySuggestion should use context strings when available`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        every { mockContext.getString(R.string.currency_recovery_empty_code) } returns "Custom recovery message"
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType, mockContext)
        
        // Then
        assertEquals("Custom recovery message", result, "Should use context string")
    }
    
    @Test
    fun `isCurrencyChangeSafe should return true for safe currency changes`() {
        // Given
        val oldCurrency = Currency.USD
        val newCurrency = Currency.EUR
        
        // When
        val result = currencyErrorHandler.isCurrencyChangeSafe(oldCurrency, newCurrency)
        
        // Then
        assertTrue(result, "Should return true for safe currency change")
    }
    
    @Test
    fun `getCurrencyChangeWarning should return null for safe currency changes`() {
        // Given
        val oldCurrency = Currency.USD
        val newCurrency = Currency.EUR
        
        // When
        val result = currencyErrorHandler.getCurrencyChangeWarning(oldCurrency, newCurrency)
        
        // Then
        assertNull(result, "Should return null for safe currency change")
    }
    
    @Test
    fun `createErrorReport should include all required information`() {
        // Given
        val errorType = CurrencyErrorType.UNSUPPORTED_CODE
        val invalidValue = "XXX"
        val context = "TestContext"
        val additionalInfo = mapOf("key1" to "value1", "key2" to 123)
        
        // When
        val result = currencyErrorHandler.createErrorReport(errorType, invalidValue, context, additionalInfo)
        
        // Then
        assertTrue(result.contains("UNSUPPORTED_CODE"), "Should include error type")
        assertTrue(result.contains("TestContext"), "Should include context")
        assertTrue(result.contains("XXX"), "Should include invalid value")
        assertTrue(result.contains("key1: value1"), "Should include additional info")
        assertTrue(result.contains("key2: 123"), "Should include additional info")
        assertTrue(result.contains("Timestamp:"), "Should include timestamp")
    }
    
    @Test
    fun `createErrorReport should handle missing optional parameters`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        
        // When
        val result = currencyErrorHandler.createErrorReport(errorType)
        
        // Then
        assertTrue(result.contains("EMPTY_CODE"), "Should include error type")
        assertTrue(result.contains("Unknown"), "Should include context")
        assertTrue(result.contains("Timestamp:"), "Should include timestamp")
        assertFalse(result.contains("Invalid Value:"), "Should not include invalid value section")
        assertFalse(result.contains("Additional Info:"), "Should not include additional info section")
    }
}
