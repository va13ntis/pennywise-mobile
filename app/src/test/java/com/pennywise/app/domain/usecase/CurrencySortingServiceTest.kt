package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.repository.UserRepository
import io.mockk.*
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
import io.mockk.junit5.MockKExtension
import java.util.Date

/**
 * Unit tests for CurrencySortingService
 * Tests currency sorting logic, caching, user usage patterns, and reactive updates
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Currency Sorting Service Tests")
class CurrencySortingServiceTest {

    private lateinit var service: CurrencySortingService
    private lateinit var mockCurrencyUsageRepository: CurrencyUsageRepository
    private lateinit var mockUserRepository: UserRepository
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        mockCurrencyUsageRepository = mockk<CurrencyUsageRepository>(relaxed = true)
        mockUserRepository = mockk<UserRepository>(relaxed = true)
        
        service = CurrencySortingService(mockCurrencyUsageRepository, mockUserRepository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Nested
    @DisplayName("getSortedCurrencies Method")
    inner class GetSortedCurrenciesMethod {

        @Test
        @DisplayName("Should return currencies sorted by usage when user has usage data")
        fun `should return currencies sorted by usage when user has usage data`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date()),
                CurrencyUsage(3L, userId, "JPY", 3, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            
            val result = service.getSortedCurrencies(userId).first()
            
            // Should have EUR first (most used), then GBP, then JPY, then others by popularity
            val eurIndex = result.indexOf(Currency.EUR)
            val gbpIndex = result.indexOf(Currency.GBP)
            val jpyIndex = result.indexOf(Currency.JPY)
            val usdIndex = result.indexOf(Currency.USD)
            
            assertTrue(eurIndex < gbpIndex, "EUR should come before GBP")
            assertTrue(gbpIndex < jpyIndex, "GBP should come before JPY")
            assertTrue(jpyIndex < usdIndex, "Used currencies should come before unused ones")
        }

        @Test
        @DisplayName("Should return currencies sorted by popularity when user has no usage data")
        fun `should return currencies sorted by popularity when user has no usage data`() = runTest {
            val userId = 1L
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            
            val result = service.getSortedCurrencies(userId).first()
            
            // Should be sorted by popularity (USD first, then EUR, etc.)
            assertEquals(Currency.USD, result.first())
            assertEquals(Currency.EUR, result[1])
            assertEquals(Currency.GBP, result[2])
        }

        @Test
        @DisplayName("Should handle errors gracefully and return cached data")
        fun `should handle errors gracefully and return cached data`() = runTest {
            val userId = 1L
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flow { throw Exception("Database error") }
            
            val result = service.getSortedCurrencies(userId).first()
            
            // Should return empty list when no cache is available
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("Should emit updates when usage data changes")
        fun `should emit updates when usage data changes`() = runTest {
            val userId = 1L
            val initialUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 5, Date())
            )
            val updatedUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 5, Date()),
                CurrencyUsage(2L, userId, "GBP", 3, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(initialUsage, updatedUsage)
            
            val results = service.getSortedCurrencies(userId).take(2).toList()
            
            assertEquals(2, results.size)
            
            // First result should have EUR first
            val firstResult = results[0]
            assertTrue(firstResult.indexOf(Currency.EUR) < firstResult.indexOf(Currency.GBP))
            
            // Second result should have EUR first, then GBP
            val secondResult = results[1]
            assertTrue(secondResult.indexOf(Currency.EUR) < secondResult.indexOf(Currency.GBP))
        }
    }

    @Nested
    @DisplayName("getSortedCurrenciesSuspend Method")
    inner class GetSortedCurrenciesSuspendMethod {

        @Test
        @DisplayName("Should return cached data when cache is valid")
        fun `should return cached data when cache is valid`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date())
            )
            
            // First call to populate cache
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            coEvery { mockUserRepository.getUserById(userId) } returns 
                User(1L, "testuser", "hash", "test@example.com", "USD", Date(), Date())
            
            val firstResult = service.getSortedCurrenciesSuspend(userId)
            
            // Second call should use cache
            val secondResult = service.getSortedCurrenciesSuspend(userId)
            
            assertEquals(firstResult, secondResult)
            
            // Verify repository was called only once
            coVerify(exactly = 1) { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) }
        }

        @Test
        @DisplayName("Should fetch fresh data when cache is expired")
        fun `should fetch fresh data when cache is expired`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            coEvery { mockUserRepository.getUserById(userId) } returns 
                User(1L, "testuser", "hash", "test@example.com", "USD", Date(), Date())
            
            // First call
            service.getSortedCurrenciesSuspend(userId)
            
            // Simulate cache expiration by waiting (in real scenario, this would be time-based)
            // For testing, we'll invalidate the cache manually
            service.invalidateCache(userId)
            
            // Second call should fetch fresh data
            service.getSortedCurrenciesSuspend(userId)
            
            // Verify repository was called twice
            coVerify(exactly = 2) { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) }
        }

        @Test
        @DisplayName("Should include user's default currency in used currencies")
        fun `should include user's default currency in used currencies`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            coEvery { mockUserRepository.getUserById(userId) } returns 
                User(1L, "testuser", "hash", "test@example.com", "GBP", Date(), Date())
            
            val result = service.getSortedCurrenciesSuspend(userId)
            
            // GBP should be included in the used currencies even though it's not in usage data
            val gbpIndex = result.indexOf(Currency.GBP)
            val usdIndex = result.indexOf(Currency.USD)
            
            assertTrue(gbpIndex < usdIndex, "GBP (default currency) should come before USD (unused)")
        }
    }

    @Nested
    @DisplayName("getTopCurrencies Method")
    inner class GetTopCurrenciesMethod {

        @Test
        @DisplayName("Should return top N currencies")
        fun `should return top N currencies`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date()),
                CurrencyUsage(3L, userId, "JPY", 3, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            
            val result = service.getTopCurrencies(userId, 3).first()
            
            assertEquals(3, result.size)
            assertEquals(Currency.EUR, result[0])
            assertEquals(Currency.GBP, result[1])
            assertEquals(Currency.JPY, result[2])
        }

        @Test
        @DisplayName("Should return all currencies when limit is larger than available")
        fun `should return all currencies when limit is larger than available`() = runTest {
            val userId = 1L
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            
            val result = service.getTopCurrencies(userId, 100).first()
            
            assertEquals(Currency.values().size, result.size)
        }
    }

    @Nested
    @DisplayName("getUsedCurrencies Method")
    inner class GetUsedCurrenciesMethod {

        @Test
        @DisplayName("Should return only currencies that user has actually used")
        fun `should return only currencies that user has actually used`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date()),
                CurrencyUsage(3L, userId, "INVALID", 3, Date()) // Invalid currency code
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            
            val result = service.getUsedCurrencies(userId).first()
            
            assertEquals(2, result.size)
            assertTrue(result.contains(Currency.EUR))
            assertTrue(result.contains(Currency.GBP))
            assertFalse(result.contains(Currency.USD)) // Not used
        }

        @Test
        @DisplayName("Should return empty list when user has no usage data")
        fun `should return empty list when user has no usage data`() = runTest {
            val userId = 1L
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            
            val result = service.getUsedCurrencies(userId).first()
            
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("trackCurrencyUsage Method")
    inner class TrackCurrencyUsageMethod {

        @Test
        @DisplayName("Should increment usage and invalidate cache")
        fun `should increment usage and invalidate cache`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) } just Runs
            
            service.trackCurrencyUsage(userId, currencyCode)
            
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, currencyCode) }
        }

        @Test
        @DisplayName("Should handle multiple currency usage tracking")
        fun `should handle multiple currency usage tracking`() = runTest {
            val userId = 1L
            
            coEvery { mockCurrencyUsageRepository.incrementCurrencyUsage(any(), any()) } just Runs
            
            service.trackCurrencyUsage(userId, "EUR")
            service.trackCurrencyUsage(userId, "GBP")
            service.trackCurrencyUsage(userId, "JPY")
            
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, "EUR") }
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, "GBP") }
            coVerify { mockCurrencyUsageRepository.incrementCurrencyUsage(userId, "JPY") }
        }
    }

    @Nested
    @DisplayName("Cache Management")
    inner class CacheManagement {

        @Test
        @DisplayName("Should invalidate cache for specific user")
        fun `should invalidate cache for specific user`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            coEvery { mockUserRepository.getUserById(userId) } returns 
                User(1L, "testuser", "hash", "test@example.com", "USD", Date(), Date())
            
            // Populate cache
            service.getSortedCurrenciesSuspend(userId)
            
            // Invalidate cache
            service.invalidateCache(userId)
            
            // Next call should fetch fresh data
            service.getSortedCurrenciesSuspend(userId)
            
            coVerify(exactly = 2) { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) }
        }

        @Test
        @DisplayName("Should invalidate all cache")
        fun `should invalidate all cache`() = runTest {
            val userId1 = 1L
            val userId2 = 2L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId1, "EUR", 10, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(any()) } returns 
                flowOf(userCurrencyUsage)
            coEvery { mockUserRepository.getUserById(any()) } returns 
                User(1L, "testuser", "hash", "test@example.com", "USD", Date(), Date())
            
            // Populate cache for two users
            service.getSortedCurrenciesSuspend(userId1)
            service.getSortedCurrenciesSuspend(userId2)
            
            // Invalidate all cache
            service.invalidateAllCache()
            
            // Next calls should fetch fresh data
            service.getSortedCurrenciesSuspend(userId1)
            service.getSortedCurrenciesSuspend(userId2)
            
            coVerify(exactly = 2) { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId1) }
            coVerify(exactly = 2) { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId2) }
        }

        @Test
        @DisplayName("Should return cache statistics")
        fun `should return cache statistics`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            coEvery { mockUserRepository.getUserById(userId) } returns 
                User(1L, "testuser", "hash", "test@example.com", "USD", Date(), Date())
            
            // Populate cache
            service.getSortedCurrenciesSuspend(userId)
            
            val stats = service.getCacheStats()
            
            assertTrue(stats.containsKey("sortedCurrenciesCacheSize"))
            assertTrue(stats.containsKey("currencyUsageCacheSize"))
            assertTrue(stats.containsKey("cacheTimestampsSize"))
            assertTrue(stats.containsKey("cacheExpirationTimeMs"))
        }
    }

    @Nested
    @DisplayName("getSortedCurrenciesReactive Method")
    inner class GetSortedCurrenciesReactiveMethod {

        @Test
        @DisplayName("Should combine multiple data sources reactively")
        fun `should combine multiple data sources reactively`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            
            val result = service.getSortedCurrenciesReactive(userId).first()
            
            // Should include EUR (used) and USD (default) in the used set
            val eurIndex = result.indexOf(Currency.EUR)
            val usdIndex = result.indexOf(Currency.USD)
            val gbpIndex = result.indexOf(Currency.GBP)
            
            assertTrue(eurIndex < gbpIndex, "EUR (used) should come before GBP (unused)")
            assertTrue(usdIndex < gbpIndex, "USD (default) should come before GBP (unused)")
        }

        @Test
        @DisplayName("Should handle errors in reactive flow")
        fun `should handle errors in reactive flow`() = runTest {
            val userId = 1L
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flow { throw Exception("Database error") }
            
            val result = service.getSortedCurrenciesReactive(userId).first()
            
            // Should return empty list when error occurs and no cache is available
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle null user data gracefully")
        fun `should handle null user data gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            coEvery { mockUserRepository.getUserById(userId) } returns null
            
            val result = service.getSortedCurrenciesSuspend(userId)
            
            // Should still work with default currency fallback
            assertTrue(result.isNotEmpty())
            assertEquals(Currency.USD, result.first()) // Should fall back to USD
        }

        @Test
        @DisplayName("Should handle empty currency usage list")
        fun `should handle empty currency usage list`() = runTest {
            val userId = 1L
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(emptyList())
            coEvery { mockUserRepository.getUserById(userId) } returns 
                User(1L, "testuser", "hash", "test@example.com", "USD", Date(), Date())
            
            val result = service.getSortedCurrenciesSuspend(userId)
            
            // Should return all currencies sorted by popularity
            assertEquals(Currency.values().size, result.size)
            assertEquals(Currency.USD, result.first())
        }

        @Test
        @DisplayName("Should handle invalid currency codes in usage data")
        fun `should handle invalid currency codes in usage data`() = runTest {
            val userId = 1L
            val userCurrencyUsage = listOf(
                CurrencyUsage(1L, userId, "INVALID", 10, Date()),
                CurrencyUsage(2L, userId, "EUR", 5, Date())
            )
            
            coEvery { mockCurrencyUsageRepository.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(userCurrencyUsage)
            coEvery { mockUserRepository.getUserById(userId) } returns 
                User(1L, "testuser", "hash", "test@example.com", "USD", Date(), Date())
            
            val result = service.getSortedCurrenciesSuspend(userId)
            
            // Should only include EUR (valid currency) in used set
            val eurIndex = result.indexOf(Currency.EUR)
            val usdIndex = result.indexOf(Currency.USD)
            val gbpIndex = result.indexOf(Currency.GBP)
            
            assertTrue(eurIndex < gbpIndex, "EUR should come before unused currencies")
            assertTrue(usdIndex < gbpIndex, "USD (default) should come before unused currencies")
        }
    }
}
