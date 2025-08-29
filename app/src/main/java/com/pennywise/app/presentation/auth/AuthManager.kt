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
    private val userRepository: UserRepository
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
            val preferences = context.dataStore.data.first()
            val userId = preferences[userIdKey]
            val username = preferences[usernameKey]
            val isLoggedIn = preferences[isLoggedInKey] ?: false
            
            if (isLoggedIn && userId != null && username != null) {
                // Verify the user still exists in the database
                val user = userRepository.getUserById(userId.toLong())
                if (user != null) {
                    _currentUser.value = user
                } else {
                    // User no longer exists, clear stored credentials
                    logout()
                }
            }
        } catch (e: Exception) {
            // If there's any error during initialization, just clear the state
            _currentUser.value = null
        }
    }
    
    /**
     * Save authenticated user to persistent storage
     */
    suspend fun saveAuthenticatedUser(user: User) {
        try {
            context.dataStore.edit { preferences ->
                preferences[userIdKey] = user.id.toInt()
                preferences[usernameKey] = user.username
                preferences[isLoggedInKey] = true
            }
            _currentUser.value = user
        } catch (e: Exception) {
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
        } catch (e: Exception) {
            // If there's an error clearing preferences, just clear the current user
        } finally {
            _currentUser.value = null
        }
    }
    
    /**
     * Get the current authenticated user
     */
    fun getCurrentUser(): User? = _currentUser.value
    
    /**
     * Check if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean = _currentUser.value != null
    
    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_preferences")
    }
}

