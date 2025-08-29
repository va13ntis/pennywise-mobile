package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.presentation.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _registerState = MutableStateFlow<RegisterState>(RegisterState.Initial)
    val registerState: StateFlow<RegisterState> = _registerState
    
    /**
     * Attempt to register with the provided credentials
     */
    fun register(username: String, password: String, confirmPassword: String) {
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
            
            val result = userRepository.registerUser(username, password)
            result.fold(
                onSuccess = { userId ->
                    // After successful registration, get the user and save to auth manager
                    val user = userRepository.getUserById(userId)
                    if (user != null) {
                        authManager.saveAuthenticatedUser(user)
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
     * Sealed class representing the different states of the registration process
     */
    sealed class RegisterState {
        object Initial : RegisterState()
        object Loading : RegisterState()
        data class Success(val userId: Long) : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}
