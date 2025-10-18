package com.pennywise.app.data.local.dao

import androidx.room.*
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Data Access Object for currency usage operations
 */
@Dao
interface CurrencyUsageDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCurrencyUsage(currencyUsage: CurrencyUsageEntity): Long
    
    @Update
    suspend fun updateCurrencyUsage(currencyUsage: CurrencyUsageEntity)
    
    @Delete
    suspend fun deleteCurrencyUsage(currencyUsage: CurrencyUsageEntity)
    
    @Query("SELECT * FROM currency_usage WHERE id = :id")
    suspend fun getCurrencyUsageById(id: Long): CurrencyUsageEntity?
    
    @Query("SELECT * FROM currency_usage WHERE currency = :currency")
    suspend fun getCurrencyUsageByCurrency(currency: String): CurrencyUsageEntity?
    
    @Query("SELECT * FROM currency_usage ORDER BY usageCount DESC, lastUsed DESC")
    fun getAllCurrencyUsage(): Flow<List<CurrencyUsageEntity>>
    
    @Query("SELECT * FROM currency_usage ORDER BY usageCount DESC LIMIT :limit")
    fun getTopCurrencies(limit: Int = 10): Flow<List<CurrencyUsageEntity>>
    
    @Query("SELECT * FROM currency_usage ORDER BY usageCount DESC, lastUsed DESC")
    fun getCurrencyUsageSortedByUsage(): Flow<List<CurrencyUsageEntity>>
    
    @Query("UPDATE currency_usage SET usageCount = usageCount + 1, lastUsed = :lastUsed, updatedAt = :updatedAt WHERE currency = :currency")
    suspend fun incrementCurrencyUsage(currency: String, lastUsed: Date, updatedAt: Date)
    
    @Transaction
    suspend fun insertOrIncrementCurrencyUsage(currency: String, lastUsed: Date, createdAt: Date, updatedAt: Date) {
        val existing = getCurrencyUsageByCurrency(currency)
        if (existing != null) {
            incrementCurrencyUsage(currency, lastUsed, updatedAt)
        } else {
            insertCurrencyUsage(CurrencyUsageEntity(
                currency = currency,
                usageCount = 1,
                lastUsed = lastUsed,
                createdAt = createdAt,
                updatedAt = updatedAt
            ))
        }
    }
    
    @Query("DELETE FROM currency_usage")
    suspend fun deleteAllCurrencyUsage()
    
    @Query("SELECT COUNT(*) FROM currency_usage")
    suspend fun getCurrencyUsageCount(): Int
}