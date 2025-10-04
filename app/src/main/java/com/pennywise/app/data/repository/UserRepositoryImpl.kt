package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.UserStatus
import com.pennywise.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of UserRepository that handles user data operations.
 * Note: This repository does NOT require authentication validation because AuthenticationValidator 
 * depends on UserRepository, which would create a circular dependency.
 * Authentication is handled at higher levels (ViewModel/UseCase).
 */
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao
) : UserRepository {
    
    override suspend fun createUser(defaultCurrency: String, locale: String): Result<Long> {
        return try {
            // Check if user already exists
            val existingUser = userDao.getUser()
            if (existingUser != null) {
                return Result.failure(Exception("User already exists"))
            }
            
            // Create new user entity
            val userEntity = UserEntity(
                defaultCurrency = defaultCurrency,
                locale = locale,
                deviceAuthEnabled = false
            )
            
            // Insert user and return the generated ID
            val userId = userDao.insertUser(userEntity)
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUser(): User? {
        return userDao.getUser()?.toDomainModel()
    }
    
    override fun getUserFlow(): Flow<User?> {
        return userDao.getUserFlow().map { it?.toDomainModel() }
    }
    
    override suspend fun updateUser(user: User) {
        val userEntity = UserEntity.fromDomainModel(user)
        userDao.updateUser(userEntity)
    }
    
    override suspend fun deleteUser(user: User) {
        val userEntity = UserEntity.fromDomainModel(user)
        userDao.deleteUser(userEntity)
    }
    
    override suspend fun updateUserStatus(status: UserStatus) {
        userDao.updateUserStatus(status)
    }
    
    override suspend fun updateDefaultCurrency(currency: String) {
        userDao.updateDefaultCurrency(currency, System.currentTimeMillis())
    }
    
    override suspend fun updateDeviceAuthEnabled(enabled: Boolean) {
        userDao.updateDeviceAuthEnabled(enabled, System.currentTimeMillis())
    }
    
    override suspend fun getUserCount(): Int {
        return userDao.getUserCount()
    }
}
