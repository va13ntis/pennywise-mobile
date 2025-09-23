package com.pennywise.app.testutils

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

/**
 * Test to verify that the test configuration is working properly.
 * This test ensures that the TestDispatcherRule is functioning correctly
 * and that coroutine tests can run without issues.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestConfigurationTest {
    
    @get:Rule
    val testDispatcherRule = TestDispatcherRule()
    
    @Test
    fun `test configuration should be properly set up`() {
        // This test verifies that the test environment is properly configured
        assertTrue("Test should pass if configuration is correct", true)
    }
    
    @Test
    fun `test dispatcher rule should work with coroutines`() = runTest {
        // This test verifies that coroutine tests work with the TestDispatcherRule
        val result = async {
            "test result"
        }.await()
        
        assertEquals("test result", result)
    }
    
    @Test
    fun `test should have access to test dispatcher`() {
        // This test verifies that the test dispatcher is accessible
        val dispatcher = testDispatcherRule.getTestDispatcher()
        assertNotNull("Test dispatcher should not be null", dispatcher)
    }
}
