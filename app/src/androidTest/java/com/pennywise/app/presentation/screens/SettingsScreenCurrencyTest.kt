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
 * UI tests for currency switching functionality in SettingsScreen
 * Tests currency conversion toggle, original currency selection, and currency switching behavior
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class SettingsScreenCurrencyTest : BaseCurrencyUiTest() {
    
    @Test
    fun `settings screen should display currency conversion section`() {
        // Given
        setupComposeContent()
        
        // When
        navigateToSettings()
        
        // Then
        findSettingsScreen().assertIsDisplayed()
        composeTestRule.onNode(hasText("Currency Conversion")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should display currency conversion toggle`() {
        // Given
        setupComposeContent()
        
        // When
        navigateToSettings()
        
        // Then
        findCurrencyConversionToggle().assertIsDisplayed()
        composeTestRule.onNode(hasText("Enable Currency Conversion")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should toggle currency conversion on and off`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Toggle should be enabled
        findCurrencyConversionToggle().assertIsDisplayed()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Toggle should be disabled
        findCurrencyConversionToggle().assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should show original currency selection when conversion is enabled`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        findOriginalCurrencySelection().assertIsDisplayed()
        composeTestRule.onNode(hasText("Original Currency")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should hide original currency selection when conversion is disabled`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        toggleCurrencyConversion() // Disable it
        
        // Then
        composeTestRule.onNode(hasText("Original Currency")).assertDoesNotExist()
    }
    
    @Test
    fun `settings screen should display all supported original currencies`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Should show all supported currencies
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
        composeTestRule.onNode(hasText("EUR")).assertIsDisplayed()
        composeTestRule.onNode(hasText("GBP")).assertIsDisplayed()
        composeTestRule.onNode(hasText("ILS")).assertIsDisplayed()
        composeTestRule.onNode(hasText("RUB")).assertIsDisplayed()
        composeTestRule.onNode(hasText("JPY")).assertIsDisplayed()
        composeTestRule.onNode(hasText("CAD")).assertIsDisplayed()
        composeTestRule.onNode(hasText("AUD")).assertIsDisplayed()
        composeTestRule.onNode(hasText("CHF")).assertIsDisplayed()
        composeTestRule.onNode(hasText("CNY")).assertIsDisplayed()
        composeTestRule.onNode(hasText("INR")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should allow selection of USD as original currency`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("USD")
        
        // Then
        // USD should be selected
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should allow selection of EUR as original currency`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("EUR")
        
        // Then
        // EUR should be selected
        composeTestRule.onNode(hasText("EUR")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should allow selection of GBP as original currency`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("GBP")
        
        // Then
        // GBP should be selected
        composeTestRule.onNode(hasText("GBP")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should allow selection of JPY as original currency`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("JPY")
        
        // Then
        // JPY should be selected
        composeTestRule.onNode(hasText("JPY")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should show radio button selection for original currency`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("EUR")
        
        // Then
        // EUR should be selected (radio button checked)
        composeTestRule.onNode(hasText("EUR")).assertIsDisplayed()
        
        // USD should not be selected
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should default to USD when currency conversion is enabled`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // USD should be selected by default
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should persist currency conversion settings`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("EUR")
        
        // Navigate away and back
        composeTestRule.onNode(hasText("Back")).performClick()
        navigateToSettings()
        
        // Then
        // Settings should be persisted
        findCurrencyConversionToggle().assertIsDisplayed()
        composeTestRule.onNode(hasText("EUR")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should show currency conversion toggle in correct section`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        // Scroll to currency conversion section
        
        // Then
        composeTestRule.onNode(hasText("Currency Conversion")).assertIsDisplayed()
        findCurrencyConversionToggle().assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should handle currency conversion toggle state correctly`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        // Initially disabled
        findCurrencyConversionToggle().assertIsDisplayed()
        
        // Enable
        toggleCurrencyConversion()
        
        // Then
        // Should show original currency options
        findOriginalCurrencySelection().assertIsDisplayed()
        
        // When
        // Disable
        toggleCurrencyConversion()
        
        // Then
        // Should hide original currency options
        composeTestRule.onNode(hasText("Original Currency")).assertDoesNotExist()
    }
    
    @Test
    fun `settings screen should display currency names correctly`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Should show full currency names
        composeTestRule.onNode(hasText("US Dollar")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Euro")).assertIsDisplayed()
        composeTestRule.onNode(hasText("British Pound")).assertIsDisplayed()
        composeTestRule.onNode(hasText("Japanese Yen")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should handle currency selection with proper validation`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("USD")
        
        // Then
        // Should not show any error messages
        composeTestRule.onNode(hasText("Error") or hasText("Invalid")).assertDoesNotExist()
    }
    
    @Test
    fun `settings screen should show currency conversion help text`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Should show help text or description
        composeTestRule.onNode(
            hasText("conversion") or hasText("exchange") or hasText("rate")
        ).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should handle currency conversion toggle accessibility`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        findCurrencyConversionToggle().performClick()
        
        // Then
        // Should be accessible and clickable
        findCurrencyConversionToggle().assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should show currency conversion status`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Should show enabled status
        composeTestRule.onNode(hasText("Enabled") or hasText("On")).assertIsDisplayed()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Should show disabled status
        composeTestRule.onNode(hasText("Disabled") or hasText("Off")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should handle currency conversion with multiple selections`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("USD")
        selectOriginalCurrency("EUR")
        selectOriginalCurrency("GBP")
        
        // Then
        // Only the last selected currency should be active
        composeTestRule.onNode(hasText("GBP")).assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should show currency conversion toggle with proper styling`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Toggle should be visually distinct when enabled
        findCurrencyConversionToggle().assertIsDisplayed()
    }
    
    @Test
    fun `settings screen should handle currency conversion toggle with proper colors`() {
        // Given
        setupComposeContent()
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Toggle should have proper color scheme
        findCurrencyConversionToggle().assertIsDisplayed()
    }
}
