package com.pennywise.app.domain.validation

/**
 * Interface for validating user authentication state
 */
interface AuthenticationValidator {
    
    /**
     * Validates if the user is properly authenticated
     * @return true if user is authenticated, false otherwise
     */
    suspend fun validateUserAuthenticated(): Boolean
}
