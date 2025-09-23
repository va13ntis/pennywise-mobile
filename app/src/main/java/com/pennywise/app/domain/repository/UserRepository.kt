package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.UserStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user data operations
 * Simplified for single-user per app
 */
interface UserRepository {
    suspend fun createUser(defaultCurrency: String = "USD", locale: String = "en"): Result<Long>
    suspend fun getUserById(userId: Long): User?
    suspend fun getSingleUser(): User?
    fun getSingleUserFlow(): Flow<User?>
    suspend fun updateUser(user: User)
    suspend fun deleteUser(user: User)
    suspend fun updateUserStatus(userId: Long, status: UserStatus)
    suspend fun updateDefaultCurrency(userId: Long, currency: String)
    suspend fun updateDeviceAuthEnabled(userId: Long, enabled: Boolean)
    suspend fun getUserCount(): Int
}
