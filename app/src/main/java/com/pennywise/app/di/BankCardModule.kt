package com.pennywise.app.di

import android.content.Context
import com.pennywise.app.data.security.CardEncryptionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Bank Card security dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object BankCardModule {
    
    @Provides
    @Singleton
    fun provideCardEncryptionManager(
        @ApplicationContext context: Context
    ): CardEncryptionManager {
        return CardEncryptionManager(context)
    }
}
