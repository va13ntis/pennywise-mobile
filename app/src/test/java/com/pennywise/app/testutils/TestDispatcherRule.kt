package com.pennywise.app.testutils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule for managing coroutine test dispatchers.
 * This rule ensures proper setup and cleanup of test dispatchers
 * to prevent test interference and memory leaks.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestDispatcherRule : TestWatcher() {
    
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
    
    /**
     * Get the test dispatcher for use in tests
     */
    fun getTestDispatcher(): TestDispatcher = testDispatcher
}
