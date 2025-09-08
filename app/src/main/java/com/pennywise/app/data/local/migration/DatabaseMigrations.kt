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
}
