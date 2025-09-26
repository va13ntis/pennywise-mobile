package com.pennywise.app.presentation.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config
import java.util.*

/**
 * Comprehensive RTL (Right-to-Left) support tests for CurrencyFormatter
 * Tests currency formatting in RTL languages like Arabic and Hebrew
 */
@Config(sdk = [34], application = android.app.Application::class)
class CurrencyFormatterRTLTest {
    
    private lateinit var context: Context
    
    @BeforeEach
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }
    
    @Test
    fun testHebrewLocaleRTLSupport() {
        val hebrewLocale = Locale("iw", "IL")
        
        // Test basic Hebrew currency formatting
        val result = CurrencyFormatter.formatAmount(100.0, "ILS", context, hebrewLocale)
        
        assertTrue(result.contains("₪"), "Hebrew result should contain ₪ symbol")
        assertTrue(result.contains("100"), "Hebrew result should contain amount")
        
        // Test RTL locale detection
        assertTrue(CurrencyFormatter.isRTLLocale(hebrewLocale), 
            "Hebrew locale should be detected as RTL")
        assertEquals("rtl", CurrencyFormatter.getTextDirection(hebrewLocale))
    }
    
    @Test
    fun testArabicLocaleRTLSupport() {
        val arabicLocale = Locale("ar", "SA")
        
        // Test basic Arabic currency formatting
        val result = CurrencyFormatter.formatAmount(100.0, "SAR", context, arabicLocale)
        
        assertTrue(result.contains("100"), "Arabic result should contain amount")
        
        // Test RTL locale detection
        assertTrue(CurrencyFormatter.isRTLLocale(arabicLocale), 
            "Arabic locale should be detected as RTL")
        assertEquals("rtl", CurrencyFormatter.getTextDirection(arabicLocale))
    }
    
    @Test
    fun testRTLBidirectionalMarkers() {
        // Test that RTL formatting includes proper bidirectional markers
        val rtlResult = CurrencyFormatter.formatAmountForRTL(100.0, "USD", context)
        
        // Should contain RTL markers (U+200F Right-to-Left Mark)
        assertTrue(rtlResult.contains("\u200F"), 
            "RTL result should contain RTL markers")
        
        // Should contain the currency symbol and amount
        assertTrue(rtlResult.contains("$"), "RTL result should contain $ symbol")
        assertTrue(rtlResult.contains("100"), "RTL result should contain amount")
    }
    
    @Test
    fun testMixedDirectionTextSupport() {
        // Test formatting for mixed LTR/RTL contexts
        val mixedResult = CurrencyFormatter.formatAmount(100.0, "USD", context, forceRTL = true)
        
        // Should contain RTL markers for proper mixed-direction display
        assertTrue(mixedResult.contains("\u200F"), 
            "Mixed direction result should contain RTL markers")
    }
    
    @Test
    fun testRTLWithDifferentCurrencies() {
        val currencies = listOf("USD", "EUR", "GBP", "ILS", "SAR", "JPY")
        
        currencies.forEach { currency ->
            val rtlResult = CurrencyFormatter.formatAmountForRTL(100.0, currency, context)
            
            // All should contain RTL markers
            assertTrue(rtlResult.contains("\u200F"), 
                "$currency RTL result should contain RTL markers")
            
            // Should contain amount
            assertTrue(rtlResult.contains("100"), 
                "$currency RTL result should contain amount")
        }
    }
    
    @Test
    fun testRTLNegativeAmounts() {
        // Test RTL formatting with negative amounts
        val negativeResult = CurrencyFormatter.formatAmountForRTL(-100.0, "USD", context)
        
        assertTrue(negativeResult.contains("\u200F"), 
            "RTL negative result should contain RTL markers")
        assertTrue(negativeResult.contains("-") || negativeResult.contains("100"), 
            "RTL negative result should contain negative sign or amount")
    }
    
    @Test
    fun testRTLZeroAmounts() {
        // Test RTL formatting with zero amounts
        val zeroResult = CurrencyFormatter.formatAmountForRTL(0.0, "USD", context)
        
        assertTrue(zeroResult.contains("\u200F"), 
            "RTL zero result should contain RTL markers")
        assertTrue(zeroResult.contains("0"), 
            "RTL zero result should contain zero")
    }
    
    @Test
    fun testRTLWithLargeAmounts() {
        // Test RTL formatting with large amounts
        val largeResult = CurrencyFormatter.formatAmountForRTL(1234567.89, "USD", context)
        
        assertTrue(largeResult.contains("\u200F"), 
            "RTL large result should contain RTL markers")
        assertTrue(largeResult.contains("1234567") || largeResult.contains("1,234,567"), 
            "RTL large result should contain amount")
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
        assertTrue(conversionResult.contains("$100"), "RTL conversion result should contain USD")
        assertTrue(conversionResult.contains("€85"), "RTL conversion result should contain EUR")
        assertTrue(conversionResult.contains("0.8500"), "RTL conversion result should contain rate")
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
        assertNotNull(sarSymbol, "SAR symbol should not be null")
    }
    
    @Test
    fun testRTLFractionDigits() {
        // Test RTL formatting with different decimal precision
        val jpyResult = CurrencyFormatter.formatAmountForRTL(1000.0, "JPY", context)
        val usdResult = CurrencyFormatter.formatAmountForRTL(100.0, "USD", context)
        
        // Both should contain RTL markers
        assertTrue(jpyResult.contains("\u200F"), "JPY RTL result should contain RTL markers")
        assertTrue(usdResult.contains("\u200F"), "USD RTL result should contain RTL markers")
        
        // JPY should not have decimal places, USD should
        assertTrue(!jpyResult.contains(".00"), "JPY RTL result should not contain .00")
        assertTrue(usdResult.contains(".00"), "USD RTL result should contain decimal places")
    }
}
