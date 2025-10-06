package com.pennywise.app.data.repository

import com.pennywise.app.domain.validation.AuthenticationValidator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BaseAuthenticatedRepository
 * Tests authentication validation wrapper functionality
 */
class BaseAuthenticatedRepositoryTest {
    
    private lateinit var authValidator: AuthenticationValidator
    private lateinit var repository: TestAuthenticatedRepository
    
    @Before
    fun setup() {
        authValidator = mockk()
        repository = TestAuthenticatedRepository(authValidator)
    }
    
    @Test
    fun `withAuthentication executes block when authentication succeeds`() = runTest {
        // Given
        val expectedResult = "Success"
        coEvery { authValidator.validateUserAuthenticated() } returns true
        
        // When
        val result = repository.testOperation { expectedResult }
        
        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `withAuthentication throws SecurityException when authentication fails`() = runTest {
        // Given
        coEvery { authValidator.validateUserAuthenticated() } returns false
        
        // When & Then
        val exception = assertThrows(SecurityException::class.java) {
            runTest {
                @Suppress("UNUSED_EXPRESSION")
                repository.testOperation { "Should not execute" }
            }
        }
        
        assertEquals("Authentication required for database operations", exception.message)
        coVerify(exactly = 1) { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `withAuthentication propagates exceptions from block`() = runTest {
        // Given
        val expectedException = RuntimeException("Database error")
        coEvery { authValidator.validateUserAuthenticated() } returns true
        
        // When & Then
        val exception = assertThrows(RuntimeException::class.java) {
            runTest {
                repository.testOperation { throw expectedException }
            }
        }
        
        assertEquals("Database error", exception.message)
        coVerify(exactly = 1) { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `withAuthentication validates authentication before executing block`() = runTest {
        // Given
        var blockExecuted = false
        coEvery { authValidator.validateUserAuthenticated() } returns false
        
        // When
        try {
            repository.testOperation {
                blockExecuted = true
                "Result"
            }
        } catch (e: SecurityException) {
            // Expected
        }
        
        // Then
        assertEquals(false, blockExecuted)
        coVerify(exactly = 1) { authValidator.validateUserAuthenticated() }
    }
    
    @Test
    fun `withAuthentication returns correct type from block`() = runTest {
        // Given
        val expectedInt = 42
        val expectedString = "test"
        val expectedList = listOf(1, 2, 3)
        coEvery { authValidator.validateUserAuthenticated() } returns true
        
        // When
        val intResult = repository.testOperation { expectedInt }
        val stringResult = repository.testOperation { expectedString }
        val listResult = repository.testOperation { expectedList }
        
        // Then
        assertEquals(expectedInt, intResult)
        assertEquals(expectedString, stringResult)
        assertEquals(expectedList, listResult)
    }
    
    /**
     * Test implementation of BaseAuthenticatedRepository for testing purposes
     */
    private class TestAuthenticatedRepository(
        authValidator: AuthenticationValidator
    ) : BaseAuthenticatedRepository(authValidator) {
        
        suspend fun <T> testOperation(block: suspend () -> T): T {
            return withAuthentication(block)
        }
    }
}

