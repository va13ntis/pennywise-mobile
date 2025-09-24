package com.pennywise.app.presentation.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.*

/**
 * Unit tests for CurrencyFormatter utility class
 * Tests various currency formatting scenarios including RTL support, decimal precision, and edge cases
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class CurrencyFormatterTest {
    
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun testBasicCurrencyFormatting() {
        // Test USD formatting
        val usdResult = CurrencyFormatter.formatAmount(10.0, "USD", context)
        assertTrue("USD formatting should contain $ symbol", usdResult.contains("$"))
        assertTrue("USD formatting should contain amount", usdResult.contains("10"))
        
        // Test EUR formatting
        val eurResult = CurrencyFormatter.formatAmount(15.0, "EUR", context)
        assertTrue("EUR formatting should contain € symbol", eurResult.contains("€"))
        assertTrue("EUR formatting should contain amount", eurResult.contains("15"))
        
        // Test GBP formatting
        val gbpResult = CurrencyFormatter.formatAmount(20.0, "GBP", context)
        assertTrue("GBP formatting should contain £ symbol", gbpResult.contains("£"))
        assertTrue("GBP formatting should contain amount", gbpResult.contains("20"))
    }
    
    @Test
    fun testJPYFormattingNoDecimals() {
        // JPY should not have decimal places
        val jpyResult = CurrencyFormatter.formatAmount(1000.0, "JPY", context)
        assertTrue("JPY formatting should contain ¥ symbol", jpyResult.contains("¥"))
        assertTrue("JPY formatting should not contain decimal places", !jpyResult.contains(".00"))
        assertTrue("JPY formatting should contain amount", jpyResult.contains("1000"))
    }
    
    @Test
    fun testNegativeAmounts() {
        // Test negative USD formatting
        val negativeResult = CurrencyFormatter.formatAmount(-25.5, "USD", context)
        assertTrue("Negative amount should be formatted correctly", negativeResult.contains("-"))
        assertTrue("Negative amount should contain currency symbol", negativeResult.contains("$"))
    }
    
    @Test
    fun testInvalidCurrencyCode() {
        // Should fall back to USD when invalid currency code is provided
        val invalidResult = CurrencyFormatter.formatAmount(10.0, "XYZ", context)
        assertTrue("Invalid currency should fall back to USD", invalidResult.contains("$"))
        assertTrue("Invalid currency should still format amount", invalidResult.contains("10"))
    }
    
    @Test
    fun testCurrencySymbolRetrieval() {
        // Test getting currency symbols
        assertEquals("$", CurrencyFormatter.getCurrencySymbol("USD"))
        assertEquals("€", CurrencyFormatter.getCurrencySymbol("EUR"))
        assertEquals("£", CurrencyFormatter.getCurrencySymbol("GBP"))
        assertEquals("¥", CurrencyFormatter.getCurrencySymbol("JPY"))
        assertEquals("₪", CurrencyFormatter.getCurrencySymbol("ILS"))
        assertEquals("₽", CurrencyFormatter.getCurrencySymbol("RUB"))
        
        // Test invalid currency code
        assertEquals("$", CurrencyFormatter.getCurrencySymbol("INVALID"))
    }
    
    @Test
    fun testAmountWithoutSymbol() {
        val amountOnly = CurrencyFormatter.formatAmountWithoutSymbol(123.45, "USD", Locale.US)
        assertTrue("Amount without symbol should not contain $", !amountOnly.contains("$"))
        assertTrue("Amount without symbol should contain formatted number", amountOnly.contains("123"))
    }
    
    @Test
    fun testAmountWithSeparateSymbol() {
        val (amount, symbol) = CurrencyFormatter.formatAmountWithSeparateSymbol(123.45, "USD", context)
        assertEquals("$", symbol)
        assertTrue("Amount should be formatted without symbol", !amount.contains("$"))
        assertTrue("Amount should contain formatted number", amount.contains("123"))
    }
    
    @Test
    fun testCurrencyConversionFormatting() {
        val result = CurrencyFormatter.formatAmountWithConversion(
            originalAmount = 100.0,
            convertedAmount = 85.0,
            originalCurrency = "USD",
            targetCurrency = "EUR",
            context = context,
            showRate = true,
            conversionRate = 0.85
        )
        
        assertTrue("Conversion result should contain original amount", result.contains("$100"))
        assertTrue("Conversion result should contain converted amount", result.contains("€85"))
        assertTrue("Conversion result should contain conversion rate", result.contains("0.8500"))
    }
    
    @Test
    fun testCurrencyConversionFormattingWithoutRate() {
        val result = CurrencyFormatter.formatAmountWithConversion(
            originalAmount = 100.0,
            convertedAmount = 85.0,
            originalCurrency = "USD",
            targetCurrency = "EUR",
            context = context,
            showRate = false
        )
        
        assertTrue("Conversion result should contain original amount", result.contains("$100"))
        assertTrue("Conversion result should contain converted amount", result.contains("€85"))
        assertTrue("Conversion result should not contain conversion rate", !result.contains("0.85"))
    }
    
    @Test
    fun testIsValidCurrencyCode() {
        assertTrue("USD should be valid", CurrencyFormatter.isValidCurrencyCode("USD"))
        assertTrue("EUR should be valid", CurrencyFormatter.isValidCurrencyCode("EUR"))
        assertTrue("JPY should be valid", CurrencyFormatter.isValidCurrencyCode("JPY"))
        assertFalse("INVALID should not be valid", CurrencyFormatter.isValidCurrencyCode("INVALID"))
        assertFalse("Empty string should not be valid", CurrencyFormatter.isValidCurrencyCode(""))
    }
    
    @Test
    fun testGetDefaultFractionDigits() {
        assertEquals("USD should have 2 decimal places", 2, CurrencyFormatter.getDefaultFractionDigits("USD"))
        assertEquals("EUR should have 2 decimal places", 2, CurrencyFormatter.getDefaultFractionDigits("EUR"))
        assertEquals("JPY should have 0 decimal places", 0, CurrencyFormatter.getDefaultFractionDigits("JPY"))
        assertEquals("Invalid currency should default to 2 decimal places", 2, CurrencyFormatter.getDefaultFractionDigits("INVALID"))
    }
    
    @Test
    fun testLargeAmountFormatting() {
        // Test without abbreviations
        val largeAmount = CurrencyFormatter.formatLargeAmount(1500000.0, "USD", context, false)
        assertTrue("Large amount should contain full number", largeAmount.contains("1,500,000"))
        
        // Test with abbreviations
        val abbreviatedAmount = CurrencyFormatter.formatLargeAmount(1500000.0, "USD", context, true)
        assertTrue("Abbreviated amount should contain M", abbreviatedAmount.contains("M"))
        assertTrue("Abbreviated amount should contain 1.5", abbreviatedAmount.contains("1.5"))
    }
    
    @Test
    fun testZeroAmountFormatting() {
        // Test normal zero formatting
        val normalZero = CurrencyFormatter.formatZeroAmount("USD", context, false)
        assertTrue("Normal zero should contain $0", normalZero.contains("$0"))
        
        // Test free zero formatting
        val freeZero = CurrencyFormatter.formatZeroAmount("USD", context, true)
        assertEquals("Free zero should show 'Free'", "Free", freeZero)
    }
    
    @Test
    fun testDifferentLocales() {
        // Test with specific locales
        val usResult = CurrencyFormatter.formatAmount(10.0, "USD", context, Locale.US)
        val frResult = CurrencyFormatter.formatAmount(10.0, "USD", context, Locale.FRANCE)
        
        // Results should be different due to locale-specific formatting
        assertNotEquals("US and French locales should format differently", usResult, frResult)
        
        // Both should contain the amount
        assertTrue("US result should contain amount", usResult.contains("10"))
        assertTrue("French result should contain amount", frResult.contains("10"))
    }
    
    @Test
    fun testRTLSupport() {
        // Test with Hebrew locale (RTL)
        val hebrewResult = CurrencyFormatter.formatAmount(100.0, "ILS", context, Locale("iw", "IL"))
        
        // Should contain the currency symbol and amount
        assertTrue("Hebrew result should contain ₪ symbol", hebrewResult.contains("₪"))
        assertTrue("Hebrew result should contain amount", hebrewResult.contains("100"))
        
        // Should contain RTL markers if needed
        // Note: The actual RTL marker behavior depends on the system's text direction handling
    }
    
    @Test
    fun testForceRTLFormatting() {
        // Test forcing RTL formatting
        val forcedRTLResult = CurrencyFormatter.formatAmount(100.0, "USD", context, forceRTL = true)
        
        // Should contain RTL markers
        assertTrue("Forced RTL result should contain RTL markers", 
            forcedRTLResult.contains("\u200F") && forcedRTLResult.contains("\u200F"))
    }
    
    @Test
    fun testFormatAmountForRTL() {
        // Test RTL-specific formatting
        val rtlResult = CurrencyFormatter.formatAmountForRTL(100.0, "USD", context)
        
        // Should always contain RTL markers
        assertTrue("RTL result should contain RTL markers", 
            rtlResult.contains("\u200F") && rtlResult.contains("\u200F"))
        assertTrue("RTL result should contain amount", rtlResult.contains("100"))
    }
    
    @Test
    fun testIsRTLLocale() {
        // Test RTL locale detection
        assertTrue("Hebrew locale should be RTL", CurrencyFormatter.isRTLLocale(Locale("iw", "IL")))
        assertTrue("Arabic locale should be RTL", CurrencyFormatter.isRTLLocale(Locale("ar", "SA")))
        assertFalse("English locale should not be RTL", CurrencyFormatter.isRTLLocale(Locale.US))
        assertFalse("French locale should not be RTL", CurrencyFormatter.isRTLLocale(Locale.FRANCE))
    }
    
    @Test
    fun testGetTextDirection() {
        // Test text direction detection
        assertEquals("rtl", CurrencyFormatter.getTextDirection(Locale("iw", "IL")))
        assertEquals("rtl", CurrencyFormatter.getTextDirection(Locale("ar", "SA")))
        assertEquals("ltr", CurrencyFormatter.getTextDirection(Locale.US))
        assertEquals("ltr", CurrencyFormatter.getTextDirection(Locale.FRANCE))
    }
    
    @Test
    fun testRTLWithDifferentCurrencies() {
        // Test RTL formatting with different currencies
        val usdRTL = CurrencyFormatter.formatAmountForRTL(100.0, "USD", context)
        val eurRTL = CurrencyFormatter.formatAmountForRTL(100.0, "EUR", context)
        val ilsRTL = CurrencyFormatter.formatAmountForRTL(100.0, "ILS", context)
        
        // All should contain RTL markers
        assertTrue("USD RTL should contain markers", usdRTL.contains("\u200F"))
        assertTrue("EUR RTL should contain markers", eurRTL.contains("\u200F"))
        assertTrue("ILS RTL should contain markers", ilsRTL.contains("\u200F"))
        
        // Should contain respective currency symbols
        assertTrue("USD RTL should contain $", usdRTL.contains("$"))
        assertTrue("EUR RTL should contain €", eurRTL.contains("€"))
        assertTrue("ILS RTL should contain ₪", ilsRTL.contains("₪"))
    }
    
    @Test
    fun testEdgeCases() {
        // Test very small amounts
        val smallAmount = CurrencyFormatter.formatAmount(0.01, "USD", context)
        assertTrue("Small amount should be formatted", smallAmount.contains("$"))
        
        // Test very large amounts
        val veryLargeAmount = CurrencyFormatter.formatAmount(999999999.99, "USD", context)
        assertTrue("Very large amount should be formatted", veryLargeAmount.contains("$"))
        
        // Test zero amount
        val zeroAmount = CurrencyFormatter.formatAmount(0.0, "USD", context)
        assertTrue("Zero amount should be formatted", zeroAmount.contains("$0"))
        
        // Test NaN
        val nanAmount = CurrencyFormatter.formatAmount(Double.NaN, "USD", context)
        assertFalse("NaN amount should be handled gracefully", nanAmount.isEmpty())
        
        // Test Infinity
        val positiveInfinityAmount = CurrencyFormatter.formatAmount(Double.POSITIVE_INFINITY, "USD", context)
        val negativeInfinityAmount = CurrencyFormatter.formatAmount(Double.NEGATIVE_INFINITY, "USD", context)
        assertFalse("Positive infinity amount should be handled gracefully", positiveInfinityAmount.isEmpty())
        assertFalse("Negative infinity amount should be handled gracefully", negativeInfinityAmount.isEmpty())
    }
    
    @Test
    fun testCurrencyCodeCaseInsensitivity() {
        val upperResult = CurrencyFormatter.formatAmount(10.0, "USD", context)
        val lowerResult = CurrencyFormatter.formatAmount(10.0, "usd", context)
        val mixedResult = CurrencyFormatter.formatAmount(10.0, "Usd", context)
        
        // All should produce the same result
        assertEquals("Upper case should work", upperResult, lowerResult)
        assertEquals("Mixed case should work", upperResult, mixedResult)
    }
    
    @Test
    fun testFormatCurrenciesWithDifferentDecimalPlaces() {
        // BHD (Bahraini Dinar) has 3 decimal places
        val bhdResult = CurrencyFormatter.formatAmount(1.234, "BHD", context)
        assertTrue("BHD should show 3 decimal places", bhdResult.contains(".234") || bhdResult.contains(",234"))
        
        // JPY has 0 decimal places
        val jpyResult = CurrencyFormatter.formatAmount(1.234, "JPY", context)
        assertFalse("JPY should not show decimal places", jpyResult.contains(".") || jpyResult.contains(","))
        
        // USD has 2 decimal places
        val usdResult = CurrencyFormatter.formatAmount(1.234, "USD", context)
        assertTrue("USD should show 2 decimal places", usdResult.contains(".23") || usdResult.contains(",23"))
    }
    
    @Test
    fun testCurrenciesWithDifferentSymbolPositions() {
        // USD typically places the symbol before the amount
        val usdResult = CurrencyFormatter.formatAmount(10.0, "USD", context, Locale.US)
        assertTrue("USD should have symbol before amount", usdResult.startsWith("$"))
        
        // EUR in French locale typically places the symbol after the amount
        val eurResult = CurrencyFormatter.formatAmount(10.0, "EUR", context, Locale.FRANCE)
        assertFalse("EUR in French locale may have symbol after amount", eurResult.startsWith("€"))
    }
    
    @Test
    fun testSpecialCurrencyFormattingRules() {
        // Test formatting with grouping separators
        val largeAmountWithSeparators = CurrencyFormatter.formatAmount(1234567.89, "USD", context, Locale.US)
        assertTrue("Large amount should contain grouping separators", 
            largeAmountWithSeparators.contains(",") || largeAmountWithSeparators.contains(" "))
        
        // Test different grouping separators in different locales
        val usFormatting = CurrencyFormatter.formatAmount(1234.56, "USD", context, Locale.US)
        val germanFormatting = CurrencyFormatter.formatAmount(1234.56, "EUR", context, Locale.GERMANY)
        
        // US uses . for decimal and , for thousands
        // German uses , for decimal and . for thousands
        assertNotEquals("US and German number formatting should differ", usFormatting, germanFormatting)
    }
}
