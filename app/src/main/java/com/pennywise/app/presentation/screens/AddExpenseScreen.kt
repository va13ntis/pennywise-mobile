package com.pennywise.app.presentation.screens

import android.content.res.Configuration
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.TransactionType
import com.pennywise.app.domain.model.PaymentMethod
import com.pennywise.app.domain.model.BankCard
import com.pennywise.app.presentation.components.CurrencyAdapter
import com.pennywise.app.presentation.util.LocaleFormatter
import com.pennywise.app.presentation.util.CategoryMapper
import com.pennywise.app.presentation.viewmodel.AddExpenseUiState
import com.pennywise.app.presentation.viewmodel.AddExpenseViewModel
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Helper function to validate and format amount input based on currency
 */
fun validateAndFormatAmount(
    input: String,
    currency: Currency?
): String {
    // Only allow digits and decimal point
    val filtered = input.filter { char -> char.isDigit() || char == '.' }
    
    // Determine decimal places to use (default to 2 if no currency selected)
    val allowedDecimalPlaces = currency?.decimalPlaces ?: 2
    
    return when (allowedDecimalPlaces) {
        0 -> {
            // For currencies with no decimal places (JPY, KRW), only allow digits
            filtered.filter { char -> char.isDigit() }
        }
        else -> {
            // For currencies with decimal places, limit to the specified number
            val parts = filtered.split(".")
            if (parts.size > 1) {
                // Only allow one decimal point and limit decimal digits
                parts[0] + "." + parts[1].take(allowedDecimalPlaces)
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
 * Modern Material 3 card container for form sections
 */
@Composable
fun SectionCard(
    title: String? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (title != null || icon != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    title?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            content()
        }
    }
}

/**
 * Material 3 Filled text field with icon
 */
@Composable
fun FilledTextFieldWithIcon(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = 1,
    placeholder: String? = null
) {
    TextField(
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
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        )
    )
}

/**
 * Currency Selector Chip - Pill-shaped surface for trailing icon in Amount field
 */
@Composable
fun CurrencySelectorChip(
    selectedCurrency: Currency?,
    onCurrencyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentDesc = stringResource(R.string.select_currency)
    
    Surface(
        onClick = onCurrencyClick,
        modifier = modifier.semantics { contentDescription = contentDesc },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = selectedCurrency?.symbol ?: "¤",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
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
 * Modern Material 3 Add Expense Screen with card-based layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    // Collect state from ViewModel
    val currentUser by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedCurrency by viewModel.selectedCurrency.collectAsState()
    val bankCards by viewModel.bankCards.collectAsState()
    val defaultPaymentMethod by viewModel.defaultPaymentMethod.collectAsState()
    
    LaunchedEffect(currentUser) {
        Log.d("AddExpenseScreen", "Current user: $currentUser")
    }
    
    // Form state management
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
    
    // Dialog states
    var currencyExpanded by remember { mutableStateOf(false) }
    var showCurrencyBottomSheet by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    
    // Initialize payment method from user's default preference
    LaunchedEffect(defaultPaymentMethod) {
        selectedPaymentMethod = defaultPaymentMethod
    }
    
    // Validation states
    var merchantError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var categoryError by remember { mutableStateOf<String?>(null) }
    
    // Form validation state
    var isFormValid by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()
    
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
    
    // Category options - use CategoryMapper for consistent handling
    val categories = CategoryMapper.getAllCategoryOptions()
    
    // Set default category
    LaunchedEffect(categories) {
        if (category.isEmpty() && categories.isNotEmpty()) {
            category = categories.first()
        }
    }
    
    // Currency adapter and lists
    val currencyAdapter = remember { CurrencyAdapter() }
    val allCurrencies = remember { currencyAdapter.getSortedCurrencies() }
    val commonCurrencyCodes = listOf("USD", "EUR", "GBP", "ILS")
    val commonCurrencies = remember(allCurrencies) {
        allCurrencies.filter { it.code in commonCurrencyCodes }
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
                title = { Text(stringResource(R.string.new_expense)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    
                    Button(
                        onClick = {
                            Log.d("AddExpenseScreen", "=== SAVE BUTTON CLICKED ===")
                            validateForm()
                            
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
                                    category = CategoryMapper.getCategoryKey(category), // Store canonical key
                                    isRecurring = isRecurring,
                                    recurringPeriod = if (isRecurring) selectedRecurringPeriod else null,
                                    notes = notes.ifBlank { null },
                                    date = selectedDate,
                                    paymentMethod = selectedPaymentMethod,
                                    installments = if ((selectedPaymentMethod == PaymentMethod.CREDIT_CARD || selectedPaymentMethod == PaymentMethod.CHEQUE) && installments > 1) installments else null,
                                    installmentAmount = installmentAmount,
                                    selectedBankCardId = if (selectedPaymentMethod == PaymentMethod.CREDIT_CARD) selectedBankCardId else null
                                )
                                viewModel.saveExpense(expenseData)
                            }
                        },
                        enabled = isFormValid && selectedCurrency != null && uiState !is AddExpenseUiState.Loading,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (uiState is AddExpenseUiState.Loading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.save),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Date Selection Card
            SectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = stringResource(R.string.content_desc_calendar),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.select_date),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = try {
                                LocaleFormatter.formatTransactionDate(selectedDate, context)
                            } catch (e: Exception) {
                                try {
                                    DateFormat.getDateInstance(DateFormat.SHORT).format(selectedDate)
                                } catch (e2: Exception) {
                                    SimpleDateFormat("MM/dd/yyyy", Locale.US).format(selectedDate)
                                }
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Merchant & Amount Card
            SectionCard(
                title = stringResource(R.string.transaction_details),
                icon = Icons.Default.Store
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextField(
                        value = merchant,
                        onValueChange = { 
                            merchant = it 
                            merchantError = if (it.isBlank()) merchantRequiredText else null
                        },
                        label = { Text(stringResource(R.string.merchant)) },
                        isError = merchantError != null,
                        supportingText = merchantError?.let { { Text(it) } },
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
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        )
                    )
                    
                    TextField(
                        value = amount,
                        onValueChange = { newValue ->
                            // Always validate and format the amount input
                            val formattedAmount = validateAndFormatAmount(newValue, selectedCurrency)
                            amount = formattedAmount
                            
                            // Validate the formatted amount
                            amountError = when {
                                formattedAmount.isBlank() -> amountRequiredText
                                formattedAmount == "." -> invalidAmountText // Just a decimal point
                                formattedAmount.toDoubleOrNull() == null -> invalidAmountText
                                formattedAmount.toDoubleOrNull()!! <= 0 -> invalidAmountText
                                else -> null
                            }
                        },
                        label = { Text(stringResource(R.string.amount)) },
                        trailingIcon = {
                            Box {
                                CurrencySelectorChip(
                                    selectedCurrency = selectedCurrency,
                                    onCurrencyClick = { currencyExpanded = !currencyExpanded }
                                )
                                
                                // Dropdown menu for common currencies
                                DropdownMenu(
                                    expanded = currencyExpanded,
                                    onDismissRequest = { currencyExpanded = false },
                                    properties = PopupProperties(focusable = false)
                                ) {
                                    // Common currencies
                                    commonCurrencies.forEach { currency ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = "${currency.symbol} ${currency.code}",
                                                        style = MaterialTheme.typography.bodyLarge
                                                    )
                                                    Text(
                                                        text = currency.displayName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                viewModel.updateSelectedCurrency(currency)
                                                currencyExpanded = false
                                            },
                                            leadingIcon = if (selectedCurrency?.code == currency.code) {
                                                {
                                                    Icon(
                                                        Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                    
                                    // "More currencies..." option if there are more than common ones
                                    if (allCurrencies.size > commonCurrencies.size) {
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.MoreHoriz,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.primary
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.more_currencies),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            onClick = {
                                                currencyExpanded = false
                                                showCurrencyBottomSheet = true
                                            }
                                        )
                                    }
                                }
                            }
                        },
                        isError = amountError != null,
                        supportingText = (amountError ?: selectedCurrency?.let {
                            stringResource(R.string.currency_decimal_places_info, it.displayName, it.decimalPlaces)
                        })?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            errorIndicatorColor = Color.Transparent
                        )
                    )
                }
            }
            
            
            // Category Selection Card
            
            SectionCard(
                title = stringResource(R.string.category),
                icon = Icons.Default.Category
            ) {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = category,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(stringResource(R.string.select_category)) },
                        isError = categoryError != null,
                        supportingText = categoryError?.let { { Text(it) } },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
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
                                }
                            )
                        }
                    }
                }
            }
            
            // Payment Details Card
            SectionCard(
                title = stringResource(R.string.payment_details),
                icon = Icons.Default.Payment
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Payment Type
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.payment_type),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !isRecurring,
                                onClick = { isRecurring = false },
                                label = { Text(stringResource(R.string.one_time)) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isRecurring,
                                onClick = { isRecurring = true },
                                label = { Text(stringResource(R.string.recurring)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Payment Method
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.payment_method),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentMethod.values().forEach { paymentMethod ->
                                FilterChip(
                                    selected = selectedPaymentMethod == paymentMethod,
                                    onClick = { selectedPaymentMethod = paymentMethod },
                                    label = { Text(getLocalizedPaymentMethodName(paymentMethod, LocalContext.current)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Recurring Period
                    AnimatedVisibility(
                        visible = isRecurring,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.recurring_period),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RecurringPeriod.values().forEach { period ->
                                    FilterChip(
                                        selected = selectedRecurringPeriod == period,
                                        onClick = { selectedRecurringPeriod = period },
                                        label = { 
                                            Text(when (period) {
                                                RecurringPeriod.DAILY -> stringResource(R.string.daily)
                                                RecurringPeriod.WEEKLY -> stringResource(R.string.weekly)
                                                RecurringPeriod.MONTHLY -> stringResource(R.string.monthly)
                                                RecurringPeriod.YEARLY -> stringResource(R.string.yearly)
                                            })
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bank Card Selection
            AnimatedVisibility(
                visible = selectedPaymentMethod == PaymentMethod.CREDIT_CARD && bankCards.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SectionCard(
                    title = stringResource(R.string.select_bank_card),
                    icon = Icons.Default.CreditCard
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        bankCards.forEach { card ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .selectable(
                                        selected = selectedBankCardId == card.id,
                                        onClick = { selectedBankCardId = card.id }
                                    )
                                    .background(
                                        if (selectedBankCardId == card.id) 
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .padding(12.dp),
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
            
            // Split Payment Options
            AnimatedVisibility(
                visible = !isRecurring && (selectedPaymentMethod == PaymentMethod.CREDIT_CARD || selectedPaymentMethod == PaymentMethod.CHEQUE),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SectionCard(
                    title = stringResource(R.string.payments_layout),
                    icon = Icons.Default.Repeat
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.number_of_payments),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledIconButton(
                                    onClick = { if (installments > 1) installments-- },
                                    enabled = installments > 1,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Remove, contentDescription = stringResource(R.string.content_desc_decrease_installments))
                                }
                                
                                Text(
                                    text = installments.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .width(40.dp)
                                        .clickable { showInstallmentOptions = true },
                                    textAlign = TextAlign.Center
                                )
                                
                                FilledIconButton(
                                    onClick = { if (installments < 36) installments++ },
                                    enabled = installments < 36,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_desc_increase_installments))
                                }
                            }
                        }
                        
                        if (installments > 1 && amount.isNotEmpty() && amount.toDoubleOrNull() != null) {
                            val monthlyAmount = viewModel.calculateInstallmentAmount(amount.toDouble(), installments)
                            val currencySymbol = selectedCurrency?.symbol ?: "$"
                            
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = stringResource(R.string.monthly_payment),
                                        style = MaterialTheme.typography.labelMedium,
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
            
            // Notes Card
            SectionCard(
                title = stringResource(R.string.notes),
                icon = Icons.AutoMirrored.Filled.Notes
            ) {
                FilledTextFieldWithIcon(
                    value = notes,
                    onValueChange = { notes = it },
                    label = stringResource(R.string.notes),
                    icon = Icons.AutoMirrored.Filled.Notes,
                    placeholder = stringResource(R.string.notes_hint),
                    singleLine = false,
                    minLines = 3,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }
            
            // Add bottom padding to account for sticky save button
            Spacer(modifier = Modifier.height(80.dp))
            
            // Display error messages from ViewModel
            if (uiState is AddExpenseUiState.Error) {
                SectionCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = (uiState as AddExpenseUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
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
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.choose_installment_months),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    // Quick selection chips
                    val commonInstallments = listOf(1, 3, 6, 12, 18, 24, 36)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(commonInstallments) { installmentCount ->
                            FilterChip(
                                selected = installments == installmentCount,
                                onClick = { 
                                    installments = installmentCount
                                    showInstallmentOptions = false
                                },
                                label = { Text("${installmentCount}x") }
                            )
                        }
                    }
                    
                    // Custom input
                    TextField(
                        value = installments.toString(),
                        onValueChange = { newValue ->
                            val newInstallments = newValue.toIntOrNull()
                            if (newInstallments != null && newInstallments >= 1 && newInstallments <= 36) {
                                installments = newInstallments
                            }
                        },
                        label = { Text(stringResource(R.string.custom_installments)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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
    
    // Currency Bottom Sheet for all currencies
    if (showCurrencyBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCurrencyBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.select_currency),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    allCurrencies.forEach { currency ->
                        val isSelected = currency.code == selectedCurrency?.code
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSelectedCurrency(currency)
                                    showCurrencyBottomSheet = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                            } else {
                                Spacer(modifier = Modifier.width(36.dp))
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${currency.symbol} ${currency.code}",
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
                        
                        if (currency != allCurrencies.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
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
    val recurringPeriod: RecurringPeriod? = null,
    val notes: String?,
    val date: Date,
    val paymentMethod: PaymentMethod,
    val installments: Int? = null,
    val installmentAmount: Double? = null,
    val selectedBankCardId: Long? = null
)

/**
 * Preview for light mode
 */
@Preview(
    name = "Add Expense Screen - Light",
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun AddExpenseScreenPreviewLight() {
    MaterialTheme {
        AddExpenseScreen(
            onNavigateBack = {}
        )
    }
}

/**
 * Preview for dark mode
 */
@Preview(
    name = "Add Expense Screen - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    showSystemUi = true
)
@Composable
private fun AddExpenseScreenPreviewDark() {
    MaterialTheme {
        AddExpenseScreen(
            onNavigateBack = {}
        )
    }
}
