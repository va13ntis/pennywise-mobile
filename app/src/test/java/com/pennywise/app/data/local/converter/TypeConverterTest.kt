package com.pennywise.app.data.local.converter

import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.UserRole
import com.pennywise.app.domain.model.UserStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.util.Date

@DisplayName("TypeConverter Tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypeConverterTest {
    
    private lateinit var dateConverter: DateConverter
    private lateinit var transactionTypeConverter: TransactionTypeConverter
    private lateinit var recurringPeriodConverter: RecurringPeriodConverter
    private lateinit var userRoleConverter: UserRoleConverter
    private lateinit var userStatusConverter: UserStatusConverter

    @BeforeEach
    fun setUp() {
        dateConverter = DateConverter()
        transactionTypeConverter = TransactionTypeConverter()
        recurringPeriodConverter = RecurringPeriodConverter()
        userRoleConverter = UserRoleConverter()
        userStatusConverter = UserStatusConverter()
    }

    @Nested
    @DisplayName("DateConverter Tests")
    inner class DateConverterTests {

        @Test
        @DisplayName("Should convert Date to timestamp and back correctly")
        fun shouldConvertDateToTimestampAndBack() {
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
        @DisplayName("Should handle null values correctly")
        fun shouldHandleNullValues() {
            // When
            val timestamp = dateConverter.dateToTimestamp(null)
            val date = dateConverter.fromTimestamp(null)

            // Then
            assertNull(timestamp)
            assertNull(date)
        }

        @Test
        @DisplayName("Should handle epoch time correctly")
        fun shouldHandleEpochTime() {
            // Given
            val epochDate = Date(0L)

            // When
            val timestamp = dateConverter.dateToTimestamp(epochDate)
            val convertedDate = dateConverter.fromTimestamp(timestamp)

            // Then
            assertEquals(0L, timestamp)
            assertEquals(epochDate.time, convertedDate!!.time)
        }

        @Test
        @DisplayName("Should handle future dates correctly")
        fun shouldHandleFutureDates() {
            // Given
            val futureDate = Date(System.currentTimeMillis() + 86400000L) // 1 day in future

            // When
            val timestamp = dateConverter.dateToTimestamp(futureDate)
            val convertedDate = dateConverter.fromTimestamp(timestamp)

            // Then
            assertNotNull(timestamp)
            assertNotNull(convertedDate)
            assertEquals(futureDate.time, convertedDate!!.time)
        }

        @Test
        @DisplayName("Should handle past dates correctly")
        fun shouldHandlePastDates() {
            // Given
            val pastDate = Date(System.currentTimeMillis() - 86400000L) // 1 day in past

            // When
            val timestamp = dateConverter.dateToTimestamp(pastDate)
            val convertedDate = dateConverter.fromTimestamp(timestamp)

            // Then
            assertNotNull(timestamp)
            assertNotNull(convertedDate)
            assertEquals(pastDate.time, convertedDate!!.time)
        }
    }

    @Nested
    @DisplayName("TransactionTypeConverter Tests")
    inner class TransactionTypeConverterTests {

        @Test
        @DisplayName("Should convert TransactionType to string and back correctly")
        fun shouldConvertTransactionTypeToStringAndBack() {
            // Given
            val originalType = TransactionType.EXPENSE

            // When
            val stringValue = transactionTypeConverter.fromTransactionType(originalType)
            val convertedType = transactionTypeConverter.toTransactionType(stringValue)

            // Then
            assertEquals("EXPENSE", stringValue)
            assertEquals(originalType, convertedType)
        }

        @ParameterizedTest
        @EnumSource(TransactionType::class)
        @DisplayName("Should handle all TransactionType enum values")
        fun shouldHandleAllTransactionTypes(type: TransactionType) {
            // When
            val stringValue = transactionTypeConverter.fromTransactionType(type)
            val convertedType = transactionTypeConverter.toTransactionType(stringValue)

            // Then
            assertEquals(type.name, stringValue)
            assertEquals(type, convertedType)
        }

        @Test
        @DisplayName("Should handle null values correctly")
        fun shouldHandleNullValues() {
            // When
            val stringValue = transactionTypeConverter.fromTransactionType(null)
            val type = transactionTypeConverter.toTransactionType(null)

            // Then
            assertNull(stringValue)
            assertNull(type)
        }

        @ParameterizedTest
        @ValueSource(strings = ["INVALID_TYPE", "UNKNOWN", "INCOME", "EXPENSE", ""])
        @NullAndEmptySource
        @DisplayName("Should handle invalid string values correctly")
        fun shouldHandleInvalidStringValues(invalidValue: String?) {
            // When
            val result = transactionTypeConverter.toTransactionType(invalidValue)

            // Then
            if (invalidValue == "INCOME" || invalidValue == "EXPENSE") {
                assertNotNull(result)
                assertEquals(invalidValue, result?.name)
            } else {
                assertNull(result)
            }
        }

        @Test
        @DisplayName("Should handle case sensitivity correctly")
        fun shouldHandleCaseSensitivity() {
            // When
            val result = transactionTypeConverter.toTransactionType("expense")

            // Then
            assertNull(result) // Should be null because enum values are case-sensitive
        }
    }

    @Nested
    @DisplayName("RecurringPeriodConverter Tests")
    inner class RecurringPeriodConverterTests {

        @Test
        @DisplayName("Should convert RecurringPeriod to string and back correctly")
        fun shouldConvertRecurringPeriodToStringAndBack() {
            // Given
            val originalPeriod = RecurringPeriod.MONTHLY

            // When
            val stringValue = recurringPeriodConverter.fromRecurringPeriod(originalPeriod)
            val convertedPeriod = recurringPeriodConverter.toRecurringPeriod(stringValue)

            // Then
            assertEquals("MONTHLY", stringValue)
            assertEquals(originalPeriod, convertedPeriod)
        }

        @ParameterizedTest
        @EnumSource(RecurringPeriod::class)
        @DisplayName("Should handle all RecurringPeriod enum values")
        fun shouldHandleAllRecurringPeriods(period: RecurringPeriod) {
            // When
            val stringValue = recurringPeriodConverter.fromRecurringPeriod(period)
            val convertedPeriod = recurringPeriodConverter.toRecurringPeriod(stringValue)

            // Then
            assertEquals(period.name, stringValue)
            assertEquals(period, convertedPeriod)
        }

        @Test
        @DisplayName("Should handle null values correctly")
        fun shouldHandleNullValues() {
            // When
            val stringValue = recurringPeriodConverter.fromRecurringPeriod(null)
            val period = recurringPeriodConverter.toRecurringPeriod(null)

            // Then
            assertNull(stringValue)
            assertNull(period)
        }

        @ParameterizedTest
        @ValueSource(strings = ["INVALID_PERIOD", "UNKNOWN", "DAILY", "WEEKLY", "MONTHLY", "YEARLY", ""])
        @NullAndEmptySource
        @DisplayName("Should handle invalid string values correctly")
        fun shouldHandleInvalidStringValues(invalidValue: String?) {
            // When
            val result = recurringPeriodConverter.toRecurringPeriod(invalidValue)

            // Then
            if (invalidValue in listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY")) {
                assertNotNull(result)
                assertEquals(invalidValue, result?.name)
            } else {
                assertNull(result)
            }
        }

        @Test
        @DisplayName("Should handle case sensitivity correctly")
        fun shouldHandleCaseSensitivity() {
            // When
            val result = recurringPeriodConverter.toRecurringPeriod("monthly")

            // Then
            assertNull(result) // Should be null because enum values are case-sensitive
        }
    }

    @Nested
    @DisplayName("UserRoleConverter Tests")
    inner class UserRoleConverterTests {

        @Test
        @DisplayName("Should convert UserRole to string and back correctly")
        fun shouldConvertUserRoleToStringAndBack() {
            // Given
            val originalRole = UserRole.ADMIN

            // When
            val stringValue = userRoleConverter.fromUserRole(originalRole)
            val convertedRole = userRoleConverter.toUserRole(stringValue)

            // Then
            assertEquals("ADMIN", stringValue)
            assertEquals(originalRole, convertedRole)
        }

        @ParameterizedTest
        @EnumSource(UserRole::class)
        @DisplayName("Should handle all UserRole enum values")
        fun shouldHandleAllUserRoles(role: UserRole) {
            // When
            val stringValue = userRoleConverter.fromUserRole(role)
            val convertedRole = userRoleConverter.toUserRole(stringValue)

            // Then
            assertEquals(role.name, stringValue)
            assertEquals(role, convertedRole)
        }

        @Test
        @DisplayName("Should handle null values correctly")
        fun shouldHandleNullValues() {
            // When
            val stringValue = userRoleConverter.fromUserRole(null)
            val role = userRoleConverter.toUserRole(null)

            // Then
            assertNull(stringValue)
            assertNull(role)
        }

        @ParameterizedTest
        @ValueSource(strings = ["INVALID_ROLE", "UNKNOWN", "USER", "ADMIN", ""])
        @NullAndEmptySource
        @DisplayName("Should handle invalid string values correctly")
        fun shouldHandleInvalidStringValues(invalidValue: String?) {
            // When
            val result = userRoleConverter.toUserRole(invalidValue)

            // Then
            if (invalidValue in listOf("USER", "ADMIN")) {
                assertNotNull(result)
                assertEquals(invalidValue, result?.name)
            } else {
                assertNull(result)
            }
        }

        @Test
        @DisplayName("Should handle case sensitivity correctly")
        fun shouldHandleCaseSensitivity() {
            // When
            val result = userRoleConverter.toUserRole("admin")

            // Then
            assertNull(result) // Should be null because enum values are case-sensitive
        }
    }

    @Nested
    @DisplayName("UserStatusConverter Tests")
    inner class UserStatusConverterTests {

        @Test
        @DisplayName("Should convert UserStatus to string and back correctly")
        fun shouldConvertUserStatusToStringAndBack() {
            // Given
            val originalStatus = UserStatus.ACTIVE

            // When
            val stringValue = userStatusConverter.fromUserStatus(originalStatus)
            val convertedStatus = userStatusConverter.toUserStatus(stringValue)

            // Then
            assertEquals("ACTIVE", stringValue)
            assertEquals(originalStatus, convertedStatus)
        }

        @ParameterizedTest
        @EnumSource(UserStatus::class)
        @DisplayName("Should handle all UserStatus enum values")
        fun shouldHandleAllUserStatuses(status: UserStatus) {
            // When
            val stringValue = userStatusConverter.fromUserStatus(status)
            val convertedStatus = userStatusConverter.toUserStatus(stringValue)

            // Then
            assertEquals(status.name, stringValue)
            assertEquals(status, convertedStatus)
        }

        @Test
        @DisplayName("Should handle null values correctly")
        fun shouldHandleNullValues() {
            // When
            val stringValue = userStatusConverter.fromUserStatus(null)
            val status = userStatusConverter.toUserStatus(null)

            // Then
            assertNull(stringValue)
            assertNull(status)
        }

        @ParameterizedTest
        @ValueSource(strings = ["INVALID_STATUS", "UNKNOWN", "ACTIVE", "INACTIVE", "SUSPENDED", ""])
        @NullAndEmptySource
        @DisplayName("Should handle invalid string values correctly")
        fun shouldHandleInvalidStringValues(invalidValue: String?) {
            // When
            val result = userStatusConverter.toUserStatus(invalidValue)

            // Then
            if (invalidValue in listOf("ACTIVE", "INACTIVE", "SUSPENDED")) {
                assertNotNull(result)
                assertEquals(invalidValue, result?.name)
            } else {
                assertNull(result)
            }
        }

        @Test
        @DisplayName("Should handle case sensitivity correctly")
        fun shouldHandleCaseSensitivity() {
            // When
            val result = userStatusConverter.toUserStatus("active")

            // Then
            assertNull(result) // Should be null because enum values are case-sensitive
        }
    }

    @Nested
    @DisplayName("Cross-Converter Integration Tests")
    inner class CrossConverterIntegrationTests {

        @Test
        @DisplayName("All converters should handle invalid string values consistently")
        fun allConvertersShouldHandleInvalidStringValues() {
            // Test that invalid string values return null for all converters
            assertNull(transactionTypeConverter.toTransactionType("INVALID_TYPE"))
            assertNull(recurringPeriodConverter.toRecurringPeriod("INVALID_PERIOD"))
            assertNull(userRoleConverter.toUserRole("INVALID_ROLE"))
            assertNull(userStatusConverter.toUserStatus("INVALID_STATUS"))
        }

        @Test
        @DisplayName("All converters should handle null values consistently")
        fun allConvertersShouldHandleNullValues() {
            // Test that null values are handled consistently across all converters
            assertNull(transactionTypeConverter.fromTransactionType(null))
            assertNull(transactionTypeConverter.toTransactionType(null))
            
            assertNull(recurringPeriodConverter.fromRecurringPeriod(null))
            assertNull(recurringPeriodConverter.toRecurringPeriod(null))
            
            assertNull(userRoleConverter.fromUserRole(null))
            assertNull(userRoleConverter.toUserRole(null))
            
            assertNull(userStatusConverter.fromUserStatus(null))
            assertNull(userStatusConverter.toUserStatus(null))
        }

        @Test
        @DisplayName("All enum converters should handle empty strings consistently")
        fun allEnumConvertersShouldHandleEmptyStrings() {
            // Test that empty strings return null for all enum converters
            assertNull(transactionTypeConverter.toTransactionType(""))
            assertNull(recurringPeriodConverter.toRecurringPeriod(""))
            assertNull(userRoleConverter.toUserRole(""))
            assertNull(userStatusConverter.toUserStatus(""))
        }

        @Test
        @DisplayName("All enum converters should handle whitespace strings consistently")
        fun allEnumConvertersShouldHandleWhitespaceStrings() {
            // Test that whitespace strings return null for all enum converters
            assertNull(transactionTypeConverter.toTransactionType(" "))
            assertNull(recurringPeriodConverter.toRecurringPeriod("  "))
            assertNull(userRoleConverter.toUserRole("\t"))
            assertNull(userStatusConverter.toUserStatus("\n"))
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("DateConverter should handle multiple conversions efficiently")
        fun dateConverterShouldHandleMultipleConversionsEfficiently() {
            // Given
            val dates = (1..1000).map { Date(System.currentTimeMillis() + it * 1000L) }

            // When & Then
            val startTime = System.currentTimeMillis()
            dates.forEach { date ->
                val timestamp = dateConverter.dateToTimestamp(date)
                val convertedDate = dateConverter.fromTimestamp(timestamp)
                assertEquals(date.time, convertedDate!!.time)
            }
            val endTime = System.currentTimeMillis()
            
            // Performance assertion - should complete within reasonable time
            assertTrue(endTime - startTime < 1000, "Date conversion should complete within 1 second for 1000 items")
        }

        @Test
        @DisplayName("Enum converters should handle multiple conversions efficiently")
        fun enumConvertersShouldHandleMultipleConversionsEfficiently() {
            // Given
            val transactionTypes = (1..1000).map { TransactionType.values()[it % TransactionType.values().size] }
            val recurringPeriods = (1..1000).map { RecurringPeriod.values()[it % RecurringPeriod.values().size] }
            val userRoles = (1..1000).map { UserRole.values()[it % UserRole.values().size] }
            val userStatuses = (1..1000).map { UserStatus.values()[it % UserStatus.values().size] }

            // When & Then
            val startTime = System.currentTimeMillis()
            
            transactionTypes.forEach { type ->
                val stringValue = transactionTypeConverter.fromTransactionType(type)
                val convertedType = transactionTypeConverter.toTransactionType(stringValue)
                assertEquals(type, convertedType)
            }
            
            recurringPeriods.forEach { period ->
                val stringValue = recurringPeriodConverter.fromRecurringPeriod(period)
                val convertedPeriod = recurringPeriodConverter.toRecurringPeriod(stringValue)
                assertEquals(period, convertedPeriod)
            }
            
            userRoles.forEach { role ->
                val stringValue = userRoleConverter.fromUserRole(role)
                val convertedRole = userRoleConverter.toUserRole(stringValue)
                assertEquals(role, convertedRole)
            }
            
            userStatuses.forEach { status ->
                val stringValue = userStatusConverter.fromUserStatus(status)
                val convertedStatus = userStatusConverter.toUserStatus(stringValue)
                assertEquals(status, convertedStatus)
            }
            
            val endTime = System.currentTimeMillis()
            
            // Performance assertion - should complete within reasonable time
            assertTrue(endTime - startTime < 1000, "Enum conversion should complete within 1 second for 4000 items")
        }
    }
}
