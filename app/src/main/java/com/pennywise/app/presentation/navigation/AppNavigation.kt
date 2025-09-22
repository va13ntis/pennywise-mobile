package com.pennywise.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.collect
import com.pennywise.app.presentation.viewmodel.AuthViewModel
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import com.pennywise.app.presentation.screens.FirstRunSetupScreen
import com.pennywise.app.presentation.screens.HomeScreen
import com.pennywise.app.presentation.screens.AddExpenseScreen
import com.pennywise.app.presentation.screens.SettingsScreen
import com.pennywise.app.presentation.viewmodel.DeviceAuthPromptViewModel

/**
 * Main app navigation that handles simplified authentication flow
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val shouldRequireDeviceAuth by authViewModel.shouldRequireDeviceAuth.collectAsState(initial = false)
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState(initial = false)
    val isInitialized by authViewModel.isInitialized.collectAsState(initial = false)
    
    // Log route changes
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            println("🧭 Navigation: current route = ${entry.destination.route}")
        }
    }
    
    // Initialize authentication state when the app starts; the ViewModel will
    // set isInitialized=true only after its async work completes
    LaunchedEffect(Unit) {
        try {
            authViewModel.initializeAuthState()
        } catch (_: Exception) {
        }
    }
    
    // Show loading screen while initializing
    if (!isInitialized) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    // Determine the appropriate start destination (no separate device-auth screen)
    val startDestination = when {
        currentUser == null -> FIRST_RUN_SETUP_ROUTE
        else -> MAIN_ROUTE
    }
    
    // Handle navigation based on authentication state changes
    LaunchedEffect(currentUser, shouldRequireDeviceAuth, isAuthenticated, isInitialized) {
        val currentRoute = navController.currentDestination?.route
        
        // Extended logging for debugging preferences
        val localUser = currentUser // Store in local variable to fix smart cast issue
        localUser?.let { user ->
            println("🔍 AppNavigation: USER PREFERENCES CHECK")
            println("   - User ID: ${user.id}")
            println("   - Default Currency: ${user.defaultCurrency}")
            println("   - Locale: ${user.locale}")
            println("   - Device Auth Enabled: ${user.deviceAuthEnabled}")
            println("   - Created At: ${user.createdAt}")
            println("   - Updated At: ${user.updatedAt}")
        }
        
        println("🔍 AppNavigation: currentUser = $currentUser, shouldRequireDeviceAuth = $shouldRequireDeviceAuth, isAuthenticated = $isAuthenticated, isInitialized = $isInitialized, currentRoute = $currentRoute")
        
        if (!isInitialized) return@LaunchedEffect
        
        if (currentUser != null) {
            if (isAuthenticated && currentRoute != MAIN_ROUTE) {
                // User is authenticated, go to main screen
                println("🔍 AppNavigation: Navigating to MAIN_ROUTE (user authenticated)")
                // Verify settings are properly applied before navigation
                val user = currentUser // Capture local reference to avoid smart cast issue
                println("✅ AppNavigation: FINAL VERIFICATION - Currency: ${user?.defaultCurrency}, Locale: ${user?.locale}")
                navController.navigate(MAIN_ROUTE) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (currentRoute != FIRST_RUN_SETUP_ROUTE) {
            // No user, go to first run setup
            println("🔍 AppNavigation: Navigating to FIRST_RUN_SETUP_ROUTE")
            navController.navigate(FIRST_RUN_SETUP_ROUTE) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // First run setup screen
        composable(FIRST_RUN_SETUP_ROUTE) {
            LaunchedEffect(Unit) { println("🖼️ Screen: FirstRunSetupScreen composing") }
            FirstRunSetupScreen(
                onSetupComplete = {
                    // After setup, the user should be authenticated automatically
                    // Refresh authentication state to ensure all settings are recognized
                    println("🔍 AppNavigation: Setup complete, explicitly refreshing auth state")
                    authViewModel.initializeAuthState() // Re-initialize to pick up new settings
                    // The LaunchedEffect will handle navigation based on authentication state
                }
            )
        }
        
        // Main app content after authentication
        composable(MAIN_ROUTE) {
            val context = LocalContext.current
            val devicePromptViewModel = hiltViewModel<DeviceAuthPromptViewModel>()
            val deviceUiState = devicePromptViewModel.uiState.collectAsState()
            
            var promptStarted by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { println("🖼️ Screen: MainRoute composing (promptStarted=$promptStarted)") }
            
            // Trigger device auth when required, without a dedicated screen
            LaunchedEffect(shouldRequireDeviceAuth, isAuthenticated) {
                if (shouldRequireDeviceAuth && !isAuthenticated && !promptStarted) {
                    println("🔐 DeviceAuth: required, starting authenticate() before rendering main content")
                    (context as? FragmentActivity)?.let { activity ->
                        promptStarted = true
                        devicePromptViewModel.authenticate(activity)
                    }
                }
            }
            
            // When device auth succeeds, mark authenticated
            LaunchedEffect(deviceUiState.value.isAuthenticated) {
                if (deviceUiState.value.isAuthenticated) {
                    println("✅ DeviceAuth: success detected in MainRoute, marking user authenticated")
                    authViewModel.markUserAsAuthenticated()
                }
            }
            
            // If auth required and not yet authenticated, render nothing to avoid flash
            if (shouldRequireDeviceAuth && !isAuthenticated) {
                println("⏸️ MainRoute: withholding UI until device auth completes (no rendering)")
                return@composable
            }
            
            println("🖼️ Screen: HomeScreen composing")
            val homeViewModel = hiltViewModel<HomeViewModel>()
            val mainCurrentUser = authViewModel.getCurrentUser()
            
            // Set the user ID for the HomeViewModel
            LaunchedEffect(mainCurrentUser) {
                mainCurrentUser?.let { user ->
                    println("🔄 AppNavigation: Setting user ID ${user.id} for HomeViewModel")
                    homeViewModel.setUserId(user.id)
                } ?: run {
                    println("❌ AppNavigation: No current user found!")
                }
            }
            
            HomeScreen(
                onAddExpense = {
                    navController.navigate(ADD_EXPENSE_ROUTE)
                },
                onNavigateToSettings = {
                    navController.navigate(SETTINGS_ROUTE)
                },
                viewModel = homeViewModel
            )
        }
        
        // Add Expense screen
        composable(ADD_EXPENSE_ROUTE) {
            AddExpenseScreen(
                onNavigateBack = { navController.popBackStack() },
                onSaveExpense = { } // Not used, ViewModel handles saving
            )
        }
        
        // Settings screen
        composable(SETTINGS_ROUTE) {
            val settingsViewModel = hiltViewModel<com.pennywise.app.presentation.viewmodel.SettingsViewModel>()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    // For simplified auth, logout just clears the user and goes to first run setup
                    authViewModel.logout()
                    navController.navigate(FIRST_RUN_SETUP_ROUTE) {
                        popUpTo(MAIN_ROUTE) { inclusive = true }
                    }
                }
            )
        }
    }
}

/**
 * Navigation routes for the main app
 */
const val FIRST_RUN_SETUP_ROUTE = "first_run_setup"
const val MAIN_ROUTE = "main"
const val ADD_EXPENSE_ROUTE = "add_expense"
const val SETTINGS_ROUTE = "settings"


