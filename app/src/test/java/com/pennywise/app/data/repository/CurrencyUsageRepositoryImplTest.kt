package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.validation.AuthenticationValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Unit tests for CurrencyUsageRepositoryImpl
 * Verifies authentication validation is properly applied to all methods
 */
class CurrencyUsageRepositoryImplTest {
    
    private lateinit var currencyUsageDao: CurrencyUsageDao
    private lateinit var authValidator: AuthenticationValidator
    private lateinit var repository: CurrencyUsageRepositoryImpl
    
    private val testUserId = 1L
    private val testCurrency = "USD"
    private val testDate = Date()
    
    private val testCurrencyUsage = CurrencyUsage(
        id = 1L,
        currency = testCurrency,
        usageCount = 5,
        lastUsed = testDate,
        createdAt = testDate,
        updatedAt = testDate
    )
    
    private val testCurrencyUsageEntity = CurrencyUsageEntity(
        id = 1L,
        currency = testCurrency,
        usageCount = 5,
        lastUsed = testDate,
        createdAt = testDate,
        updatedAt = testDate
    )
    
    @Before
    fun setup() {
        currencyUsageDao = mockk()
        authValidator = mockk()
        repository = CurrencyUsageRepositoryImpl(currencyUsageDao, authValidator)
    }
    
    @Test
    fun `insertCurrencyUsage succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { currencyUsageDao.insertCurrencyUsage(any()) } returns 1L
        
        // When
        val result = repository.insertCurrencyUsage(testCurrencyUsage)
        
        // Then
        assertEquals(1L, result)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { currencyUsageDao.insertCurrencyUsage(any()) }
    }
    
    @Test
    fun `insertCurrencyUsage throws SecurityException when not authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns false
        
        // When & Then
        val exception = assertThrows(SecurityException::class.java) {
            runTest {
                repository.insertCurrencyUsage(testCurrencyUsage)
            }
        }
        
        assertEquals("Authentication required for database operations", exception.message)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify(exactly = 0) { currencyUsageDao.insertCurrencyUsage(any()) }
    }
    
    @Test
    fun `updateCurrencyUsage succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { currencyUsageDao.updateCurrencyUsage(any()) } returns Unit
        
        // When
        repository.updateCurrencyUsage(testCurrencyUsage)
        
        // Then
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { currencyUsageDao.updateCurrencyUsage(any()) }
    }
    
    @Test
    fun `deleteCurrencyUsage succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { currencyUsageDao.deleteCurrencyUsage(any()) } returns Unit
        
        // When
        repository.deleteCurrencyUsage(testCurrencyUsage)
        
        // Then
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { currencyUsageDao.deleteCurrencyUsage(any()) }
    }
    
    @Test
    fun `getCurrencyUsageById succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { currencyUsageDao.getCurrencyUsageById(1L) } returns testCurrencyUsageEntity
        
        // When
        val result = repository.getCurrencyUsageById(1L)
        
        // Then
        assertEquals(testCurrencyUsage, result)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { currencyUsageDao.getCurrencyUsageById(1L) }
    }
    
    @Test
    fun `getCurrencyUsageByCurrency succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
            coEvery { currencyUsageDao.getCurrencyUsageByCurrency(testCurrency) } returns testCurrencyUsageEntity
        
        // When
        val result = repository.getCurrencyUsageByCurrency(testCurrency)
        
        // Then
        assertEquals(testCurrencyUsage, result)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { currencyUsageDao.getCurrencyUsageByCurrency(testCurrency) }
    }
    
    @Test
    fun `getCurrencyUsage returns flow when authenticated`() = runTest {
        // Given
        val entities = listOf(testCurrencyUsageEntity)
        coEvery { authValidator.validateUserAuthenticated() } returns true
            every { currencyUsageDao.getAllCurrencyUsage() } returns flowOf(entities)
        
        // When
        val result = repository.getCurrencyUsage().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(testCurrencyUsage, result[0])
        coVerify { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `getCurrencyUsage throws SecurityException when not authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns false
        
        // When & Then
        val exception = assertThrows(SecurityException::class.java) {
            runTest {
                repository.getCurrencyUsage().first()
            }
        }
        
        assertEquals("Authentication required for database operations", exception.message)
        coVerify { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `getTopCurrencies returns flow when authenticated`() = runTest {
        // Given
        val entities = listOf(testCurrencyUsageEntity)
        coEvery { authValidator.validateUserAuthenticated() } returns true
            every { currencyUsageDao.getTopCurrencies(5) } returns flowOf(entities)
        
        // When
        val result = repository.getTopCurrencies(5).first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(testCurrencyUsage, result[0])
        coVerify { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `getCurrenciesSortedByUsage returns flow when authenticated`() = runTest {
        // Given
        val entities = listOf(testCurrencyUsageEntity)
        coEvery { authValidator.validateUserAuthenticated() } returns true
            every { currencyUsageDao.getCurrencyUsageSortedByUsage() } returns flowOf(entities)
        
        // When
        val result = repository.getCurrenciesSortedByUsage().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(testCurrencyUsage, result[0])
        coVerify { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `incrementCurrencyUsage succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { currencyUsageDao.insertOrIncrementCurrencyUsage(any(), any(), any(), any()) } returns Unit
        
        // When
        repository.incrementCurrencyUsage(testCurrency)
        
        // Then
        coVerify { authValidator.validateUserAuthenticated() }
            coVerify { currencyUsageDao.insertOrIncrementCurrencyUsage(any(), any(), any(), any()) }
    }
    
    @Test
    fun `deleteAllCurrencyUsage succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { currencyUsageDao.deleteAllCurrencyUsage() } returns Unit
        
        // When
        repository.deleteAllCurrencyUsage()
        
        // Then
        coVerify { authValidator.validateUserAuthenticated() }
            coVerify { currencyUsageDao.deleteAllCurrencyUsage() }
    }
    
    @Test
    fun `getCurrencyUsageCount succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
            coEvery { currencyUsageDao.getCurrencyUsageCount() } returns 5
        
        // When
        val result = repository.getCurrencyUsageCount()
        
        // Then
        assertEquals(5, result)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { currencyUsageDao.getCurrencyUsageCount() }
    }
}

