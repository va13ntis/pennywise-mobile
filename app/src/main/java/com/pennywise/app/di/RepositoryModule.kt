package com.pennywise.app.di

import android.content.Context
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.dao.BankCardDao
import com.pennywise.app.data.local.dao.SplitPaymentInstallmentDao
import com.pennywise.app.data.local.dao.PaymentMethodConfigDao
import com.pennywise.app.data.repository.TransactionRepositoryImpl
import com.pennywise.app.data.repository.UserRepositoryImpl
import com.pennywise.app.data.repository.CurrencyUsageRepositoryImpl
import com.pennywise.app.data.repository.BankCardRepositoryImpl
import com.pennywise.app.data.repository.SplitPaymentInstallmentRepositoryImpl
import com.pennywise.app.data.repository.PaymentMethodConfigRepositoryImpl
import com.pennywise.app.data.util.DataSeeder
import com.pennywise.app.data.util.DataMigrationService
import com.pennywise.app.data.util.SettingsDataStore
import com.pennywise.app.data.security.CardEncryptionManager
import com.pennywise.app.domain.repository.TransactionRepository
import com.pennywise.app.domain.repository.UserRepository
import com.pennywise.app.domain.repository.CurrencyUsageRepository
import com.pennywise.app.domain.repository.BankCardRepository
import com.pennywise.app.domain.repository.SplitPaymentInstallmentRepository
import com.pennywise.app.domain.repository.PaymentMethodConfigRepository
import com.pennywise.app.domain.usecase.CurrencySortingService
import com.pennywise.app.presentation.util.LocaleManager
import com.pennywise.app.presentation.auth.AuthManager
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
     * Provides the BankCardDao
     */
    @Provides
    @Singleton
    fun provideBankCardDao(database: PennyWiseDatabase): BankCardDao {
        return database.bankCardDao()
    }
    
    /**
     * Provides the SplitPaymentInstallmentDao
     */
    @Provides
    @Singleton
    fun provideSplitPaymentInstallmentDao(database: PennyWiseDatabase): SplitPaymentInstallmentDao {
        return database.splitPaymentInstallmentDao()
    }
    
    /**
     * Provides the PaymentMethodConfigDao
     */
    @Provides
    @Singleton
    fun providePaymentMethodConfigDao(database: PennyWiseDatabase): PaymentMethodConfigDao {
        return database.paymentMethodConfigDao()
    }
    
    /**
     * Provides the UserRepository implementation
     */
    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao
    ): UserRepository {
        return UserRepositoryImpl(userDao)
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
        transactionDao: TransactionDao
    ): DataSeeder {
        return DataSeeder(userDao, transactionDao)
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
    
    /**
     * Provides the BankCardRepository implementation
     */
    @Provides
    @Singleton
    fun provideBankCardRepository(
        bankCardDao: BankCardDao,
        encryptionManager: CardEncryptionManager
    ): BankCardRepository {
        return BankCardRepositoryImpl(bankCardDao, encryptionManager)
    }
    
    /**
     * Provides the SplitPaymentInstallmentRepository implementation
     */
    @Provides
    @Singleton
    fun provideSplitPaymentInstallmentRepository(
        splitPaymentInstallmentDao: SplitPaymentInstallmentDao
    ): SplitPaymentInstallmentRepository {
        return SplitPaymentInstallmentRepositoryImpl(splitPaymentInstallmentDao)
    }
    
    /**
     * Provides the PaymentMethodConfigRepository implementation
     */
    @Provides
    @Singleton
    fun providePaymentMethodConfigRepository(
        paymentMethodConfigDao: PaymentMethodConfigDao
    ): PaymentMethodConfigRepository {
        return PaymentMethodConfigRepositoryImpl(paymentMethodConfigDao)
    }
    
    /**
     * Provides the SettingsDataStore utility
     */
    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
    
    /**
     * Provides the LocaleManager utility
     */
    @Provides
    @Singleton
    fun provideLocaleManager(): LocaleManager {
        return LocaleManager()
    }
    
    /**
     * Provides the AuthManager
     */
    @Provides
    @Singleton
    fun provideAuthManager(
        @ApplicationContext context: Context,
        userRepository: UserRepository
    ): AuthManager {
        return AuthManager(context, userRepository)
    }
    
}
