package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.domain.model.CurrencyUsage
import io.mockk.*
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Date

/**
 * Integration tests for CurrencyUsageRepositoryImpl
 * Tests repository layer integration with DAO and domain model mapping
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Currency Usage Repository Implementation Tests")
class CurrencyUsageRepositoryImplTest {

    private lateinit var repository: CurrencyUsageRepositoryImpl
    private lateinit var mockDao: CurrencyUsageDao

    @BeforeEach
    fun setUp() {
        mockDao = mockk<CurrencyUsageDao>(relaxed = true)
        repository = CurrencyUsageRepositoryImpl(mockDao)
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    @DisplayName("getCurrencyUsageByUser Method")
    inner class GetCurrencyUsageByUserMethod {

        @Test
        @DisplayName("Should return currency usage list mapped to domain models")
        fun `should return currency usage list mapped to domain models`() = runTest {
            // Given
            val userId = 1L
            val entity1 = CurrencyUsageEntity(
                id = 1L,
                userId = userId,
                currency = "EUR",
                usageCount = 10,
                lastUsed = Date()
            )
            val entity2 = CurrencyUsageEntity(
                id = 2L,
                userId = userId,
                currency = "GBP",
                usageCount = 5,
                lastUsed = Date()
            )
            
            coEvery { mockDao.getCurrencyUsageByUser(userId) } returns 
                flowOf(listOf(entity1, entity2))
            
            // When
            val result = repository.getCurrencyUsageByUser(userId).first()
            
            // Then
            assertEquals(2, result.size)
            
            val eurUsage = result.find { it.currency == "EUR" }!!
            assertEquals(10, eurUsage.usageCount)
            assertEquals(userId, eurUsage.userId)
            
            val gbpUsage = result.find { it.currency == "GBP" }!!
            assertEquals(5, gbpUsage.usageCount)
            assertEquals(userId, gbpUsage.userId)
        }

        @Test
        @DisplayName("Should return empty list when no usage data")
        fun `should return empty list when no usage data`() = runTest {
            // Given
            val userId = 1L
            coEvery { mockDao.getCurrencyUsageByUser(userId) } returns flowOf(emptyList())
            
            // When
            val result = repository.getCurrencyUsageByUser(userId).first()
            
            // Then
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    @DisplayName("getTopCurrenciesByUser Method")
    inner class GetTopCurrenciesByUserMethod {

        @Test
        @DisplayName("Should return top currencies sorted by usage")
        fun `should return top currencies sorted by usage`() = runTest {
            // Given
            val userId = 1L
            val limit = 3
            val entity1 = CurrencyUsageEntity(
                id = 1L,
                userId = userId,
                currency = "EUR",
                usageCount = 15,
                lastUsed = Date()
            )
            val entity2 = CurrencyUsageEntity(
                id = 2L,
                userId = userId,
                currency = "GBP",
                usageCount = 10,
                lastUsed = Date()
            )
            val entity3 = CurrencyUsageEntity(
                id = 3L,
                userId = userId,
                currency = "JPY",
                usageCount = 5,
                lastUsed = Date()
            )
            
            coEvery { mockDao.getTopCurrenciesByUser(userId, limit) } returns 
                flowOf(listOf(entity1, entity2, entity3))
            
            // When
            val result = repository.getTopCurrenciesByUser(userId, limit).first()
            
            // Then
            assertEquals(3, result.size)
            assertEquals("EUR", result[0].currency)
            assertEquals(15, result[0].usageCount)
            assertEquals("GBP", result[1].currency)
            assertEquals(10, result[1].usageCount)
            assertEquals("JPY", result[2].currency)
            assertEquals(5, result[2].usageCount)
        }

        @Test
        @DisplayName("Should use default limit when not specified")
        fun `should use default limit when not specified`() = runTest {
            // Given
            val userId = 1L
            coEvery { mockDao.getTopCurrenciesByUser(userId, 10) } returns flowOf(emptyList())
            
            // When
            repository.getTopCurrenciesByUser(userId).first()
            
            // Then
            coVerify { mockDao.getTopCurrenciesByUser(userId, 10) }
        }
    }

    @Nested
    @DisplayName("getUserCurrenciesSortedByUsage Method")
    inner class GetUserCurrenciesSortedByUsageMethod {

        @Test
        @DisplayName("Should return currencies sorted by usage count")
        fun `should return currencies sorted by usage count`() = runTest {
            // Given
            val userId = 1L
            val entity1 = CurrencyUsageEntity(
                id = 1L,
                userId = userId,
                currency = "EUR",
                usageCount = 20,
                lastUsed = Date()
            )
            val entity2 = CurrencyUsageEntity(
                id = 2L,
                userId = userId,
                currency = "GBP",
                usageCount = 10,
                lastUsed = Date()
            )
            val entity3 = CurrencyUsageEntity(
                id = 3L,
                userId = userId,
                currency = "JPY",
                usageCount = 5,
                lastUsed = Date()
            )
            
            coEvery { mockDao.getUserCurrenciesSortedByUsage(userId) } returns 
                flowOf(listOf(entity1, entity2, entity3))
            
            // When
            val result = repository.getUserCurrenciesSortedByUsage(userId).first()
            
            // Then
            assertEquals(3, result.size)
            assertEquals("EUR", result[0].currency)
            assertEquals(20, result[0].usageCount)
            assertEquals("GBP", result[1].currency)
            assertEquals(10, result[1].usageCount)
            assertEquals("JPY", result[2].currency)
            assertEquals(5, result[2].usageCount)
        }
    }

    @Nested
    @DisplayName("incrementCurrencyUsage Method")
    inner class IncrementCurrencyUsageMethod {

        @Test
        @DisplayName("Should increment currency usage")
        fun `should increment currency usage`() = runTest {
            // Given
            val userId = 1L
            val currencyCode = "EUR"
            coEvery { mockDao.insertOrIncrementCurrencyUsage(any(), any(), any(), any(), any()) } just Runs
            
            // When
            repository.incrementCurrencyUsage(userId, currencyCode)
            
            // Then
            coVerify { mockDao.insertOrIncrementCurrencyUsage(any(), any(), any(), any(), any()) }
        }

        @Test
        @DisplayName("Should handle multiple increments")
        fun `should handle multiple increments`() = runTest {
            // Given
            val userId = 1L
            val currencyCode = "EUR"
            coEvery { mockDao.insertOrIncrementCurrencyUsage(any(), any(), any(), any(), any()) } just Runs
            
            // When
            repository.incrementCurrencyUsage(userId, currencyCode)
            repository.incrementCurrencyUsage(userId, currencyCode)
            repository.incrementCurrencyUsage(userId, currencyCode)
            
            // Then
            coVerify(exactly = 3) { mockDao.insertOrIncrementCurrencyUsage(any(), any(), any(), any(), any()) }
        }
    }




    @Nested
    @DisplayName("insertCurrencyUsage Method")
    inner class InsertCurrencyUsageMethod {

        @Test
        @DisplayName("Should insert currency usage and return domain model")
        fun `should insert currency usage and return domain model`() = runTest {
            // Given
            val currencyUsage = CurrencyUsage(
                id = 0L,
                userId = 1L,
                currency = "EUR",
                usageCount = 1,
                lastUsed = Date()
            )
            val entity = CurrencyUsageEntity(
                id = 0L,
                userId = 1L,
                currency = "EUR",
                usageCount = 1,
                lastUsed = currencyUsage.lastUsed
            )
            val insertedEntity = entity.copy(id = 1L)
            
            coEvery { mockDao.insertCurrencyUsage(any()) } returns 1L
            coEvery { mockDao.getCurrencyUsageById(1L) } returns insertedEntity
            
            // When
            val result = repository.insertCurrencyUsage(currencyUsage)
            
            // Then
            assertEquals(1L, result.id)
            assertEquals(currencyUsage.userId, result.userId)
            assertEquals(currencyUsage.currency, result.currency)
            assertEquals(currencyUsage.usageCount, result.usageCount)
        }
    }

    @Nested
    @DisplayName("updateCurrencyUsage Method")
    inner class UpdateCurrencyUsageMethod {

        @Test
        @DisplayName("Should update currency usage")
        fun `should update currency usage`() = runTest {
            // Given
            val currencyUsage = CurrencyUsage(
                id = 1L,
                userId = 1L,
                currency = "EUR",
                usageCount = 10,
                lastUsed = Date()
            )
            val entity = CurrencyUsageEntity(
                id = 1L,
                userId = 1L,
                currency = "EUR",
                usageCount = 10,
                lastUsed = currencyUsage.lastUsed
            )
            
            coEvery { mockDao.updateCurrencyUsage(any()) } just Runs
            
            // When
            repository.updateCurrencyUsage(currencyUsage)
            
            // Then
            coVerify { mockDao.updateCurrencyUsage(any()) }
        }
    }

    @Nested
    @DisplayName("deleteCurrencyUsage Method")
    inner class DeleteCurrencyUsageMethod {

        @Test
        @DisplayName("Should delete currency usage")
        fun `should delete currency usage`() = runTest {
            // Given
            val currencyUsage = CurrencyUsage(
                id = 1L,
                userId = 1L,
                currency = "EUR",
                usageCount = 5,
                lastUsed = Date()
            )
            val entity = CurrencyUsageEntity(
                id = 1L,
                userId = 1L,
                currency = "EUR",
                usageCount = 5,
                lastUsed = currencyUsage.lastUsed
            )
            
            coEvery { mockDao.deleteCurrencyUsage(any()) } just Runs
            
            // When
            repository.deleteCurrencyUsage(currencyUsage)
            
            // Then
            coVerify { mockDao.deleteCurrencyUsage(any()) }
        }
    }

    @Nested
    @DisplayName("deleteAllCurrencyUsageForUser Method")
    inner class DeleteAllCurrencyUsageForUserMethod {

        @Test
        @DisplayName("Should delete all currency usage for user")
        fun `should delete all currency usage for user`() = runTest {
            // Given
            val userId = 1L
            coEvery { mockDao.deleteAllCurrencyUsageForUser(userId) } just Runs
            
            // When
            repository.deleteAllCurrencyUsageForUser(userId)
            
            // Then
            coVerify { mockDao.deleteAllCurrencyUsageForUser(userId) }
        }
    }

    @Nested
    @DisplayName("Domain Model Mapping")
    inner class DomainModelMapping {

        @Test
        @DisplayName("Should correctly map entity to domain model")
        fun `should correctly map entity to domain model`() = runTest {
            // Given
            val entity = CurrencyUsageEntity(
                id = 1L,
                userId = 2L,
                currency = "EUR",
                usageCount = 10,
                lastUsed = Date(1234567890L)
            )
            
            // When
            val domainModel = entity.toDomainModel()
            
            // Then
            assertEquals(entity.id, domainModel.id)
            assertEquals(entity.userId, domainModel.userId)
            assertEquals(entity.currency, domainModel.currency)
            assertEquals(entity.usageCount, domainModel.usageCount)
            assertEquals(entity.lastUsed, domainModel.lastUsed)
        }

        @Test
        @DisplayName("Should correctly map domain model to entity")
        fun `should correctly map domain model to entity`() = runTest {
            // Given
            val domainModel = CurrencyUsage(
                id = 1L,
                userId = 2L,
                currency = "EUR",
                usageCount = 10,
                lastUsed = Date(1234567890L)
            )
            
            // When
            val entity = CurrencyUsageEntity.fromDomainModel(domainModel)
            
            // Then
            assertEquals(domainModel.id, entity.id)
            assertEquals(domainModel.userId, entity.userId)
            assertEquals(domainModel.currency, entity.currency)
            assertEquals(domainModel.usageCount, entity.usageCount)
            assertEquals(domainModel.lastUsed, entity.lastUsed)
        }
    }

    @Nested
    @DisplayName("getCurrencyUsageById Method")
    inner class GetCurrencyUsageByIdMethod {

        @Test
        @DisplayName("Should return currency usage by ID")
        fun `should return currency usage by ID`() = runTest {
            // Given
            val id = 1L
            val entity = CurrencyUsageEntity(
                id = id,
                userId = 1L,
                currency = "EUR",
                usageCount = 10,
                lastUsed = Date()
            )
            
            coEvery { mockDao.getCurrencyUsageById(id) } returns entity
            
            // When
            val result = repository.getCurrencyUsageById(id)
            
            // Then
            assertNotNull(result)
            assertEquals(id, result!!.id)
            assertEquals("EUR", result.currency)
            assertEquals(10, result.usageCount)
        }

        @Test
        @DisplayName("Should return null when currency usage not found")
        fun `should return null when currency usage not found`() = runTest {
            // Given
            val id = 999L
            coEvery { mockDao.getCurrencyUsageById(id) } returns null
            
            // When
            val result = repository.getCurrencyUsageById(id)
            
            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("getCurrencyUsageByUserAndCurrency Method")
    inner class GetCurrencyUsageByUserAndCurrencyMethod {

        @Test
        @DisplayName("Should return currency usage by user and currency")
        fun `should return currency usage by user and currency`() = runTest {
            // Given
            val userId = 1L
            val currency = "EUR"
            val entity = CurrencyUsageEntity(
                id = 1L,
                userId = userId,
                currency = currency,
                usageCount = 5,
                lastUsed = Date()
            )
            
            coEvery { mockDao.getCurrencyUsageByUserAndCurrency(userId, currency) } returns entity
            
            // When
            val result = repository.getCurrencyUsageByUserAndCurrency(userId, currency)
            
            // Then
            assertNotNull(result)
            assertEquals(userId, result!!.userId)
            assertEquals(currency, result.currency)
            assertEquals(5, result.usageCount)
        }

        @Test
        @DisplayName("Should return null when currency usage not found")
        fun `should return null when currency usage not found`() = runTest {
            // Given
            val userId = 1L
            val currency = "JPY"
            coEvery { mockDao.getCurrencyUsageByUserAndCurrency(userId, currency) } returns null
            
            // When
            val result = repository.getCurrencyUsageByUserAndCurrency(userId, currency)
            
            // Then
            assertNull(result)
        }
    }

    @Nested
    @DisplayName("getCurrencyUsageCountForUser Method")
    inner class GetCurrencyUsageCountForUserMethod {

        @Test
        @DisplayName("Should return currency usage count for user")
        fun `should return currency usage count for user`() = runTest {
            // Given
            val userId = 1L
            coEvery { mockDao.getCurrencyUsageCountForUser(userId) } returns 3
            
            // When
            val result = repository.getCurrencyUsageCountForUser(userId)
            
            // Then
            assertEquals(3, result)
        }

        @Test
        @DisplayName("Should return zero when no currency usage")
        fun `should return zero when no currency usage`() = runTest {
            // Given
            val userId = 1L
            coEvery { mockDao.getCurrencyUsageCountForUser(userId) } returns 0
            
            // When
            val result = repository.getCurrencyUsageCountForUser(userId)
            
            // Then
            assertEquals(0, result)
        }
    }

    @Nested
    @DisplayName("Error Handling")
    inner class ErrorHandling {

        @Test
        @DisplayName("Should handle DAO errors gracefully")
        fun `should handle DAO errors gracefully`() = runTest {
            // Given
            val userId = 1L
            coEvery { mockDao.getCurrencyUsageByUser(userId) } throws Exception("Database error")
            
            // When & Then
            assertThrows(Exception::class.java) {
                runTest { repository.getCurrencyUsageByUser(userId).first() }
            }
        }

        @Test
        @DisplayName("Should handle null DAO responses")
        fun `should handle null DAO responses`() = runTest {
            // Given
            val userId = 1L
            coEvery { mockDao.getCurrencyUsageByUser(userId) } returns flowOf(null)
            
            // When
            val result = repository.getCurrencyUsageByUser(userId).first()
            
            // Then
            assertTrue(result.isEmpty())
        }
    }
}
