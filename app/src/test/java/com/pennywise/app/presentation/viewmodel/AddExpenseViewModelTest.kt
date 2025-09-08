package com.pennywise.app.presentation.viewmodel

import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.validation.CurrencyErrorHandler
import com.pennywise.app.domain.validation.CurrencyValidator
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.screens.ExpenseFormData
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {
    
    @get:org.junit.Rule
    val mainDispatcherRule = MainDispatcherRule()
    
    private lateinit var viewModel: AddExpenseViewModel
    private lateinit var mockTransactionRepository: TransactionRepository
    private lateinit var mockAuthManager: AuthManager
    private lateinit var mockCurrencyValidator: CurrencyValidator
    private lateinit var mockCurrencyErrorHandler: CurrencyErrorHandler
    
    @Before
    fun setUp() {
        mockTransactionRepository = mockk()
        mockAuthManager = mockk()
        mockCurrencyValidator = mockk()
        mockCurrencyErrorHandler = mockk()
        
        // Setup default behaviors
        every { mockAuthManager.currentUser } returns MutableStateFlow(null)
        every { mockCurrencyValidator.getValidCurrencyOrFallback(any()) } returns Currency.USD
        every { mockCurrencyValidator.validateCurrencyCode(any()) } returns com.pennywise.app.domain.validation.ValidationResult.Success
        every { mockCurrencyValidator.validateAmountForCurrency(any(), any()) } returns com.pennywise.app.domain.validation.ValidationResult.Success
        every { mockCurrencyErrorHandler.handleCurrencyFallback(any(), any(), any()) } just Runs
        every { mockCurrencyErrorHandler.handleCurrencyValidationError(any(), any(), any()) } just Runs
        
        viewModel = AddExpenseViewModel(
            mockTransactionRepository,
            mockAuthManager,
            mockCurrencyValidator,
            mockCurrencyErrorHandler
        )
    }
    
    @Test
    fun `updateSelectedCurrency should validate currency before updating`() = runTest {
        // Given
        val validCurrency = Currency.EUR
        every { mockCurrencyValidator.validateCurrencyCode(validCurrency.code) } returns com.pennywise.app.domain.validation.ValidationResult.Success
        
        // When
        viewModel.updateSelectedCurrency(validCurrency)
        
        // Then
        assertEquals("Should update selected currency", validCurrency, viewModel.selectedCurrency.value)
        verify { mockCurrencyValidator.validateCurrencyCode(validCurrency.code) }
        verify(exactly = 0) { mockCurrencyErrorHandler.handleCurrencyValidationError(any(), any(), any()) }
    }
    
    @Test
    fun `updateSelectedCurrency should log error for invalid currency but still update`() = runTest {
        // Given
        val invalidCurrency = Currency.EUR
        every { mockCurrencyValidator.validateCurrencyCode(invalidCurrency.code) } returns com.pennywise.app.domain.validation.ValidationResult.Error("Invalid currency")
        
        // When
        viewModel.updateSelectedCurrency(invalidCurrency)
        
        // Then
        assertEquals("Should still update selected currency", invalidCurrency, viewModel.selectedCurrency.value)
        verify { mockCurrencyValidator.validateCurrencyCode(invalidCurrency.code) }
        verify { mockCurrencyErrorHandler.handleCurrencyValidationError(any(), invalidCurrency.code, "AddExpenseViewModel.updateSelectedCurrency") }
    }
    
    @Test
    fun `saveExpense should validate currency and amount before saving`() = runTest {
        // Given
        val expenseData = ExpenseFormData(
            merchant = "Test Merchant",
            amount = 100.0,
            category = "Food",
            date = Date(),
            isRecurring = false
        )
        val userId = 1L
        val transactionId = 123L
        
        every { mockTransactionRepository.insertTransaction(any()) } returns transactionId
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        verify { mockCurrencyValidator.validateCurrencyCode(any()) }
        verify { mockCurrencyValidator.validateAmountForCurrency(expenseData.amount, any()) }
        verify { mockTransactionRepository.insertTransaction(any()) }
        
        // Wait for state to update
        advanceUntilIdle()
        assertEquals("Should return success state", AddExpenseUiState.Success(transactionId), viewModel.uiState.value)
    }
    
    @Test
    fun `saveExpense should return error for invalid currency`() = runTest {
        // Given
        val expenseData = ExpenseFormData(
            merchant = "Test Merchant",
            amount = 100.0,
            category = "Food",
            date = Date(),
            isRecurring = false
        )
        val userId = 1L
        
        every { mockCurrencyValidator.validateCurrencyCode(any()) } returns com.pennywise.app.domain.validation.ValidationResult.Error("Invalid currency code")
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        verify { mockCurrencyValidator.validateCurrencyCode(any()) }
        verify { mockCurrencyErrorHandler.handleCurrencyValidationError(any(), any(), "AddExpenseViewModel.saveExpense") }
        verify(exactly = 0) { mockTransactionRepository.insertTransaction(any()) }
        
        // Wait for state to update
        advanceUntilIdle()
        assertTrue("Should return error state", viewModel.uiState.value is AddExpenseUiState.Error)
        assertEquals("Should have correct error message", "Invalid currency: Invalid currency code", (viewModel.uiState.value as AddExpenseUiState.Error).message)
    }
    
    @Test
    fun `saveExpense should return error for invalid amount`() = runTest {
        // Given
        val expenseData = ExpenseFormData(
            merchant = "Test Merchant",
            amount = -100.0, // Negative amount
            category = "Food",
            date = Date(),
            isRecurring = false
        )
        val userId = 1L
        
        every { mockCurrencyValidator.validateAmountForCurrency(any(), any()) } returns com.pennywise.app.domain.validation.ValidationResult.Error("Amount cannot be negative")
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        verify { mockCurrencyValidator.validateAmountForCurrency(expenseData.amount, any()) }
        verify { mockCurrencyErrorHandler.handleCurrencyValidationError(any(), null, "AddExpenseViewModel.saveExpense") }
        verify(exactly = 0) { mockTransactionRepository.insertTransaction(any()) }
        
        // Wait for state to update
        advanceUntilIdle()
        assertTrue("Should return error state", viewModel.uiState.value is AddExpenseUiState.Error)
        assertEquals("Should have correct error message", "Invalid amount: Amount cannot be negative", (viewModel.uiState.value as AddExpenseUiState.Error).message)
    }
    
    @Test
    fun `saveExpense should create transaction with correct currency`() = runTest {
        // Given
        val expenseData = ExpenseFormData(
            merchant = "Test Merchant",
            amount = 100.0,
            category = "Food",
            date = Date(),
            isRecurring = false
        )
        val userId = 1L
        val transactionId = 123L
        val expectedCurrency = Currency.EUR
        
        // Set selected currency
        viewModel.updateSelectedCurrency(expectedCurrency)
        
        every { mockTransactionRepository.insertTransaction(any()) } returns transactionId
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        verify {
            mockTransactionRepository.insertTransaction(match {
                it.currency == expectedCurrency.code &&
                it.amount == expenseData.amount &&
                it.description == expenseData.merchant &&
                it.category == expenseData.category &&
                it.type == TransactionType.EXPENSE &&
                it.userId == userId
            })
        }
    }
    
    @Test
    fun `initialization should use currency validator for default currency`() = runTest {
        // Given
        val user = com.pennywise.app.domain.model.User(
            id = 1L,
            username = "testuser",
            passwordHash = "hash",
            defaultCurrency = "INVALID"
        )
        val userFlow = MutableStateFlow(user)
        
        every { mockAuthManager.currentUser } returns userFlow
        every { mockCurrencyValidator.getValidCurrencyOrFallback("INVALID") } returns Currency.USD
        
        // When
        viewModel = AddExpenseViewModel(
            mockTransactionRepository,
            mockAuthManager,
            mockCurrencyValidator,
            mockCurrencyErrorHandler
        )
        
        // Then
        verify { mockCurrencyValidator.getValidCurrencyOrFallback("INVALID") }
        verify { mockCurrencyErrorHandler.handleCurrencyFallback("INVALID", "USD", "AddExpenseViewModel initialization") }
        
        // Wait for initialization
        advanceUntilIdle()
        assertEquals("Should set default currency", Currency.USD, viewModel.selectedCurrency.value)
    }
    
    @Test
    fun `resetState should reset UI state to idle`() = runTest {
        // Given
        val expenseData = ExpenseFormData(
            merchant = "Test Merchant",
            amount = 100.0,
            category = "Food",
            date = Date(),
            isRecurring = false
        )
        val userId = 1L
        val transactionId = 123L
        
        every { mockTransactionRepository.insertTransaction(any()) } returns transactionId
        
        // Set to success state
        viewModel.saveExpense(expenseData, userId)
        advanceUntilIdle()
        assertTrue("Should be in success state", viewModel.uiState.value is AddExpenseUiState.Success)
        
        // When
        viewModel.resetState()
        
        // Then
        assertTrue("Should reset to idle state", viewModel.uiState.value is AddExpenseUiState.Idle)
    }
}

// Test rule for main dispatcher
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule : kotlinx.coroutines.test.TestWatcher() {
    private val testDispatcher = StandardTestDispatcher()
    
    override fun starting(description: org.junit.runner.Description) {
        super.starting(description)
        Dispatchers.setMain(testDispatcher)
    }
    
    override fun finished(description: org.junit.runner.Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
