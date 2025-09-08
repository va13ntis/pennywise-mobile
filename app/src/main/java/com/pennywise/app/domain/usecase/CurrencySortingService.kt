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
 * Service for sorting currencies by popularity based on user usage patterns
 */
class CurrencySortingService @Inject constructor(
    private val currencyUsageRepository: CurrencyUsageRepository,
    private val userRepository: UserRepository
) {
    
    // Cache for sorted currencies by user ID
    private val sortedCurrenciesCache = ConcurrentHashMap<Long, List<Currency>>()
    
    // Cache for currency usage data by user ID
    private val currencyUsageCache = ConcurrentHashMap<Long, List<com.pennywise.app.domain.model.CurrencyUsage>>()
    
    // Mutex for thread-safe cache operations
    private val cacheMutex = Mutex()
    
    // Cache expiration time (5 minutes)
    private val cacheExpirationTime = 5 * 60 * 1000L
    
    // Cache timestamps
    private val cacheTimestamps = ConcurrentHashMap<Long, Long>()
    
    // Coroutine scope for background operations
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    /**
     * Check if cache is valid for a user
     */
    private fun isCacheValid(userId: Long): Boolean {
        val timestamp = cacheTimestamps[userId] ?: return false
        return System.currentTimeMillis() - timestamp < cacheExpirationTime
    }
    
    /**
     * Invalidate cache for a specific user
     */
    suspend fun invalidateCache(userId: Long) {
        cacheMutex.withLock {
            sortedCurrenciesCache.remove(userId)
            currencyUsageCache.remove(userId)
            cacheTimestamps.remove(userId)
        }
    }
    
    /**
     * Invalidate cache for all users
     */
    suspend fun invalidateAllCache() {
        cacheMutex.withLock {
            sortedCurrenciesCache.clear()
            currencyUsageCache.clear()
            cacheTimestamps.clear()
        }
    }
    
    /**
     * Get currencies sorted by user usage patterns with reactive updates
     * @param userId The user ID to get sorted currencies for
     * @return Flow of sorted currencies list that updates when usage patterns change
     */
    fun getSortedCurrencies(userId: Long): Flow<List<Currency>> {
        return currencyUsageRepository.getUserCurrenciesSortedByUsage(userId)
            .onStart {
                // Optional: Emit cached data immediately if available
                if (isCacheValid(userId)) {
                    sortedCurrenciesCache[userId]?.let { cachedCurrencies ->
                        // Note: We can't emit from onStart in a regular Flow, but this is where
                        // we could log or perform other startup operations
                    }
                }
            }
            .map { userCurrencyUsage ->
                // Get user's default currency - for now we'll use a fallback approach
                // In a production app, you might want to add a Flow-based method to UserRepository
                val defaultCurrency = getDefaultCurrencyForUser(userId)
                
                // Create a set of used currencies
                val usedCurrencyCodes = userCurrencyUsage.map { it.currency }.toSet()
                
                // Ensure default currency is included
                val allUsedCodes = usedCurrencyCodes + defaultCurrency
                
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
                
                // Update cache asynchronously (non-blocking)
                backgroundScope.launch {
                    updateCacheAsync(userId, sortedCurrencies, userCurrencyUsage)
                }
                
                sortedCurrencies
            }
            .catch { exception ->
                // Handle errors gracefully - emit empty list or cached data if available
                val cachedCurrencies = sortedCurrenciesCache[userId] ?: emptyList()
                // Log the error (in a real app, you'd use proper logging)
                println("Error in getSortedCurrencies for user $userId: ${exception.message}")
                emit(cachedCurrencies)
            }
            .onCompletion { cause ->
                // Optional: Log completion or perform cleanup
                if (cause != null) {
                    println("Flow completed with exception for user $userId: ${cause.message}")
                }
            }
    }
    
    /**
     * Get currencies sorted by user usage patterns (suspend version with caching)
     * @param userId The user ID to get sorted currencies for
     * @return List of sorted currencies
     */
    suspend fun getSortedCurrenciesSuspend(userId: Long): List<Currency> {
        // Check cache first
        if (isCacheValid(userId)) {
            sortedCurrenciesCache[userId]?.let { return it }
        }
        
        // Cache miss or expired - fetch from repository
        val userCurrencyUsage = currencyUsageRepository.getUserCurrenciesSortedByUsage(userId).first()
        
        // Get user's default currency
        val user = userRepository.getUserById(userId)
        val defaultCurrency = user?.defaultCurrency ?: "USD"
        
        // Create a set of used currencies
        val usedCurrencyCodes = userCurrencyUsage.map { it.currency }.toSet()
        
        // Ensure default currency is included
        val allUsedCodes = usedCurrencyCodes + defaultCurrency
        
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
        
        // Update cache
        cacheMutex.withLock {
            sortedCurrenciesCache[userId] = sortedCurrencies
            currencyUsageCache[userId] = userCurrencyUsage
            cacheTimestamps[userId] = System.currentTimeMillis()
        }
        
        return sortedCurrencies
    }
    
    /**
     * Get top N currencies for a user based on usage
     * @param userId The user ID
     * @param limit Maximum number of currencies to return
     * @return Flow of top currencies list
     */
    fun getTopCurrencies(userId: Long, limit: Int = 10): Flow<List<Currency>> {
        return getSortedCurrencies(userId).map { sortedCurrencies ->
            sortedCurrencies.take(limit)
        }
    }
    
    /**
     * Get currencies that the user has actually used
     * @param userId The user ID
     * @return Flow of used currencies list
     */
    fun getUsedCurrencies(userId: Long): Flow<List<Currency>> {
        return currencyUsageRepository.getUserCurrenciesSortedByUsage(userId).map { userCurrencyUsage ->
            userCurrencyUsage.mapNotNull { usage ->
                Currency.fromCode(usage.currency)
            }
        }
    }
    
    /**
     * Track currency usage and invalidate cache when usage changes
     * This method should be called whenever a user uses a currency
     * @param userId The user ID
     * @param currencyCode The currency code that was used
     */
    suspend fun trackCurrencyUsage(userId: Long, currencyCode: String) {
        // Increment usage in repository
        currencyUsageRepository.incrementCurrencyUsage(userId, currencyCode)
        
        // Invalidate cache for this user since usage pattern changed
        invalidateCache(userId)
    }
    
    /**
     * Get cache statistics for debugging/monitoring
     * @return Map of cache statistics
     */
    fun getCacheStats(): Map<String, Any> {
        return mapOf(
            "sortedCurrenciesCacheSize" to sortedCurrenciesCache.size,
            "currencyUsageCacheSize" to currencyUsageCache.size,
            "cacheTimestampsSize" to cacheTimestamps.size,
            "cacheExpirationTimeMs" to cacheExpirationTime
        )
    }
    
    /**
     * Get default currency for a user with fallback
     * @param userId The user ID
     * @return Default currency code
     */
    private fun getDefaultCurrencyForUser(userId: Long): String {
        // Try to get from cache first
        val cachedUsage = currencyUsageCache[userId]
        if (cachedUsage != null && isCacheValid(userId)) {
            // If we have cached data, we can make a reasonable assumption
            // In a real implementation, you might want to store user preferences in a Flow
            return "USD" // Default fallback
        }
        
        // For now, return a default currency
        // In a production app, you might want to:
        // 1. Add a Flow-based method to UserRepository to get user data reactively
        // 2. Store user preferences in a separate reactive data source
        // 3. Use a combination of flows to get both usage and user data
        return "USD"
    }
    
    /**
     * Update cache asynchronously without blocking the Flow
     * @param userId The user ID
     * @param sortedCurrencies The sorted currencies to cache
     * @param userCurrencyUsage The currency usage data to cache
     */
    private suspend fun updateCacheAsync(
        userId: Long, 
        sortedCurrencies: List<Currency>, 
        userCurrencyUsage: List<com.pennywise.app.domain.model.CurrencyUsage>
    ) {
        // Update cache in a non-blocking way
        try {
            cacheMutex.withLock {
                sortedCurrenciesCache[userId] = sortedCurrencies
                currencyUsageCache[userId] = userCurrencyUsage
                cacheTimestamps[userId] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            // Log error but don't fail the flow
            println("Error updating cache for user $userId: ${e.message}")
        }
    }
    
    /**
     * Get currencies sorted by user usage patterns with enhanced reactive updates
     * This method provides a more sophisticated Flow that combines multiple data sources
     * @param userId The user ID to get sorted currencies for
     * @return Flow of sorted currencies list that updates when any underlying data changes
     */
    fun getSortedCurrenciesReactive(userId: Long): Flow<List<Currency>> {
        return combine(
            currencyUsageRepository.getUserCurrenciesSortedByUsage(userId),
            // For now, we'll use a static flow for user data since UserRepository doesn't have Flow methods
            // In a production app, you'd want to add Flow-based methods to UserRepository
            flowOf("USD") // This represents the user's default currency
        ) { userCurrencyUsage, defaultCurrency ->
            // Create a set of used currencies
            val usedCurrencyCodes = userCurrencyUsage.map { it.currency }.toSet()
            
            // Ensure default currency is included
            val allUsedCodes = usedCurrencyCodes + defaultCurrency
            
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
                updateCacheAsync(userId, sortedCurrencies, userCurrencyUsage)
            }
            
            sortedCurrencies
        }
        .catch { exception ->
            // Handle errors gracefully - emit cached data if available
            val cachedCurrencies = sortedCurrenciesCache[userId] ?: emptyList()
            println("Error in getSortedCurrenciesReactive for user $userId: ${exception.message}")
            emit(cachedCurrencies)
        }
    }
}
