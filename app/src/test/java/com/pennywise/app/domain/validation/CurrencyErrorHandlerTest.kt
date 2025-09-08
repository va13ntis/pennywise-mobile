package com.pennywise.app.domain.validation

import android.content.Context
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CurrencyErrorHandlerTest {
    
    private lateinit var currencyErrorHandler: CurrencyErrorHandler
    private lateinit var mockContext: Context
    
    @Before
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
        assertEquals("Should return correct message", "Please select a currency", result)
    }
    
    @Test
    fun `getUserFriendlyErrorMessage should return correct message for invalid length`() {
        // Given
        val errorType = CurrencyErrorType.INVALID_LENGTH
        val invalidValue = "US"
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessage(errorType, invalidValue)
        
        // Then
        assertEquals("Should return correct message", "Currency code must be exactly 3 characters", result)
    }
    
    @Test
    fun `getUserFriendlyErrorMessage should return correct message for unsupported code`() {
        // Given
        val errorType = CurrencyErrorType.UNSUPPORTED_CODE
        val invalidValue = "XXX"
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessage(errorType, invalidValue)
        
        // Then
        assertEquals("Should return correct message", "Currency 'XXX' is not supported", result)
    }
    
    @Test
    fun `getUserFriendlyErrorMessage should return correct message for invalid amount`() {
        // Given
        val errorType = CurrencyErrorType.INVALID_AMOUNT
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessage(errorType)
        
        // Then
        assertEquals("Should return correct message", "Please enter a valid amount", result)
    }
    
    @Test
    fun `getUserFriendlyErrorMessage should use context strings when available`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        every { mockContext.getString(R.string.currency_error_empty_code) } returns "Custom empty code message"
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessage(errorType, context = mockContext)
        
        // Then
        assertEquals("Should use context string", "Custom empty code message", result)
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
        assertTrue("Should include suggestions", result.contains("USD, EUR, GBP"))
        assertTrue("Should include base message", result.contains("Currency 'US' is not supported"))
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
        assertEquals("Should not include suggestions", "Please select a currency", result)
    }
    
    @Test
    fun `getUserFriendlyErrorMessageWithSuggestions should use context strings when available`() {
        // Given
        val errorType = CurrencyErrorType.UNSUPPORTED_CODE
        val invalidValue = "US"
        val suggestions = listOf("USD", "EUR")
        every { mockContext.getString(R.string.currency_error_unsupported_code, invalidValue) } returns "Custom unsupported message"
        every { mockContext.getString(R.string.currency_error_with_suggestions, any(), any()) } returns "Custom message with USD, EUR"
        
        // When
        val result = currencyErrorHandler.getUserFriendlyErrorMessageWithSuggestions(
            errorType, invalidValue, suggestions, mockContext
        )
        
        // Then
        assertEquals("Should use context string", "Custom message with USD, EUR", result)
    }
    
    @Test
    fun `getRecoverySuggestion should return correct suggestion for empty code`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType)
        
        // Then
        assertEquals("Should return correct suggestion", "Please select a currency from the dropdown", result)
    }
    
    @Test
    fun `getRecoverySuggestion should return correct suggestion for invalid length`() {
        // Given
        val errorType = CurrencyErrorType.INVALID_LENGTH
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType)
        
        // Then
        assertEquals("Should return correct suggestion", "Please enter a 3-letter currency code", result)
    }
    
    @Test
    fun `getRecoverySuggestion should return correct suggestion for unsupported code`() {
        // Given
        val errorType = CurrencyErrorType.UNSUPPORTED_CODE
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType)
        
        // Then
        assertEquals("Should return correct suggestion", "Please select from the supported currencies", result)
    }
    
    @Test
    fun `getRecoverySuggestion should return correct suggestion for invalid amount`() {
        // Given
        val errorType = CurrencyErrorType.INVALID_AMOUNT
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType)
        
        // Then
        assertEquals("Should return correct suggestion", "Please enter a valid positive number", result)
    }
    
    @Test
    fun `getRecoverySuggestion should use context strings when available`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        every { mockContext.getString(R.string.currency_recovery_empty_code) } returns "Custom recovery message"
        
        // When
        val result = currencyErrorHandler.getRecoverySuggestion(errorType, mockContext)
        
        // Then
        assertEquals("Should use context string", "Custom recovery message", result)
    }
    
    @Test
    fun `isCurrencyChangeSafe should return true for safe currency changes`() {
        // Given
        val oldCurrency = Currency.USD
        val newCurrency = Currency.EUR
        
        // When
        val result = currencyErrorHandler.isCurrencyChangeSafe(oldCurrency, newCurrency)
        
        // Then
        assertTrue("Should return true for safe currency change", result)
    }
    
    @Test
    fun `getCurrencyChangeWarning should return null for safe currency changes`() {
        // Given
        val oldCurrency = Currency.USD
        val newCurrency = Currency.EUR
        
        // When
        val result = currencyErrorHandler.getCurrencyChangeWarning(oldCurrency, newCurrency)
        
        // Then
        assertNull("Should return null for safe currency change", result)
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
        assertTrue("Should include error type", result.contains("UNSUPPORTED_CODE"))
        assertTrue("Should include context", result.contains("TestContext"))
        assertTrue("Should include invalid value", result.contains("XXX"))
        assertTrue("Should include additional info", result.contains("key1: value1"))
        assertTrue("Should include additional info", result.contains("key2: 123"))
        assertTrue("Should include timestamp", result.contains("Timestamp:"))
    }
    
    @Test
    fun `createErrorReport should handle missing optional parameters`() {
        // Given
        val errorType = CurrencyErrorType.EMPTY_CODE
        
        // When
        val result = currencyErrorHandler.createErrorReport(errorType)
        
        // Then
        assertTrue("Should include error type", result.contains("EMPTY_CODE"))
        assertTrue("Should include context", result.contains("Unknown"))
        assertTrue("Should include timestamp", result.contains("Timestamp:"))
        assertFalse("Should not include invalid value section", result.contains("Invalid Value:"))
        assertFalse("Should not include additional info section", result.contains("Additional Info:"))
    }
}
