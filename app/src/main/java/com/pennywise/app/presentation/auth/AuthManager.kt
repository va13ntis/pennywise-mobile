package com.pennywise.app.presentation.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) {
    
    private val isLoggedInKey = booleanPreferencesKey("is_logged_in")
    
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser
    
    val isAuthenticated: Flow<Boolean> = _currentUser.map { it != null }
    
    /**
     * Initialize authentication state by loading the user from database
     */
    suspend fun initializeAuthState() {
        try {
            val user = userRepository.getUser()
            if (user != null) {
                _currentUser.value = user
                println("✅ AuthManager: User found in database")
            } else {
                _currentUser.value = null
                println("ℹ️ AuthManager: No user found in database")
            }
        } catch (e: Exception) {
            // If there's any error during initialization, just clear the state
            println("❌ AuthManager: Error during initialization: ${e.message}")
            _currentUser.value = null
        }
    }
    
    /**
     * Save the authenticated user
     */
    suspend fun saveAuthenticatedUser(user: User) {
        try {
            println("🔄 AuthManager: Setting authenticated user")
            context.dataStore.edit { preferences ->
                preferences[isLoggedInKey] = true
            }
            _currentUser.value = user
            println("✅ AuthManager: User set successfully")
        } catch (e: Exception) {
            println("❌ AuthManager: Error saving user: ${e.message}")
            // If there's an error saving preferences, still set the current user
            _currentUser.value = user
        }
    }
    
    /**
     * Clear authentication state
     */
    suspend fun logout() {
        try {
            context.dataStore.edit { preferences ->
                preferences[isLoggedInKey] = false
            }
        } catch (e: Exception) {
            // If there's an error clearing preferences, just clear the current user
        } finally {
            _currentUser.value = null
        }
    }
    
    
    /**
     * Update the current user object (useful when user data changes)
     */
    suspend fun updateCurrentUser(updatedUser: User) {
        try {
            println("🔄 AuthManager: Updating current user")
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