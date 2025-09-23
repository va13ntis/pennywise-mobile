package com.pennywise.app.presentation.viewmodel

import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.repository.BankCardRepository
import com.pennywise.app.domain.repository.SplitPaymentInstallmentRepository
import com.pennywise.app.domain.usecase.CurrencySortingService
import com.pennywise.app.domain.validation.CurrencyErrorHandler
import com.pennywise.app.domain.validation.CurrencyValidator
import com.pennywise.app.presentation.auth.AuthManager
import com.pennywise.app.presentation.util.SoundManager
import com.pennywise.app.presentation.screens.ExpenseFormData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.pennywise.app.testutils.TestDispatcherRule
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {
    
    @get:Rule
    val testDispatcherRule = TestDispatcherRule()
    
    private lateinit var viewModel: AddExpenseViewModel
    private lateinit var mockTransactionRepository: TransactionRepository
    private lateinit var mockBankCardRepository: BankCardRepository
    private lateinit var mockSplitPaymentInstallmentRepository: SplitPaymentInstallmentRepository
    private lateinit var mockAuthManager: AuthManager
    private lateinit var mockCurrencyValidator: CurrencyValidator
    private lateinit var mockCurrencyErrorHandler: CurrencyErrorHandler
    private lateinit var mockCurrencySortingService: CurrencySortingService
    private lateinit var mockSoundManager: SoundManager
    
    @Before
    fun setUp() {
        mockTransactionRepository = mockk()
        mockBankCardRepository = mockk()
        mockSplitPaymentInstallmentRepository = mockk()
        mockAuthManager = mockk()
        mockCurrencyValidator = mockk()
        mockCurrencyErrorHandler = mockk()
        mockCurrencySortingService = mockk()
        mockSoundManager = mockk()
        
        // Setup default behaviors
        every { mockAuthManager.currentUser } returns MutableStateFlow(null)
        every { mockAuthManager.getCurrentUser() } returns null
        every { mockCurrencyValidator.getValidCurrencyOrFallback(any()) } returns Currency.USD
        every { mockCurrencyValidator.validateCurrencyCode(any()) } returns com.pennywise.app.domain.validation.ValidationResult.Success
        every { mockCurrencyValidator.validateAmountForCurrency(any(), any()) } returns com.pennywise.app.domain.validation.ValidationResult.Success
        every { mockCurrencyErrorHandler.handleCurrencyFallback(any(), any(), any()) } just Runs
        every { mockCurrencyErrorHandler.handleCurrencyValidationError(any(), any(), any()) } just Runs
        every { mockCurrencySortingService.getSortedCurrencies(any()) } returns MutableStateFlow(emptyList())
        every { mockCurrencySortingService.getTopCurrencies(any(), any()) } returns MutableStateFlow(emptyList())
        coEvery { mockCurrencySortingService.trackCurrencyUsage(any(), any()) } just Runs
        every { mockBankCardRepository.getActiveBankCardsByUserId(any()) } returns MutableStateFlow(emptyList())
        every { mockSplitPaymentInstallmentRepository.getInstallmentsByParentTransaction(any()) } returns MutableStateFlow(emptyList())
        every { mockSoundManager.playKachingSound() } just Runs
        coEvery { mockTransactionRepository.insertTransaction(any()) } returns 1L
        
        viewModel = AddExpenseViewModel(
            mockTransactionRepository,
            mockBankCardRepository,
            mockSplitPaymentInstallmentRepository,
            mockAuthManager,
            mockCurrencyValidator,
            mockCurrencyErrorHandler,
            mockCurrencySortingService,
            mockSoundManager
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
            currency = "USD",
            category = "Food",
            isRecurring = false,
            recurringPeriod = null,
            notes = null,
            date = Date(),
            paymentMethod = PaymentMethod.CASH
        )
        val userId = 1L
        val transactionId = 123L
        
        coEvery { mockTransactionRepository.insertTransaction(any()) } returns transactionId
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        verify { mockCurrencyValidator.validateCurrencyCode(any()) }
        verify { mockCurrencyValidator.validateAmountForCurrency(expenseData.amount, any()) }
        coVerify { mockTransactionRepository.insertTransaction(any()) }
        
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
            currency = "USD",
            category = "Food",
            isRecurring = false,
            recurringPeriod = null,
            notes = null,
            date = Date(),
            paymentMethod = PaymentMethod.CASH
        )
        val userId = 1L
        
        every { mockCurrencyValidator.validateCurrencyCode(any()) } returns com.pennywise.app.domain.validation.ValidationResult.Error("Invalid currency code")
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        verify { mockCurrencyValidator.validateCurrencyCode(any()) }
        verify { mockCurrencyErrorHandler.handleCurrencyValidationError(any(), any(), "AddExpenseViewModel.saveExpense") }
        coVerify(exactly = 0) { mockTransactionRepository.insertTransaction(any()) }
        
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
            currency = "USD",
            category = "Food",
            isRecurring = false,
            recurringPeriod = null,
            notes = null,
            date = Date(),
            paymentMethod = PaymentMethod.CASH
        )
        val userId = 1L
        
        every { mockCurrencyValidator.validateAmountForCurrency(any(), any()) } returns com.pennywise.app.domain.validation.ValidationResult.Error("Amount cannot be negative")
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        verify { mockCurrencyValidator.validateAmountForCurrency(expenseData.amount, any()) }
        verify { mockCurrencyErrorHandler.handleCurrencyValidationError(any(), null, "AddExpenseViewModel.saveExpense") }
        coVerify(exactly = 0) { mockTransactionRepository.insertTransaction(any()) }
        
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
            currency = "USD",
            category = "Food",
            isRecurring = false,
            recurringPeriod = null,
            notes = null,
            date = Date(),
            paymentMethod = PaymentMethod.CASH
        )
        val userId = 1L
        val transactionId = 123L
        val expectedCurrency = Currency.EUR
        
        // Set selected currency
        viewModel.updateSelectedCurrency(expectedCurrency)
        
        coEvery { mockTransactionRepository.insertTransaction(any()) } returns transactionId
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        coVerify {
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
            locale = "en",
            deviceAuthEnabled = false,
            defaultCurrency = "INVALID"
        )
        val userFlow = MutableStateFlow(user)
        
        every { mockAuthManager.currentUser } returns userFlow
        every { mockCurrencyValidator.getValidCurrencyOrFallback("INVALID") } returns Currency.USD
        
        // When
        viewModel = AddExpenseViewModel(
            mockTransactionRepository,
            mockBankCardRepository,
            mockSplitPaymentInstallmentRepository,
            mockAuthManager,
            mockCurrencyValidator,
            mockCurrencyErrorHandler,
            mockCurrencySortingService,
            mockSoundManager
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
            currency = "USD",
            category = "Food",
            isRecurring = false,
            recurringPeriod = null,
            notes = null,
            date = Date(),
            paymentMethod = PaymentMethod.CASH
        )
        val userId = 1L
        val transactionId = 123L
        
        coEvery { mockTransactionRepository.insertTransaction(any()) } returns transactionId
        
        // Set to success state
        viewModel.saveExpense(expenseData, userId)
        advanceUntilIdle()
        assertTrue("Should be in success state", viewModel.uiState.value is AddExpenseUiState.Success)
        
        // When
        viewModel.resetState()
        
        // Then
        assertTrue("Should reset to idle state", viewModel.uiState.value is AddExpenseUiState.Idle)
    }
    
    @Test
    fun `saveExpense should create recurring transaction with correct period`() = runTest {
        // Given
        val expenseData = ExpenseFormData(
            merchant = "Netflix",
            amount = 15.99,
            currency = "USD",
            category = "Entertainment",
            isRecurring = true,
            recurringPeriod = RecurringPeriod.MONTHLY,
            notes = "Monthly subscription",
            date = Date(),
            paymentMethod = PaymentMethod.CREDIT_CARD
        )
        val userId = 1L
        val transactionId = 123L
        
        coEvery { mockTransactionRepository.insertTransaction(any()) } returns transactionId
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        coVerify {
            mockTransactionRepository.insertTransaction(match {
                it.isRecurring == true &&
                it.recurringPeriod == RecurringPeriod.MONTHLY &&
                it.description == "Netflix" &&
                it.amount == 15.99 &&
                it.paymentMethod == PaymentMethod.CREDIT_CARD
            })
        }
        
        // Wait for state to update
        advanceUntilIdle()
        assertEquals("Should return success state", AddExpenseUiState.Success(transactionId), viewModel.uiState.value)
    }
    
    @Test
    fun `saveExpense should create transaction with cheque payment method and installments`() = runTest {
        // Given
        val expenseData = ExpenseFormData(
            merchant = "Furniture Store",
            amount = 1200.0,
            currency = "USD",
            category = "Shopping",
            isRecurring = false,
            recurringPeriod = null,
            notes = "Split payment with cheque",
            date = Date(),
            paymentMethod = PaymentMethod.CHEQUE,
            installments = 6,
            installmentAmount = 200.0
        )
        val userId = 1L
        val transactionId = 456L
        
        coEvery { mockTransactionRepository.insertTransaction(any()) } returns transactionId
        
        // When
        viewModel.saveExpense(expenseData, userId)
        
        // Then
        coVerify {
            mockTransactionRepository.insertTransaction(match {
                it.description == "Furniture Store" &&
                it.amount == 1200.0 &&
                it.paymentMethod == PaymentMethod.CHEQUE &&
                it.installments == 6 &&
                it.installmentAmount == 200.0
            })
        }
        
        // Wait for state to update
        advanceUntilIdle()
        assertEquals("Should return success state", AddExpenseUiState.Success(transactionId), viewModel.uiState.value)
    }
}

