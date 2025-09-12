package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.BankCard
import com.pennywise.app.domain.repository.BankCardRepository
import com.pennywise.app.presentation.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for bank card management
 */
@HiltViewModel
class BankCardViewModel @Inject constructor(
    private val bankCardRepository: BankCardRepository,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<BankCardUiState>(BankCardUiState.Idle)
    val uiState: StateFlow<BankCardUiState> = _uiState.asStateFlow()
    
    private val _bankCards = MutableStateFlow<List<BankCard>>(emptyList())
    val bankCards: StateFlow<List<BankCard>> = _bankCards.asStateFlow()
    
    private val _activeBankCards = MutableStateFlow<List<BankCard>>(emptyList())
    val activeBankCards: StateFlow<List<BankCard>> = _activeBankCards.asStateFlow()
    
    init {
        loadBankCards()
    }
    
    /**
     * Load all bank cards for the current user
     */
    private fun loadBankCards() {
        viewModelScope.launch {
            try {
                val currentUser = authManager.getCurrentUser()
                if (currentUser != null) {
                    bankCardRepository.getBankCardsByUserId(currentUser.id).collect { cards ->
                        _bankCards.value = cards
                    }
                    
                    bankCardRepository.getActiveBankCardsByUserId(currentUser.id).collect { cards ->
                        _activeBankCards.value = cards
                    }
                }
            } catch (e: Exception) {
                _uiState.value = BankCardUiState.Error("Failed to load bank cards: ${e.message}")
            }
        }
    }
    
    /**
     * Add a new bank card
     */
    fun addBankCard(alias: String, lastFourDigits: String, paymentDay: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = BankCardUiState.Loading
                
                val currentUser = authManager.getCurrentUser()
                if (currentUser == null) {
                    _uiState.value = BankCardUiState.Error("User not authenticated")
                    return@launch
                }
                
                // Validate input
                val validationResult = validateBankCardInput(alias, lastFourDigits, paymentDay)
                if (validationResult != null) {
                    _uiState.value = BankCardUiState.Error(validationResult)
                    return@launch
                }
                
                val bankCard = BankCard(
                    userId = currentUser.id,
                    alias = alias.trim(),
                    lastFourDigits = lastFourDigits.trim(),
                    paymentDay = paymentDay,
                    isActive = true,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                val result = bankCardRepository.insertBankCard(bankCard)
                result.fold(
                    onSuccess = {
                        _uiState.value = BankCardUiState.Success("Bank card added successfully")
                        loadBankCards() // Refresh the list
                    },
                    onFailure = { error ->
                        _uiState.value = BankCardUiState.Error("Failed to add bank card: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BankCardUiState.Error("Failed to add bank card: ${e.message}")
            }
        }
    }
    
    /**
     * Update an existing bank card
     */
    fun updateBankCard(cardId: Long, alias: String, lastFourDigits: String, paymentDay: Int) {
        viewModelScope.launch {
            try {
                _uiState.value = BankCardUiState.Loading
                
                // Validate input
                val validationResult = validateBankCardInput(alias, lastFourDigits, paymentDay)
                if (validationResult != null) {
                    _uiState.value = BankCardUiState.Error(validationResult)
                    return@launch
                }
                
                val existingCard = bankCardRepository.getBankCardById(cardId)
                if (existingCard == null) {
                    _uiState.value = BankCardUiState.Error("Bank card not found")
                    return@launch
                }
                
                val updatedCard = existingCard.copy(
                    alias = alias.trim(),
                    lastFourDigits = lastFourDigits.trim(),
                    paymentDay = paymentDay,
                    updatedAt = Date()
                )
                
                val result = bankCardRepository.updateBankCard(updatedCard)
                result.fold(
                    onSuccess = {
                        _uiState.value = BankCardUiState.Success("Bank card updated successfully")
                        loadBankCards() // Refresh the list
                    },
                    onFailure = { error ->
                        _uiState.value = BankCardUiState.Error("Failed to update bank card: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BankCardUiState.Error("Failed to update bank card: ${e.message}")
            }
        }
    }
    
    /**
     * Delete a bank card
     */
    fun deleteBankCard(bankCard: BankCard) {
        viewModelScope.launch {
            try {
                _uiState.value = BankCardUiState.Loading
                
                val result = bankCardRepository.deleteBankCard(bankCard)
                result.fold(
                    onSuccess = {
                        _uiState.value = BankCardUiState.Success("Bank card deleted successfully")
                        loadBankCards() // Refresh the list
                    },
                    onFailure = { error ->
                        _uiState.value = BankCardUiState.Error("Failed to delete bank card: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BankCardUiState.Error("Failed to delete bank card: ${e.message}")
            }
        }
    }
    
    /**
     * Toggle bank card active status
     */
    fun toggleBankCardStatus(bankCard: BankCard) {
        viewModelScope.launch {
            try {
                _uiState.value = BankCardUiState.Loading
                
                val result = bankCardRepository.updateBankCardStatus(bankCard.id, !bankCard.isActive)
                result.fold(
                    onSuccess = {
                        val status = if (bankCard.isActive) "deactivated" else "activated"
                        _uiState.value = BankCardUiState.Success("Bank card $status successfully")
                        loadBankCards() // Refresh the list
                    },
                    onFailure = { error ->
                        _uiState.value = BankCardUiState.Error("Failed to update bank card status: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = BankCardUiState.Error("Failed to update bank card status: ${e.message}")
            }
        }
    }
    
    /**
     * Clear the current UI state
     */
    fun clearUiState() {
        _uiState.value = BankCardUiState.Idle
    }
    
    /**
     * Validate bank card input
     */
    private fun validateBankCardInput(alias: String, lastFourDigits: String, paymentDay: Int): String? {
        if (alias.isBlank()) {
            return "Card alias cannot be empty"
        }
        
        if (alias.length < 2) {
            return "Card alias must be at least 2 characters long"
        }
        
        if (alias.length > 50) {
            return "Card alias must be less than 50 characters"
        }
        
        if (lastFourDigits.isBlank()) {
            return "Last four digits cannot be empty"
        }
        
        if (!lastFourDigits.matches(Regex("\\d{4}"))) {
            return "Last four digits must be exactly 4 digits"
        }
        
        if (paymentDay < 1 || paymentDay > 31) {
            return "Payment day must be between 1 and 31"
        }
        
        return null
    }
}

/**
 * UI state for bank card management
 */
sealed class BankCardUiState {
    object Idle : BankCardUiState()
    object Loading : BankCardUiState()
    data class Success(val message: String) : BankCardUiState()
    data class Error(val message: String) : BankCardUiState()
}
