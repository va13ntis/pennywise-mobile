package com.pennywise.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for PennyWise app
 */
object DatabaseMigrations {
    
    /**
     * Migration from version 1 to 2
     * - Add defaultCurrency column to users table
     * - Add currency column to transactions table
     * - Create currency_usage table
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add defaultCurrency column to users table
            database.execSQL(
                "ALTER TABLE users ADD COLUMN defaultCurrency TEXT NOT NULL DEFAULT 'USD'"
            )
            
            // Add currency column to transactions table
            database.execSQL(
                "ALTER TABLE transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD'"
            )
            
            // Create currency_usage table
            database.execSQL(
                """
                CREATE TABLE currency_usage (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    currency TEXT NOT NULL,
                    usageCount INTEGER NOT NULL DEFAULT 0,
                    lastUsed INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """
            )
            
            // Create indices for currency_usage table
            database.execSQL(
                "CREATE INDEX index_currency_usage_userId ON currency_usage (userId)"
            )
            database.execSQL(
                "CREATE UNIQUE INDEX index_currency_usage_userId_currency ON currency_usage (userId, currency)"
            )
        }
    }
    
    /**
     * Migration from version 2 to 3
     * - Add paymentMethod column to transactions table
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add paymentMethod column to transactions table
            database.execSQL(
                "ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'"
            )
        }
    }
    
    /**
     * Migration from version 3 to 4
     * - Add installments and installmentAmount columns to transactions table for split payments
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add installments column to transactions table
            database.execSQL(
                "ALTER TABLE transactions ADD COLUMN installments INTEGER"
            )
            
            // Add installmentAmount column to transactions table
            database.execSQL(
                "ALTER TABLE transactions ADD COLUMN installmentAmount REAL"
            )
        }
    }
    
    /**
     * Migration from version 4 to 5
     * - Create bank_cards table for bank card management
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create bank_cards table
            database.execSQL(
                """
                CREATE TABLE bank_cards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    alias TEXT NOT NULL,
                    lastFourDigits TEXT NOT NULL,
                    paymentDay INTEGER NOT NULL,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """
            )
            
            // Create indices for bank_cards table
            database.execSQL(
                "CREATE INDEX index_bank_cards_userId ON bank_cards (userId)"
            )
        }
    }
    
    /**
     * Migration from version 5 to 6
     * - Create split_payment_installments table for tracking split payments by months
     */
    val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create split_payment_installments table
            database.execSQL(
                """
                CREATE TABLE split_payment_installments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    parentTransactionId INTEGER NOT NULL,
                    userId INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    currency TEXT NOT NULL DEFAULT 'USD',
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    type TEXT NOT NULL,
                    dueDate INTEGER NOT NULL,
                    installmentNumber INTEGER NOT NULL,
                    totalInstallments INTEGER NOT NULL,
                    isPaid INTEGER NOT NULL DEFAULT 0,
                    paidDate INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (parentTransactionId) REFERENCES transactions(id) ON DELETE CASCADE
                )
                """
            )
            
            // Create indices for split_payment_installments table
            database.execSQL(
                "CREATE INDEX index_split_payment_installments_userId ON split_payment_installments (userId)"
            )
            database.execSQL(
                "CREATE INDEX index_split_payment_installments_parentTransactionId ON split_payment_installments (parentTransactionId)"
            )
            database.execSQL(
                "CREATE INDEX index_split_payment_installments_dueDate ON split_payment_installments (dueDate)"
            )
            database.execSQL(
                "CREATE INDEX index_split_payment_installments_isPaid ON split_payment_installments (isPaid)"
            )
        }
    }
    
    /**
     * Migration from version 6 to 7
     * - Create payment_method_configs table for managing payment method configurations
     */
    val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create payment_method_configs table
            database.execSQL(
                """
                CREATE TABLE payment_method_configs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    alias TEXT NOT NULL,
                    isDefault INTEGER NOT NULL,
                    withdrawDay INTEGER,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """
            )
            
            // Create indices for payment_method_configs table
            database.execSQL(
                "CREATE INDEX index_payment_method_configs_userId ON payment_method_configs (userId)"
            )
            database.execSQL(
                "CREATE INDEX index_payment_method_configs_paymentMethod ON payment_method_configs (paymentMethod)"
            )
            database.execSQL(
                "CREATE INDEX index_payment_method_configs_isDefault ON payment_method_configs (isDefault)"
            )
        }
    }
    
    /**
     * Migration from version 7 to 8
     * - Fix payment_method_configs table schema to match entity definition
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Drop and recreate the payment_method_configs table with correct schema
            database.execSQL("DROP TABLE IF EXISTS payment_method_configs")
            
            // Recreate the table with the correct schema (no SQL default values)
            database.execSQL(
                """
                CREATE TABLE payment_method_configs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    alias TEXT NOT NULL,
                    isDefault INTEGER NOT NULL,
                    withdrawDay INTEGER,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """
            )
            
            // Recreate indices
            database.execSQL(
                "CREATE INDEX index_payment_method_configs_userId ON payment_method_configs (userId)"
            )
            database.execSQL(
                "CREATE INDEX index_payment_method_configs_paymentMethod ON payment_method_configs (paymentMethod)"
            )
            database.execSQL(
                "CREATE INDEX index_payment_method_configs_isDefault ON payment_method_configs (isDefault)"
            )
        }
    }
    
    /**
     * Migration from version 8 to 9
     * - Force complete database recreation to fix schema issues
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Drop all tables to force complete recreation
            database.execSQL("DROP TABLE IF EXISTS payment_method_configs")
            database.execSQL("DROP TABLE IF EXISTS split_payment_installments")
            database.execSQL("DROP TABLE IF EXISTS bank_cards")
            database.execSQL("DROP TABLE IF EXISTS currency_usage")
            database.execSQL("DROP TABLE IF EXISTS transactions")
            database.execSQL("DROP TABLE IF EXISTS users")
            
            // Recreate all tables with correct schema
            
            // Users table
            database.execSQL(
                """
                CREATE TABLE users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    username TEXT NOT NULL UNIQUE,
                    passwordHash TEXT NOT NULL,
                    email TEXT,
                    defaultCurrency TEXT NOT NULL,
                    locale TEXT NOT NULL,
                    role TEXT NOT NULL,
                    status TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """
            )
            
            // Transactions table
            database.execSQL(
                """
                CREATE TABLE transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    currency TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    type TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    isRecurring INTEGER NOT NULL,
                    recurringPeriod TEXT,
                    paymentMethod TEXT NOT NULL,
                    installments INTEGER,
                    installmentAmount REAL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """
            )
            
            // Create index for transactions table
            database.execSQL("CREATE INDEX index_transactions_userId ON transactions (userId)")
            
            // Currency usage table
            database.execSQL(
                """
                CREATE TABLE currency_usage (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    currency TEXT NOT NULL,
                    usageCount INTEGER NOT NULL DEFAULT 0,
                    lastUsed INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """
            )
            
            // Bank cards table
            database.execSQL(
                """
                CREATE TABLE bank_cards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    alias TEXT NOT NULL,
                    lastFourDigits TEXT NOT NULL,
                    paymentDay INTEGER NOT NULL,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE
                )
                """
            )
            
            // Split payment installments table
            database.execSQL(
                """
                CREATE TABLE split_payment_installments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    parentTransactionId INTEGER NOT NULL,
                    userId INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    currency TEXT NOT NULL DEFAULT 'USD',
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    type TEXT NOT NULL,
                    dueDate INTEGER NOT NULL,
                    installmentNumber INTEGER NOT NULL,
                    totalInstallments INTEGER NOT NULL,
                    isPaid INTEGER NOT NULL DEFAULT 0,
                    paidDate INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
                    FOREIGN KEY (parentTransactionId) REFERENCES transactions(id) ON DELETE CASCADE
                )
                """
            )
            
            // Payment method configs table
            database.execSQL(
                """
                CREATE TABLE payment_method_configs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    userId INTEGER NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    alias TEXT NOT NULL,
                    isDefault INTEGER NOT NULL,
                    withdrawDay INTEGER,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """
            )
            
            // Create all indices
            database.execSQL("CREATE INDEX index_currency_usage_userId ON currency_usage (userId)")
            database.execSQL("CREATE UNIQUE INDEX index_currency_usage_userId_currency ON currency_usage (userId, currency)")
            database.execSQL("CREATE INDEX index_bank_cards_userId ON bank_cards (userId)")
            database.execSQL("CREATE INDEX index_split_payment_installments_userId ON split_payment_installments (userId)")
            database.execSQL("CREATE INDEX index_split_payment_installments_parentTransactionId ON split_payment_installments (parentTransactionId)")
            database.execSQL("CREATE INDEX index_split_payment_installments_dueDate ON split_payment_installments (dueDate)")
            database.execSQL("CREATE INDEX index_split_payment_installments_isPaid ON split_payment_installments (isPaid)")
        }
    }
    
    /**
     * Migration from version 9 to 10
     * - Simplify users table for single-user per app with device authentication
     * - Remove username, passwordHash, email fields
     * - Add deviceAuthEnabled field
     * - Preserve existing user data (defaultCurrency, locale, etc.)
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create temporary table with new schema
            database.execSQL(
                """
                CREATE TABLE users_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    defaultCurrency TEXT NOT NULL,
                    locale TEXT NOT NULL,
                    deviceAuthEnabled INTEGER NOT NULL DEFAULT 0,
                    role TEXT NOT NULL,
                    status TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """
            )
            
            // Migrate existing user data, preserving preferences
            database.execSQL(
                """
                INSERT INTO users_new (
                    id, defaultCurrency, locale, deviceAuthEnabled, 
                    role, status, createdAt, updatedAt
                )
                SELECT 
                    id,
                    COALESCE(defaultCurrency, 'USD') as defaultCurrency,
                    COALESCE(locale, 'en') as locale,
                    0 as deviceAuthEnabled,
                    COALESCE(role, 'USER') as role,
                    COALESCE(status, 'ACTIVE') as status,
                    COALESCE(createdAt, strftime('%s', 'now') * 1000) as createdAt,
                    strftime('%s', 'now') * 1000 as updatedAt
                FROM users
                """
            )
            
            // Drop old table and rename new table
            database.execSQL("DROP TABLE users")
            database.execSQL("ALTER TABLE users_new RENAME TO users")
        }
    }
}
