package com.pennywise.app.data.local.migration

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import androidx.test.platform.app.InstrumentationRegistry
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.data.local.migration.DatabaseMigrations.MIGRATION_1_2
import com.pennywise.app.domain.model.Currency
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Integration tests for database migrations
 * Tests migration from version 1 to version 2 (adding currency support)
 */
@RunWith(AndroidJUnit4::class)
class CurrencyMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PennyWiseDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Database has schema version 1. Insert some data using SQL queries.
            // You can't use DAO classes because they expect the latest schema.
            execSQL("INSERT INTO users (id, email, password_hash, created_at, updated_at) VALUES (1, 'test@example.com', 'hash', 1234567890, 1234567890)")
            execSQL("INSERT INTO transactions (id, user_id, amount, description, category, date, created_at, updated_at) VALUES (1, 1, 100.0, 'Test transaction', 'Food', 1234567890, 1234567890, 1234567890)")
            
            // Prepare for the next version.
            close()
        }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you can also validate that the data was preserved.
        val cursor = db.query("SELECT * FROM users WHERE id = 1")
        assert(cursor.moveToFirst())
        cursor.close()

        val transactionCursor = db.query("SELECT * FROM transactions WHERE id = 1")
        assert(transactionCursor.moveToFirst())
        transactionCursor.close()

        // Verify new currency_usage table exists
        val currencyUsageCursor = db.query("SELECT name FROM sqlite_master WHERE type='table' AND name='currency_usage'")
        assert(currencyUsageCursor.moveToFirst())
        currencyUsageCursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2WithCurrencyData() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert test data
            execSQL("INSERT INTO users (id, email, password_hash, created_at, updated_at) VALUES (1, 'test@example.com', 'hash', 1234567890, 1234567890)")
            execSQL("INSERT INTO transactions (id, user_id, amount, description, category, date, created_at, updated_at) VALUES (1, 1, 100.0, 'Test transaction', 'Food', 1234567890, 1234567890, 1234567890)")
            close()
        }

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Insert currency usage data to test the new table
        db.execSQL("INSERT INTO currency_usage (userId, currency, usageCount, lastUsed, createdAt, updatedAt) VALUES (1, 'USD', 5, 1234567890, 1234567890, 1234567890)")
        db.execSQL("INSERT INTO currency_usage (userId, currency, usageCount, lastUsed, createdAt, updatedAt) VALUES (1, 'EUR', 3, 1234567890, 1234567890, 1234567890)")

        // Verify currency usage data
        val cursor = db.query("SELECT * FROM currency_usage WHERE userId = 1")
        var count = 0
        while (cursor.moveToNext()) {
            count++
            val currencyCode = cursor.getString(cursor.getColumnIndexOrThrow("currency"))
            val usageCount = cursor.getInt(cursor.getColumnIndexOrThrow("usageCount"))
            assert(currencyCode in listOf("USD", "EUR"))
            assert(usageCount > 0)
        }
        assert(count == 2)
        cursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2PreservesData() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert comprehensive test data
            execSQL("INSERT INTO users (id, email, password_hash, created_at, updated_at) VALUES (1, 'user1@example.com', 'hash1', 1234567890, 1234567890)")
            execSQL("INSERT INTO users (id, email, password_hash, created_at, updated_at) VALUES (2, 'user2@example.com', 'hash2', 1234567890, 1234567890)")
            
            execSQL("INSERT INTO transactions (id, user_id, amount, description, category, date, created_at, updated_at) VALUES (1, 1, 100.0, 'Transaction 1', 'Food', 1234567890, 1234567890, 1234567890)")
            execSQL("INSERT INTO transactions (id, user_id, amount, description, category, date, created_at, updated_at) VALUES (2, 1, 200.0, 'Transaction 2', 'Transport', 1234567890, 1234567890, 1234567890)")
            execSQL("INSERT INTO transactions (id, user_id, amount, description, category, date, created_at, updated_at) VALUES (3, 2, 300.0, 'Transaction 3', 'Entertainment', 1234567890, 1234567890, 1234567890)")
            
            close()
        }

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Verify all users are preserved
        val userCursor = db.query("SELECT COUNT(*) FROM users")
        userCursor.moveToFirst()
        val userCount = userCursor.getInt(0)
        assert(userCount == 2)
        userCursor.close()

        // Verify all transactions are preserved
        val transactionCursor = db.query("SELECT COUNT(*) FROM transactions")
        transactionCursor.moveToFirst()
        val transactionCount = transactionCursor.getInt(0)
        assert(transactionCount == 3)
        transactionCursor.close()

        // Verify specific user data
        val user1Cursor = db.query("SELECT * FROM users WHERE id = 1")
        user1Cursor.moveToFirst()
        assert(user1Cursor.getString(user1Cursor.getColumnIndexOrThrow("email")) == "user1@example.com")
        user1Cursor.close()

        // Verify specific transaction data
        val transaction1Cursor = db.query("SELECT * FROM transactions WHERE id = 1")
        transaction1Cursor.moveToFirst()
        assert(transaction1Cursor.getDouble(transaction1Cursor.getColumnIndexOrThrow("amount")) == 100.0)
        assert(transaction1Cursor.getString(transaction1Cursor.getColumnIndexOrThrow("description")) == "Transaction 1")
        transaction1Cursor.close()

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2CreatesCurrencyUsageTable() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Verify currency_usage table structure
        val cursor = db.query("PRAGMA table_info(currency_usage)")
        val columns = mutableListOf<String>()
        while (cursor.moveToNext()) {
            columns.add(cursor.getString(cursor.getColumnIndexOrThrow("name")))
        }
        cursor.close()

        // Verify all expected columns exist
        assert(columns.contains("id"))
        assert(columns.contains("userId"))
        assert(columns.contains("currency"))
        assert(columns.contains("usageCount"))
        assert(columns.contains("lastUsed"))
        assert(columns.contains("createdAt"))
        assert(columns.contains("updatedAt"))

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2WithConstraints() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Test foreign key constraint
        db.execSQL("INSERT INTO users (id, email, password_hash, created_at, updated_at) VALUES (1, 'test@example.com', 'hash', 1234567890, 1234567890)")
        
        // This should succeed
        db.execSQL("INSERT INTO currency_usage (userId, currency, usageCount, lastUsed, createdAt, updatedAt) VALUES (1, 'USD', 1, 1234567890, 1234567890, 1234567890)")
        
        // This should fail due to foreign key constraint
        try {
            db.execSQL("INSERT INTO currency_usage (userId, currency, usageCount, lastUsed, createdAt, updatedAt) VALUES (999, 'USD', 1, 1234567890, 1234567890, 1234567890)")
            assert(false) // Should not reach here
        } catch (e: Exception) {
            // Expected to fail due to foreign key constraint
            assert(true)
        }

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2WithIndexes() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            close()
        }

        // Run migration
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)

        // Verify indexes exist
        val indexCursor = db.query("SELECT name FROM sqlite_master WHERE type='index' AND name LIKE '%currency_usage%'")
        val indexes = mutableListOf<String>()
        while (indexCursor.moveToNext()) {
            indexes.add(indexCursor.getString(0))
        }
        indexCursor.close()

        // Verify expected indexes exist
        assert(indexes.any { it.contains("index_currency_usage_userId") })
        assert(indexes.any { it.contains("index_currency_usage_userId_currency") })

        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrate1To2Performance() {
        var db = helper.createDatabase(TEST_DB, 1).apply {
            // Insert large amount of test data
            execSQL("INSERT INTO users (id, email, password_hash, created_at, updated_at) VALUES (1, 'test@example.com', 'hash', 1234567890, 1234567890)")
            
            // Insert 1000 transactions
            for (i in 1..1000) {
                execSQL("INSERT INTO transactions (id, user_id, amount, description, category, date, created_at, updated_at) VALUES ($i, 1, ${i * 10.0}, 'Transaction $i', 'Category $i', 1234567890, 1234567890, 1234567890)")
            }
            
            close()
        }

        // Measure migration time
        val startTime = System.currentTimeMillis()
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        val endTime = System.currentTimeMillis()

        // Verify migration completed within reasonable time (less than 5 seconds)
        val duration = endTime - startTime
        assert(duration < 5000) { "Migration took too long: ${duration}ms" }

        // Verify all data is preserved
        val transactionCursor = db.query("SELECT COUNT(*) FROM transactions")
        transactionCursor.moveToFirst()
        val transactionCount = transactionCursor.getInt(0)
        assert(transactionCount == 1000)
        transactionCursor.close()

        db.close()
    }
}
