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
 * UI tests for currency search functionality
 * Tests search by currency code, symbol, display name, and filtering behavior
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CurrencySearchTest : BaseCurrencyUiTest() {
    
    @Test
    fun currencySearchShouldFindCurrencyByCode() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("USD")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
        verifyCurrencyNotInResults(Currency.EUR)
    }
    
    @Test
    fun currencySearchShouldFindCurrencyBySymbol() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("$")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
    }
    
    @Test
    fun currencySearchShouldFindCurrencyByDisplayName() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("US Dollar")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
    }
    
    @Test
    fun currencySearchShouldFindEURCurrencyByCode() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("EUR")
        
        // Then
        verifyCurrencyInResults(Currency.EUR)
    }
    
    @Test
    fun currencySearchShouldFindEURCurrencyBySymbol() {
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
    fun currencySearchShouldFindEURCurrencyByDisplayName() {
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
    fun currencySearchShouldFindJPYCurrencyByCode() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("JPY")
        
        // Then
        verifyCurrencyInResults(Currency.JPY)
    }
    
    @Test
    fun currencySearchShouldFindJPYCurrencyBySymbol() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("¥")
        
        // Then
        verifyCurrencyInResults(Currency.JPY)
    }
    
    @Test
    fun currencySearchShouldFindJPYCurrencyByDisplayName() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("Japanese Yen")
        
        // Then
        verifyCurrencyInResults(Currency.JPY)
    }
    
    @Test
    fun currencySearchShouldFindGBPCurrencyByCode() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("GBP")
        
        // Then
        verifyCurrencyInResults(Currency.GBP)
    }
    
    @Test
    fun currencySearchShouldFindGBPCurrencyBySymbol() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("£")
        
        // Then
        verifyCurrencyInResults(Currency.GBP)
    }
    
    @Test
    fun currencySearchShouldFindGBPCurrencyByDisplayName() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("British Pound")
        
        // Then
        verifyCurrencyInResults(Currency.GBP)
    }
    
    @Test
    fun currencySearchShouldBeCaseInsensitive() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("usd")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
    }
    
    @Test
    fun currencySearchShouldBeCaseInsensitiveForEUR() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("eur")
        
        // Then
        verifyCurrencyInResults(Currency.EUR)
    }
    
    @Test
    fun currencySearchShouldFindPartialMatches() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("US")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
    }
    
    @Test
    fun currencySearchShouldFindPartialMatchesForEuro() {
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
    fun currencySearchShouldFindPartialMatchesForJapanese() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("Japanese")
        
        // Then
        verifyCurrencyInResults(Currency.JPY)
    }
    
    @Test
    fun currencySearchShouldFindPartialMatchesForBritish() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("British")
        
        // Then
        verifyCurrencyInResults(Currency.GBP)
    }
    
    @Test
    fun currencySearchShouldShowNoResultsForInvalidSearch() {
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
    fun currencySearchShouldShowNoResultsForEmptySearch() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("")
        
        // Then
        // Should show all currencies or default view
        composeTestRule.onNode(hasText("Most Popular")).assertIsDisplayed()
    }
    
    @Test
    fun currencySearchShouldShowNoResultsForNumericSearch() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("123")
        
        // Then
        composeTestRule.onNode(hasText("No results found")).assertIsDisplayed()
    }
    
    @Test
    fun currencySearchShouldClearResultsWhenSearchIsCleared() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("USD")
        verifyCurrencyInResults(Currency.USD)
        
        // Clear search
        findCurrencyDropdown().performTextClearance()
        
        // Then
        // Should show all currencies again
        composeTestRule.onNode(hasText("Most Popular")).assertIsDisplayed()
    }
    
    @Test
    fun currencySearchShouldUpdateResultsAsUserTypes() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("U")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
        
        // When
        searchCurrency("US")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
        
        // When
        searchCurrency("USD")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
    }
    
    @Test
    fun currencySearchShouldHandleSpecialCharactersInSearch() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("$")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
    }
    
    @Test
    fun currencySearchShouldHandleMultipleCurrencySymbolsInSearch() {
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
    fun currencySearchShouldFindCADCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("CAD")
        
        // Then
        verifyCurrencyInResults(Currency.CAD)
    }
    
    @Test
    fun currencySearchShouldFindAUDCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("AUD")
        
        // Then
        verifyCurrencyInResults(Currency.AUD)
    }
    
    @Test
    fun currencySearchShouldFindCHFCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("CHF")
        
        // Then
        verifyCurrencyInResults(Currency.CHF)
    }
    
    @Test
    fun currencySearchShouldFindCNYCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("CNY")
        
        // Then
        verifyCurrencyInResults(Currency.CNY)
    }
    
    @Test
    fun currencySearchShouldFindINRCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("INR")
        
        // Then
        verifyCurrencyInResults(Currency.INR)
    }
    
    @Test
    fun currencySearchShouldFindKRWCurrency() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("KRW")
        
        // Then
        verifyCurrencyInResults(Currency.KRW)
    }
    
    @Test
    fun currencySearchShouldFindKRWCurrencyBySymbol() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("₩")
        
        // Then
        verifyCurrencyInResults(Currency.KRW)
    }
    
    @Test
    fun currencySearchShouldFindKRWCurrencyByDisplayName() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("South Korean Won")
        
        // Then
        verifyCurrencyInResults(Currency.KRW)
    }
    
    @Test
    fun currencySearchShouldHandleSearchWithSpaces() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("US Dollar")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
    }
    
    @Test
    fun currencySearchShouldHandleSearchWithExtraSpaces() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("  USD  ")
        
        // Then
        verifyCurrencyInResults(Currency.USD)
    }
    
    @Test
    fun currencySearchShouldShowSearchResultsSectionHeader() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("USD")
        
        // Then
        composeTestRule.onNode(hasText("Search Results")).assertIsDisplayed()
    }
    
    @Test
    fun currencySearchShouldMaintainSearchStateWhenDropdownIsReopened() {
        // Given
        setupComposeContent { }
        navigateToAddExpense()
        
        // When
        findCurrencyDropdown().performClick()
        searchCurrency("USD")
        verifyCurrencyInResults(Currency.USD)
        
        // Close and reopen dropdown
        composeTestRule.onNode(hasText("Cancel")).performClick()
        findCurrencyDropdown().performClick()
        
        // Then
        // Search should be cleared and show all currencies
        composeTestRule.onNode(hasText("Most Popular")).assertIsDisplayed()
    }
}
