package com.pennywise.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for PennyWise app
 * These migrations preserve user data across schema changes
 */
object DatabaseMigrations {
    
    /**
     * Migration from version 2 to 3
     * Adds:
     * - paymentMethodConfigId column to transactions table (for payment method configuration support)
     * - Creates payment_method_configs table
     * - Creates bank_cards table
     * - Creates split_payment_installments table
     * - Creates currency_usage table
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add paymentMethodConfigId column to transactions table
            db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethodConfigId INTEGER")
            
            // Create payment_method_configs table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS payment_method_configs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    alias TEXT NOT NULL,
                    isDefault INTEGER NOT NULL,
                    withdrawDay INTEGER,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
            
            // Create bank_cards table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS bank_cards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    alias TEXT NOT NULL,
                    lastFourDigits TEXT NOT NULL,
                    paymentDay INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
            
            // Create split_payment_installments table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS split_payment_installments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    parentTransactionId INTEGER NOT NULL,
                    amount REAL NOT NULL,
                    currency TEXT NOT NULL,
                    description TEXT NOT NULL,
                    category TEXT NOT NULL,
                    type TEXT NOT NULL,
                    dueDate INTEGER NOT NULL,
                    installmentNumber INTEGER NOT NULL,
                    totalInstallments INTEGER NOT NULL,
                    isPaid INTEGER NOT NULL,
                    paidDate INTEGER,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    FOREIGN KEY(parentTransactionId) REFERENCES transactions(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """)
            
            // Create currency_usage table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS currency_usage (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    currency TEXT NOT NULL,
                    usageCount INTEGER NOT NULL,
                    lastUsed INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """)
            
            // Create indices for performance
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_currency_usage_currency ON currency_usage (currency)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_split_payment_installments_parentTransactionId ON split_payment_installments (parentTransactionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_split_payment_installments_dueDate ON split_payment_installments (dueDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_split_payment_installments_isPaid ON split_payment_installments (isPaid)")
        }
    }
    
    /**
     * Migration from version 3 to 4
     * This migration handles any schema refinements between versions 3 and 4
     * Based on the schema file, version 4 appears to be the same as version 3
     * but we include this migration for completeness and future-proofing
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Version 3 to 4 appears to be schema refinements
            // No specific changes needed based on the schema file
            // This migration exists to maintain the upgrade path
        }
    }

    /**
     * Direct migration from version 2 to 4.
     * Used by tests that validate skipping version 3.
     */
    val MIGRATION_2_4 = object : Migration(2, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            MIGRATION_2_3.migrate(db)
            MIGRATION_3_4.migrate(db)
        }
    }
    
    /**
     * Migration from version 4 to 5
     * Adds:
     * - billingDelayDays column to transactions table (for credit card billing delay support)
     */
    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Add billingDelayDays column with default value of 0 (immediate/שוטף)
            db.execSQL("ALTER TABLE transactions ADD COLUMN billingDelayDays INTEGER NOT NULL DEFAULT 0")
        }
    }
    
    /**
     * Get all available migrations
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5
        )
    }
}

