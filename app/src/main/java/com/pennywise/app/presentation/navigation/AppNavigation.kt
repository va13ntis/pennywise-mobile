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
    val homeViewModel: HomeViewModel = hiltViewModel() // Shared across all screens
    
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val shouldRequireDeviceAuth by authViewModel.shouldRequireDeviceAuth.collectAsState(initial = false)
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState(initial = false)
    val isInitialized by authViewModel.isInitialized.collectAsState(initial = false)
    
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
        
        if (!isInitialized) return@LaunchedEffect
        
        if (currentUser != null) {
            if (isAuthenticated && currentRoute != MAIN_ROUTE) {
                // User is authenticated, go to main screen
                navController.navigate(MAIN_ROUTE) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else if (currentRoute != FIRST_RUN_SETUP_ROUTE) {
            // No user, go to first run setup
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
            FirstRunSetupScreen(
                onSetupComplete = {
                    // After setup, the user should be authenticated automatically
                    // Refresh authentication state to ensure all settings are recognized
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
            
            // Trigger device auth when required, without a dedicated screen
            LaunchedEffect(shouldRequireDeviceAuth, isAuthenticated) {
                if (shouldRequireDeviceAuth && !isAuthenticated && !promptStarted) {
                    (context as? FragmentActivity)?.let { activity ->
                        promptStarted = true
                        devicePromptViewModel.authenticate(activity)
                    }
                }
            }
            
            // When device auth succeeds, mark authenticated
            LaunchedEffect(deviceUiState.value.isAuthenticated) {
                if (deviceUiState.value.isAuthenticated) {
                    authViewModel.markUserAsAuthenticated()
                }
            }
            
            // If auth required and not yet authenticated, render nothing to avoid flash
            if (shouldRequireDeviceAuth && !isAuthenticated) {
                return@composable
            }
            
            // HomeScreen uses shared HomeViewModel with actual transaction data
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
                onNavigateBack = { 
                    // Refresh home data before navigating back using shared ViewModel
                    homeViewModel.refreshData()
                    navController.popBackStack() 
                }
            )
        }
        
        // Settings screen
        composable(SETTINGS_ROUTE) {
            val settingsViewModel = hiltViewModel<com.pennywise.app.presentation.viewmodel.SettingsViewModel>()
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
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


