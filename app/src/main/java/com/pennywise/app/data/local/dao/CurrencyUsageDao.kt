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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCurrencyUsage(currencyUsage: CurrencyUsageEntity): Long
    
    @Update
    suspend fun updateCurrencyUsage(currencyUsage: CurrencyUsageEntity)
    
    @Delete
    suspend fun deleteCurrencyUsage(currencyUsage: CurrencyUsageEntity)
    
    @Query("SELECT * FROM currency_usage WHERE id = :id")
    suspend fun getCurrencyUsageById(id: Long): CurrencyUsageEntity?
    
    @Query("SELECT * FROM currency_usage WHERE userId = :userId AND currency = :currency")
    suspend fun getCurrencyUsageByUserAndCurrency(userId: Long, currency: String): CurrencyUsageEntity?
    
    @Query("SELECT * FROM currency_usage WHERE userId = :userId ORDER BY usageCount DESC, lastUsed DESC")
    fun getCurrencyUsageByUser(userId: Long): Flow<List<CurrencyUsageEntity>>
    
    @Query("SELECT * FROM currency_usage WHERE userId = :userId ORDER BY usageCount DESC LIMIT :limit")
    fun getTopCurrenciesByUser(userId: Long, limit: Int = 10): Flow<List<CurrencyUsageEntity>>
    
    @Query("SELECT * FROM currency_usage WHERE userId = :userId ORDER BY usageCount DESC, lastUsed DESC")
    fun getUserCurrenciesSortedByUsage(userId: Long): Flow<List<CurrencyUsageEntity>>
    
    @Query("UPDATE currency_usage SET usageCount = usageCount + 1, lastUsed = :lastUsed, updatedAt = :updatedAt WHERE userId = :userId AND currency = :currency")
    suspend fun incrementCurrencyUsage(userId: Long, currency: String, lastUsed: Date, updatedAt: Date)
    
    @Query("INSERT OR REPLACE INTO currency_usage (userId, currency, usageCount, lastUsed, createdAt, updatedAt) VALUES (:userId, :currency, 1, :lastUsed, :createdAt, :updatedAt)")
    suspend fun insertOrIncrementCurrencyUsage(userId: Long, currency: String, lastUsed: Date, createdAt: Date, updatedAt: Date)
    
    @Query("DELETE FROM currency_usage WHERE userId = :userId")
    suspend fun deleteAllCurrencyUsageForUser(userId: Long)
    
    @Query("SELECT COUNT(*) FROM currency_usage WHERE userId = :userId")
    suspend fun getCurrencyUsageCountForUser(userId: Long): Int
}
