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
}
