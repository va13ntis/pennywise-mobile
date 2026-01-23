package com.pennywise.app.data.api

import com.google.gson.Gson
import com.pennywise.app.data.model.ExchangeRateResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.HttpURLConnection

/**
 * Unit tests for CurrencyApi
 * Tests API request formatting and response parsing
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("Currency API Tests")
class CurrencyApiTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var currencyApi: CurrencyApi
    private lateinit var gson: Gson

    @BeforeEach
    fun setUp() {
        gson = Gson()
        mockWebServer = MockWebServer()
        mockWebServer.start()

        currencyApi = Retrofit.Builder()
            .baseUrl(mockWebServer.url("/"))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(CurrencyApi::class.java)
    }

    @AfterEach
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Nested
    @DisplayName("getExchangeRate Method")
    inner class GetExchangeRateMethod {

        @Test
        @DisplayName("Should format request correctly and parse response")
        fun `should format request correctly and parse response`() = runTest {
            // Given
            val baseCode = "USD"
            val targetCode = "EUR"
            val conversionRate = 0.85
            
            val mockResponse = MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(
                    """
                    {
                        "result": "success",
                        "base_code": "USD",
                        "target_code": "EUR",
                        "conversion_rate": $conversionRate,
                        "time_last_update_utc": "Mon, 01 Jan 2023 00:00:00 +0000",
                        "time_next_update_utc": "Tue, 02 Jan 2023 00:00:00 +0000"
                    }
                    """.trimIndent()
                )
            
            mockWebServer.enqueue(mockResponse)
            
            // When
            val response = currencyApi.getExchangeRate(baseCode, targetCode)
            
            // Then
            val request = mockWebServer.takeRequest()
            
            // Verify request format
            assertTrue(request.path!!.contains("/v6/latest/$baseCode"))
            assertTrue(request.path!!.contains("symbols=$targetCode"))
            
            // Verify response parsing
            assertEquals("success", response.result)
            assertEquals(baseCode, response.baseCode)
            assertEquals(targetCode, response.targetCode)
            assertEquals(conversionRate, response.conversionRate)
            assertNotNull(response.lastUpdateTime)
            assertNotNull(response.nextUpdateTime)
        }

        @Test
        @DisplayName("Should handle error response")
        fun `should handle error response`() = runTest {
            // Given
            val baseCode = "USD"
            val targetCode = "INVALID"
            
            val mockResponse = MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .setBody(
                    """
                    {
                        "result": "error",
                        "error-type": "unsupported-code",
                        "error-info": "The target currency code is invalid or unsupported."
                    }
                    """.trimIndent()
                )
            
            mockWebServer.enqueue(mockResponse)
            
            // When & Then
            try {
                currencyApi.getExchangeRate(baseCode, targetCode)
                fail("Should have thrown an exception")
            } catch (e: Exception) {
                // Expected exception
                assertTrue(e.message?.contains("HTTP 400") ?: false)
            }
        }
    }

    @Nested
    @DisplayName("getExchangeRates Method")
    inner class GetExchangeRatesMethod {

        @Test
        @DisplayName("Should format request correctly for multiple currencies")
        fun `should format request correctly for multiple currencies`() = runTest {
            // Given
            val baseCode = "USD"
            val targetCodes = "EUR,GBP,JPY"
            
            val mockResponse = MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(
                    """
                    {
                        "result": "success",
                        "base_code": "USD",
                        "target_code": "EUR,GBP,JPY",
                        "conversion_rate": 0.85,
                        "time_last_update_utc": "Mon, 01 Jan 2023 00:00:00 +0000",
                        "time_next_update_utc": "Tue, 02 Jan 2023 00:00:00 +0000"
                    }
                    """.trimIndent()
                )
            
            mockWebServer.enqueue(mockResponse)
            
            // When
            val response = currencyApi.getExchangeRates(baseCode, targetCodes)
            
            // Then
            val request = mockWebServer.takeRequest()
            
            // Verify request format
            assertTrue(request.path!!.contains("/v6/latest/$baseCode"))
            // Check for symbols parameter - it might be URL encoded
            assertTrue(request.path!!.contains("symbols=") || request.path!!.contains("symbols%3D"))
            
            // Verify response parsing
            assertEquals("success", response.result)
            assertEquals(baseCode, response.baseCode)
            assertEquals(targetCodes, response.targetCode)
        }

        @Test
        @DisplayName("Should handle empty target codes")
        fun `should handle empty target codes`() = runTest {
            // Given
            val baseCode = "USD"
            val targetCodes = ""
            
            val mockResponse = MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(
                    """
                    {
                        "result": "success",
                        "base_code": "USD",
                        "target_code": "",
                        "conversion_rate": 1.0,
                        "time_last_update_utc": "Mon, 01 Jan 2023 00:00:00 +0000",
                        "time_next_update_utc": "Tue, 02 Jan 2023 00:00:00 +0000"
                    }
                    """.trimIndent()
                )
            
            mockWebServer.enqueue(mockResponse)
            
            // When
            val response = currencyApi.getExchangeRates(baseCode, targetCodes)
            
            // Then
            val request = mockWebServer.takeRequest()
            
            // Verify request format
            assertTrue(request.path!!.contains("/v6/latest"))
            assertTrue(request.path!!.contains("base=$baseCode"))
            assertTrue(request.path!!.contains("symbols="))
            
            // Verify response parsing
            assertEquals("success", response.result)
            assertEquals(baseCode, response.baseCode)
            assertEquals("", response.targetCode)
        }
    }

    @Nested
    @DisplayName("Response Parsing")
    inner class ResponseParsing {

        @Test
        @DisplayName("Should parse ExchangeRateResponse correctly")
        fun `should parse ExchangeRateResponse correctly`() {
            // Given
            val json = """
            {
                "result": "success",
                "base_code": "USD",
                "target_code": "EUR",
                "conversion_rate": 0.85,
                "time_last_update_utc": "Mon, 01 Jan 2023 00:00:00 +0000",
                "time_next_update_utc": "Tue, 02 Jan 2023 00:00:00 +0000"
            }
            """.trimIndent()
            
            // When
            val response = gson.fromJson(json, ExchangeRateResponse::class.java)
            
            // Then
            assertEquals("success", response.result)
            assertEquals("USD", response.baseCode)
            assertEquals("EUR", response.targetCode)
            assertEquals(0.85, response.conversionRate)
            assertEquals("Mon, 01 Jan 2023 00:00:00 +0000", response.lastUpdateTime)
            assertEquals("Tue, 02 Jan 2023 00:00:00 +0000", response.nextUpdateTime)
        }

        @Test
        @DisplayName("Should handle missing fields in response")
        fun `should handle missing fields in response`() {
            // Given
            val json = """
            {
                "result": "success",
                "base_code": "USD",
                "target_code": "EUR",
                "conversion_rate": 0.85
            }
            """.trimIndent()
            
            // When
            val response = gson.fromJson(json, ExchangeRateResponse::class.java)
            
            // Then
            assertEquals("success", response.result)
            assertEquals("USD", response.baseCode)
            assertEquals("EUR", response.targetCode)
            assertEquals(0.85, response.conversionRate)
            assertNull(response.lastUpdateTime)
            assertNull(response.nextUpdateTime)
        }
    }
}

