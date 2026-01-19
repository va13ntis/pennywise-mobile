package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.User
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.auth.DeviceAuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing authentication state across the app
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val deviceAuthService: DeviceAuthService
) : ViewModel() {
    
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized
    val currentUser: StateFlow<User?> = authManager.currentUser
    val isDeviceAuthEnabled: Flow<Boolean> = deviceAuthService.isDeviceAuthEnabled
    
    // Computed property that considers both user existence and device authentication state
    val shouldRequireDeviceAuth: StateFlow<Boolean> = combine(
        authManager.currentUser,
        deviceAuthService.isDeviceAuthEnabled,
        _isAuthenticated
    ) { user, deviceAuthEnabled, isAuthenticated ->
        // Require device auth if:
        // 1. User exists AND
        // 2. Device auth is enabled AND  
        // 3. User is not yet authenticated
        val result = user != null && deviceAuthEnabled && !isAuthenticated
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    
    init {
        // Don't automatically sync with AuthManager.isAuthenticated
        // Instead, we'll manage _isAuthenticated manually based on device auth completion
        
        // Observe device authentication state
        viewModelScope.launch {
            isDeviceAuthEnabled.collect { deviceAuthEnabled ->
            }
        }
        
        // Debug: Log current user and device auth state changes
        viewModelScope.launch {
            combine(
                authManager.currentUser,
                deviceAuthService.isDeviceAuthEnabled,
                _isAuthenticated
            ) { user, deviceAuthEnabled, isAuthenticated ->
            }.collect { }
        }
    }
    
    /**
     * Initialize authentication state when the app starts
     */
    fun initializeAuthState() {
        viewModelScope.launch {
            try {
                authManager.initializeAuthState()
                
                // Check if device authentication is required
                val deviceAuthEnabled = deviceAuthService.isDeviceAuthEnabled.first()
                val user = authManager.currentUser.value
                
                if (user != null && !deviceAuthEnabled) {
                    // User exists but device auth is disabled, mark as authenticated
                    _isAuthenticated.value = true
                } else if (user != null && deviceAuthEnabled) {
                    // User exists and device auth is enabled, require device auth
                    _isAuthenticated.value = false
                } else {
                    // No user, keep _isAuthenticated as false
                    _isAuthenticated.value = false
                }
                
            } catch (e: Exception) {
                _isAuthenticated.value = false
            } finally {
                // Signal to UI that initialization work has completed
                _isInitialized.value = true
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
     * Check if user is currently authenticated
     */
    fun isUserAuthenticated(): Boolean = authManager.isUserAuthenticated()
    
    /**
     * Mark user as authenticated after successful device authentication
     */
    fun markUserAsAuthenticated() {
        // Set authentication state immediately (synchronous)
        _isAuthenticated.value = true
        
        // Save to persistent storage (async)
        viewModelScope.launch {
            val user = authManager.currentUser.value
            if (user != null) {
                authManager.saveAuthenticatedUser(user)
            }
        }
    }

}
