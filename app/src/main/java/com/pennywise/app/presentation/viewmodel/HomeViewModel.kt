package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.SplitPaymentInstallment
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.repository.SplitPaymentInstallmentRepository
import com.pennywise.app.domain.repository.PaymentMethodConfigRepository
import com.pennywise.app.domain.model.BillingCycle
import com.pennywise.app.domain.model.getBillingCycles
import com.pennywise.app.data.util.SettingsDataStore
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
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Locale
import java.util.Date
import javax.inject.Inject

/**
 * Combined UI state for the Home screen
 */
data class HomeScreenState(
    val totalAmount: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val netBalance: Double = 0.0,
    val recurringTransactions: List<Transaction> = emptyList(),
    val splitPaymentInstallments: List<SplitPaymentInstallment> = emptyList(),
    val transactionsByWeek: Map<Int, List<Transaction>> = emptyMap(),
    val selectedBillingCycle: BillingCycle? = null,
    val availableBillingCycles: List<BillingCycle> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val needsAuthentication: Boolean = false
)

/**
 * ViewModel for the Home screen that manages monthly expense data
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val splitPaymentInstallmentRepository: SplitPaymentInstallmentRepository,
    private val paymentMethodConfigRepository: PaymentMethodConfigRepository,
    private val settingsDataStore: SettingsDataStore,
    private val authManager: AuthManager
) : ViewModel() {
    
    // Remove _userId - we'll use authManager.currentUser directly
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedPaymentMethod = MutableStateFlow<com.pennywise.app.domain.model.PaymentMethod?>(null)
    
    // Billing cycle state management
    private val _selectedBillingCycle = MutableStateFlow<BillingCycle?>(null)
    val selectedBillingCycle: StateFlow<BillingCycle?> = _selectedBillingCycle.asStateFlow()
    
    private val _availableBillingCycles = MutableStateFlow<List<BillingCycle>>(emptyList())
    val availableBillingCycles: StateFlow<List<BillingCycle>> = _availableBillingCycles.asStateFlow()
    
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
    val transactionsByWeek: StateFlow<Map<Int, List<Transaction>>> = combine(
        _transactions.asStateFlow(),
        _currentMonth.asStateFlow()
    ) { transactions, currentMonth ->
        val startOfMonth = currentMonth.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault())
        val endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault())
        
        transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isRecurring }  // Exclude recurring expenses
            .filter { transaction ->
                // EXCLUDE delayed transactions - they're treated as split payment installments
                // Only show if billing date is in current month AND not delayed
                val billingDate = transaction.getBillingDate()
                val billingInstant = billingDate.toInstant().atZone(java.time.ZoneId.systemDefault())
                
                val isInMonth = !billingInstant.isBefore(startOfMonth) && !billingInstant.isAfter(endOfMonth)
                val isNotDelayed = !transaction.hasDelayedBilling()
                
                isInMonth && isNotDelayed
            }
            .sortedByDescending { it.date }
            .groupBy { transaction ->
                // Group by week based on transaction date (when it was created)
                // This is more intuitive for users to find their purchases
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
        splitPaymentInstallments,
        _selectedPaymentMethod.asStateFlow(),
        _currentMonth.asStateFlow()
    ) { transactions, recurringTransactions, splitInstallments, selectedPaymentMethod, currentMonth ->
        val startOfMonth = currentMonth.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault())
        val endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault())
        
        println("💰 HomeViewModel: Calculating totalExpenses for $currentMonth")
        println("💰 HomeViewModel: Total transactions available: ${transactions.size}")
        
        // Only include NON-recurring expenses from monthly transactions
        // Filter by BILLING date, not transaction date
        // EXCLUDE delayed transactions - they're handled as split payment installments
        val regularExpensesFiltered = transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isRecurring }
            .filter { transaction ->
                // EXCLUDE delayed transactions - they're treated as split payment installments
                !transaction.hasDelayedBilling()
            }
            .filter { transaction ->
                // Include transaction if its BILLING date falls within the current month
                val billingDate = transaction.getBillingDate()
                val billingInstant = billingDate.toInstant().atZone(java.time.ZoneId.systemDefault())
                
                val isInMonth = !billingInstant.isBefore(startOfMonth) && !billingInstant.isAfter(endOfMonth)
                
                isInMonth
            }
            .filter { transaction ->
                selectedPaymentMethod?.let { method ->
                    transaction.paymentMethod == method
                } ?: true // Show all if no filter selected
            }
        
        val regularExpenses = regularExpensesFiltered.sumOf { it.amount }
        println("💰 HomeViewModel: Regular expenses count: ${regularExpensesFiltered.size}, total: $$regularExpenses")
        
        val recurringExpenses = recurringTransactions
            .filter { transaction ->
                selectedPaymentMethod?.let { method ->
                    transaction.paymentMethod == method
                } ?: true // Show all if no filter selected
            }
            .sumOf { it.amount }
        
        println("💰 HomeViewModel: Recurring expenses: $$recurringExpenses")
        
        // Add split payment installments (they're already filtered by month)
        // Note: Split installments don't have paymentMethod field, so we include all of them
        val splitPaymentExpenses = splitInstallments.sumOf { it.amount }
        
        println("💰 HomeViewModel: Split payment installments: $$splitPaymentExpenses (${splitInstallments.size} installments)")
        println("💰 HomeViewModel: TOTAL EXPENSES for $currentMonth: $${regularExpenses + recurringExpenses + splitPaymentExpenses}")
        
        regularExpenses + recurringExpenses + splitPaymentExpenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )
    
    val netBalance: StateFlow<Double> = combine(
        _transactions.asStateFlow(),
        recurringTransactions,
        splitPaymentInstallments,
        _currentMonth.asStateFlow()
    ) { transactions, recurringTransactions, splitInstallments, currentMonth ->
        val startOfMonth = currentMonth.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault())
        val endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault())
        
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        // Only include NON-recurring expenses from monthly transactions
        // Filter by BILLING date, not transaction date
        // EXCLUDE delayed transactions - they're handled as split payment installments
        val regularExpenses = transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isRecurring }
            .filter { transaction ->
                // EXCLUDE delayed transactions - they're treated as split payment installments
                !transaction.hasDelayedBilling()
            }
            .filter { transaction ->
                // Include transaction if its BILLING date falls within the current month
                val billingDate = transaction.getBillingDate()
                val billingInstant = billingDate.toInstant().atZone(java.time.ZoneId.systemDefault())
                
                !billingInstant.isBefore(startOfMonth) && !billingInstant.isAfter(endOfMonth)
            }
            .sumOf { it.amount }
        val recurringExpenses = recurringTransactions.sumOf { it.amount }
        val splitPaymentExpenses = splitInstallments.sumOf { it.amount }
        income - (regularExpenses + recurringExpenses + splitPaymentExpenses)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )
    
    // Combined UI state for HomeScreen (defined after all dependencies)
    // Note: combine() only supports up to 5 flows, so we use nested combines
    val uiState: StateFlow<HomeScreenState> = combine(
        combine(
            totalIncome,
            totalExpenses,
            netBalance
        ) { income, expenses, balance ->
            Triple(income, expenses, balance)
        },
        combine(
            recurringTransactions,
            splitPaymentInstallments,
            transactionsByWeek
        ) { recurring, installments, byWeek ->
            Triple(recurring, installments, byWeek)
        },
        combine(
            selectedBillingCycle,
            availableBillingCycles
        ) { selectedCycle, availableCycles ->
            Pair(selectedCycle, availableCycles)
        },
        combine(
            isLoading,
            error,
            needsAuthentication
        ) { loading, err, needsAuth ->
            Triple(loading, err, needsAuth)
        }
    ) { financial, transactions, billing, uiFlags ->
        val (income, expenses, balance) = financial
        val (recurring, installments, byWeek) = transactions
        val (selectedCycle, availableCycles) = billing
        val (loading, err, needsAuth) = uiFlags
        
        HomeScreenState(
            totalAmount = expenses,
            totalIncome = income,
            totalExpenses = expenses,
            netBalance = balance,
            recurringTransactions = recurring,
            splitPaymentInstallments = installments,
            transactionsByWeek = byWeek,
            selectedBillingCycle = selectedCycle,
            availableBillingCycles = availableCycles,
            isLoading = loading,
            error = err,
            needsAuthentication = needsAuth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = HomeScreenState()
    )
    
    // Job management for proper coroutine lifecycle
    private var monthlyTransactionsJob: Job? = null
    private var recurringTransactionsJob: Job? = null
    
    init {
        // Load billing cycles first
        loadBillingCycles()
        // Start observing transactions immediately when ViewModel is created
        startObservingTransactions()
        // Observe billing cycle selection changes
        observeBillingCycleChanges()
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
     * Also converts delayed transactions into pending installments
     */
    private fun filterSplitPaymentInstallmentsForMonth(
        allInstallments: List<SplitPaymentInstallment>,
        currentMonth: YearMonth
    ): List<SplitPaymentInstallment> {
        val startOfMonth = currentMonth.atDay(1).atStartOfDay()
        val endOfMonth = currentMonth.atEndOfMonth().atTime(23, 59, 59)
        
        println("🔍 HomeViewModel: Filtering split payment installments for month: $currentMonth")
        println("🔍 HomeViewModel: Date range: $startOfMonth to $endOfMonth")
        println("🔍 HomeViewModel: Total installments to filter: ${allInstallments.size}")
        
        // Filter existing split payment installments
        val filteredInstallments = allInstallments.filter { installment ->
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
        
        // Convert delayed transactions into "pending installments" and include them
        val delayedTransactions = _transactions.value.filter { transaction ->
            transaction.hasDelayedBilling()
        }
        
        println("🔍 HomeViewModel: Found ${delayedTransactions.size} delayed transactions to convert to installments")
        
        val pendingInstallments = delayedTransactions.map { transaction ->
            SplitPaymentInstallment(
                id = transaction.id, // Use transaction ID as a unique identifier
                parentTransactionId = transaction.id,
                amount = transaction.amount,
                currency = transaction.currency,
                description = transaction.description,
                category = transaction.category,
                type = transaction.type,
                dueDate = transaction.getBillingDate(), // The billing date
                installmentNumber = 1,
                totalInstallments = 1,
                isPaid = false, // Always pending until billed
                paidDate = null,
                createdAt = transaction.createdAt,
                updatedAt = transaction.updatedAt
            )
        }.filter { installment ->
            // Only include if billing date is in current month
            val dueDate = installment.dueDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            val isInCurrentMonth = !dueDate.isBefore(startOfMonth) && !dueDate.isAfter(endOfMonth)
            
            if (isInCurrentMonth) {
                println("✅ HomeViewModel: Including delayed transaction as pending installment: ${installment.description} - ${dueDate.toLocalDate()}")
            }
            
            isInCurrentMonth
        }
        
        val allFiltered = filteredInstallments + pendingInstallments
        println("🔍 HomeViewModel: Filtered to ${allFiltered.size} total (${filteredInstallments.size} split + ${pendingInstallments.size} delayed) for $currentMonth")
        
        return allFiltered
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
        // We need to fetch transactions from current month AND previous months (up to 90 days back)
        // to capture delayed billing transactions that will be billed in the current month
        monthlyTransactionsJob = viewModelScope.launch {
            try {
                combine(authManager.currentUser, _currentMonth.asStateFlow()) { user, month ->
                    Pair(user, month)
                }.collect { (user, month) ->
                    if (user != null) {
                        println("🔄 HomeViewModel: Loading transactions for month: $month (including delayed billing)")
                        _isLoading.value = true
                        
                        try {
                            // Load current month AND previous 3 months to capture delayed billing (max 90 days)
                            val monthsToLoad = listOf(
                                month,           // Current month
                                month.minusMonths(1), // Previous month (for 30-day delays)
                                month.minusMonths(2), // 2 months ago (for 60-day delays)
                                month.minusMonths(3)  // 3 months ago (for 90-day delays)
                            )
                            
                            val allTransactions = mutableListOf<Transaction>()
                            monthsToLoad.forEach { monthToLoad ->
                                val monthTransactions = transactionRepository.getTransactionsByMonth(monthToLoad).first()
                                allTransactions.addAll(monthTransactions)
                                println("📅 HomeViewModel: Loaded ${monthTransactions.size} transactions from $monthToLoad")
                            }
                            
                            println("✅ HomeViewModel: Loaded ${allTransactions.size} total transactions (will filter by billing date)")
                            
                            _transactions.value = allTransactions
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
            val currentMonth = _currentMonth.value
            
            // Force refresh by loading transactions from current and previous months
            viewModelScope.launch {
                try {
                    // Load current month AND previous 3 months to capture delayed billing (max 90 days)
                    val monthsToLoad = listOf(
                        currentMonth,           // Current month
                        currentMonth.minusMonths(1), // Previous month (for 30-day delays)
                        currentMonth.minusMonths(2), // 2 months ago (for 60-day delays)
                        currentMonth.minusMonths(3)  // 3 months ago (for 90-day delays)
                    )
                    
                    val allTransactions = mutableListOf<Transaction>()
                    monthsToLoad.forEach { monthToLoad ->
                        val monthTransactions = transactionRepository.getTransactionsByMonth(monthToLoad).first()
                        allTransactions.addAll(monthTransactions)
                    }
                    
                    _transactions.value = allTransactions
                    println("✅ HomeViewModel: Refreshed ${allTransactions.size} total transactions (including delayed billing)")
                    
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
     * Load billing cycles from all credit cards
     */
    private fun loadBillingCycles() {
        viewModelScope.launch {
            try {
                val user = authManager.currentUser.value
                if (user == null) {
                    println("🔒 HomeViewModel: User not authenticated, cannot load billing cycles")
                    return@launch
                }
                
                println("🔄 HomeViewModel: Loading billing cycles from payment method configs")
                _isLoading.value = true
                
                // Get all payment method configs
                val configs = paymentMethodConfigRepository.getPaymentMethodConfigs().first()
                
                // Generate billing cycles for all credit cards
                val allCycles = configs.flatMap { config ->
                    config.getBillingCycles(3) // Get last 3 billing cycles per card
                }
                
                // Sort cycles by end date (most recent first)
                val sortedCycles = allCycles.sortedByDescending { it.endDate }
                
                _availableBillingCycles.value = sortedCycles
                
                // Set default selection to most recent cycle if none selected
                if (_selectedBillingCycle.value == null && sortedCycles.isNotEmpty()) {
                    _selectedBillingCycle.value = sortedCycles.first()
                    println("✅ HomeViewModel: Auto-selected most recent billing cycle: ${sortedCycles.first().displayName}")
                }
                
                println("✅ HomeViewModel: Loaded ${sortedCycles.size} billing cycles from ${configs.size} cards")
                _error.value = null
            } catch (e: SecurityException) {
                println("🔒 HomeViewModel: Authentication required for loading billing cycles")
                _error.value = "Authentication required"
                _needsAuthentication.value = true
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    println("❌ HomeViewModel: Failed to load billing cycles: ${e.message}")
                    e.printStackTrace()
                    _error.value = "Failed to load billing cycles: ${e.message}"
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Select a billing cycle and load transactions for it
     */
    fun selectBillingCycle(billingCycle: BillingCycle) {
        println("🔄 HomeViewModel: Selecting billing cycle: ${billingCycle.displayName}")
        _selectedBillingCycle.value = billingCycle
    }
    
    /**
     * Observe billing cycle selection changes and load transactions accordingly
     */
    private fun observeBillingCycleChanges() {
        viewModelScope.launch {
            _selectedBillingCycle.asStateFlow().collect { selectedCycle ->
                if (selectedCycle != null) {
                    println("🔄 HomeViewModel: Billing cycle changed, loading transactions for: ${selectedCycle.displayName}")
                    loadTransactionsForBillingCycle(selectedCycle)
                }
            }
        }
    }
    
    /**
     * Load transactions for the selected billing cycle
     */
    private suspend fun loadTransactionsForBillingCycle(billingCycle: BillingCycle) {
        try {
            println("🔄 HomeViewModel: Loading transactions for billing cycle: ${billingCycle.displayName}")
            println("🔄 HomeViewModel: Date range: ${billingCycle.startDate} to ${billingCycle.endDate}")
            _isLoading.value = true
            
            // Convert LocalDate to Date for repository
            val startDate = Date.from(
                billingCycle.startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            )
            val endDate = Date.from(
                billingCycle.endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            )
            
            // Get transactions for the billing cycle date range
            val transactions = transactionRepository.getTransactionsByDateRange(startDate, endDate).first()
            
            println("✅ HomeViewModel: Loaded ${transactions.size} transactions for billing cycle")
            
            // Update transactions - note: this will be filtered by existing month logic
            // For now, we'll store them but the existing month-based filtering will still apply
            // In a future update, we can add billing cycle-specific filtering
            _transactions.value = transactions
            _error.value = null
            _needsAuthentication.value = false
        } catch (e: SecurityException) {
            println("🔒 HomeViewModel: Authentication required for billing cycle transactions")
            _error.value = "Authentication required"
            _needsAuthentication.value = true
        } catch (e: Exception) {
            if (e !is CancellationException) {
                println("❌ HomeViewModel: Failed to load transactions for billing cycle: ${e.message}")
                e.printStackTrace()
                _error.value = "Failed to load transactions: ${e.message}"
                _needsAuthentication.value = false
            }
        } finally {
            _isLoading.value = false
        }
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
