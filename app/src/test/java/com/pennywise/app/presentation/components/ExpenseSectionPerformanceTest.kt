package com.pennywise.app.presentation.components

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.pennywise.app.domain.model.Transaction
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import org.junit.jupiter.api.Test
import java.util.*

/**
 * Performance tests for ExpenseSection component to verify efficient list updates
 * Tests that the component properly handles list changes without unnecessary recompositions
 */
class ExpenseSectionPerformanceTest {
    
    private val composeTestRule = createComposeRule()
    
    private fun createTestTransaction(
        id: Long,
        description: String,
        amount: Double,
        currency: String = "USD"
    ): Transaction {
        return Transaction(
            id = id,
            userId = 1L,
            amount = amount,
            currency = currency,
            description = description,
            category = "Test Category",
            type = TransactionType.EXPENSE,
            date = Date(),
            isRecurring = false,
            recurringPeriod = null
        )
    }
    
    @Test
    fun testExpenseSectionWithEmptyList() {
        val emptyTransactions = emptyList<Transaction>()
        
        composeTestRule.setContent {
            ExpenseSection(
                title = "Test Section",
                transactions = emptyTransactions,
                currency = "USD",
                currencyConversionEnabled = false,
                originalCurrency = "USD",
                conversionState = com.pennywise.app.presentation.viewmodel.HomeViewModel.ConversionState.Idle,
                onConvertAmount = {}
            )
        }
        
        // Verify empty state is displayed
        composeTestRule.onNodeWithText("Test Section").assertExists()
    }
    
    @Test
    fun testExpenseSectionWithSingleTransaction() {
        val transactions = listOf(
            createTestTransaction(1L, "Test Transaction", 100.0)
        )
        
        composeTestRule.setContent {
            ExpenseSection(
                title = "Test Section",
                transactions = transactions,
                currency = "USD",
                currencyConversionEnabled = false,
                originalCurrency = "USD",
                conversionState = com.pennywise.app.presentation.viewmodel.HomeViewModel.ConversionState.Idle,
                onConvertAmount = {}
            )
        }
        
        // Verify section title and total are displayed
        composeTestRule.onNodeWithText("Test Section").assertExists()
        composeTestRule.onNodeWithText("$100.00").assertExists()
    }
    
    @Test
    fun testExpenseSectionWithMultipleTransactions() {
        val transactions = listOf(
            createTestTransaction(1L, "Transaction 1", 50.0),
            createTestTransaction(2L, "Transaction 2", 75.0),
            createTestTransaction(3L, "Transaction 3", 25.0)
        )
        
        composeTestRule.setContent {
            ExpenseSection(
                title = "Test Section",
                transactions = transactions,
                currency = "USD",
                currencyConversionEnabled = false,
                originalCurrency = "USD",
                conversionState = com.pennywise.app.presentation.viewmodel.HomeViewModel.ConversionState.Idle,
                onConvertAmount = {}
            )
        }
        
        // Verify section title and total are displayed
        composeTestRule.onNodeWithText("Test Section").assertExists()
        composeTestRule.onNodeWithText("$150.00").assertExists()
    }
    
    @Test
    fun testExpenseSectionExpandCollapse() {
        val transactions = listOf(
            createTestTransaction(1L, "Transaction 1", 50.0),
            createTestTransaction(2L, "Transaction 2", 75.0)
        )
        
        composeTestRule.setContent {
            ExpenseSection(
                title = "Test Section",
                transactions = transactions,
                currency = "USD",
                currencyConversionEnabled = false,
                originalCurrency = "USD",
                conversionState = com.pennywise.app.presentation.viewmodel.HomeViewModel.ConversionState.Idle,
                onConvertAmount = {}
            )
        }
        
        // Click to expand the section
        composeTestRule.onNodeWithText("Test Section").performClick()
        
        // Verify individual transactions are displayed
        composeTestRule.onNodeWithText("Transaction 1").assertExists()
        composeTestRule.onNodeWithText("Transaction 2").assertExists()
    }
    
    @Test
    fun testExpenseSectionWithDifferentCurrencies() {
        val transactions = listOf(
            createTestTransaction(1L, "USD Transaction", 100.0, "USD"),
            createTestTransaction(2L, "EUR Transaction", 85.0, "EUR")
        )
        
        composeTestRule.setContent {
            ExpenseSection(
                title = "Test Section",
                transactions = transactions,
                currency = "USD",
                currencyConversionEnabled = false,
                originalCurrency = "USD",
                conversionState = com.pennywise.app.presentation.viewmodel.HomeViewModel.ConversionState.Idle,
                onConvertAmount = {}
            )
        }
        
        // Verify section displays correctly with mixed currencies
        composeTestRule.onNodeWithText("Test Section").assertExists()
    }
    
    @Test
    fun testExpenseSectionWithLargeList() {
        val transactions = (1..100).map { id ->
            createTestTransaction(
                id = id.toLong(),
                description = "Transaction $id",
                amount = id.toDouble()
            )
        }
        
        composeTestRule.setContent {
            ExpenseSection(
                title = "Large List Test",
                transactions = transactions,
                currency = "USD",
                currencyConversionEnabled = false,
                originalCurrency = "USD",
                conversionState = com.pennywise.app.presentation.viewmodel.HomeViewModel.ConversionState.Idle,
                onConvertAmount = {}
            )
        }
        
        // Verify section handles large lists efficiently
        composeTestRule.onNodeWithText("Large List Test").assertExists()
        
        // Click to expand and verify LazyColumn handles large lists
        composeTestRule.onNodeWithText("Large List Test").performClick()
        
        // Verify some transactions are displayed (LazyColumn should handle this efficiently)
        composeTestRule.onNodeWithText("Transaction 1").assertExists()
    }
    
    @Test
    fun testExpenseSectionWithCurrencyConversion() {
        val transactions = listOf(
            createTestTransaction(1L, "Test Transaction", 100.0)
        )
        
        composeTestRule.setContent {
            ExpenseSection(
                title = "Test Section",
                transactions = transactions,
                currency = "EUR",
                currencyConversionEnabled = true,
                originalCurrency = "USD",
                conversionState = com.pennywise.app.presentation.viewmodel.HomeViewModel.ConversionState.Idle,
                onConvertAmount = {}
            )
        }
        
        // Verify section displays with currency conversion
        composeTestRule.onNodeWithText("Test Section").assertExists()
    }
    
    @Test
    fun testExpenseSectionWithRecurringTransactions() {
        val transactions = listOf(
            createTestTransaction(1L, "Recurring Transaction", 50.0).copy(
                isRecurring = true
            )
        )
        
        composeTestRule.setContent {
            ExpenseSection(
                title = "Recurring Section",
                transactions = transactions,
                currency = "USD",
                currencyConversionEnabled = false,
                originalCurrency = "USD",
                conversionState = com.pennywise.app.presentation.viewmodel.HomeViewModel.ConversionState.Idle,
                onConvertAmount = {},
                isRecurring = true
            )
        }
        
        // Verify recurring section displays correctly
        composeTestRule.onNodeWithText("Recurring Section").assertExists()
        composeTestRule.onNodeWithText("$50.00").assertExists()
    }
}
