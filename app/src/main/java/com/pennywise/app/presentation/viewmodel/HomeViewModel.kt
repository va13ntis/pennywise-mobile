package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.data.util.SettingsDataStore
import com.pennywise.app.data.service.CurrencyConversionService
import com.pennywise.app.presentation.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * ViewModel for the Home screen that manages monthly expense data
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val settingsDataStore: SettingsDataStore,
    private val currencyConversionService: CurrencyConversionService,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _userId = MutableStateFlow<Long?>(null)
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    private val _recurringTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    val recurringTransactions: StateFlow<List<Transaction>> = _recurringTransactions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()
    
    /**
     * Flow of the current currency preference - uses user's default currency
     */
    val currency: StateFlow<String> = authManager.currentUser.map { user ->
        user?.defaultCurrency ?: "USD"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "USD"
    )
    
    /**
     * Flow of the currency conversion enabled state
     */
    val currencyConversionEnabled: StateFlow<Boolean> = settingsDataStore.currencyConversionEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )
    
    /**
     * Flow of the original currency preference
     */
    val originalCurrency: StateFlow<String> = settingsDataStore.originalCurrency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )
    
    /**
     * State for currency conversion operations
     */
    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState.asStateFlow()
    
    /**
     * Sealed class representing different conversion states
     */
    sealed class ConversionState {
        object Idle : ConversionState()
        object Loading : ConversionState()
        data class Success(
            val originalAmount: Double,
            val convertedAmount: Double,
            val originalCurrency: String,
            val targetCurrency: String,
            val conversionRate: Double,
            val isUsingCachedRate: Boolean = false
        ) : ConversionState()
        data class Error(val message: String) : ConversionState()
    }
    
    /**
     * Set the current user ID and load their data
     */
    fun setUserId(userId: Long) {
        println("🔄 HomeViewModel: Setting user ID to $userId")
        _userId.value = userId
        loadTransactions()
    }
    
    /**
     * Navigate to a different month (positive for next, negative for previous)
     */
    fun changeMonth(offset: Int) {
        _currentMonth.value = _currentMonth.value.plusMonths(offset.toLong())
        loadTransactions()
    }
    
    /**
     * Load transactions for the current month and user
     */
    private fun loadTransactions() {
        val userId = _userId.value ?: return
        
        println("🔄 HomeViewModel: Loading transactions for user $userId")
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // Use async to run both operations concurrently
                val monthlyDeferred = async {
                    println("🔄 HomeViewModel: Loading monthly transactions...")
                    val transactions = transactionRepository.getTransactionsByMonth(userId, _currentMonth.value).first()
                    println("✅ HomeViewModel: Loaded ${transactions.size} monthly transactions")
                    transactions
                }
                
                val recurringDeferred = async {
                    println("🔄 HomeViewModel: Loading recurring transactions...")
                    val transactions = transactionRepository.getRecurringTransactionsByUser(userId).first()
                    println("✅ HomeViewModel: Loaded ${transactions.size} recurring transactions")
                    transactions
                }
                
                // Wait for both operations to complete
                val monthlyTransactions = monthlyDeferred.await()
                val recurringTransactions = recurringDeferred.await()
                
                // Update the state
                _transactions.value = monthlyTransactions
                _recurringTransactions.value = recurringTransactions
                
                println("✅ HomeViewModel: All transactions loading completed")
            } catch (e: Exception) {
                println("❌ HomeViewModel: Failed to load transactions: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load transactions: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get transactions grouped by week number
     */
    fun getTransactionsGroupedByWeek(): Map<Int, List<Transaction>> {
        return _transactions.value
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { transaction ->
                val calendar = Calendar.getInstance().apply {
                    time = transaction.date
                }
                calendar.get(Calendar.WEEK_OF_MONTH)
            }
            .toSortedMap()
    }
    
    /**
     * Get total expenses for the current month
     */
    fun getTotalExpenses(): Double {
        return _transactions.value
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
    }
    
    /**
     * Get total income for the current month
     */
    fun getTotalIncome(): Double {
        return _transactions.value
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }
    
    /**
     * Get net balance (income - expenses) for the current month
     */
    fun getNetBalance(): Double {
        return getTotalIncome() - getTotalExpenses()
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Convert an amount from original currency to target currency
     */
    fun convertAmount(amount: Double) {
        val enabled = currencyConversionEnabled.value
        val original = originalCurrency.value
        val target = currency.value
        
        if (!enabled || original.isEmpty() || target.isEmpty() || original == target) {
            _conversionState.value = ConversionState.Idle
            return
        }
        
        viewModelScope.launch {
            try {
                _conversionState.value = ConversionState.Loading
                
                val convertedAmount = currencyConversionService.convertCurrency(amount, original, target)
                
                if (convertedAmount != null) {
                    val conversionRate = convertedAmount / amount
                    _conversionState.value = ConversionState.Success(
                        originalAmount = amount,
                        convertedAmount = convertedAmount,
                        originalCurrency = original,
                        targetCurrency = target,
                        conversionRate = conversionRate
                    )
                } else {
                    _conversionState.value = ConversionState.Error("Conversion failed")
                }
            } catch (e: Exception) {
                _conversionState.value = ConversionState.Error("Conversion error: ${e.message}")
            }
        }
    }
    
    /**
     * Clear conversion state
     */
    fun clearConversionState() {
        _conversionState.value = ConversionState.Idle
    }
}
