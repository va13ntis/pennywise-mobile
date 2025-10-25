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
     * - bank_cards table
     * - split_payment_installments table
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create bank_cards table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS bank_cards (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    alias TEXT NOT NULL,
                    lastFourDigits TEXT NOT NULL,
                    paymentDay INTEGER NOT NULL,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Create split_payment_installments table with foreign key to transactions
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS split_payment_installments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    parentTransactionId INTEGER NOT NULL,
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
                    FOREIGN KEY(parentTransactionId) REFERENCES transactions(id) ON DELETE CASCADE
                )
            """.trimIndent())
            
            // Create indices for split_payment_installments
            db.execSQL("CREATE INDEX IF NOT EXISTS index_split_payment_installments_parentTransactionId ON split_payment_installments(parentTransactionId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_split_payment_installments_dueDate ON split_payment_installments(dueDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_split_payment_installments_isPaid ON split_payment_installments(isPaid)")
        }
    }
    
    /**
     * Migration from version 3 to 4
     * Adds:
     * - payment_method_configs table
     * - paymentMethodConfigId column to transactions table (nullable)
     * - installments and installmentAmount columns to transactions table (nullable)
     */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Create payment_method_configs table
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS payment_method_configs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    paymentMethod TEXT NOT NULL,
                    alias TEXT NOT NULL,
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    withdrawDay INTEGER,
                    isActive INTEGER NOT NULL DEFAULT 1,
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
            """.trimIndent())
            
            // Add new nullable columns to transactions table
            // These are safe to add as ALTER TABLE ADD COLUMN since they're nullable
            db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethodConfigId INTEGER")
            db.execSQL("ALTER TABLE transactions ADD COLUMN installments INTEGER")
            db.execSQL("ALTER TABLE transactions ADD COLUMN installmentAmount REAL")
        }
    }
    
    /**
     * Combined migration from version 2 to 4 (for users who never installed version 3)
     * This performs all schema changes at once
     */
    val MIGRATION_2_4 = object : Migration(2, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Execute all changes from both migrations
            MIGRATION_2_3.migrate(db)
            MIGRATION_3_4.migrate(db)
        }
    }
    
    /**
     * Get all available migrations
     */
    fun getAllMigrations(): Array<Migration> {
        return arrayOf(
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_2_4
        )
    }
}

