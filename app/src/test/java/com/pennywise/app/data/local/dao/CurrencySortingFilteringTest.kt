package com.pennywise.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Tests for currency sorting and filtering logic
 * Verifies that currency usage data is properly sorted and filtered according to business rules
 */
@RunWith(AndroidJUnit4::class)
class CurrencySortingFilteringTest {
    private lateinit var database: PennyWiseDatabase
    private lateinit var currencyUsageDao: CurrencyUsageDao
    private lateinit var userDao: UserDao
    private lateinit var testUser: UserEntity

    @Before
    fun createDb() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        currencyUsageDao = database.currencyUsageDao()
        userDao = database.userDao()
        
        // Create a test user
        testUser = UserEntity(defaultCurrency = "USD", locale = "en", deviceAuthEnabled = false)
        userDao.insertUser(testUser)
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun getCurrencyUsageByUser_shouldSortByUsageCountDescThenLastUsedDesc() = runTest {
        // Given - Create currency usages with different usage counts and last used dates
        val now = System.currentTimeMillis()
        val currencies = listOf(
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "EUR",
                usageCount = 10,
                lastUsed = Date(now - 10000) // 10 seconds ago
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "USD",
                usageCount = 15, // Highest usage
                lastUsed = Date(now - 20000) // 20 seconds ago (older)
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "GBP",
                usageCount = 10, // Same usage as EUR
                lastUsed = Date(now - 5000) // 5 seconds ago (more recent than EUR)
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "JPY",
                usageCount = 5, // Lowest usage
                lastUsed = Date(now - 1000) // 1 second ago (most recent)
            )
        )

        currencies.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // When
        val sortedCurrencies = currencyUsageDao.getCurrencyUsageByUser(testUser.id).first()

        // Then
        assertEquals(4, sortedCurrencies.size)
        // Should be sorted by usage count DESC, then by last used DESC
        assertEquals("USD", sortedCurrencies[0].currency) // 15 usage (highest)
        assertEquals("GBP", sortedCurrencies[1].currency) // 10 usage, more recent than EUR
        assertEquals("EUR", sortedCurrencies[2].currency) // 10 usage, older than GBP
        assertEquals("JPY", sortedCurrencies[3].currency) // 5 usage (lowest)
    }

    @Test
    fun getTopCurrenciesByUser_shouldRespectLimitAndSortByUsage() = runTest {
        // Given - Create 10 currencies with different usage counts
        val currencies = (1..10).map { index ->
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "CURRENCY_$index",
                usageCount = index, // Usage count from 1 to 10
                lastUsed = Date()
            )
        }

        currencies.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // When
        val top5Currencies = currencyUsageDao.getTopCurrenciesByUser(testUser.id, 5).first()

        // Then
        assertEquals(5, top5Currencies.size)
        // Should be sorted by usage count DESC
        assertEquals("CURRENCY_10", top5Currencies[0].currency) // Highest usage
        assertEquals("CURRENCY_9", top5Currencies[1].currency)
        assertEquals("CURRENCY_8", top5Currencies[2].currency)
        assertEquals("CURRENCY_7", top5Currencies[3].currency)
        assertEquals("CURRENCY_6", top5Currencies[4].currency) // Lowest in top 5
    }

    @Test
    fun getTopCurrenciesByUser_shouldUseDefaultLimit() = runTest {
        // Given - Create 15 currencies
        val currencies = (1..15).map { index ->
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "CURRENCY_$index",
                usageCount = index,
                lastUsed = Date()
            )
        }

        currencies.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // When - Don't specify limit (should use default of 10)
        val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(testUser.id).first()

        // Then
        assertEquals(10, topCurrencies.size) // Default limit
        assertEquals("CURRENCY_15", topCurrencies[0].currency) // Highest usage
        assertEquals("CURRENCY_6", topCurrencies[9].currency) // Lowest in top 10
    }

    @Test
    fun getUserCurrenciesSortedByUsage_shouldSortByUsageCountDescThenLastUsedDesc() = runTest {
        // Given - Create currencies with same usage count but different last used dates
        val now = System.currentTimeMillis()
        val currencies = listOf(
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "EUR",
                usageCount = 5,
                lastUsed = Date(now - 10000) // Older
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "USD",
                usageCount = 10, // Higher usage
                lastUsed = Date(now - 20000) // Older
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "GBP",
                usageCount = 5, // Same as EUR
                lastUsed = Date(now - 5000) // More recent than EUR
            )
        )

        currencies.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // When
        val sortedCurrencies = currencyUsageDao.getUserCurrenciesSortedByUsage(testUser.id).first()

        // Then
        assertEquals(3, sortedCurrencies.size)
        assertEquals("USD", sortedCurrencies[0].currency) // Highest usage (10)
        assertEquals("GBP", sortedCurrencies[1].currency) // Same usage as EUR (5), but more recent
        assertEquals("EUR", sortedCurrencies[2].currency) // Same usage as GBP (5), but older
    }

    @Test
    fun getCurrencyUsageByUserAndCurrency_shouldBeCaseSensitive() = runTest {
        // Given
        val currencyUsage = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 5,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // When
        val foundUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")
        val notFoundUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "eur")
        val notFoundUsage2 = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "Eur")

        // Then
        assertNotNull(foundUsage)
        assertEquals("EUR", foundUsage!!.currency)
        
        assertNull(notFoundUsage) // Case sensitive - "eur" should not match "EUR"
        assertNull(notFoundUsage2) // Case sensitive - "Eur" should not match "EUR"
    }

    @Test
    fun currencyUsageFiltering_shouldWorkWithMultipleUsers() = runTest {
        // Given - Create another user
        val otherUser = UserEntity(defaultCurrency = "EUR", locale = "en", deviceAuthEnabled = false)
        val otherUserId = userDao.insertUser(otherUser)

        val testUserCurrencies = listOf(
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "EUR",
                usageCount = 10,
                lastUsed = Date()
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "USD",
                usageCount = 5,
                lastUsed = Date()
            )
        )

        val otherUserCurrencies = listOf(
            CurrencyUsageEntity(
                userId = otherUserId,
                currency = "EUR",
                usageCount = 3,
                lastUsed = Date()
            ),
            CurrencyUsageEntity(
                userId = otherUserId,
                currency = "GBP",
                usageCount = 7,
                lastUsed = Date()
            )
        )

        testUserCurrencies.forEach { currencyUsageDao.insertCurrencyUsage(it) }
        otherUserCurrencies.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // When
        val testUserCurrenciesFromDb = currencyUsageDao.getCurrencyUsageByUser(testUser.id).first()
        val otherUserCurrenciesFromDb = currencyUsageDao.getCurrencyUsageByUser(otherUserId).first()

        // Then
        assertEquals(2, testUserCurrenciesFromDb.size)
        assertEquals(2, otherUserCurrenciesFromDb.size)
        
        // Test user should only see their currencies
        assertTrue(testUserCurrenciesFromDb.all { it.userId == testUser.id })
        assertTrue(testUserCurrenciesFromDb.any { it.currency == "EUR" })
        assertTrue(testUserCurrenciesFromDb.any { it.currency == "USD" })
        
        // Other user should only see their currencies
        assertTrue(otherUserCurrenciesFromDb.all { it.userId == otherUserId })
        assertTrue(otherUserCurrenciesFromDb.any { it.currency == "EUR" })
        assertTrue(otherUserCurrenciesFromDb.any { it.currency == "GBP" })
    }

    @Test
    fun getTopCurrenciesByUser_shouldFilterByUser() = runTest {
        // Given - Create another user
        val otherUser = UserEntity(defaultCurrency = "EUR", locale = "en", deviceAuthEnabled = false)
        val otherUserId = userDao.insertUser(otherUser)

        // Test user has currencies with usage counts 10, 5, 3
        val testUserCurrencies = listOf(
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "EUR",
                usageCount = 10,
                lastUsed = Date()
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "USD",
                usageCount = 5,
                lastUsed = Date()
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "GBP",
                usageCount = 3,
                lastUsed = Date()
            )
        )

        // Other user has currencies with usage counts 15, 8, 2
        val otherUserCurrencies = listOf(
            CurrencyUsageEntity(
                userId = otherUserId,
                currency = "EUR",
                usageCount = 15,
                lastUsed = Date()
            ),
            CurrencyUsageEntity(
                userId = otherUserId,
                currency = "USD",
                usageCount = 8,
                lastUsed = Date()
            ),
            CurrencyUsageEntity(
                userId = otherUserId,
                currency = "GBP",
                usageCount = 2,
                lastUsed = Date()
            )
        )

        testUserCurrencies.forEach { currencyUsageDao.insertCurrencyUsage(it) }
        otherUserCurrencies.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // When
        val testUserTopCurrencies = currencyUsageDao.getTopCurrenciesByUser(testUser.id, 2).first()
        val otherUserTopCurrencies = currencyUsageDao.getTopCurrenciesByUser(otherUserId, 2).first()

        // Then
        assertEquals(2, testUserTopCurrencies.size)
        assertEquals(2, otherUserTopCurrencies.size)
        
        // Test user's top currencies should be EUR (10) and USD (5)
        assertEquals("EUR", testUserTopCurrencies[0].currency)
        assertEquals(10, testUserTopCurrencies[0].usageCount)
        assertEquals("USD", testUserTopCurrencies[1].currency)
        assertEquals(5, testUserTopCurrencies[1].usageCount)
        
        // Other user's top currencies should be EUR (15) and USD (8)
        assertEquals("EUR", otherUserTopCurrencies[0].currency)
        assertEquals(15, otherUserTopCurrencies[0].usageCount)
        assertEquals("USD", otherUserTopCurrencies[1].currency)
        assertEquals(8, otherUserTopCurrencies[1].usageCount)
    }

    @Test
    fun currencyUsageCount_shouldBeAccurate() = runTest {
        // Given
        val currencies = listOf("EUR", "USD", "GBP", "JPY", "CAD")
        currencies.forEach { currency ->
            val currencyUsage = CurrencyUsageEntity(
                userId = testUser.id,
                currency = currency,
                usageCount = currencies.indexOf(currency) + 1,
                lastUsed = Date()
            )
            currencyUsageDao.insertCurrencyUsage(currencyUsage)
        }

        // When
        val count = currencyUsageDao.getCurrencyUsageCountForUser(testUser.id)

        // Then
        assertEquals(5, count)
    }

    @Test
    fun emptyCurrencyUsage_shouldReturnEmptyResults() = runTest {
        // When
        val currencies = currencyUsageDao.getCurrencyUsageByUser(testUser.id).first()
        val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(testUser.id, 5).first()
        val sortedCurrencies = currencyUsageDao.getUserCurrenciesSortedByUsage(testUser.id).first()
        val count = currencyUsageDao.getCurrencyUsageCountForUser(testUser.id)

        // Then
        assertTrue(currencies.isEmpty())
        assertTrue(topCurrencies.isEmpty())
        assertTrue(sortedCurrencies.isEmpty())
        assertEquals(0, count)
    }

    @Test
    fun currencyUsageWithSameUsageCount_shouldSortByLastUsedDesc() = runTest {
        // Given - Create currencies with same usage count but different last used dates
        val now = System.currentTimeMillis()
        val currencies = listOf(
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "EUR",
                usageCount = 5,
                lastUsed = Date(now - 10000) // 10 seconds ago
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "USD",
                usageCount = 5, // Same usage count
                lastUsed = Date(now - 5000) // 5 seconds ago (more recent)
            ),
            CurrencyUsageEntity(
                userId = testUser.id,
                currency = "GBP",
                usageCount = 5, // Same usage count
                lastUsed = Date(now - 15000) // 15 seconds ago (oldest)
            )
        )

        currencies.forEach { currencyUsageDao.insertCurrencyUsage(it) }

        // When
        val sortedCurrencies = currencyUsageDao.getUserCurrenciesSortedByUsage(testUser.id).first()

        // Then
        assertEquals(3, sortedCurrencies.size)
        assertEquals("USD", sortedCurrencies[0].currency) // Most recent
        assertEquals("EUR", sortedCurrencies[1].currency) // Middle
        assertEquals("GBP", sortedCurrencies[2].currency) // Oldest
    }
}
