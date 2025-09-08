package com.pennywise.app.di

import android.content.Context
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.repository.TransactionRepositoryImpl
import com.pennywise.app.data.repository.UserRepositoryImpl
import com.pennywise.app.data.repository.CurrencyUsageRepositoryImpl
import com.pennywise.app.data.util.PasswordHasher
import com.pennywise.app.data.util.DataSeeder
import com.pennywise.app.data.util.DataMigrationService
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.usecase.CurrencySortingService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing repository dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    /**
     * Provides the PennyWise database instance
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PennyWiseDatabase {
        return PennyWiseDatabase.getDatabase(context)
    }
    
    /**
     * Provides the UserDao
     */
    @Provides
    @Singleton
    fun provideUserDao(database: PennyWiseDatabase): UserDao {
        return database.userDao()
    }
    
    /**
     * Provides the TransactionDao
     */
    @Provides
    @Singleton
    fun provideTransactionDao(database: PennyWiseDatabase): TransactionDao {
        return database.transactionDao()
    }
    
    /**
     * Provides the CurrencyUsageDao
     */
    @Provides
    @Singleton
    fun provideCurrencyUsageDao(database: PennyWiseDatabase): CurrencyUsageDao {
        return database.currencyUsageDao()
    }
    
    /**
     * Provides the PasswordHasher utility
     */
    @Provides
    @Singleton
    fun providePasswordHasher(): PasswordHasher {
        return PasswordHasher()
    }
    
    /**
     * Provides the UserRepository implementation
     */
    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        passwordHasher: PasswordHasher
    ): UserRepository {
        return UserRepositoryImpl(userDao, passwordHasher)
    }
    
    /**
     * Provides the TransactionRepository implementation
     */
    @Provides
    @Singleton
    fun provideTransactionRepository(
        transactionDao: TransactionDao
    ): TransactionRepository {
        return TransactionRepositoryImpl(transactionDao)
    }
    
    /**
     * Provides the CurrencyUsageRepository implementation
     */
    @Provides
    @Singleton
    fun provideCurrencyUsageRepository(
        currencyUsageDao: CurrencyUsageDao
    ): CurrencyUsageRepository {
        return CurrencyUsageRepositoryImpl(currencyUsageDao)
    }
    
    /**
     * Provides the DataSeeder utility
     */
    @Provides
    @Singleton
    fun provideDataSeeder(
        userDao: UserDao,
        transactionDao: TransactionDao,
        passwordHasher: PasswordHasher
    ): DataSeeder {
        return DataSeeder(userDao, transactionDao, passwordHasher)
    }
    
    /**
     * Provides the DataMigrationService
     */
    @Provides
    @Singleton
    fun provideDataMigrationService(
        userRepository: UserRepository,
        transactionRepository: TransactionRepository,
        currencyUsageRepository: CurrencyUsageRepository
    ): DataMigrationService {
        return DataMigrationService(userRepository, transactionRepository, currencyUsageRepository)
    }
    
    /**
     * Provides the CurrencySortingService
     */
    @Provides
    @Singleton
    fun provideCurrencySortingService(
        currencyUsageRepository: CurrencyUsageRepository,
        userRepository: UserRepository
    ): CurrencySortingService {
        return CurrencySortingService(currencyUsageRepository, userRepository)
    }
}
