package com.pennywise.app.data.local.dao

import androidx.room.*
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.UserStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for user operations
 * Simplified for single-user per app
 */
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    @Delete
    suspend fun deleteUser(user: UserEntity)
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?
    
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getSingleUser(): UserEntity?
    
    @Query("SELECT * FROM users LIMIT 1")
    fun getSingleUserFlow(): Flow<UserEntity?>
    
    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int
    
    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: Long, status: UserStatus)
    
    @Query("UPDATE users SET updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateLastActivity(userId: Long, updatedAt: Long)
    
    @Query("UPDATE users SET defaultCurrency = :currency, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateDefaultCurrency(userId: Long, currency: String, updatedAt: Long)
    
    @Query("UPDATE users SET deviceAuthEnabled = :enabled, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateDeviceAuthEnabled(userId: Long, enabled: Boolean, updatedAt: Long)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
