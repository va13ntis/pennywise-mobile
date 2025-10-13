package com.pennywise.app.domain.repository

import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.UserStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for user data operations
 */
interface UserRepository {
    suspend fun createUser(defaultCurrency: String = "USD", locale: String = "en"): Result<Long>
    suspend fun getUser(): User?
    fun getUserFlow(): Flow<User?>
    suspend fun updateUser(user: User)
    suspend fun deleteUser(user: User)
    suspend fun updateUserStatus(status: UserStatus)
    suspend fun updateDefaultCurrency(currency: String)
    suspend fun updateDefaultPaymentMethod(paymentMethod: PaymentMethod)
    suspend fun updateDeviceAuthEnabled(enabled: Boolean)
    suspend fun getUserCount(): Int
}
