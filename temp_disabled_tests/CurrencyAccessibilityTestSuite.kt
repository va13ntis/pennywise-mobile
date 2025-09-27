package com.pennywise.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.Suite
import com.pennywise.app.presentation.components.CurrencyAccessibilityTest
import com.pennywise.app.presentation.components.CurrencyEspressoAccessibilityTest
import com.pennywise.app.presentation.components.CurrencySelectionViewAccessibilityTest
import com.pennywise.app.presentation.components.CurrencyTalkBackAccessibilityTest
import org.junit.runner.RunWith
import org.junit.runners.Suite.SuiteClasses

/**
 * Comprehensive test suite for currency UI component accessibility
 * 
 * This test suite includes:
 * 1. Compose-based accessibility tests
 * 2. Espresso accessibility checks
 * 3. Custom view accessibility tests
 * 4. TalkBack compatibility tests
 * 
 * Run this suite to verify all currency UI components meet accessibility guidelines
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
@Suite.SuiteClasses(
    CurrencyAccessibilityTest::class,
    CurrencyEspressoAccessibilityTest::class,
    CurrencySelectionViewAccessibilityTest::class,
    CurrencyTalkBackAccessibilityTest::class
)
class CurrencyAccessibilityTestSuite
