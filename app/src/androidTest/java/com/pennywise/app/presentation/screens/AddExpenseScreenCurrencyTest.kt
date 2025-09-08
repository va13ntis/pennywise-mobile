package com.pennywise.app.presentation.screens

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.testutils.BaseCurrencyUiTest
import com.pennywise.app.testutils.CurrencyTestFixtures
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for currency selection functionality in AddExpenseScreen
 * Tests currency selection component behavior, amount field formatting, and validation
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class AddExpenseScreenCurrencyTest : BaseCurrencyUiTest() {
    
    @Test
    fun `add expense screen should display currency selection dropdown`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // Then
        findCurrencyDropdown().assertIsDisplayed()
    }
    
    @Test
    fun `add expense screen should allow currency selection`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        
        // Then
        composeTestRule.onNode(
            hasText("EUR") and hasText("€") and hasText("Euro")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `add expense screen should update currency symbol in amount field when currency changes`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        enterAmount("100")
        
        // Then
        verifyCurrencySymbol(Currency.EUR)
    }
    
    @Test
    fun `add expense screen should format amount correctly for USD currency`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        
        // Then
        findAmountField().assertTextContains("100.50")
        verifyCurrencySymbol(Currency.USD)
    }
    
    @Test
    fun `add expense screen should format amount correctly for JPY currency`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("15000")
        
        // Then
        findAmountField().assertTextContains("15000")
        verifyCurrencySymbol(Currency.JPY)
    }
    
    @Test
    fun `add expense screen should prevent decimal input for JPY currency`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("15000.50")
        
        // Then
        // Should only allow whole numbers for JPY
        findAmountField().assertTextContains("15000")
        findAmountField().assertTextDoesNotContain("15000.50")
    }
    
    @Test
    fun `add expense screen should prevent decimal input for KRW currency`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.KRW)
        enterAmount("50000.25")
        
        // Then
        // Should only allow whole numbers for KRW
        findAmountField().assertTextContains("50000")
        findAmountField().assertTextDoesNotContain("50000.25")
    }
    
    @Test
    fun `add expense screen should allow decimal input for USD currency`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        
        // Then
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun `add expense screen should allow decimal input for EUR currency`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        enterAmount("85.75")
        
        // Then
        findAmountField().assertTextContains("85.75")
    }
    
    @Test
    fun `add expense screen should validate amount field when currency changes`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.JPY)
        
        // Then
        // Amount should be reformatted for JPY (no decimals)
        findAmountField().assertTextContains("100")
        findAmountField().assertTextDoesNotContain("100.50")
    }
    
    @Test
    fun `add expense screen should show currency-specific validation messages`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("100.50")
        
        // Then
        // Should show validation message about JPY not using decimals
        composeTestRule.onNode(
            hasText("Japanese Yen") or hasText("decimal places") or hasText("whole numbers")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `add expense screen should enable save button when valid currency and amount are entered`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        composeTestRule.onNode(hasText("Test Merchant")).performTextInput("Test Merchant")
        
        // Then
        verifySaveButtonEnabled()
    }
    
    @Test
    fun `add expense screen should disable save button when no currency is selected`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        enterAmount("100.50")
        composeTestRule.onNode(hasText("Merchant")).performTextInput("Test Merchant")
        
        // Then
        verifySaveButtonDisabled()
    }
    
    @Test
    fun `add expense screen should disable save button when invalid amount is entered`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("")
        composeTestRule.onNode(hasText("Merchant")).performTextInput("Test Merchant")
        
        // Then
        verifySaveButtonDisabled()
    }
    
    @Test
    fun `add expense screen should show error message for invalid amount`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("")
        
        // Then
        verifyErrorMessage("Amount is required")
    }
    
    @Test
    fun `add expense screen should show error message for negative amount`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("-100")
        
        // Then
        verifyErrorMessage("Invalid amount")
    }
    
    @Test
    fun `add expense screen should show error message for zero amount`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("0")
        
        // Then
        verifyErrorMessage("Invalid amount")
    }
    
    @Test
    fun `add expense screen should handle currency search functionality`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("EUR")
        
        // Then
        verifyCurrencyInResults(Currency.EUR)
        verifyCurrencyNotInResults(Currency.USD)
    }
    
    @Test
    fun `add expense screen should handle currency search by symbol`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("€")
        
        // Then
        verifyCurrencyInResults(Currency.EUR)
    }
    
    @Test
    fun `add expense screen should handle currency search by display name`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("Euro")
        
        // Then
        verifyCurrencyInResults(Currency.EUR)
    }
    
    @Test
    fun `add expense screen should show no results for invalid currency search`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("XYZ")
        
        // Then
        composeTestRule.onNode(hasText("No results found")).assertIsDisplayed()
    }
    
    @Test
    fun `add expense screen should group currencies by popularity`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        
        // Then
        // Should show "Most Popular" section first
        composeTestRule.onNode(hasText("Most Popular")).assertIsDisplayed()
        
        // Should show "All Currencies" section
        composeTestRule.onNode(hasText("All Currencies")).assertIsDisplayed()
    }
    
    @Test
    fun `add expense screen should save expense with correct currency`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        enterAmount("100.50")
        composeTestRule.onNode(hasText("Merchant")).performTextInput("Test Merchant")
        performSave()
        
        // Then
        // Should navigate back or show success message
        composeTestRule.onNode(hasText("Add Expense")).assertDoesNotExist()
    }
    
    @Test
    fun `add expense screen should handle currency change confirmation`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.EUR)
        
        // Then
        // Should show confirmation dialog or update amount formatting
        verifyCurrencySymbol(Currency.EUR)
    }
    
    @Test
    fun `add expense screen should display currency information in supporting text`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        
        // Then
        composeTestRule.onNode(
            hasText("US Dollar") and hasText("2 decimal places")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `add expense screen should display currency information for JPY`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        
        // Then
        composeTestRule.onNode(
            hasText("Japanese Yen") and hasText("0 decimal places")
        ).assertIsDisplayed()
    }
}
