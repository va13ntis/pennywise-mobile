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
 * Simplified for single-user per app with device authentication
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) {
    
    private val userIdKey = intPreferencesKey("user_id")
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
            val isLoggedIn = preferences[isLoggedInKey] ?: false
            
            if (isLoggedIn && userId != null) {
                // Verify the user still exists in the database
                val user = userRepository.getUserById(userId.toLong())
                if (user != null) {
                    _currentUser.value = user
                    println("✅ AuthManager: User restored from storage (ID: ${user.id})")
                } else {
                    // User no longer exists, clear stored credentials
                    logout()
                }
            } else {
                // No stored user, try to get the single user from database
                val user = userRepository.getSingleUser()
                if (user != null) {
                    _currentUser.value = user
                    println("✅ AuthManager: Single user found in database (ID: ${user.id})")
                }
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
            println("🔄 AuthManager: Saving authenticated user (ID: ${user.id})")
            context.dataStore.edit { preferences ->
                preferences[userIdKey] = user.id.toInt()
                preferences[isLoggedInKey] = true
            }
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
                preferences[isLoggedInKey] = false
            }
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
        println("🔄 AuthManager: getCurrentUser() called, returning user (ID: ${user?.id})")
        return user
    }
    
    /**
     * Update the current user object (useful when user data changes)
     */
    suspend fun updateCurrentUser(updatedUser: User) {
        try {
            println("🔄 AuthManager: Updating current user (ID: ${updatedUser.id})")
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
    
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")
    }
}