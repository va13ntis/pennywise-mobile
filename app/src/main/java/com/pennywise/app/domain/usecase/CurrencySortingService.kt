package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * Service for sorting currencies by popularity based on usage patterns
 */
class CurrencySortingService @Inject constructor(
    private val currencyUsageRepository: CurrencyUsageRepository,
    private val userRepository: UserRepository
) {
    
    // Cache for sorted currencies
    private var sortedCurrenciesCache: List<Currency>? = null
    
    // Cache for currency usage data
    private var currencyUsageCache: List<com.pennywise.app.domain.model.CurrencyUsage>? = null
    
    // Mutex for thread-safe cache operations
    private val cacheMutex = Mutex()
    
    // Cache expiration time (5 minutes)
    private val cacheExpirationTime = 5 * 60 * 1000L
    
    // Cache timestamp
    private var cacheTimestamp: Long? = null
    
    // Coroutine scope for background operations
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Check if cache is valid
     */
    private fun isCacheValid(): Boolean {
        val timestamp = cacheTimestamp ?: return false
        return System.currentTimeMillis() - timestamp < cacheExpirationTime
    }
    
    /**
     * Invalidate cache
     */
    suspend fun invalidateCache() {
        cacheMutex.withLock {
            sortedCurrenciesCache = null
            currencyUsageCache = null
            cacheTimestamp = null
        }
    }
    
    /**
     * Get currencies sorted by usage patterns with reactive updates
     * @return Flow of sorted currencies list that updates when usage patterns change
     */
    fun getSortedCurrencies(): Flow<List<Currency>> {
        return try {
            currencyUsageRepository.getCurrenciesSortedByUsage()
                .onStart {
                    // Optional: Emit cached data immediately if available
                    if (isCacheValid()) {
                        sortedCurrenciesCache?.let { _ ->
                            // Note: We can't emit from onStart in a regular Flow, but this is where
                            // we could log or perform other startup operations
                        }
                    }
                }
                .map { userCurrencyUsage ->
                // Create a set of used currencies
                val usedCurrencyCodes = userCurrencyUsage.map { it.currency }.toSet()
                
                // Get all currencies
                val allCurrencies = Currency.values().toList()
                
                // Sort currencies: first by usage (if used), then by default enum popularity
                val sortedCurrencies = allCurrencies.sortedWith(compareBy(
                    // First sort by whether it's in the used set (used currencies first)
                    { !(it.code in usedCurrencyCodes) },
                    // Then sort used currencies by their usage order
                    { currency -> 
                        if (currency.code in usedCurrencyCodes) {
                            userCurrencyUsage.indexOfFirst { it.currency == currency.code }
                        } else {
                            Int.MAX_VALUE // Unused currencies go to the end
                        }
                    },
                    // Finally sort by the default popularity for currencies not in the used set
                    { it.popularity }
                ))
                
                // Update cache asynchronously (non-blocking)
                backgroundScope.launch {
                    updateCacheAsync(sortedCurrencies, userCurrencyUsage)
                }
                
                sortedCurrencies
            }
            .catch { exception ->
                // Handle errors gracefully - emit empty list or cached data if available
                val cachedCurrencies = sortedCurrenciesCache ?: emptyList()
                emit(cachedCurrencies)
            }
            .onCompletion { cause ->
                // Optional: Log completion or perform cleanup
                if (cause != null) {
                }
            }
        } catch (e: Exception) {
            // Handle exceptions thrown during Flow creation
            flowOf(sortedCurrenciesCache ?: emptyList())
        }
    }
    
    /**
     * Get currencies sorted by usage patterns (suspend version with caching)
     * @return List of sorted currencies
     */
    suspend fun getSortedCurrenciesSuspend(): List<Currency> {
        return try {
            // Check cache first
            if (isCacheValid()) {
                sortedCurrenciesCache?.let { return it }
            }
            
            // Cache miss or expired - fetch from repository
            val userCurrencyUsage = currencyUsageRepository.getCurrenciesSortedByUsage().first()
            
            // Get user's default currency (if any)
            val defaultCurrency = getDefaultCurrency()
            
            // Get all currencies
            val allCurrencies = Currency.values().toList()
            
            // Sort currencies with proper priority: used currencies by usage count, then default currency, then by popularity
            val sortedCurrencies = allCurrencies.sortedWith(compareBy(
                // First sort by whether it's actually used (has usage count > 0)
                { currency -> 
                    val usage = userCurrencyUsage.find { it.currency == currency.code }
                    if (usage != null && usage.usageCount > 0) 0 else 1
                },
                // Then sort used currencies by usage count (descending - highest usage first)
                { currency -> 
                    val usage = userCurrencyUsage.find { it.currency == currency.code }
                    if (usage != null && usage.usageCount > 0) {
                        -usage.usageCount // Negative for descending order
                    } else {
                        Int.MAX_VALUE // Unused currencies go to the end
                    }
                },
                // Then sort by whether it's the user's default currency (default currency gets priority among unused)
                { currency -> 
                    if (currency.code == defaultCurrency) 0 else 1
                },
                // Finally sort by popularity for currencies in the same category
                { currency -> 
                    currency.popularity
                }
            ))
            
            // Update cache
            cacheMutex.withLock {
                sortedCurrenciesCache = sortedCurrencies
                currencyUsageCache = userCurrencyUsage
                cacheTimestamp = System.currentTimeMillis()
            }
            
            sortedCurrencies
        } catch (e: Exception) {
            // Handle errors gracefully - return empty list or cached data if available
            sortedCurrenciesCache ?: emptyList()
        }
    }
    
    /**
     * Get top N currencies based on usage
     * @param limit Maximum number of currencies to return
     * @return Flow of top currencies list
     */
    fun getTopCurrencies(limit: Int = 10): Flow<List<Currency>> {
        return getSortedCurrencies().map { sortedCurrencies ->
            sortedCurrencies.take(limit)
        }
    }
    
    /**
     * Get currencies that have actually been used
     * @return Flow of used currencies list
     */
    fun getUsedCurrencies(): Flow<List<Currency>> {
        return currencyUsageRepository.getCurrenciesSortedByUsage().map { userCurrencyUsage ->
            userCurrencyUsage.mapNotNull { usage ->
                Currency.fromCode(usage.currency)
            }
        }
    }
    
    /**
     * Track currency usage and invalidate cache when usage changes
     * This method should be called whenever a currency is used
     * @param currencyCode The currency code that was used
     */
    suspend fun trackCurrencyUsage(currencyCode: String) {
        try {
            // Increment usage in repository
            currencyUsageRepository.incrementCurrencyUsage(currencyCode)
            
            // Invalidate cache since usage pattern changed
            invalidateCache()
        } catch (e: Exception) {
            // Log error but don't fail the operation
        }
    }
    
    /**
     * Get currencies sorted by usage patterns with enhanced reactive updates
     * This method provides a more sophisticated Flow that combines multiple data sources
     * @return Flow of sorted currencies list that updates when any underlying data changes
     */
    fun getSortedCurrenciesReactive(): Flow<List<Currency>> {
        return combine(
            currencyUsageRepository.getCurrenciesSortedByUsage(),
            // For now, we'll use a static flow for user data since UserRepository doesn't have Flow methods
            // In a production app, you'd want to add Flow-based methods to UserRepository
            flowOf(null as String?) // This represents the user's default currency (null if no default)
        ) { userCurrencyUsage, defaultCurrency ->
            // Create a set of used currencies
            val usedCurrencyCodes = userCurrencyUsage.map { it.currency }.toSet()
            
            // Include default currency if it exists
            val allUsedCodes = usedCurrencyCodes + if (defaultCurrency != null) setOf(defaultCurrency) else emptySet()
            
            // Get all currencies
            val allCurrencies = Currency.values().toList()
            
            // Sort currencies: first by usage (if used), then by default enum popularity
            val sortedCurrencies = allCurrencies.sortedWith(compareBy(
                // First sort by whether it's in the used set (used currencies first)
                { !(it.code in allUsedCodes) },
                // Then sort used currencies by their usage order
                { currency -> 
                    if (currency.code in allUsedCodes) {
                        userCurrencyUsage.indexOfFirst { it.currency == currency.code }
                    } else {
                        Int.MAX_VALUE // Unused currencies go to the end
                    }
                },
                // Finally sort by the default popularity for currencies not in the used set
                { it.popularity }
            ))
            
            // Update cache asynchronously
            backgroundScope.launch {
                updateCacheAsync(sortedCurrencies, userCurrencyUsage)
            }
            
            sortedCurrencies
        }
        .catch { exception ->
            // Handle errors gracefully - emit cached data if available
            val cachedCurrencies = sortedCurrenciesCache ?: emptyList()
            emit(cachedCurrencies)
        }
    }
    
    /**
     * Get cache statistics for debugging/monitoring
     * @return Map of cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "sortedCurrenciesCacheSize" to (if (sortedCurrenciesCache != null) 1 else 0),
            "currencyUsageCacheSize" to (if (currencyUsageCache != null) 1 else 0),
            "cacheTimestampSet" to (cacheTimestamp != null),
            "cacheExpirationTimeMs" to cacheExpirationTime
        )
    }
    
    /**
     * Get default currency with fallback
     * @return Default currency code or null if no default is set
     */
    private suspend fun getDefaultCurrency(): String? {
        // Try to get user's actual default currency from repository
        return try {
            val user = userRepository.getUser()
            user?.defaultCurrency
        } catch (e: Exception) {
            // If we can't get user data, return null (no default currency)
            null
        }
    }
    
    /**
     * Update cache asynchronously without blocking the Flow
     * @param sortedCurrencies The sorted currencies to cache
     * @param userCurrencyUsage The currency usage data to cache
     */
    private suspend fun updateCacheAsync(
        sortedCurrencies: List<Currency>, 
        userCurrencyUsage: List<com.pennywise.app.domain.model.CurrencyUsage>
    ) {
        // Update cache in a non-blocking way
        try {
            cacheMutex.withLock {
                sortedCurrenciesCache = sortedCurrencies
                currencyUsageCache = userCurrencyUsage
                cacheTimestamp = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Log error but don't fail the flow
        }
    }
}