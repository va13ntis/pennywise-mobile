package com.pennywise.app.data.local.entity

import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.UserRole
import com.pennywise.app.domain.model.UserStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.Date

/**
 * Unit tests for entity-domain model mapping functions
 */
class EntityMappingTest {
    
    @Test
    fun `UserEntity toDomainModel should map all properties correctly`() {
        // Given
        val date = Date()
        val userEntity = UserEntity(
            id = 1L,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = date,
            updatedAt = date
        )
        
        // When
        val user = userEntity.toDomainModel()
        
        // Then
        assertEquals(1L, user.id, "Should map ID correctly")
        assertEquals("USD", user.defaultCurrency, "Should map default currency correctly")
        assertEquals("en", user.locale, "Should map locale correctly")
        assertEquals(false, user.deviceAuthEnabled, "Should map device auth enabled correctly")
        assertEquals(date, user.createdAt, "Should map created at correctly")
        assertEquals(date, user.updatedAt, "Should map updated at correctly")
    }
    
    @Test
    fun `UserEntity fromDomainModel should map all properties correctly`() {
        // Given
        val date = Date()
        val user = User(
            id = 2L,
            defaultCurrency = "EUR",
            locale = "en",
            deviceAuthEnabled = true,
            createdAt = date,
            updatedAt = date
        )
        
        // When
        val userEntity = UserEntity.fromDomainModel(user)
        
        // Then
        assertEquals(2L, userEntity.id, "Should map ID correctly")
        assertEquals("EUR", userEntity.defaultCurrency, "Should map default currency correctly")
        assertEquals("en", userEntity.locale, "Should map locale correctly")
        assertEquals(true, userEntity.deviceAuthEnabled, "Should map device auth enabled correctly")
        assertEquals(date, userEntity.createdAt, "Should map created at correctly")
        assertEquals(date, userEntity.updatedAt, "Should map updated at correctly")
        assertEquals(UserRole.USER, userEntity.role, "Should have default role")
        assertEquals(UserStatus.ACTIVE, userEntity.status, "Should have default status")
    }
    
    @Test
    fun `TransactionEntity toDomainModel should map all properties correctly`() {
        // Given
        val date = Date()
        val transactionEntity = TransactionEntity(
            id = 1L,
            userId = 1L,
            amount = 100.50,
            description = "Test transaction",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = date,
            isRecurring = true,
            recurringPeriod = RecurringPeriod.MONTHLY,
            createdAt = date,
            updatedAt = date
        )
        
        // When
        val transaction = transactionEntity.toDomainModel()
        
        // Then
        assertEquals(1L, transaction.id, "Should map ID correctly")
        assertEquals(1L, transaction.userId, "Should map user ID correctly")
        assertEquals(100.50, transaction.amount, 0.01, "Should map amount correctly")
        assertEquals("Test transaction", transaction.description, "Should map description correctly")
        assertEquals("Food", transaction.category, "Should map category correctly")
        assertEquals(TransactionType.EXPENSE, transaction.type, "Should map type correctly")
        assertEquals(date, transaction.date, "Should map date correctly")
        assertTrue(transaction.isRecurring, "Should map isRecurring correctly")
        assertEquals(RecurringPeriod.MONTHLY, transaction.recurringPeriod, "Should map recurring period correctly")
        assertEquals(date, transaction.createdAt, "Should map created at correctly")
        assertEquals(date, transaction.updatedAt, "Should map updated at correctly")
    }
    
    @Test
    fun `TransactionEntity fromDomainModel should map all properties correctly`() {
        // Given
        val date = Date()
        val transaction = Transaction(
            id = 2L,
            userId = 1L,
            amount = 200.75,
            description = "Domain transaction",
            category = "Transport",
            type = TransactionType.INCOME,
            date = date,
            isRecurring = false,
            recurringPeriod = null,
            createdAt = date,
            updatedAt = date
        )
        
        // When
        val transactionEntity = TransactionEntity.fromDomainModel(transaction)
        
        // Then
        assertEquals(2L, transactionEntity.id, "Should map ID correctly")
        assertEquals(1L, transactionEntity.userId, "Should map user ID correctly")
        assertEquals(200.75, transactionEntity.amount, 0.01, "Should map amount correctly")
        assertEquals("Domain transaction", transactionEntity.description, "Should map description correctly")
        assertEquals("Transport", transactionEntity.category, "Should map category correctly")
        assertEquals(TransactionType.INCOME, transactionEntity.type, "Should map type correctly")
        assertEquals(date, transactionEntity.date, "Should map date correctly")
        assertFalse(transactionEntity.isRecurring, "Should map isRecurring correctly")
        assertNull(transactionEntity.recurringPeriod, "Should map recurring period correctly")
        assertEquals(date, transactionEntity.createdAt, "Should map created at correctly")
        assertEquals(date, transactionEntity.updatedAt, "Should map updated at correctly")
    }
    
    @Test
    fun `UserEntity mapping should handle null email`() {
        // Given
        val userEntity = UserEntity(
            id = 1L,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false
        )
        
        // When
        val user = userEntity.toDomainModel()
        
        // Then
        assertFalse(user.deviceAuthEnabled, "Should have device auth disabled")
    }
    
    @Test
    fun `TransactionEntity mapping should handle null recurring period`() {
        // Given
        val transactionEntity = TransactionEntity(
            id = 1L,
            userId = 1L,
            amount = 100.0,
            description = "Test",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date(),
            isRecurring = false,
            recurringPeriod = null
        )
        
        // When
        val transaction = transactionEntity.toDomainModel()
        
        // Then
        assertNull(transaction.recurringPeriod, "Should handle null recurring period")
    }
    
    @Test
    fun `UserEntity mapping should preserve default values`() {
        // Given
        val user = User(
            id = 0L,
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false
        )
        
        // When
        val userEntity = UserEntity.fromDomainModel(user)
        
        // Then
        assertEquals(UserRole.USER, userEntity.role, "Should preserve default role")
        assertEquals(UserStatus.ACTIVE, userEntity.status, "Should preserve default status")
        assertNotNull(userEntity.createdAt, "Should have created at date")
        assertNotNull(userEntity.updatedAt, "Should have updated at date")
    }
    
    @Test
    fun `TransactionEntity mapping should preserve default values`() {
        // Given
        val transaction = Transaction(
            id = 0L,
            userId = 1L,
            amount = 100.0,
            description = "Test",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = Date()
        )
        
        // When
        val transactionEntity = TransactionEntity.fromDomainModel(transaction)
        
        // Then
        assertFalse(transactionEntity.isRecurring, "Should have default isRecurring value")
        assertNull(transactionEntity.recurringPeriod, "Should have default recurring period value")
        assertNotNull(transactionEntity.createdAt, "Should have created at date")
        assertNotNull(transactionEntity.updatedAt, "Should have updated at date")
    }
}
