package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.PaymentMethodConfigDao
import com.pennywise.app.data.local.entity.PaymentMethodConfigEntity
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig
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

/**
 * Unit tests for PaymentMethodConfigRepositoryImpl
 * Verifies authentication validation is properly applied to all methods
 */
class PaymentMethodConfigRepositoryImplTest {
    
    private lateinit var paymentMethodConfigDao: PaymentMethodConfigDao
    private lateinit var authValidator: AuthenticationValidator
    private lateinit var repository: PaymentMethodConfigRepositoryImpl
    
    private val testUserId = 1L
    private val testConfigId = 1L
    
    private val testConfig = PaymentMethodConfig(
        id = testConfigId,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        alias = "Test Credit Card",
        isDefault = true,
        withdrawDay = 15,
        isActive = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    
    private val testConfigEntity = PaymentMethodConfigEntity(
        id = testConfigId,
        paymentMethod = PaymentMethod.CREDIT_CARD,
        alias = "Test Credit Card",
        isDefault = true,
        withdrawDay = 15,
        isActive = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    
    @Before
    fun setup() {
        paymentMethodConfigDao = mockk()
        authValidator = mockk()
        repository = PaymentMethodConfigRepositoryImpl(paymentMethodConfigDao, authValidator)
    }
    
    @Test
    fun `getPaymentMethodConfigs returns flow when authenticated`() = runTest {
        // Given
        val entities = listOf(testConfigEntity)
        coEvery { authValidator.validateUserAuthenticated() } returns true
        every { paymentMethodConfigDao.getAllPaymentMethodConfigs() } returns flowOf(entities)
        
        // When
        val result = repository.getPaymentMethodConfigs().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(testConfig, result[0])
        coVerify { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `getPaymentMethodConfigs throws SecurityException when not authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns false
        
        // When & Then
        val exception = assertThrows(SecurityException::class.java) {
            runTest {
                repository.getPaymentMethodConfigs().first()
            }
        }
        
        assertEquals("Authentication required for database operations", exception.message)
        coVerify { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `getPaymentMethodConfigById succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { paymentMethodConfigDao.getPaymentMethodConfigById(testConfigId) } returns testConfigEntity
        
        // When
        val result = repository.getPaymentMethodConfigById(testConfigId)
        
        // Then
        assertEquals(testConfig, result)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { paymentMethodConfigDao.getPaymentMethodConfigById(testConfigId) }
    }
    
    @Test
    fun `getPaymentMethodConfigById throws SecurityException when not authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns false
        
        // When & Then
        val exception = assertThrows(SecurityException::class.java) {
            runTest {
                repository.getPaymentMethodConfigById(testConfigId)
            }
        }
        
        assertEquals("Authentication required for database operations", exception.message)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify(exactly = 0) { paymentMethodConfigDao.getPaymentMethodConfigById(any()) }
    }
    
    @Test
    fun `insertPaymentMethodConfig succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { paymentMethodConfigDao.insertPaymentMethodConfig(any()) } returns testConfigId
        
        // When
        val result = repository.insertPaymentMethodConfig(testConfig)
        
        // Then
        assertEquals(testConfigId, result)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { paymentMethodConfigDao.insertPaymentMethodConfig(any()) }
    }
    
    @Test
    fun `insertPaymentMethodConfig throws SecurityException when not authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns false
        
        // When & Then
        val exception = assertThrows(SecurityException::class.java) {
            runTest {
                repository.insertPaymentMethodConfig(testConfig)
            }
        }
        
        assertEquals("Authentication required for database operations", exception.message)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify(exactly = 0) { paymentMethodConfigDao.insertPaymentMethodConfig(any()) }
    }
    
    @Test
    fun `updatePaymentMethodConfig succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { paymentMethodConfigDao.updatePaymentMethodConfig(any()) } returns Unit
        
        // When
        repository.updatePaymentMethodConfig(testConfig)
        
        // Then
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { paymentMethodConfigDao.updatePaymentMethodConfig(any()) }
    }
    
    @Test
    fun `deletePaymentMethodConfig succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { paymentMethodConfigDao.deletePaymentMethodConfig(testConfigId) } returns Unit
        
        // When
        repository.deletePaymentMethodConfig(testConfigId)
        
        // Then
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { paymentMethodConfigDao.deletePaymentMethodConfig(testConfigId) }
    }
    
    @Test
    fun `setDefaultPaymentMethodConfig succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { paymentMethodConfigDao.setDefaultPaymentMethodConfig(testConfigId) } returns Unit
        
        // When
        repository.setDefaultPaymentMethodConfig(testConfigId)
        
        // Then
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { paymentMethodConfigDao.setDefaultPaymentMethodConfig(testConfigId) }
    }
    
    @Test
    fun `getPaymentMethodConfigCount succeeds when authenticated`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns true
        coEvery { paymentMethodConfigDao.getPaymentMethodConfigCount() } returns 3
        
        // When
        val result = repository.getPaymentMethodConfigCount()
        
        // Then
        assertEquals(3, result)
        coVerify { authValidator.validateUserAuthenticated() }
        coVerify { paymentMethodConfigDao.getPaymentMethodConfigCount() }
    }
    
    @Test
    fun `getCreditCardConfigs returns flow when authenticated`() = runTest {
        // Given
        val entities = listOf(testConfigEntity)
        coEvery { authValidator.validateUserAuthenticated() } returns true
        every { paymentMethodConfigDao.getCreditCardConfigs() } returns flowOf(entities)
        
        // When
        val result = repository.getCreditCardConfigs().first()
        
        // Then
        assertEquals(1, result.size)
        assertEquals(testConfig, result[0])
        coVerify { authValidator.validateUserAuthenticated() }
    }
}

