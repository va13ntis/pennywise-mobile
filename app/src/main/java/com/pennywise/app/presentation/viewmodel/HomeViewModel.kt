package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.SplitPaymentInstallment
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.repository.SplitPaymentInstallmentRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
    private val splitPaymentInstallmentRepository: SplitPaymentInstallmentRepository,
    private val settingsDataStore: SettingsDataStore,
    private val currencyConversionService: CurrencyConversionService,
    private val authManager: AuthManager
) : ViewModel() {
    
    // Remove _userId - we'll use authManager.currentUser directly
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedPaymentMethod = MutableStateFlow<com.pennywise.app.domain.model.PaymentMethod?>(null)
    
    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()
    
    private val _allRecurringTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val _splitPaymentInstallments = MutableStateFlow<List<SplitPaymentInstallment>>(emptyList())
    
    // Computed recurring transactions filtered by current month
    val recurringTransactions: StateFlow<List<Transaction>> = combine(
        _allRecurringTransactions.asStateFlow(),
        _currentMonth.asStateFlow()
    ) { allRecurring, currentMonth ->
        filterRecurringTransactionsForMonth(allRecurring, currentMonth)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    
    // Computed split payment installments filtered by current month
    val splitPaymentInstallments: StateFlow<List<SplitPaymentInstallment>> = combine(
        _splitPaymentInstallments.asStateFlow(),
        _currentMonth.asStateFlow()
    ) { allInstallments, currentMonth ->
        filterSplitPaymentInstallmentsForMonth(allInstallments, currentMonth)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _needsAuthentication = MutableStateFlow(false)
    val needsAuthentication: StateFlow<Boolean> = _needsAuthentication.asStateFlow()
    
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()
    
    // Reactive computed values
    val transactionsByWeek: StateFlow<Map<Int, List<Transaction>>> = _transactions.map { transactions ->
        transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isRecurring }  // Exclude recurring expenses
            .sortedByDescending { it.date }
            .groupBy { transaction ->
                val calendar = Calendar.getInstance().apply {
                    time = transaction.date
                }
                calendar.get(Calendar.WEEK_OF_MONTH)
            }
            .toSortedMap()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyMap()
    )
    
    val totalIncome: StateFlow<Double> = _transactions.map { transactions ->
        transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )
    
    val totalExpenses: StateFlow<Double> = combine(
        _transactions.asStateFlow(),
        recurringTransactions,
        _selectedPaymentMethod.asStateFlow()
    ) { transactions, recurringTransactions, selectedPaymentMethod ->
        // Only include NON-recurring expenses from monthly transactions
        val regularExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isRecurring }
            .filter { transaction ->
                selectedPaymentMethod?.let { method ->
                    transaction.paymentMethod == method
                } ?: true // Show all if no filter selected
            }
            .sumOf { it.amount }
        
        val recurringExpenses = recurringTransactions
            .filter { transaction ->
                selectedPaymentMethod?.let { method ->
                    transaction.paymentMethod == method
                } ?: true // Show all if no filter selected
            }
            .sumOf { it.amount }
        
        regularExpenses + recurringExpenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )
    
    val netBalance: StateFlow<Double> = combine(
        _transactions.asStateFlow(),
        recurringTransactions
    ) { transactions, recurringTransactions ->
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        // Only include NON-recurring expenses from monthly transactions
        val regularExpenses = transactions.filter { it.type == TransactionType.EXPENSE && !it.isRecurring }.sumOf { it.amount }
        val recurringExpenses = recurringTransactions.sumOf { it.amount }
        income - (regularExpenses + recurringExpenses)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )
    
    // Job management for proper coroutine lifecycle
    private var monthlyTransactionsJob: Job? = null
    private var recurringTransactionsJob: Job? = null
    
    init {
        // Start observing transactions immediately when ViewModel is created
        startObservingTransactions()
    }
    
    /**
     * Flow of the current currency preference - uses user's default currency
     */
    val currency: StateFlow<String> = authManager.currentUser.map { user ->
        user?.defaultCurrency ?: "USD"
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "USD"
    )
    
    /**
     * Flow of the currency conversion enabled state
     */
    val currencyConversionEnabled: StateFlow<Boolean> = settingsDataStore.currencyConversionEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false
    )
    
    /**
     * Flow of the original currency preference
     */
    val originalCurrency: StateFlow<String> = settingsDataStore.originalCurrency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
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
     * Filter recurring transactions to show only those relevant for the current month
     * Based on the recurrence pattern (DAILY, WEEKLY, MONTHLY, YEARLY)
     */
    private fun filterRecurringTransactionsForMonth(
        allRecurring: List<Transaction>,
        currentMonth: YearMonth
    ): List<Transaction> {
        if (allRecurring.isEmpty()) return emptyList()
        
        println("🔍 HomeViewModel: Filtering recurring transactions for month: $currentMonth")
        println("🔍 HomeViewModel: Total recurring transactions to filter: ${allRecurring.size}")
        
        val filtered = allRecurring.filter { transaction ->
            val transactionDate = transaction.date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
            val transactionYearMonth = YearMonth.from(transactionDate)
            
            // Only show recurring transactions that started on or before the current month
            if (transactionYearMonth.isAfter(currentMonth)) {
                return@filter false
            }
            
            // Check if this recurring transaction should appear in the current month
            val shouldAppear = when (transaction.recurringPeriod) {
                com.pennywise.app.domain.model.RecurringPeriod.DAILY -> {
                    // Daily recurring: appears every month after start date
                    true
                }
                com.pennywise.app.domain.model.RecurringPeriod.WEEKLY -> {
                    // Weekly recurring: appears every month after start date
                    true
                }
                com.pennywise.app.domain.model.RecurringPeriod.MONTHLY -> {
                    // Monthly recurring: appears every month after start date
                    true
                }
                com.pennywise.app.domain.model.RecurringPeriod.YEARLY -> {
                    // Yearly recurring: appears only in the same month each year
                    transactionDate.month == currentMonth.month
                }
                null -> {
                    // Shouldn't happen for recurring transactions, but fall back to date check
                    transactionYearMonth == currentMonth
                }
            }
            
            if (shouldAppear) {
                println("✅ HomeViewModel: Including recurring transaction: ${transaction.description} (${transaction.recurringPeriod}) - started: ${transactionDate}")
            }
            
            shouldAppear
        }
        
        println("🔍 HomeViewModel: Filtered to ${filtered.size} recurring transactions for $currentMonth")
        return filtered
    }
    
    /**
     * Filter split payment installments for the current month
     */
    private fun filterSplitPaymentInstallmentsForMonth(
        allInstallments: List<SplitPaymentInstallment>,
        currentMonth: YearMonth
    ): List<SplitPaymentInstallment> {
        if (allInstallments.isEmpty()) return emptyList()
        
        val startOfMonth = currentMonth.atDay(1).atStartOfDay()
        val endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59)
        
        println("🔍 HomeViewModel: Filtering split payment installments for month: $currentMonth")
        println("🔍 HomeViewModel: Date range: $startOfMonth to $endOfMonth")
        println("🔍 HomeViewModel: Total installments to filter: ${allInstallments.size}")
        
        val filtered = allInstallments.filter { installment ->
            val dueDate = installment.dueDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            
            // Only include if the installment due date falls within the current month
            val isInCurrentMonth = !dueDate.isBefore(startOfMonth) && !dueDate.isAfter(endOfMonth)
            
            // Exclude the first installment (installmentNumber == 1) since it's already shown in the week section
            val isNotFirstInstallment = installment.installmentNumber > 1
            
            val shouldInclude = isInCurrentMonth && isNotFirstInstallment
            
            if (shouldInclude) {
                println("✅ HomeViewModel: Including split payment installment: ${installment.description} - ${dueDate.toLocalDate()}")
            }
            
            shouldInclude
        }
        
        println("🔍 HomeViewModel: Filtered to ${filtered.size} split payment installments for $currentMonth")
        return filtered
    }

    /**
     * Navigate to a different month (positive for next, negative for previous)
     */
    fun changeMonth(offset: Int) {
        _currentMonth.value = _currentMonth.value.plusMonths(offset.toLong())
    }
    
    /**
     * Navigate to the current month
     */
    fun navigateToCurrentMonth() {
        _currentMonth.value = YearMonth.now()
    }
    
    /**
     * Navigate to the beginning of the current year (January)
     */
    fun navigateToBeginningOfYear() {
        _currentMonth.value = _currentMonth.value.withMonth(1)
    }
    
    /**
     * Navigate to the end of the current year (December)
     */
    fun navigateToEndOfYear() {
        _currentMonth.value = _currentMonth.value.withMonth(12)
    }
    
    /**
     * Start observing transactions reactively based on authenticated user and month changes
     */
    private fun startObservingTransactions() {
        println("🔄 HomeViewModel: Starting to observe transactions")
        
        // Observe monthly transactions - this will automatically update when month changes
        monthlyTransactionsJob = viewModelScope.launch {
            try {
                combine(authManager.currentUser, _currentMonth.asStateFlow()) { user, month ->
                    Pair(user, month)
                }.collect { (user, month) ->
                    if (user != null) {
                        println("🔄 HomeViewModel: Loading monthly transactions for month: $month")
                        _isLoading.value = true
                        
                        try {
                            val transactions = transactionRepository.getTransactionsByMonth(month).first()
                            println("✅ HomeViewModel: Loaded ${transactions.size} monthly transactions for $month")
                            
                            // Log detailed transaction information
                            if (transactions.isNotEmpty()) {
                                println("📊 HomeViewModel: Transaction details for $month:")
                                transactions.forEachIndexed { index, transaction ->
                                    println("  ${index + 1}. ${transaction.description} - $${transaction.amount} (${transaction.type}) - ${transaction.date}")
                                }
                                
                                val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                                val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                                println("📊 HomeViewModel: Total Income: $${totalIncome}, Total Expenses: $${totalExpense}")
                            } else {
                                println("⚠️ HomeViewModel: No transactions found for $month")
                            }
                            
                            _transactions.value = transactions
                            _error.value = null
                            _needsAuthentication.value = false
                        } catch (e: SecurityException) {
                            println("🔒 HomeViewModel: Authentication required for monthly transactions")
                            _error.value = "Authentication required"
                            _needsAuthentication.value = true
                        } catch (e: Exception) {
                            // Don't show cancellation errors to the user
                            if (e !is CancellationException) {
                                println("❌ HomeViewModel: Failed to load monthly transactions: ${e.message}")
                                e.printStackTrace()
                                _error.value = "Failed to load transactions: ${e.message}"
                                _needsAuthentication.value = false
                            }
                        } finally {
                            _isLoading.value = false
                        }
                    }
                }
            } catch (e: SecurityException) {
                println("🔒 HomeViewModel: Authentication required for monthly transactions observation")
                _error.value = "Authentication required"
                _needsAuthentication.value = true
                _isLoading.value = false
            } catch (e: Exception) {
                // Don't show cancellation errors to the user
                if (e !is CancellationException) {
                    println("❌ HomeViewModel: Failed to observe monthly transactions: ${e.message}")
                    e.printStackTrace()
                    _error.value = "Failed to load transactions: ${e.message}"
                    _needsAuthentication.value = false
                    _isLoading.value = false
                }
            }
        }
        
        // Observe recurring transactions continuously
        recurringTransactionsJob = viewModelScope.launch {
            try {
                authManager.currentUser.collect { user ->
                    if (user != null) {
                        println("🔄 HomeViewModel: Starting to observe recurring transactions")
                        try {
                            transactionRepository.getRecurringTransactions().collect { transactions ->
                                println("✅ HomeViewModel: Updated recurring transactions: ${transactions.size} transactions")
                                _allRecurringTransactions.value = transactions
                                _error.value = null
                                _needsAuthentication.value = false
                            }
                        } catch (e: SecurityException) {
                            println("🔒 HomeViewModel: Authentication required for recurring transactions")
                            _error.value = "Authentication required"
                            _needsAuthentication.value = true
                        } catch (e: Exception) {
                            // Don't show cancellation errors to the user
                            if (e !is CancellationException) {
                                println("❌ HomeViewModel: Failed to observe recurring transactions: ${e.message}")
                                _error.value = "Failed to load recurring transactions: ${e.message}"
                                _needsAuthentication.value = false
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                println("🔒 HomeViewModel: Authentication required for recurring transactions observation")
                _error.value = "Authentication required"
                _needsAuthentication.value = true
            } catch (e: Exception) {
                // Don't show cancellation errors to the user
                if (e !is CancellationException) {
                    println("❌ HomeViewModel: Failed to observe recurring transactions: ${e.message}")
                    e.printStackTrace()
                    _error.value = "Failed to load recurring transactions: ${e.message}"
                    _needsAuthentication.value = false
                }
            }
        }
        
        // Observe split payment installments continuously
        viewModelScope.launch {
            try {
                authManager.currentUser.collect { user ->
                    if (user != null) {
                        println("🔄 HomeViewModel: Starting to observe split payment installments")
                        try {
                            splitPaymentInstallmentRepository.getInstallments().collect { installments ->
                                println("✅ HomeViewModel: Updated split payment installments: ${installments.size} installments")
                                _splitPaymentInstallments.value = installments
                                _error.value = null
                                _needsAuthentication.value = false
                            }
                        } catch (e: SecurityException) {
                            println("🔒 HomeViewModel: Authentication required for split payment installments")
                            _error.value = "Authentication required"
                            _needsAuthentication.value = true
                        } catch (e: Exception) {
                            // Don't show cancellation errors to the user
                            if (e !is CancellationException) {
                                println("❌ HomeViewModel: Failed to observe split payment installments: ${e.message}")
                                _error.value = "Failed to load split payment installments: ${e.message}"
                                _needsAuthentication.value = false
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                println("🔒 HomeViewModel: Authentication required for split payment installments observation")
                _error.value = "Authentication required"
                _needsAuthentication.value = true
            } catch (e: Exception) {
                // Don't show cancellation errors to the user
                if (e !is CancellationException) {
                    println("❌ HomeViewModel: Failed to observe split payment installments: ${e.message}")
                    e.printStackTrace()
                    _error.value = "Failed to load split payment installments: ${e.message}"
                    _needsAuthentication.value = false
                }
            }
        }
    }
    
    /**
     * Update the payment method filter for expense calculations
     */
    fun setPaymentMethodFilter(paymentMethod: com.pennywise.app.domain.model.PaymentMethod?) {
        _selectedPaymentMethod.value = paymentMethod
    }
    
    /**
     * Clear any error messages and authentication state
     */
    fun clearError() {
        _error.value = null
        _needsAuthentication.value = false
    }
    
    /**
     * Refresh all data - useful for manual refresh after adding new expenses
     */
    fun refreshData() {
        val user = authManager.currentUser.value
        if (user != null) {
            println("🔄 HomeViewModel: Manual refresh requested")
            // Force refresh by triggering the current month change
            val currentMonth = _currentMonth.value
            _currentMonth.value = currentMonth.plusMonths(0) // This will trigger the flow to refresh
            
            // Also force refresh the recurring transactions and split payments
            viewModelScope.launch {
                try {
                    // Force reload monthly transactions
                    val transactions = transactionRepository.getTransactionsByMonth(currentMonth).first()
                    _transactions.value = transactions
                    println("✅ HomeViewModel: Refreshed ${transactions.size} monthly transactions")
                    
                    // Force reload recurring transactions
                    val recurringTransactions = transactionRepository.getRecurringTransactions().first()
                    _allRecurringTransactions.value = recurringTransactions
                    println("✅ HomeViewModel: Refreshed ${recurringTransactions.size} recurring transactions")
                    
                    // Force reload split payment installments
                    val installments = splitPaymentInstallmentRepository.getInstallments().first()
                    _splitPaymentInstallments.value = installments
                    println("✅ HomeViewModel: Refreshed ${installments.size} split payment installments")
                } catch (e: Exception) {
                    println("❌ HomeViewModel: Error during manual refresh: ${e.message}")
                }
            }
        }
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
    
    /**
     * Clean up resources when ViewModel is cleared
     */
    override fun onCleared() {
        super.onCleared()
        monthlyTransactionsJob?.cancel()
        recurringTransactionsJob?.cancel()
    }
}
