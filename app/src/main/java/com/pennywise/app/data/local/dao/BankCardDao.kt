package com.pennywise.app.data.local.dao

import androidx.room.*
import com.pennywise.app.data.local.entity.BankCardEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for bank card operations
 */
@Dao
interface BankCardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankCard(bankCard: BankCardEntity): Long
    
    @Update
    suspend fun updateBankCard(bankCard: BankCardEntity)
    
    @Delete
    suspend fun deleteBankCard(bankCard: BankCardEntity)
    
    @Query("SELECT * FROM bank_cards WHERE id = :cardId")
    suspend fun getBankCardById(cardId: Long): BankCardEntity?
    
    @Query("SELECT * FROM bank_cards ORDER BY createdAt DESC")
    fun getAllBankCards(): Flow<List<BankCardEntity>>
    
    @Query("SELECT * FROM bank_cards WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveBankCards(): Flow<List<BankCardEntity>>
    
    @Query("SELECT * FROM bank_cards WHERE alias = :alias")
    suspend fun getBankCardByAlias(alias: String): BankCardEntity?
    
    @Query("UPDATE bank_cards SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun updateBankCardStatus(cardId: Long, isActive: Boolean, updatedAt: Long)
    
    @Query("UPDATE bank_cards SET alias = :alias, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun updateBankCardAlias(cardId: Long, alias: String, updatedAt: Long)
    
    @Query("UPDATE bank_cards SET paymentDay = :paymentDay, updatedAt = :updatedAt WHERE id = :cardId")
    suspend fun updateBankCardPaymentDay(cardId: Long, paymentDay: Int, updatedAt: Long)
    
    @Query("SELECT COUNT(*) FROM bank_cards WHERE alias = :alias AND id != :excludeId")
    suspend fun isAliasTaken(alias: String, excludeId: Long = 0): Int
    
    @Query("DELETE FROM bank_cards")
    suspend fun deleteAllBankCards()
}