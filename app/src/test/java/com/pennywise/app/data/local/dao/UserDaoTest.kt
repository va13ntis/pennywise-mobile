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
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false
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
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false
        )
        val userId = userDao.insertUser(user)

        // When
        val retrievedUser = userDao.getUserById(userId)

        // Then
        assertNotNull(retrievedUser)
        assertEquals(user.defaultCurrency, retrievedUser!!.defaultCurrency)
        assertEquals(user.locale, retrievedUser.locale)
        assertEquals(user.deviceAuthEnabled, retrievedUser.deviceAuthEnabled)
    }

    @Test
    fun getSingleUser_shouldReturnUser() = runTest {
        // Given
        val user = UserEntity(
            defaultCurrency = "EUR",
            locale = "en",
            deviceAuthEnabled = true
        )
        userDao.insertUser(user)

        // When
        val retrievedUser = userDao.getSingleUser()

        // Then
        assertNotNull(retrievedUser)
        assertEquals(user.defaultCurrency, retrievedUser!!.defaultCurrency)
        assertEquals(user.locale, retrievedUser.locale)
    }

    @Test
    fun getSingleUserFlow_shouldReturnUser() = runTest {
        // Given
        val user = UserEntity(
            defaultCurrency = "GBP",
            locale = "en",
            deviceAuthEnabled = false
        )
        userDao.insertUser(user)

        // When
        val retrievedUser = userDao.getSingleUserFlow().first()

        // Then
        assertNotNull(retrievedUser)
        assertEquals(user.defaultCurrency, retrievedUser!!.defaultCurrency)
        assertEquals(user.locale, retrievedUser.locale)
    }

    @Test
    fun getUserCount_shouldReturnCorrectCount() = runTest {
        // Given
        val user1 = UserEntity(defaultCurrency = "USD", locale = "en")
        val user2 = UserEntity(defaultCurrency = "EUR", locale = "fr")
        userDao.insertUser(user1)
        userDao.insertUser(user2)

        // When
        val count = userDao.getUserCount()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun updateUserStatus_shouldUpdateStatus() = runTest {
        // Given
        val user = UserEntity(
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
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
        val user = UserEntity(defaultCurrency = "USD", locale = "en")
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
    fun updateDefaultCurrency_shouldUpdateCurrency() = runTest {
        // Given
        val user = UserEntity(defaultCurrency = "USD", locale = "en")
        val userId = userDao.insertUser(user)

        // When
        val newTimestamp = System.currentTimeMillis()
        userDao.updateDefaultCurrency(userId, "EUR", newTimestamp)

        // Then
        val updatedUser = userDao.getUserById(userId)!!
        assertEquals("EUR", updatedUser.defaultCurrency)
        assertTrue(updatedUser.updatedAt.time >= newTimestamp)
    }

    @Test
    fun updateDeviceAuthEnabled_shouldUpdateAuthSetting() = runTest {
        // Given
        val user = UserEntity(defaultCurrency = "USD", locale = "en", deviceAuthEnabled = false)
        val userId = userDao.insertUser(user)

        // When
        val newTimestamp = System.currentTimeMillis()
        userDao.updateDeviceAuthEnabled(userId, true, newTimestamp)

        // Then
        val updatedUser = userDao.getUserById(userId)!!
        assertTrue(updatedUser.deviceAuthEnabled)
        assertTrue(updatedUser.updatedAt.time >= newTimestamp)
    }

    @Test
    fun deleteUser_shouldRemoveUser() = runTest {
        // Given
        val user = UserEntity(defaultCurrency = "USD", locale = "en")
        val userId = userDao.insertUser(user)
        assertNotNull(userDao.getUserById(userId))

        // When
        val userToDelete = userDao.getUserById(userId)!!
        userDao.deleteUser(userToDelete)

        // Then
        assertNull(userDao.getUserById(userId))
    }

    @Test
    fun deleteAllUsers_shouldRemoveAllUsers() = runTest {
        // Given
        val user1 = UserEntity(defaultCurrency = "USD", locale = "en")
        val user2 = UserEntity(defaultCurrency = "EUR", locale = "fr")
        userDao.insertUser(user1)
        userDao.insertUser(user2)
        assertEquals(2, userDao.getUserCount())

        // When
        userDao.deleteAllUsers()

        // Then
        assertEquals(0, userDao.getUserCount())
    }
}