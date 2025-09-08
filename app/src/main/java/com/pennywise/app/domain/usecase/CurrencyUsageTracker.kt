package com.pennywise.app.domain.usecase

import com.pennywise.app.domain.repository.CurrencyUsageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject

/**
 * Service to track currency usage when transactions are created or updated
 */
class CurrencyUsageTracker @Inject constructor(
    private val currencyUsageRepository: CurrencyUsageRepository,
    private val ioDispatcher: CoroutineDispatcher
) {
    
    /**
     * Track currency usage for a specific user and currency
     * This method should be called whenever a transaction is created or updated with a currency
     */
    suspend fun trackCurrencyUsage(userId: Long, currencyCode: String) {
        withContext(ioDispatcher) {
            try {
                // Use the optimized method from the repository
                currencyUsageRepository.incrementCurrencyUsage(userId, currencyCode)
            } catch (e: Exception) {
                // Log error but don't fail the transaction creation
                // In a production app, you might want to use a proper logging framework
                println("Error tracking currency usage: ${e.message}")
            }
        }
    }
    
    /**
     * Get currencies for a user sorted by popularity (usage count)
     */
    suspend fun getUserCurrenciesByPopularity(userId: Long): List<String> {
        return withContext(ioDispatcher) {
            try {
                currencyUsageRepository.getCurrencyUsageByUser(userId)
                    .first()
                    .map { it.currency }
            } catch (e: Exception) {
                println("Error getting user currencies by popularity: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * Get top currencies for a user with a limit
     */
    suspend fun getTopCurrenciesForUser(userId: Long, limit: Int = 10): List<String> {
        return withContext(ioDispatcher) {
            try {
                currencyUsageRepository.getTopCurrenciesByUser(userId, limit)
                    .first()
                    .map { it.currency }
            } catch (e: Exception) {
                println("Error getting top currencies for user: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * Get currency usage statistics for a user
     */
    suspend fun getCurrencyUsageStats(userId: Long): CurrencyUsageStats {
        return withContext(ioDispatcher) {
            try {
                val usageList = currencyUsageRepository.getCurrencyUsageByUser(userId).first()
                val totalUsage = usageList.sumOf { it.usageCount }
                val mostUsedCurrency = usageList.maxByOrNull { it.usageCount }?.currency
                val leastUsedCurrency = usageList.minByOrNull { it.usageCount }?.currency
                val uniqueCurrencies = usageList.size
                
                CurrencyUsageStats(
                    totalUsage = totalUsage,
                    uniqueCurrencies = uniqueCurrencies,
                    mostUsedCurrency = mostUsedCurrency,
                    leastUsedCurrency = leastUsedCurrency,
                    currencies = usageList.map { it.currency }
                )
            } catch (e: Exception) {
                println("Error getting currency usage stats: ${e.message}")
                CurrencyUsageStats()
            }
        }
    }
    
    /**
     * Get most used currencies for a user
     */
    suspend fun getMostUsedCurrencies(userId: Long, limit: Int = 5): List<CurrencyUsageInfo> {
        return withContext(ioDispatcher) {
            try {
                currencyUsageRepository.getTopCurrenciesByUser(userId, limit)
                    .first()
                    .map { 
                        CurrencyUsageInfo(
                            currency = it.currency,
                            usageCount = it.usageCount,
                            lastUsed = it.lastUsed,
                            percentage = 0.0 // Will be calculated below
                        )
                    }
                    .let { currencies ->
                        val totalUsage = currencies.sumOf { it.usageCount }
                        currencies.map { currency ->
                            currency.copy(
                                percentage = if (totalUsage > 0) (currency.usageCount.toDouble() / totalUsage) * 100 else 0.0
                            )
                        }
                    }
            } catch (e: Exception) {
                println("Error getting most used currencies: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * Get least used currencies for a user
     */
    suspend fun getLeastUsedCurrencies(userId: Long, limit: Int = 5): List<CurrencyUsageInfo> {
        return withContext(ioDispatcher) {
            try {
                val allCurrencies = currencyUsageRepository.getCurrencyUsageByUser(userId).first()
                val totalUsage = allCurrencies.sumOf { it.usageCount }
                
                allCurrencies
                    .sortedBy { it.usageCount }
                    .take(limit)
                    .map { 
                        CurrencyUsageInfo(
                            currency = it.currency,
                            usageCount = it.usageCount,
                            lastUsed = it.lastUsed,
                            percentage = if (totalUsage > 0) (it.usageCount.toDouble() / totalUsage) * 100 else 0.0
                        )
                    }
            } catch (e: Exception) {
                println("Error getting least used currencies: ${e.message}")
                emptyList()
            }
        }
    }
    
    /**
     * Get currency usage trend (recent vs historical usage)
     */
    suspend fun getCurrencyUsageTrend(userId: Long, daysBack: Int = 30): CurrencyUsageTrend {
        return withContext(ioDispatcher) {
            try {
                val allCurrencies = currencyUsageRepository.getCurrencyUsageByUser(userId).first()
                val cutoffDate = Date(System.currentTimeMillis() - (daysBack * 24 * 60 * 60 * 1000L))
                
                val recentCurrencies = allCurrencies.filter { it.lastUsed.after(cutoffDate) }
                val historicalCurrencies = allCurrencies.filter { it.lastUsed.before(cutoffDate) }
                
                CurrencyUsageTrend(
                    recentCurrencies = recentCurrencies.map { it.currency },
                    historicalCurrencies = historicalCurrencies.map { it.currency },
                    totalCurrencies = allCurrencies.size,
                    activeCurrencies = recentCurrencies.size
                )
            } catch (e: Exception) {
                println("Error getting currency usage trend: ${e.message}")
                CurrencyUsageTrend()
            }
        }
    }
    
    /**
     * Get currency usage summary for display in UI
     */
    suspend fun getCurrencyUsageSummary(userId: Long): CurrencyUsageSummary {
        return withContext(ioDispatcher) {
            try {
                val stats = getCurrencyUsageStats(userId)
                val mostUsed = getMostUsedCurrencies(userId, 3)
                val trend = getCurrencyUsageTrend(userId)
                
                CurrencyUsageSummary(
                    totalTransactions = stats.totalUsage,
                    uniqueCurrencies = stats.uniqueCurrencies,
                    primaryCurrency = stats.mostUsedCurrency,
                    topCurrencies = mostUsed,
                    recentActivity = trend.activeCurrencies > 0,
                    usageTrend = if (trend.activeCurrencies > 0) "Active" else "Inactive"
                )
            } catch (e: Exception) {
                println("Error getting currency usage summary: ${e.message}")
                CurrencyUsageSummary()
            }
        }
    }
}

/**
 * Data class representing currency usage statistics
 */
data class CurrencyUsageStats(
    val totalUsage: Int = 0,
    val uniqueCurrencies: Int = 0,
    val mostUsedCurrency: String? = null,
    val leastUsedCurrency: String? = null,
    val currencies: List<String> = emptyList()
)

/**
 * Data class representing detailed currency usage information
 */
data class CurrencyUsageInfo(
    val currency: String,
    val usageCount: Int,
    val lastUsed: Date,
    val percentage: Double
)

/**
 * Data class representing currency usage trend
 */
data class CurrencyUsageTrend(
    val recentCurrencies: List<String> = emptyList(),
    val historicalCurrencies: List<String> = emptyList(),
    val totalCurrencies: Int = 0,
    val activeCurrencies: Int = 0
)

/**
 * Data class representing currency usage summary for UI display
 */
data class CurrencyUsageSummary(
    val totalTransactions: Int = 0,
    val uniqueCurrencies: Int = 0,
    val primaryCurrency: String? = null,
    val topCurrencies: List<CurrencyUsageInfo> = emptyList(),
    val recentActivity: Boolean = false,
    val usageTrend: String = "Unknown"
)
