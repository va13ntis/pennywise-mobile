package com.pennywise.app.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import com.pennywise.app.R
import com.pennywise.app.domain.model.Currency
import com.pennywise.app.presentation.components.CurrencySelectionDropdown
import com.pennywise.app.presentation.components.LocaleSelectionDropdown
import com.pennywise.app.presentation.viewmodel.RegisterViewModel

/**
 * Register screen with enhanced form validation and user feedback
 */
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel,
    onNavigateBack: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    val registerState by viewModel.registerState.collectAsState()
    
    // String resources
    val usernameRequiredText = stringResource(R.string.username_required)
    val passwordRequiredText = stringResource(R.string.password_required)
    val confirmPasswordRequiredText = stringResource(R.string.confirm_password_required)
    val passwordsDontMatchText = stringResource(R.string.passwords_dont_match)
    val passwordTooShortText = stringResource(R.string.password_too_short)
    val currencyRequiredText = stringResource(R.string.currency_required)
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf<Currency?>(Currency.getDefault()) }
    var selectedLocale by remember { mutableStateOf(viewModel.getDetectedLocale()) }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var currencyError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val supportedLocales = remember { viewModel.getSupportedLocales() }
    
    val focusManager = LocalFocusManager.current
    
    // Handle registration success
    LaunchedEffect(registerState) {
        if (registerState is RegisterViewModel.RegisterState.Success) {
            onRegisterSuccess()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Screen title
        Text(
            text = stringResource(R.string.create_account),
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 32.dp),
            textAlign = TextAlign.Center
        )
        
        // Username field
        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it 
                usernameError = if (it.isBlank()) usernameRequiredText else null
            },
            label = { Text(stringResource(R.string.username)) },
            isError = usernameError != null,
            supportingText = { usernameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                textDirection = TextDirection.Ltr
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it 
                passwordError = when {
                    it.isBlank() -> passwordRequiredText
                    it.length < 6 -> passwordTooShortText
                    else -> null
                }
                
                // Update confirm password error if it's not blank
                if (confirmPassword.isNotBlank()) {
                    confirmPasswordError = if (it != confirmPassword) {
                        passwordsDontMatchText
                    } else null
                }
            },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                textDirection = TextDirection.Ltr
            ),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Confirm password field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { 
                confirmPassword = it 
                confirmPasswordError = when {
                    it.isBlank() -> confirmPasswordRequiredText
                    it != password -> passwordsDontMatchText
                    else -> null
                }
            },
            label = { Text(stringResource(R.string.confirm_password)) },
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = confirmPasswordError != null,
            supportingText = { confirmPasswordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.clearFocus() }
            ),
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                textDirection = TextDirection.Ltr
            ),
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                    )
                }
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Currency selection
        CurrencySelectionDropdown(
            currentCurrency = selectedCurrency?.code ?: "USD",
            onCurrencySelected = { currencyCode ->
                selectedCurrency = Currency.fromCode(currencyCode)
                currencyError = null
            }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Language selection
        LocaleSelectionDropdown(
            currentLocale = selectedLocale,
            supportedLocales = supportedLocales,
            onLocaleSelected = { localeCode ->
                selectedLocale = localeCode
            }
        )
        
        // Show detected language info
        Text(
            text = stringResource(R.string.language_auto_detected),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        
        // Error message from ViewModel
        if (registerState is RegisterViewModel.RegisterState.Error) {
            Text(
                text = (registerState as RegisterViewModel.RegisterState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Register button
        Button(
            onClick = { 
                // Validate all fields
                usernameError = if (username.isBlank()) usernameRequiredText else null
                passwordError = when {
                    password.isBlank() -> passwordRequiredText
                    password.length < 6 -> passwordTooShortText
                    else -> null
                }
                confirmPasswordError = when {
                    confirmPassword.isBlank() -> confirmPasswordRequiredText
                    confirmPassword != password -> passwordsDontMatchText
                    else -> null
                }
                currencyError = if (selectedCurrency == null) currencyRequiredText else null
                
                // Only proceed if no validation errors
                if (usernameError == null && passwordError == null && confirmPasswordError == null && currencyError == null) {
                    viewModel.register(username, password, confirmPassword, selectedCurrency?.code ?: "USD", selectedLocale)
                }
            },
            enabled = registerState !is RegisterViewModel.RegisterState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (registerState is RegisterViewModel.RegisterState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(stringResource(R.string.register))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back to login link
        TextButton(
            onClick = onNavigateBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.back_to_login))
        }
    }
}
