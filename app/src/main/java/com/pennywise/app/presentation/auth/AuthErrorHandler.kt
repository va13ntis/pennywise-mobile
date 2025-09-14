package com.pennywise.app.presentation.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handler for authentication errors and edge cases
 * Provides comprehensive error handling and recovery mechanisms
 */
@Singleton
class AuthErrorHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceAuthService: DeviceAuthService
) {
    
    private val failedAttemptsKey = intPreferencesKey("auth_failed_attempts")
    private val lastFailedAttemptKey = intPreferencesKey("last_failed_attempt_timestamp")
    
    companion object {
        private const val MAX_FAILED_ATTEMPTS = 5
        private const val LOCKOUT_DURATION_MS = 5 * 60 * 1000 // 5 minutes
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_error_preferences")
    }
    
    /**
     * Handle authentication failure
     */
    suspend fun handleAuthenticationFailure() {
        try {
            val preferences = context.dataStore.data.first()
            val currentAttempts = preferences[failedAttemptsKey] ?: 0
            val newAttempts = currentAttempts + 1
            
            context.dataStore.edit { prefs ->
                prefs[failedAttemptsKey] = newAttempts
                prefs[lastFailedAttemptKey] = System.currentTimeMillis().toInt()
            }
            
            if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                // Disable device authentication temporarily
                deviceAuthService.setDeviceAuthEnabled(false)
            }
        } catch (e: Exception) {
            // Log error but don't crash
            println("❌ AuthErrorHandler: Error handling authentication failure: ${e.message}")
        }
    }
    
    /**
     * Handle successful authentication - reset failed attempts
     */
    suspend fun handleAuthenticationSuccess() {
        try {
            context.dataStore.edit { prefs ->
                prefs.remove(failedAttemptsKey)
                prefs.remove(lastFailedAttemptKey)
            }
        } catch (e: Exception) {
            println("❌ AuthErrorHandler: Error handling authentication success: ${e.message}")
        }
    }
    
    /**
     * Check if authentication is currently locked out
     */
    suspend fun isAuthenticationLocked(): Boolean {
        return try {
            val preferences = context.dataStore.data.first()
            val failedAttempts = preferences[failedAttemptsKey] ?: 0
            val lastFailedAttempt = preferences[lastFailedAttemptKey] ?: 0
            
            if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                val timeSinceLastAttempt = System.currentTimeMillis() - lastFailedAttempt
                timeSinceLastAttempt < LOCKOUT_DURATION_MS
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get remaining lockout time in seconds
     */
    suspend fun getRemainingLockoutTime(): Long {
        return try {
            val preferences = context.dataStore.data.first()
            val lastFailedAttempt = preferences[lastFailedAttemptKey] ?: 0
            val timeSinceLastAttempt = System.currentTimeMillis() - lastFailedAttempt
            val remainingTime = LOCKOUT_DURATION_MS - timeSinceLastAttempt
            if (remainingTime > 0) remainingTime / 1000 else 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Get the number of failed attempts
     */
    suspend fun getFailedAttempts(): Int {
        return try {
            val preferences = context.dataStore.data.first()
            preferences[failedAttemptsKey] ?: 0
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Reset authentication lockout (for testing or admin purposes)
     */
    suspend fun resetAuthenticationLockout() {
        try {
            context.dataStore.edit { prefs ->
                prefs.remove(failedAttemptsKey)
                prefs.remove(lastFailedAttemptKey)
            }
        } catch (e: Exception) {
            println("❌ AuthErrorHandler: Error resetting authentication lockout: ${e.message}")
        }
    }
    
    /**
     * Handle biometric authentication errors
     */
    fun handleBiometricError(errorCode: Int, errorMessage: String): String {
        return when (errorCode) {
            androidx.biometric.BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                "Biometric hardware is currently unavailable. Please try again later."
            }
            androidx.biometric.BiometricPrompt.ERROR_UNABLE_TO_PROCESS -> {
                "Unable to process biometric data. Please try again."
            }
            androidx.biometric.BiometricPrompt.ERROR_TIMEOUT -> {
                "Authentication timed out. Please try again."
            }
            androidx.biometric.BiometricPrompt.ERROR_NO_SPACE -> {
                "Insufficient storage for biometric data. Please free up space and try again."
            }
            androidx.biometric.BiometricPrompt.ERROR_CANCELED -> {
                "Authentication was canceled."
            }
            androidx.biometric.BiometricPrompt.ERROR_LOCKOUT -> {
                "Too many failed attempts. Please try again later."
            }
            androidx.biometric.BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                "Biometric authentication is permanently locked. Please use password authentication."
            }
            androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED -> {
                "Authentication was canceled by user."
            }
            androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                "User chose to use password instead."
            }
            androidx.biometric.BiometricPrompt.ERROR_HW_NOT_PRESENT -> {
                "Biometric hardware is not available on this device."
            }
            else -> {
                "Authentication error: $errorMessage"
            }
        }
    }
    
    /**
     * Handle general authentication errors
     */
    fun handleGeneralAuthError(error: Throwable): String {
        return when (error) {
            is SecurityException -> {
                "Security error: ${error.message}"
            }
            is IllegalStateException -> {
                "Authentication system is in an invalid state. Please restart the app."
            }
            is UnsupportedOperationException -> {
                "This authentication method is not supported on this device."
            }
            else -> {
                "Authentication failed: ${error.message ?: "Unknown error"}"
            }
        }
    }
}
