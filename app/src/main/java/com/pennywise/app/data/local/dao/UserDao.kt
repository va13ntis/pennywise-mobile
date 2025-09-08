package com.pennywise.app.data.local.dao

import androidx.room.*
import com.pennywise.app.data.local.entity.UserEntity
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
    
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Long): UserEntity?
    
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE username = :username AND passwordHash = :passwordHash")
    suspend fun authenticateUser(username: String, passwordHash: String): UserEntity?
    
    @Query("SELECT * FROM users WHERE status = :status")
    fun getUsersByStatus(status: UserStatus): Flow<List<UserEntity>>
    
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserEntity>>
    
    @Query("SELECT COUNT(*) FROM users WHERE username = :username")
    suspend fun isUsernameTaken(username: String): Int
    
    @Query("SELECT COUNT(*) FROM users WHERE email = :email")
    suspend fun isEmailTaken(email: String): Int
    
    @Query("UPDATE users SET status = :status WHERE id = :userId")
    suspend fun updateUserStatus(userId: Long, status: UserStatus)
    
    @Query("UPDATE users SET updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateLastActivity(userId: Long, updatedAt: Long)
    
    @Query("UPDATE users SET defaultCurrency = :currency, updatedAt = :updatedAt WHERE id = :userId")
    suspend fun updateDefaultCurrency(userId: Long, currency: String, updatedAt: Long)
    
    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()
}
