package com.pennywise.app.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.pennywise.app.data.local.converter.DateConverter
import com.pennywise.app.data.local.converter.TransactionTypeConverter
import com.pennywise.app.data.local.converter.RecurringPeriodConverter
import com.pennywise.app.data.local.converter.UserRoleConverter
import com.pennywise.app.data.local.converter.UserStatusConverter
import com.pennywise.app.data.local.converter.PaymentMethodConverter
import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.dao.BankCardDao
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.BankCardEntity
import com.pennywise.app.data.local.migration.DatabaseMigrations

/**
 * Room database for PennyWise app
 */
@Database(
    entities = [UserEntity::class, TransactionEntity::class, CurrencyUsageEntity::class, BankCardEntity::class],
    version = 5,
    exportSchema = false
)
@TypeConverters(
    DateConverter::class,
    TransactionTypeConverter::class,
    RecurringPeriodConverter::class,
    UserRoleConverter::class,
    UserStatusConverter::class,
    PaymentMethodConverter::class
)
abstract class PennyWiseDatabase : RoomDatabase() {
    
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun currencyUsageDao(): CurrencyUsageDao
    abstract fun bankCardDao(): BankCardDao
    
    companion object {
        @Volatile
        private var INSTANCE: PennyWiseDatabase? = null
        
        fun getDatabase(context: Context): PennyWiseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PennyWiseDatabase::class.java,
                    "pennywise_database"
                )
                .addMigrations(DatabaseMigrations.MIGRATION_1_2, DatabaseMigrations.MIGRATION_2_3, DatabaseMigrations.MIGRATION_3_4, DatabaseMigrations.MIGRATION_4_5)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Database created successfully
                    }
                    
                    override fun onOpen(db: SupportSQLiteDatabase) {
                        super.onOpen(db)
                        // Database opened successfully
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

