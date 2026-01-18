package com.pennywise.app.domain.util

import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.PaymentMethodConfig
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

class BillingCycleUtilsTest {
    
    private fun localDateToDate(localDate: LocalDate): Date {
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }
    
    @Test
    fun `calculateBillingCycle - transaction before withdraw day belongs to current month cycle`() {
        // Transaction on Sep-15 with withdrawDay=20 should belong to Sep-20 to Oct-19 cycle
        val transactionDate = localDateToDate(LocalDate.of(2024, 9, 15))
        val withdrawDay = 20
        
        val (cycleStart, cycleEnd) = BillingCycleUtils.calculateBillingCycle(transactionDate, withdrawDay)
        
        val expectedStart = localDateToDate(LocalDate.of(2024, 8, 21))
        val expectedEnd = localDateToDate(LocalDate.of(2024, 9, 20))
        
        assertEquals("Cycle start should be Aug 21", expectedStart.toString(), cycleStart.toString())
        assertEquals("Cycle end should be Sep 20", expectedEnd.toString(), cycleEnd.toString())
    }
    
    @Test
    fun `calculateBillingCycle - transaction after withdraw day belongs to next month cycle`() {
        // Transaction on Sep-25 with withdrawDay=20 should belong to Oct-20 to Nov-19 cycle
        val transactionDate = localDateToDate(LocalDate.of(2024, 9, 25))
        val withdrawDay = 20
        
        val (cycleStart, cycleEnd) = BillingCycleUtils.calculateBillingCycle(transactionDate, withdrawDay)
        
        val expectedStart = localDateToDate(LocalDate.of(2024, 9, 21))
        val expectedEnd = localDateToDate(LocalDate.of(2024, 10, 20))
        
        assertEquals("Cycle start should be Sep 21", expectedStart.toString(), cycleStart.toString())
        assertEquals("Cycle end should be Oct 20", expectedEnd.toString(), cycleEnd.toString())
    }
    
    @Test
    fun `calculateBillingCycle - transaction on withdraw day belongs to current month cycle`() {
        // Transaction on Sep-20 with withdrawDay=20 should belong to Sep-20 to Oct-19 cycle (inclusive)
        val transactionDate = localDateToDate(LocalDate.of(2024, 9, 20))
        val withdrawDay = 20
        
        val (cycleStart, cycleEnd) = BillingCycleUtils.calculateBillingCycle(transactionDate, withdrawDay)
        
        val expectedStart = localDateToDate(LocalDate.of(2024, 8, 21))
        val expectedEnd = localDateToDate(LocalDate.of(2024, 9, 20))
        
        assertEquals("Cycle start should be Aug 21", expectedStart.toString(), cycleStart.toString())
        assertEquals("Cycle end should be Sep 20", expectedEnd.toString(), cycleEnd.toString())
    }
    
    @Test
    fun `calculateBillingCycle - february leap year edge case`() {
        // Transaction in Feb 2024 (leap year) with withdrawDay=29
        val transactionDate = localDateToDate(LocalDate.of(2024, 2, 15))
        val withdrawDay = 29
        
        val (cycleStart, cycleEnd) = BillingCycleUtils.calculateBillingCycle(transactionDate, withdrawDay)
        
        val expectedStart = localDateToDate(LocalDate.of(2024, 1, 30))
        val expectedEnd = localDateToDate(LocalDate.of(2024, 2, 29))
        
        assertEquals("Cycle start should be Jan 30", expectedStart.toString(), cycleStart.toString())
        assertEquals("Cycle end should be Feb 29", expectedEnd.toString(), cycleEnd.toString())
    }
    
    @Test
    fun `calculateBillingCycle - february non-leap year edge case`() {
        // Transaction in Feb 2023 (non-leap year) with withdrawDay=29, should clamp to 28
        val transactionDate = localDateToDate(LocalDate.of(2023, 2, 15))
        val withdrawDay = 29
        
        val (cycleStart, cycleEnd) = BillingCycleUtils.calculateBillingCycle(transactionDate, withdrawDay)
        
        val expectedStart = localDateToDate(LocalDate.of(2023, 1, 29))
        val expectedEnd = localDateToDate(LocalDate.of(2023, 2, 28))
        
        assertEquals("Cycle start should be Jan 29", expectedStart.toString(), cycleStart.toString())
        assertEquals("Cycle end should be Feb 28", expectedEnd.toString(), cycleEnd.toString())
    }
    
    @Test
    fun `calculateBillingCycle - month with 30 days and withdrawDay 31`() {
        // Transaction in Apr 2024 with withdrawDay=31, should clamp to 30
        val transactionDate = localDateToDate(LocalDate.of(2024, 4, 15))
        val withdrawDay = 31
        
        val (cycleStart, cycleEnd) = BillingCycleUtils.calculateBillingCycle(transactionDate, withdrawDay)
        
        val expectedStart = localDateToDate(LocalDate.of(2024, 3, 31))
        val expectedEnd = localDateToDate(LocalDate.of(2024, 4, 30))
        
        assertEquals("Cycle start should be Mar 31", expectedStart.toString(), cycleStart.toString())
        assertEquals("Cycle end should be Apr 30", expectedEnd.toString(), cycleEnd.toString())
    }
    
    @Test
    fun `isCurrentDateInBillingCycle - current date within cycle returns true`() {
        val transactionDate = localDateToDate(LocalDate.of(2024, 10, 15))
        val currentDate = localDateToDate(LocalDate.of(2024, 10, 28))
        val withdrawDay = 9
        
        // Transaction on Oct-15 with cycle 10-9 means Oct-10 to Nov-9
        // Current date Oct-28 should be within this cycle
        val result = BillingCycleUtils.isCurrentDateInBillingCycle(transactionDate, withdrawDay, currentDate)
        
        assertTrue("Oct-28 should be within Oct-10 to Nov-9 cycle", result)
    }
    
    @Test
    fun `isCurrentDateInBillingCycle - current date outside cycle returns false`() {
        val transactionDate = localDateToDate(LocalDate.of(2024, 9, 15))
        val currentDate = localDateToDate(LocalDate.of(2024, 10, 28))
        val withdrawDay = 9
        
        // Transaction on Sep-15 with cycle 10-9 means Sep-10 to Oct-9
        // Current date Oct-28 should be outside this cycle
        val result = BillingCycleUtils.isCurrentDateInBillingCycle(transactionDate, withdrawDay, currentDate)
        
        assertFalse("Oct-28 should be outside Sep-10 to Oct-9 cycle", result)
    }
    
    @Test
    fun `getTransactionBillingCycle - credit card with config returns cycle`() {
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Test",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = localDateToDate(LocalDate.of(2024, 10, 15)),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentMethodConfigId = 1L
        )
        
        val config = PaymentMethodConfig(
            id = 1L,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            alias = "Test Card",
            withdrawDay = 9
        )
        
        val result = BillingCycleUtils.getTransactionBillingCycle(transaction, listOf(config))
        
        assertTrue("Should return billing cycle for credit card with config", result != null)
    }
    
    @Test
    fun `getTransactionBillingCycle - cash transaction returns null`() {
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Test",
            category = "Food", 
            type = TransactionType.EXPENSE,
            date = localDateToDate(LocalDate.of(2024, 10, 15)),
            paymentMethod = PaymentMethod.CASH
        )
        
        val result = BillingCycleUtils.getTransactionBillingCycle(transaction, emptyList())
        
        assertEquals("Cash transactions should not have billing cycles", null, result)
    }
    
    @Test
    fun `getTransactionBillingCycle - credit card without config returns null`() {
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Test",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = localDateToDate(LocalDate.of(2024, 10, 15)),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentMethodConfigId = null
        )
        
        val result = BillingCycleUtils.getTransactionBillingCycle(transaction, emptyList())
        
        assertEquals("Credit card without config should not have billing cycle", null, result)
    }
    
    @Test
    fun `shouldIncludeTransactionInCurrentTotals - case 1 - oct 28 transaction with cycle 10-9 should be included`() {
        // Case 1: Transaction on Oct-28 with cycle 10-9 should appear in October total
        // since current date (Oct-28) falls within that cycle (Oct-10 to Nov-9)
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Test Purchase",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = localDateToDate(LocalDate.of(2024, 10, 28)),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentMethodConfigId = 1L
        )
        
        val config = PaymentMethodConfig(
            id = 1L,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            alias = "Test Card",
            withdrawDay = 9
        )
        
        val currentDate = localDateToDate(LocalDate.of(2024, 10, 28))
        
        val result = BillingCycleUtils.shouldIncludeTransactionInCurrentTotals(
            transaction, listOf(config), currentDate
        )
        
        assertTrue("Oct-28 transaction with cycle 10-9 should be included when current date is Oct-28", result)
    }
    
    @Test
    fun `shouldIncludeTransactionInCurrentTotals - case 2 - sep 15 transaction should not appear in october`() {
        // Case 2: Transaction from Sep-15 should NOT appear in October total
        // since Sep-15 doesn't fall within any billing cycle containing current date (Oct-28)
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Test Purchase",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = localDateToDate(LocalDate.of(2024, 9, 15)),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentMethodConfigId = 1L
        )
        
        val config = PaymentMethodConfig(
            id = 1L,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            alias = "Test Card",
            withdrawDay = 9
        )
        
        val currentDate = localDateToDate(LocalDate.of(2024, 10, 28))
        
        val result = BillingCycleUtils.shouldIncludeTransactionInCurrentTotals(
            transaction, listOf(config), currentDate
        )
        
        assertFalse("Sep-15 transaction should not appear when current date is Oct-28", result)
    }
    
    @Test
    fun `shouldIncludeTransactionInCurrentTotals - cash transaction uses billing date logic`() {
        // Cash transactions should use the existing billing date logic (same month)
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Cash Purchase", 
            category = "Food",
            type = TransactionType.EXPENSE,
            date = localDateToDate(LocalDate.of(2024, 10, 15)),
            paymentMethod = PaymentMethod.CASH
        )
        
        val currentDate = localDateToDate(LocalDate.of(2024, 10, 28))
        
        val result = BillingCycleUtils.shouldIncludeTransactionInCurrentTotals(
            transaction, emptyList(), currentDate
        )
        
        assertTrue("Cash transaction in same month should be included", result)
    }
    
    @Test
    fun `shouldIncludeTransactionInCurrentTotals - cash transaction different month excluded`() {
        // Cash transaction from different month should be excluded
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Cash Purchase",
            category = "Food", 
            type = TransactionType.EXPENSE,
            date = localDateToDate(LocalDate.of(2024, 9, 15)),
            paymentMethod = PaymentMethod.CASH
        )
        
        val currentDate = localDateToDate(LocalDate.of(2024, 10, 28))
        
        val result = BillingCycleUtils.shouldIncludeTransactionInCurrentTotals(
            transaction, emptyList(), currentDate
        )
        
        assertFalse("Cash transaction from different month should be excluded", result)
    }
    
    @Test
    fun `shouldIncludeTransactionInCurrentTotals - credit card without withdraw day uses billing date logic`() {
        // Credit card transaction without withdraw day should fall back to billing date logic
        val transaction = Transaction(
            id = 1,
            amount = 100.0,
            description = "Credit Purchase",
            category = "Food",
            type = TransactionType.EXPENSE,
            date = localDateToDate(LocalDate.of(2024, 10, 15)),
            paymentMethod = PaymentMethod.CREDIT_CARD,
            paymentMethodConfigId = 1L,
            billingDelayDays = 30
        )
        
        val config = PaymentMethodConfig(
            id = 1L,
            paymentMethod = PaymentMethod.CREDIT_CARD,
            alias = "Test Card",
            withdrawDay = null // No withdraw day configured
        )
        
        val currentDate = localDateToDate(LocalDate.of(2024, 11, 20))
        
        val result = BillingCycleUtils.shouldIncludeTransactionInCurrentTotals(
            transaction, listOf(config), currentDate
        )
        
        assertTrue("Credit card without withdraw day should use billing date logic", result)
    }
}







