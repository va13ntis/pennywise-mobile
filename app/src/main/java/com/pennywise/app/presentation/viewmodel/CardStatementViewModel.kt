package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.PaymentMethodConfig
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.repository.PaymentMethodConfigRepository
import com.pennywise.app.domain.util.BillingCycleUtils
import com.pennywise.app.presentation.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject

/**
 * UI state for Card Statement screen
 */
data class CardStatementUiState(
    val cardConfig: PaymentMethodConfig? = null,
    val currentCycleIndex: Int = 0,
    val availableCycles: List<Pair<Date, Date>> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val totalAmount: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel for Card Statement screen that manages billing cycle-based transaction display
 */
@HiltViewModel
class CardStatementViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val paymentMethodConfigRepository: PaymentMethodConfigRepository,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CardStatementUiState())
    val uiState: StateFlow<CardStatementUiState> = _uiState.asStateFlow()
    
    private val _cardId = MutableStateFlow<Long?>(null)
    private val _allTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    
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
     * Initialize the screen with a card ID
     */
    fun initialize(cardId: Long) {
        if (_cardId.value == cardId) return // Already initialized with this card
        
        _cardId.value = cardId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                // Load card config
                val cardConfig = paymentMethodConfigRepository.getPaymentMethodConfigById(cardId)
                
                if (cardConfig == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Card not found"
                    )
                    return@launch
                }
                
                // Load all transactions
                val transactions = transactionRepository.getTransactions().first()
                
                // Filter transactions for this card
                val cardTransactions = transactions.filter { 
                    it.paymentMethodConfigId == cardId 
                }
                
                _allTransactions.value = cardTransactions
                
                // Calculate available billing cycles
                val cycles = calculateAvailableCycles(cardTransactions, cardConfig.withdrawDay)
                
                // Set initial cycle to most recent (last in list)
                val initialCycleIndex = if (cycles.isNotEmpty()) cycles.size - 1 else 0
                
                // Filter transactions for initial cycle
                val initialTransactions = if (cycles.isNotEmpty()) {
                    filterTransactionsForCycle(cardTransactions, cycles[initialCycleIndex])
                } else {
                    emptyList()
                }
                
                val totalAmount = initialTransactions.sumOf { it.amount }
                
                _uiState.value = CardStatementUiState(
                    cardConfig = cardConfig,
                    currentCycleIndex = initialCycleIndex,
                    availableCycles = cycles,
                    transactions = initialTransactions,
                    totalAmount = totalAmount,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load card statement"
                )
            }
        }
    }
    
    /**
     * Change to a different billing cycle
     */
    fun changeCycle(cycleIndex: Int) {
        if (cycleIndex < 0 || cycleIndex >= _uiState.value.availableCycles.size) return
        
        val cycle = _uiState.value.availableCycles[cycleIndex]
        val cardTransactions = _allTransactions.value
        val filteredTransactions = filterTransactionsForCycle(cardTransactions, cycle)
        val totalAmount = filteredTransactions.sumOf { it.amount }
        
        _uiState.value = _uiState.value.copy(
            currentCycleIndex = cycleIndex,
            transactions = filteredTransactions,
            totalAmount = totalAmount
        )
    }
    
    /**
     * Navigate to previous cycle
     */
    fun previousCycle() {
        val currentIndex = _uiState.value.currentCycleIndex
        if (currentIndex > 0) {
            changeCycle(currentIndex - 1)
        }
    }
    
    /**
     * Navigate to next cycle
     */
    fun nextCycle() {
        val currentIndex = _uiState.value.currentCycleIndex
        val cycles = _uiState.value.availableCycles
        if (currentIndex < cycles.size - 1) {
            changeCycle(currentIndex + 1)
        }
    }
    
    /**
     * Calculate available billing cycles from transactions
     */
    private fun calculateAvailableCycles(
        transactions: List<Transaction>,
        withdrawDay: Int?
    ): List<Pair<Date, Date>> {
        if (withdrawDay == null || transactions.isEmpty()) {
            return emptyList()
        }
        
        // Calculate cycle for each transaction and collect unique cycles
        val cycles = transactions.mapNotNull { transaction ->
            try {
                BillingCycleUtils.calculateBillingCycle(transaction.date, withdrawDay)
            } catch (e: Exception) {
                null
            }
        }.distinctBy { it.first.time } // Distinct by cycle start date
        
        // Sort cycles by start date (oldest first)
        return cycles.sortedBy { it.first.time }
    }
    
    /**
     * Filter transactions that belong to a specific billing cycle
     * Uses effective billing date (transaction.getBillingDate())
     */
    private fun filterTransactionsForCycle(
        transactions: List<Transaction>,
        cycle: Pair<Date, Date>
    ): List<Transaction> {
        val (cycleStart, cycleEnd) = cycle
        
        return transactions.filter { transaction ->
            val effectiveBillingDate = transaction.getBillingDate()
            
            // Check if effective billing date is within cycle boundaries (inclusive)
            effectiveBillingDate.time >= cycleStart.time && 
            effectiveBillingDate.time <= cycleEnd.time
        }.sortedByDescending { it.date } // Sort by purchase date, newest first
    }
}

