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
import com.pennywise.app.data.service.CurrencyConversionService
import com.pennywise.app.data.util.MerchantIconRepository
import com.pennywise.app.data.util.NetworkUtils
import com.pennywise.app.presentation.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
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
import java.io.File

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
    private val authManager: AuthManager,
    private val currencyConversionService: CurrencyConversionService,
    private val merchantIconRepository: MerchantIconRepository,
    private val networkUtils: NetworkUtils
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
    private val _installmentParentTransactions = MutableStateFlow<Map<Long, Transaction>>(emptyMap())
    val installmentParentTransactions: StateFlow<Map<Long, Transaction>> = _installmentParentTransactions.asStateFlow()
    
    private val _convertedTransactionAmounts = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val convertedTransactionAmounts: StateFlow<Map<Long, Double>> = _convertedTransactionAmounts.asStateFlow()

    private val _convertedInstallmentAmounts = MutableStateFlow<Map<Long, Double>>(emptyMap())
    val convertedInstallmentAmounts: StateFlow<Map<Long, Double>> = _convertedInstallmentAmounts.asStateFlow()

    private val _merchantIconFiles = MutableStateFlow<Map<String, File>>(emptyMap())
    val merchantIconFiles: StateFlow<Map<String, File>> = _merchantIconFiles.asStateFlow()
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
    
    val totalIncome: StateFlow<Double> = combine(
        _transactions.asStateFlow(),
        _convertedTransactionAmounts.asStateFlow()
    ) { transactions, convertedAmounts ->
        transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { transaction ->
                convertedAmounts[transaction.id] ?: transaction.amount
            }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )

    private val expenseInputs = combine(
        _transactions.asStateFlow(),
        recurringTransactions,
        splitPaymentInstallments,
        _selectedPaymentMethod.asStateFlow(),
        _currentMonth.asStateFlow()
    ) { transactions, recurringTransactions, splitInstallments, selectedPaymentMethod, currentMonth ->
        ExpenseInputs(
            transactions = transactions,
            recurringTransactions = recurringTransactions,
            splitInstallments = splitInstallments,
            selectedPaymentMethod = selectedPaymentMethod,
            currentMonth = currentMonth
        )
    }

    val totalExpenses: StateFlow<Double> = combine(
        expenseInputs,
        _convertedTransactionAmounts.asStateFlow(),
        _convertedInstallmentAmounts.asStateFlow()
    ) { inputs, convertedAmounts, convertedInstallments ->
        val startOfMonth = inputs.currentMonth.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault())
        val endOfMonth = inputs.currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault())

        // Only include NON-recurring expenses from monthly transactions
        // Filter by BILLING date, not transaction date
        // EXCLUDE delayed transactions - they're handled as split payment installments
        val regularExpensesFiltered = inputs.transactions
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
            .filter { transaction ->
                inputs.selectedPaymentMethod?.let { method ->
                    transaction.paymentMethod == method
                } ?: true // Show all if no filter selected
            }

        val regularExpenses = regularExpensesFiltered.sumOf { transaction ->
            convertedAmounts[transaction.id] ?: transaction.amount
        }

        val recurringExpenses = inputs.recurringTransactions
            .filter { transaction ->
                inputs.selectedPaymentMethod?.let { method ->
                    transaction.paymentMethod == method
                } ?: true // Show all if no filter selected
            }
            .sumOf { transaction ->
                convertedAmounts[transaction.id] ?: transaction.amount
            }

        // Add split payment installments (they're already filtered by month)
        // Note: Split installments don't have paymentMethod field, so we include all of them
        val splitPaymentExpenses = inputs.splitInstallments.sumOf { installment ->
            convertedInstallments[installment.id] ?: installment.amount
        }

        regularExpenses + recurringExpenses + splitPaymentExpenses
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0.0
    )

    val netBalance: StateFlow<Double> = combine(
        expenseInputs,
        _convertedTransactionAmounts.asStateFlow(),
        _convertedInstallmentAmounts.asStateFlow()
    ) { inputs, convertedAmounts, convertedInstallments ->
        val startOfMonth = inputs.currentMonth.atDay(1).atStartOfDay(java.time.ZoneId.systemDefault())
        val endOfMonth = inputs.currentMonth.atEndOfMonth().atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault())

        val income = inputs.transactions.filter { it.type == TransactionType.INCOME }.sumOf { transaction ->
            convertedAmounts[transaction.id] ?: transaction.amount
        }
        // Only include NON-recurring expenses from monthly transactions
        // Filter by BILLING date, not transaction date
        // EXCLUDE delayed transactions - they're handled as split payment installments
        val regularExpenses = inputs.transactions
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
            .sumOf { transaction ->
                convertedAmounts[transaction.id] ?: transaction.amount
            }
        val recurringExpenses = inputs.recurringTransactions.sumOf { transaction ->
            convertedAmounts[transaction.id] ?: transaction.amount
        }
        val splitPaymentExpenses = inputs.splitInstallments.sumOf { installment ->
            convertedInstallments[installment.id] ?: installment.amount
        }
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
        // Keep converted amounts in sync with data and currency
        observeCurrencyConversions()
        // Load any cached merchant icons
        _merchantIconFiles.value = merchantIconRepository.loadCachedIcons()
    }
    
    private fun observeCurrencyConversions() {
        viewModelScope.launch {
            combine(
                _transactions.asStateFlow(),
                _allRecurringTransactions.asStateFlow(),
                _splitPaymentInstallments.asStateFlow(),
                currency
            ) { transactions, recurringTransactions, splitInstallments, targetCurrency ->
                ConversionPayload(transactions, recurringTransactions, splitInstallments, targetCurrency)
            }.collectLatest { payload ->
                updateConvertedAmounts(
                    transactions = payload.transactions,
                    recurringTransactions = payload.recurringTransactions,
                    splitInstallments = payload.splitInstallments,
                    targetCurrency = payload.targetCurrency
                )
            }
        }
    }

    private suspend fun updateConvertedAmounts(
        transactions: List<Transaction>,
        recurringTransactions: List<Transaction>,
        splitInstallments: List<SplitPaymentInstallment>,
        targetCurrency: String
    ) {
        val conversionRates = mutableMapOf<String, Double?>()
        val combinedTransactions = (transactions + recurringTransactions)
            .associateBy { it.id }
            .values

        val transactionAmounts = mutableMapOf<Long, Double>()
        combinedTransactions.forEach { transaction ->
            val rateKey = "${transaction.currency.uppercase()}_${targetCurrency.uppercase()}"
            val rate = conversionRates.getOrPut(rateKey) {
                currencyConversionService.convertCurrency(1.0, transaction.currency, targetCurrency)
            }
            rate?.let { transactionAmounts[transaction.id] = transaction.amount * it }
        }

        val installmentAmounts = mutableMapOf<Long, Double>()
        splitInstallments.forEach { installment ->
            val rateKey = "${installment.currency.uppercase()}_${targetCurrency.uppercase()}"
            val rate = conversionRates.getOrPut(rateKey) {
                currencyConversionService.convertCurrency(1.0, installment.currency, targetCurrency)
            }
            rate?.let { installmentAmounts[installment.id] = installment.amount * it }
        }

        _convertedTransactionAmounts.value = transactionAmounts
        _convertedInstallmentAmounts.value = installmentAmounts
    }

    private data class ExpenseInputs(
        val transactions: List<Transaction>,
        val recurringTransactions: List<Transaction>,
        val splitInstallments: List<SplitPaymentInstallment>,
        val selectedPaymentMethod: com.pennywise.app.domain.model.PaymentMethod?,
        val currentMonth: YearMonth
    )

    private data class ConversionPayload(
        val transactions: List<Transaction>,
        val recurringTransactions: List<Transaction>,
        val splitInstallments: List<SplitPaymentInstallment>,
        val targetCurrency: String
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
            
            shouldAppear
        }
        
        return filtered
    }
    
    /**
     * Filter split payment installments for the current month.
     * Includes previous month so credit card billing cycles (e.g. Feb 10 - Mar 9) see installments
     * due in the cycle's early part (Feb 10-28) that would otherwise be excluded.
     * Also converts delayed transactions into pending installments.
     */
    private fun filterSplitPaymentInstallmentsForMonth(
        allInstallments: List<SplitPaymentInstallment>,
        currentMonth: YearMonth
    ): List<SplitPaymentInstallment> {
        // Include previous month: credit card cycles span (prevMonth.withdrawDay)..(currMonth.withdrawDay-1)
        val startOfRange = currentMonth.minusMonths(1).atDay(1).atStartOfDay()
        val endOfRange = currentMonth.atEndOfMonth().atTime(23, 59, 59)
        
        // Filter existing split payment installments
        val filteredInstallments = allInstallments.filter { installment ->
            val dueDate = installment.dueDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            
            val isInRange = !dueDate.isBefore(startOfRange) && !dueDate.isAfter(endOfRange)
            
            // Exclude the first installment (installmentNumber == 1) since it's already shown in the week section
            val isNotFirstInstallment = installment.installmentNumber > 1
            
            isInRange && isNotFirstInstallment
        }
        
        // Convert delayed transactions into "pending installments" and include them
        val delayedTransactions = _transactions.value.filter { transaction ->
            transaction.hasDelayedBilling()
        }
        
        
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
            // Only include if billing date is in range (prev month + current month for cycle support)
            val dueDate = installment.dueDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
            !dueDate.isBefore(startOfRange) && !dueDate.isAfter(endOfRange)
        }
        
        val allFiltered = filteredInstallments + pendingInstallments
        
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
        
        // Observe monthly transactions - this will automatically update when month changes
        // We need to fetch transactions from current month AND previous months (up to 90 days back)
        // to capture delayed billing transactions that will be billed in the current month
        monthlyTransactionsJob = viewModelScope.launch {
            try {
                combine(authManager.currentUser, _currentMonth.asStateFlow()) { user, month ->
                    Pair(user, month)
                }.collect { (user, month) ->
                    if (user != null) {
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
                            }
                            
                            
                            _transactions.value = allTransactions
                            _error.value = null
                            _needsAuthentication.value = false
                        } catch (e: SecurityException) {
                            _error.value = "Authentication required"
                            _needsAuthentication.value = true
                        } catch (e: Exception) {
                            // Don't show cancellation errors to the user
                            if (e !is CancellationException) {
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
                _error.value = "Authentication required"
                _needsAuthentication.value = true
                _isLoading.value = false
            } catch (e: Exception) {
                // Don't show cancellation errors to the user
                if (e !is CancellationException) {
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
                        try {
                            transactionRepository.getRecurringTransactions().collect { transactions ->
                                _allRecurringTransactions.value = transactions
                                _error.value = null
                                _needsAuthentication.value = false
                            }
                        } catch (e: SecurityException) {
                            _error.value = "Authentication required"
                            _needsAuthentication.value = true
                        } catch (e: Exception) {
                            // Don't show cancellation errors to the user
                            if (e !is CancellationException) {
                                _error.value = "Failed to load recurring transactions: ${e.message}"
                                _needsAuthentication.value = false
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                _error.value = "Authentication required"
                _needsAuthentication.value = true
            } catch (e: Exception) {
                // Don't show cancellation errors to the user
                if (e !is CancellationException) {
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
                        try {
                            splitPaymentInstallmentRepository.getInstallments().collect { installments ->
                                _splitPaymentInstallments.value = installments
                                _error.value = null
                                _needsAuthentication.value = false
                            }
                        } catch (e: SecurityException) {
                            _error.value = "Authentication required"
                            _needsAuthentication.value = true
                        } catch (e: Exception) {
                            // Don't show cancellation errors to the user
                            if (e !is CancellationException) {
                                _error.value = "Failed to load split payment installments: ${e.message}"
                                _needsAuthentication.value = false
                            }
                        }
                    }
                }
            } catch (e: SecurityException) {
                _error.value = "Authentication required"
                _needsAuthentication.value = true
            } catch (e: Exception) {
                // Don't show cancellation errors to the user
                if (e !is CancellationException) {
                    e.printStackTrace()
                    _error.value = "Failed to load split payment installments: ${e.message}"
                    _needsAuthentication.value = false
                }
            }
        }
        
        // Load parent transactions for installments whose parent is not in our 4-month window
        viewModelScope.launch {
            combine(
                splitPaymentInstallments,
                _transactions.asStateFlow(),
                _allRecurringTransactions.asStateFlow()
            ) { installments, transactions, recurring ->
                val existingIds = (transactions + recurring).map { it.id }.toSet()
                val parentIdsToLoad = installments.map { it.parentTransactionId }.distinct().filter { it !in existingIds }
                parentIdsToLoad
            }.collect { parentIdsToLoad ->
                if (parentIdsToLoad.isEmpty()) {
                    _installmentParentTransactions.value = emptyMap()
                } else {
                    val loaded = parentIdsToLoad.mapNotNull { id ->
                        transactionRepository.getTransactionById(id)?.let { id to it }
                    }.toMap()
                    _installmentParentTransactions.value = loaded
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
                    
                    // Force reload recurring transactions
                    val recurringTransactions = transactionRepository.getRecurringTransactions().first()
                    _allRecurringTransactions.value = recurringTransactions
                    
                    // Force reload split payment installments
                    val installments = splitPaymentInstallmentRepository.getInstallments().first()
                    _splitPaymentInstallments.value = installments
                } catch (e: Exception) {
                }
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
            refreshData()
        }
    }

    fun refreshMerchantIcons(
        merchantNames: List<String>,
        wifiOnly: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (wifiOnly && !networkUtils.isWifiConnected()) {
                return@launch
            }

            val updated = _merchantIconFiles.value.toMutableMap()
            merchantNames.distinct().forEach { merchantName ->
                if (merchantName.isBlank()) return@forEach
                val cached = merchantIconRepository.getCachedIconFile(merchantName)
                if (cached != null) {
                    updated[cached.nameWithoutExtension] = cached
                    return@forEach
                }
                val file = merchantIconRepository.fetchAndCacheIcon(merchantName) ?: return@forEach
                updated[file.nameWithoutExtension] = file
                _merchantIconFiles.value = updated.toMap()
            }
        }
    }
    
    
    /**
     * Ensures currentMonth is set so the displayed billing cycle contains today's date.
     * For credit cards: cycle is (previousMonth.withdrawDay) .. (currentMonth.withdrawDay - 1).
     * If today falls after the cycle end, advance to next month; if before cycle start, go to previous month.
     */
    private fun ensureCurrentMonthShowsActiveBillingCycle(configs: List<com.pennywise.app.domain.model.PaymentMethodConfig>) {
        val card = configs.firstOrNull { it.isCreditCard() } ?: return
        val withdrawDay = card.withdrawDay!!
        val today = LocalDate.now()
        val currentMonth = _currentMonth.value
        val previousMonth = currentMonth.minusMonths(1)
        val validDayPrev = minOf(withdrawDay, previousMonth.lengthOfMonth())
        val validDayCur = minOf(withdrawDay, currentMonth.lengthOfMonth())
        val cycleStart = previousMonth.atDay(validDayPrev)
        val cycleEnd = currentMonth.atDay(validDayCur).minusDays(1)
        val todayAfter = today.isAfter(cycleEnd)
        val todayBefore = today.isBefore(cycleStart)
        when {
            todayAfter -> _currentMonth.value = currentMonth.plusMonths(1)
            todayBefore -> _currentMonth.value = currentMonth.minusMonths(1)
        }
    }

    /**
     * Load billing cycles from all credit cards.
     * Observes authManager.currentUser so it runs when user becomes available (fixes race where
     * init runs before auth completes and ensureCurrentMonthShowsActiveBillingCycle was never called).
     */
    private fun loadBillingCycles() {
        viewModelScope.launch {
            try {
                authManager.currentUser.collect { user ->
                    if (user == null) return@collect
                    try {
                        _isLoading.value = true
                        val configs = paymentMethodConfigRepository.getPaymentMethodConfigs().first()
                        ensureCurrentMonthShowsActiveBillingCycle(configs)
                        val allCycles = configs.flatMap { it.getBillingCycles(3) }
                        val sortedCycles = allCycles.sortedByDescending { it.endDate }
                        _availableBillingCycles.value = sortedCycles
                        if (_selectedBillingCycle.value == null && sortedCycles.isNotEmpty()) {
                            _selectedBillingCycle.value = sortedCycles.first()
                        }
                        _error.value = null
                    } catch (e: SecurityException) {
                        _error.value = "Authentication required"
                        _needsAuthentication.value = true
                    } catch (e: Exception) {
                        if (e !is CancellationException) {
                            e.printStackTrace()
                            _error.value = "Failed to load billing cycles: ${e.message}"
                        }
                    } finally {
                        _isLoading.value = false
                    }
                }
            } catch (e: SecurityException) {
                _error.value = "Authentication required"
                _needsAuthentication.value = true
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    e.printStackTrace()
                    _error.value = "Failed to load billing cycles: ${e.message}"
                }
            }
        }
    }
    
    /**
     * Select a billing cycle and load transactions for it
     */
    fun selectBillingCycle(billingCycle: BillingCycle) {
        _selectedBillingCycle.value = billingCycle
    }
    
    /**
     * Observe billing cycle selection changes and load transactions accordingly
     */
    private fun observeBillingCycleChanges() {
        viewModelScope.launch {
            _selectedBillingCycle.asStateFlow().collect { selectedCycle ->
                if (selectedCycle != null) {
                    loadTransactionsForBillingCycle(selectedCycle)
                }
            }
        }
    }
    
    /**
     * Load transactions for the selected billing cycle.
     * Does NOT overwrite _transactions - startObservingTransactions keeps 4 months of data
     * which covers any billing cycle. Totals are computed in HomeScreen by filtering raw
     * transactions by billing cycle in computePaymentMethodSummaries.
     */
    private suspend fun loadTransactionsForBillingCycle(@Suppress("UNUSED_PARAMETER") billingCycle: BillingCycle) {
        try {
            // Do NOT overwrite _transactions - keep 4 months from startObservingTransactions
            // so totals and recurring can be computed correctly by filtering in the UI
            _error.value = null
            _needsAuthentication.value = false
        } catch (e: SecurityException) {
            _error.value = "Authentication required"
            _needsAuthentication.value = true
        } catch (e: Exception) {
            if (e !is CancellationException) {
                e.printStackTrace()
                _error.value = "Failed to load transactions: ${e.message}"
                _needsAuthentication.value = false
            }
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
