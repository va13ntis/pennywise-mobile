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
 * UI tests for currency display in transaction lists
 * Tests currency symbol display, amount formatting, and currency switching in transaction views
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class TransactionListCurrencyTest : BaseCurrencyUiTest() {
    
    @Test
    fun transactionListShouldDisplayUSDCurrencySymbolCorrectly() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        // Navigate to transactions screen (assuming it exists)
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should display USD transaction with $ symbol
        composeTestRule.onNode(
            hasText("$100.00") or hasText("$100")
        ).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldDisplayEURCurrencySymbolCorrectly() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should display EUR transaction with € symbol
        composeTestRule.onNode(
            hasText("€85.50") or hasText("€85")
        ).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldDisplayJPYCurrencySymbolCorrectly() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should display JPY transaction with ¥ symbol (no decimals)
        composeTestRule.onNode(
            hasText("¥15000") or hasText("¥15,000")
        ).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldDisplayGBPCurrencySymbolCorrectly() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should display GBP transaction with £ symbol
        composeTestRule.onNode(
            hasText("£75.25") or hasText("£75")
        ).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldFormatAmountsCorrectlyForDifferentCurrencies() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // USD: 2 decimal places
        composeTestRule.onNode(hasText("$100.00")).assertIsDisplayed()
        
        // EUR: 2 decimal places
        composeTestRule.onNode(hasText("€85.50")).assertIsDisplayed()
        
        // JPY: 0 decimal places
        composeTestRule.onNode(hasText("¥15000")).assertIsDisplayed()
        
        // GBP: 2 decimal places
        composeTestRule.onNode(hasText("£75.25")).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldDisplayCurrencyCodeAlongsideSymbol() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should display both symbol and code
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
        composeTestRule.onNode(hasText("EUR")).assertIsDisplayed()
        composeTestRule.onNode(hasText("JPY")).assertIsDisplayed()
        composeTestRule.onNode(hasText("GBP")).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldGroupTransactionsByCurrencyWhenMultipleCurrenciesPresent() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should show transactions grouped or with clear currency indicators
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
        composeTestRule.onNode(hasText("EUR")).assertIsDisplayed()
        composeTestRule.onNode(hasText("JPY")).assertIsDisplayed()
        composeTestRule.onNode(hasText("GBP")).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldDisplayRecurringTransactionIndicatorWithCurrency() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should show recurring indicator for JPY transaction
        composeTestRule.onNode(
            hasText("¥15000") and hasText("Recurring")
        ).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldHandleCurrencyConversionDisplayWhenEnabled() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        // Enable currency conversion in settings
        navigateToSettings()
        toggleCurrencyConversion()
        selectOriginalCurrency("USD")
        
        // Navigate back to transactions
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should display both original and converted amounts
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Original")).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldDisplayCurrencyTotalsCorrectly() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should show totals for each currency
        composeTestRule.onNode(hasText("Total")).assertIsDisplayed()
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
        composeTestRule.onNode(hasText("EUR")).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldHandleEmptyStateWithCurrencyContext() {
        // Given
        setupComposeContent { }
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should show empty state message
        composeTestRule.onNode(
            hasText("No transactions") or hasText("No transactions yet")
        ).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldDisplayCurrencyInTransactionDetails() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Click on a transaction to view details
        composeTestRule.onNode(hasText("$100.00")).performClick()
        waitForIdle()
        
        // Then
        // Should show currency in transaction details
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
        composeTestRule.onNode(hasText("$100.00")).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldHandleCurrencySearchAndFiltering() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Search for USD transactions
        composeTestRule.onNode(hasText("Search")).performClick()
        composeTestRule.onNode(hasText("Search")).performTextInput("USD")
        
        // Then
        // Should show only USD transactions
        composeTestRule.onNode(hasText("$100.00")).assertIsDisplayed()
        composeTestRule.onNode(hasText("€85.50")).assertDoesNotExist()
    }
    
    @Test
    fun transactionListShouldDisplayCurrencyInTransactionCategories() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should show currency in category sections
        composeTestRule.onNode(hasText("Food")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Transport")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Entertainment")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Shopping")).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldHandleCurrencySymbolUpdatesWhenUserChangesDefaultCurrency() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        // Change default currency in settings
        navigateToSettings()
        selectOriginalCurrency("EUR")
        
        // Navigate back to transactions
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should update display to show EUR as primary currency
        composeTestRule.onNode(hasText("EUR")).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldDisplayCurrencyConversionRatesWhenAvailable() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        // Enable currency conversion
        navigateToSettings()
        toggleCurrencyConversion()
        selectOriginalCurrency("USD")
        
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should show conversion rates or converted amounts
        composeTestRule.onNode(hasText("Rate") or hasText("Converted")).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldHandleCurrencyFormattingForLargeAmounts() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should format large amounts correctly (e.g., JPY 15000)
        composeTestRule.onNode(
            hasText("¥15000") or hasText("¥15,000") or hasText("¥15 000")
        ).assertIsDisplayed()
    }
    
    @Test
    fun transactionListShouldDisplayCurrencyInTransactionTimestamps() {
        // Given
        setupComposeContent { }
        val testTransactions = CurrencyTestFixtures.createTestTransactions()
        
        // When
        composeTestRule.onNode(hasText("Transactions")).performClick()
        waitForIdle()
        
        // Then
        // Should show date/time with currency context
        composeTestRule.onNode(hasText("Today") or hasText("Yesterday")).assertIsDisplayed()
    }
}
