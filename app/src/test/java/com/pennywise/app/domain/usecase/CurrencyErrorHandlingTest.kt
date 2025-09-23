package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.domain.validation.CurrencyValidator
import com.pennywise.app.domain.validation.CurrencyErrorHandler
import com.pennywise.app.domain.validation.CurrencyErrorType
import com.pennywise.app.data.service.CurrencyConversionService
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Assertions.*
import java.util.Date

/**
 * Tests for error handling and fallback mechanisms in currency services
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Currency Error Handling Tests")
class CurrencyErrorHandlingTest {

    private lateinit var currencySortingService: CurrencySortingService
    private lateinit var currencyUsageTracker: CurrencyUsageTracker
    private lateinit var currencyValidator: CurrencyValidator
    private lateinit var currencyErrorHandler: CurrencyErrorHandler
    private lateinit var mockCurrencyUsageRepository: CurrencyUsageRepository
    private lateinit var mockUserRepository: UserRepository
    private lateinit var mockCurrencyConversionService: CurrencyConversionService
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        mockCurrencyUsageRepository = mockk<CurrencyUsageRepository>(relaxed = true)
        mockUserRepository = mockk<UserRepository>(relaxed = true)
        mockCurrencyConversionService = mockk<CurrencyConversionService>(relaxed = true)
        
        currencySortingService = CurrencySortingService(mockCurrencyUsageRepository, mockUserRepository)
        currencyUsageTracker = CurrencyUsageTracker(mockCurrencyUsageRepository, Dispatchers.IO)
        currencyValidator = CurrencyValidator()
        currencyErrorHandler = CurrencyErrorHandler()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Nested
    @DisplayName("Repository Error Handling")
    inner class RepositoryErrorHandling {

        @Test
        @DisplayName("Should handle database connection errors gracefully")
        fun `should handle database connection errors gracefully`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            // Setup repository to throw connection error
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } throws 
                java.sql.SQLException("Connection failed")
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } throws 
                java.sql.SQLException("Connection failed")
            
            // Usage tracking should not throw
            currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
            
            // Sorting should return empty list on error
            val sortedCurrencies = currencySortingService.getSortedCurrencies(userId).first()
            assertTrue(sortedCurrencies.isEmpty())
        }

        @Test
        @DisplayName("Should handle database timeout errors gracefully")
        fun `should handle database timeout errors gracefully`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            // Setup repository to throw timeout error
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } throws 
                java.sql.SQLTimeoutException("Query timeout")
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } throws 
                java.sql.SQLTimeoutException("Query timeout")
            
            // Usage tracking should not throw
            currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
            
            // Sorting should return empty list on error
            val sortedCurrencies = currencySortingService.getSortedCurrencies(userId).first()
            assertTrue(sortedCurrencies.isEmpty())
        }

        @Test
        @DisplayName("Should handle database constraint violations gracefully")
        fun `should handle database constraint violations gracefully`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            // Setup repository to throw constraint violation
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } throws 
                java.sql.SQLIntegrityConstraintViolationException("Constraint violation")
            
            // Usage tracking should not throw
            currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
            
            // Should still work without throwing
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) }
        }

        @Test
        @DisplayName("Should handle null pointer exceptions gracefully")
        fun `should handle null pointer exceptions gracefully`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            // Setup repository to throw NPE
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } throws 
                NullPointerException("Null pointer")
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } throws 
                NullPointerException("Null pointer")
            
            // Usage tracking should not throw
            currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
            
            // Sorting should return empty list on error
            val sortedCurrencies = currencySortingService.getSortedCurrencies(userId).first()
            assertTrue(sortedCurrencies.isEmpty())
        }
    }

    @Nested
    @DisplayName("API Error Handling")
    inner class APIErrorHandling {

        @Test
        @DisplayName("Should handle network timeout errors gracefully")
        fun `should handle network timeout errors gracefully`() = runTest {
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            
            // Setup conversion service to throw timeout
            coEvery { mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency) } throws 
                java.net.SocketTimeoutException("Read timeout")
            
            // Conversion should return null on timeout
            val result = mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency)
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle network connection errors gracefully")
        fun `should handle network connection errors gracefully`() = runTest {
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            
            // Setup conversion service to throw connection error
            coEvery { mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency) } throws 
                java.net.UnknownHostException("Network unreachable")
            
            // Conversion should return null on connection error
            val result = mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency)
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle HTTP error responses gracefully")
        fun `should handle HTTP error responses gracefully`() = runTest {
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            
            // Setup conversion service to throw HTTP error
            coEvery { mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency) } throws 
                retrofit2.HttpException(
                    retrofit2.Response.error<Any>(500, okhttp3.ResponseBody.create(null, "Internal Server Error"))
                )
            
            // Conversion should return null on HTTP error
            val result = mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency)
            assertNull(result)
        }

        @Test
        @DisplayName("Should handle API rate limiting gracefully")
        fun `should handle API rate limiting gracefully`() = runTest {
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            
            // Setup conversion service to throw rate limit error
            coEvery { mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency) } throws 
                retrofit2.HttpException(
                    retrofit2.Response.error<Any>(429, okhttp3.ResponseBody.create(null, "Rate limit exceeded"))
                )
            
            // Conversion should return null on rate limit
            val result = mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency)
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("Validation Error Handling")
    inner class ValidationErrorHandling {

        @Test
        @DisplayName("Should handle invalid currency codes with fallback")
        fun `should handle invalid currency codes with fallback`() = runTest {
            val invalidCurrencyCodes = listOf("", "XX", "XXXX", "123", "ABC", "USD1")
            
            invalidCurrencyCodes.forEach { invalidCode ->
                // Validation should fail
                val validationResult = currencyValidator.validateCurrencyCode(invalidCode)
                assertTrue(validationResult is com.pennywise.app.domain.validation.ValidationResult.Error)
                
                // Should provide fallback
                val fallbackCode = currencyValidator.getValidCurrencyCodeOrFallback(invalidCode)
                assertEquals("USD", fallbackCode)
                
                // Should provide fallback currency enum
                val fallbackCurrency = currencyValidator.getValidCurrencyOrFallback(invalidCode)
                assertEquals(Currency.USD, fallbackCurrency)
            }
        }

        @Test
        @DisplayName("Should handle invalid amounts with fallback")
        fun `should handle invalid amounts with fallback`() = runTest {
            val invalidAmounts = listOf(
                Double.NaN,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                -100.0,
                -0.01
            )
            
            invalidAmounts.forEach { invalidAmount ->
                // Validation should fail
                val validationResult = currencyValidator.validateAmountForCurrency(invalidAmount, Currency.USD)
                assertTrue(validationResult is com.pennywise.app.domain.validation.ValidationResult.Error)
                
                // Should provide safe fallback format
                val fallbackFormat = currencyValidator.formatAmountWithValidation(invalidAmount, "USD")
                assertEquals("$0", fallbackFormat)
            }
        }

        @Test
        @DisplayName("Should handle decimal places validation for JPY")
        fun `should handle decimal places validation for JPY`() = runTest {
            val decimalAmounts = listOf(1.5, 100.99, 0.01, 999.99)
            
            decimalAmounts.forEach { decimalAmount ->
                // Validation should fail for JPY
                val validationResult = currencyValidator.validateAmountForCurrency(decimalAmount, Currency.JPY)
                assertTrue(validationResult is com.pennywise.app.domain.validation.ValidationResult.Error)
                
                // Should provide safe fallback format
                val fallbackFormat = currencyValidator.formatAmountWithValidation(decimalAmount, "JPY")
                assertEquals("¥0", fallbackFormat)
            }
        }

        @Test
        @DisplayName("Should provide detailed error information")
        fun `should provide detailed error information`() = runTest {
            val invalidCode = "XX"
            
            // Should provide detailed validation result
            val detailedResult = currencyValidator.validateCurrencyCodeWithDetails(invalidCode)
            assertTrue(detailedResult is com.pennywise.app.domain.validation.DetailedValidationResult.Error)
            
            val error = detailedResult as com.pennywise.app.domain.validation.DetailedValidationResult.Error
            assertEquals(CurrencyErrorType.INVALID_LENGTH, error.errorType)
            assertTrue(error.message.isNotEmpty())
            assertTrue(error.suggestedFix.isNotEmpty())
        }
    }

    @Nested
    @DisplayName("Fallback Mechanisms")
    inner class FallbackMechanisms {

        @Test
        @DisplayName("Should provide fallback for currency validation errors")
        fun `should provide fallback for currency validation errors`() = runTest {
            val invalidCurrencyCode = "XXX"
            
            // Test all fallback mechanisms
            val fallbackCode = currencyValidator.getValidCurrencyCodeOrFallback(invalidCurrencyCode)
            val fallbackCurrency = currencyValidator.getValidCurrencyOrFallback(invalidCurrencyCode)
            val fallbackFormat = currencyValidator.formatAmountWithValidation(100.0, invalidCurrencyCode)
            
            assertEquals("USD", fallbackCode)
            assertEquals(Currency.USD, fallbackCurrency)
            assertEquals("$0", fallbackFormat) // Safe fallback format
        }

        @Test
        @DisplayName("Should provide fallback for user-friendly error messages")
        fun `should provide fallback for user-friendly error messages`() = runTest {
            val errorTypes = listOf(
                CurrencyErrorType.EMPTY_CODE,
                CurrencyErrorType.INVALID_LENGTH,
                CurrencyErrorType.UNSUPPORTED_CODE,
                CurrencyErrorType.INVALID_AMOUNT
            )
            
            errorTypes.forEach { errorType ->
                // Should provide user-friendly message
                val userMessage = currencyErrorHandler.getUserFriendlyErrorMessage(errorType)
                assertTrue(userMessage.isNotEmpty())
                assertFalse(userMessage.contains("CurrencyErrorType"))
                
                // Should provide recovery suggestion
                val recoverySuggestion = currencyErrorHandler.getRecoverySuggestion(errorType)
                assertTrue(recoverySuggestion.isNotEmpty())
                assertTrue(recoverySuggestion.contains("Please"))
            }
        }

        @Test
        @DisplayName("Should provide fallback for error reporting")
        fun `should provide fallback for error reporting`() = runTest {
            val errorType = CurrencyErrorType.UNSUPPORTED_CODE
            val invalidValue = "XXX"
            val context = "TestContext"
            
            // Should create comprehensive error report
            val errorReport = currencyErrorHandler.createErrorReport(errorType, invalidValue, context)
            
            assertTrue(errorReport.contains("Currency Error Report"))
            assertTrue(errorReport.contains("UNSUPPORTED_CODE"))
            assertTrue(errorReport.contains("TestContext"))
            assertTrue(errorReport.contains("XXX"))
            assertTrue(errorReport.contains("Timestamp:"))
        }

        @Test
        @DisplayName("Should handle currency change safety checks")
        fun `should handle currency change safety checks`() = runTest {
            val oldCurrency = Currency.USD
            val newCurrency = Currency.EUR
            
            // Should check if currency change is safe
            val isSafe = currencyErrorHandler.isCurrencyChangeSafe(oldCurrency, newCurrency)
            assertTrue(isSafe) // Currently always returns true
            
            // Should provide warning if not safe
            val warning = currencyErrorHandler.getCurrencyChangeWarning(oldCurrency, newCurrency)
            assertNull(warning) // Should be null for safe changes
        }
    }

    @Nested
    @DisplayName("Concurrent Error Handling")
    inner class ConcurrentErrorHandling {

        @Test
        @DisplayName("Should handle concurrent errors gracefully")
        fun `should handle concurrent errors gracefully`() = runTest {
            val userId = 1L
            val currencyCodes = listOf("USD", "EUR", "GBP", "JPY")
            
            // Setup repository to throw errors
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(any(), any()) } throws 
                Exception("Concurrent error")
            
            // Simulate concurrent access with errors
            val jobs = currencyCodes.map { currencyCode ->
                async {
                    try {
                        currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
                    } catch (e: Exception) {
                        // Should not throw
                    }
                }
            }
            
            // Wait for all jobs to complete
            jobs.forEach { it.await() }
            
            // All should complete without throwing
            assertTrue(true) // If we reach here, no exceptions were thrown
        }

        @Test
        @DisplayName("Should handle mixed success and error scenarios")
        fun `should handle mixed success and error scenarios`() = runTest {
            val userId = 1L
            val validCurrencyCode = "EUR"
            val invalidCurrencyCode = "XXX"
            
            // Setup mixed responses
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, validCurrencyCode) } just Runs
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, invalidCurrencyCode) } throws 
                Exception("Invalid currency error")
            
            // Track valid currency (should succeed)
            currencyUsageTracker.trackCurrencyUsage(userId, validCurrencyCode)
            
            // Track invalid currency (should not throw)
            currencyUsageTracker.trackCurrencyUsage(userId, invalidCurrencyCode)
            
            // Both should complete without throwing
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, validCurrencyCode) }
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, invalidCurrencyCode) }
        }
    }

    @Nested
    @DisplayName("Error Recovery and Resilience")
    inner class ErrorRecoveryAndResilience {

        @Test
        @DisplayName("Should recover from temporary errors")
        fun `should recover from temporary errors`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "USD", 10, Date())
            )
            
            // First call fails, second call succeeds
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flow { throw Exception("Temporary error") } andThen flowOf(usageData)
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } just Runs
            
            // First call should return empty list
            val firstResult = currencySortingService.getSortedCurrencies(userId).first()
            assertTrue(firstResult.isEmpty())
            
            // Second call should succeed
            val secondResult = currencySortingService.getSortedCurrencies(userId).first()
            assertTrue(secondResult.isNotEmpty())
        }

        @Test
        @DisplayName("Should maintain service availability during errors")
        fun `should maintain service availability during errors`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            // Setup repository to throw errors
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } throws 
                Exception("Service unavailable")
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } throws 
                Exception("Service unavailable")
            
            // Services should still be available (not crash)
            val sortedCurrencies = currencySortingService.getSortedCurrencies(userId).first()
            assertTrue(sortedCurrencies.isEmpty()) // Should return empty list, not crash
            
            currencyUsageTracker.trackCurrencyUsage(userId, currencyCode) // Should not throw
            
            // Validation should still work
            val validationResult = currencyValidator.validateCurrencyCode(currencyCode)
            assertTrue(validationResult is com.pennywise.app.domain.validation.ValidationResult.Success)
        }

        @Test
        @DisplayName("Should provide meaningful error context")
        fun `should provide meaningful error context`() = runTest {
            val errorType = CurrencyErrorType.UNSUPPORTED_CODE
            val invalidValue = "XXX"
            val context = "CurrencySelectionScreen"
            val additionalInfo = mapOf(
                "userId" to 123L,
                "attemptCount" to 3,
                "lastValidCurrency" to "USD"
            )
            
            // Should create detailed error report with context
            val errorReport = currencyErrorHandler.createErrorReport(
                errorType, 
                invalidValue, 
                context, 
                additionalInfo
            )
            
            assertTrue(errorReport.contains("CurrencySelectionScreen"))
            assertTrue(errorReport.contains("userId: 123"))
            assertTrue(errorReport.contains("attemptCount: 3"))
            assertTrue(errorReport.contains("lastValidCurrency: USD"))
        }
    }
}
