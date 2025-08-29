package com.pennywise.app.data.repository

import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.data.util.PasswordHasher
import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.UserStatus
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
    private lateinit var passwordHasher: PasswordHasher
    
    @Before
    fun setUp() {
        mockUserDao = MockUserDao()
        passwordHasher = PasswordHasher()
        userRepository = UserRepositoryImpl(mockUserDao, passwordHasher)
    }
    
    @Test
    fun `registerUser should return success when username is available`() = runTest {
        // Given
        val username = "testuser"
        val password = "testpassword"
        mockUserDao.shouldReturnUser = null // No existing user
        
        // When
        val result = userRepository.registerUser(username, password)
        
        // Then
        assertTrue("Registration should succeed", result.isSuccess)
        assertTrue("Should return a valid user ID", result.getOrNull()!! > 0)
        assertEquals("Should insert user with correct username", username, mockUserDao.lastInsertedUser?.username)
        assertTrue("Should hash the password", mockUserDao.lastInsertedUser?.passwordHash?.contains(":") == true)
    }
    
    @Test
    fun `registerUser should return failure when username already exists`() = runTest {
        // Given
        val username = "existinguser"
        val password = "testpassword"
        val existingUser = UserEntity(id = 1, username = username, passwordHash = "hash")
        mockUserDao.shouldReturnUser = existingUser
        
        // When
        val result = userRepository.registerUser(username, password)
        
        // Then
        assertTrue("Registration should fail", result.isFailure)
        assertEquals("Should return username exists error", "Username already exists", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `authenticateUser should return success for valid credentials`() = runTest {
        // Given
        val username = "testuser"
        val password = "testpassword"
        val hashedPassword = passwordHasher.hashPassword(password)
        val userEntity = UserEntity(id = 1, username = username, passwordHash = hashedPassword)
        mockUserDao.shouldReturnUser = userEntity
        
        // When
        val result = userRepository.authenticateUser(username, password)
        
        // Then
        assertTrue("Authentication should succeed", result.isSuccess)
        val user = result.getOrNull()
        assertNotNull("Should return user", user)
        assertEquals("Should return correct username", username, user?.username)
        assertEquals("Should return correct ID", 1L, user?.id)
    }
    
    @Test
    fun `authenticateUser should return failure when user not found`() = runTest {
        // Given
        val username = "nonexistentuser"
        val password = "testpassword"
        mockUserDao.shouldReturnUser = null
        
        // When
        val result = userRepository.authenticateUser(username, password)
        
        // Then
        assertTrue("Authentication should fail", result.isFailure)
        assertEquals("Should return user not found error", "User not found", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `authenticateUser should return failure for invalid password`() = runTest {
        // Given
        val username = "testuser"
        val correctPassword = "correctpassword"
        val wrongPassword = "wrongpassword"
        val hashedPassword = passwordHasher.hashPassword(correctPassword)
        val userEntity = UserEntity(id = 1, username = username, passwordHash = hashedPassword)
        mockUserDao.shouldReturnUser = userEntity
        
        // When
        val result = userRepository.authenticateUser(username, wrongPassword)
        
        // Then
        assertTrue("Authentication should fail", result.isFailure)
        assertEquals("Should return invalid password error", "Invalid password", result.exceptionOrNull()?.message)
    }
    
    @Test
    fun `getUserById should return user when found`() = runTest {
        // Given
        val userId = 1L
        val userEntity = UserEntity(id = userId, username = "testuser", passwordHash = "hash")
        mockUserDao.shouldReturnUser = userEntity
        
        // When
        val result = userRepository.getUserById(userId)
        
        // Then
        assertNotNull("Should return user", result)
        assertEquals("Should return correct user ID", userId, result?.id)
        assertEquals("Should return correct username", "testuser", result?.username)
    }
    
    @Test
    fun `getUserById should return null when user not found`() = runTest {
        // Given
        val userId = 999L
        mockUserDao.shouldReturnUser = null
        
        // When
        val result = userRepository.getUserById(userId)
        
        // Then
        assertNull("Should return null", result)
    }
    
    @Test
    fun `isUsernameTaken should return true when username exists`() = runTest {
        // Given
        val username = "existinguser"
        mockUserDao.shouldReturnUsernameCount = 1
        
        // When
        val result = userRepository.isUsernameTaken(username)
        
        // Then
        assertTrue("Should return true for existing username", result)
    }
    
    @Test
    fun `isUsernameTaken should return false when username is available`() = runTest {
        // Given
        val username = "newuser"
        mockUserDao.shouldReturnUsernameCount = 0
        
        // When
        val result = userRepository.isUsernameTaken(username)
        
        // Then
        assertFalse("Should return false for available username", result)
    }
    
    @Test
    fun `getUsersByStatus should return users with correct status`() = runTest {
        // Given
        val status = UserStatus.ACTIVE
        val users = listOf(
            UserEntity(id = 1, username = "user1", passwordHash = "hash1", status = status),
            UserEntity(id = 2, username = "user2", passwordHash = "hash2", status = status)
        )
        mockUserDao.shouldReturnUsers = users
        
        // When
        val result = userRepository.getUsersByStatus(status)
        
        // Then
        val resultList = mutableListOf<User>()
        result.collect { resultList.addAll(it) }
        assertEquals("Should return correct number of users", 2, resultList.size)
        assertEquals("Should return correct first user", "user1", resultList[0].username)
        assertEquals("Should return correct second user", "user2", resultList[1].username)
    }
    
    // Mock implementation of UserDao for testing
    private class MockUserDao : UserDao {
        var shouldReturnUser: UserEntity? = null
        var shouldReturnUsers: List<UserEntity> = emptyList()
        var shouldReturnUsernameCount: Int = 0
        var lastInsertedUser: UserEntity? = null
        
        override suspend fun insertUser(user: UserEntity): Long {
            lastInsertedUser = user
            return 1L
        }
        
        override suspend fun updateUser(user: UserEntity) {
            // Mock implementation
        }
        
        override suspend fun deleteUser(user: UserEntity) {
            // Mock implementation
        }
        
        override suspend fun getUserById(userId: Long): UserEntity? {
            return shouldReturnUser
        }
        
        override suspend fun getUserByUsername(username: String): UserEntity? {
            return shouldReturnUser
        }
        
        override suspend fun getUserByEmail(email: String): UserEntity? {
            return shouldReturnUser
        }
        
        override suspend fun authenticateUser(username: String, passwordHash: String): UserEntity? {
            return shouldReturnUser
        }
        
        override fun getUsersByStatus(status: UserStatus) = flowOf(shouldReturnUsers)
        
        override fun getAllUsers() = flowOf(shouldReturnUsers)
        
        override suspend fun isUsernameTaken(username: String): Int {
            return shouldReturnUsernameCount
        }
        
        override suspend fun isEmailTaken(email: String): Int {
            return shouldReturnUsernameCount
        }
        
        override suspend fun updateUserStatus(userId: Long, status: UserStatus) {
            // Mock implementation
        }
        
        override suspend fun updateLastActivity(userId: Long, updatedAt: Long) {
            // Mock implementation
        }
    }
}
