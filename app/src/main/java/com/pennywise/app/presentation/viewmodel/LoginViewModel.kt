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
 * ViewModel for handling login functionality
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Initial)
    val loginState: StateFlow<LoginState> = _loginState
    
    /**
     * Attempt to login with the provided credentials
     */
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            if (username.isBlank() || password.isBlank()) {
                _loginState.value = LoginState.Error("Username and password cannot be empty")
                return@launch
            }
            
            val result = userRepository.authenticateUser(username, password)
            result.fold(
                onSuccess = { user ->
                    authManager.saveAuthenticatedUser(user)
                    _loginState.value = LoginState.Success(user)
                },
                onFailure = { error ->
                    _loginState.value = LoginState.Error(error.message ?: "Authentication failed")
                }
            )
        }
    }
    
    /**
     * Reset the login state to initial
     */
    fun resetState() {
        _loginState.value = LoginState.Initial
    }
    
    /**
     * Sealed class representing the different states of the login process
     */
    /**
     * Login using biometric authentication
     */
    fun loginWithBiometric() {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            // The user is already authenticated via biometric, just retrieve the stored user
            val user = authManager.getCurrentUser()
            if (user != null) {
                _loginState.value = LoginState.Success(user)
            } else {
                _loginState.value = LoginState.Error("Biometric authentication failed")
            }
        }
    }
    
    sealed class LoginState {
        object Initial : LoginState()
        object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
    }
}
