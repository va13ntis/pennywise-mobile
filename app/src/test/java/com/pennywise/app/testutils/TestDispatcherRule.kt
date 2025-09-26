package com.pennywise.app.testutils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit 5 extension for managing coroutine test dispatchers.
 * This extension ensures proper setup and cleanup of test dispatchers
 * to prevent test interference and memory leaks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherRule : BeforeEachCallback, AfterEachCallback {
    
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    
    override fun beforeEach(context: ExtensionContext?) {
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun afterEach(context: ExtensionContext?) {
        Dispatchers.resetMain()
    }
    
    /**
     * Get the test dispatcher for use in tests
     */
    fun getTestDispatcher(): TestDispatcher = testDispatcher
}
