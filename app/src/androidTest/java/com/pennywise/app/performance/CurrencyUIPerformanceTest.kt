package com.pennywise.app.performance

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.domain.model.Currency
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Performance tests for currency UI operations
 * Tests the performance of currency formatting and UI-related operations
 */
@RunWith(AndroidJUnit4::class)
class CurrencyUIPerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    /**
     * Benchmark: Currency formatting performance
     * This tests the performance of formatting currency amounts
     */
    @Test
    fun benchmarkCurrencyFormatting() {
        val amounts = listOf(100.0, 1000.0, 10000.0, 100000.0, 1000000.0)
        val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD")

        benchmarkRule.measureRepeated {
            currencies.forEach { currency ->
                amounts.forEach { amount ->
                    val formatted = formatCurrency(amount, currency)
                    assert(formatted.isNotEmpty())
                }
            }
        }
    }

    /**
     * Benchmark: Currency search performance
     * This tests the performance of searching through currency lists
     */
    @Test
    fun benchmarkCurrencySearch() {
        val allCurrencies = Currency.values().toList()
        val searchTerms = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CHF")

        benchmarkRule.measureRepeated {
            searchTerms.forEach { term ->
                val results = allCurrencies.filter { 
                    it.code.contains(term, ignoreCase = true) ||
                    it.name.contains(term, ignoreCase = true)
                }
                assert(results.isNotEmpty())
            }
        }
    }

    private fun formatCurrency(amount: Double, currency: String): String {
        return when (currency) {
            "USD" -> "$${String.format("%.2f", amount)}"
            "EUR" -> "€${String.format("%.2f", amount)}"
            "GBP" -> "£${String.format("%.2f", amount)}"
            "JPY" -> "¥${String.format("%.0f", amount)}"
            "CAD" -> "C$${String.format("%.2f", amount)}"
            else -> "${currency} ${String.format("%.2f", amount)}"
        }
    }
}
