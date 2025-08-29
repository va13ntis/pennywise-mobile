package com.pennywise.app.data.util

import com.pennywise.app.data.local.dao.TransactionDao
import com.pennywise.app.data.local.dao.UserDao
import com.pennywise.app.data.local.entity.TransactionEntity
import com.pennywise.app.data.local.entity.UserEntity
import com.pennywise.app.domain.model.RecurringPeriod
import com.pennywise.app.domain.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for seeding the database with test data
 */
@Singleton
class DataSeeder @Inject constructor(
    private val userDao: UserDao,
    private val transactionDao: TransactionDao,
    private val passwordHasher: PasswordHasher
) {
    
    /**
     * Seed the database with test data
     */
    suspend fun seedTestData() = withContext(Dispatchers.IO) {
        try {
            println("🔄 Starting to seed test data...")
            
            // Create a test user
            val testUser = createTestUser()
            println("✅ Test user created: ${testUser.username} (${testUser.email})")
            
            // Create sample transactions for the current month
            createSampleTransactions(testUser.id)
            println("✅ Sample transactions created")
            
            println("✅ Test data seeded successfully!")
            println("📝 Login credentials:")
            println("   Email: ${testUser.email}")
            println("   Password: test123")
        } catch (e: Exception) {
            println("❌ Failed to seed test data: ${e.message}")
            e.printStackTrace()
            throw e // Re-throw to let the ViewModel handle it
        }
    }
    
    /**
     * Create a test user
     */
    private suspend fun createTestUser(): UserEntity {
        val hashedPassword = passwordHasher.hashPassword("test123")
        
        // Check if user already exists by email
        val existingUserByEmail = userDao.getUserByEmail("test@pennywise.com")
        if (existingUserByEmail != null) {
            println("⚠️ Test user already exists by email with ID: ${existingUserByEmail.id}")
            return existingUserByEmail
        }
        
        // Check if user already exists by username
        val existingUserByUsername = userDao.getUserByUsername("testuser")
        if (existingUserByUsername != null) {
            println("⚠️ Test user already exists by username with ID: ${existingUserByUsername.id}")
            return existingUserByUsername
        }
        
        val testUser = UserEntity(
            id = 0, // Let Room auto-generate the ID
            email = "test@pennywise.com",
            passwordHash = hashedPassword,
            username = "testuser",
            createdAt = Date()
        )
        
        val userId = userDao.insertUser(testUser)
        println("✅ Test user created with ID: $userId")
        
        // Verify the user was created correctly
        val createdUser = userDao.getUserByUsername("testuser")
        println("🔍 Verification - Created user: ${createdUser?.username} (ID: ${createdUser?.id})")
        
        // Return the created user with the correct ID, not the original testUser
        return createdUser ?: testUser.copy(id = userId)
    }
    
    /**
     * Create sample transactions for testing
     */
    private suspend fun createSampleTransactions(userId: Long) {
        println("🔄 Creating sample transactions for user ID: $userId")
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val transactions = mutableListOf<TransactionEntity>()
        
        // Income transactions
        val incomeTransactions = createIncomeTransactions(userId, currentMonth, currentYear)
        transactions.addAll(incomeTransactions)
        println("✅ Created ${incomeTransactions.size} income transactions")
        
        // Regular expense transactions
        val regularExpenses = createRegularExpenses(userId, currentMonth, currentYear)
        transactions.addAll(regularExpenses)
        println("✅ Created ${regularExpenses.size} regular expense transactions")
        
        // Recurring expense transactions
        val recurringExpenses = createRecurringExpenses(userId, currentMonth, currentYear)
        transactions.addAll(recurringExpenses)
        println("✅ Created ${recurringExpenses.size} recurring expense transactions")
        
        // Insert all transactions
        transactionDao.insertTransactions(transactions)
        println("✅ Inserted ${transactions.size} total transactions for user ID: $userId")
        
        // Verify transactions were created
        val transactionCount = transactionDao.getTransactionCount(userId)
        println("🔍 Verification - Transactions for user $userId: $transactionCount")
        
        // Also verify by type
        val incomeCount = transactionDao.getTransactionsByType(userId, TransactionType.INCOME).first().size
        val expenseCount = transactionDao.getTransactionsByType(userId, TransactionType.EXPENSE).first().size
        val recurringCount = transactionDao.getRecurringTransactions(userId).first().size
        println("🔍 Verification - Income: $incomeCount, Expenses: $expenseCount, Recurring: $recurringCount")
    }
    
    /**
     * Create sample income transactions
     */
    private fun createIncomeTransactions(userId: Long, month: Int, year: Int): List<TransactionEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        return listOf(
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = 3500.0,
                description = "Salary",
                category = "Income",
                type = TransactionType.INCOME,
                date = calendar.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = 500.0,
                description = "Freelance Project",
                category = "Income",
                type = TransactionType.INCOME,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 15) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            )
        )
    }
    
    /**
     * Create sample regular expense transactions
     */
    private fun createRegularExpenses(userId: Long, month: Int, year: Int): List<TransactionEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        return listOf(
            // Week 1 expenses
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -120.0,
                description = "Grocery Shopping",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 2) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -45.0,
                description = "Gas Station",
                category = "Transportation",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 3) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            
            // Week 2 expenses
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -85.0,
                description = "Restaurant Dinner",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 8) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -200.0,
                description = "Shopping Mall",
                category = "Shopping",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 10) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            
            // Week 3 expenses
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -60.0,
                description = "Movie Tickets",
                category = "Entertainment",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 16) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -95.0,
                description = "Gym Membership",
                category = "Health",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 18) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            
            // Week 4 expenses
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -150.0,
                description = "Home Supplies",
                category = "Home",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 22) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -75.0,
                description = "Coffee Shop",
                category = "Food",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 25) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            )
        )
    }
    
    /**
     * Create sample recurring expense transactions
     */
    private fun createRecurringExpenses(userId: Long, month: Int, year: Int): List<TransactionEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        return listOf(
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -1200.0,
                description = "Rent Payment",
                category = "Housing",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -150.0,
                description = "Electricity Bill",
                category = "Utilities",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 5) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -80.0,
                description = "Internet Bill",
                category = "Utilities",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 7) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0, // Let Room auto-generate the ID
                userId = userId,
                amount = -25.0,
                description = "Netflix Subscription",
                category = "Entertainment",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 10) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = Date()
            )
        )
    }
    
    /**
     * Clear all test data
     */
    suspend fun clearTestData() = withContext(Dispatchers.IO) {
        try {
            transactionDao.deleteAllTransactions()
            userDao.deleteAllUsers()
            println("✅ Test data cleared successfully!")
        } catch (e: Exception) {
            println("❌ Failed to clear test data: ${e.message}")
            e.printStackTrace()
        }
    }
}
