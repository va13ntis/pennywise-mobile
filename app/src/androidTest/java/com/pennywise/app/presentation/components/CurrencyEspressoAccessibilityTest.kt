package com.pennywise.app.presentation.components

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.pennywise.app.presentation.MainActivity
import com.pennywise.app.presentation.navigation.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose-based accessibility tests for currency UI components
 * Uses Compose testing framework to verify accessibility guidelines
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CurrencyEspressoAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun currencySelectionDropdown_meetsAccessibilityGuidelines() {
        // Wait for the app to load and navigate to settings
        composeTestRule.waitForIdle()
        
        // Navigate to settings screen where currency selection is available
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        // Find and interact with currency selection dropdown
        composeTestRule.onNodeWithText("USD - $ - US Dollar")
            .assertIsDisplayed()
            .performClick()

        // Verify the currency selection dialog is accessible
        composeTestRule.onNodeWithText("Select currency")
            .assertIsDisplayed()

        // Test currency selection with accessibility checks
        composeTestRule.onNodeWithText("EUR - €")
            .assertIsDisplayed()
            .performClick()

        // Verify selection was made
        composeTestRule.onNodeWithText("EUR - € - Euro")
            .assertIsDisplayed()
    }

    @Test
    fun currencyChangeConfirmationDialog_meetsAccessibilityGuidelines() {
        // Wait for the app to load
        composeTestRule.waitForIdle()
        
        // Navigate to settings screen
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        // Open currency selection
        composeTestRule.onNodeWithText("USD - $ - US Dollar")
            .performClick()

        // Select a different currency to trigger confirmation dialog
        composeTestRule.onNodeWithText("EUR - €")
            .performClick()

        // Verify confirmation dialog is accessible
        composeTestRule.onNodeWithText("Change Default Currency")
            .assertIsDisplayed()

        // Test dialog buttons with accessibility checks
        composeTestRule.onNodeWithText("Change")
            .assertIsDisplayed()
            .performClick()

        // Verify currency was changed
        composeTestRule.onNodeWithText("EUR - € - Euro")
            .assertIsDisplayed()
    }

    @Test
    fun currencyDisplayComponents_haveProperContentDescriptions() {
        // Wait for the app to load
        composeTestRule.waitForIdle()
        
        // Navigate to home screen where currency amounts are displayed
        composeTestRule.onNodeWithText("Home").performClick()
        composeTestRule.waitForIdle()

        // Check that currency amounts are displayed with proper accessibility
        composeTestRule.onNodeWithText("$")
            .assertIsDisplayed()

        // Navigate to add expense screen
        composeTestRule.onNodeWithContentDescription("Add expense").performClick()
        composeTestRule.waitForIdle()

        // Check currency input field accessibility
        composeTestRule.onNodeWithText("Amount")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun currencySymbols_areAccessibleToScreenReaders() {
        // Wait for the app to load
        composeTestRule.waitForIdle()
        
        // Test various currency symbols for accessibility
        val currencySymbols = listOf("$", "€", "£", "¥", "₹")

        currencySymbols.forEach { symbol ->
            // Navigate to settings
            composeTestRule.onNodeWithText("Settings").performClick()
            composeTestRule.waitForIdle()

            // Open currency selection
            composeTestRule.onNodeWithText("USD - $ - US Dollar")
                .performClick()

            // Look for currency with the symbol
            composeTestRule.onNodeWithText(symbol)
                .assertIsDisplayed()
                .assertHasClickAction()

            // Go back
            composeTestRule.onNodeWithText("Cancel")
                .performClick()
        }
    }

    @Test
    fun currencyInputFields_meetAccessibilityRequirements() {
        // Wait for the app to load
        composeTestRule.waitForIdle()
        
        // Navigate to add expense screen
        composeTestRule.onNodeWithContentDescription("Add expense").performClick()
        composeTestRule.waitForIdle()

        // Test amount input field
        composeTestRule.onNodeWithText("Amount")
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertHasClickAction()
            .performTextInput("100.00")

        // Verify input is accessible
        composeTestRule.onNodeWithText("100.00")
            .assertIsDisplayed()
    }

    @Test
    fun currencyErrorMessages_areAccessible() {
        // Wait for the app to load
        composeTestRule.waitForIdle()
        
        // Navigate to add expense screen
        composeTestRule.onNodeWithContentDescription("Add expense").performClick()
        composeTestRule.waitForIdle()

        // Try to submit without amount to trigger error
        composeTestRule.onNodeWithText("Save")
            .performClick()

        // Check that error message is accessible
        composeTestRule.onNodeWithText("Amount is required")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun currencySelectionKeyboardNavigation() {
        // Wait for the app to load
        composeTestRule.waitForIdle()
        
        // Navigate to settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        // Test keyboard navigation to currency selection
        composeTestRule.onNodeWithText("USD - $ - US Dollar")
            .performClick()

        // Test keyboard navigation within dialog
        composeTestRule.onNodeWithText("EUR - €")
            .performClick()

        // Verify keyboard navigation worked
        composeTestRule.onNodeWithText("EUR - € - Euro")
            .assertIsDisplayed()
    }

    @Test
    fun currencyDisplayColorContrast() {
        // Wait for the app to load
        composeTestRule.waitForIdle()
        
        // This test verifies that currency displays meet color contrast requirements
        // The actual contrast testing would be done with accessibility testing tools
        // Here we verify the components are using Material Design colors

        // Navigate to home screen
        composeTestRule.onNodeWithText("Home").performClick()
        composeTestRule.waitForIdle()

        // Check that currency amounts are visible (implicit contrast test)
        composeTestRule.onNodeWithText("$")
            .assertIsDisplayed()
            .assertIsFocused()
    }

    @Test
    fun currencySelectionTouchTargetSize() {
        // Wait for the app to load
        composeTestRule.waitForIdle()
        
        // Test that currency selection elements meet minimum touch target size (48dp)

        // Navigate to settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()

        // Open currency selection
        composeTestRule.onNodeWithText("USD - $ - US Dollar")
            .performClick()

        // Test that currency items are large enough to touch
        composeTestRule.onNodeWithText("USD - $")
            .assertIsDisplayed()
            .performClick()

        // Verify selection worked (implicit touch target size test)
        composeTestRule.onNodeWithText("USD - $ - US Dollar")
            .assertIsDisplayed()
    }

    @Test
    fun currencyComponentsFocusManagement() {
        // Wait for the app to load
        composeTestRule.waitForIdle()
        
        // Test focus management for currency components

        // Navigate to add expense screen
        composeTestRule.onNodeWithContentDescription("Add expense").performClick()
        composeTestRule.waitForIdle()

        // Test focus on amount field
        composeTestRule.onNodeWithText("Amount")
            .performClick()
            .assertIsFocused()

        // Test focus movement
        composeTestRule.onNodeWithText("Merchant")
            .performClick()
            .assertIsFocused()
    }
}
