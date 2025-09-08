package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
import java.util.concurrent.TimeUnit

/**
 * Unit tests for CurrencyUsageTracker
 * Tests currency usage tracking, statistics, trends, and error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Currency Usage Tracker Tests")
class CurrencyUsageTrackerTest {

    private lateinit var tracker: CurrencyUsageTracker
    private lateinit var mockRepository: CurrencyUsageRepository
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        mockRepository = mockk<CurrencyUsageRepository>(relaxed = true)
        tracker = CurrencyUsageTracker(mockRepository, Dispatchers.IO)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Nested
    @DisplayName("trackCurrencyUsage Method")
    inner class TrackCurrencyUsageMethod {

        @Test
        @DisplayName("Should track currency usage successfully")
        fun `should track currency usage successfully`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            coEvery { mockRepository.incrementCurrencyUsage(userId, currencyCode) } just Runs
            
            tracker.trackCurrencyUsage(userId, currencyCode)
            
            coVerify { mockRepository.incrementCurrencyUsage(userId, currencyCode) }
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            val currencyCode = "EUR"
            
            coEvery { mockRepository.incrementCurrencyUsage(userId, currencyCode) } throws 
                Exception("Database error")
            
            // Should not throw exception
            tracker.trackCurrencyUsage(userId, currencyCode)
            
            coVerify { mockRepository.incrementCurrencyUsage(userId, currencyCode) }
        }

        @Test
        @DisplayName("Should track multiple currency usages")
        fun `should track multiple currency usages`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.incrementCurrencyUsage(any(), any()) } just Runs
            
            tracker.trackCurrencyUsage(userId, "EUR")
            tracker.trackCurrencyUsage(userId, "GBP")
            tracker.trackCurrencyUsage(userId, "JPY")
            
            coVerify { mockRepository.incrementCurrencyUsage(userId, "EUR") }
            coVerify { mockRepository.incrementCurrencyUsage(userId, "GBP") }
            coVerify { mockRepository.incrementCurrencyUsage(userId, "JPY") }
        }
    }

    @Nested
    @DisplayName("getUserCurrenciesByPopularity Method")
    inner class GetUserCurrenciesByPopularityMethod {

        @Test
        @DisplayName("Should return currencies sorted by popularity")
        fun `should return currencies sorted by popularity`() = runTest {
            val userId = 1L
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date()),
                CurrencyUsage(3L, userId, "JPY", 3, Date())
            )
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getUserCurrenciesByPopularity(userId)
            
            assertEquals(listOf("EUR", "GBP", "JPY"), result)
        }

        @Test
        @DisplayName("Should return empty list when no usage data")
        fun `should return empty list when no usage data`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(emptyList())
            
            val result = tracker.getUserCurrenciesByPopularity(userId)
            
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } throws 
                Exception("Database error")
            
            val result = tracker.getUserCurrenciesByPopularity(userId)
            
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getTopCurrenciesForUser Method")
    inner class GetTopCurrenciesForUserMethod {

        @Test
        @DisplayName("Should return top N currencies")
        fun `should return top N currencies`() = runTest {
            val userId = 1L
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date()),
                CurrencyUsage(3L, userId, "JPY", 3, Date()),
                CurrencyUsage(4L, userId, "USD", 1, Date())
            )
            
            coEvery { mockRepository.getTopCurrenciesByUser(userId, 3) } returns 
                flowOf(currencyUsages.take(3))
            
            val result = tracker.getTopCurrenciesForUser(userId, 3)
            
            assertEquals(listOf("EUR", "GBP", "JPY"), result)
        }

        @Test
        @DisplayName("Should use default limit when not specified")
        fun `should use default limit when not specified`() = runTest {
            val userId = 1L
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date())
            )
            
            coEvery { mockRepository.getTopCurrenciesByUser(userId, 10) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getTopCurrenciesForUser(userId)
            
            assertEquals(listOf("EUR", "GBP"), result)
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getTopCurrenciesByUser(userId, 10) } throws 
                Exception("Database error")
            
            val result = tracker.getTopCurrenciesForUser(userId)
            
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getCurrencyUsageStats Method")
    inner class GetCurrencyUsageStatsMethod {

        @Test
        @DisplayName("Should return correct usage statistics")
        fun `should return correct usage statistics`() = runTest {
            val userId = 1L
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date()),
                CurrencyUsage(3L, userId, "JPY", 3, Date())
            )
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getCurrencyUsageStats(userId)
            
            assertEquals(18, result.totalUsage)
            assertEquals(3, result.uniqueCurrencies)
            assertEquals("EUR", result.mostUsedCurrency)
            assertEquals("JPY", result.leastUsedCurrency)
            assertEquals(listOf("EUR", "GBP", "JPY"), result.currencies)
        }

        @Test
        @DisplayName("Should handle empty usage data")
        fun `should handle empty usage data`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(emptyList())
            
            val result = tracker.getCurrencyUsageStats(userId)
            
            assertEquals(0, result.totalUsage)
            assertEquals(0, result.uniqueCurrencies)
            assertNull(result.mostUsedCurrency)
            assertNull(result.leastUsedCurrency)
            assertTrue(result.currencies.isEmpty())
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } throws 
                Exception("Database error")
            
            val result = tracker.getCurrencyUsageStats(userId)
            
            assertEquals(0, result.totalUsage)
            assertEquals(0, result.uniqueCurrencies)
            assertNull(result.mostUsedCurrency)
            assertNull(result.leastUsedCurrency)
            assertTrue(result.currencies.isEmpty())
        }
    }

    @Nested
    @DisplayName("getMostUsedCurrencies Method")
    inner class GetMostUsedCurrenciesMethod {

        @Test
        @DisplayName("Should return most used currencies with percentages")
        fun `should return most used currencies with percentages`() = runTest {
            val userId = 1L
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date()),
                CurrencyUsage(3L, userId, "JPY", 3, Date())
            )
            
            coEvery { mockRepository.getTopCurrenciesByUser(userId, 5) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getMostUsedCurrencies(userId, 5)
            
            assertEquals(3, result.size)
            
            val eurInfo = result.find { it.currency == "EUR" }!!
            assertEquals(10, eurInfo.usageCount)
            assertEquals(55.56, eurInfo.percentage, 0.01) // 10/18 * 100
            
            val gbpInfo = result.find { it.currency == "GBP" }!!
            assertEquals(5, gbpInfo.usageCount)
            assertEquals(27.78, gbpInfo.percentage, 0.01) // 5/18 * 100
            
            val jpyInfo = result.find { it.currency == "JPY" }!!
            assertEquals(3, jpyInfo.usageCount)
            assertEquals(16.67, jpyInfo.percentage, 0.01) // 3/18 * 100
        }

        @Test
        @DisplayName("Should handle zero total usage")
        fun `should handle zero total usage`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getTopCurrenciesByUser(userId, 5) } returns 
                flowOf(emptyList())
            
            val result = tracker.getMostUsedCurrencies(userId, 5)
            
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getTopCurrenciesByUser(userId, 5) } throws 
                Exception("Database error")
            
            val result = tracker.getMostUsedCurrencies(userId, 5)
            
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getLeastUsedCurrencies Method")
    inner class GetLeastUsedCurrenciesMethod {

        @Test
        @DisplayName("Should return least used currencies with percentages")
        fun `should return least used currencies with percentages`() = runTest {
            val userId = 1L
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date()),
                CurrencyUsage(2L, userId, "GBP", 5, Date()),
                CurrencyUsage(3L, userId, "JPY", 3, Date()),
                CurrencyUsage(4L, userId, "USD", 1, Date())
            )
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getLeastUsedCurrencies(userId, 2)
            
            assertEquals(2, result.size)
            
            val usdInfo = result[0]
            assertEquals("USD", usdInfo.currency)
            assertEquals(1, usdInfo.usageCount)
            assertEquals(5.26, usdInfo.percentage, 0.01) // 1/19 * 100
            
            val jpyInfo = result[1]
            assertEquals("JPY", jpyInfo.currency)
            assertEquals(3, jpyInfo.usageCount)
            assertEquals(15.79, jpyInfo.percentage, 0.01) // 3/19 * 100
        }

        @Test
        @DisplayName("Should handle empty usage data")
        fun `should handle empty usage data`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(emptyList())
            
            val result = tracker.getLeastUsedCurrencies(userId, 5)
            
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } throws 
                Exception("Database error")
            
            val result = tracker.getLeastUsedCurrencies(userId, 5)
            
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getCurrencyUsageTrend Method")
    inner class GetCurrencyUsageTrendMethod {

        @Test
        @DisplayName("Should return correct usage trend")
        fun `should return correct usage trend`() = runTest {
            val userId = 1L
            val now = System.currentTimeMillis()
            val recentDate = Date(now - TimeUnit.DAYS.toMillis(10)) // 10 days ago
            val oldDate = Date(now - TimeUnit.DAYS.toMillis(40)) // 40 days ago
            
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, recentDate),
                CurrencyUsage(2L, userId, "GBP", 5, recentDate),
                CurrencyUsage(3L, userId, "JPY", 3, oldDate)
            )
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getCurrencyUsageTrend(userId, 30)
            
            assertEquals(2, result.recentCurrencies.size)
            assertTrue(result.recentCurrencies.contains("EUR"))
            assertTrue(result.recentCurrencies.contains("GBP"))
            
            assertEquals(1, result.historicalCurrencies.size)
            assertTrue(result.historicalCurrencies.contains("JPY"))
            
            assertEquals(3, result.totalCurrencies)
            assertEquals(2, result.activeCurrencies)
        }

        @Test
        @DisplayName("Should handle empty usage data")
        fun `should handle empty usage data`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(emptyList())
            
            val result = tracker.getCurrencyUsageTrend(userId, 30)
            
            assertTrue(result.recentCurrencies.isEmpty())
            assertTrue(result.historicalCurrencies.isEmpty())
            assertEquals(0, result.totalCurrencies)
            assertEquals(0, result.activeCurrencies)
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } throws 
                Exception("Database error")
            
            val result = tracker.getCurrencyUsageTrend(userId, 30)
            
            assertTrue(result.recentCurrencies.isEmpty())
            assertTrue(result.historicalCurrencies.isEmpty())
            assertEquals(0, result.totalCurrencies)
            assertEquals(0, result.activeCurrencies)
        }
    }

    @Nested
    @DisplayName("getCurrencyUsageSummary Method")
    inner class GetCurrencyUsageSummaryMethod {

        @Test
        @DisplayName("Should return comprehensive usage summary")
        fun `should return comprehensive usage summary`() = runTest {
            val userId = 1L
            val now = System.currentTimeMillis()
            val recentDate = Date(now - TimeUnit.DAYS.toMillis(10))
            
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, recentDate),
                CurrencyUsage(2L, userId, "GBP", 5, recentDate),
                CurrencyUsage(3L, userId, "JPY", 3, recentDate)
            )
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(currencyUsages)
            coEvery { mockRepository.getTopCurrenciesByUser(userId, 3) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getCurrencyUsageSummary(userId)
            
            assertEquals(18, result.totalTransactions)
            assertEquals(3, result.uniqueCurrencies)
            assertEquals("EUR", result.primaryCurrency)
            assertEquals(3, result.topCurrencies.size)
            assertTrue(result.recentActivity)
            assertEquals("Active", result.usageTrend)
        }

        @Test
        @DisplayName("Should handle empty usage data")
        fun `should handle empty usage data`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(emptyList())
            coEvery { mockRepository.getTopCurrenciesByUser(userId, 3) } returns 
                flowOf(emptyList())
            
            val result = tracker.getCurrencyUsageSummary(userId)
            
            assertEquals(0, result.totalTransactions)
            assertEquals(0, result.uniqueCurrencies)
            assertNull(result.primaryCurrency)
            assertTrue(result.topCurrencies.isEmpty())
            assertFalse(result.recentActivity)
            assertEquals("Inactive", result.usageTrend)
        }

        @Test
        @DisplayName("Should handle repository errors gracefully")
        fun `should handle repository errors gracefully`() = runTest {
            val userId = 1L
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } throws 
                Exception("Database error")
            coEvery { mockRepository.getTopCurrenciesByUser(userId, 3) } throws 
                Exception("Database error")
            
            val result = tracker.getCurrencyUsageSummary(userId)
            
            assertEquals(0, result.totalTransactions)
            assertEquals(0, result.uniqueCurrencies)
            assertNull(result.primaryCurrency)
            assertTrue(result.topCurrencies.isEmpty())
            assertFalse(result.recentActivity)
            assertEquals("Unknown", result.usageTrend)
        }
    }

    @Nested
    @DisplayName("Data Classes")
    inner class DataClasses {

        @Test
        @DisplayName("CurrencyUsageStats should have correct default values")
        fun `CurrencyUsageStats should have correct default values`() {
            val stats = CurrencyUsageStats()
            
            assertEquals(0, stats.totalUsage)
            assertEquals(0, stats.uniqueCurrencies)
            assertNull(stats.mostUsedCurrency)
            assertNull(stats.leastUsedCurrency)
            assertTrue(stats.currencies.isEmpty())
        }

        @Test
        @DisplayName("CurrencyUsageInfo should store data correctly")
        fun `CurrencyUsageInfo should store data correctly`() {
            val date = Date()
            val info = CurrencyUsageInfo("EUR", 10, date, 50.0)
            
            assertEquals("EUR", info.currency)
            assertEquals(10, info.usageCount)
            assertEquals(date, info.lastUsed)
            assertEquals(50.0, info.percentage)
        }

        @Test
        @DisplayName("CurrencyUsageTrend should have correct default values")
        fun `CurrencyUsageTrend should have correct default values`() {
            val trend = CurrencyUsageTrend()
            
            assertTrue(trend.recentCurrencies.isEmpty())
            assertTrue(trend.historicalCurrencies.isEmpty())
            assertEquals(0, trend.totalCurrencies)
            assertEquals(0, trend.activeCurrencies)
        }

        @Test
        @DisplayName("CurrencyUsageSummary should have correct default values")
        fun `CurrencyUsageSummary should have correct default values`() {
            val summary = CurrencyUsageSummary()
            
            assertEquals(0, summary.totalTransactions)
            assertEquals(0, summary.uniqueCurrencies)
            assertNull(summary.primaryCurrency)
            assertTrue(summary.topCurrencies.isEmpty())
            assertFalse(summary.recentActivity)
            assertEquals("Unknown", summary.usageTrend)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    inner class EdgeCases {

        @Test
        @DisplayName("Should handle very large usage counts")
        fun `should handle very large usage counts`() = runTest {
            val userId = 1L
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", Int.MAX_VALUE, Date()),
                CurrencyUsage(2L, userId, "GBP", 1, Date())
            )
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getCurrencyUsageStats(userId)
            
            assertEquals(Int.MAX_VALUE + 1L, result.totalUsage.toLong())
            assertEquals("EUR", result.mostUsedCurrency)
            assertEquals("GBP", result.leastUsedCurrency)
        }

        @Test
        @DisplayName("Should handle zero usage counts")
        fun `should handle zero usage counts`() = runTest {
            val userId = 1L
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 0, Date()),
                CurrencyUsage(2L, userId, "GBP", 0, Date())
            )
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getCurrencyUsageStats(userId)
            
            assertEquals(0, result.totalUsage)
            assertEquals(2, result.uniqueCurrencies)
            // When all usage counts are equal, the first one should be returned
            assertEquals("EUR", result.mostUsedCurrency)
            assertEquals("EUR", result.leastUsedCurrency)
        }

        @Test
        @DisplayName("Should handle negative days back in trend calculation")
        fun `should handle negative days back in trend calculation`() = runTest {
            val userId = 1L
            val currencyUsages = listOf(
                CurrencyUsage(1L, userId, "EUR", 10, Date())
            )
            
            coEvery { mockRepository.getCurrencyUsageByUser(userId) } returns 
                flowOf(currencyUsages)
            
            val result = tracker.getCurrencyUsageTrend(userId, -10)
            
            // Should treat negative days as 0, so all currencies should be recent
            assertEquals(1, result.recentCurrencies.size)
            assertTrue(result.recentCurrencies.contains("EUR"))
            assertTrue(result.historicalCurrencies.isEmpty())
        }
    }
}
