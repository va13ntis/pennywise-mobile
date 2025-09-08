package com.pennywise.app.presentation.screens

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.pennywise.app.R
import com.pennywise.app.domain.model.User
import com.pennywise.app.presentation.viewmodel.LoginViewModel
import com.pennywise.app.presentation.viewmodel.TestDataViewModel

/**
 * Login screen with enhanced form validation and user feedback
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: (User) -> Unit,
    testDataViewModel: TestDataViewModel = hiltViewModel()
) {
    val loginState by viewModel.loginState.collectAsState()
    val isUserRegistered by viewModel.isUserRegistered.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()
    val canUseBiometric = viewModel.canUseBiometric
    
    // Test data states
    val isSeeding by testDataViewModel.isSeeding.collectAsState()
    val isClearing by testDataViewModel.isClearing.collectAsState()
    val testMessage by testDataViewModel.message.collectAsState()
    
    // String resources
    val usernameRequiredText = stringResource(R.string.username_required)
    val passwordRequiredText = stringResource(R.string.password_required)
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    
    // Handle login success
    LaunchedEffect(loginState) {
        if (loginState is LoginViewModel.LoginState.Success) {
            onLoginSuccess((loginState as LoginViewModel.LoginState.Success).user)
        }
    }
    
    // Handle biometric authentication
    fun handleBiometricAuthentication() {
        if (canUseBiometric && isBiometricEnabled) {
            val activity = context as? FragmentActivity
            if (activity != null) {
                // For now, we'll use a simple approach - in a real implementation,
                // you would integrate with the BiometricAuthManager here
                // This is a placeholder for the biometric authentication flow
                viewModel.login("", "") // This would be replaced with actual biometric login
            }
        }
    }
    
    // Show test data messages
    LaunchedEffect(testMessage) {
        testMessage?.let { message ->
            // For now, we'll just clear the message after a delay
            // In a real app, you'd show this in a snackbar or dialog
            kotlinx.coroutines.delay(3000)
            testDataViewModel.clearMessage()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App title
        Text(
            text = stringResource(R.string.app_name),
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
                passwordError = if (it.isBlank()) passwordRequiredText else null
            },
            label = { Text(stringResource(R.string.password)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
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
        
        // Error message from ViewModel
        if (loginState is LoginViewModel.LoginState.Error) {
            Text(
                text = (loginState as LoginViewModel.LoginState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Login button
        Button(
            onClick = { 
                // Validate fields
                usernameError = if (username.isBlank()) usernameRequiredText else null
                passwordError = if (password.isBlank()) passwordRequiredText else null
                
                // Only proceed if no validation errors
                if (usernameError == null && passwordError == null) {
                    viewModel.login(username, password)
                }
            },
            enabled = loginState !is LoginViewModel.LoginState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            if (loginState is LoginViewModel.LoginState.Loading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(stringResource(R.string.login))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Biometric authentication button - only show if available and enabled
        if (canUseBiometric && isBiometricEnabled) {
            OutlinedButton(
                onClick = { handleBiometricAuthentication() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = loginState !is LoginViewModel.LoginState.Loading
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Biometric Authentication",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.login_with_biometric))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Register link - only show if user is not registered
        if (!isUserRegistered) {
            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.create_account))
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Test data section
        LoginTestDataSection(
            onSeedTestData = { testDataViewModel.seedTestData() },
            onClearTestData = { testDataViewModel.clearTestData() },
            isSeeding = isSeeding,
            isClearing = isClearing
        )
    }
}

/**
 * Test data section for the login screen
 */
@Composable
fun LoginTestDataSection(
    modifier: Modifier = Modifier,
    onSeedTestData: () -> Unit = {},
    onClearTestData: () -> Unit = {},
    isSeeding: Boolean = false,
    isClearing: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "🧪 Test Data",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create test user and sample data",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onSeedTestData,
            enabled = !isSeeding && !isClearing,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSeeding) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isSeeding) "Creating Test Data..." else "Create Test Data"
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onClearTestData,
            enabled = !isSeeding && !isClearing,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            if (isClearing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onError
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = if (isClearing) "Clearing Data..." else "Clear All Data"
            )
        }
    }
}
