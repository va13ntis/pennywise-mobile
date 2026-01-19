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
     * Track currency usage for a specific currency
     * This method should be called whenever a transaction is created or updated with a currency
     */
    suspend fun trackCurrencyUsage(currencyCode: String) {
        withContext(ioDispatcher) {
            try {
                currencyUsageRepository.incrementCurrencyUsage(currencyCode)
            } catch (e: Exception) {
                // Log error but don't fail the transaction creation
                // In a production app, you might want to use a proper logging framework
            }
        }
    }
    
    /**
     * Get currencies sorted by popularity (usage count)
     */
    suspend fun getCurrenciesByPopularity(): List<String> {
        return withContext(ioDispatcher) {
            try {
                currencyUsageRepository.getCurrencyUsage()
                    .first()
                    .map { it.currency }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Get top currencies with a limit
     */
    suspend fun getTopCurrencies(limit: Int = 10): List<String> {
        return withContext(ioDispatcher) {
            try {
                currencyUsageRepository.getTopCurrencies(limit)
                    .first()
                    .map { it.currency }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    /**
     * Get currency usage statistics
     */
    suspend fun getCurrencyUsageStats(): CurrencyUsageStats {
        return withContext(ioDispatcher) {
            try {
                val usageList = currencyUsageRepository.getCurrencyUsage().first()
                val totalUsage = usageList.sumOf { it.usageCount.toLong() }
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
                CurrencyUsageStats()
            }
        }
    }
    
    /**
     * Get currency usage trend (recent vs historical usage)
     */
    suspend fun getCurrencyUsageTrend(daysBack: Int = 30): CurrencyUsageTrend {
        return withContext(ioDispatcher) {
            try {
                val allCurrencies = currencyUsageRepository.getCurrencyUsage().first()
                val actualDaysBack = maxOf(0, daysBack) // Handle negative days by treating as 0
                
                val recentCurrencies: List<com.pennywise.app.domain.model.CurrencyUsage>
                val historicalCurrencies: List<com.pennywise.app.domain.model.CurrencyUsage>
                
                if (actualDaysBack == 0) {
                    // When daysBack is 0, all currencies are considered recent
                    recentCurrencies = allCurrencies
                    historicalCurrencies = emptyList()
                } else {
                    val cutoffDate = Date(System.currentTimeMillis() - (actualDaysBack * 24 * 60 * 60 * 1000L))
                    recentCurrencies = allCurrencies.filter { it.lastUsed.after(cutoffDate) || it.lastUsed == cutoffDate }
                    historicalCurrencies = allCurrencies.filter { it.lastUsed.before(cutoffDate) }
                }
                
                CurrencyUsageTrend(
                    recentCurrencies = recentCurrencies.map { it.currency },
                    historicalCurrencies = historicalCurrencies.map { it.currency },
                    totalCurrencies = allCurrencies.size,
                    activeCurrencies = recentCurrencies.size
                )
            } catch (e: Exception) {
                CurrencyUsageTrend()
            }
        }
    }
    
    /**
     * Get currency usage summary for display in UI
     */
    suspend fun getCurrencyUsageSummary(): CurrencyUsageSummary {
        return withContext(ioDispatcher) {
            try {
                // Directly call repository methods to handle exceptions properly
                val usageList = currencyUsageRepository.getCurrencyUsage().first()
                val topCurrencies = currencyUsageRepository.getTopCurrencies(3).first()
                
                val totalUsage = usageList.sumOf { it.usageCount.toLong() }
                val mostUsedCurrency = usageList.maxByOrNull { it.usageCount }?.currency
                val uniqueCurrencies = usageList.size
                
                // Calculate trend
                val cutoffDate = Date(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L))
                val recentCurrencies = usageList.filter { it.lastUsed.after(cutoffDate) || it.lastUsed == cutoffDate }
                val activeCurrencies = recentCurrencies.size
                
                val mostUsedInfo = topCurrencies.map { 
                    CurrencyUsageInfo(
                        currency = it.currency,
                        usageCount = it.usageCount,
                        lastUsed = it.lastUsed,
                        percentage = if (totalUsage > 0) (it.usageCount.toDouble() / totalUsage) * 100 else 0.0
                    )
                }
                
                CurrencyUsageSummary(
                    totalTransactions = totalUsage,
                    uniqueCurrencies = uniqueCurrencies,
                    primaryCurrency = mostUsedCurrency,
                    topCurrencies = mostUsedInfo,
                    recentActivity = activeCurrencies > 0,
                    usageTrend = if (activeCurrencies > 0) "Active" else "Inactive"
                )
            } catch (e: Exception) {
                CurrencyUsageSummary()
            }
        }
    }
    
    /**
     * Get most used currencies
     */
    suspend fun getMostUsedCurrencies(limit: Int = 5): List<CurrencyUsageInfo> {
        return withContext(ioDispatcher) {
            try {
                currencyUsageRepository.getTopCurrencies(limit)
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
                emptyList()
            }
        }
    }
    
    /**
     * Get least used currencies
     */
    suspend fun getLeastUsedCurrencies(limit: Int = 5): List<CurrencyUsageInfo> {
        return withContext(ioDispatcher) {
            try {
                val allCurrencies = currencyUsageRepository.getCurrencyUsage().first()
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
                emptyList()
            }
        }
    }
}

/**
 * Data class representing currency usage statistics
 */
data class CurrencyUsageStats(
    val totalUsage: Long = 0L,
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
    val totalTransactions: Long = 0L,
    val uniqueCurrencies: Int = 0,
    val primaryCurrency: String? = null,
    val topCurrencies: List<CurrencyUsageInfo> = emptyList(),
    val recentActivity: Boolean = false,
    val usageTrend: String = "Unknown"
)