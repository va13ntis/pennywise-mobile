package com.pennywise.app

import android.app.Application
import android.content.pm.ApplicationInfo
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Main Application class for PennyWise
 */
@HiltAndroidApp
class PennyWiseApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            Timber.plant(Timber.DebugTree())
        }
        
        Timber.d("PennyWise Application initialized")
    }
}
