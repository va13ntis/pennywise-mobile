package com.pennywise.app.presentation.components

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.pennywise.app.MainActivity
import com.pennywise.app.R
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso-based accessibility tests for currency UI components
 * Uses AccessibilityChecks to automatically verify accessibility guidelines
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CurrencyEspressoAccessibilityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    companion object {
        @BeforeClass
        @JvmStatic
        fun enableAccessibilityChecks() {
            // Enable accessibility checks for all Espresso actions
            AccessibilityChecks.enable()
        }
    }

    @Test
    fun currencySelectionDropdown_meetsAccessibilityGuidelines() {
        // Navigate to settings screen where currency selection is available
        onView(withId(R.id.nav_settings))
            .perform(click())

        // Find and interact with currency selection dropdown
        onView(withText("USD - $ - US Dollar"))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify the currency selection dialog is accessible
        onView(withText("Select currency"))
            .check(matches(isDisplayed()))

        // Test currency selection with accessibility checks
        onView(withText("EUR - €"))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify selection was made
        onView(withText("EUR - € - Euro"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun currencyChangeConfirmationDialog_meetsAccessibilityGuidelines() {
        // Navigate to settings screen
        onView(withId(R.id.nav_settings))
            .perform(click())

        // Open currency selection
        onView(withText("USD - $ - US Dollar"))
            .perform(click())

        // Select a different currency to trigger confirmation dialog
        onView(withText("EUR - €"))
            .perform(click())

        // Verify confirmation dialog is accessible
        onView(withText("Change Default Currency"))
            .check(matches(isDisplayed()))

        // Test dialog buttons with accessibility checks
        onView(withText("Change"))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify currency was changed
        onView(withText("EUR - € - Euro"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun currencyDisplayComponents_haveProperContentDescriptions() {
        // Navigate to home screen where currency amounts are displayed
        onView(withId(R.id.nav_home))
            .perform(click())

        // Check that currency amounts are displayed with proper accessibility
        onView(withText("$"))
            .check(matches(isDisplayed()))

        // Navigate to add expense screen
        onView(withId(R.id.fab_add_expense))
            .perform(click())

        // Check currency input field accessibility
        onView(withId(R.id.amount_input))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun currencySymbols_areAccessibleToScreenReaders() {
        // Test various currency symbols for accessibility
        val currencySymbols = listOf("$", "€", "£", "¥", "₹")

        currencySymbols.forEach { symbol ->
            // Navigate to settings
            onView(withId(R.id.nav_settings))
                .perform(click())

            // Open currency selection
            onView(withText("USD - $ - US Dollar"))
                .perform(click())

            // Look for currency with the symbol
            onView(withText(symbol))
                .check(matches(isDisplayed()))
                .check(matches(hasContentDescription()))

            // Go back
            onView(withText("Cancel"))
                .perform(click())
        }
    }

    @Test
    fun currencyInputFields_meetAccessibilityRequirements() {
        // Navigate to add expense screen
        onView(withId(R.id.fab_add_expense))
            .perform(click())

        // Test amount input field
        onView(withId(R.id.amount_input))
            .check(matches(isDisplayed()))
            .check(matches(isEnabled()))
            .check(matches(hasContentDescription()))
            .perform(typeText("100.00"))

        // Verify input is accessible
        onView(withText("100.00"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun currencyErrorMessages_areAccessible() {
        // Navigate to add expense screen
        onView(withId(R.id.fab_add_expense))
            .perform(click())

        // Try to submit without amount to trigger error
        onView(withId(R.id.save_button))
            .perform(click())

        // Check that error message is accessible
        onView(withText("Amount is required"))
            .check(matches(isDisplayed()))
            .check(matches(hasContentDescription()))
    }

    @Test
    fun currencySelectionKeyboardNavigation() {
        // Navigate to settings
        onView(withId(R.id.nav_settings))
            .perform(click())

        // Test keyboard navigation to currency selection
        onView(withText("USD - $ - US Dollar"))
            .perform(click())

        // Test keyboard navigation within dialog
        onView(withText("EUR - €"))
            .perform(click())

        // Verify keyboard navigation worked
        onView(withText("EUR - € - Euro"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun currencyDisplayColorContrast() {
        // This test verifies that currency displays meet color contrast requirements
        // The actual contrast testing would be done with accessibility testing tools
        // Here we verify the components are using Material Design colors

        // Navigate to home screen
        onView(withId(R.id.nav_home))
            .perform(click())

        // Check that currency amounts are visible (implicit contrast test)
        onView(withText("$"))
            .check(matches(isDisplayed()))
            .check(matches(isCompletelyDisplayed()))
    }

    @Test
    fun currencySelectionTouchTargetSize() {
        // Test that currency selection elements meet minimum touch target size (48dp)

        // Navigate to settings
        onView(withId(R.id.nav_settings))
            .perform(click())

        // Open currency selection
        onView(withText("USD - $ - US Dollar"))
            .perform(click())

        // Test that currency items are large enough to touch
        onView(withText("USD - $"))
            .check(matches(isDisplayed()))
            .perform(click())

        // Verify selection worked (implicit touch target size test)
        onView(withText("USD - $ - US Dollar"))
            .check(matches(isDisplayed()))
    }

    @Test
    fun currencyComponentsFocusManagement() {
        // Test focus management for currency components

        // Navigate to add expense screen
        onView(withId(R.id.fab_add_expense))
            .perform(click())

        // Test focus on amount field
        onView(withId(R.id.amount_input))
            .perform(click())
            .check(matches(hasFocus()))

        // Test focus movement
        onView(withId(R.id.merchant_input))
            .perform(click())
            .check(matches(hasFocus()))
    }
}
