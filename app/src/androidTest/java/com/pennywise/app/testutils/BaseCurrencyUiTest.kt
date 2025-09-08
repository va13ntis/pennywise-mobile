package com.pennywise.app.testutils

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.MainActivity
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.User
import com.pennywise.app.presentation.PennyWiseApp
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for currency-related UI tests
 * Provides common setup and utilities for testing currency functionality
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
abstract class BaseCurrencyUiTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    protected lateinit var context: Context
    protected lateinit var instrumentationContext: Context
    
    @Before
    fun setUp() {
        hiltRule.inject()
        context = ApplicationProvider.getApplicationContext()
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
    }
    
    /**
     * Set up compose content with test data
     */
    protected fun setupComposeContent(
        user: User = CurrencyTestFixtures.createTestUser(),
        content: @Composable () -> Unit
    ) {
        composeTestRule.setContent {
            PennyWiseApp()
        }
    }
    
    /**
     * Wait for compose to be idle
     */
    protected fun waitForIdle() {
        composeTestRule.waitForIdle()
    }
    
    /**
     * Find currency selection dropdown
     */
    protected fun findCurrencyDropdown(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("currency_dropdown") or
            hasContentDescription("Select Currency") or
            hasText("Select Currency")
        )
    }
    
    /**
     * Find currency selection dialog
     */
    protected fun findCurrencyDialog(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("currency_selection_dialog") or
            hasText("Select Currency")
        )
    }
    
    /**
     * Find amount input field
     */
    protected fun findAmountField(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("amount_field") or
            hasContentDescription("Amount") or
            hasText("Amount")
        )
    }
    
    /**
     * Find currency symbol in amount field
     */
    protected fun findCurrencySymbol(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("currency_symbol") or
            hasText("$") or
            hasText("€") or
            hasText("£") or
            hasText("¥")
        )
    }
    
    /**
     * Find save button
     */
    protected fun findSaveButton(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("save_button") or
            hasText("Save") or
            hasContentDescription("Save")
        )
    }
    
    /**
     * Find settings screen
     */
    protected fun findSettingsScreen(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("settings_screen") or
            hasText("Settings")
        )
    }
    
    /**
     * Find currency conversion toggle
     */
    protected fun findCurrencyConversionToggle(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("currency_conversion_toggle") or
            hasText("Enable Currency Conversion")
        )
    }
    
    /**
     * Find original currency selection
     */
    protected fun findOriginalCurrencySelection(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("original_currency_selection") or
            hasText("Original Currency")
        )
    }
    
    /**
     * Find error message
     */
    protected fun findErrorMessage(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("error_message") or
            hasText("Error") or
            hasText("Invalid")
        )
    }
    
    /**
     * Find loading indicator
     */
    protected fun findLoadingIndicator(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("loading_indicator") or
            hasContentDescription("Loading")
        )
    }
    
    /**
     * Select currency from dropdown
     */
    protected fun selectCurrency(currency: Currency) {
        // Open dropdown
        findCurrencyDropdown().performClick()
        waitForIdle()
        
        // Find and click the currency option
        composeTestRule.onNode(
            hasText(currency.code) or
            hasText(currency.displayName) or
            hasText(currency.symbol)
        ).performClick()
        
        waitForIdle()
    }
    
    /**
     * Enter amount in amount field
     */
    protected fun enterAmount(amount: String) {
        findAmountField().performTextInput(amount)
        waitForIdle()
    }
    
    /**
     * Clear amount field
     */
    protected fun clearAmountField() {
        findAmountField().performTextClearance()
        waitForIdle()
    }
    
    /**
     * Verify currency symbol is displayed
     */
    protected fun verifyCurrencySymbol(currency: Currency) {
        findCurrencySymbol().assertIsDisplayed()
        findCurrencySymbol().assertTextContains(currency.symbol)
    }
    
    /**
     * Verify error message is displayed
     */
    protected fun verifyErrorMessage(message: String) {
        findErrorMessage().assertIsDisplayed()
        findErrorMessage().assertTextContains(message)
    }
    
    /**
     * Verify loading indicator is displayed
     */
    protected fun verifyLoadingIndicator() {
        findLoadingIndicator().assertIsDisplayed()
    }
    
    /**
     * Verify loading indicator is not displayed
     */
    protected fun verifyNoLoadingIndicator() {
        findLoadingIndicator().assertDoesNotExist()
    }
    
    /**
     * Verify save button is enabled
     */
    protected fun verifySaveButtonEnabled() {
        findSaveButton().assertIsEnabled()
    }
    
    /**
     * Verify save button is disabled
     */
    protected fun verifySaveButtonDisabled() {
        findSaveButton().assertIsNotEnabled()
    }
    
    /**
     * Perform save action
     */
    protected fun performSave() {
        findSaveButton().performClick()
        waitForIdle()
    }
    
    /**
     * Search for currency in dropdown
     */
    protected fun searchCurrency(query: String) {
        findCurrencyDropdown().performTextInput(query)
        waitForIdle()
    }
    
    /**
     * Verify currency is in search results
     */
    protected fun verifyCurrencyInResults(currency: Currency) {
        composeTestRule.onNode(
            hasText(currency.code) or
            hasText(currency.displayName)
        ).assertIsDisplayed()
    }
    
    /**
     * Verify currency is not in search results
     */
    protected fun verifyCurrencyNotInResults(currency: Currency) {
        composeTestRule.onNode(
            hasText(currency.code) or
            hasText(currency.displayName)
        ).assertDoesNotExist()
    }
    
    /**
     * Navigate to settings screen
     */
    protected fun navigateToSettings() {
        composeTestRule.onNode(
            hasText("Settings") or
            hasContentDescription("Settings")
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Navigate to add expense screen
     */
    protected fun navigateToAddExpense() {
        composeTestRule.onNode(
            hasText("Add Expense") or
            hasContentDescription("Add Expense") or
            hasText("+")
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Toggle currency conversion
     */
    protected fun toggleCurrencyConversion() {
        findCurrencyConversionToggle().performClick()
        waitForIdle()
    }
    
    /**
     * Select original currency
     */
    protected fun selectOriginalCurrency(currencyCode: String) {
        composeTestRule.onNode(
            hasText(currencyCode) or
            hasText("currency_$currencyCode".lowercase())
        ).performClick()
        waitForIdle()
    }
}
