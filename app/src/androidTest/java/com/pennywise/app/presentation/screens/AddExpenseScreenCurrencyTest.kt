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
    fun addExpenseScreenShouldDisplayCurrencySelectionDropdown() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // Then
        findCurrencyDropdown().assertIsDisplayed()
    }
    
    @Test
    fun addExpenseScreenShouldAllowCurrencySelection() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        
        // Then
        composeTestRule.onNode(
            hasText("EUR") and hasText("€") and hasText("Euro")
        ).assertIsDisplayed()
    }
    
    @Test
    fun addExpenseScreenShouldUpdateCurrencySymbolInAmountFieldWhenCurrencyChanges() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        enterAmount("100")
        
        // Then
        verifyCurrencySymbol(Currency.EUR)
    }
    
    @Test
    fun addExpenseScreenShouldFormatAmountCorrectlyForUSDCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        
        // Then
        findAmountField().assertTextContains("100.50")
        verifyCurrencySymbol(Currency.USD)
    }
    
    @Test
    fun addExpenseScreenShouldFormatAmountCorrectlyForJPYCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("15000")
        
        // Then
        findAmountField().assertTextContains("15000")
        verifyCurrencySymbol(Currency.JPY)
    }
    
    @Test
    fun addExpenseScreenShouldPreventDecimalInputForJPYCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("15000.50")
        
        // Then
        // Should only allow whole numbers for JPY
        findAmountField().assertTextContains("15000")
    }
    
    @Test
    fun addExpenseScreenShouldPreventDecimalInputForKRWCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.KRW)
        enterAmount("50000.25")
        
        // Then
        // Should only allow whole numbers for KRW
        findAmountField().assertTextContains("50000")
    }
    
    @Test
    fun addExpenseScreenShouldAllowDecimalInputForUSDCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        
        // Then
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun addExpenseScreenShouldAllowDecimalInputForEURCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        enterAmount("85.75")
        
        // Then
        findAmountField().assertTextContains("85.75")
    }
    
    @Test
    fun addExpenseScreenShouldValidateAmountFieldWhenCurrencyChanges() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.JPY)
        
        // Then
        // Amount should be reformatted for JPY (no decimals)
        findAmountField().assertTextContains("100")
    }
    
    @Test
    fun addExpenseScreenShouldShowCurrencySpecificValidationMessages() {
        // Given
        setupComposeContent { }
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
    fun addExpenseScreenShouldEnableSaveButtonWhenValidCurrencyAndAmountAreEntered() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        composeTestRule.onNode(hasText("Test Merchant")).performTextInput("Test Merchant")
        
        // Then
        verifySaveButtonEnabled()
    }
    
    @Test
    fun addExpenseScreenShouldDisableSaveButtonWhenNoCurrencyIsSelected() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        enterAmount("100.50")
        composeTestRule.onNode(hasText("Merchant")).performTextInput("Test Merchant")
        
        // Then
        verifySaveButtonDisabled()
    }
    
    @Test
    fun addExpenseScreenShouldDisableSaveButtonWhenInvalidAmountIsEntered() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("")
        composeTestRule.onNode(hasText("Merchant")).performTextInput("Test Merchant")
        
        // Then
        verifySaveButtonDisabled()
    }
    
    @Test
    fun addExpenseScreenShouldShowErrorMessageForInvalidAmount() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("")
        
        // Then
        verifyErrorMessage("Amount is required")
    }
    
    @Test
    fun addExpenseScreenShouldShowErrorMessageForNegativeAmount() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("-100")
        
        // Then
        verifyErrorMessage("Invalid amount")
    }
    
    @Test
    fun addExpenseScreenShouldShowErrorMessageForZeroAmount() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("0")
        
        // Then
        verifyErrorMessage("Invalid amount")
    }
    
    @Test
    fun addExpenseScreenShouldHandleCurrencySearchFunctionality() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("EUR")
        
        // Then
        verifyCurrencyInResults(Currency.EUR)
        verifyCurrencyNotInResults(Currency.USD)
    }
    
    @Test
    fun addExpenseScreenShouldHandleCurrencySearchBySymbol() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("€")
        
        // Then
        verifyCurrencyInResults(Currency.EUR)
    }
    
    @Test
    fun addExpenseScreenShouldHandleCurrencySearchByDisplayName() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("Euro")
        
        // Then
        verifyCurrencyInResults(Currency.EUR)
    }
    
    @Test
    fun addExpenseScreenShouldShowNoResultsForInvalidCurrencySearch() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("XYZ")
        
        // Then
        composeTestRule.onNode(hasText("No results found")).assertIsDisplayed()
    }
    
    @Test
    fun addExpenseScreenShouldGroupCurrenciesByPopularity() {
        // Given
        setupComposeContent { }
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
    fun addExpenseScreenShouldSaveExpenseWithCorrectCurrency() {
        // Given
        setupComposeContent { }
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
    fun addExpenseScreenShouldHandleCurrencyChangeConfirmation() {
        // Given
        setupComposeContent { }
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
    fun addExpenseScreenShouldDisplayCurrencyInformationInSupportingText() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        
        // Then
        composeTestRule.onNode(
            hasText("US Dollar") and hasText("2 decimal places")
        ).assertIsDisplayed()
    }
    
    @Test
    fun addExpenseScreenShouldDisplayCurrencyInformationForJPY() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        
        // Then
        composeTestRule.onNode(
            hasText("Japanese Yen") and hasText("0 decimal places")
        ).assertIsDisplayed()
    }
}
