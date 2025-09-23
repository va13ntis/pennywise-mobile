package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.UserStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.util.Date

/**
 * Unit tests for UserRepositoryImpl
 */
class UserRepositoryImplTest {
    
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var mockUserDao: MockUserDao
    
    @Before
    fun setUp() {
        mockUserDao = MockUserDao()
        userRepository = UserRepositoryImpl(mockUserDao)
    }
    
    @Test
    fun `getSingleUser should return user when exists`() = runTest {
        // Given
        val userEntity = UserEntity(
            id = 1,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false
        )
        mockUserDao.shouldReturnUser = userEntity
        
        // When
        val result = userRepository.getSingleUser()
        
        // Then
        assertNotNull("Should return user when exists", result)
        assertEquals("Should return correct user ID", 1L, result!!.id)
        assertEquals("Should return correct currency", "USD", result.defaultCurrency)
        assertEquals("Should return correct locale", "en", result.locale)
        assertFalse("Should return correct device auth setting", result.deviceAuthEnabled)
    }
    
    @Test
    fun `getSingleUser should return null when no user exists`() = runTest {
        // Given
        mockUserDao.shouldReturnUser = null
        
        // When
        val result = userRepository.getSingleUser()
        
        // Then
        assertNull("Should return null when no user exists", result)
    }
    
    @Test
    fun `getSingleUserFlow should return flow with user when exists`() = runTest {
        // Given
        val userEntity = UserEntity(
            id = 1,
            defaultCurrency = "EUR",
            locale = "fr",
            deviceAuthEnabled = true
        )
        mockUserDao.shouldReturnUserFlow = flowOf(userEntity)
        
        // When
        val result = userRepository.getSingleUserFlow()
        val user = result.first()
        
        // Then
        assertNotNull("Should return user when exists", user)
        assertEquals("Should return correct user ID", 1L, user!!.id)
        assertEquals("Should return correct currency", "EUR", user.defaultCurrency)
        assertEquals("Should return correct locale", "fr", user.locale)
        assertTrue("Should return correct device auth setting", user.deviceAuthEnabled)
    }
    
    @Test
    fun `getSingleUserFlow should return flow with null when no user exists`() = runTest {
        // Given
        mockUserDao.shouldReturnUserFlow = flowOf(null)
        
        // When
        val result = userRepository.getSingleUserFlow()
        val user = result.first()
        
        // Then
        assertNull("Should return null when no user exists", user)
    }
    
    @Test
    fun `saveUser should insert new user`() = runTest {
        // Given
        val user = User(
            id = 0, // New user
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date(),
            updatedAt = Date()
        )
        mockUserDao.shouldReturnUserId = 1L
        
        // When
        val result = userRepository.createUser(user.defaultCurrency, user.locale)
        
        // Then
        assertTrue("Save should succeed", result.isSuccess)
        assertEquals("Should return correct user ID", 1L, result.getOrNull()!!)
        assertNotNull("Should insert user entity", mockUserDao.lastInsertedUser)
        assertEquals("Should insert with correct currency", "USD", mockUserDao.lastInsertedUser?.defaultCurrency)
    }
    
    @Test
    fun `saveUser should update existing user`() = runTest {
        // Given
        val user = User(
            id = 1, // Existing user
            defaultCurrency = "EUR",
            locale = "fr",
            deviceAuthEnabled = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        // When
        val result = userRepository.createUser(user.defaultCurrency, user.locale)
        
        // Then
        assertTrue("Save should succeed", result.isSuccess)
        assertEquals("Should return correct user ID", 1L, result.getOrNull()!!)
        assertNotNull("Should update user entity", mockUserDao.lastUpdatedUser)
        assertEquals("Should update with correct currency", "EUR", mockUserDao.lastUpdatedUser?.defaultCurrency)
    }
    
    @Test
    fun `getUserCount should return correct count`() = runTest {
        // Given
        mockUserDao.shouldReturnUserCount = 2
        
        // When
        val result = userRepository.getUserCount()
        
        // Then
        assertEquals("Should return correct count", 2, result)
    }
    
    @Test
    fun `updateUserStatus should update status`() = runTest {
        // Given
        val userId = 1L
        val newStatus = UserStatus.SUSPENDED
        
        // When
        userRepository.updateUserStatus(userId, newStatus)
        
        // Then
        assertEquals("Should call updateUserStatus with correct parameters", userId, mockUserDao.lastUpdatedUserId)
        assertEquals("Should call updateUserStatus with correct status", newStatus, mockUserDao.lastUpdatedStatus)
    }
    
    // deleteAllUsers method doesn't exist in UserRepository interface
    
    /**
     * Mock implementation of UserDao for testing
     */
    private class MockUserDao : UserDao {
        var shouldReturnUser: UserEntity? = null
        var shouldReturnUserFlow: kotlinx.coroutines.flow.Flow<UserEntity?> = flowOf(null)
        var shouldReturnUserId: Long = 1L
        var shouldReturnUserCount: Int = 0
        
        var lastInsertedUser: UserEntity? = null
        var lastUpdatedUser: UserEntity? = null
        var lastUpdatedUserId: Long = 0L
        var lastUpdatedStatus: UserStatus? = null
        var deleteAllUsersCalled = false
        
        override suspend fun insertUser(user: UserEntity): Long {
            lastInsertedUser = user
            return shouldReturnUserId
        }
        
        override suspend fun updateUser(user: UserEntity) {
            lastUpdatedUser = user
        }
        
        override suspend fun deleteUser(user: UserEntity) {
            // Mock implementation
        }
        
        override suspend fun getUserById(userId: Long): UserEntity? {
            return shouldReturnUser
        }
        
        override suspend fun getSingleUser(): UserEntity? {
            return shouldReturnUser
        }
        
        override fun getSingleUserFlow(): kotlinx.coroutines.flow.Flow<UserEntity?> {
            return shouldReturnUserFlow
        }
        
        override suspend fun getUserCount(): Int {
            return shouldReturnUserCount
        }
        
        override suspend fun updateUserStatus(userId: Long, status: UserStatus) {
            lastUpdatedUserId = userId
            lastUpdatedStatus = status
        }
        
        override suspend fun updateLastActivity(userId: Long, updatedAt: Long) {
            // Mock implementation
        }
        
        override suspend fun updateDefaultCurrency(userId: Long, currency: String, updatedAt: Long) {
            // Mock implementation
        }
        
        override suspend fun updateDeviceAuthEnabled(userId: Long, enabled: Boolean, updatedAt: Long) {
            // Mock implementation
        }
        
        override suspend fun deleteAllUsers() {
            deleteAllUsersCalled = true
        }
    }
}