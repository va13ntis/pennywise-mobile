package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.BankCard
import com.pennywise.app.domain.model.SplitPaymentInstallment
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.repository.BankCardRepository
import com.pennywise.app.domain.repository.SplitPaymentInstallmentRepository
import com.pennywise.app.domain.usecase.CurrencySortingService
import com.pennywise.app.domain.validation.CurrencyValidator
import com.pennywise.app.domain.validation.CurrencyErrorHandler
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.screens.ExpenseFormData
import com.pennywise.app.presentation.util.SoundManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for the Add Expense screen
 */
@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val bankCardRepository: BankCardRepository,
    private val splitPaymentInstallmentRepository: SplitPaymentInstallmentRepository,
    private val paymentMethodConfigRepository: com.pennywise.app.domain.repository.PaymentMethodConfigRepository,
    private val authManager: AuthManager,
    private val currencyValidator: CurrencyValidator,
    private val currencyErrorHandler: CurrencyErrorHandler,
    private val currencySortingService: CurrencySortingService,
    private val soundManager: SoundManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AddExpenseUiState>(AddExpenseUiState.Idle)
    val uiState: StateFlow<AddExpenseUiState> = _uiState
    
    private val _needsAuthentication = MutableStateFlow(false)
    val needsAuthentication: StateFlow<Boolean> = _needsAuthentication
    
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
    
    // Payment method configurations (credit cards)
    private val _paymentMethodConfigs = MutableStateFlow<List<com.pennywise.app.domain.model.PaymentMethodConfig>>(emptyList())
    val paymentMethodConfigs: StateFlow<List<com.pennywise.app.domain.model.PaymentMethodConfig>> = _paymentMethodConfigs
    
    // Default payment method from user settings
    private val _defaultPaymentMethod = MutableStateFlow<com.pennywise.app.domain.model.PaymentMethod>(com.pennywise.app.domain.model.PaymentMethod.CASH)
    val defaultPaymentMethod: StateFlow<com.pennywise.app.domain.model.PaymentMethod> = _defaultPaymentMethod
    
    // Merchant suggestions
    private val _merchantSuggestions = MutableStateFlow<List<String>>(emptyList())
    val merchantSuggestions: StateFlow<List<String>> = _merchantSuggestions
    
    private val _topMerchants = MutableStateFlow<List<String>>(emptyList())
    val topMerchants: StateFlow<List<String>> = _topMerchants
    
    // Current user
    val currentUser = authManager.currentUser
    
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
                    
                    // Load sorted currencies
                    loadSortedCurrencies()
                    
                    // Load bank cards
                    loadBankCards()
                    
                    // Load payment method configurations
                    loadPaymentMethodConfigs()
                    
                    // Load default payment method
                    loadDefaultPaymentMethod()
                }
            }
        }
    }
    
    /**
     * Load sorted currencies
     */
    private fun loadSortedCurrencies() {
        viewModelScope.launch {
            currencySortingService.getSortedCurrencies().collect { sortedCurrencies ->
                _sortedCurrencies.value = sortedCurrencies
            }
        }
        
        viewModelScope.launch {
            currencySortingService.getTopCurrencies(10).collect { topCurrencies ->
                _topCurrencies.value = topCurrencies
            }
        }
    }
    
    /**
     * Load bank cards
     */
    private fun loadBankCards() {
        viewModelScope.launch {
            try {
                bankCardRepository.getActiveBankCards().collect { cards ->
                    _bankCards.value = cards
                    _needsAuthentication.value = false
                }
            } catch (e: SecurityException) {
                _needsAuthentication.value = true
                _bankCards.value = emptyList()
            } catch (e: Exception) {
                // Handle error - could log or show user feedback
                // For now, just set empty list
                _bankCards.value = emptyList()
                _needsAuthentication.value = false
            }
        }
    }
    
    /**
     * Load payment method configurations (credit cards)
     */
    private fun loadPaymentMethodConfigs() {
        viewModelScope.launch {
            try {
                authManager.currentUser.collect { user ->
                    if (user != null) {
                        paymentMethodConfigRepository.getPaymentMethodConfigs().collect { configs ->
                            _paymentMethodConfigs.value = configs
                            _needsAuthentication.value = false
                        }
                    }
                }
            } catch (e: SecurityException) {
                _needsAuthentication.value = true
                _paymentMethodConfigs.value = emptyList()
            } catch (e: Exception) {
                _paymentMethodConfigs.value = emptyList()
                _needsAuthentication.value = false
            }
        }
    }
    
    /**
     * Load default payment method from user settings
     */
    private fun loadDefaultPaymentMethod() {
        viewModelScope.launch {
            try {
                authManager.currentUser.collect { user ->
                    if (user != null) {
                        _defaultPaymentMethod.value = user.defaultPaymentMethod
                        _needsAuthentication.value = false
                    }
                }
            } catch (e: Exception) {
                // Handle error - keep CASH as fallback
                _defaultPaymentMethod.value = com.pennywise.app.domain.model.PaymentMethod.CASH
            }
        }
    }
    
    /**
     * Load merchant suggestions based on category
     */
    fun loadMerchantSuggestions(category: String) {
        viewModelScope.launch {
            try {
                val merchants = transactionRepository.getFrequentMerchantsByCategory(
                    category = category,
                    limit = 20
                )
                _merchantSuggestions.value = merchants
                _topMerchants.value = merchants.take(5) // Top 5 for chips
            } catch (e: Exception) {
                // Silently fail - suggestions are optional
                _merchantSuggestions.value = emptyList()
                _topMerchants.value = emptyList()
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
                currencySortingService.trackCurrencyUsage(currency.code)
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
                currencySortingService.trackCurrencyUsage(currency.code)
            }
        }
    }
    
    /**
     * Save expense to the repository with currency validation
     */
    fun saveExpense(expenseData: ExpenseFormData) {
        viewModelScope.launch {
            _uiState.value = AddExpenseUiState.Loading
            
            try {
                android.util.Log.d("AddExpenseViewModel", "Saving expense")
                android.util.Log.d("AddExpenseViewModel", "Expense data: $expenseData")
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
                
                // For split payments, the main transaction should only record the first installment amount
                val isSplitPayment = expenseData.installments != null && expenseData.installments > 1
                val mainTransactionAmount = if (isSplitPayment) {
                    expenseData.installmentAmount ?: calculateInstallmentAmount(expenseData.amount, expenseData.installments!!)
                } else {
                    expenseData.amount
                }
                
                val transaction = Transaction(
                    amount = mainTransactionAmount,
                    currency = currency.code,
                    description = expenseData.merchant,
                    category = expenseData.category,
                    type = TransactionType.EXPENSE,
                    date = expenseData.date,
                    isRecurring = expenseData.isRecurring,
                    recurringPeriod = expenseData.recurringPeriod,
                    paymentMethod = expenseData.paymentMethod,
                    paymentMethodConfigId = expenseData.selectedPaymentMethodConfigId,
                    installments = expenseData.installments,
                    installmentAmount = expenseData.installmentAmount,
                    billingDelayDays = expenseData.billingDelayDays,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                val transactionId = transactionRepository.insertTransaction(transaction)
                
                // If this is a split payment, create installments for future months
                if (isSplitPayment) {
                    createSplitPaymentInstallments(
                        parentTransactionId = transactionId,
                        totalAmount = expenseData.amount,
                        currency = currency.code,
                        description = expenseData.merchant,
                        category = expenseData.category,
                        startDate = expenseData.date,
                        installments = expenseData.installments!!
                    )
                }
                
                _uiState.value = AddExpenseUiState.Success(transactionId)
                _needsAuthentication.value = false
                
                // Play kaching sound to celebrate the successful expense addition
                soundManager.playKachingSound()
            } catch (e: SecurityException) {
                _uiState.value = AddExpenseUiState.Error("Authentication required")
                _needsAuthentication.value = true
            } catch (e: Exception) {
                _uiState.value = AddExpenseUiState.Error(e.message ?: "Failed to save expense")
                _needsAuthentication.value = false
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
     * Get billing cycle date based on selected credit card's withdraw day
     * Returns the next billing date for the selected credit card
     */
    fun getBillingCycleDate(selectedConfigId: Long?): java.time.LocalDate {
        val selectedConfig = _paymentMethodConfigs.value.find { it.id == selectedConfigId }
        val withdrawDay = selectedConfig?.withdrawDay ?: java.time.LocalDate.now().dayOfMonth
        val today = java.time.LocalDate.now()
        
        // Calculate next billing cycle date based on withdrawDay
        var billingDate = try {
            java.time.LocalDate.of(today.year, today.month, withdrawDay)
        } catch (e: java.time.DateTimeException) {
            // Handle invalid day for month (e.g., Feb 31)
            today.withDayOfMonth(today.lengthOfMonth().coerceAtMost(withdrawDay))
        }
        
        // If billing date is today or in the past, move to next month
        if (billingDate.isBefore(today) || billingDate.isEqual(today)) {
            billingDate = billingDate.plusMonths(1)
            // Handle month overflow (e.g., Jan 31 -> Feb)
            billingDate = try {
                billingDate.withDayOfMonth(withdrawDay)
            } catch (e: java.time.DateTimeException) {
                billingDate.withDayOfMonth(billingDate.lengthOfMonth().coerceAtMost(withdrawDay))
            }
        }
        
        return billingDate
    }
    
    /**
     * Create split payment installments for all months
     */
    private suspend fun createSplitPaymentInstallments(
        parentTransactionId: Long,
        totalAmount: Double,
        currency: String,
        description: String,
        category: String,
        startDate: Date,
        installments: Int
    ) {
        val installmentAmount = totalAmount / installments
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        
        val installmentsList = mutableListOf<SplitPaymentInstallment>()
        
        // Create installments for all months
        for (i in 1..installments) {
            val installment = SplitPaymentInstallment(
                parentTransactionId = parentTransactionId,
                amount = installmentAmount,
                currency = currency,
                description = description,
                category = category,
                type = TransactionType.EXPENSE,
                dueDate = calendar.time,
                installmentNumber = i,
                totalInstallments = installments,
                isPaid = (i == 1), // Mark the first installment as paid since it's recorded in the main transaction
                paidDate = if (i == 1) startDate else null,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            installmentsList.add(installment)
            
            // Move to the next month for the next installment
            if (i < installments) {
                calendar.add(Calendar.MONTH, 1)
            }
        }
        
        // Insert all installments at once
        if (installmentsList.isNotEmpty()) {
            splitPaymentInstallmentRepository.insertInstallments(installmentsList)
        }
    }
    
    /**
     * Reset UI state to idle
     */
    fun resetState() {
        _uiState.value = AddExpenseUiState.Idle
        _needsAuthentication.value = false
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
