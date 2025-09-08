package com.pennywise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.usecase.CurrencySortingService
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.utils.CurrencyListHelpers
import com.pennywise.app.presentation.utils.CurrencyViewModelExtensions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for currency selection UI components
 * Integrates with CurrencySortingService to provide sorted currency lists
 */
@HiltViewModel
class CurrencySelectionViewModel @Inject constructor(
    private val currencySortingService: CurrencySortingService,
    private val authManager: AuthManager
) : ViewModel() {
    
    // Search query for filtering currencies
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery
    
    // Selected currency
    private val _selectedCurrency = MutableStateFlow<Currency?>(null)
    val selectedCurrency: StateFlow<Currency?> = _selectedCurrency
    
    // UI state
    private val _uiState = MutableStateFlow<CurrencySelectionUiState>(CurrencySelectionUiState.Loading)
    val uiState: StateFlow<CurrencySelectionUiState> = _uiState
    
    // Sorted currencies (all)
    private val _sortedCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val sortedCurrencies: StateFlow<List<Currency>> = _sortedCurrencies
    
    // Top currencies (most used)
    private val _topCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val topCurrencies: StateFlow<List<Currency>> = _topCurrencies
    
    // Used currencies only
    private val _usedCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val usedCurrencies: StateFlow<List<Currency>> = _usedCurrencies
    
    // Filtered currencies based on search query
    private val _filteredCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val filteredCurrencies: StateFlow<List<Currency>> = _filteredCurrencies
    
    // Popular currencies (by default popularity)
    private val _popularCurrencies = MutableStateFlow<List<Currency>>(emptyList())
    val popularCurrencies: StateFlow<List<Currency>> = _popularCurrencies
    
    init {
        observeUserAndLoadCurrencies()
        observeSearchQuery()
    }
    
    /**
     * Observe current user and load currencies when user changes
     */
    private fun observeUserAndLoadCurrencies() {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                if (user != null) {
                    loadCurrenciesForUser(user.id)
                } else {
                    _uiState.value = CurrencySelectionUiState.Error("User not authenticated")
                }
            }
        }
    }
    
    /**
     * Load currencies for a specific user
     */
    private fun loadCurrenciesForUser(userId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = CurrencySelectionUiState.Loading
                
                // Collect sorted currencies
                currencySortingService.getSortedCurrencies(userId).collect { sortedCurrencies ->
                    _sortedCurrencies.value = sortedCurrencies
                    updateFilteredCurrencies()
                }
            } catch (e: Exception) {
                _uiState.value = CurrencySelectionUiState.Error("Failed to load currencies: ${e.message}")
            }
        }
        
        viewModelScope.launch {
            // Collect top currencies
            currencySortingService.getTopCurrencies(userId, 10).collect { topCurrencies ->
                _topCurrencies.value = topCurrencies
            }
        }
        
        viewModelScope.launch {
            // Collect used currencies
            currencySortingService.getUsedCurrencies(userId).collect { usedCurrencies ->
                _usedCurrencies.value = usedCurrencies
            }
        }
        
        viewModelScope.launch {
            // Load popular currencies (by default popularity)
            val allCurrencies = Currency.values().toList()
            _popularCurrencies.value = CurrencyListHelpers.getPopularCurrencies(allCurrencies, 5)
        }
    }
    
    /**
     * Observe search query changes and update filtered currencies
     */
    private fun observeSearchQuery() {
        viewModelScope.launch {
            combine(
                _searchQuery,
                _sortedCurrencies
            ) { query, currencies ->
                CurrencyListHelpers.filterCurrenciesByQuery(currencies, query)
            }.collect { filtered ->
                _filteredCurrencies.value = filtered
                _uiState.value = CurrencySelectionUiState.Success
            }
        }
    }
    
    /**
     * Update filtered currencies based on current search query
     */
    private fun updateFilteredCurrencies() {
        val query = _searchQuery.value
        val currencies = _sortedCurrencies.value
        _filteredCurrencies.value = CurrencyListHelpers.filterCurrenciesByQuery(currencies, query)
    }
    
    /**
     * Set search query for filtering currencies
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Select a currency
     */
    fun selectCurrency(currency: Currency) {
        _selectedCurrency.value = currency
    }
    
    /**
     * Clear search query
     */
    fun clearSearch() {
        _searchQuery.value = ""
    }
    
    /**
     * Track currency usage when a currency is selected
     */
    fun trackCurrencyUsage(currency: Currency) {
        viewModelScope.launch {
            val currentUser = authManager.getCurrentUser()
            if (currentUser != null) {
                currencySortingService.trackCurrencyUsage(currentUser.id, currency.code)
            }
        }
    }
    
    /**
     * Get formatted currencies for dropdown display
     */
    fun getFormattedCurrenciesForDropdown(currencies: List<Currency>): List<String> {
        return CurrencyListHelpers.formatCurrenciesForDropdown(currencies, showSymbol = true)
    }
    
    /**
     * Get formatted currencies for compact display
     */
    fun getFormattedCurrenciesForCompactDisplay(currencies: List<Currency>): List<String> {
        return CurrencyListHelpers.formatCurrenciesForCompactDisplay(currencies)
    }
    
    /**
     * Find currency by code
     */
    fun findCurrencyByCode(code: String): Currency? {
        return CurrencyListHelpers.findCurrencyByCode(_sortedCurrencies.value, code)
    }
    
    /**
     * Get currencies grouped by usage status
     */
    fun getCurrenciesGroupedByUsage(): Pair<List<Currency>, List<Currency>> {
        return CurrencyListHelpers.groupCurrenciesByUsage(
            _sortedCurrencies.value,
            _usedCurrencies.value
        )
    }
    
    /**
     * Refresh currencies data
     */
    fun refreshCurrencies() {
        val currentUser = authManager.getCurrentUser()
        if (currentUser != null) {
            loadCurrenciesForUser(currentUser.id)
        }
    }
    
    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = CurrencySelectionUiState.Loading
        _searchQuery.value = ""
        _selectedCurrency.value = null
    }
}

/**
 * UI state for currency selection
 */
sealed class CurrencySelectionUiState {
    object Loading : CurrencySelectionUiState()
    object Success : CurrencySelectionUiState()
    data class Error(val message: String) : CurrencySelectionUiState()
}
