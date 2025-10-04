package com.pennywise.app.presentation.screens

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add

import androidx.compose.material3.*
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.BankCard
import com.pennywise.app.presentation.components.CurrencySelectionDropdown
import com.pennywise.app.presentation.components.CurrencyAdapter
import com.pennywise.app.presentation.components.CompactCircularCurrencyButton
import com.pennywise.app.presentation.viewmodel.AddExpenseUiState
import com.pennywise.app.presentation.viewmodel.AddExpenseViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import com.pennywise.app.presentation.util.LocaleFormatter
import java.text.SimpleDateFormat
import java.text.DateFormat
import java.util.*

/**
 * Helper function to validate and format amount input based on currency
 */
fun validateAndFormatAmount(
    input: String,
    currency: Currency?
): String {
    if (currency == null) return input
    
    // Only allow digits and decimal point
    val filtered = input.filter { char -> char.isDigit() || char == '.' }
    
    return when (currency.decimalPlaces) {
        0 -> {
            // For currencies with no decimal places (JPY, KRW), only allow digits
            filtered.filter { char -> char.isDigit() }
        }
        else -> {
            // For currencies with decimal places, limit to the specified number
            val parts = filtered.split(".")
            if (parts.size > 1) {
                parts[0] + "." + parts[1].take(currency.decimalPlaces)
            } else {
                filtered
            }
        }
    }
}

/**
 * Visual transformation to format amount with currency symbol
 */
class CurrencyAmountTransformation(
    private val currency: Currency
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        val formattedText = if (originalText.isNotEmpty()) {
            "${currency.symbol}$originalText"
        } else {
            currency.symbol
        }
        
        return TransformedText(
            AnnotatedString(formattedText),
            OffsetMapping.Identity
        )
    }
}

/**
 * Update amount field formatting based on selected currency
 */
fun updateAmountFieldForCurrency(
    currentAmount: String,
    selectedCurrency: Currency?
): String {
    if (selectedCurrency == null) return currentAmount
    
    return when (selectedCurrency.decimalPlaces) {
        0 -> {
            // For currencies with no decimal places (JPY, KRW), remove decimal point and everything after
            currentAmount.split(".")[0]
        }
        else -> {
            // For currencies with decimal places, limit to the specified number
            val parts = currentAmount.split(".")
            if (parts.size > 1) {
                parts[0] + "." + parts[1].take(selectedCurrency.decimalPlaces)
            } else {
                currentAmount
            }
        }
    }
}

/**
 * Modern card container for form sections
 */
@Composable
fun FormSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

/**
 * Modern pill-shaped toggle button with enhanced visual feedback
 */
@Composable
fun PillToggleButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation for selection state
    val elevation by animateFloatAsState(
        targetValue = if (isSelected) 8f else 2f,
        animationSpec = spring(dampingRatio = 0.6f),
        label = "elevation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "scale"
    )

    Card(
        modifier = modifier
            .selectable(
                selected = isSelected,
                onClick = onClick
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (!isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Modern input field with icon and error handling
 */
@Composable
fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isError: Boolean = false,
    errorMessage: String? = null,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            leadingIcon = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            },
            isError = isError,
            supportingText = {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (supportingText != null) {
                    Text(
                        text = supportingText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
    }
}

/**
 * Get localized payment method name
 */
@Composable
private fun getLocalizedPaymentMethodName(paymentMethod: com.pennywise.app.domain.model.PaymentMethod, context: android.content.Context): String {
    return when (paymentMethod) {
        PaymentMethod.CASH -> context.getString(R.string.payment_method_cash)
        PaymentMethod.CHEQUE -> context.getString(R.string.payment_method_cheque)
        PaymentMethod.CREDIT_CARD -> context.getString(R.string.payment_method_credit_card)
    }
}

/**
 * Modern redesigned Add Expense Screen with card-based layout and improved UX
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    // Get the current user from the ViewModel (for display purposes only)
    val currentUser by viewModel.currentUser.collectAsState()
    
    // Debug logging for current user
    LaunchedEffect(currentUser) {
        Log.d("AddExpenseScreen", "Current user: $currentUser")
    }
    
    // Form state management using remember and mutableStateOf
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var isRecurring by remember { mutableStateOf(false) }
    var selectedRecurringPeriod by remember { mutableStateOf(RecurringPeriod.MONTHLY) }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(Date()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var installments by remember { mutableStateOf(1) }
    var showInstallmentOptions by remember { mutableStateOf(false) }
    var selectedBankCardId by remember { mutableStateOf<Long?>(null) }
    
    // Validation states
    var merchantError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    
    // Form validation state
    var isFormValid by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    
    val uiState by viewModel.uiState.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val bankCards by viewModel.bankCards.collectAsState()
    
    // Pre-fetch string resources to avoid calling them from non-@Composable contexts
    val merchantRequiredText = stringResource(R.string.merchant_required)
    val amountRequiredText = stringResource(R.string.amount_required)
    val invalidAmountText = stringResource(R.string.invalid_amount)
    val categoryRequiredText = stringResource(R.string.category_required)
    
    // Currency dropdown string resources
    val currencyNoDecimalPlaces = stringResource(R.string.currency_no_decimal_places)

    // Currency-specific validation messages
    val currencyValidationMessages = remember {
        mapOf(
            "JPY" to currencyNoDecimalPlaces,
            "KRW" to currencyNoDecimalPlaces
        )
    }
    
    // Date formatter - use LocaleFormatter for consistent region detection
    val context = LocalContext.current
    
    // Category options - pre-fetch string resources
    val categoryFood = stringResource(R.string.category_food)
    val categoryTransport = stringResource(R.string.category_transport)
    val categoryShopping = stringResource(R.string.category_shopping)
    val categoryEntertainment = stringResource(R.string.category_entertainment)
    val categoryUtilities = stringResource(R.string.category_utilities)
    val categoryHealth = stringResource(R.string.category_health)
    val categoryOther = stringResource(R.string.category_other)
    
    val categories = remember {
        listOf(
            categoryFood,
            categoryTransport,
            categoryShopping,
            categoryEntertainment,
            categoryUtilities,
            categoryHealth,
            categoryOther
        )
    }
    
    // Set default category
    LaunchedEffect(categoryFood) {
        if (category.isEmpty()) {
            category = categoryFood
        }
    }
    
    // Validation function
    val validateForm = {
        merchantError = if (merchant.isBlank()) merchantRequiredText else null
        
        // Enhanced amount validation with currency-specific rules
        amountError = when {
            amount.isBlank() -> amountRequiredText
            amount.toDoubleOrNull() == null -> invalidAmountText
            amount.toDoubleOrNull()!! <= 0 -> invalidAmountText
            selectedCurrency?.let { currency ->
                when (currency.decimalPlaces) {
                    0 -> amount.contains(".")
                    else -> false
                }
            } == true -> currencyValidationMessages[selectedCurrency?.code] ?: currencyNoDecimalPlaces
            else -> null
        }
        
        categoryError = if (category.isBlank()) categoryRequiredText else null
        
        // Form is valid only if all fields are valid AND currency is selected
        val wasValid = isFormValid
        isFormValid = merchantError == null && amountError == null && categoryError == null && 
                     selectedCurrency != null
        
        Log.d("AddExpenseScreen", "Validation: merchant='$merchant' (error: $merchantError), amount='$amount' (error: $amountError), category='$category' (error: $categoryError), currency=${selectedCurrency?.code}, valid=$isFormValid (was: $wasValid)")
    }
    
    // Handle currency changes and update amount field formatting
    LaunchedEffect(selectedCurrency) {
        if (amount.isNotEmpty()) {
            // Update amount field formatting when currency changes
            val formattedAmount = validateAndFormatAmount(amount, selectedCurrency)
            if (formattedAmount != amount) {
                amount = formattedAmount
            }
        }
    }
    
    // Reset bank card selection when payment method changes
    LaunchedEffect(selectedPaymentMethod) {
        if (selectedPaymentMethod != PaymentMethod.CREDIT_CARD) {
            selectedBankCardId = null
        }
    }
    
    // Validate form whenever any field changes
    LaunchedEffect(merchant, amount, category, selectedCurrency, selectedPaymentMethod) {
        validateForm()
    }
    
    // Handle UI state changes from ViewModel
    LaunchedEffect(uiState) {
        Log.d("AddExpenseScreen", "UI State changed: $uiState")
        when (uiState) {
            is AddExpenseUiState.Success -> {
                Log.d("AddExpenseScreen", "Save successful, navigating back")
                onNavigateBack()
            }
            is AddExpenseUiState.Error -> {
                Log.e("AddExpenseScreen", "Save failed: ${(uiState as AddExpenseUiState.Error).message}")
                // In a real app, you'd show a snackbar or dialog here
                // For now, we'll just reset the state
                viewModel.resetState()
            }
            is AddExpenseUiState.Loading -> {
                Log.d("AddExpenseScreen", "Save in progress...")
            }
            else -> {
                Log.d("AddExpenseScreen", "UI State: $uiState")
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_expense)) }
            )
        },
        bottomBar = {
            // Sticky Save and Cancel Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel Button
                OutlinedButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                // Save Button
                Button(
                    onClick = {
                        Log.d("AddExpenseScreen", "=== SAVE BUTTON CLICKED ===")
                        Log.d("AddExpenseScreen", "Save button clicked")
                        Log.d("AddExpenseScreen", "Form valid: $isFormValid, Currency: ${selectedCurrency?.code}")
                        Log.d("AddExpenseScreen", "Merchant: '$merchant', Amount: '$amount', Category: '$category'")
                        
                        // Final validation before submission
                        validateForm()
                        
                        Log.d("AddExpenseScreen", "After validation - Form valid: $isFormValid")
                        
                        // Only proceed if form is valid and currency is selected
                        if (isFormValid && selectedCurrency != null) {
                            Log.d("AddExpenseScreen", "Proceeding with save...")
                            val totalAmount = amount.toDouble()
                            val installmentAmount = if ((selectedPaymentMethod == PaymentMethod.CREDIT_CARD || selectedPaymentMethod == PaymentMethod.CHEQUE) && installments > 1) {
                                viewModel.calculateInstallmentAmount(totalAmount, installments)
                            } else null
                            
                            val expenseData = ExpenseFormData(
                                merchant = merchant,
                                amount = totalAmount,
                                currency = selectedCurrency!!.code,
                                category = category,
                                isRecurring = isRecurring,
                                recurringPeriod = if (isRecurring) selectedRecurringPeriod else null,
                                notes = notes.ifBlank { null },
                                date = selectedDate,
                                paymentMethod = selectedPaymentMethod,
                                installments = if ((selectedPaymentMethod == PaymentMethod.CREDIT_CARD || selectedPaymentMethod == PaymentMethod.CHEQUE) && installments > 1) installments else null,
                                installmentAmount = installmentAmount,
                                selectedBankCardId = if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD) selectedBankCardId else null
                            )
                            Log.d("AddExpenseScreen", "Calling viewModel.saveExpense with data: $expenseData")
                            viewModel.saveExpense(expenseData)
                        } else {
                            Log.d("AddExpenseScreen", "Form validation failed - not saving")
                        }
                    },
                    enabled = run {
                        val enabled = isFormValid && selectedCurrency != null && uiState !is AddExpenseUiState.Loading
                        Log.d("AddExpenseScreen", "Save button enabled: $enabled (formValid: $isFormValid, currency: ${selectedCurrency?.code}, uiState: $uiState)")
                        enabled
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (uiState is AddExpenseUiState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.save),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // Date Picker - Custom clickable field
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true }
            ) {
                // Label
                Text(
                    text = stringResource(R.string.select_date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Custom field that looks like OutlinedTextField
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = stringResource(R.string.content_desc_calendar),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = try {
                                // Try LocaleFormatter first
                                LocaleFormatter.formatTransactionDate(selectedDate, context)
                            } catch (e: Exception) {
                                // Fallback to system default date format
                                try {
                                    DateFormat.getDateInstance(DateFormat.SHORT).format(selectedDate)
                                } catch (e2: Exception) {
                                    // Final fallback to simple format
                                    SimpleDateFormat("MM/dd/yyyy", Locale.US).format(selectedDate)
                                }
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Merchant field
            OutlinedTextField(
                value = merchant,
                onValueChange = { 
                    merchant = it 
                    merchantError = if (it.isBlank()) merchantRequiredText else null
                },
                label = { Text(stringResource(R.string.merchant)) },
                isError = merchantError != null,
                supportingText = { merchantError?.let { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    capitalization = KeyboardCapitalization.Words
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
            
            // Currency selection dialog state (declared early to avoid reference issues)
            var currencyExpanded by remember { mutableStateOf(false) }
            val currencyAdapter = remember { CurrencyAdapter() }
            val currencies = remember { currencyAdapter.getSortedCurrencies() }
            
            // Amount field with inline currency selection
            Column {
                // Row containing amount field and currency button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically // Align centers for perfect alignment
                ) {
                    // Amount field (takes most of the space)
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            // Apply currency-specific validation and formatting
                            val formattedAmount = validateAndFormatAmount(newValue, selectedCurrency)
                            
                            amount = formattedAmount
                            amountError = when {
                                formattedAmount.isBlank() -> amountRequiredText
                                formattedAmount.toDoubleOrNull() == null -> invalidAmountText
                                formattedAmount.toDoubleOrNull()!! <= 0 -> invalidAmountText
                                else -> null
                            }
                        },
                        label = { Text(stringResource(R.string.amount)) },
                        isError = amountError != null,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        singleLine = true,
                        textStyle = TextStyle(
                            textDirection = TextDirection.Ltr
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                    
                    // Circular currency button matching text field height
                    CompactCircularCurrencyButton(
                        selectedCurrency = selectedCurrency,
                        onCurrencyClick = { currencyExpanded = true },
                        enabled = true,
                        modifier = Modifier.size(56.dp) // Match the text field content height
                    )
                }
                
                // Supporting text for amount field
                if (amountError != null) {
                    Text(
                        text = amountError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else {
                    selectedCurrency?.let { currency ->
                        Text(
                            text = stringResource(R.string.currency_decimal_places_info, currency.displayName, currency.decimalPlaces),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            if (currencyExpanded) {
                AlertDialog(
                    onDismissRequest = { currencyExpanded = false },
                    title = { Text(stringResource(R.string.select_currency)) },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            currencies.forEach { currency ->
                                val isSelected = currency.code == selectedCurrency?.code
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.updateSelectedCurrency(currency)
                                            currencyExpanded = false
                                        }
                                        .padding(vertical = 8.dp, horizontal = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    } else {
                                        Spacer(modifier = Modifier.width(28.dp))
                                    }
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${currency.code} - ${currency.symbol}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            }
                                        )
                                        Text(
                                            text = currency.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                if (currency != currencies.last()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { currencyExpanded = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
            
            // Category dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(stringResource(R.string.category)) },
                    isError = categoryError != null,
                    supportingText = { categoryError?.let { Text(it) } },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                category = option
                                categoryError = if (option.isBlank()) categoryRequiredText else null
                                categoryExpanded = false
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        )
                    }
                }
            }
            
            // Payment Method and Type Card
            FormSectionCard(
                title = stringResource(R.string.payment_method_and_type),
                icon = Icons.Default.Payment
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Payment Type Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PillToggleButton(
                            text = stringResource(R.string.one_time),
                            isSelected = !isRecurring,
                            onClick = { isRecurring = false },
                            modifier = Modifier.weight(1f)
                        )
                        PillToggleButton(
                            text = stringResource(R.string.recurring),
                            isSelected = isRecurring,
                            onClick = { isRecurring = true },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Payment Method Section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PaymentMethod.values().forEach { paymentMethod ->
                            PillToggleButton(
                                text = getLocalizedPaymentMethodName(paymentMethod, LocalContext.current),
                                isSelected = selectedPaymentMethod == paymentMethod,
                                onClick = { selectedPaymentMethod = paymentMethod },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Recurring Period Selection (only shown when recurring is selected)
                    AnimatedVisibility(
                        visible = isRecurring,
                        enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                        exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.recurring_period),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RecurringPeriod.values().forEach { period ->
                                    PillToggleButton(
                                        text = when (period) {
                                            RecurringPeriod.DAILY -> stringResource(R.string.daily)
                                            RecurringPeriod.WEEKLY -> stringResource(R.string.weekly)
                                            RecurringPeriod.MONTHLY -> stringResource(R.string.monthly)
                                            RecurringPeriod.YEARLY -> stringResource(R.string.yearly)
                                        },
                                        isSelected = selectedRecurringPeriod == period,
                                        onClick = { selectedRecurringPeriod = period },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bank Card Selection (only shown when Credit card is selected)
            AnimatedVisibility(
                visible = selectedPaymentMethod == PaymentMethod.CREDIT_CARD && bankCards.isNotEmpty(),
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
            ) {
                FormSectionCard(
                    title = stringResource(R.string.select_bank_card),
                    icon = Icons.Default.CreditCard
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        bankCards.forEach { card ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selectedBankCardId == card.id,
                                        onClick = { selectedBankCardId = card.id }
                                    )
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedBankCardId == card.id,
                                    onClick = { selectedBankCardId = card.id }
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column {
                                    Text(
                                        text = card.alias,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = stringResource(R.string.card_number_masked, card.lastFourDigits),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.payment_day_label, card.paymentDay),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Split Payment Options (only shown when Credit card or Cheque is selected)
            AnimatedVisibility(
                visible = selectedPaymentMethod == PaymentMethod.CREDIT_CARD || selectedPaymentMethod == PaymentMethod.CHEQUE,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(200))
            ) {
                FormSectionCard(
                    title = stringResource(R.string.payments_layout),
                    icon = Icons.Default.Repeat
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Installment selection
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.number_of_payments),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Decrease button
                                IconButton(
                                    onClick = { 
                                        if (installments > 1) {
                                            installments--
                                        }
                                    },
                                    enabled = installments > 1
                                ) {
                                    Icon(
                                        Icons.Default.Remove,
                                        contentDescription = stringResource(R.string.content_desc_decrease_installments),
                                        tint = if (installments > 1) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                                
                                // Installment count display
                                Text(
                                    text = installments.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .width(40.dp)
                                        .clickable { showInstallmentOptions = true }
                                )
                                
                                // Increase button
                                IconButton(
                                    onClick = { 
                                        if (installments < 36) {
                                            installments++
                                        }
                                    },
                                    enabled = installments < 36
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.content_desc_increase_installments),
                                        tint = if (installments < 36) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                        
                        // Monthly payment amount display
                        if (installments > 1 && amount.isNotEmpty() && amount.toDoubleOrNull() != null) {
                            val monthlyAmount = viewModel.calculateInstallmentAmount(amount.toDouble(), installments)
                            val currencySymbol = selectedCurrency?.symbol ?: "$"
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.monthly_payment),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$currencySymbol${String.format("%.2f", monthlyAmount)}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = stringResource(R.string.for_months, installments),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes)) },
                placeholder = { Text(stringResource(R.string.notes_hint)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                    capitalization = KeyboardCapitalization.Sentences
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                ),
                shape = RoundedCornerShape(16.dp)
            )
            
            // Add bottom padding to account for sticky save button
            Spacer(modifier = Modifier.height(80.dp))
            
            // Display error messages from ViewModel
            if (uiState is AddExpenseUiState.Error) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = (uiState as AddExpenseUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
    
    // Installment selection dialog
    if (showInstallmentOptions) {
        AlertDialog(
            onDismissRequest = { showInstallmentOptions = false },
            title = { Text(stringResource(R.string.select_number_of_installments)) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    Text(
                        text = stringResource(R.string.choose_installment_months),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Quick selection buttons for common installment counts
                    val commonInstallments = listOf(1, 3, 6, 12, 18, 24, 36)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(commonInstallments) { installmentCount ->
                            PillToggleButton(
                                text = "${installmentCount}x",
                                isSelected = installments == installmentCount,
                                onClick = { 
                                    installments = installmentCount
                                    showInstallmentOptions = false
                                },
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Custom input
                    OutlinedTextField(
                        value = installments.toString(),
                        onValueChange = { newValue ->
                            val newInstallments = newValue.toIntOrNull()
                            if (newInstallments != null && newInstallments >= 1 && newInstallments <= 36) {
                                installments = newInstallments
                            }
                        },
                        label = { Text(stringResource(R.string.custom_installments)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showInstallmentOptions = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.time
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Date(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Data class to hold expense form data
 */
data class ExpenseFormData(
    val merchant: String,
    val amount: Double,
    val currency: String,
    val category: String,
    val isRecurring: Boolean,
    val recurringPeriod: RecurringPeriod? = null, // Recurring period when isRecurring is true
    val notes: String?,
    val date: Date,
    val paymentMethod: PaymentMethod,
    val installments: Int? = null, // Only used for split payments
    val installmentAmount: Double? = null, // Calculated monthly payment amount
    val selectedBankCardId: Long? = null // Selected bank card ID when payment method is CREDIT_CARD
)
