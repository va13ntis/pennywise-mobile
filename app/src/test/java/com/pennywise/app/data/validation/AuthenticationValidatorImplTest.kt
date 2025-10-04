package com.pennywise.app.data.validation

import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.presentation.auth.DeviceAuthService
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class AuthenticationValidatorImplTest {

    private lateinit var userRepository: UserRepository
    private lateinit var deviceAuthService: DeviceAuthService
    private lateinit var authenticationValidator: AuthenticationValidatorImpl

    @Before
    fun setUp() {
        userRepository = mockk()
        deviceAuthService = mockk()
        authenticationValidator = AuthenticationValidatorImpl(userRepository, deviceAuthService)
    }

    @Test
    fun `validateUserAuthenticated should return false when user is null`() = runTest {
        // Given
        coEvery { userRepository.getUser() } returns null

        // When
        val result = authenticationValidator.validateUserAuthenticated()

        // Then
        assertFalse(result)
    }

    @Test
    fun `validateUserAuthenticated should return true when user exists and device auth is disabled`() = runTest {
        // Given
        val user = User(
            id = 1L,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false
        )
        coEvery { userRepository.getUser() } returns user

        // When
        val result = authenticationValidator.validateUserAuthenticated()

        // Then
        assertTrue(result)
    }

    @Test
    fun `validateUserAuthenticated should return true when user exists, device auth is enabled and device auth is available`() = runTest {
        // Given
        val user = User(
            id = 1L,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = true
        )
        coEvery { userRepository.getUser() } returns user
        every { deviceAuthService.canUseDeviceAuth() } returns true

        // When
        val result = authenticationValidator.validateUserAuthenticated()

        // Then
        assertTrue(result)
    }

    @Test
    fun `validateUserAuthenticated should return false when user exists, device auth is enabled but device auth is not available`() = runTest {
        // Given
        val user = User(
            id = 1L,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = true
        )
        coEvery { userRepository.getUser() } returns user
        every { deviceAuthService.canUseDeviceAuth() } returns false

        // When
        val result = authenticationValidator.validateUserAuthenticated()

        // Then
        assertFalse(result)
    }

    @Test
    fun `validateUserAuthenticated should return false when user exists but device auth is enabled and device auth check fails`() = runTest {
        // Given
        val user = User(
            id = 1L,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = true
        )
        coEvery { userRepository.getUser() } returns user
        every { deviceAuthService.canUseDeviceAuth() } returns false

        // When
        val result = authenticationValidator.validateUserAuthenticated()

        // Then
        assertFalse(result)
    }
}
