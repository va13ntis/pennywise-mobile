package com.pennywise.app.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.pennywise.app.presentation.theme.PennyWiseThemeWithManager
import com.pennywise.app.presentation.PennyWiseApp
import com.pennywise.app.presentation.theme.ThemeManager
import com.pennywise.app.presentation.util.LocaleManager
import com.pennywise.app.data.util.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var themeManager: ThemeManager
    
    @Inject
    lateinit var localeManager: LocaleManager
    
    @Inject
    lateinit var settingsDataStore: SettingsDataStore
    
    private var currentLanguageCode: String = ""
    private var isRestartingForLanguage: Boolean = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply initial locale based on saved preference
        applyInitialLocale()
        
        // Observe language changes and restart if needed
        observeLanguageChanges()
        
        setContent {
            PennyWiseThemeWithManager(themeManager = themeManager) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PennyWiseApp()
                }
            }
        }
    }
    
    /**
     * Apply the initial locale based on saved preference
     */
    private fun applyInitialLocale() {
        lifecycleScope.launch {
            val languageCode = settingsDataStore.language.first()
            currentLanguageCode = languageCode
            if (languageCode.isNotEmpty()) {
                applyLocaleChange(languageCode)
            }
        }
    }
    
    /**
     * Observe language changes from DataStore and restart if needed
     */
    private fun observeLanguageChanges() {
        lifecycleScope.launch {
            settingsDataStore.language.collectLatest { languageCode ->
                if (languageCode != currentLanguageCode && !isRestartingForLanguage) {
                    currentLanguageCode = languageCode
                    restartForLanguageChange()
                }
            }
        }
    }
    
    /**
     * Apply locale change and restart the activity
     * @param languageCode The language code from DataStore
     */
    private fun applyLocaleChange(languageCode: String) {
        val updatedContext = if (languageCode.isEmpty()) {
            localeManager.updateLocaleToSystem(this)
        } else {
            localeManager.updateLocale(this, languageCode)
        }
        
        // Update the base context configuration
        baseContext.resources.updateConfiguration(
            updatedContext.resources.configuration,
            updatedContext.resources.displayMetrics
        )
    }
    
    /**
     * Restart the activity for language change
     */
    private fun restartForLanguageChange() {
        isRestartingForLanguage = true
        
        // Apply the locale change
        applyLocaleChange(currentLanguageCode)
        
        // Restart the activity
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }
}

