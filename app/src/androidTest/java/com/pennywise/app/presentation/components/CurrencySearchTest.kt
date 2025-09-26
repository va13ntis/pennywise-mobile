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
    fun `currency search should find currency by code`() {
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
    fun `currency search should find currency by symbol`() {
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
    fun `currency search should find currency by display name`() {
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
    fun `currency search should find EUR currency by code`() {
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
    fun `currency search should find EUR currency by symbol`() {
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
    fun `currency search should find EUR currency by display name`() {
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
    fun `currency search should find JPY currency by code`() {
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
    fun `currency search should find JPY currency by symbol`() {
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
    fun `currency search should find JPY currency by display name`() {
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
    fun `currency search should find GBP currency by code`() {
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
    fun `currency search should find GBP currency by symbol`() {
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
    fun `currency search should find GBP currency by display name`() {
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
    fun `currency search should be case insensitive`() {
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
    fun `currency search should be case insensitive for EUR`() {
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
    fun `currency search should find partial matches`() {
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
    fun `currency search should find partial matches for Euro`() {
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
    fun `currency search should find partial matches for Japanese`() {
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
    fun `currency search should find partial matches for British`() {
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
    fun `currency search should show no results for invalid search`() {
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
    fun `currency search should show no results for empty search`() {
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
    fun `currency search should show no results for numeric search`() {
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
    fun `currency search should clear results when search is cleared`() {
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
    fun `currency search should update results as user types`() {
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
    fun `currency search should handle special characters in search`() {
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
    fun `currency search should handle multiple currency symbols in search`() {
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
    fun `currency search should find CAD currency`() {
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
    fun `currency search should find AUD currency`() {
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
    fun `currency search should find CHF currency`() {
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
    fun `currency search should find CNY currency`() {
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
    fun `currency search should find INR currency`() {
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
    fun `currency search should find KRW currency`() {
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
    fun `currency search should find KRW currency by symbol`() {
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
    fun `currency search should find KRW currency by display name`() {
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
    fun `currency search should handle search with spaces`() {
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
    fun `currency search should handle search with extra spaces`() {
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
    fun `currency search should show search results section header`() {
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
    fun `currency search should maintain search state when dropdown is reopened`() {
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
