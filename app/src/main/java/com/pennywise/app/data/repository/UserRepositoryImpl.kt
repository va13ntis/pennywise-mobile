package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.data.util.PasswordHasher
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.UserStatus
import com.pennywise.app.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of UserRepository that handles user data operations
 */
class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val passwordHasher: PasswordHasher
) : UserRepository {
    
    override suspend fun registerUser(username: String, password: String, defaultCurrency: String, locale: String): Result<Long> {
        return try {
            // Check if username already exists
            val existingUser = userDao.getUserByUsername(username)
            if (existingUser != null) {
                return Result.failure(Exception("Username already exists"))
            }
            
            // Hash the password
            val passwordHash = passwordHasher.hashPassword(password)
            
            // Create new user entity
            val userEntity = UserEntity(
                username = username,
                passwordHash = passwordHash,
                defaultCurrency = defaultCurrency,
                locale = locale
            )
            
            // Insert user and return the generated ID
            val userId = userDao.insertUser(userEntity)
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun authenticateUser(username: String, password: String): Result<User> {
        return try {
            // Get user by username
            val userEntity = userDao.getUserByUsername(username)
                ?: return Result.failure(Exception("User not found"))
            
            // Verify password
            if (passwordHasher.verifyPassword(password, userEntity.passwordHash)) {
                Result.success(userEntity.toDomainModel())
            } else {
                Result.failure(Exception("Invalid password"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getUserById(userId: Long): User? {
        return userDao.getUserById(userId)?.toDomainModel()
    }
    
    override suspend fun getUserByUsername(username: String): User? {
        return userDao.getUserByUsername(username)?.toDomainModel()
    }
    
    override suspend fun getUserByEmail(email: String): User? {
        return userDao.getUserByEmail(email)?.toDomainModel()
    }
    
    override suspend fun updateUser(user: User) {
        val userEntity = UserEntity.fromDomainModel(user)
        userDao.updateUser(userEntity)
    }
    
    override suspend fun deleteUser(user: User) {
        val userEntity = UserEntity.fromDomainModel(user)
        userDao.deleteUser(userEntity)
    }
    
    override suspend fun updateUserStatus(userId: Long, status: UserStatus) {
        userDao.updateUserStatus(userId, status)
    }
    
    override suspend fun updateDefaultCurrency(userId: Long, currency: String) {
        userDao.updateDefaultCurrency(userId, currency, System.currentTimeMillis())
    }
    
    override suspend fun isUsernameTaken(username: String): Boolean {
        return userDao.isUsernameTaken(username) > 0
    }
    
    override suspend fun isEmailTaken(email: String): Boolean {
        return userDao.isEmailTaken(email) > 0
    }
    
    override fun getUsersByStatus(status: UserStatus): Flow<List<User>> {
        return userDao.getUsersByStatus(status).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
}
