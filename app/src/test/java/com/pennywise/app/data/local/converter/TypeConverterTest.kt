package com.pennywise.app.data.local.converter

import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.UserRole
import com.pennywise.app.domain.model.UserStatus
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class TypeConverterTest {
    
    private val dateConverter = DateConverter()
    private val transactionTypeConverter = TransactionTypeConverter()
    private val recurringPeriodConverter = RecurringPeriodConverter()
    private val userRoleConverter = UserRoleConverter()
    private val userStatusConverter = UserStatusConverter()

    @Test
    fun dateConverter_shouldConvertDateToTimestampAndBack() {
        // Given
        val originalDate = Date()

        // When
        val timestamp = dateConverter.dateToTimestamp(originalDate)
        val convertedDate = dateConverter.fromTimestamp(timestamp)

        // Then
        assertNotNull(timestamp)
        assertNotNull(convertedDate)
        assertEquals(originalDate.time, convertedDate!!.time)
    }

    @Test
    fun dateConverter_shouldHandleNullValues() {
        // When
        val timestamp = dateConverter.dateToTimestamp(null)
        val date = dateConverter.fromTimestamp(null)

        // Then
        assertNull(timestamp)
        assertNull(date)
    }

    @Test
    fun transactionTypeConverter_shouldConvertTransactionTypeToStringAndBack() {
        // Given
        val originalType = TransactionType.EXPENSE

        // When
        val stringValue = transactionTypeConverter.fromTransactionType(originalType)
        val convertedType = transactionTypeConverter.toTransactionType(stringValue)

        // Then
        assertEquals("EXPENSE", stringValue)
        assertEquals(originalType, convertedType)
    }

    @Test
    fun transactionTypeConverter_shouldHandleAllTransactionTypes() {
        // Test all enum values
        TransactionType.values().forEach { type ->
            val stringValue = transactionTypeConverter.fromTransactionType(type)
            val convertedType = transactionTypeConverter.toTransactionType(stringValue)
            assertEquals(type, convertedType)
        }
    }

    @Test
    fun transactionTypeConverter_shouldHandleNullValues() {
        // When
        val stringValue = transactionTypeConverter.fromTransactionType(null)
        val type = transactionTypeConverter.toTransactionType(null)

        // Then
        assertNull(stringValue)
        assertNull(type)
    }

    @Test
    fun recurringPeriodConverter_shouldConvertRecurringPeriodToStringAndBack() {
        // Given
        val originalPeriod = RecurringPeriod.MONTHLY

        // When
        val stringValue = recurringPeriodConverter.fromRecurringPeriod(originalPeriod)
        val convertedPeriod = recurringPeriodConverter.toRecurringPeriod(stringValue)

        // Then
        assertEquals("MONTHLY", stringValue)
        assertEquals(originalPeriod, convertedPeriod)
    }

    @Test
    fun recurringPeriodConverter_shouldHandleAllRecurringPeriods() {
        // Test all enum values
        RecurringPeriod.values().forEach { period ->
            val stringValue = recurringPeriodConverter.fromRecurringPeriod(period)
            val convertedPeriod = recurringPeriodConverter.toRecurringPeriod(stringValue)
            assertEquals(period, convertedPeriod)
        }
    }

    @Test
    fun recurringPeriodConverter_shouldHandleNullValues() {
        // When
        val stringValue = recurringPeriodConverter.fromRecurringPeriod(null)
        val period = recurringPeriodConverter.toRecurringPeriod(null)

        // Then
        assertNull(stringValue)
        assertNull(period)
    }

    @Test
    fun userRoleConverter_shouldConvertUserRoleToStringAndBack() {
        // Given
        val originalRole = UserRole.ADMIN

        // When
        val stringValue = userRoleConverter.fromUserRole(originalRole)
        val convertedRole = userRoleConverter.toUserRole(stringValue)

        // Then
        assertEquals("ADMIN", stringValue)
        assertEquals(originalRole, convertedRole)
    }

    @Test
    fun userRoleConverter_shouldHandleAllUserRoles() {
        // Test all enum values
        UserRole.values().forEach { role ->
            val stringValue = userRoleConverter.fromUserRole(role)
            val convertedRole = userRoleConverter.toUserRole(stringValue)
            assertEquals(role, convertedRole)
        }
    }

    @Test
    fun userRoleConverter_shouldHandleNullValues() {
        // When
        val stringValue = userRoleConverter.fromUserRole(null)
        val role = userRoleConverter.toUserRole(null)

        // Then
        assertNull(stringValue)
        assertNull(role)
    }

    @Test
    fun userStatusConverter_shouldConvertUserStatusToStringAndBack() {
        // Given
        val originalStatus = UserStatus.ACTIVE

        // When
        val stringValue = userStatusConverter.fromUserStatus(originalStatus)
        val convertedStatus = userStatusConverter.toUserStatus(stringValue)

        // Then
        assertEquals("ACTIVE", stringValue)
        assertEquals(originalStatus, convertedStatus)
    }

    @Test
    fun userStatusConverter_shouldHandleAllUserStatuses() {
        // Test all enum values
        UserStatus.values().forEach { status ->
            val stringValue = userStatusConverter.fromUserStatus(status)
            val convertedStatus = userStatusConverter.toUserStatus(stringValue)
            assertEquals(status, convertedStatus)
        }
    }

    @Test
    fun userStatusConverter_shouldHandleNullValues() {
        // When
        val stringValue = userStatusConverter.fromUserStatus(null)
        val status = userStatusConverter.toUserStatus(null)

        // Then
        assertNull(stringValue)
        assertNull(status)
    }

    @Test
    fun allConverters_shouldHandleInvalidStringValues() {
        // Test that invalid string values return null
        assertNull(transactionTypeConverter.toTransactionType("INVALID_TYPE"))
        assertNull(recurringPeriodConverter.toRecurringPeriod("INVALID_PERIOD"))
        assertNull(userRoleConverter.toUserRole("INVALID_ROLE"))
        assertNull(userStatusConverter.toUserStatus("INVALID_STATUS"))
    }
}
