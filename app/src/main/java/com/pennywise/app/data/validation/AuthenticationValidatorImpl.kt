package com.pennywise.app.data.validation

import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.domain.validation.AuthenticationValidator
import com.pennywise.app.presentation.auth.DeviceAuthService
import javax.inject.Inject

/**
 * Implementation of AuthenticationValidator interface
 * Validates user authentication state by checking user existence and device authentication
 */
class AuthenticationValidatorImpl @Inject constructor(
    private val userRepository: UserRepository,
    private val deviceAuthService: DeviceAuthService
) : AuthenticationValidator {
    
    override suspend fun validateUserAuthenticated(): Boolean {
        // Check if user exists and is authenticated
        val user = userRepository.getUser()
        return user != null && (!user.deviceAuthEnabled || deviceAuthService.canUseDeviceAuth())
    }
}
