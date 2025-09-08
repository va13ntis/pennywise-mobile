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
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import com.pennywise.app.presentation.viewmodel.AuthViewModel
import com.pennywise.app.presentation.viewmodel.LoginViewModel
import com.pennywise.app.presentation.viewmodel.RegisterViewModel
import com.pennywise.app.presentation.viewmodel.HomeViewModel
import com.pennywise.app.presentation.screens.LoginScreen
import com.pennywise.app.presentation.screens.RegisterScreen
import com.pennywise.app.presentation.screens.HomeScreen
import com.pennywise.app.presentation.screens.AddExpenseScreen
import com.pennywise.app.presentation.screens.SettingsScreen
import com.pennywise.app.presentation.navigation.LOGIN_ROUTE
import com.pennywise.app.presentation.navigation.REGISTER_ROUTE

/**
 * Main app navigation that handles authentication state and routes
 */
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState(initial = null)
    val isAuthenticated = currentUser != null
    
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
    
    // Handle navigation when authentication state changes
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            // User is authenticated, navigate to main app
            navController.navigate(MAIN_ROUTE) {
                // Clear the back stack so user can't go back to login/register
                popUpTo(LOGIN_ROUTE) { inclusive = true }
            }
        } else {
            // User is not authenticated, navigate to login
            navController.navigate(LOGIN_ROUTE) {
                // Clear the back stack so user can't go back to main app
                popUpTo(MAIN_ROUTE) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (currentUser != null) MAIN_ROUTE else LOGIN_ROUTE
    ) {
        // Login screen
        composable(LOGIN_ROUTE) {
            val viewModel = hiltViewModel<LoginViewModel>()
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate(REGISTER_ROUTE) },
                onLoginSuccess = { user ->
                    // Navigation will be handled automatically by the isAuthenticated state change
                }
            )
        }
        
        // Register screen
        composable(REGISTER_ROUTE) {
            val viewModel = hiltViewModel<RegisterViewModel>()
            RegisterScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onRegisterSuccess = { 
                    // Navigation will be handled automatically by the authentication state change
                    // No need to navigate manually here
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
                }
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
                    authViewModel.logout()
                }
            )
        }
    }
}

/**
 * Navigation routes for the main app
 */
const val MAIN_ROUTE = "main"
const val ADD_EXPENSE_ROUTE = "add_expense"
const val SETTINGS_ROUTE = "settings"


