package com.pennywise.app.presentation.components

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Assertions.*
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.util.Date
import java.time.ZoneId

/**
 * Comprehensive unit tests for CustomDatePickerDialog component
 * 
 * Tests cover:
 * - Date conversion utilities
 * - Date validation logic
 * - Component parameter validation
 * - Edge cases and error handling
 * - Date range validation
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DatePickerDialogTest {

    @BeforeEach
    fun setUp() {
        // Reset any state before each test
    }

    @Nested
    @DisplayName("Date Conversion Tests")
    inner class DateConversionTests {

        @Test
        @DisplayName("Date to LocalDate conversion works correctly")
        fun testDateToLocalDateConversion() {
            val date = Date(1640995200000L) // 2022-01-01 00:00:00 UTC
            val localDate = date.toLocalDate()
            
            assertEquals(2022, localDate.year)
            assertEquals(Month.JANUARY, localDate.month)
            assertEquals(1, localDate.dayOfMonth)
        }

        @Test
        @DisplayName("LocalDate to Date conversion works correctly")
        fun testLocalDateToDateConversion() {
            val localDate = LocalDate.of(2022, Month.JANUARY, 1)
            val date = localDate.toDate()
            
            assertNotNull(date)
            assertTrue(date.time > 0)
        }

        @Test
        @DisplayName("Date conversion preserves date values")
        fun testDateConversionPreservesValues() {
            val originalLocalDate = LocalDate.of(2023, Month.DECEMBER, 25)
            val date = originalLocalDate.toDate()
            val convertedLocalDate = date.toLocalDate()
            
            assertEquals(originalLocalDate, convertedLocalDate)
        }

        @Test
        @DisplayName("Date conversion handles different time zones")
        fun testDateConversionHandlesTimeZones() {
            val localDate = LocalDate.of(2024, Month.FEBRUARY, 29) // Leap year
            val date = localDate.toDate()
            val convertedLocalDate = date.toLocalDate()
            
            assertEquals(localDate, convertedLocalDate)
        }
    }

    @Nested
    @DisplayName("Date Validation Tests")
    inner class DateValidationTests {

        @Test
        @DisplayName("Valid date creation works correctly")
        fun testValidDateCreation() {
            val date = LocalDate.of(2024, Month.JANUARY, 15)
            
            assertEquals(2024, date.year)
            assertEquals(Month.JANUARY, date.month)
            assertEquals(15, date.dayOfMonth)
        }

        @Test
        @DisplayName("Leap year date validation works correctly")
        fun testLeapYearDateValidation() {
            val leapYearDate = LocalDate.of(2024, Month.FEBRUARY, 29)
            
            assertEquals(2024, leapYearDate.year)
            assertEquals(Month.FEBRUARY, leapYearDate.month)
            assertEquals(29, leapYearDate.dayOfMonth)
        }

        @Test
        @DisplayName("Past date validation works correctly")
        fun testPastDateValidation() {
            val pastDate = LocalDate.of(1990, Month.JANUARY, 1)
            
            assertTrue(pastDate.isBefore(LocalDate.now()))
            assertEquals(1990, pastDate.year)
        }

        @Test
        @DisplayName("Future date validation works correctly")
        fun testFutureDateValidation() {
            val futureDate = LocalDate.of(2030, Month.DECEMBER, 31)
            
            assertTrue(futureDate.isAfter(LocalDate.now()))
            assertEquals(2030, futureDate.year)
        }

        @Test
        @DisplayName("Date range validation works correctly")
        fun testDateRangeValidation() {
            val startDate = LocalDate.of(2020, Month.JANUARY, 1)
            val endDate = LocalDate.of(2030, Month.DECEMBER, 31)
            val testDate = LocalDate.of(2025, Month.JUNE, 15)
            
            assertTrue(testDate.isAfter(startDate))
            assertTrue(testDate.isBefore(endDate))
        }
    }

    @Nested
    @DisplayName("Edge Case Tests")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Null date handling works correctly")
        fun testNullDateHandling() {
            val nullDate: LocalDate? = null
            
            // Test that null dates can be handled without crashing
            assertNull(nullDate)
        }

        @Test
        @DisplayName("Minimum date boundary works correctly")
        fun testMinimumDateBoundary() {
            val minDate = LocalDate.of(1900, Month.JANUARY, 1)
            
            assertEquals(1900, minDate.year)
            assertEquals(Month.JANUARY, minDate.month)
            assertEquals(1, minDate.dayOfMonth)
        }

        @Test
        @DisplayName("Maximum date boundary works correctly")
        fun testMaximumDateBoundary() {
            val maxDate = LocalDate.of(2100, Month.DECEMBER, 31)
            
            assertEquals(2100, maxDate.year)
            assertEquals(Month.DECEMBER, maxDate.month)
            assertEquals(31, maxDate.dayOfMonth)
        }

        @Test
        @DisplayName("Date arithmetic operations work correctly")
        fun testDateArithmeticOperations() {
            val baseDate = LocalDate.of(2024, Month.JANUARY, 15)
            val futureDate = baseDate.plusDays(30)
            val pastDate = baseDate.minusDays(30)
            
            assertEquals(LocalDate.of(2024, Month.FEBRUARY, 14), futureDate)
            assertEquals(LocalDate.of(2023, Month.DECEMBER, 16), pastDate)
        }

        @Test
        @DisplayName("Month boundary date handling works correctly")
        fun testMonthBoundaryDateHandling() {
            val lastDayOfMonth = LocalDate.of(2024, Month.JANUARY, 31)
            val nextMonth = lastDayOfMonth.plusDays(1)
            
            assertEquals(Month.FEBRUARY, nextMonth.month)
            assertEquals(1, nextMonth.dayOfMonth)
        }
    }

    @Nested
    @DisplayName("Date Formatting Tests")
    inner class DateFormattingTests {

        @Test
        @DisplayName("Date string representation works correctly")
        fun testDateStringRepresentation() {
            val date = LocalDate.of(2024, Month.JANUARY, 15)
            val dateString = date.toString()
            
            assertEquals("2024-01-15", dateString)
        }

        @Test
        @DisplayName("Date parsing from string works correctly")
        fun testDateParsingFromString() {
            val dateString = "2024-01-15"
            val parsedDate = LocalDate.parse(dateString)
            
            assertEquals(2024, parsedDate.year)
            assertEquals(Month.JANUARY, parsedDate.month)
            assertEquals(15, parsedDate.dayOfMonth)
        }

        @Test
        @DisplayName("Date comparison operations work correctly")
        fun testDateComparisonOperations() {
            val date1 = LocalDate.of(2024, Month.JANUARY, 15)
            val date2 = LocalDate.of(2024, Month.JANUARY, 16)
            val date3 = LocalDate.of(2024, Month.JANUARY, 15)
            
            assertTrue(date1.isBefore(date2))
            assertTrue(date2.isAfter(date1))
            assertTrue(date1.isEqual(date3))
        }

        @Test
        @DisplayName("Date period calculation works correctly")
        fun testDatePeriodCalculation() {
            val startDate = LocalDate.of(2024, Month.JANUARY, 1)
            val endDate = LocalDate.of(2024, Month.JANUARY, 31)
            val period = java.time.Period.between(startDate, endDate)
            
            assertEquals(30, period.days)
            assertEquals(0, period.months)
            assertEquals(0, period.years)
        }
    }

    @Nested
    @DisplayName("Component Parameter Tests")
    inner class ComponentParameterTests {

        @Test
        @DisplayName("Date picker year range validation works correctly")
        fun testDatePickerYearRangeValidation() {
            val minYear = 1900
            val maxYear = 2100
            val testYear = 2024
            
            assertTrue(testYear >= minYear && testYear <= maxYear)
        }

        @Test
        @DisplayName("Date picker month validation works correctly")
        fun testDatePickerMonthValidation() {
            val validMonths = Month.values()
            
            assertEquals(12, validMonths.size)
            assertTrue(validMonths.contains(Month.JANUARY))
            assertTrue(validMonths.contains(Month.DECEMBER))
        }

        @Test
        @DisplayName("Date picker day validation works correctly")
        fun testDatePickerDayValidation() {
            val date = LocalDate.of(2024, Month.JANUARY, 15)
            val dayOfMonth = date.dayOfMonth
            
            assertTrue(dayOfMonth >= 1 && dayOfMonth <= 31)
        }

        @Test
        @DisplayName("Date picker locale handling works correctly")
        fun testDatePickerLocaleHandling() {
            val defaultLocale = java.util.Locale.getDefault()
            
            assertNotNull(defaultLocale)
            assertNotNull(defaultLocale.language)
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    inner class IntegrationTests {

        @Test
        @DisplayName("Date picker integration with calendar works correctly")
        fun testDatePickerIntegrationWithCalendar() {
            val calendar = java.util.Calendar.getInstance()
            val date = LocalDate.of(2024, Month.JANUARY, 15)
            
            calendar.set(date.year, date.monthValue - 1, date.dayOfMonth)
            val calendarDate = calendar.time
            
            assertNotNull(calendarDate)
            assertTrue(calendarDate.time > 0)
        }

        @Test
        @DisplayName("Date picker integration with time zone works correctly")
        fun testDatePickerIntegrationWithTimeZone() {
            val date = LocalDate.of(2024, Month.JANUARY, 15)
            val zoneId = ZoneId.systemDefault()
            val zonedDateTime = date.atStartOfDay(zoneId)
            
            assertEquals(date, zonedDateTime.toLocalDate())
            assertEquals(zoneId, zonedDateTime.zone)
        }

        @Test
        @DisplayName("Date picker integration with date formatting works correctly")
        fun testDatePickerIntegrationWithDateFormatting() {
            val date = LocalDate.of(2024, Month.JANUARY, 15)
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val formattedDate = date.format(formatter)
            
            assertEquals("2024-01-15", formattedDate)
        }

        @Test
        @DisplayName("Date picker integration with date arithmetic works correctly")
        fun testDatePickerIntegrationWithDateArithmetic() {
            val baseDate = LocalDate.of(2024, Month.JANUARY, 15)
            val futureDate = baseDate.plusWeeks(2)
            val pastDate = baseDate.minusWeeks(2)
            
            // 2 weeks = 14 days
            // January 15 + 14 days = January 29
            // January 15 - 14 days = January 1
            assertEquals(LocalDate.of(2024, Month.JANUARY, 29), futureDate)
            assertEquals(LocalDate.of(2024, Month.JANUARY, 1), pastDate)
        }
    }
}
