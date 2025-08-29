package com.pennywise.app.data.local.entity

import com.pennywise.app.domain.model.User
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.UserRole
import com.pennywise.app.domain.model.UserStatus
import org.junit.Test
import org.junit.Assert.*
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
            username = "testuser",
            passwordHash = "hashedpassword",
            email = "test@example.com",
            role = UserRole.USER,
            status = UserStatus.ACTIVE,
            createdAt = date,
            updatedAt = date
        )
        
        // When
        val user = userEntity.toDomainModel()
        
        // Then
        assertEquals("Should map ID correctly", 1L, user.id)
        assertEquals("Should map username correctly", "testuser", user.username)
        assertEquals("Should map password hash correctly", "hashedpassword", user.passwordHash)
        assertEquals("Should map email correctly", "test@example.com", user.email)
        assertEquals("Should map created at correctly", date, user.createdAt)
        assertEquals("Should map updated at correctly", date, user.updatedAt)
    }
    
    @Test
    fun `UserEntity fromDomainModel should map all properties correctly`() {
        // Given
        val date = Date()
        val user = User(
            id = 2L,
            username = "domainuser",
            passwordHash = "domainhash",
            email = "domain@example.com",
            createdAt = date,
            updatedAt = date
        )
        
        // When
        val userEntity = UserEntity.fromDomainModel(user)
        
        // Then
        assertEquals("Should map ID correctly", 2L, userEntity.id)
        assertEquals("Should map username correctly", "domainuser", userEntity.username)
        assertEquals("Should map password hash correctly", "domainhash", userEntity.passwordHash)
        assertEquals("Should map email correctly", "domain@example.com", userEntity.email)
        assertEquals("Should map created at correctly", date, userEntity.createdAt)
        assertEquals("Should map updated at correctly", date, userEntity.updatedAt)
        assertEquals("Should have default role", UserRole.USER, userEntity.role)
        assertEquals("Should have default status", UserStatus.ACTIVE, userEntity.status)
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
        assertEquals("Should map ID correctly", 1L, transaction.id)
        assertEquals("Should map user ID correctly", 1L, transaction.userId)
        assertEquals("Should map amount correctly", 100.50, transaction.amount, 0.01)
        assertEquals("Should map description correctly", "Test transaction", transaction.description)
        assertEquals("Should map category correctly", "Food", transaction.category)
        assertEquals("Should map type correctly", TransactionType.EXPENSE, transaction.type)
        assertEquals("Should map date correctly", date, transaction.date)
        assertTrue("Should map isRecurring correctly", transaction.isRecurring)
        assertEquals("Should map recurring period correctly", RecurringPeriod.MONTHLY, transaction.recurringPeriod)
        assertEquals("Should map created at correctly", date, transaction.createdAt)
        assertEquals("Should map updated at correctly", date, transaction.updatedAt)
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
        assertEquals("Should map ID correctly", 2L, transactionEntity.id)
        assertEquals("Should map user ID correctly", 1L, transactionEntity.userId)
        assertEquals("Should map amount correctly", 200.75, transactionEntity.amount, 0.01)
        assertEquals("Should map description correctly", "Domain transaction", transactionEntity.description)
        assertEquals("Should map category correctly", "Transport", transactionEntity.category)
        assertEquals("Should map type correctly", TransactionType.INCOME, transactionEntity.type)
        assertEquals("Should map date correctly", date, transactionEntity.date)
        assertFalse("Should map isRecurring correctly", transactionEntity.isRecurring)
        assertNull("Should map recurring period correctly", transactionEntity.recurringPeriod)
        assertEquals("Should map created at correctly", date, transactionEntity.createdAt)
        assertEquals("Should map updated at correctly", date, transactionEntity.updatedAt)
    }
    
    @Test
    fun `UserEntity mapping should handle null email`() {
        // Given
        val userEntity = UserEntity(
            id = 1L,
            username = "testuser",
            passwordHash = "hash",
            email = null
        )
        
        // When
        val user = userEntity.toDomainModel()
        
        // Then
        assertNull("Should handle null email", user.email)
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
        assertNull("Should handle null recurring period", transaction.recurringPeriod)
    }
    
    @Test
    fun `UserEntity mapping should preserve default values`() {
        // Given
        val user = User(
            id = 0L,
            username = "testuser",
            passwordHash = "hash"
        )
        
        // When
        val userEntity = UserEntity.fromDomainModel(user)
        
        // Then
        assertEquals("Should preserve default role", UserRole.USER, userEntity.role)
        assertEquals("Should preserve default status", UserStatus.ACTIVE, userEntity.status)
        assertNotNull("Should have created at date", userEntity.createdAt)
        assertNotNull("Should have updated at date", userEntity.updatedAt)
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
        assertFalse("Should have default isRecurring value", transactionEntity.isRecurring)
        assertNull("Should have default recurring period value", transactionEntity.recurringPeriod)
        assertNotNull("Should have created at date", transactionEntity.createdAt)
        assertNotNull("Should have updated at date", transactionEntity.updatedAt)
    }
}
