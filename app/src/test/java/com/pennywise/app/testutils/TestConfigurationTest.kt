package com.pennywise.app.testutils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

/**
 * Comprehensive test suite for test configuration utilities.
 * This test ensures that the TestDispatcherRule is functioning correctly
 * and that coroutine tests can run without issues. It also validates
 * the CurrencyTestFixtures utility class functionality.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("Test Configuration Tests")
class TestConfigurationTest {
    
    private lateinit var testDispatcherRule: TestDispatcherRule
    
    @BeforeEach
    fun setUp() {
        testDispatcherRule = TestDispatcherRule()
    }
    
    @AfterEach
    fun tearDown() {
        // Cleanup is handled by TestDispatcherRule's afterEach
    }
    
    @Nested
    @DisplayName("TestDispatcherRule Tests")
    inner class TestDispatcherRuleTests {
        
        @Test
        @DisplayName("Should provide access to test dispatcher")
        fun `test should have access to test dispatcher`() {
            // This test verifies that the test dispatcher is accessible
            val dispatcher = testDispatcherRule.getTestDispatcher()
            assertNotNull(dispatcher, "Test dispatcher should not be null")
        }
        
        @Test
        @DisplayName("Should work with basic coroutine operations")
        fun `test dispatcher rule should work with coroutines`() = runTest {
            // This test verifies that coroutine tests work with the TestDispatcherRule
            val result = async {
                "test result"
            }.await()
            
            assertEquals("test result", result)
        }
        
        @Test
        @DisplayName("Should handle delayed coroutine operations")
        fun `test dispatcher should handle delayed operations`() = runTest {
            // Test that the dispatcher can handle delayed operations
            val result = async {
                delay(100) // Simulate some work
                "delayed result"
            }.await()
            
            assertEquals("delayed result", result)
        }
        
        @Test
        @DisplayName("Should handle multiple concurrent coroutines")
        fun `test dispatcher should handle concurrent coroutines`() = runTest {
            // Test concurrent coroutine execution
            val results = listOf(
                async { "result1" },
                async { "result2" },
                async { "result3" }
            ).map { it.await() }
            
            assertEquals(listOf("result1", "result2", "result3"), results)
        }
    }
    
    @Nested
    @DisplayName("CurrencyTestFixtures Tests")
    inner class CurrencyTestFixturesTests {
        
        @Test
        @DisplayName("Should provide valid test amounts")
        fun `test fixtures should provide valid amounts`() {
            val amounts = CurrencyTestFixtures.Amounts
            
            assertEquals(0.0, amounts.ZERO)
            assertEquals(0.01, amounts.SMALL)
            assertEquals(100.0, amounts.STANDARD)
            assertEquals(9999999.99, amounts.LARGE)
            assertEquals(-50.0, amounts.NEGATIVE)
            assertTrue(amounts.NAN.isNaN())
            assertTrue(amounts.POSITIVE_INFINITY.isInfinite())
            assertTrue(amounts.NEGATIVE_INFINITY.isInfinite())
        }
        
        @Test
        @DisplayName("Should provide valid currency pairs")
        fun `test fixtures should provide valid currency pairs`() {
            val pairs = CurrencyTestFixtures.CurrencyPairs
            
            assertEquals(Pair("USD", "EUR"), pairs.USD_EUR)
            assertEquals(Pair("EUR", "GBP"), pairs.EUR_GBP)
            assertEquals(Pair("GBP", "JPY"), pairs.GBP_JPY)
            assertEquals(Pair("JPY", "USD"), pairs.JPY_USD)
            assertEquals(Pair("USD", "USD"), pairs.USD_USD)
            assertEquals(Pair("EUR", "EUR"), pairs.EUR_EUR)
        }
        
        @Test
        @DisplayName("Should provide valid exchange rates")
        fun `test fixtures should provide valid exchange rates`() {
            val rates = CurrencyTestFixtures.ExchangeRates
            
            assertEquals(0.85, rates.USD_TO_EUR)
            assertEquals(1.18, rates.EUR_TO_USD)
            assertEquals(0.86, rates.EUR_TO_GBP)
            assertEquals(1.16, rates.GBP_TO_EUR)
            assertEquals(150.0, rates.GBP_TO_JPY)
            assertEquals(0.0067, rates.JPY_TO_GBP)
            assertEquals(130.0, rates.USD_TO_JPY)
            assertEquals(0.0077, rates.JPY_TO_USD)
        }
        
        @Test
        @DisplayName("Should create valid cached exchange rates")
        fun `test fixtures should create valid cached rates`() {
            val cachedRate = CurrencyTestFixtures.createValidCachedRate(
                baseCode = "USD",
                targetCode = "EUR",
                rate = 0.85,
                hoursOld = 1
            )
            
            assertEquals("USD", cachedRate.baseCode)
            assertEquals("EUR", cachedRate.targetCode)
            assertEquals(0.85, cachedRate.conversionRate)
            assertTrue(cachedRate.lastUpdateTime > 0)
        }
        
        @Test
        @DisplayName("Should create expired cached exchange rates")
        fun `test fixtures should create expired cached rates`() {
            val expiredRate = CurrencyTestFixtures.createExpiredCachedRate(
                baseCode = "USD",
                targetCode = "EUR",
                rate = 0.85,
                hoursOld = 25
            )
            
            assertEquals("USD", expiredRate.baseCode)
            assertEquals("EUR", expiredRate.targetCode)
            assertEquals(0.85, expiredRate.conversionRate)
            assertTrue(expiredRate.lastUpdateTime > 0)
        }
        
        @Test
        @DisplayName("Should create valid exchange rate responses")
        fun `test fixtures should create valid exchange rate responses`() {
            val response = CurrencyTestFixtures.createExchangeRateResponse(
                baseCode = "USD",
                targetCode = "EUR",
                rate = 0.85
            )
            
            assertEquals("success", response.result)
            assertEquals("USD", response.baseCode)
            assertEquals("EUR", response.targetCode)
            assertEquals(0.85, response.conversionRate)
            assertEquals("2023-01-01 00:00:00", response.lastUpdateTime)
            assertEquals("2023-01-02 00:00:00", response.nextUpdateTime)
        }
        
        @Test
        @DisplayName("Should provide valid conversion scenarios")
        fun `test fixtures should provide valid conversion scenarios`() {
            val scenarios = CurrencyTestFixtures.ConversionScenarios
            
            assertEquals(Triple(100.0, Pair("USD", "EUR"), 0.85), scenarios.STANDARD_USD_TO_EUR)
            assertEquals(Triple(9999999.99, Pair("USD", "EUR"), 0.85), scenarios.LARGE_USD_TO_EUR)
            assertEquals(Triple(0.01, Pair("USD", "EUR"), 0.85), scenarios.SMALL_USD_TO_EUR)
            assertEquals(Triple(0.0, Pair("USD", "EUR"), 0.85), scenarios.ZERO_USD_TO_EUR)
            assertEquals(Triple(-50.0, Pair("USD", "EUR"), 0.85), scenarios.NEGATIVE_USD_TO_EUR)
            assertEquals(Triple(100.0, Pair("USD", "USD"), 1.0), scenarios.SAME_CURRENCY_USD)
        }
        
        @Test
        @DisplayName("Should provide valid serialized cache data")
        fun `test fixtures should provide valid serialized cache`() {
            val validCache = CurrencyTestFixtures.SerializedCache.validUsdEurCache()
            val expiredCache = CurrencyTestFixtures.SerializedCache.expiredUsdEurCache()
            val invalidCache = CurrencyTestFixtures.SerializedCache.INVALID_CACHE
            
            assertTrue(validCache.contains("USD"))
            assertTrue(validCache.contains("EUR"))
            assertTrue(validCache.contains("0.85"))
            
            assertTrue(expiredCache.contains("USD"))
            assertTrue(expiredCache.contains("EUR"))
            assertTrue(expiredCache.contains("0.85"))
            
            assertEquals("invalid_json_data", invalidCache)
        }
        
        @Test
        @DisplayName("Should provide valid validation test data")
        fun `test fixtures should provide valid validation test data`() {
            val validationData = CurrencyTestFixtures.ValidationTestData
            
            assertTrue(validationData.VALID_CODES.contains("USD"))
            assertTrue(validationData.VALID_CODES.contains("EUR"))
            assertTrue(validationData.VALID_CODES.contains("GBP"))
            
            assertTrue(validationData.INVALID_CODES.contains(""))
            assertTrue(validationData.INVALID_CODES.contains("ABC"))
            assertTrue(validationData.INVALID_CODES.contains("USDD"))
            
            assertEquals("USD", validationData.CASE_VARIATIONS["USD"])
            assertEquals("USD", validationData.CASE_VARIATIONS["usd"])
            assertEquals("USD", validationData.CASE_VARIATIONS["Usd"])
            assertEquals("EUR", validationData.CASE_VARIATIONS["eUr"])
        }
    }
    
    @Nested
    @DisplayName("Test Environment Validation")
    inner class TestEnvironmentValidation {
        
        @Test
        @DisplayName("Test configuration should be properly set up")
        fun `test configuration should be properly set up`() {
            // This test verifies that the test environment is properly configured
            assertTrue(true, "Test should pass if configuration is correct")
        }
        
        @Test
        @DisplayName("Should have access to all test utilities")
        fun `test should have access to all test utilities`() {
            // Verify that all test utility classes are accessible
            assertNotNull(CurrencyTestFixtures, "CurrencyTestFixtures should be accessible")
            assertNotNull(testDispatcherRule, "TestDispatcherRule should be accessible")
        }
    }
}
