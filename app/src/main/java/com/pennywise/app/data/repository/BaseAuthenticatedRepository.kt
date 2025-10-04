package com.pennywise.app.data.repository

import com.pennywise.app.domain.validation.AuthenticationValidator

/**
 * Base repository class that provides authentication validation for sensitive operations.
 * All repository implementations that require authentication should extend this class
 * and wrap their database operations with the [withAuthentication] function.
 *
 * @property authValidator Validator to check user authentication state
 */
abstract class BaseAuthenticatedRepository(
    private val authValidator: AuthenticationValidator
) {
    
    /**
     * Wraps a database operation with authentication validation.
     * Throws [SecurityException] if the user is not authenticated.
     *
     * @param T The return type of the operation
     * @param block The database operation to execute after authentication validation
     * @return The result of the database operation
     * @throws SecurityException if authentication validation fails
     */
    protected suspend fun <T> withAuthentication(block: suspend () -> T): T {
        if (!authValidator.validateUserAuthenticated()) {
            throw SecurityException("Authentication required for database operations")
        }
        return block()
    }
}
