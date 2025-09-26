package com.pennywise.app.presentation.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config
import java.util.*

/**
 * Unit tests for CurrencyFormatter utility class
 * Tests various currency formatting scenarios including RTL support, decimal precision, and edge cases
 */
@Config(sdk = [34], application = android.app.Application::class)
class CurrencyFormatterTest {
    
    private lateinit var context: Context
    
    @BeforeEach
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun testBasicCurrencyFormatting() {
        // Test USD formatting
        val usdResult = CurrencyFormatter.formatAmount(10.0, "USD", context)
        assertTrue(usdResult.contains("$"), "USD formatting should contain $ symbol")
        assertTrue(usdResult.contains("10"), "USD formatting should contain amount")
        
        // Test EUR formatting
        val eurResult = CurrencyFormatter.formatAmount(15.0, "EUR", context)
        assertTrue(eurResult.contains("€"), "EUR formatting should contain € symbol")
        assertTrue(eurResult.contains("15"), "EUR formatting should contain amount")
        
        // Test GBP formatting
        val gbpResult = CurrencyFormatter.formatAmount(20.0, "GBP", context)
        assertTrue(gbpResult.contains("£"), "GBP formatting should contain £ symbol")
        assertTrue(gbpResult.contains("20"), "GBP formatting should contain amount")
    }
    
    @Test
    fun testJPYFormattingNoDecimals() {
        // JPY should not have decimal places
        val jpyResult = CurrencyFormatter.formatAmount(1000.0, "JPY", context)
        assertTrue(jpyResult.contains("¥"), "JPY formatting should contain ¥ symbol")
        assertTrue(!jpyResult.contains(".00"), "JPY formatting should not contain decimal places")
        assertTrue(jpyResult.contains("1000"), "JPY formatting should contain amount")
    }
    
    @Test
    fun testNegativeAmounts() {
        // Test negative USD formatting
        val negativeResult = CurrencyFormatter.formatAmount(-25.5, "USD", context)
        assertTrue(negativeResult.contains("-"), "Negative amount should be formatted correctly")
        assertTrue(negativeResult.contains("$"), "Negative amount should contain currency symbol")
    }
    
    @Test
    fun testInvalidCurrencyCode() {
        // Should fall back to USD when invalid currency code is provided
        val invalidResult = CurrencyFormatter.formatAmount(10.0, "XYZ", context)
        assertTrue(invalidResult.contains("$"), "Invalid currency should fall back to USD")
        assertTrue(invalidResult.contains("10"), "Invalid currency should still format amount")
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
        assertTrue(!amountOnly.contains("$"), "Amount without symbol should not contain $")
        assertTrue(amountOnly.contains("123"), "Amount without symbol should contain formatted number")
    }
    
    @Test
    fun testAmountWithSeparateSymbol() {
        val (amount, symbol) = CurrencyFormatter.formatAmountWithSeparateSymbol(123.45, "USD", context)
        assertEquals("$", symbol)
        assertTrue(!amount.contains("$"), "Amount should be formatted without symbol")
        assertTrue(amount.contains("123"), "Amount should contain formatted number")
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
        
        assertTrue(result.contains("$100"), "Conversion result should contain original amount")
        assertTrue(result.contains("€85"), "Conversion result should contain converted amount")
        assertTrue(result.contains("0.8500"), "Conversion result should contain conversion rate")
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
        
        assertTrue(result.contains("$100"), "Conversion result should contain original amount")
        assertTrue(result.contains("€85"), "Conversion result should contain converted amount")
        assertTrue(!result.contains("0.85"), "Conversion result should not contain conversion rate")
    }
    
    @Test
    fun testIsValidCurrencyCode() {
        assertTrue(CurrencyFormatter.isValidCurrencyCode("USD"), "USD should be valid")
        assertTrue(CurrencyFormatter.isValidCurrencyCode("EUR"), "EUR should be valid")
        assertTrue(CurrencyFormatter.isValidCurrencyCode("JPY"), "JPY should be valid")
        assertFalse(CurrencyFormatter.isValidCurrencyCode("INVALID"), "INVALID should not be valid")
        assertFalse(CurrencyFormatter.isValidCurrencyCode(""), "Empty string should not be valid")
    }
    
    @Test
    fun testGetDefaultFractionDigits() {
        assertEquals(2, CurrencyFormatter.getDefaultFractionDigits("USD"), "USD should have 2 decimal places")
        assertEquals(2, CurrencyFormatter.getDefaultFractionDigits("EUR"), "EUR should have 2 decimal places")
        assertEquals(0, CurrencyFormatter.getDefaultFractionDigits("JPY"), "JPY should have 0 decimal places")
        assertEquals(2, CurrencyFormatter.getDefaultFractionDigits("INVALID"), "Invalid currency should default to 2 decimal places")
    }
    
    @Test
    fun testLargeAmountFormatting() {
        // Test without abbreviations
        val largeAmount = CurrencyFormatter.formatLargeAmount(1500000.0, "USD", context, false)
        assertTrue(largeAmount.contains("1,500,000"), "Large amount should contain full number")
        
        // Test with abbreviations
        val abbreviatedAmount = CurrencyFormatter.formatLargeAmount(1500000.0, "USD", context, true)
        assertTrue(abbreviatedAmount.contains("M"), "Abbreviated amount should contain M")
        assertTrue(abbreviatedAmount.contains("1.5"), "Abbreviated amount should contain 1.5")
    }
    
    @Test
    fun testZeroAmountFormatting() {
        // Test normal zero formatting
        val normalZero = CurrencyFormatter.formatZeroAmount("USD", context, false)
        assertTrue(normalZero.contains("$0"), "Normal zero should contain $0")
        
        // Test free zero formatting
        val freeZero = CurrencyFormatter.formatZeroAmount("USD", context, true)
        assertEquals("Free", freeZero, "Free zero should show 'Free'")
    }
    
    @Test
    fun testDifferentLocales() {
        // Test with specific locales
        val usResult = CurrencyFormatter.formatAmount(10.0, "USD", context, Locale.US)
        val frResult = CurrencyFormatter.formatAmount(10.0, "USD", context, Locale.FRANCE)
        
        // Results should be different due to locale-specific formatting
        assertNotEquals(usResult, frResult, "US and French locales should format differently")
        
        // Both should contain the amount
        assertTrue(usResult.contains("10"), "US result should contain amount")
        assertTrue(frResult.contains("10"), "French result should contain amount")
    }
    
    @Test
    fun testRTLSupport() {
        // Test with Hebrew locale (RTL)
        val hebrewResult = CurrencyFormatter.formatAmount(100.0, "ILS", context, Locale("iw", "IL"))
        
        // Should contain the currency symbol and amount
        assertTrue(hebrewResult.contains("₪"), "Hebrew result should contain ₪ symbol")
        assertTrue(hebrewResult.contains("100"), "Hebrew result should contain amount")
        
        // Should contain RTL markers if needed
        // Note: The actual RTL marker behavior depends on the system's text direction handling
    }
    
    @Test
    fun testForceRTLFormatting() {
        // Test forcing RTL formatting
        val forcedRTLResult = CurrencyFormatter.formatAmount(100.0, "USD", context, forceRTL = true)
        
        // Should contain RTL markers
        assertTrue(forcedRTLResult.contains("\u200F") && forcedRTLResult.contains("\u200F"), 
            "Forced RTL result should contain RTL markers")
    }
    
    @Test
    fun testFormatAmountForRTL() {
        // Test RTL-specific formatting
        val rtlResult = CurrencyFormatter.formatAmountForRTL(100.0, "USD", context)
        
        // Should always contain RTL markers
        assertTrue(rtlResult.contains("\u200F") && rtlResult.contains("\u200F"), 
            "RTL result should contain RTL markers")
        assertTrue(rtlResult.contains("100"), "RTL result should contain amount")
    }
    
    @Test
    fun testIsRTLLocale() {
        // Test RTL locale detection
        assertTrue(CurrencyFormatter.isRTLLocale(Locale("iw", "IL")), "Hebrew locale should be RTL")
        assertTrue(CurrencyFormatter.isRTLLocale(Locale("ar", "SA")), "Arabic locale should be RTL")
        assertFalse(CurrencyFormatter.isRTLLocale(Locale.US), "English locale should not be RTL")
        assertFalse(CurrencyFormatter.isRTLLocale(Locale.FRANCE), "French locale should not be RTL")
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
        assertTrue(usdRTL.contains("\u200F"), "USD RTL should contain markers")
        assertTrue(eurRTL.contains("\u200F"), "EUR RTL should contain markers")
        assertTrue(ilsRTL.contains("\u200F"), "ILS RTL should contain markers")
        
        // Should contain respective currency symbols
        assertTrue(usdRTL.contains("$"), "USD RTL should contain $")
        assertTrue(eurRTL.contains("€"), "EUR RTL should contain €")
        assertTrue(ilsRTL.contains("₪"), "ILS RTL should contain ₪")
    }
    
    @Test
    fun testEdgeCases() {
        // Test very small amounts
        val smallAmount = CurrencyFormatter.formatAmount(0.01, "USD", context)
        assertTrue(smallAmount.contains("$"), "Small amount should be formatted")
        
        // Test very large amounts
        val veryLargeAmount = CurrencyFormatter.formatAmount(999999999.99, "USD", context)
        assertTrue(veryLargeAmount.contains("$"), "Very large amount should be formatted")
        
        // Test zero amount
        val zeroAmount = CurrencyFormatter.formatAmount(0.0, "USD", context)
        assertTrue(zeroAmount.contains("$0"), "Zero amount should be formatted")
        
        // Test NaN
        val nanAmount = CurrencyFormatter.formatAmount(Double.NaN, "USD", context)
        assertFalse(nanAmount.isEmpty(), "NaN amount should be handled gracefully")
        
        // Test Infinity
        val positiveInfinityAmount = CurrencyFormatter.formatAmount(Double.POSITIVE_INFINITY, "USD", context)
        val negativeInfinityAmount = CurrencyFormatter.formatAmount(Double.NEGATIVE_INFINITY, "USD", context)
        assertFalse(positiveInfinityAmount.isEmpty(), "Positive infinity amount should be handled gracefully")
        assertFalse(negativeInfinityAmount.isEmpty(), "Negative infinity amount should be handled gracefully")
    }
    
    @Test
    fun testCurrencyCodeCaseInsensitivity() {
        val upperResult = CurrencyFormatter.formatAmount(10.0, "USD", context)
        val lowerResult = CurrencyFormatter.formatAmount(10.0, "usd", context)
        val mixedResult = CurrencyFormatter.formatAmount(10.0, "Usd", context)
        
        // All should produce the same result
        assertEquals(upperResult, lowerResult, "Upper case should work")
        assertEquals(upperResult, mixedResult, "Mixed case should work")
    }
    
    @Test
    fun testFormatCurrenciesWithDifferentDecimalPlaces() {
        // BHD (Bahraini Dinar) has 3 decimal places
        val bhdResult = CurrencyFormatter.formatAmount(1.234, "BHD", context)
        assertTrue(bhdResult.contains(".234") || bhdResult.contains(",234"), "BHD should show 3 decimal places")
        
        // JPY has 0 decimal places
        val jpyResult = CurrencyFormatter.formatAmount(1.234, "JPY", context)
        assertFalse(jpyResult.contains(".") || jpyResult.contains(","), "JPY should not show decimal places")
        
        // USD has 2 decimal places
        val usdResult = CurrencyFormatter.formatAmount(1.234, "USD", context)
        assertTrue(usdResult.contains(".23") || usdResult.contains(",23"), "USD should show 2 decimal places")
    }
    
    @Test
    fun testCurrenciesWithDifferentSymbolPositions() {
        // USD typically places the symbol before the amount
        val usdResult = CurrencyFormatter.formatAmount(10.0, "USD", context, Locale.US)
        assertTrue(usdResult.startsWith("$"), "USD should have symbol before amount")
        
        // EUR in French locale typically places the symbol after the amount
        val eurResult = CurrencyFormatter.formatAmount(10.0, "EUR", context, Locale.FRANCE)
        assertFalse(eurResult.startsWith("€"), "EUR in French locale may have symbol after amount")
    }
    
    @Test
    fun testSpecialCurrencyFormattingRules() {
        // Test formatting with grouping separators
        val largeAmountWithSeparators = CurrencyFormatter.formatAmount(1234567.89, "USD", context, Locale.US)
        assertTrue(largeAmountWithSeparators.contains(",") || largeAmountWithSeparators.contains(" "), 
            "Large amount should contain grouping separators")
        
        // Test different grouping separators in different locales
        val usFormatting = CurrencyFormatter.formatAmount(1234.56, "USD", context, Locale.US)
        val germanFormatting = CurrencyFormatter.formatAmount(1234.56, "EUR", context, Locale.GERMANY)
        
        // US uses . for decimal and , for thousands
        // German uses , for decimal and . for thousands
        assertNotEquals("US and German number formatting should differ", usFormatting, germanFormatting)
    }
}
