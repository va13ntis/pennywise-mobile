package com.pennywise.app.presentation.utils

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.usecase.CurrencySortingService
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.first

/**
 * Unit tests for CurrencyUiUtils
 * Tests lifecycle-aware currency collection and UI helper functions
 */
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(MockKExtension::class)
@DisplayName("Currency UI Utils Tests")
class CurrencyUiUtilsTest {

    private lateinit var currencyUiUtils: CurrencyUiUtils
    private lateinit var mockCurrencySortingService: CurrencySortingService
    private lateinit var mockLifecycleOwner: LifecycleOwner
    private lateinit var lifecycleRegistry: LifecycleRegistry
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        
        mockCurrencySortingService = mockk<CurrencySortingService>()
        mockLifecycleOwner = mockk<LifecycleOwner>()
        lifecycleRegistry = LifecycleRegistry(mockLifecycleOwner)
        
        every { mockLifecycleOwner.lifecycle } returns lifecycleRegistry
        every { mockLifecycleOwner.lifecycleScope } returns mockk(relaxed = true)
        
        currencyUiUtils = CurrencyUiUtils()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    @Nested
    @DisplayName("CurrencyUiUtils Class Tests")
    inner class CurrencyUiUtilsClassTests {

        @Test
        @DisplayName("collectSortedCurrencies should return StateFlow of sorted currencies")
        fun `collectSortedCurrencies should return StateFlow of sorted currencies`() = runTest {
            // Given
            val userId = 1L
            val currencies = listOf(Currency.USD, Currency.EUR, Currency.GBP)
            
            every { mockCurrencySortingService.getSortedCurrencies(userId) } returns flowOf(currencies)
            
            // When
            val result = currencyUiUtils.collectSortedCurrencies(
                mockCurrencySortingService,
                userId,
                mockLifecycleOwner
            )
            
            // Then
            // Initial value should be empty list
            assertEquals(emptyList<Currency>(), result.value)
            
            // Verify the correct method was called
            verify { mockCurrencySortingService.getSortedCurrencies(userId) }
            verify { mockLifecycleOwner.lifecycleScope }
        }

        @Test
        @DisplayName("collectTopCurrencies should return StateFlow of top currencies")
        fun `collectTopCurrencies should return StateFlow of top currencies`() = runTest {
            // Given
            val userId = 1L
            val limit = 5
            val currencies = listOf(Currency.USD, Currency.EUR)
            
            every { mockCurrencySortingService.getTopCurrencies(userId, limit) } returns flowOf(currencies)
            
            // When
            val result = currencyUiUtils.collectTopCurrencies(
                mockCurrencySortingService,
                userId,
                limit,
                mockLifecycleOwner
            )
            
            // Then
            // Initial value should be empty list
            assertEquals(emptyList<Currency>(), result.value)
            
            // Verify the correct method was called
            verify { mockCurrencySortingService.getTopCurrencies(userId, limit) }
            verify { mockLifecycleOwner.lifecycleScope }
        }

        @Test
        @DisplayName("collectUsedCurrencies should return StateFlow of used currencies")
        fun `collectUsedCurrencies should return StateFlow of used currencies`() = runTest {
            // Given
            val userId = 1L
            val currencies = listOf(Currency.USD, Currency.EUR)
            
            every { mockCurrencySortingService.getUsedCurrencies(userId) } returns flowOf(currencies)
            
            // When
            val result = currencyUiUtils.collectUsedCurrencies(
                mockCurrencySortingService,
                userId,
                mockLifecycleOwner
            )
            
            // Then
            // Initial value should be empty list
            assertEquals(emptyList<Currency>(), result.value)
            
            // Verify the correct method was called
            verify { mockCurrencySortingService.getUsedCurrencies(userId) }
            verify { mockLifecycleOwner.lifecycleScope }
        }

        @Test
        @DisplayName("collectSortedCurrenciesReactive should return StateFlow of reactive sorted currencies")
        fun `collectSortedCurrenciesReactive should return StateFlow of reactive sorted currencies`() = runTest {
            // Given
            val userId = 1L
            val currencies = listOf(Currency.USD, Currency.EUR, Currency.GBP)
            
            every { mockCurrencySortingService.getSortedCurrenciesReactive(userId) } returns flowOf(currencies)
            
            // When
            val result = currencyUiUtils.collectSortedCurrenciesReactive(
                mockCurrencySortingService,
                userId,
                mockLifecycleOwner
            )
            
            // Then
            // Initial value should be empty list
            assertEquals(emptyList<Currency>(), result.value)
            
            // Verify the correct method was called
            verify { mockCurrencySortingService.getSortedCurrenciesReactive(userId) }
            verify { mockLifecycleOwner.lifecycleScope }
        }
    }

    @Nested
    @DisplayName("CurrencyViewModelExtensions Tests")
    inner class CurrencyViewModelExtensionsTests {

        @Test
        @DisplayName("createSortedCurrenciesStateFlow should return Flow of sorted currencies")
        fun `createSortedCurrenciesStateFlow should return Flow of sorted currencies`() = runTest {
            // Given
            val userId = 1L
            val currencies = listOf(Currency.USD, Currency.EUR, Currency.GBP)
            
            every { mockCurrencySortingService.getSortedCurrencies(userId) } returns flowOf(currencies)
            
            // When
            val result = CurrencyViewModelExtensions.createSortedCurrenciesStateFlow(
                mockCurrencySortingService,
                userId
            )
            
            // Then
            assertEquals(currencies, result.first())
            
            // Verify the correct method was called
            verify { mockCurrencySortingService.getSortedCurrencies(userId) }
        }

        @Test
        @DisplayName("createTopCurrenciesStateFlow should return Flow of top currencies")
        fun `createTopCurrenciesStateFlow should return Flow of top currencies`() = runTest {
            // Given
            val userId = 1L
            val limit = 5
            val currencies = listOf(Currency.USD, Currency.EUR)
            
            every { mockCurrencySortingService.getTopCurrencies(userId, limit) } returns flowOf(currencies)
            
            // When
            val result = CurrencyViewModelExtensions.createTopCurrenciesStateFlow(
                mockCurrencySortingService,
                userId,
                limit
            )
            
            // Then
            assertEquals(currencies, result.first())
            
            // Verify the correct method was called
            verify { mockCurrencySortingService.getTopCurrencies(userId, limit) }
        }

        @Test
        @DisplayName("createUsedCurrenciesStateFlow should return Flow of used currencies")
        fun `createUsedCurrenciesStateFlow should return Flow of used currencies`() = runTest {
            // Given
            val userId = 1L
            val currencies = listOf(Currency.USD, Currency.EUR)
            
            every { mockCurrencySortingService.getUsedCurrencies(userId) } returns flowOf(currencies)
            
            // When
            val result = CurrencyViewModelExtensions.createUsedCurrenciesStateFlow(
                mockCurrencySortingService,
                userId
            )
            
            // Then
            assertEquals(currencies, result.first())
            
            // Verify the correct method was called
            verify { mockCurrencySortingService.getUsedCurrencies(userId) }
        }

        @Test
        @DisplayName("createSortedCurrenciesReactiveStateFlow should return Flow of reactive sorted currencies")
        fun `createSortedCurrenciesReactiveStateFlow should return Flow of reactive sorted currencies`() = runTest {
            // Given
            val userId = 1L
            val currencies = listOf(Currency.USD, Currency.EUR, Currency.GBP)
            
            every { mockCurrencySortingService.getSortedCurrenciesReactive(userId) } returns flowOf(currencies)
            
            // When
            val result = CurrencyViewModelExtensions.createSortedCurrenciesReactiveStateFlow(
                mockCurrencySortingService,
                userId
            )
            
            // Then
            assertEquals(currencies, result.first())
            
            // Verify the correct method was called
            verify { mockCurrencySortingService.getSortedCurrenciesReactive(userId) }
        }
    }

    @Nested
    @DisplayName("CurrencyListHelpers Tests")
    inner class CurrencyListHelpersTests {

        @Test
        @DisplayName("filterCurrenciesByQuery should filter currencies by query")
        fun `filterCurrenciesByQuery should filter currencies by query`() {
            // Given
            val currencies = listOf(Currency.USD, Currency.EUR, Currency.GBP, Currency.JPY)
            
            // When - Search by code
            val resultByCode = CurrencyListHelpers.filterCurrenciesByQuery(currencies, "US")
            
            // Then
            assertEquals(1, resultByCode.size)
            assertEquals(Currency.USD, resultByCode[0])
            
            // When - Search by name (case insensitive)
            val resultByName = CurrencyListHelpers.filterCurrenciesByQuery(currencies, "euro")
            
            // Then
            assertEquals(1, resultByName.size)
            assertEquals(Currency.EUR, resultByName[0])
            
            // When - Empty query
            val resultEmptyQuery = CurrencyListHelpers.filterCurrenciesByQuery(currencies, "")
            
            // Then
            assertEquals(currencies, resultEmptyQuery)
        }

        @Test
        @DisplayName("groupCurrenciesByUsage should group currencies by usage")
        fun `groupCurrenciesByUsage should group currencies by usage`() {
            // Given
            val sortedCurrencies = listOf(Currency.USD, Currency.EUR, Currency.GBP, Currency.JPY)
            val usedCurrencies = listOf(Currency.EUR, Currency.JPY)
            
            // When
            val result = CurrencyListHelpers.groupCurrenciesByUsage(sortedCurrencies, usedCurrencies)
            
            // Then
            assertEquals(2, result.first.size) // Used currencies
            assertEquals(2, result.second.size) // Unused currencies
            
            assertTrue(result.first.contains(Currency.EUR))
            assertTrue(result.first.contains(Currency.JPY))
            
            assertTrue(result.second.contains(Currency.USD))
            assertTrue(result.second.contains(Currency.GBP))
        }

        @Test
        @DisplayName("formatCurrenciesForDropdown should format currencies for dropdown")
        fun `formatCurrenciesForDropdown should format currencies for dropdown`() {
            // Given
            val currencies = listOf(Currency.USD, Currency.EUR)
            
            // When - With symbol
            val resultWithSymbol = CurrencyListHelpers.formatCurrenciesForDropdown(currencies, true)
            
            // Then
            assertEquals(2, resultWithSymbol.size)
            assertEquals("$ USD - US Dollar", resultWithSymbol[0])
            assertEquals("€ EUR - Euro", resultWithSymbol[1])
            
            // When - Without symbol
            val resultWithoutSymbol = CurrencyListHelpers.formatCurrenciesForDropdown(currencies, false)
            
            // Then
            assertEquals(2, resultWithoutSymbol.size)
            assertEquals("USD - US Dollar", resultWithoutSymbol[0])
            assertEquals("EUR - Euro", resultWithoutSymbol[1])
        }

        @Test
        @DisplayName("formatCurrenciesForCompactDisplay should format currencies for compact display")
        fun `formatCurrenciesForCompactDisplay should format currencies for compact display`() {
            // Given
            val currencies = listOf(Currency.USD, Currency.EUR)
            
            // When
            val result = CurrencyListHelpers.formatCurrenciesForCompactDisplay(currencies)
            
            // Then
            assertEquals(2, result.size)
            assertEquals("$ USD", result[0])
            assertEquals("€ EUR", result[1])
        }

        @Test
        @DisplayName("findCurrencyByCode should find currency by code")
        fun `findCurrencyByCode should find currency by code`() {
            // Given
            val currencies = listOf(Currency.USD, Currency.EUR, Currency.GBP)
            
            // When - Exact match
            val resultExact = CurrencyListHelpers.findCurrencyByCode(currencies, "USD")
            
            // Then
            assertEquals(Currency.USD, resultExact)
            
            // When - Case insensitive match
            val resultCaseInsensitive = CurrencyListHelpers.findCurrencyByCode(currencies, "eur")
            
            // Then
            assertEquals(Currency.EUR, resultCaseInsensitive)
            
            // When - No match
            val resultNoMatch = CurrencyListHelpers.findCurrencyByCode(currencies, "XYZ")
            
            // Then
            assertNull(resultNoMatch)
        }

        @Test
        @DisplayName("getPopularCurrencies should return popular currencies")
        fun `getPopularCurrencies should return popular currencies`() {
            // Given
            val currencies = listOf(
                Currency.USD, // popularity 1
                Currency.EUR, // popularity 2
                Currency.GBP, // popularity 3
                Currency.JPY, // popularity 4
                Currency.CAD  // popularity 5
            )
            
            // When - Default limit
            val resultDefaultLimit = CurrencyListHelpers.getPopularCurrencies(currencies)
            
            // Then
            assertEquals(5, resultDefaultLimit.size)
            assertEquals(Currency.USD, resultDefaultLimit[0]) // Most popular
            
            // When - Custom limit
            val resultCustomLimit = CurrencyListHelpers.getPopularCurrencies(currencies, 3)
            
            // Then
            assertEquals(3, resultCustomLimit.size)
            assertEquals(Currency.USD, resultCustomLimit[0]) // Most popular
            assertEquals(Currency.EUR, resultCustomLimit[1])
            assertEquals(Currency.GBP, resultCustomLimit[2])
        }
    }
}

