package com.pennywise.app.presentation.components

import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.testutils.BaseCurrencyUiTest
import com.pennywise.app.testutils.CurrencyTestFixtures
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for currency symbol updates when currency changes
 * Tests that currency symbols are updated correctly across different components and screens
 */
@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class CurrencySymbolUpdateTest : BaseCurrencyUiTest() {
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to EUR`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.EUR)
        
        // Then
        verifyCurrencySymbol(Currency.EUR)
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from EUR to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        enterAmount("85.75")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("85.75")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to JPY`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.JPY)
        
        // Then
        verifyCurrencySymbol(Currency.JPY)
        // Amount should be reformatted for JPY (no decimals)
        findAmountField().assertTextContains("100")
        findAmountField().assertTextDoesNotContain("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from JPY to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("15000")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("15000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to GBP`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.GBP)
        
        // Then
        verifyCurrencySymbol(Currency.GBP)
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from GBP to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.GBP)
        enterAmount("75.25")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("75.25")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to KRW`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.KRW)
        
        // Then
        verifyCurrencySymbol(Currency.KRW)
        // Amount should be reformatted for KRW (no decimals)
        findAmountField().assertTextContains("100")
        findAmountField().assertTextDoesNotContain("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from KRW to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.KRW)
        enterAmount("50000")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("50000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from EUR to JPY`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        enterAmount("85.75")
        selectCurrency(Currency.JPY)
        
        // Then
        verifyCurrencySymbol(Currency.JPY)
        // Amount should be reformatted for JPY (no decimals)
        findAmountField().assertTextContains("85")
        findAmountField().assertTextDoesNotContain("85.75")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from JPY to EUR`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("15000")
        selectCurrency(Currency.EUR)
        
        // Then
        verifyCurrencySymbol(Currency.EUR)
        findAmountField().assertTextContains("15000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from GBP to JPY`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.GBP)
        enterAmount("75.25")
        selectCurrency(Currency.JPY)
        
        // Then
        verifyCurrencySymbol(Currency.JPY)
        // Amount should be reformatted for JPY (no decimals)
        findAmountField().assertTextContains("75")
        findAmountField().assertTextDoesNotContain("75.25")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from JPY to GBP`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("15000")
        selectCurrency(Currency.GBP)
        
        // Then
        verifyCurrencySymbol(Currency.GBP)
        findAmountField().assertTextContains("15000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to CAD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.CAD)
        
        // Then
        verifyCurrencySymbol(Currency.CAD)
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from CAD to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.CAD)
        enterAmount("125.75")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("125.75")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to AUD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.AUD)
        
        // Then
        verifyCurrencySymbol(Currency.AUD)
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from AUD to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.AUD)
        enterAmount("150.25")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("150.25")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to CHF`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.CHF)
        
        // Then
        verifyCurrencySymbol(Currency.CHF)
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from CHF to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.CHF)
        enterAmount("95.75")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("95.75")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to CNY`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.CNY)
        
        // Then
        verifyCurrencySymbol(Currency.CNY)
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from CNY to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.CNY)
        enterAmount("650.25")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("650.25")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to INR`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.INR)
        
        // Then
        verifyCurrencySymbol(Currency.INR)
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from INR to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.INR)
        enterAmount("7500.75")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("7500.75")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to KRW`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.KRW)
        
        // Then
        verifyCurrencySymbol(Currency.KRW)
        // Amount should be reformatted for KRW (no decimals)
        findAmountField().assertTextContains("100")
        findAmountField().assertTextDoesNotContain("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from KRW to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.KRW)
        enterAmount("50000")
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("50000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from EUR to KRW`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.EUR)
        enterAmount("85.75")
        selectCurrency(Currency.KRW)
        
        // Then
        verifyCurrencySymbol(Currency.KRW)
        // Amount should be reformatted for KRW (no decimals)
        findAmountField().assertTextContains("85")
        findAmountField().assertTextDoesNotContain("85.75")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from KRW to EUR`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.KRW)
        enterAmount("50000")
        selectCurrency(Currency.EUR)
        
        // Then
        verifyCurrencySymbol(Currency.EUR)
        findAmountField().assertTextContains("50000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from GBP to KRW`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.GBP)
        enterAmount("75.25")
        selectCurrency(Currency.KRW)
        
        // Then
        verifyCurrencySymbol(Currency.KRW)
        // Amount should be reformatted for KRW (no decimals)
        findAmountField().assertTextContains("75")
        findAmountField().assertTextDoesNotContain("75.25")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from KRW to GBP`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.KRW)
        enterAmount("50000")
        selectCurrency(Currency.GBP)
        
        // Then
        verifyCurrencySymbol(Currency.GBP)
        findAmountField().assertTextContains("50000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from JPY to KRW`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("15000")
        selectCurrency(Currency.KRW)
        
        // Then
        verifyCurrencySymbol(Currency.KRW)
        findAmountField().assertTextContains("15000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from KRW to JPY`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.KRW)
        enterAmount("50000")
        selectCurrency(Currency.JPY)
        
        // Then
        verifyCurrencySymbol(Currency.JPY)
        findAmountField().assertTextContains("50000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to EUR and back to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.EUR)
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to JPY and back to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.JPY)
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("100")
        findAmountField().assertTextDoesNotContain("100.50")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from JPY to USD and back to JPY`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.JPY)
        enterAmount("15000")
        selectCurrency(Currency.USD)
        selectCurrency(Currency.JPY)
        
        // Then
        verifyCurrencySymbol(Currency.JPY)
        findAmountField().assertTextContains("15000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from KRW to USD and back to KRW`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.KRW)
        enterAmount("50000")
        selectCurrency(Currency.USD)
        selectCurrency(Currency.KRW)
        
        // Then
        verifyCurrencySymbol(Currency.KRW)
        findAmountField().assertTextContains("50000")
    }
    
    @Test
    fun `currency symbol should update in amount field when currency changes from USD to KRW and back to USD`() {
        // Given
        setupComposeContent()
        navigateToAddExpense()
        
        // When
        selectCurrency(Currency.USD)
        enterAmount("100.50")
        selectCurrency(Currency.KRW)
        selectCurrency(Currency.USD)
        
        // Then
        verifyCurrencySymbol(Currency.USD)
        findAmountField().assertTextContains("100")
        findAmountField().assertTextDoesNotContain("100.50")
    }
}
