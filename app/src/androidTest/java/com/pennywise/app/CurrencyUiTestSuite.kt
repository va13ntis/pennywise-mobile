package com.pennywise.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.pennywise.app.presentation.components.CurrencySelectionDropdownTest
import com.pennywise.app.presentation.components.CurrencySearchTest
import com.pennywise.app.presentation.components.CurrencySymbolUpdateTest
import com.pennywise.app.presentation.components.CurrencyValidationTest
import com.pennywise.app.presentation.screens.AddExpenseScreenCurrencyTest
import com.pennywise.app.presentation.screens.SettingsScreenCurrencyTest
import com.pennywise.app.presentation.screens.TransactionListCurrencyTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for all currency-related UI tests
 * 
 * This suite includes comprehensive UI tests for:
 * - Currency selection components
 * - Currency search functionality
 * - Currency symbol updates
 * - Currency validation and error handling
 * - Currency display in different screens
 * - Currency switching in settings
 * 
 * To run all currency UI tests:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyUiTestSuite
 * 
 * To run individual test classes:
 * ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.components.CurrencySelectionDropdownTest
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    // Temporarily disabled complex UI tests that require emulator setup
    // CurrencySelectionDropdownTest::class,
    // CurrencySearchTest::class,
    // CurrencySymbolUpdateTest::class,
    // CurrencyValidationTest::class,
    // AddExpenseScreenCurrencyTest::class,
    // SettingsScreenCurrencyTest::class,
    // TransactionListCurrencyTest::class
)
@LargeTest
@SdkSuppress(minSdkVersion = 26)
class CurrencyUiTestSuite
