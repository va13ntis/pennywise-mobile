package com.pennywise.app.data.local.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Database migrations for PennyWise app
 * These migrations preserve user data across schema changes
 */
object DatabaseMigrations {
    
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
            MIGRATION_4_5
        )
    }
}

