package com.pennywise.app.testutils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.pennywise.app.presentation.MainActivity
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.User
import com.pennywise.app.presentation.PennyWiseApp
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Base class for currency-related UI tests
 * Provides common setup and utilities for testing currency functionality
 */
@RunWith(AndroidJUnit4::class)
abstract class BaseCurrencyUiTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)
    
    protected lateinit var context: Context
    protected lateinit var instrumentationContext: Context
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        instrumentationContext = InstrumentationRegistry.getInstrumentation().context
        
        // Grant permissions for CI environment
        grantPermissions()
    }
    
    /**
     * Grant runtime permissions for CI environment
     * This prevents "Failed to grant permissions" errors on emulator
     */
    private fun grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val uiAutomation = instrumentation.uiAutomation
            
            // Grant all permissions declared in the manifest
            val permissions = listOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            
            // Grant all permissions using UiAutomation
            permissions.forEach { permission ->
                try {
                    uiAutomation.grantRuntimePermission(
                        context.packageName,
                        permission
                    )
                } catch (e: Exception) {
                    // Permission may not be declared in manifest or already granted
                    // This is expected and safe to ignore
                }
            }
        }
    }
    
    /**
     * Set up compose content with test data
     */
    protected fun setupComposeContent(
        user: User = CurrencyTestFixtures.createTestUser(),
        content: @Composable () -> Unit = { PennyWiseApp() }
    ) {
        composeTestRule.setContent {
            content()
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
    protected fun verifyLoadingIndicatorNotDisplayed() {
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
    
    /**
     * Find transaction list
     */
    protected fun findTransactionList(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("transaction_list") or
            hasText("Transactions") or
            hasContentDescription("Transaction List")
        )
    }
    
    /**
     * Find add expense button
     */
    protected fun findAddExpenseButton(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("add_expense_button") or
            hasText("Add Expense") or
            hasContentDescription("Add Expense") or
            hasText("+")
        )
    }
    
    /**
     * Find merchant input field
     */
    protected fun findMerchantField(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("merchant_field") or
            hasContentDescription("Merchant") or
            hasText("Merchant")
        )
    }
    
    /**
     * Find category selection
     */
    protected fun findCategorySelection(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("category_selection") or
            hasContentDescription("Category") or
            hasText("Category")
        )
    }
    
    /**
     * Find date picker
     */
    protected fun findDatePicker(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("date_picker") or
            hasContentDescription("Date") or
            hasText("Date")
        )
    }
    
    /**
     * Find recurring toggle
     */
    protected fun findRecurringToggle(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("recurring_toggle") or
            hasText("Recurring") or
            hasContentDescription("Recurring")
        )
    }
    
    /**
     * Find navigation drawer
     */
    protected fun findNavigationDrawer(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("navigation_drawer") or
            hasContentDescription("Navigation") or
            hasText("Menu")
        )
    }
    
    /**
     * Find home screen
     */
    protected fun findHomeScreen(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("home_screen") or
            hasText("Home") or
            hasContentDescription("Home")
        )
    }
    
    /**
     * Find statistics screen
     */
    protected fun findStatisticsScreen(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("statistics_screen") or
            hasText("Statistics") or
            hasContentDescription("Statistics")
        )
    }
    
    /**
     * Find profile screen
     */
    protected fun findProfileScreen(): SemanticsNodeInteraction {
        return composeTestRule.onNode(
            hasTestTag("profile_screen") or
            hasText("Profile") or
            hasContentDescription("Profile")
        )
    }
    
    /**
     * Enter merchant name
     */
    protected fun enterMerchant(merchant: String) {
        findMerchantField().performTextInput(merchant)
        waitForIdle()
    }
    
    /**
     * Select category
     */
    protected fun selectCategory(category: String) {
        findCategorySelection().performClick()
        waitForIdle()
        
        composeTestRule.onNode(
            hasText(category) or
            hasContentDescription(category)
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Select date
     */
    protected fun selectDate(date: String) {
        findDatePicker().performClick()
        waitForIdle()
        
        composeTestRule.onNode(
            hasText(date) or
            hasContentDescription(date)
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Toggle recurring
     */
    protected fun toggleRecurring() {
        findRecurringToggle().performClick()
        waitForIdle()
    }
    
    /**
     * Verify merchant is entered
     */
    protected fun verifyMerchant(merchant: String) {
        findMerchantField().assertTextContains(merchant)
    }
    
    /**
     * Verify category is selected
     */
    protected fun verifyCategory(category: String) {
        findCategorySelection().assertTextContains(category)
    }
    
    /**
     * Verify date is selected
     */
    protected fun verifyDate(date: String) {
        findDatePicker().assertTextContains(date)
    }
    
    /**
     * Verify recurring is enabled
     */
    protected fun verifyRecurringEnabled() {
        findRecurringToggle().assertIsOn()
    }
    
    /**
     * Verify recurring is disabled
     */
    protected fun verifyRecurringDisabled() {
        findRecurringToggle().assertIsOff()
    }
    
    /**
     * Verify transaction list is displayed
     */
    protected fun verifyTransactionListDisplayed() {
        findTransactionList().assertIsDisplayed()
    }
    
    /**
     * Verify add expense button is displayed
     */
    protected fun verifyAddExpenseButtonDisplayed() {
        findAddExpenseButton().assertIsDisplayed()
    }
    
    /**
     * Verify add expense button is clickable
     */
    protected fun verifyAddExpenseButtonClickable() {
        findAddExpenseButton().assertHasClickAction()
    }
    
    /**
     * Navigate to home screen
     */
    protected fun navigateToHome() {
        composeTestRule.onNode(
            hasText("Home") or
            hasContentDescription("Home")
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Navigate to statistics screen
     */
    protected fun navigateToStatistics() {
        composeTestRule.onNode(
            hasText("Statistics") or
            hasContentDescription("Statistics")
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Navigate to profile screen
     */
    protected fun navigateToProfile() {
        composeTestRule.onNode(
            hasText("Profile") or
            hasContentDescription("Profile")
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Open navigation drawer
     */
    protected fun openNavigationDrawer() {
        composeTestRule.onNode(
            hasContentDescription("Open navigation drawer") or
            hasTestTag("navigation_drawer_button")
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Close navigation drawer
     */
    protected fun closeNavigationDrawer() {
        composeTestRule.onNode(
            hasContentDescription("Close navigation drawer") or
            hasTestTag("navigation_drawer_close")
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Verify navigation drawer is open
     */
    protected fun verifyNavigationDrawerOpen() {
        findNavigationDrawer().assertIsDisplayed()
    }
    
    /**
     * Verify navigation drawer is closed
     */
    protected fun verifyNavigationDrawerClosed() {
        findNavigationDrawer().assertDoesNotExist()
    }
    
    /**
     * Verify home screen is displayed
     */
    protected fun verifyHomeScreenDisplayed() {
        findHomeScreen().assertIsDisplayed()
    }
    
    /**
     * Verify settings screen is displayed
     */
    protected fun verifySettingsScreenDisplayed() {
        findSettingsScreen().assertIsDisplayed()
    }
    
    /**
     * Verify statistics screen is displayed
     */
    protected fun verifyStatisticsScreenDisplayed() {
        findStatisticsScreen().assertIsDisplayed()
    }
    
    /**
     * Verify profile screen is displayed
     */
    protected fun verifyProfileScreenDisplayed() {
        findProfileScreen().assertIsDisplayed()
    }
    
    /**
     * Verify currency conversion toggle is enabled
     */
    protected fun verifyCurrencyConversionEnabled() {
        findCurrencyConversionToggle().assertIsOn()
    }
    
    /**
     * Verify currency conversion toggle is disabled
     */
    protected fun verifyCurrencyConversionDisabled() {
        findCurrencyConversionToggle().assertIsOff()
    }
    
    /**
     * Verify original currency selection is displayed
     */
    protected fun verifyOriginalCurrencySelectionDisplayed() {
        findOriginalCurrencySelection().assertIsDisplayed()
    }
    
    /**
     * Verify original currency selection is not displayed
     */
    protected fun verifyOriginalCurrencySelectionNotDisplayed() {
        findOriginalCurrencySelection().assertDoesNotExist()
    }
    
    /**
     * Verify amount field is empty
     */
    protected fun verifyAmountFieldEmpty() {
        findAmountField().assertTextEquals("")
    }
    
    /**
     * Verify amount field contains text
     */
    protected fun verifyAmountFieldContains(text: String) {
        findAmountField().assertTextContains(text)
    }
    
    /**
     * Verify currency dropdown is displayed
     */
    protected fun verifyCurrencyDropdownDisplayed() {
        findCurrencyDropdown().assertIsDisplayed()
    }
    
    /**
     * Verify currency dropdown is clickable
     */
    protected fun verifyCurrencyDropdownClickable() {
        findCurrencyDropdown().assertHasClickAction()
    }
    
    /**
     * Verify currency dialog is displayed
     */
    protected fun verifyCurrencyDialogDisplayed() {
        findCurrencyDialog().assertIsDisplayed()
    }
    
    /**
     * Verify currency dialog is not displayed
     */
    protected fun verifyCurrencyDialogNotDisplayed() {
        findCurrencyDialog().assertDoesNotExist()
    }
    
    /**
     * Verify error message is not displayed
     */
    protected fun verifyNoErrorMessage() {
        findErrorMessage().assertDoesNotExist()
    }
    
    /**
     * Verify save button is displayed
     */
    protected fun verifySaveButtonDisplayed() {
        findSaveButton().assertIsDisplayed()
    }
    
    /**
     * Verify save button is clickable
     */
    protected fun verifySaveButtonClickable() {
        findSaveButton().assertHasClickAction()
    }
    
    /**
     * Verify save button is not clickable
     */
    protected fun verifySaveButtonNotClickable() {
        findSaveButton().assertIsNotEnabled()
    }
    
    /**
     * Clear merchant field
     */
    protected fun clearMerchantField() {
        findMerchantField().performTextClearance()
        waitForIdle()
    }
    
    /**
     * Clear category selection
     */
    protected fun clearCategorySelection() {
        findCategorySelection().performClick()
        waitForIdle()
        
        composeTestRule.onNode(
            hasText("Clear") or
            hasContentDescription("Clear")
        ).performClick()
        waitForIdle()
    }
    
    /**
     * Reset form to default state
     */
    protected fun resetForm() {
        clearAmountField()
        clearMerchantField()
        clearCategorySelection()
        waitForIdle()
    }
    
    /**
     * Fill complete expense form
     */
    protected fun fillExpenseForm(
        amount: String,
        currency: Currency,
        merchant: String,
        category: String
    ) {
        selectCurrency(currency)
        enterAmount(amount)
        enterMerchant(merchant)
        selectCategory(category)
        waitForIdle()
    }
    
    /**
     * Verify complete expense form is filled
     */
    protected fun verifyExpenseFormFilled(
        amount: String,
        currency: Currency,
        merchant: String,
        category: String
    ) {
        verifyAmountFieldContains(amount)
        verifyCurrencySymbol(currency)
        verifyMerchant(merchant)
        verifyCategory(category)
    }
    
    /**
     * Verify expense form is empty
     */
    protected fun verifyExpenseFormEmpty() {
        verifyAmountFieldEmpty()
        verifyMerchant("")
        // Note: Category and currency have default values, so we don't check them
    }
    
    /**
     * Perform complete expense creation flow
     */
    protected fun createExpense(
        amount: String,
        currency: Currency,
        merchant: String,
        category: String
    ) {
        fillExpenseForm(amount, currency, merchant, category)
        performSave()
        waitForIdle()
    }
    
    /**
     * Verify expense was created successfully
     */
    protected fun verifyExpenseCreated() {
        // This would typically check for success message or navigation back to list
        verifyNoErrorMessage()
        verifySaveButtonEnabled() // Should be enabled for next entry
    }
    
    /**
     * Verify expense creation failed
     */
    protected fun verifyExpenseCreationFailed() {
        verifyErrorMessage("") // Any error message indicates failure
    }
    
    /**
     * Wait for specific text to appear
     */
    protected fun waitForText(text: String, timeoutMillis: Long = 5000) {
        composeTestRule.waitUntil(timeoutMillis) {
            try {
                composeTestRule.onNode(hasText(text)).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
    
    /**
     * Wait for specific node to appear
     */
    protected fun waitForNode(
        matcher: SemanticsMatcher,
        timeoutMillis: Long = 5000
    ) {
        composeTestRule.waitUntil(timeoutMillis) {
            try {
                composeTestRule.onNode(matcher).assertExists()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
    
    /**
     * Wait for specific node to disappear
     */
    protected fun waitForNodeToDisappear(
        matcher: SemanticsMatcher,
        timeoutMillis: Long = 5000
    ) {
        composeTestRule.waitUntil(timeoutMillis) {
            try {
                composeTestRule.onNode(matcher).assertDoesNotExist()
                true
            } catch (e: AssertionError) {
                false
            }
        }
    }
    
    /**
     * Perform swipe gesture
     */
    protected fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {
        composeTestRule.onRoot().performTouchInput {
            swipe(
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                durationMillis = 1000
            )
        }
        waitForIdle()
    }
    
    /**
     * Perform long click
     */
    protected fun performLongClick(matcher: SemanticsMatcher) {
        composeTestRule.onNode(matcher).performTouchInput {
            longClick()
        }
        waitForIdle()
    }
    
    /**
     * Perform double click
     */
    protected fun performDoubleClick(matcher: SemanticsMatcher) {
        composeTestRule.onNode(matcher).performTouchInput {
            doubleClick()
        }
        waitForIdle()
    }
    
    /**
     * Get text content from node
     */
    protected fun getTextContent(matcher: SemanticsMatcher): String {
        return composeTestRule.onNode(matcher).fetchSemanticsNode().config
            .getOrElse(SemanticsProperties.Text) { emptyList() }.firstOrNull()?.text ?: ""
    }
    
    /**
     * Verify node has specific text content
     */
    protected fun verifyNodeText(matcher: SemanticsMatcher, expectedText: String) {
        val actualText = getTextContent(matcher)
        assert(actualText.contains(expectedText)) {
            "Expected text '$expectedText' not found in '$actualText'"
        }
    }
    
    /**
     * Verify node does not have specific text content
     */
    protected fun verifyNodeDoesNotHaveText(matcher: SemanticsMatcher, unexpectedText: String) {
        val actualText = getTextContent(matcher)
        assert(!actualText.contains(unexpectedText)) {
            "Unexpected text '$unexpectedText' found in '$actualText'"
        }
    }
}
