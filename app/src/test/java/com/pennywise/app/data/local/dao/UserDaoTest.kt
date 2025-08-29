package com.pennywise.app.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.UserRole
import com.pennywise.app.domain.model.UserStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class UserDaoTest {
    private lateinit var database: PennyWiseDatabase
    private lateinit var userDao: UserDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, PennyWiseDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = database.userDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertUser_shouldReturnUserId() = runTest {
        // Given
        val user = UserEntity(
            username = "testuser",
            passwordHash = "hashedpassword",
            email = "test@example.com"
        )

        // When
        val userId = userDao.insertUser(user)

        // Then
        assertTrue(userId > 0)
    }

    @Test
    fun getUserById_shouldReturnUser() = runTest {
        // Given
        val user = UserEntity(
            username = "testuser",
            passwordHash = "hashedpassword",
            email = "test@example.com"
        )
        val userId = userDao.insertUser(user)

        // When
        val retrievedUser = userDao.getUserById(userId)

        // Then
        assertNotNull(retrievedUser)
        assertEquals(user.username, retrievedUser!!.username)
        assertEquals(user.passwordHash, retrievedUser.passwordHash)
        assertEquals(user.email, retrievedUser.email)
    }

    @Test
    fun getUserByUsername_shouldReturnUser() = runTest {
        // Given
        val user = UserEntity(
            username = "testuser",
            passwordHash = "hashedpassword",
            email = "test@example.com"
        )
        userDao.insertUser(user)

        // When
        val retrievedUser = userDao.getUserByUsername("testuser")

        // Then
        assertNotNull(retrievedUser)
        assertEquals(user.username, retrievedUser!!.username)
    }

    @Test
    fun getUserByEmail_shouldReturnUser() = runTest {
        // Given
        val user = UserEntity(
            username = "testuser",
            passwordHash = "hashedpassword",
            email = "test@example.com"
        )
        userDao.insertUser(user)

        // When
        val retrievedUser = userDao.getUserByEmail("test@example.com")

        // Then
        assertNotNull(retrievedUser)
        assertEquals(user.email, retrievedUser!!.email)
    }

    @Test
    fun authenticateUser_shouldReturnUser_whenCredentialsMatch() = runTest {
        // Given
        val user = UserEntity(
            username = "testuser",
            passwordHash = "hashedpassword",
            email = "test@example.com"
        )
        userDao.insertUser(user)

        // When
        val authenticatedUser = userDao.authenticateUser("testuser", "hashedpassword")

        // Then
        assertNotNull(authenticatedUser)
        assertEquals(user.username, authenticatedUser!!.username)
    }

    @Test
    fun authenticateUser_shouldReturnNull_whenCredentialsDontMatch() = runTest {
        // Given
        val user = UserEntity(
            username = "testuser",
            passwordHash = "hashedpassword",
            email = "test@example.com"
        )
        userDao.insertUser(user)

        // When
        val authenticatedUser = userDao.authenticateUser("testuser", "wrongpassword")

        // Then
        assertNull(authenticatedUser)
    }

    @Test
    fun getAllUsers_shouldReturnAllUsers() = runTest {
        // Given
        val user1 = UserEntity(username = "user1", passwordHash = "hash1")
        val user2 = UserEntity(username = "user2", passwordHash = "hash2")
        userDao.insertUser(user1)
        userDao.insertUser(user2)

        // When
        val users = userDao.getAllUsers().first()

        // Then
        assertEquals(2, users.size)
        assertTrue(users.any { it.username == "user1" })
        assertTrue(users.any { it.username == "user2" })
    }

    @Test
    fun getUsersByStatus_shouldReturnFilteredUsers() = runTest {
        // Given
        val activeUser = UserEntity(
            username = "activeuser",
            passwordHash = "hash1",
            status = UserStatus.ACTIVE
        )
        val inactiveUser = UserEntity(
            username = "inactiveuser",
            passwordHash = "hash2",
            status = UserStatus.INACTIVE
        )
        userDao.insertUser(activeUser)
        userDao.insertUser(inactiveUser)

        // When
        val activeUsers = userDao.getUsersByStatus(UserStatus.ACTIVE).first()

        // Then
        assertEquals(1, activeUsers.size)
        assertEquals("activeuser", activeUsers[0].username)
    }

    @Test
    fun isUsernameTaken_shouldReturnOne_whenUsernameExists() = runTest {
        // Given
        val user = UserEntity(username = "testuser", passwordHash = "hash")
        userDao.insertUser(user)

        // When
        val count = userDao.isUsernameTaken("testuser")

        // Then
        assertEquals(1, count)
    }

    @Test
    fun isUsernameTaken_shouldReturnZero_whenUsernameDoesNotExist() = runTest {
        // When
        val count = userDao.isUsernameTaken("nonexistent")

        // Then
        assertEquals(0, count)
    }

    @Test
    fun isEmailTaken_shouldReturnOne_whenEmailExists() = runTest {
        // Given
        val user = UserEntity(
            username = "testuser",
            passwordHash = "hash",
            email = "test@example.com"
        )
        userDao.insertUser(user)

        // When
        val count = userDao.isEmailTaken("test@example.com")

        // Then
        assertEquals(1, count)
    }

    @Test
    fun updateUserStatus_shouldUpdateStatus() = runTest {
        // Given
        val user = UserEntity(
            username = "testuser",
            passwordHash = "hash",
            status = UserStatus.ACTIVE
        )
        val userId = userDao.insertUser(user)

        // When
        userDao.updateUserStatus(userId, UserStatus.SUSPENDED)

        // Then
        val updatedUser = userDao.getUserById(userId)
        assertEquals(UserStatus.SUSPENDED, updatedUser!!.status)
    }

    @Test
    fun updateLastActivity_shouldUpdateTimestamp() = runTest {
        // Given
        val user = UserEntity(username = "testuser", passwordHash = "hash")
        val userId = userDao.insertUser(user)
        val originalUser = userDao.getUserById(userId)!!

        // When
        val newTimestamp = System.currentTimeMillis()
        userDao.updateLastActivity(userId, newTimestamp)

        // Then
        val updatedUser = userDao.getUserById(userId)!!
        assertTrue(updatedUser.updatedAt.time >= newTimestamp)
    }

    @Test
    fun deleteUser_shouldRemoveUser() = runTest {
        // Given
        val user = UserEntity(username = "testuser", passwordHash = "hash")
        val userId = userDao.insertUser(user)
        assertNotNull(userDao.getUserById(userId))

        // When
        val userToDelete = userDao.getUserById(userId)!!
        userDao.deleteUser(userToDelete)

        // Then
        assertNull(userDao.getUserById(userId))
    }
}
