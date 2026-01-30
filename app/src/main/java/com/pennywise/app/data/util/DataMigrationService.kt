package com.pennywise.app.data.util

import com.pennywise.app.domain.model.CurrencyUsage
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.first
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling data migrations when the database schema changes
 */
@Singleton
class DataMigrationService @Inject constructor(
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository,
    private val currencyUsageRepository: CurrencyUsageRepository
) {
    
    /**
     * Migrate existing data to include currency information
     * This should be called once after a database schema migration
     */
    suspend fun migrateCurrencyData() {
        try {
            // Get the user
            val user = userRepository.getUser()
            
            if (user != null) {
                // Update user's default currency if not already set
                if (user.defaultCurrency.isEmpty()) {
                    userRepository.updateDefaultCurrency("USD")
                }
                
                // Get all transactions
                val transactions = transactionRepository.getTransactions().first()
                
                // Group transactions by currency and count usage
                val currencyCounts = transactions.groupBy { it.currency }
                    .mapValues { it.value.size }
                
                // Initialize currency usage data
                currencyCounts.forEach { (currency, count) ->
                    val currencyUsage = CurrencyUsage(
                        currency = currency,
                        usageCount = count,
                        lastUsed = Date(),
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    currencyUsageRepository.insertCurrencyUsage(currencyUsage)
                }
                
                // If no currency usage data exists, create default USD usage
                if (currencyCounts.isEmpty()) {
                    val defaultUsage = CurrencyUsage(
                        currency = "USD",
                        usageCount = 0,
                        lastUsed = Date(),
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                    currencyUsageRepository.insertCurrencyUsage(defaultUsage)
                }
            }
        } catch (e: Exception) {
            // Log the error but don't crash the app
        }
    }
    
    /**
     * Check if currency migration is needed
     * Returns true if the user doesn't have a default currency set
     */
    suspend fun isCurrencyMigrationNeeded(): Boolean {
        return try {
            val user = userRepository.getUser()
            user?.defaultCurrency?.isEmpty() ?: false
        } catch (e: Exception) {
            false
        }
    }
}