package com.pennywise.app.performance

import android.Manifest
import android.os.Build
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.room.Room
import com.pennywise.app.data.local.PennyWiseDatabase
import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.dao.CurrencyUsageDao
import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.CurrencyUsageEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.junit.After
import java.util.Date
import java.util.Calendar

/**
 * Performance tests for database operations
 * Tests the performance of database queries and operations with different scenarios:
 * - Single record operations
 * - Batch operations
 * - Complex queries
 * - Large dataset operations
 * - Concurrent operations
 * 
 */
@RunWith(AndroidJUnit4::class)
class DatabasePerformanceTest {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var database: PennyWiseDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var currencyUsageDao: CurrencyUsageDao
    private lateinit var userDao: UserDao
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    init {
        // Configure benchmark output to internal storage before any tests run
        // This must be done in init block to run before BenchmarkRule tries to grant permissions
        System.setProperty("benchmark.output.path", 
            InstrumentationRegistry.getInstrumentation().targetContext.filesDir.absolutePath)
    }

    @Before
    fun setup() {
        // Grant permissions for CI environment
        grantPermissions()
        
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            PennyWiseDatabase::class.java
        ).build()
        
        transactionDao = database.transactionDao()
        currencyUsageDao = database.currencyUsageDao()
        userDao = database.userDao()
        
        // Setup test data
        setupTestData()
    }
    
    /**
     * Grant runtime permissions for CI environment
     * This prevents "Failed to grant permissions" errors on emulator
     */
    private fun grantPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val uiAutomation = instrumentation.uiAutomation
            
            // Grant all permissions declared in the manifest
            val permissions = listOf(
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            
            // Grant all permissions using UiAutomation
            permissions.forEach { permission ->
                try {
                    uiAutomation.grantRuntimePermission(
                        context.packageName,
                        permission
                    )
                } catch (e: Exception) {
                    // Permission may not be declared in manifest or already granted
                    // This is expected and safe to ignore
                }
            }
        }
    }

    @After
    fun cleanup() {
        database.close()
    }

    private fun setupTestData() {
        runBlocking {
            // Create test user
            val testUser = UserEntity(
                defaultCurrency = "USD",
                locale = "en",
                deviceAuthEnabled = false,
                createdAt = Date(),
                updatedAt = Date()
            )
            userDao.insertUser(testUser)
            
            // Create test currency usage data
            val currencies = listOf("USD", "EUR", "GBP", "JPY", "CAD")
            val now = Date()
            currencies.forEachIndexed { index, currency ->
                val currencyUsage = CurrencyUsageEntity(
                    currency = currency,
                    usageCount = (100 - index * 10),
                    lastUsed = now,
                    createdAt = now,
                    updatedAt = now
                )
                currencyUsageDao.insertCurrencyUsage(currencyUsage)
            }
            
            // Create test transactions
            val calendar = Calendar.getInstance()
            repeat(100) { index ->
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_MONTH, -index)
                
                val transaction = TransactionEntity(
                    amount = (100.0 + index * 10),
                    description = "Test transaction $index",
                    category = "Test Category",
                    type = if (index % 2 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
                    date = calendar.time,
                    currency = currencies[index % currencies.size],
                    isRecurring = index % 5 == 0,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                transactionDao.insertTransaction(transaction)
            }
        }
    }

    /**
     * Benchmark: Single transaction insertion
     * This tests the performance of inserting a single transaction
     */
    @Test
    fun benchmarkSingleTransactionInsertion() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val transaction = TransactionEntity(
                    amount = 100.0,
                    description = "Benchmark transaction",
                    category = "Benchmark",
                    type = TransactionType.EXPENSE,
                    date = Date(),
                    currency = "USD",
                    isRecurring = false,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                val id = transactionDao.insertTransaction(transaction)
                assert(id > 0)
            }
        }
    }

    /**
     * Benchmark: Batch transaction insertion
     * This tests the performance of inserting multiple transactions
     */
    @Test
    fun benchmarkBatchTransactionInsertion() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val transactions = (1..10).map { index ->
                    TransactionEntity(
                        amount = 100.0 + index,
                        description = "Batch transaction $index",
                        category = "Batch",
                        type = TransactionType.EXPENSE,
                        date = Date(),
                        currency = "USD",
                        isRecurring = false,
                        createdAt = Date(),
                        updatedAt = Date()
                    )
                }
                
                transactions.forEach { transaction ->
                    val id = transactionDao.insertTransaction(transaction)
                    assert(id > 0)
                }
            }
        }
    }

    /**
     * Benchmark: Transaction query (all transactions)
     * This tests the performance of querying all transactions
     */
    @Test
    fun benchmarkAllTransactionsQuery() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val transactions = transactionDao.getAllTransactions().first()
                assert(transactions.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Transaction query by date range
     * This tests the performance of querying transactions by date range
     */
    @Test
    fun benchmarkTransactionQueryByDateRange() {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.MONTH, -1)
        val startDate = calendar.time

        benchmarkRule.measureRepeated {
            runBlocking {
                val transactions = transactionDao.getTransactionsByDateRange(startDate, endDate).first()
                assert(transactions.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Transaction query by category
     * This tests the performance of querying transactions by category
     */
    @Test
    fun benchmarkTransactionQueryByCategory() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val transactions = transactionDao.getTransactionsByCategory("Test Category").first()
                assert(transactions.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Transaction query by type
     * This tests the performance of querying transactions by type
     */
    @Test
    fun benchmarkTransactionQueryByType() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val transactions = transactionDao.getTransactionsByType(TransactionType.EXPENSE).first()
                assert(transactions.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Total calculation by type and date range
     * This tests the performance of calculating totals
     */
    @Test
    fun benchmarkTotalCalculationByTypeAndDateRange() {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.MONTH, -1)
        val startDate = calendar.time

        benchmarkRule.measureRepeated {
            runBlocking {
                val total = transactionDao.getTotalByTypeAndDateRange(TransactionType.EXPENSE, startDate, endDate)
                assert(total >= 0.0)
            }
        }
    }

    /**
     * Benchmark: Currency usage tracking
     * This tests the performance of currency usage operations
     */
    @Test
    fun benchmarkCurrencyUsageTracking() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val now = Date()
                currencyUsageDao.insertOrIncrementCurrencyUsage("USD", now, now, now)
                val usage = currencyUsageDao.getAllCurrencyUsage().first()
                assert(usage.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: All currency usage query
     * This tests the performance of querying all currency usage
     */
    @Test
    fun benchmarkAllCurrencyUsageQuery() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val usage = currencyUsageDao.getAllCurrencyUsage().first()
                assert(usage.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Top currencies query
     * This tests the performance of querying top currencies
     */
    @Test
    fun benchmarkTopCurrenciesQuery() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val topCurrencies = currencyUsageDao.getTopCurrencies(5).first()
                assert(topCurrencies.size <= 5)
            }
        }
    }

    /**
     * Benchmark: User query by ID
     * This tests the performance of querying user by ID
     */
    @Test
    fun benchmarkUserQueryById() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val user = userDao.getUser()
                assert(user != null)
            }
        }
    }

    /**
     * Benchmark: Complex query with multiple operations
     * This tests the performance of complex queries involving multiple tables
     */
    @Test
    fun benchmarkComplexMultipleOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // This simulates a complex query with multiple operations
                val user = userDao.getUser()
                val transactions = transactionDao.getAllTransactions().first()
                val currencyUsage = currencyUsageDao.getAllCurrencyUsage().first()
                
                assert(user != null)
                assert(transactions.isNotEmpty())
                assert(currencyUsage.isNotEmpty())
            }
        }
    }

    /**
     * Benchmark: Concurrent database operations
     * This tests the performance under concurrent load simulation
     */
    @Test
    fun benchmarkConcurrentDatabaseOperations() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val operations = listOf(
                    async { transactionDao.getAllTransactions().first() },
                    async { currencyUsageDao.getAllCurrencyUsage().first() },
                    async { userDao.getUser() },
                    async { transactionDao.getTransactionCount() },
                    async { currencyUsageDao.getTopCurrencies(3).first() }
                )
                
                val results = operations.map { it.await() }
                assert(results.size == 5)
            }
        }
    }

    /**
     * Benchmark: Large dataset query performance
     * This tests the performance with a larger dataset
     */
    @Test
    fun benchmarkLargeDatasetQueryPerformance() {
        // Create additional test data
        runBlocking {
            val now = Date()
            repeat(1000) { index ->
                val transaction = TransactionEntity(
                    amount = 50.0 + index,
                    description = "Large dataset transaction $index",
                    category = "Large Dataset",
                    type = TransactionType.EXPENSE,
                    date = now,
                    currency = "USD",
                    isRecurring = false,
                    createdAt = now,
                    updatedAt = now
                )
                transactionDao.insertTransaction(transaction)
            }
        }

        benchmarkRule.measureRepeated {
            runBlocking {
                val transactions = transactionDao.getAllTransactions().first()
                assert(transactions.size >= 1000)
            }
        }
    }

    /**
     * Benchmark: Database transaction performance
     * This tests the performance of database transactions
     */
    @Test
    fun benchmarkDatabaseTransactionPerformance() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val now = Date()
                // Simulate a database transaction by performing multiple operations
                val transaction = TransactionEntity(
                    amount = 200.0,
                    description = "Transaction test",
                    category = "Transaction",
                    type = TransactionType.EXPENSE,
                    date = now,
                    currency = "USD",
                    isRecurring = false,
                    createdAt = now,
                    updatedAt = now
                )
                
                val id = transactionDao.insertTransaction(transaction)
                currencyUsageDao.insertOrIncrementCurrencyUsage("USD", now, now, now)
                val transactionCount = transactionDao.getTransactionCount()
                
                assert(id > 0)
                assert(transactionCount > 0)
            }
        }
    }
}
