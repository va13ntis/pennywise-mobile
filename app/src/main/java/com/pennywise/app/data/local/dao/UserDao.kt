package com.pennywise.app.data.local.dao

import androidx.room.*
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.UserStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user operations
 */
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getUser(): UserEntity?
    
    @Query("SELECT * FROM users LIMIT 1")
    fun getUserFlow(): Flow<UserEntity?>
    
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
    
    @Query("UPDATE users SET status = :status")
    suspend fun updateUserStatus(status: UserStatus)
    
    @Query("UPDATE users SET updatedAt = :updatedAt")
    suspend fun updateLastActivity(updatedAt: Long)
    
    @Query("UPDATE users SET defaultCurrency = :currency, updatedAt = :updatedAt")
    suspend fun updateDefaultCurrency(currency: String, updatedAt: Long)
    
    @Query("UPDATE users SET defaultPaymentMethod = :paymentMethod, updatedAt = :updatedAt")
    suspend fun updateDefaultPaymentMethod(paymentMethod: PaymentMethod, updatedAt: Long)
    
    @Query("UPDATE users SET deviceAuthEnabled = :enabled, updatedAt = :updatedAt")
    suspend fun updateDeviceAuthEnabled(enabled: Boolean, updatedAt: Long)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
