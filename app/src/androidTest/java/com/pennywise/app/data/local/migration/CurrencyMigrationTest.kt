package com.pennywise.app.data.local.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.data.local.PennyWiseDatabase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull

/**
 * Tests for database migrations to ensure data is preserved during schema changes
 */
@RunWith(AndroidJUnit4::class)
class CurrencyMigrationTest {
    
    private val TEST_DB_NAME = "migration-test"
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PennyWiseDatabase::class.java,
        listOf(),
        FrameworkSQLiteOpenHelperFactory()
    )
    
    /**
     * Test migration from version 2 to 3
     * Verifies that:
     * - Existing data is preserved
     * - New tables are created properly
     */
    @Test
    @Throws(IOException::class)
    fun migrate2To3_preservesData() {
        // Create database at version 2 and insert sample data
        helper.createDatabase(TEST_DB_NAME, 2).apply {
            execSQL("""
                INSERT INTO transactions (id, amount, currency, description, category, type, date, isRecurring, paymentMethod, createdAt, updatedAt)
                VALUES (1, 100.0, 'USD', 'Test Transaction', 'Food', 'EXPENSE', 1234567890000, 0, 'CASH', 1234567890000, 1234567890000)
            """)
            close()
        }
        
        // Run migration to version 3
        helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, DatabaseMigrations.MIGRATION_2_3).apply {
            // Verify original data is preserved
            query("SELECT * FROM transactions WHERE id = 1").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(100.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.01)
                assertEquals("USD", cursor.getString(cursor.getColumnIndexOrThrow("currency")))
                assertEquals("Test Transaction", cursor.getString(cursor.getColumnIndexOrThrow("description")))
            }
            
            // Verify new tables exist
            query("SELECT name FROM sqlite_master WHERE type='table' AND name='bank_cards'").use { cursor ->
                assertEquals(1, cursor.count)
            }
            
            query("SELECT name FROM sqlite_master WHERE type='table' AND name='split_payment_installments'").use { cursor ->
                assertEquals(1, cursor.count)
            }
            
            close()
        }
    }
    
    /**
     * Test migration from version 3 to 4
     * Verifies that:
     * - Existing data is preserved
     * - New columns are added as nullable
     * - New payment_method_configs table is created
     */
    @Test
    @Throws(IOException::class)
    fun migrate3To4_preservesData() {
        // Create database at version 2, migrate to 3
        helper.createDatabase(TEST_DB_NAME, 2).apply {
            execSQL("""
                INSERT INTO transactions (id, amount, currency, description, category, type, date, isRecurring, paymentMethod, createdAt, updatedAt)
                VALUES (1, 100.0, 'USD', 'Test Transaction', 'Food', 'EXPENSE', 1234567890000, 0, 'CASH', 1234567890000, 1234567890000)
            """)
            close()
        }
        
        helper.runMigrationsAndValidate(TEST_DB_NAME, 3, true, DatabaseMigrations.MIGRATION_2_3)
        
        // Migrate to version 4
        helper.runMigrationsAndValidate(TEST_DB_NAME, 4, true, DatabaseMigrations.MIGRATION_3_4).apply {
            // Verify original data is preserved
            query("SELECT * FROM transactions WHERE id = 1").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(100.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.01)
                assertEquals("USD", cursor.getString(cursor.getColumnIndexOrThrow("currency")))
                
                // Verify new nullable columns exist and are null
                val configIdIndex = cursor.getColumnIndexOrThrow("paymentMethodConfigId")
                val installmentsIndex = cursor.getColumnIndexOrThrow("installments")
                val installmentAmountIndex = cursor.getColumnIndexOrThrow("installmentAmount")
                
                assertEquals(true, cursor.isNull(configIdIndex))
                assertEquals(true, cursor.isNull(installmentsIndex))
                assertEquals(true, cursor.isNull(installmentAmountIndex))
            }
            
            // Verify new table exists
            query("SELECT name FROM sqlite_master WHERE type='table' AND name='payment_method_configs'").use { cursor ->
                assertEquals(1, cursor.count)
            }
            
            close()
        }
    }
    
    /**
     * Test combined migration from version 2 to 4
     * Verifies that users who skip version 3 can migrate directly
     */
    @Test
    @Throws(IOException::class)
    fun migrate2To4_direct_preservesData() {
        // Create database at version 2 and insert sample data
        helper.createDatabase(TEST_DB_NAME, 2).apply {
            execSQL("""
                INSERT INTO transactions (id, amount, currency, description, category, type, date, isRecurring, paymentMethod, createdAt, updatedAt)
                VALUES (1, 100.0, 'USD', 'Test Transaction', 'Food', 'EXPENSE', 1234567890000, 0, 'CASH', 1234567890000, 1234567890000)
            """)
            execSQL("""
                INSERT INTO users (id, defaultCurrency, defaultPaymentMethod, locale, deviceAuthEnabled, role, status, createdAt, updatedAt)
                VALUES (1, 'USD', 'CASH', 'en', 0, 'USER', 'ACTIVE', 1234567890000, 1234567890000)
            """)
            close()
        }
        
        // Run direct migration to version 4
        helper.runMigrationsAndValidate(TEST_DB_NAME, 4, true, DatabaseMigrations.MIGRATION_2_4).apply {
            // Verify transaction data is preserved
            query("SELECT * FROM transactions WHERE id = 1").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals(100.0, cursor.getDouble(cursor.getColumnIndexOrThrow("amount")), 0.01)
                assertEquals("USD", cursor.getString(cursor.getColumnIndexOrThrow("currency")))
                assertEquals("Test Transaction", cursor.getString(cursor.getColumnIndexOrThrow("description")))
                
                // Verify new columns exist and are null
                assertEquals(true, cursor.isNull(cursor.getColumnIndexOrThrow("paymentMethodConfigId")))
                assertEquals(true, cursor.isNull(cursor.getColumnIndexOrThrow("installments")))
            }
            
            // Verify user data is preserved
            query("SELECT * FROM users WHERE id = 1").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("USD", cursor.getString(cursor.getColumnIndexOrThrow("defaultCurrency")))
            }
            
            // Verify all new tables exist
            query("SELECT name FROM sqlite_master WHERE type='table' AND name='bank_cards'").use { cursor ->
                assertEquals(1, cursor.count)
            }
            query("SELECT name FROM sqlite_master WHERE type='table' AND name='split_payment_installments'").use { cursor ->
                assertEquals(1, cursor.count)
            }
            query("SELECT name FROM sqlite_master WHERE type='table' AND name='payment_method_configs'").use { cursor ->
                assertEquals(1, cursor.count)
            }
            
            close()
        }
    }
    
    /**
     * Test that all migrations can be applied in sequence from version 2 to 4
     */
    @Test
    @Throws(IOException::class)
    fun migrateAll_preservesData() {
        // Create database at version 2
        helper.createDatabase(TEST_DB_NAME, 2).apply {
            execSQL("""
                INSERT INTO transactions (id, amount, currency, description, category, type, date, isRecurring, paymentMethod, createdAt, updatedAt)
                VALUES (1, 50.0, 'EUR', 'Migration Test', 'Transport', 'EXPENSE', 1234567890000, 0, 'CREDIT_CARD', 1234567890000, 1234567890000)
            """)
            close()
        }
        
        // Migrate through all versions
        val db = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            4,
            true,
            DatabaseMigrations.MIGRATION_2_3,
            DatabaseMigrations.MIGRATION_3_4
        )
        
        // Open database with Room to verify schema is correct
        Room.databaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PennyWiseDatabase::class.java,
            TEST_DB_NAME
        ).addMigrations(*DatabaseMigrations.getAllMigrations())
            .build().apply {
                openHelper.writableDatabase.close()
            }
        
        db.close()
    }
}