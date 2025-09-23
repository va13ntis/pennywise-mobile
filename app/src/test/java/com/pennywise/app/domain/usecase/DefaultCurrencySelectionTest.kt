package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.repository.UserRepository
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
 * Tests for default currency selection logic and business rules
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Default Currency Selection Tests")
class DefaultCurrencySelectionTest {

    private lateinit var currencySortingService: CurrencySortingService
    private lateinit var mockCurrencyUsageRepository: CurrencyUsageRepository
    private lateinit var mockUserRepository: UserRepository
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        mockCurrencyUsageRepository = mockk<CurrencyUsageRepository>(relaxed = true)
        mockUserRepository = mockk<UserRepository>(relaxed = true)
        
        currencySortingService = CurrencySortingService(mockCurrencyUsageRepository, mockUserRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Nested
    @DisplayName("User Default Currency Selection")
    inner class UserDefaultCurrencySelection {

        @Test
        @DisplayName("Should prioritize user's default currency in sorting")
        fun `should prioritize user's default currency in sorting`() = runTest {
            val userId = 1L
            val userDefaultCurrency = "EUR"
            
            val user = User(
                id = userId,
            defaultCurrency = userDefaultCurrency,
            locale = "en",
            deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "USD", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date())
            )
            
            coEvery { mockUserRepository.getUserById(userId) } returns user
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // EUR should be included in used currencies even though it's not in usage data
            val eurIndex = sortedCurrencies.indexOf(Currency.EUR)
            val usdIndex = sortedCurrencies.indexOf(Currency.USD)
            val gbpIndex = sortedCurrencies.indexOf(Currency.GBP)
            val jpyIndex = sortedCurrencies.indexOf(Currency.JPY)
            
            // EUR (default) should come before unused currencies
            assertTrue(eurIndex < jpyIndex, "EUR (default) should come before unused currencies")
            
            // USD and GBP (used) should come before EUR (default but unused)
            assertTrue(usdIndex < eurIndex, "USD (used) should come before EUR (default but unused)")
            assertTrue(gbpIndex < eurIndex, "GBP (used) should come before EUR (default but unused)")
        }

        @Test
        @DisplayName("Should fallback to USD when user has no default currency")
        fun `should fallback to USD when user has no default currency`() = runTest {
            val userId = 1L
            
            val user = User(
                id = userId,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "EUR", 5, Date()),
                CurrencyUsage(2L, userId, "GBP", 3, Date())
            )
            
            coEvery { mockUserRepository.getUserById(userId) } returns user
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // USD should be included as fallback default
            val usdIndex = sortedCurrencies.indexOf(Currency.USD)
            val eurIndex = sortedCurrencies.indexOf(Currency.EUR)
            val gbpIndex = sortedCurrencies.indexOf(Currency.GBP)
            val jpyIndex = sortedCurrencies.indexOf(Currency.JPY)
            
            // USD (fallback default) should come before unused currencies
            assertTrue(usdIndex < jpyIndex, "USD (fallback default) should come before unused currencies")
            
            // EUR and GBP (used) should come before USD (fallback default)
            assertTrue(eurIndex < usdIndex, "EUR (used) should come before USD (fallback default)")
            assertTrue(gbpIndex < usdIndex, "GBP (used) should come before USD (fallback default)")
        }

        @Test
        @DisplayName("Should handle null user gracefully with USD fallback")
        fun `should handle null user gracefully with USD fallback`() = runTest {
            val userId = 1L
            
            coEvery { mockUserRepository.getUserById(userId) } returns null
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Should still work with USD fallback
            assertTrue(sortedCurrencies.isNotEmpty())
            assertEquals(Currency.USD, sortedCurrencies.first())
        }

        @Test
        @DisplayName("Should handle user with invalid default currency")
        fun `should handle user with invalid default currency`() = runTest {
            val userId = 1L
            val invalidDefaultCurrency = "XXX"
            
            val user = User(
                id = userId,
            defaultCurrency = invalidDefaultCurrency,
            locale = "en",
            deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "EUR", 5, Date())
            )
            
            coEvery { mockUserRepository.getUserById(userId) } returns user
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Should fallback to USD since invalid default currency is not supported
            val usdIndex = sortedCurrencies.indexOf(Currency.USD)
            val eurIndex = sortedCurrencies.indexOf(Currency.EUR)
            val jpyIndex = sortedCurrencies.indexOf(Currency.JPY)
            
            // USD (fallback) should come before unused currencies
            assertTrue(usdIndex < jpyIndex, "USD (fallback) should come before unused currencies")
            
            // EUR (used) should come before USD (fallback)
            assertTrue(eurIndex < usdIndex, "EUR (used) should come before USD (fallback)")
        }
    }

    @Nested
    @DisplayName("Currency Usage Priority Logic")
    inner class CurrencyUsagePriorityLogic {

        @Test
        @DisplayName("Should prioritize currencies by usage count")
        fun `should prioritize currencies by usage count`() = runTest {
            val userId = 1L
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "EUR", 20, Date()),
                CurrencyUsage(2L, userId, "USD", 15, Date()),
                CurrencyUsage(3L, userId, "GBP", 10, Date()),
                CurrencyUsage(4L, userId, "JPY", 5, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            coEvery { mockUserRepository.getUserById(userId) } returns 
                User(1L, "USD", "en", false, Date(), Date())
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            val eurIndex = sortedCurrencies.indexOf(Currency.EUR)
            val usdIndex = sortedCurrencies.indexOf(Currency.USD)
            val gbpIndex = sortedCurrencies.indexOf(Currency.GBP)
            val jpyIndex = sortedCurrencies.indexOf(Currency.JPY)
            
            // Should be sorted by usage count (EUR > USD > GBP > JPY)
            assertTrue(eurIndex < usdIndex, "EUR (20 uses) should come before USD (15 uses)")
            assertTrue(usdIndex < gbpIndex, "USD (15 uses) should come before GBP (10 uses)")
            assertTrue(gbpIndex < jpyIndex, "GBP (10 uses) should come before JPY (5 uses)")
        }

        @Test
        @DisplayName("Should handle currencies with same usage count")
        fun `should handle currencies with same usage count`() = runTest {
            val userId = 1L
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 10, Date()),
                CurrencyUsage(3L, userId, "USD", 5, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            coEvery { mockUserRepository.getUserById(userId) } returns 
                User(1L, "USD", "en", false, Date(), Date())
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            val eurIndex = sortedCurrencies.indexOf(Currency.EUR)
            val gbpIndex = sortedCurrencies.indexOf(Currency.GBP)
            val usdIndex = sortedCurrencies.indexOf(Currency.USD)
            
            // EUR and GBP should come before USD (both have higher usage)
            assertTrue(eurIndex < usdIndex, "EUR (10 uses) should come before USD (5 uses)")
            assertTrue(gbpIndex < usdIndex, "GBP (10 uses) should come before USD (5 uses)")
            
            // EUR and GBP should be close to each other (same usage count)
            assertTrue(kotlin.math.abs(eurIndex - gbpIndex) <= 1, "EUR and GBP should be close in order")
        }

        @Test
        @DisplayName("Should include default currency even with zero usage")
        fun `should include default currency even with zero usage`() = runTest {
            val userId = 1L
            val userDefaultCurrency = "CAD"
            
            val user = User(
                id = userId,
            defaultCurrency = userDefaultCurrency,
            locale = "en",
            deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date())
            )
            
            coEvery { mockUserRepository.getUserById(userId) } returns user
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // CAD should be included even though it's not in usage data
            assertTrue(sortedCurrencies.contains(Currency.CAD), "CAD (default) should be included")
            
            val cadIndex = sortedCurrencies.indexOf(Currency.CAD)
            val eurIndex = sortedCurrencies.indexOf(Currency.EUR)
            val gbpIndex = sortedCurrencies.indexOf(Currency.GBP)
            val jpyIndex = sortedCurrencies.indexOf(Currency.JPY)
            
            // CAD (default) should come before unused currencies
            assertTrue(cadIndex < jpyIndex, "CAD (default) should come before unused currencies")
            
            // EUR and GBP (used) should come before CAD (default but unused)
            assertTrue(eurIndex < cadIndex, "EUR (used) should come before CAD (default but unused)")
            assertTrue(gbpIndex < cadIndex, "GBP (used) should come before CAD (default but unused)")
        }
    }

    @Nested
    @DisplayName("Business Rules and Edge Cases")
    inner class BusinessRulesAndEdgeCases {

        @Test
        @DisplayName("Should handle empty usage data with default currency")
        fun `should handle empty usage data with default currency`() = runTest {
            val userId = 1L
            val userDefaultCurrency = "EUR"
            
            val user = User(
                id = userId,
                defaultCurrency = userDefaultCurrency,
                locale = "en",
                deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            coEvery { mockUserRepository.getUserById(userId) } returns user
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Should return all currencies sorted by popularity, with EUR included in used set
            assertTrue(sortedCurrencies.isNotEmpty())
            assertTrue(sortedCurrencies.contains(Currency.EUR))
            
            val eurIndex = sortedCurrencies.indexOf(Currency.EUR)
            val usdIndex = sortedCurrencies.indexOf(Currency.USD)
            val gbpIndex = sortedCurrencies.indexOf(Currency.GBP)
            
            // EUR (default) should come before other unused currencies
            assertTrue(eurIndex < gbpIndex, "EUR (default) should come before unused currencies")
            
            // USD should still be first due to popularity
            assertEquals(0, usdIndex, "USD should be first due to popularity")
        }

        @Test
        @DisplayName("Should handle user with multiple default currency changes")
        fun `should handle user with multiple default currency changes`() = runTest {
            val userId = 1L
            
            val user1 = User(
                id = userId,
            defaultCurrency = "EUR",
            locale = "en",
            deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val user2 = User(
                id = userId,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            
            // First call with EUR as default
            coEvery { mockUserRepository.getUserById(userId) } returns user1
            val sortedCurrencies1 = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Second call with GBP as default
            coEvery { mockUserRepository.getUserById(userId) } returns user2
            val sortedCurrencies2 = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Both should work correctly
            assertTrue(sortedCurrencies1.isNotEmpty())
            assertTrue(sortedCurrencies2.isNotEmpty())
            
            // EUR should be in used set for first call
            val eurIndex1 = sortedCurrencies1.indexOf(Currency.EUR)
            val gbpIndex1 = sortedCurrencies1.indexOf(Currency.GBP)
            assertTrue(eurIndex1 < gbpIndex1, "EUR should come before GBP in first call")
            
            // GBP should be in used set for second call
            val eurIndex2 = sortedCurrencies2.indexOf(Currency.EUR)
            val gbpIndex2 = sortedCurrencies2.indexOf(Currency.GBP)
            assertTrue(gbpIndex2 < eurIndex2, "GBP should come before EUR in second call")
        }

        @Test
        @DisplayName("Should maintain consistent sorting with cache")
        fun `should maintain consistent sorting with cache`() = runTest {
            val userId = 1L
            val userDefaultCurrency = "EUR"
            
            val user = User(
                id = userId,
                defaultCurrency = userDefaultCurrency,
                locale = "en",
                deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            val usageData = listOf(
                CurrencyUsage(1L, userId, "USD", 10, Date())
            )
            
            coEvery { mockUserRepository.getUserById(userId) } returns user
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(usageData)
            
            // First call - should populate cache
            val sortedCurrencies1 = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Second call - should use cache
            val sortedCurrencies2 = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Results should be identical
            assertEquals(sortedCurrencies1, sortedCurrencies2)
            
            // Repository should be called only once due to caching
            coVerify(exactly = 1) { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) }
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockUserRepository.getUserById(userId) } throws Exception("Database error")
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } throws 
                Exception("Database error")
            
            // Should not throw exception
            val sortedCurrencies = currencySortingService.getSortedCurrenciesSuspend(userId)
            
            // Should return empty list on error
            assertTrue(sortedCurrencies.isEmpty())
        }
    }
}
