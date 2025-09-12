package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.BankCard
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.repository.BankCardRepository
import com.pennywise.app.domain.usecase.CurrencySortingService
import com.pennywise.app.domain.validation.CurrencyValidator
import com.pennywise.app.domain.validation.CurrencyErrorHandler
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.screens.ExpenseFormData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

/**
 * ViewModel for the Add Expense screen
 */
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val bankCardRepository: BankCardRepository,
    private val authManager: AuthManager,
    private val currencyValidator: CurrencyValidator,
    private val currencyErrorHandler: CurrencyErrorHandler,
    private val currencySortingService: CurrencySortingService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AddExpenseUiState>(AddExpenseUiState.Idle)
    val uiState: StateFlow<AddExpenseUiState> = _uiState
    
    private val _selectedCurrency = MutableStateFlow<Currency?>(null)
    val selectedCurrency: StateFlow<Currency?> = _selectedCurrency
    
    // Sorted currencies for selection
    private val _sortedCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val sortedCurrencies: StateFlow<List<Currency>> = _sortedCurrencies
    
    // Top currencies (most used)
    private val _topCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val topCurrencies: StateFlow<List<Currency>> = _topCurrencies
    
    // Bank cards for the current user
    private val _bankCards = MutableStateFlow<List<BankCard>>(emptyList())
    val bankCards: StateFlow<List<BankCard>> = _bankCards
    
    init {
        // Initialize with current user's default currency and load sorted currencies
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                user?.let { currentUser ->
                    // Use currency validator to ensure we have a valid currency
                    val defaultCurrency = currencyValidator.getValidCurrencyOrFallback(currentUser.defaultCurrency)
                    _selectedCurrency.value = defaultCurrency
                    
                    // Log if fallback was used
                    if (defaultCurrency.code != currentUser.defaultCurrency) {
                        currencyErrorHandler.handleCurrencyFallback(
                            currentUser.defaultCurrency,
                            defaultCurrency.code,
                            "AddExpenseViewModel initialization"
                        )
                    }
                    
                    // Load sorted currencies for this user
                    loadSortedCurrencies(currentUser.id)
                    
                    // Load bank cards for this user
                    loadBankCards(currentUser.id)
                }
            }
        }
    }
    
    /**
     * Load sorted currencies for a user
     */
    private fun loadSortedCurrencies(userId: Long) {
        viewModelScope.launch {
            currencySortingService.getSortedCurrencies(userId).collect { sortedCurrencies ->
                _sortedCurrencies.value = sortedCurrencies
            }
        }
        
        viewModelScope.launch {
            currencySortingService.getTopCurrencies(userId, 10).collect { topCurrencies ->
                _topCurrencies.value = topCurrencies
            }
        }
    }
    
    /**
     * Load bank cards for a user
     */
    private fun loadBankCards(userId: Long) {
        viewModelScope.launch {
            try {
                bankCardRepository.getActiveBankCardsByUserId(userId).collect { cards ->
                    _bankCards.value = cards
                }
            } catch (e: Exception) {
                // Handle error - could log or show user feedback
                // For now, just set empty list
                _bankCards.value = emptyList()
            }
        }
    }
    
    /**
     * Update the selected currency with validation and track usage
     */
    fun updateSelectedCurrency(currency: Currency) {
        // Validate the currency before updating
        val validationResult = currencyValidator.validateCurrencyCode(currency.code)
        if (validationResult is com.pennywise.app.domain.validation.ValidationResult.Success) {
            _selectedCurrency.value = currency
            
            // Track currency usage for sorting
            viewModelScope.launch {
                val currentUser = authManager.getCurrentUser()
                if (currentUser != null) {
                    currencySortingService.trackCurrencyUsage(currentUser.id, currency.code)
                }
            }
        } else {
            // Log the validation error
            currencyErrorHandler.handleCurrencyValidationError(
                com.pennywise.app.domain.validation.CurrencyErrorType.UNSUPPORTED_CODE,
                currency.code,
                "AddExpenseViewModel.updateSelectedCurrency"
            )
            // Still update with the currency but log the issue
            _selectedCurrency.value = currency
            
            // Track currency usage even if validation failed
            viewModelScope.launch {
                val currentUser = authManager.getCurrentUser()
                if (currentUser != null) {
                    currencySortingService.trackCurrencyUsage(currentUser.id, currency.code)
                }
            }
        }
    }
    
    /**
     * Save expense to the repository with currency validation
     */
    fun saveExpense(expenseData: ExpenseFormData, userId: Long) {
        viewModelScope.launch {
            _uiState.value = AddExpenseUiState.Loading
            
            try {
                val currency = _selectedCurrency.value ?: Currency.getDefault()
                
                // Validate currency and amount
                val currencyValidation = currencyValidator.validateCurrencyCode(currency.code)
                val amountValidation = currencyValidator.validateAmountForCurrency(expenseData.amount, currency)
                
                when {
                    currencyValidation is com.pennywise.app.domain.validation.ValidationResult.Error -> {
                        currencyErrorHandler.handleCurrencyValidationError(
                            com.pennywise.app.domain.validation.CurrencyErrorType.UNSUPPORTED_CODE,
                            currency.code,
                            "AddExpenseViewModel.saveExpense"
                        )
                        _uiState.value = AddExpenseUiState.Error("Invalid currency: ${currencyValidation.message}")
                        return@launch
                    }
                    amountValidation is com.pennywise.app.domain.validation.ValidationResult.Error -> {
                        currencyErrorHandler.handleCurrencyValidationError(
                            com.pennywise.app.domain.validation.CurrencyErrorType.INVALID_AMOUNT,
                            null,
                            "AddExpenseViewModel.saveExpense"
                        )
                        _uiState.value = AddExpenseUiState.Error("Invalid amount: ${amountValidation.message}")
                        return@launch
                    }
                }
                
                val transaction = Transaction(
                    userId = userId,
                    amount = expenseData.amount,
                    currency = currency.code,
                    description = expenseData.merchant,
                    category = expenseData.category,
                    type = TransactionType.EXPENSE,
                    date = expenseData.date,
                    isRecurring = expenseData.isRecurring,
                    recurringPeriod = if (expenseData.isRecurring) RecurringPeriod.MONTHLY else null,
                    paymentMethod = expenseData.paymentMethod,
                    installments = expenseData.installments,
                    installmentAmount = expenseData.installmentAmount,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                val transactionId = transactionRepository.insertTransaction(transaction)
                _uiState.value = AddExpenseUiState.Success(transactionId)
            } catch (e: Exception) {
                _uiState.value = AddExpenseUiState.Error(e.message ?: "Failed to save expense")
            }
        }
    }
    
    /**
     * Calculate installment amount based on total amount and number of installments
     */
    fun calculateInstallmentAmount(totalAmount: Double, installments: Int): Double {
        return if (installments > 0) {
            totalAmount / installments
        } else {
            totalAmount
        }
    }
    
    /**
     * Reset UI state to idle
     */
    fun resetState() {
        _uiState.value = AddExpenseUiState.Idle
    }
}

/**
 * UI state for the Add Expense screen
 */
sealed class AddExpenseUiState {
    object Idle : AddExpenseUiState()
    object Loading : AddExpenseUiState()
    data class Success(val transactionId: Long) : AddExpenseUiState()
    data class Error(val message: String) : AddExpenseUiState()
}
