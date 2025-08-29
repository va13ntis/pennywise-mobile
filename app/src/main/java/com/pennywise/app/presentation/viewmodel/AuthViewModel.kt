package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.User
import com.pennywise.app.presentation.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing authentication state across the app
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated
    val currentUser: StateFlow<User?> = authManager.currentUser
    
    init {
        // Observe the authentication state from AuthManager
        viewModelScope.launch {
            authManager.isAuthenticated.collect { isAuthenticated ->
                _isAuthenticated.value = isAuthenticated
            }
        }
    }
    
    /**
     * Initialize authentication state when the app starts
     */
    fun initializeAuthState() {
        viewModelScope.launch {
            try {
                authManager.initializeAuthState()
            } catch (e: Exception) {
                // If initialization fails, just continue with unauthenticated state
            }
        }
    }
    
    /**
     * Logout the current user
     */
    fun logout() {
        viewModelScope.launch {
            authManager.logout()
        }
    }
    
    /**
     * Get the current authenticated user
     */
    fun getCurrentUser(): User? = authManager.getCurrentUser()
    
    /**
     * Check if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean = authManager.isUserAuthenticated()
}
