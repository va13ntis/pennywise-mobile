package com.pennywise.app.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.data.util.SettingsDataStore
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for handling registration functionality
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authManager: AuthManager,
    private val localeManager: LocaleManager,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Initial)
    val registerState: StateFlow<RegisterState> = _registerState
    
    /**
     * Attempt to register with the provided credentials, default currency, and locale
     */
    fun register(username: String, password: String, confirmPassword: String, defaultCurrency: String = "USD", locale: String? = null) {
        viewModelScope.launch {
            _registerState.value = RegisterState.Loading
            
            if (username.isBlank() || password.isBlank()) {
                _registerState.value = RegisterState.Error("Username and password cannot be empty")
                return@launch
            }
            
            if (password != confirmPassword) {
                _registerState.value = RegisterState.Error("Passwords do not match")
                return@launch
            }
            
            // Use provided locale or detect from device
            val detectedLocale = locale ?: localeManager.detectDeviceLocale(context)
            val result = userRepository.registerUser(username, password, defaultCurrency, detectedLocale)
            result.fold(
                onSuccess = { userId ->
                    // After successful registration, get the user and save to auth manager
                    val user = userRepository.getUserById(userId)
                    if (user != null) {
                        // Save the authenticated user FIRST to ensure it's persisted
                        authManager.saveAuthenticatedUser(user)
                        
                        // Save the selected locale to SettingsDataStore
                        // This will trigger the activity restart, but the user is already authenticated
                        settingsDataStore.setLanguage(detectedLocale)
                    }
                    _registerState.value = RegisterState.Success(userId)
                },
                onFailure = { error ->
                    _registerState.value = RegisterState.Error(error.message ?: "Registration failed")
                }
            )
        }
    }
    
    /**
     * Reset the registration state to initial
     */
    fun resetState() {
        _registerState.value = RegisterState.Initial
    }
    
    /**
     * Get the detected device locale
     */
    fun getDetectedLocale(): String {
        return localeManager.detectDeviceLocale(context)
    }
    
    /**
     * Get all supported locales
     */
    fun getSupportedLocales(): Map<String, String> {
        return localeManager.getSupportedLocales()
    }
    
    /**
     * Sealed class representing the different states of the registration process
     */
    sealed class RegisterState {
        object Initial : RegisterState()
        object Loading : RegisterState()
        data class Success(val userId: Long) : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}
