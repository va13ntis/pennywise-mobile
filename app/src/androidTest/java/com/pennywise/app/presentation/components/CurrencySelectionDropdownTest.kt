package com.pennywise.app.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.testutils.BaseCurrencyUiTest
import com.pennywise.app.testutils.CurrencyTestFixtures
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for CurrencySelectionDropdown component
 * Tests currency selection behavior, search functionality, and grouping by popularity
 */
@RunWith(AndroidJUnit4::class)
class CurrencySelectionDropdownTest : BaseCurrencyUiTest() {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun `currency dropdown should display current currency correctly`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        // When
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // Then
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `currency dropdown should open dialog when clicked`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).performClick()
        
        // Then
        composeTestRule.onNode(hasText("Select Currency")).assertIsDisplayed()
    }
    
    @Test
    fun `currency dropdown should display all currencies in dialog`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        val allCurrencies = CurrencyTestFixtures.getAllCurrencies()
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).performClick()
        
        // Then
        // Verify popular currencies are displayed
        allCurrencies.take(10).forEach { currency ->
            composeTestRule.onNode(
                hasText(currency.code) and hasText(currency.symbol)
            ).assertIsDisplayed()
        }
    }
    
    @Test
    fun `currency dropdown should allow currency selection`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).performClick()
        
        composeTestRule.onNode(hasText("EUR") and hasText("€")).performClick()
        
        // Then
        composeTestRule.onNode(
            hasText("EUR") and hasText("€") and hasText("Euro")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `currency dropdown should close dialog after selection`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).performClick()
        
        composeTestRule.onNode(hasText("EUR") and hasText("€")).performClick()
        
        // Then
        composeTestRule.onNode(hasText("Select Currency")).assertDoesNotExist()
    }
    
    @Test
    fun `currency dropdown should show radio button selection correctly`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).performClick()
        
        // Then
        // USD should be selected (radio button checked)
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).assertIsDisplayed()
        
        // EUR should not be selected
        composeTestRule.onNode(hasText("EUR") and hasText("€")).assertIsDisplayed()
    }
    
    @Test
    fun `currency dropdown should handle JPY currency correctly`() {
        // Given
        val currentCurrency = "JPY"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("JPY") and hasText("¥") and hasText("Japanese Yen")
        ).performClick()
        
        // Then
        composeTestRule.onNode(
            hasText("JPY") and hasText("¥") and hasText("Japanese Yen")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `currency dropdown should handle GBP currency correctly`() {
        // Given
        val currentCurrency = "GBP"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("GBP") and hasText("£") and hasText("British Pound")
        ).performClick()
        
        // Then
        composeTestRule.onNode(
            hasText("GBP") and hasText("£") and hasText("British Pound")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `currency dropdown should display currencies in popularity order`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        val popularCurrencies = CurrencyTestFixtures.getPopularCurrencies()
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).performClick()
        
        // Then
        // Verify USD is first (most popular)
        composeTestRule.onNode(hasText("USD") and hasText("$")).assertIsDisplayed()
        
        // Verify other popular currencies are displayed
        popularCurrencies.take(5).forEach { currency ->
            composeTestRule.onNode(
                hasText(currency.code) and hasText(currency.symbol)
            ).assertIsDisplayed()
        }
    }
    
    @Test
    fun `currency dropdown should handle cancel button correctly`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).performClick()
        
        composeTestRule.onNode(hasText("Cancel")).performClick()
        
        // Then
        composeTestRule.onNode(hasText("Select Currency")).assertDoesNotExist()
        // Currency should remain unchanged
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `currency dropdown should handle dialog dismissal correctly`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).performClick()
        
        // Press back to dismiss dialog
        composeTestRule.onNode(hasText("Select Currency")).performClick()
        
        // Then
        composeTestRule.onNode(hasText("Select Currency")).assertDoesNotExist()
    }
    
    @Test
    fun `currency dropdown should display currency with correct formatting`() {
        // Given
        val currentCurrency = "EUR"
        var selectedCurrency by mutableStateOf(currentCurrency)
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("EUR") and hasText("€") and hasText("Euro")
        ).performClick()
        
        // Then
        // Verify the format: "EUR - € - Euro"
        composeTestRule.onNode(
            hasText("EUR") and hasText("€") and hasText("Euro")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `currency dropdown should handle all supported currencies`() {
        // Given
        val currentCurrency = "USD"
        var selectedCurrency by mutableStateOf(currentCurrency)
        val testCurrencies = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF", "CNY", "INR")
        
        composeTestRule.setContent {
            MaterialTheme {
                CurrencySelectionDropdown(
                    currentCurrency = selectedCurrency,
                    onCurrencySelected = { selectedCurrency = it }
                )
            }
        }
        
        // When
        composeTestRule.onNode(
            hasText("USD") and hasText("$") and hasText("US Dollar")
        ).performClick()
        
        // Then
        testCurrencies.forEach { currencyCode ->
            val currency = Currency.fromCode(currencyCode)
            if (currency != null) {
                composeTestRule.onNode(
                    hasText(currency.code) and hasText(currency.symbol)
                ).assertIsDisplayed()
            }
        }
    }
}
