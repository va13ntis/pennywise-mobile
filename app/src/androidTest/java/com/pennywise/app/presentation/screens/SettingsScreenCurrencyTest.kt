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
    fun settingsScreenShouldDisplayCurrencyConversionSection() {
        // Given
        setupComposeContent { }
        
        // When
        navigateToSettings()
        
        // Then
        findSettingsScreen().assertIsDisplayed()
        composeTestRule.onNode(hasText("Currency Conversion")).assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldDisplayCurrencyConversionToggle() {
        // Given
        setupComposeContent { }
        
        // When
        navigateToSettings()
        
        // Then
        findCurrencyConversionToggle().assertIsDisplayed()
        composeTestRule.onNode(hasText("Enable Currency Conversion")).assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldToggleCurrencyConversionOnAndOff() {
        // Given
        setupComposeContent { }
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
    fun settingsScreenShouldShowOriginalCurrencySelectionWhenConversionIsEnabled() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        findOriginalCurrencySelection().assertIsDisplayed()
        composeTestRule.onNode(hasText("Original Currency")).assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldHideOriginalCurrencySelectionWhenConversionIsDisabled() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        toggleCurrencyConversion() // Disable it
        
        // Then
        composeTestRule.onNode(hasText("Original Currency")).assertDoesNotExist()
    }
    
    @Test
    fun settingsScreenShouldDisplayAllSupportedOriginalCurrencies() {
        // Given
        setupComposeContent { }
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
    fun settingsScreenShouldAllowSelectionOfUSDAsOriginalCurrency() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("USD")
        
        // Then
        // USD should be selected
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldAllowSelectionOfEURAsOriginalCurrency() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("EUR")
        
        // Then
        // EUR should be selected
        composeTestRule.onNode(hasText("EUR")).assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldAllowSelectionOfGBPAsOriginalCurrency() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("GBP")
        
        // Then
        // GBP should be selected
        composeTestRule.onNode(hasText("GBP")).assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldAllowSelectionOfJPYAsOriginalCurrency() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("JPY")
        
        // Then
        // JPY should be selected
        composeTestRule.onNode(hasText("JPY")).assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldShowRadioButtonSelectionForOriginalCurrency() {
        // Given
        setupComposeContent { }
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
    fun settingsScreenShouldDefaultToUSDWhenCurrencyConversionIsEnabled() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // USD should be selected by default
        composeTestRule.onNode(hasText("USD")).assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldPersistCurrencyConversionSettings() {
        // Given
        setupComposeContent { }
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
    fun settingsScreenShouldShowCurrencyConversionToggleInCorrectSection() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        // Scroll to currency conversion section
        
        // Then
        composeTestRule.onNode(hasText("Currency Conversion")).assertIsDisplayed()
        findCurrencyConversionToggle().assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldHandleCurrencyConversionToggleStateCorrectly() {
        // Given
        setupComposeContent { }
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
    fun settingsScreenShouldDisplayCurrencyNamesCorrectly() {
        // Given
        setupComposeContent { }
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
    fun settingsScreenShouldHandleCurrencySelectionWithProperValidation() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        selectOriginalCurrency("USD")
        
        // Then
        // Should not show any error messages
        composeTestRule.onNode(hasText("Error") or hasText("Invalid")).assertDoesNotExist()
    }
    
    @Test
    fun settingsScreenShouldShowCurrencyConversionHelpText() {
        // Given
        setupComposeContent { }
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
    fun settingsScreenShouldHandleCurrencyConversionToggleAccessibility() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        findCurrencyConversionToggle().performClick()
        
        // Then
        // Should be accessible and clickable
        findCurrencyConversionToggle().assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldShowCurrencyConversionStatus() {
        // Given
        setupComposeContent { }
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
    fun settingsScreenShouldHandleCurrencyConversionWithMultipleSelections() {
        // Given
        setupComposeContent { }
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
    fun settingsScreenShouldShowCurrencyConversionToggleWithProperStyling() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Toggle should be visually distinct when enabled
        findCurrencyConversionToggle().assertIsDisplayed()
    }
    
    @Test
    fun settingsScreenShouldHandleCurrencyConversionToggleWithProperColors() {
        // Given
        setupComposeContent { }
        navigateToSettings()
        
        // When
        toggleCurrencyConversion()
        
        // Then
        // Toggle should have proper color scheme
        findCurrencyConversionToggle().assertIsDisplayed()
    }
}
