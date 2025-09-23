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
 * Integration tests for CurrencyUsageDao
 * Tests currency usage tracking, sorting, filtering, and database operations
 */
@RunWith(AndroidJUnit4::class)
class CurrencyUsageDaoTest {
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
    fun insertCurrencyUsage_shouldReturnUsageId() = runTest {
        // Given
        val currencyUsage = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 1,
            lastUsed = Date()
        )

        // When
        val usageId = currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // Then
        assertTrue(usageId > 0)
    }

    @Test
    fun getCurrencyUsageById_shouldReturnUsage() = runTest {
        // Given
        val currencyUsage = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 5,
            lastUsed = Date()
        )
        val usageId = currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // When
        val retrievedUsage = currencyUsageDao.getCurrencyUsageById(usageId)

        // Then
        assertNotNull(retrievedUsage)
        assertEquals(currencyUsage.userId, retrievedUsage!!.userId)
        assertEquals(currencyUsage.currency, retrievedUsage.currency)
        assertEquals(currencyUsage.usageCount, retrievedUsage.usageCount)
    }

    @Test
    fun getCurrencyUsageByUser_shouldReturnUserUsages() = runTest {
        // Given
        val usage1 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 10,
            lastUsed = Date()
        )
        val usage2 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "GBP",
            usageCount = 5,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(usage1)
        currencyUsageDao.insertCurrencyUsage(usage2)

        // When
        val usages = currencyUsageDao.getCurrencyUsageByUser(testUser.id).first()

        // Then
        assertEquals(2, usages.size)
        assertTrue(usages.any { it.currency == "EUR" })
        assertTrue(usages.any { it.currency == "GBP" })
    }

    @Test
    fun getCurrencyUsageByUserAndCurrency_shouldReturnSpecificUsage() = runTest {
        // Given
        val currencyUsage = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 3,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // When
        val retrievedUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")

        // Then
        assertNotNull(retrievedUsage)
        assertEquals("EUR", retrievedUsage!!.currency)
        assertEquals(3, retrievedUsage.usageCount)
    }

    @Test
    fun getCurrencyUsageByUserAndCurrency_shouldReturnNull_whenNotFound() = runTest {
        // When
        val retrievedUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "JPY")

        // Then
        assertNull(retrievedUsage)
    }

    @Test
    fun incrementCurrencyUsage_shouldCreateNewUsage_whenNotExists() = runTest {
        // When
        val now = Date()
        currencyUsageDao.incrementCurrencyUsage(testUser.id, "EUR", now, now)

        // Then
        val usage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")
        assertNotNull(usage)
        assertEquals(1, usage!!.usageCount)
    }

    @Test
    fun incrementCurrencyUsage_shouldIncrementExistingUsage() = runTest {
        // Given
        val currencyUsage = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 5,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // When
        val now = Date()
        currencyUsageDao.incrementCurrencyUsage(testUser.id, "EUR", now, now)

        // Then
        val updatedUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")
        assertEquals(6, updatedUsage!!.usageCount)
    }

    @Test
    fun incrementCurrencyUsage_shouldUpdateLastUsedTimestamp() = runTest {
        // Given
        val oldDate = Date(System.currentTimeMillis() - 10000) // 10 seconds ago
        val currencyUsage = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 1,
            lastUsed = oldDate
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // When
        Thread.sleep(100) // Small delay to ensure timestamp difference
        val now = Date()
        currencyUsageDao.incrementCurrencyUsage(testUser.id, "EUR", now, now)

        // Then
        val updatedUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")
        assertTrue(updatedUsage!!.lastUsed.after(oldDate))
    }

    @Test
    fun getTopCurrenciesByUser_shouldReturnCurrenciesSortedByUsage() = runTest {
        // Given
        val usage1 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 10,
            lastUsed = Date()
        )
        val usage2 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "GBP",
            usageCount = 5,
            lastUsed = Date()
        )
        val usage3 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "JPY",
            usageCount = 15,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(usage1)
        currencyUsageDao.insertCurrencyUsage(usage2)
        currencyUsageDao.insertCurrencyUsage(usage3)

        // When
        val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(testUser.id, 2).first()

        // Then
        assertEquals(2, topCurrencies.size)
        assertEquals("JPY", topCurrencies[0].currency) // Highest usage
        assertEquals("EUR", topCurrencies[1].currency) // Second highest
    }

    @Test
    fun getUserCurrenciesSortedByUsage_shouldReturnCurrenciesSortedByUsage() = runTest {
        // Given
        val usage1 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 10,
            lastUsed = Date()
        )
        val usage2 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "GBP",
            usageCount = 5,
            lastUsed = Date()
        )
        val usage3 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "JPY",
            usageCount = 15,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(usage1)
        currencyUsageDao.insertCurrencyUsage(usage2)
        currencyUsageDao.insertCurrencyUsage(usage3)

        // When
        val sortedCurrencies = currencyUsageDao.getUserCurrenciesSortedByUsage(testUser.id).first()

        // Then
        assertEquals(3, sortedCurrencies.size)
        assertEquals("JPY", sortedCurrencies[0].currency) // Highest usage
        assertEquals("EUR", sortedCurrencies[1].currency) // Second highest
        assertEquals("GBP", sortedCurrencies[2].currency) // Lowest usage
    }


    @Test
    fun updateCurrencyUsage_shouldUpdateUsage() = runTest {
        // Given
        val currencyUsage = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 5,
            lastUsed = Date()
        )
        val usageId = currencyUsageDao.insertCurrencyUsage(currencyUsage)
        val originalUsage = currencyUsageDao.getCurrencyUsageById(usageId)!!

        // When
        val updatedUsage = originalUsage.copy(
            usageCount = 10,
            lastUsed = Date()
        )
        currencyUsageDao.updateCurrencyUsage(updatedUsage)

        // Then
        val retrievedUsage = currencyUsageDao.getCurrencyUsageById(usageId)
        assertEquals(10, retrievedUsage!!.usageCount)
    }

    @Test
    fun deleteCurrencyUsage_shouldRemoveUsage() = runTest {
        // Given
        val currencyUsage = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 5,
            lastUsed = Date()
        )
        val usageId = currencyUsageDao.insertCurrencyUsage(currencyUsage)
        assertNotNull(currencyUsageDao.getCurrencyUsageById(usageId))

        // When
        val usageToDelete = currencyUsageDao.getCurrencyUsageById(usageId)!!
        currencyUsageDao.deleteCurrencyUsage(usageToDelete)

        // Then
        assertNull(currencyUsageDao.getCurrencyUsageById(usageId))
    }

    @Test
    fun deleteAllCurrencyUsageForUser_shouldRemoveAllUserUsages() = runTest {
        // Given
        val usage1 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 5,
            lastUsed = Date()
        )
        val usage2 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "GBP",
            usageCount = 3,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(usage1)
        currencyUsageDao.insertCurrencyUsage(usage2)

        // When
        currencyUsageDao.deleteAllCurrencyUsageForUser(testUser.id)

        // Then
        val usages = currencyUsageDao.getCurrencyUsageByUser(testUser.id).first()
        assertTrue(usages.isEmpty())
    }



    @Test
    fun getCurrencyUsageCountForUser_shouldReturnCorrectCount() = runTest {
        // Given
        val usage1 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 5,
            lastUsed = Date()
        )
        val usage2 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "GBP",
            usageCount = 3,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(usage1)
        currencyUsageDao.insertCurrencyUsage(usage2)

        // When
        val count = currencyUsageDao.getCurrencyUsageCountForUser(testUser.id)

        // Then
        assertEquals(2, count)
    }


    @Test
    fun getCurrencyUsageByUser_shouldReturnEmptyList_whenNoUsages() = runTest {
        // When
        val usages = currencyUsageDao.getCurrencyUsageByUser(testUser.id).first()

        // Then
        assertTrue(usages.isEmpty())
    }

    @Test
    fun incrementCurrencyUsage_shouldHandleMultipleIncrements() = runTest {
        // When
        val now = Date()
        currencyUsageDao.incrementCurrencyUsage(testUser.id, "EUR", now, now)
        currencyUsageDao.incrementCurrencyUsage(testUser.id, "EUR", now, now)
        currencyUsageDao.incrementCurrencyUsage(testUser.id, "EUR", now, now)

        // Then
        val usage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")
        assertEquals(3, usage!!.usageCount)
    }

    @Test
    fun getTopCurrenciesByUser_shouldRespectLimit() = runTest {
        // Given
        val currencies = listOf("EUR", "GBP", "JPY", "USD", "CAD")
        currencies.forEachIndexed { index, currency ->
            val usage = CurrencyUsageEntity(
                userId = testUser.id,
                currency = currency,
                usageCount = 10 - index, // Decreasing usage count
                lastUsed = Date()
            )
            currencyUsageDao.insertCurrencyUsage(usage)
        }

        // When
        val topCurrencies = currencyUsageDao.getTopCurrenciesByUser(testUser.id, 3).first()

        // Then
        assertEquals(3, topCurrencies.size)
        assertEquals("EUR", topCurrencies[0].currency) // Highest usage
        assertEquals("GBP", topCurrencies[1].currency) // Second highest
        assertEquals("JPY", topCurrencies[2].currency) // Third highest
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
        val foundUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "eur")
        val notFoundUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")

        // Then
        assertNull(foundUsage) // Case sensitive, so "eur" should not match "EUR"
        assertNotNull(notFoundUsage) // "EUR" should match "EUR"
    }

    @Test
    fun insertOrIncrementCurrencyUsage_shouldCreateNewUsage_whenNotExists() = runTest {
        // When
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, "EUR", now, now, now)

        // Then
        val usage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")
        assertNotNull(usage)
        assertEquals(1, usage!!.usageCount)
    }

    @Test
    fun insertOrIncrementCurrencyUsage_shouldIncrementExistingUsage() = runTest {
        // Given
        val currencyUsage = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 5,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(currencyUsage)

        // When
        val now = Date()
        currencyUsageDao.insertOrIncrementCurrencyUsage(testUser.id, "EUR", now, now, now)

        // Then
        val updatedUsage = currencyUsageDao.getCurrencyUsageByUserAndCurrency(testUser.id, "EUR")
        assertEquals(6, updatedUsage!!.usageCount)
    }

    @Test
    fun getCurrencyUsageCountForUser_shouldReturnZero_whenNoUsages() = runTest {
        // When
        val count = currencyUsageDao.getCurrencyUsageCountForUser(testUser.id)

        // Then
        assertEquals(0, count)
    }

    @Test
    fun deleteAllCurrencyUsageForUser_shouldNotAffectOtherUsers() = runTest {
        // Given - Create another user
        val otherUser = UserEntity(defaultCurrency = "EUR", locale = "en", deviceAuthEnabled = false)
        val otherUserId = userDao.insertUser(otherUser)

        val usage1 = CurrencyUsageEntity(
            userId = testUser.id,
            currency = "EUR",
            usageCount = 5,
            lastUsed = Date()
        )
        val usage2 = CurrencyUsageEntity(
            userId = otherUserId,
            currency = "EUR",
            usageCount = 3,
            lastUsed = Date()
        )
        currencyUsageDao.insertCurrencyUsage(usage1)
        currencyUsageDao.insertCurrencyUsage(usage2)

        // When
        currencyUsageDao.deleteAllCurrencyUsageForUser(testUser.id)

        // Then
        val testUserUsages = currencyUsageDao.getCurrencyUsageByUser(testUser.id).first()
        val otherUserUsages = currencyUsageDao.getCurrencyUsageByUser(otherUserId).first()
        
        assertTrue(testUserUsages.isEmpty())
        assertEquals(1, otherUserUsages.size)
        assertEquals("EUR", otherUserUsages[0].currency)
    }
}
