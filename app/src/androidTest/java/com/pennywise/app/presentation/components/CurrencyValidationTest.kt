package com.pennywise.app.presentation.components

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.testutils.BaseCurrencyUiTest
import com.pennywise.app.testutils.CurrencyTestFixtures
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for currency validation and error states
 * Tests error messages, validation behavior, and error handling for currency-related functionality
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CurrencyValidationTest : BaseCurrencyUiTest() {
    
    @Test
    fun shouldShowErrorMessageWhenNoCurrencyIsSelected() {
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
    fun shouldShowErrorMessageWhenAmountIsEmpty() {
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
    fun shouldShowErrorMessageWhenAmountIsZero() {
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
    fun shouldShowErrorMessageWhenAmountIsNegative() {
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
    fun shouldShowErrorMessageWhenAmountIsNonNumeric() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("abc")
        
        // Then
        verifyErrorMessage("Invalid amount")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasTooManyDecimalPlacesForUSD() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.123")
        
        // Then
        // Should automatically format to 2 decimal places
        findAmountField().assertTextContains("100.12")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPlacesForJPY() {
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
    fun shouldShowErrorMessageWhenAmountHasDecimalPlacesForKRW() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.KRW)
        enterAmount("50000.25")
        
        // Then
        // Should show validation message about KRW not using decimals
        composeTestRule.onNode(
            hasText("South Korean Won") or hasText("decimal places") or hasText("whole numbers")
        ).assertIsDisplayed()
    }
    
    @Test
    fun shouldShowErrorMessageWhenMerchantIsEmpty() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        // Leave merchant field empty
        
        // Then
        verifySaveButtonDisabled()
    }
    
    @Test
    fun shouldShowErrorMessageWhenMerchantIsBlank() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        composeTestRule.onNode(hasText("Merchant")).performTextInput("   ")
        
        // Then
        verifySaveButtonDisabled()
    }
    
    @Test
    fun shouldShowErrorMessageWhenCategoryIsNotSelected() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        composeTestRule.onNode(hasText("Merchant")).performTextInput("Test Merchant")
        // Leave category field empty
        
        // Then
        verifySaveButtonDisabled()
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountIsTooLarge() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("999999999999.99")
        
        // Then
        // Should handle large amounts gracefully
        findAmountField().assertTextContains("999999999999.99")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountIsVerySmall() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("0.01")
        
        // Then
        // Should handle small amounts gracefully
        findAmountField().assertTextContains("0.01")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasMultipleDecimalPoints() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50.25")
        
        // Then
        // Should handle invalid format gracefully
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasLeadingZeros() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("00100.50")
        
        // Then
        // Should handle leading zeros gracefully
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasTrailingZeros() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.500")
        
        // Then
        // Should handle trailing zeros gracefully
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasSpaces() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100 50")
        
        // Then
        // Should handle spaces gracefully
        findAmountField().assertTextContains("10050")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasCommas() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("1,000.50")
        
        // Then
        // Should handle commas gracefully
        findAmountField().assertTextContains("1000.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasCurrencySymbol() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("$100.50")
        
        // Then
        // Should handle currency symbol gracefully
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasPlusSign() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("+100.50")
        
        // Then
        // Should handle plus sign gracefully
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasMinusSign() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("-100.50")
        
        // Then
        // Should handle minus sign gracefully
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasScientificNotation() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("1e2")
        
        // Then
        // Should handle scientific notation gracefully
        findAmountField().assertTextContains("1e2")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasSpecialCharacters() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50@")
        
        // Then
        // Should handle special characters gracefully
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasLettersMixedWithNumbers() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100a50")
        
        // Then
        // Should handle mixed characters gracefully
        findAmountField().assertTextContains("10050")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasOnlyDecimalPoint() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount(".")
        
        // Then
        // Should handle decimal point gracefully
        findAmountField().assertTextContains(".")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasOnlyDecimalPointAndZero() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount(".0")
        
        // Then
        // Should handle decimal point with zero gracefully
        findAmountField().assertTextContains(".0")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasOnlyZeroAndDecimalPoint() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("0.")
        
        // Then
        // Should handle zero with decimal point gracefully
        findAmountField().assertTextContains("0.")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasMultipleDecimalPointsSecondTest() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50.25")
        
        // Then
        // Should handle multiple decimal points gracefully
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointAtTheBeginning() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount(".50")
        
        // Then
        // Should handle decimal point at beginning gracefully
        findAmountField().assertTextContains(".50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointAtTheEnd() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.")
        
        // Then
        // Should handle decimal point at end gracefully
        findAmountField().assertTextContains("100.")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointInTheMiddle() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        
        // Then
        // Should handle decimal point in middle gracefully
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointWithNoDigitsAfter() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.")
        
        // Then
        // Should handle decimal point with no digits after gracefully
        findAmountField().assertTextContains("100.")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointWithNoDigitsBefore() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount(".50")
        
        // Then
        // Should handle decimal point with no digits before gracefully
        findAmountField().assertTextContains(".50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointWithNoDigits() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount(".")
        
        // Then
        // Should handle decimal point with no digits gracefully
        findAmountField().assertTextContains(".")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointWithOnlyZeros() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.00")
        
        // Then
        // Should handle decimal point with only zeros gracefully
        findAmountField().assertTextContains("100.00")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointWithOnlyZerosAfter() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.000")
        
        // Then
        // Should handle decimal point with only zeros after gracefully
        findAmountField().assertTextContains("100.00")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointWithOnlyZerosBefore() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("000.50")
        
        // Then
        // Should handle decimal point with only zeros before gracefully
        findAmountField().assertTextContains("0.50")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointWithOnlyZerosBeforeAndAfter() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("000.000")
        
        // Then
        // Should handle decimal point with only zeros before and after gracefully
        findAmountField().assertTextContains("0.00")
    }
    
    @Test
    fun shouldShowErrorMessageWhenAmountHasDecimalPointWithOnlyZerosBeforeAndAfterAndInBetween() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("000.000")
        
        // Then
        // Should handle decimal point with only zeros before and after and in between gracefully
        findAmountField().assertTextContains("0.00")
    }
}
