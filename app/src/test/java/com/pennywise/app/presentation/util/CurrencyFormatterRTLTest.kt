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
 * Comprehensive RTL (Right-to-Left) support tests for CurrencyFormatter
 * Tests currency formatting in RTL languages like Arabic and Hebrew
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CurrencyFormatterRTLTest {
    
    private lateinit var context: Context
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun testHebrewLocaleRTLSupport() {
        val hebrewLocale = Locale("iw", "IL")
        
        // Test basic Hebrew currency formatting
        val result = CurrencyFormatter.formatAmount(100.0, "ILS", context, hebrewLocale)
        
        assertTrue("Hebrew result should contain ₪ symbol", result.contains("₪"))
        assertTrue("Hebrew result should contain amount", result.contains("100"))
        
        // Test RTL locale detection
        assertTrue("Hebrew locale should be detected as RTL", 
            CurrencyFormatter.isRTLLocale(hebrewLocale))
        assertEquals("rtl", CurrencyFormatter.getTextDirection(hebrewLocale))
    }
    
    @Test
    fun testArabicLocaleRTLSupport() {
        val arabicLocale = Locale("ar", "SA")
        
        // Test basic Arabic currency formatting
        val result = CurrencyFormatter.formatAmount(100.0, "SAR", context, arabicLocale)
        
        assertTrue("Arabic result should contain amount", result.contains("100"))
        
        // Test RTL locale detection
        assertTrue("Arabic locale should be detected as RTL", 
            CurrencyFormatter.isRTLLocale(arabicLocale))
        assertEquals("rtl", CurrencyFormatter.getTextDirection(arabicLocale))
    }
    
    @Test
    fun testRTLBidirectionalMarkers() {
        // Test that RTL formatting includes proper bidirectional markers
        val rtlResult = CurrencyFormatter.formatAmountForRTL(100.0, "USD", context)
        
        // Should contain RTL markers (U+200F Right-to-Left Mark)
        assertTrue("RTL result should contain RTL markers", 
            rtlResult.contains("\u200F"))
        
        // Should contain the currency symbol and amount
        assertTrue("RTL result should contain $ symbol", rtlResult.contains("$"))
        assertTrue("RTL result should contain amount", rtlResult.contains("100"))
    }
    
    @Test
    fun testMixedDirectionTextSupport() {
        // Test formatting for mixed LTR/RTL contexts
        val mixedResult = CurrencyFormatter.formatAmount(100.0, "USD", context, forceRTL = true)
        
        // Should contain RTL markers for proper mixed-direction display
        assertTrue("Mixed direction result should contain RTL markers", 
            mixedResult.contains("\u200F"))
    }
    
    @Test
    fun testRTLWithDifferentCurrencies() {
        val currencies = listOf("USD", "EUR", "GBP", "ILS", "SAR", "JPY")
        
        currencies.forEach { currency ->
            val rtlResult = CurrencyFormatter.formatAmountForRTL(100.0, currency, context)
            
            // All should contain RTL markers
            assertTrue("$currency RTL result should contain RTL markers", 
                rtlResult.contains("\u200F"))
            
            // Should contain amount
            assertTrue("$currency RTL result should contain amount", 
                rtlResult.contains("100"))
        }
    }
    
    @Test
    fun testRTLNegativeAmounts() {
        // Test RTL formatting with negative amounts
        val negativeResult = CurrencyFormatter.formatAmountForRTL(-100.0, "USD", context)
        
        assertTrue("RTL negative result should contain RTL markers", 
            negativeResult.contains("\u200F"))
        assertTrue("RTL negative result should contain negative sign or amount", 
            negativeResult.contains("-") || negativeResult.contains("100"))
    }
    
    @Test
    fun testRTLZeroAmounts() {
        // Test RTL formatting with zero amounts
        val zeroResult = CurrencyFormatter.formatAmountForRTL(0.0, "USD", context)
        
        assertTrue("RTL zero result should contain RTL markers", 
            zeroResult.contains("\u200F"))
        assertTrue("RTL zero result should contain zero", 
            zeroResult.contains("0"))
    }
    
    @Test
    fun testRTLWithLargeAmounts() {
        // Test RTL formatting with large amounts
        val largeResult = CurrencyFormatter.formatAmountForRTL(1234567.89, "USD", context)
        
        assertTrue("RTL large result should contain RTL markers", 
            largeResult.contains("\u200F"))
        assertTrue("RTL large result should contain amount", 
            largeResult.contains("1234567") || largeResult.contains("1,234,567"))
    }
    
    @Test
    fun testRTLConversionFormatting() {
        // Test RTL formatting with currency conversion
        val conversionResult = CurrencyFormatter.formatAmountWithConversion(
            originalAmount = 100.0,
            convertedAmount = 85.0,
            originalCurrency = "USD",
            targetCurrency = "EUR",
            context = context,
            showRate = true,
            conversionRate = 0.85
        )
        
        // Should contain both currencies and conversion info
        assertTrue("RTL conversion result should contain USD", conversionResult.contains("$100"))
        assertTrue("RTL conversion result should contain EUR", conversionResult.contains("€85"))
        assertTrue("RTL conversion result should contain rate", conversionResult.contains("0.8500"))
    }
    
    @Test
    fun testRTLSymbolRetrieval() {
        // Test RTL-specific symbol retrieval
        val hebrewLocale = Locale("iw", "IL")
        val arabicLocale = Locale("ar", "SA")
        
        // Test symbol retrieval for RTL locales
        val ilsSymbol = CurrencyFormatter.getCurrencySymbol("ILS", hebrewLocale)
        val sarSymbol = CurrencyFormatter.getCurrencySymbol("SAR", arabicLocale)
        
        assertEquals("₪", ilsSymbol)
        assertNotNull("SAR symbol should not be null", sarSymbol)
    }
    
    @Test
    fun testRTLFractionDigits() {
        // Test RTL formatting with different decimal precision
        val jpyResult = CurrencyFormatter.formatAmountForRTL(1000.0, "JPY", context)
        val usdResult = CurrencyFormatter.formatAmountForRTL(100.0, "USD", context)
        
        // Both should contain RTL markers
        assertTrue("JPY RTL result should contain RTL markers", jpyResult.contains("\u200F"))
        assertTrue("USD RTL result should contain RTL markers", usdResult.contains("\u200F"))
        
        // JPY should not have decimal places, USD should
        assertTrue("JPY RTL result should not contain .00", !jpyResult.contains(".00"))
        assertTrue("USD RTL result should contain decimal places", usdResult.contains(".00"))
    }
}
