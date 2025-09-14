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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import com.pennywise.app.presentation.viewmodel.AuthViewModel
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import com.pennywise.app.presentation.screens.FirstRunSetupScreen
import com.pennywise.app.presentation.screens.DeviceAuthPromptScreen
import com.pennywise.app.presentation.screens.HomeScreen
import com.pennywise.app.presentation.screens.AddExpenseScreen
import com.pennywise.app.presentation.screens.SettingsScreen

/**
 * Main app navigation that handles simplified authentication flow
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val isDeviceAuthEnabled by authViewModel.isDeviceAuthEnabled.collectAsState(initial = false)
    
    // Show loading state initially
    var isInitialized by remember { mutableStateOf(false) }
    
    // Initialize authentication state when the app starts
    LaunchedEffect(Unit) {
        try {
            authViewModel.initializeAuthState()
        } catch (e: Exception) {
            // If initialization fails, just continue with unauthenticated state
        } finally {
            isInitialized = true
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
    
    // Determine the appropriate start destination
    val startDestination = when {
        currentUser == null -> FIRST_RUN_SETUP_ROUTE
        isDeviceAuthEnabled -> DEVICE_AUTH_PROMPT_ROUTE
        else -> MAIN_ROUTE
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // First run setup screen
        composable(FIRST_RUN_SETUP_ROUTE) {
            FirstRunSetupScreen(
                onSetupComplete = {
                    // Navigate to device auth prompt if enabled, otherwise to main
                    if (isDeviceAuthEnabled) {
                        navController.navigate(DEVICE_AUTH_PROMPT_ROUTE) {
                            popUpTo(FIRST_RUN_SETUP_ROUTE) { inclusive = true }
                        }
                    } else {
                        navController.navigate(MAIN_ROUTE) {
                            popUpTo(FIRST_RUN_SETUP_ROUTE) { inclusive = true }
                        }
                    }
                }
            )
        }
        
        // Device authentication prompt screen
        composable(DEVICE_AUTH_PROMPT_ROUTE) {
            DeviceAuthPromptScreen(
                onAuthSuccess = {
                    navController.navigate(MAIN_ROUTE) {
                        popUpTo(DEVICE_AUTH_PROMPT_ROUTE) { inclusive = true }
                    }
                },
                onAuthCancel = {
                    // User cancelled authentication, go back to first run setup
                    navController.navigate(FIRST_RUN_SETUP_ROUTE) {
                        popUpTo(DEVICE_AUTH_PROMPT_ROUTE) { inclusive = true }
                    }
                }
            )
        }
        
        // Main app content after authentication
        composable(MAIN_ROUTE) {
            val homeViewModel = hiltViewModel<HomeViewModel>()
            val currentUser = authViewModel.getCurrentUser()
            
            // Set the user ID for the HomeViewModel
            LaunchedEffect(currentUser) {
                currentUser?.let { user ->
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
const val DEVICE_AUTH_PROMPT_ROUTE = "device_auth_prompt"
const val MAIN_ROUTE = "main"
const val ADD_EXPENSE_ROUTE = "add_expense"
const val SETTINGS_ROUTE = "settings"


