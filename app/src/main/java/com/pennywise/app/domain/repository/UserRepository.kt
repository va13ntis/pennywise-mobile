package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.UserStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user data operations
 */
interface UserRepository {
    suspend fun registerUser(username: String, password: String, defaultCurrency: String = "USD"): Result<Long>
    suspend fun authenticateUser(username: String, password: String): Result<User>
    suspend fun getUserById(userId: Long): User?
    suspend fun getUserByUsername(username: String): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun updateUser(user: User)
    suspend fun deleteUser(user: User)
    suspend fun updateUserStatus(userId: Long, status: UserStatus)
    suspend fun updateDefaultCurrency(userId: Long, currency: String)
    suspend fun isUsernameTaken(username: String): Boolean
    suspend fun isEmailTaken(email: String): Boolean
    fun getUsersByStatus(status: UserStatus): Flow<List<User>>
    fun getAllUsers(): Flow<List<User>>
}
