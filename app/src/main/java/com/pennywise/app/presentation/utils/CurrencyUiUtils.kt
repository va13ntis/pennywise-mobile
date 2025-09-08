package com.pennywise.app.presentation.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.usecase.CurrencySortingService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Utility class for integrating CurrencySortingService with UI components
 */
class CurrencyUiUtils {
    
    /**
     * Extension function to collect sorted currencies with lifecycle awareness
     * @param currencySortingService The service to get sorted currencies from
     * @param userId The user ID
     * @param lifecycleOwner The lifecycle owner (Activity, Fragment, etc.)
     * @param lifecycleState The lifecycle state to collect in (default: STARTED)
     * @return StateFlow of sorted currencies that automatically stops collecting when lifecycle stops
     */
    fun collectSortedCurrencies(
        currencySortingService: CurrencySortingService,
        userId: Long,
        lifecycleOwner: LifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED
    ): StateFlow<List<Currency>> {
        val stateFlow = MutableStateFlow<List<Currency>>(emptyList())
        
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(lifecycleState) {
                currencySortingService.getSortedCurrencies(userId).collect { sortedCurrencies ->
                    stateFlow.value = sortedCurrencies
                }
            }
        }
        
        return stateFlow.asStateFlow()
    }
    
    /**
     * Extension function to collect top N currencies with lifecycle awareness
     * @param currencySortingService The service to get sorted currencies from
     * @param userId The user ID
     * @param limit Maximum number of currencies to return
     * @param lifecycleOwner The lifecycle owner
     * @param lifecycleState The lifecycle state to collect in
     * @return StateFlow of top currencies
     */
    fun collectTopCurrencies(
        currencySortingService: CurrencySortingService,
        userId: Long,
        limit: Int = 10,
        lifecycleOwner: LifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED
    ): StateFlow<List<Currency>> {
        val stateFlow = MutableStateFlow<List<Currency>>(emptyList())
        
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(lifecycleState) {
                currencySortingService.getTopCurrencies(userId, limit).collect { topCurrencies ->
                    stateFlow.value = topCurrencies
                }
            }
        }
        
        return stateFlow.asStateFlow()
    }
    
    /**
     * Extension function to collect used currencies with lifecycle awareness
     * @param currencySortingService The service to get sorted currencies from
     * @param userId The user ID
     * @param lifecycleOwner The lifecycle owner
     * @param lifecycleState The lifecycle state to collect in
     * @return StateFlow of used currencies
     */
    fun collectUsedCurrencies(
        currencySortingService: CurrencySortingService,
        userId: Long,
        lifecycleOwner: LifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED
    ): StateFlow<List<Currency>> {
        val stateFlow = MutableStateFlow<List<Currency>>(emptyList())
        
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(lifecycleState) {
                currencySortingService.getUsedCurrencies(userId).collect { usedCurrencies ->
                    stateFlow.value = usedCurrencies
                }
            }
        }
        
        return stateFlow.asStateFlow()
    }
    
    /**
     * Extension function to collect sorted currencies with enhanced reactive updates
     * @param currencySortingService The service to get sorted currencies from
     * @param userId The user ID
     * @param lifecycleOwner The lifecycle owner
     * @param lifecycleState The lifecycle state to collect in
     * @return StateFlow of sorted currencies with enhanced reactivity
     */
    fun collectSortedCurrenciesReactive(
        currencySortingService: CurrencySortingService,
        userId: Long,
        lifecycleOwner: LifecycleOwner,
        lifecycleState: Lifecycle.State = Lifecycle.State.STARTED
    ): StateFlow<List<Currency>> {
        val stateFlow = MutableStateFlow<List<Currency>>(emptyList())
        
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(lifecycleState) {
                currencySortingService.getSortedCurrenciesReactive(userId).collect { sortedCurrencies ->
                    stateFlow.value = sortedCurrencies
                }
            }
        }
        
        return stateFlow.asStateFlow()
    }
}

/**
 * Extension functions for ViewModels to easily integrate with CurrencySortingService
 */
object CurrencyViewModelExtensions {
    
    /**
     * Create a StateFlow that collects sorted currencies for a user
     * This is a convenience method for ViewModels
     */
    fun createSortedCurrenciesStateFlow(
        currencySortingService: CurrencySortingService,
        userId: Long
    ): Flow<List<Currency>> {
        return currencySortingService.getSortedCurrencies(userId)
    }
    
    /**
     * Create a StateFlow that collects top N currencies for a user
     */
    fun createTopCurrenciesStateFlow(
        currencySortingService: CurrencySortingService,
        userId: Long,
        limit: Int = 10
    ): Flow<List<Currency>> {
        return currencySortingService.getTopCurrencies(userId, limit)
    }
    
    /**
     * Create a StateFlow that collects used currencies for a user
     */
    fun createUsedCurrenciesStateFlow(
        currencySortingService: CurrencySortingService,
        userId: Long
    ): Flow<List<Currency>> {
        return currencySortingService.getUsedCurrencies(userId)
    }
    
    /**
     * Create a StateFlow that collects sorted currencies with enhanced reactivity
     */
    fun createSortedCurrenciesReactiveStateFlow(
        currencySortingService: CurrencySortingService,
        userId: Long
    ): Flow<List<Currency>> {
        return currencySortingService.getSortedCurrenciesReactive(userId)
    }
}

/**
 * Helper functions for filtering and customizing currency lists for specific UI needs
 */
object CurrencyListHelpers {
    
    /**
     * Filter currencies by search query
     * @param currencies List of currencies to filter
     * @param query Search query (case-insensitive)
     * @return Filtered list of currencies
     */
    fun filterCurrenciesByQuery(currencies: List<Currency>, query: String): List<Currency> {
        if (query.isBlank()) return currencies
        
        val lowercaseQuery = query.lowercase()
        return currencies.filter { currency ->
            currency.code.lowercase().contains(lowercaseQuery) ||
            currency.name.lowercase().contains(lowercaseQuery) ||
            currency.symbol.lowercase().contains(lowercaseQuery)
        }
    }
    
    /**
     * Get currencies grouped by usage status
     * @param sortedCurrencies List of sorted currencies
     * @param usedCurrencies List of currencies that have been used
     * @return Pair of (used currencies, unused currencies)
     */
    fun groupCurrenciesByUsage(
        sortedCurrencies: List<Currency>,
        usedCurrencies: List<Currency>
    ): Pair<List<Currency>, List<Currency>> {
        val usedSet = usedCurrencies.map { it.code }.toSet()
        val used = sortedCurrencies.filter { it.code in usedSet }
        val unused = sortedCurrencies.filter { it.code !in usedSet }
        return Pair(used, unused)
    }
    
    /**
     * Get currencies for dropdown/selection UI with proper formatting
     * @param currencies List of currencies
     * @param showSymbol Whether to include currency symbol in display
     * @return List of formatted currency strings for UI display
     */
    fun formatCurrenciesForDropdown(
        currencies: List<Currency>,
        showSymbol: Boolean = true
    ): List<String> {
        return currencies.map { currency ->
            if (showSymbol) {
                "${currency.symbol} ${currency.code} - ${currency.name}"
            } else {
                "${currency.code} - ${currency.name}"
            }
        }
    }
    
    /**
     * Get currencies for compact display (e.g., in chips or tags)
     * @param currencies List of currencies
     * @return List of compact currency strings
     */
    fun formatCurrenciesForCompactDisplay(currencies: List<Currency>): List<String> {
        return currencies.map { currency ->
            "${currency.symbol} ${currency.code}"
        }
    }
    
    /**
     * Find currency by code in a list
     * @param currencies List of currencies
     * @param code Currency code to find
     * @return Currency if found, null otherwise
     */
    fun findCurrencyByCode(currencies: List<Currency>, code: String): Currency? {
        return currencies.find { it.code.equals(code, ignoreCase = true) }
    }
    
    /**
     * Get popular currencies (top N by default popularity)
     * @param currencies List of all currencies
     * @param limit Number of popular currencies to return
     * @return List of most popular currencies
     */
    fun getPopularCurrencies(currencies: List<Currency>, limit: Int = 5): List<Currency> {
        return currencies.sortedBy { it.popularity }.take(limit)
    }
}
