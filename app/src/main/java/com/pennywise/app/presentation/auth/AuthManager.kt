package com.pennywise.app.presentation.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central authentication state manager to handle user session persistence
 * and provide the current authentication state to the rest of the application
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository,
    private val userRegistrationManager: UserRegistrationManager,
    private val authMigrationManager: AuthMigrationManager
) {
    
    private val userIdKey = intPreferencesKey("user_id")
    private val usernameKey = stringPreferencesKey("username")
    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    val isAuthenticated: Flow<Boolean> = _currentUser.map { it != null }
    
    /**
     * Initialize authentication state from stored preferences
     */
    suspend fun initializeAuthState() {
        try {
            // First, try to restore the current user from storage
            val preferences = context.dataStore.data.first()
            val userId = preferences[userIdKey]
            val username = preferences[usernameKey]
            val isLoggedIn = preferences[isLoggedInKey] ?: false
            
            if (isLoggedIn && userId != null && username != null) {
                // Verify the user still exists in the database
                val user = userRepository.getUserById(userId.toLong())
                if (user != null) {
                    _currentUser.value = user
                    println("✅ AuthManager: User restored from storage: ${user.username} (ID: ${user.id})")
                } else {
                    // User no longer exists, clear stored credentials
                    logout()
                }
            }
            
            // Perform migrations after restoring user state (if not cancelled)
            try {
                authMigrationManager.performMigrations()
                authMigrationManager.handleAuthenticationEdgeCases()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Migration was cancelled (likely due to activity restart) - this is OK
                println("ℹ️ AuthManager: Migration cancelled during restart - continuing with current state")
            } catch (e: Exception) {
                // Other migration errors - log but don't affect auth state
                println("❌ AuthManager: Migration error: ${e.message}")
            }
        } catch (e: Exception) {
            // If there's any error during initialization, just clear the state
            println("❌ AuthManager: Error during initialization: ${e.message}")
            _currentUser.value = null
        }
    }
    
    /**
     * Save authenticated user to persistent storage
     */
    suspend fun saveAuthenticatedUser(user: User) {
        try {
            println("🔄 AuthManager: Saving authenticated user ${user.username} (ID: ${user.id})")
            context.dataStore.edit { preferences ->
                preferences[userIdKey] = user.id.toInt()
                preferences[usernameKey] = user.username
                preferences[isLoggedInKey] = true
            }
            // Mark user as registered on this device
            userRegistrationManager.setUserRegistered(true)
            _currentUser.value = user
            println("✅ AuthManager: User saved successfully")
        } catch (e: Exception) {
            println("❌ AuthManager: Error saving user: ${e.message}")
            // If there's an error saving preferences, still set the current user
            _currentUser.value = user
        }
    }
    
    /**
     * Clear authentication state and stored credentials
     */
    suspend fun logout() {
        try {
            context.dataStore.edit { preferences ->
                preferences.remove(userIdKey)
                preferences.remove(usernameKey)
                preferences[isLoggedInKey] = false
            }
            // Clear registration status when logging out
            userRegistrationManager.clearRegistrationStatus()
        } catch (e: Exception) {
            // If there's an error clearing preferences, just clear the current user
        } finally {
            _currentUser.value = null
        }
    }
    
    /**
     * Get the current authenticated user
     */
    fun getCurrentUser(): User? {
        val user = _currentUser.value
        println("🔄 AuthManager: getCurrentUser() called, returning: ${user?.username} (ID: ${user?.id})")
        return user
    }
    
    /**
     * Update the current user object (useful when user data changes)
     */
    suspend fun updateCurrentUser(updatedUser: User) {
        try {
            println("🔄 AuthManager: Updating current user to ${updatedUser.username} (ID: ${updatedUser.id})")
            // Update the user in the database
            userRepository.updateUser(updatedUser)
            // Update the current user state
            _currentUser.value = updatedUser
            println("✅ AuthManager: Current user updated successfully")
        } catch (e: Exception) {
            println("❌ AuthManager: Error updating current user: ${e.message}")
            throw e
        }
    }
    
    /**
     * Check if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean = _currentUser.value != null
    
    /**
     * Get the registration status flow
     */
    fun getRegistrationStatus(): Flow<Boolean> = userRegistrationManager.isUserRegistered
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")
    }
}

