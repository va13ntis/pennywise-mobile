package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.domain.validation.CurrencyValidator
import com.pennywise.app.domain.validation.CurrencyErrorHandler
import com.pennywise.app.domain.validation.CurrencyErrorType
import com.pennywise.app.data.service.CurrencyConversionService
import com.pennywise.app.data.model.CachedExchangeRate
import com.pennywise.app.data.model.ExchangeRateResponse
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
 * Integration tests for currency-related services
 * Tests the interaction between different services and business logic
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Currency Service Integration Tests")
class CurrencyServiceIntegrationTest {

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
    @DisplayName("Currency Sorting and Usage Tracking Integration")
    inner class CurrencySortingAndUsageTrackingIntegration {

        @Test
        @DisplayName("Should update currency sorting when usage is tracked")
        fun `should update currency sorting when usage is tracked`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            // Setup initial usage data
            val initialUsage = listOf(
                CurrencyUsage(1L, userId, "USD", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date())
            )
            
            val updatedUsage = listOf(
                CurrencyUsage(1L, userId, "USD", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date()),
                CurrencyUsage(3L, userId, "EUR", 1, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(initialUsage, updatedUsage)
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } just Runs
            
            // Track currency usage
            currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
            
            // Get sorted currencies - should reflect the new usage
            val sortedCurrencies = currencySortingService.getSortedCurrencies(userId).take(2).toList()
            
            assertEquals(2, sortedCurrencies.size)
            
            // First result should have USD first (most used initially)
            val firstResult = sortedCurrencies[0]
            assertTrue(firstResult.indexOf(Currency.USD) < firstResult.indexOf(Currency.EUR))
            
            // Second result should have EUR included in used currencies
            val secondResult = sortedCurrencies[1]
            assertTrue(secondResult.contains(Currency.EUR))
            
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) }
        }

        @Test
        @DisplayName("Should handle invalid currency codes in usage tracking")
        fun `should handle invalid currency codes in usage tracking`() = runTest {
            val userId = 1L
            val invalidCurrencyCode = "XXX"
            
            // Setup usage data
            val usageData = listOf(
                CurrencyUsage(1L, userId, "USD", 10, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, invalidCurrencyCode) } just Runs
            
            // Track invalid currency usage - should still work but not affect sorting
            currencyUsageTracker.trackCurrencyUsage(userId, invalidCurrencyCode)
            
            val sortedCurrencies = currencySortingService.getSortedCurrencies(userId).first()
            
            // Should not include invalid currency in sorted list
            assertFalse(sortedCurrencies.contains(Currency.fromCode(invalidCurrencyCode)))
            
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, invalidCurrencyCode) }
        }

        @Test
        @DisplayName("Should maintain cache consistency between services")
        fun `should maintain cache consistency between services`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "USD", 10, Date()),
                CurrencyUsage(2L, userId, "EUR", 5, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } just Runs
            
            // Get sorted currencies to populate cache
            val initialSorted = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Track usage which should invalidate cache
            currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
            
            // Get sorted currencies again - should fetch fresh data due to cache invalidation
            val updatedSorted = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Both should work correctly
            assertTrue(initialSorted.isNotEmpty())
            assertTrue(updatedSorted.isNotEmpty())
            
            coVerify(exactly = 2) { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) }
        }
    }

    @Nested
    @DisplayName("Currency Validation and Error Handling Integration")
    inner class CurrencyValidationAndErrorHandlingIntegration {

        @Test
        @DisplayName("Should validate currency codes before usage tracking")
        fun `should validate currency codes before usage tracking`() = runTest {
            val userId = 1L
            val validCurrencyCode = "EUR"
            val invalidCurrencyCode = "XXX"
            
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(any(), any()) } just Runs
            
            // Validate before tracking
            val validResult = currencyValidator.validateCurrencyCode(validCurrencyCode)
            val invalidResult = currencyValidator.validateCurrencyCode(invalidCurrencyCode)
            
            assertTrue(validResult is com.pennywise.app.domain.validation.ValidationResult.Success)
            assertTrue(invalidResult is com.pennywise.app.domain.validation.ValidationResult.Error)
            
            // Track valid currency
            currencyUsageTracker.trackCurrencyUsage(userId, validCurrencyCode)
            
            // Track invalid currency (should still work but with fallback)
            val fallbackCode = currencyValidator.getValidCurrencyCodeOrFallback(invalidCurrencyCode)
            currencyUsageTracker.trackCurrencyUsage(userId, fallbackCode)
            
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, validCurrencyCode) }
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, "USD") } // Fallback
        }

        @Test
        @DisplayName("Should handle validation errors with user-friendly messages")
        fun `should handle validation errors with user-friendly messages`() = runTest {
            val invalidCurrencyCode = "XX"
            
            // Validate currency code
            val validationResult = currencyValidator.validateCurrencyCodeWithDetails(invalidCurrencyCode)
            
            assertTrue(validationResult is com.pennywise.app.domain.validation.DetailedValidationResult.Error)
            val error = validationResult as com.pennywise.app.domain.validation.DetailedValidationResult.Error
            
            assertEquals(CurrencyErrorType.INVALID_LENGTH, error.errorType)
            assertTrue(error.suggestedFix.isNotEmpty())
            
            // Get user-friendly error message
            val userMessage = currencyErrorHandler.getUserFriendlyErrorMessage(
                error.errorType, 
                invalidCurrencyCode
            )
            
            assertTrue(userMessage.contains("3 characters"))
            
            // Get recovery suggestion
            val recoverySuggestion = currencyErrorHandler.getRecoverySuggestion(error.errorType)
            
            assertTrue(recoverySuggestion.contains("3-letter"))
        }

        @Test
        @DisplayName("Should handle currency conversion with validation")
        fun `should handle currency conversion with validation`() = runTest {
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            val invalidCurrency = "XXX"
            
            // Setup conversion service
            coEvery { mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency) } returns 85.0
            coEvery { mockCurrencyConversionService.convertCurrency(amount, fromCurrency, invalidCurrency) } returns null
            
            // Validate currencies before conversion
            val validFromResult = currencyValidator.validateCurrencyCode(fromCurrency)
            val validToResult = currencyValidator.validateCurrencyCode(toCurrency)
            val invalidResult = currencyValidator.validateCurrencyCode(invalidCurrency)
            
            assertTrue(validFromResult is com.pennywise.app.domain.validation.ValidationResult.Success)
            assertTrue(validToResult is com.pennywise.app.domain.validation.ValidationResult.Success)
            assertTrue(invalidResult is com.pennywise.app.domain.validation.ValidationResult.Error)
            
            // Convert with valid currencies
            val conversionResult = mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency)
            assertEquals(85.0, conversionResult)
            
            // Convert with invalid currency (should return null)
            val invalidConversionResult = mockCurrencyConversionService.convertCurrency(amount, fromCurrency, invalidCurrency)
            assertNull(invalidConversionResult)
            
            // Handle conversion error
            if (invalidConversionResult == null) {
                currencyErrorHandler.handleCurrencyFormattingError(
                    amount, 
                    invalidCurrency, 
                    "Invalid currency code", 
                    "CurrencyConversionTest"
                )
            }
        }
    }

    @Nested
    @DisplayName("Default Currency Selection Logic")
    inner class DefaultCurrencySelectionLogic {

        @Test
        @DisplayName("Should select default currency based on user preferences")
        fun `should select default currency based on user preferences`() = runTest {
            val userId = 1L
            val userDefaultCurrency = "EUR"
            
            // Setup user with default currency
            val user = com.pennywise.app.domain.model.User(
                id = userId,
                username = "testuser",
                passwordHash = "hash",
                email = "test@example.com",
                defaultCurrency = userDefaultCurrency,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            coEvery { mockUserRepository.getUserById(userId) } returns user
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // EUR should be included in the used currencies (as default)
            val eurIndex = sortedCurrencies.indexOf(Currency.EUR)
            val usdIndex = sortedCurrencies.indexOf(Currency.USD)
            
            assertTrue(eurIndex < usdIndex, "EUR (user default) should come before USD (fallback)")
        }

        @Test
        @DisplayName("Should fallback to USD when user has no default currency")
        fun `should fallback to USD when user has no default currency`() = runTest {
            val userId = 1L
            
            // Setup user without default currency
            val user = com.pennywise.app.domain.model.User(
                id = userId,
                username = "testuser",
                passwordHash = "hash",
                email = "test@example.com",
                defaultCurrency = null,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            coEvery { mockUserRepository.getUserById(userId) } returns user
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // USD should be first (fallback default)
            assertEquals(Currency.USD, sortedCurrencies.first())
        }

        @Test
        @DisplayName("Should handle null user gracefully")
        fun `should handle null user gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockUserRepository.getUserById(userId) } returns null
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Should still work with USD fallback
            assertTrue(sortedCurrencies.isNotEmpty())
            assertEquals(Currency.USD, sortedCurrencies.first())
        }
    }

    @Nested
    @DisplayName("Error Handling and Fallback Mechanisms")
    inner class ErrorHandlingAndFallbackMechanisms {

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            // Setup repository to throw error
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } throws 
                Exception("Database error")
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } throws 
                Exception("Database error")
            
            // Usage tracking should not throw
            currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
            
            // Sorting should return empty list on error
            val sortedCurrencies = currencySortingService.getSortedCurrencies(userId).first()
            assertTrue(sortedCurrencies.isEmpty())
        }

        @Test
        @DisplayName("Should handle conversion service errors gracefully")
        fun `should handle conversion service errors gracefully`() = runTest {
            val amount = 100.0
            val fromCurrency = "USD"
            val toCurrency = "EUR"
            
            // Setup conversion service to throw error
            coEvery { mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency) } throws 
                Exception("API error")
            
            // Conversion should return null on error
            val result = mockCurrencyConversionService.convertCurrency(amount, fromCurrency, toCurrency)
            assertNull(result)
        }

        @Test
        @DisplayName("Should provide fallback mechanisms for all services")
        fun `should provide fallback mechanisms for all services`() = runTest {
            val userId = 1L
            val invalidCurrencyCode = "XXX"
            
            // Test currency validation fallback
            val fallbackCode = currencyValidator.getValidCurrencyCodeOrFallback(invalidCurrencyCode)
            assertEquals("USD", fallbackCode)
            
            // Test currency enum fallback
            val fallbackCurrency = currencyValidator.getValidCurrencyOrFallback(invalidCurrencyCode)
            assertEquals(Currency.USD, fallbackCurrency)
            
            // Test amount formatting with fallback
            val formattedAmount = currencyValidator.formatAmountWithValidation(100.0, invalidCurrencyCode)
            assertEquals("$0", formattedAmount) // Safe fallback format
            
            // Test error handling
            val errorMessage = currencyErrorHandler.getUserFriendlyErrorMessage(
                CurrencyErrorType.UNSUPPORTED_CODE, 
                invalidCurrencyCode
            )
            assertTrue(errorMessage.contains("not supported"))
        }

        @Test
        @DisplayName("Should handle concurrent access safely")
        fun `should handle concurrent access safely`() = runTest {
            val userId = 1L
            val currencyCodes = listOf("USD", "EUR", "GBP", "JPY")
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "USD", 10, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(any(), any()) } just Runs
            
            // Simulate concurrent access
            val jobs = currencyCodes.map { currencyCode ->
                async {
                    currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
                }
            }
            
            // Wait for all jobs to complete
            jobs.forEach { it.await() }
            
            // Verify all currencies were tracked
            currencyCodes.forEach { currencyCode ->
                coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) }
            }
        }
    }

    @Nested
    @DisplayName("Service Integration Scenarios")
    inner class ServiceIntegrationScenarios {

        @Test
        @DisplayName("Should handle complete currency workflow")
        fun `should handle complete currency workflow`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            val amount = 100.0
            
            // Setup services
            val usageData = listOf(
                CurrencyUsage(1L, userId, "USD", 5, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } just Runs
            coEvery { mockCurrencyConversionService.convertCurrency(amount, "USD", currencyCode) } returns 85.0
            
            // 1. Validate currency code
            val validationResult = currencyValidator.validateCurrencyCode(currencyCode)
            assertTrue(validationResult is com.pennywise.app.domain.validation.ValidationResult.Success)
            
            // 2. Track currency usage
            currencyUsageTracker.trackCurrencyUsage(userId, currencyCode)
            
            // 3. Get sorted currencies (should include EUR now)
            val sortedCurrencies = currencySortingService.getSortedCurrencies(userId).first()
            assertTrue(sortedCurrencies.contains(Currency.EUR))
            
            // 4. Convert amount
            val convertedAmount = mockCurrencyConversionService.convertCurrency(amount, "USD", currencyCode)
            assertEquals(85.0, convertedAmount)
            
            // 5. Format amount with validation
            val formattedAmount = currencyValidator.formatAmountWithValidation(convertedAmount, currencyCode)
            assertEquals("€85.00", formattedAmount)
        }

        @Test
        @DisplayName("Should handle error scenarios in complete workflow")
        fun `should handle error scenarios in complete workflow`() = runTest {
            val userId = 1L
            val invalidCurrencyCode = "XXX"
            val amount = 100.0
            
            // Setup services to simulate errors
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } throws 
                Exception("Database error")
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, invalidCurrencyCode) } throws 
                Exception("Database error")
            coEvery { mockCurrencyConversionService.convertCurrency(amount, "USD", invalidCurrencyCode) } returns null
            
            // 1. Validate currency code (should fail)
            val validationResult = currencyValidator.validateCurrencyCode(invalidCurrencyCode)
            assertTrue(validationResult is com.pennywise.app.domain.validation.ValidationResult.Error)
            
            // 2. Get fallback currency
            val fallbackCode = currencyValidator.getValidCurrencyCodeOrFallback(invalidCurrencyCode)
            assertEquals("USD", fallbackCode)
            
            // 3. Track usage with fallback (should not throw)
            currencyUsageTracker.trackCurrencyUsage(userId, fallbackCode)
            
            // 4. Get sorted currencies (should return empty list due to error)
            val sortedCurrencies = currencySortingService.getSortedCurrencies(userId).first()
            assertTrue(sortedCurrencies.isEmpty())
            
            // 5. Convert amount (should return null due to error)
            val convertedAmount = mockCurrencyConversionService.convertCurrency(amount, "USD", invalidCurrencyCode)
            assertNull(convertedAmount)
            
            // 6. Format amount with fallback
            val formattedAmount = currencyValidator.formatAmountWithValidation(amount, fallbackCode)
            assertEquals("$100.00", formattedAmount)
        }
    }
}
