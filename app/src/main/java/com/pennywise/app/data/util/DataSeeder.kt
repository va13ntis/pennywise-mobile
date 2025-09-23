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
    private val transactionDao: TransactionDao
) {
    
    /**
     * Seed the database with test data
     */
    suspend fun seedTestData() = withContext(Dispatchers.IO) {
        try {
            println("🔄 Starting to seed test data...")
            
            // Create a test user
            val testUser = createTestUser()
            println("✅ Test user created (ID: ${testUser.id})")
            
            // Create sample transactions for the current month
            createSampleTransactions(testUser.id)
            println("✅ Sample transactions for current month created")
            
            // Create historical data for previous months (6 months of history)
            createHistoricalTransactions(testUser.id, 6)
            println("✅ Historical transactions created")
            
            // Create future data for next months (2 months ahead for testing navigation)
            createFutureTransactions(testUser.id, 2)
            println("✅ Future transactions created")
            
            println("✅ Test data seeded successfully!")
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
        // Check if user already exists
        val existingUser = userDao.getSingleUser()
        if (existingUser != null) {
            println("⚠️ Test user already exists with ID: ${existingUser.id}")
            return existingUser
        }
        
        val testUser = UserEntity(
            id = 0, // Let Room auto-generate the ID
            defaultCurrency = "USD",
            locale = "en",
            deviceAuthEnabled = false,
            createdAt = Date()
        )
        
        println("🔄 Creating test user with default settings")
        val userId = userDao.insertUser(testUser)
        println("✅ Test user created with ID: $userId")
        
        // Verify the user was created correctly
        val createdUser = userDao.getUserById(userId)
        println("🔍 Verification - Created user (ID: ${createdUser?.id})")
        
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
                description = "Salary - ${getMonthName(month)} ${year}",
                category = "Salary",
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
                category = "Freelance",
                type = TransactionType.INCOME,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 15) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = 250.0,
                description = "Investment Dividend",
                category = "Investment",
                type = TransactionType.INCOME,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 20) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = 100.0,
                description = "Cash Gift",
                category = "Gift",
                type = TransactionType.INCOME,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 25) }.time,
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
                category = "Groceries",
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
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -35.0,
                description = "Pharmacy",
                category = "Healthcare",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 4) }.time,
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
                category = "Dining Out",
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
                category = "Clothing",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 10) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -20.0,
                description = "Mobile App Purchase",
                category = "Software",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 12) }.time,
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
                category = "Fitness",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 18) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -50.0,
                description = "Bookstore",
                category = "Education",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 19) }.time,
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
                category = "Household",
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
                category = "Coffee",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 25) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -120.0,
                description = "Pet Supplies",
                category = "Pets",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 27) }.time,
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
                description = "Rent Payment - ${getMonthName(month)} ${year}",
                category = "Rent",
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
                category = "Electricity",
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
                category = "Internet",
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
                category = "Streaming",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 10) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -60.0,
                description = "Phone Plan",
                category = "Mobile",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 15) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -35.0,
                description = "Cloud Storage",
                category = "Subscription",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 20) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = Date()
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -15.0,
                description = "Music Streaming",
                category = "Subscription",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 23) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = Date()
            )
        )
    }
    
    /**
     * Create future transactions for testing navigation
     */
    private suspend fun createFutureTransactions(userId: Long, numberOfMonths: Int) {
        println("🔄 Creating future transactions for the next $numberOfMonths months")
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val allFutureTransactions = mutableListOf<TransactionEntity>()
        
        // Generate data for each future month
        for (i in 1..numberOfMonths) {
            val targetMonth = getNextMonth(currentMonth, i)
            val targetYear = if (currentMonth + i > 11) currentYear + 1 else currentYear
            
            println("🔄 Generating future transactions for ${getMonthName(targetMonth)} $targetYear")
            
            val monthTransactions = mutableListOf<TransactionEntity>()
            
            // Create recurring transactions (similar each month)
            val recurringExpenses = createFutureRecurringExpenses(userId, targetMonth, targetYear, i)
            monthTransactions.addAll(recurringExpenses)
            
            // Create income (mostly stable)
            val incomeTransactions = createFutureIncomeTransactions(userId, targetMonth, targetYear, i)
            monthTransactions.addAll(incomeTransactions)
            
            // Create expenses with some variations
            val regularExpenses = createFutureRegularExpenses(userId, targetMonth, targetYear, i)
            monthTransactions.addAll(regularExpenses)
            
            allFutureTransactions.addAll(monthTransactions)
            println("✅ Generated ${monthTransactions.size} future transactions for ${getMonthName(targetMonth)}")
        }
        
        // Insert all future transactions
        transactionDao.insertTransactions(allFutureTransactions)
        println("✅ Inserted ${allFutureTransactions.size} future transactions")
    }
    
    /**
     * Create historical transactions for previous months
     */
    private suspend fun createHistoricalTransactions(userId: Long, numberOfMonths: Int) {
        println("🔄 Creating historical transactions for the past $numberOfMonths months")
        
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        val allHistoricalTransactions = mutableListOf<TransactionEntity>()
        
        // Generate data for each previous month
        for (i in 1..numberOfMonths) {
            val targetMonth = getPreviousMonth(currentMonth, i)
            val targetYear = if (currentMonth - i < 0) currentYear - 1 else currentYear
            
            println("🔄 Generating transactions for ${getMonthName(targetMonth)} $targetYear")
            
            val monthTransactions = mutableListOf<TransactionEntity>()
            
            // Create recurring transactions (similar each month with slight variations)
            val recurringExpenses = createHistoricalRecurringExpenses(userId, targetMonth, targetYear, i)
            monthTransactions.addAll(recurringExpenses)
            
            // Create income (mostly stable with occasional variations)
            val incomeTransactions = createHistoricalIncomeTransactions(userId, targetMonth, targetYear, i)
            monthTransactions.addAll(incomeTransactions)
            
            // Create expenses with seasonal variations
            val regularExpenses = createHistoricalRegularExpenses(userId, targetMonth, targetYear, i)
            monthTransactions.addAll(regularExpenses)
            
            allHistoricalTransactions.addAll(monthTransactions)
            println("✅ Generated ${monthTransactions.size} transactions for ${getMonthName(targetMonth)}")
        }
        
        // Insert all historical transactions
        transactionDao.insertTransactions(allHistoricalTransactions)
        println("✅ Inserted ${allHistoricalTransactions.size} historical transactions")
    }
    
    /**
     * Create historical recurring expenses for a specific month
     */
    private fun createHistoricalRecurringExpenses(userId: Long, month: Int, year: Int, monthsAgo: Int): List<TransactionEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        // Base recurring expenses
        val recurringTransactions = mutableListOf(
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -1200.0, // Rent stays the same
                description = "Rent Payment - ${getMonthName(month)} ${year}",
                category = "Rent",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -60.0, // Phone plan stays the same
                description = "Phone Plan",
                category = "Mobile",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 15) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            )
        )
        
        // Add seasonal variations to utility bills
        val seasonalFactor = getSeasonalUtilityFactor(month)
        recurringTransactions.add(
            TransactionEntity(
                id = 0,
                userId = userId,
                // Electricity varies by season
                amount = -130.0 * seasonalFactor,
                description = "Electricity Bill",
                category = "Electricity",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 5) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            )
        )
        
        // Internet and subscriptions (constant across months)
        recurringTransactions.addAll(listOf(
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -80.0,
                description = "Internet Bill",
                category = "Internet",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 7) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -25.0,
                description = "Netflix Subscription",
                category = "Streaming",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 10) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -15.0,
                description = "Music Streaming",
                category = "Subscription",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 23) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            )
        ))
        
        return recurringTransactions
    }
    
    /**
     * Create historical income transactions for a specific month
     */
    private fun createHistoricalIncomeTransactions(userId: Long, month: Int, year: Int, monthsAgo: Int): List<TransactionEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        val incomeTransactions = mutableListOf<TransactionEntity>(
            // Regular salary - consistent each month
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = 3500.0,
                description = "Salary - ${getMonthName(month)} ${year}",
                category = "Salary",
                type = TransactionType.INCOME,
                date = calendar.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            )
        )
        
        // Add variable freelance income - not every month
        if (monthsAgo % 2 == 0) { // Every other month
            incomeTransactions.add(
                TransactionEntity(
                    id = 0,
                    userId = userId,
                    amount = 400.0 + (monthsAgo * 20), // Slightly variable amount
                    description = "Freelance Project",
                    category = "Freelance",
                    type = TransactionType.INCOME,
                    date = calendar.apply { set(Calendar.DAY_OF_MONTH, 15) }.time,
                    isRecurring = false,
                    recurringPeriod = null,
                    createdAt = getHistoricalCreatedDate(calendar.time)
                )
            )
        }
        
        // Add quarterly investment dividends
        if (month % 3 == 0) { // March, June, September, December
            incomeTransactions.add(
                TransactionEntity(
                    id = 0,
                    userId = userId,
                    amount = 250.0,
                    description = "Investment Dividend",
                    category = "Investment",
                    type = TransactionType.INCOME,
                    date = calendar.apply { set(Calendar.DAY_OF_MONTH, 20) }.time,
                    isRecurring = false,
                    recurringPeriod = null,
                    createdAt = getHistoricalCreatedDate(calendar.time)
                )
            )
        }
        
        // Add occasional gift income (birthdays, holidays)
        if (month == 11 || month == 0) { // December or January (holiday gifts)
            incomeTransactions.add(
                TransactionEntity(
                    id = 0,
                    userId = userId,
                    amount = 150.0,
                    description = "Holiday Gift",
                    category = "Gift",
                    type = TransactionType.INCOME,
                    date = calendar.apply { 
                        set(Calendar.DAY_OF_MONTH, if (month == 11) 25 else 5) 
                    }.time,
                    isRecurring = false,
                    recurringPeriod = null,
                    createdAt = getHistoricalCreatedDate(calendar.time)
                )
            )
        }
        
        return incomeTransactions
    }
    
    /**
     * Create historical regular expenses for a specific month
     */
    private fun createHistoricalRegularExpenses(userId: Long, month: Int, year: Int, monthsAgo: Int): List<TransactionEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        val regularExpenses = mutableListOf<TransactionEntity>()
        
        // Add grocery expenses - people shop every week
        for (week in 0..3) {
            val groceryDay = 2 + (week * 7)
            if (groceryDay <= 28) { // Ensure we don't exceed month days
                regularExpenses.add(
                    TransactionEntity(
                        id = 0,
                        userId = userId,
                        amount = -(100.0 + (Math.random() * 40)), // Variable grocery expenses
                        description = "Grocery Shopping",
                        category = "Groceries",
                        type = TransactionType.EXPENSE,
                        date = calendar.apply { set(Calendar.DAY_OF_MONTH, groceryDay) }.time,
                        isRecurring = false,
                        recurringPeriod = null,
                        createdAt = getHistoricalCreatedDate(calendar.time)
                    )
                )
            }
        }
        
        // Transportation expenses - bi-weekly gas fillup
        for (week in 0..1) {
            regularExpenses.add(
                TransactionEntity(
                    id = 0,
                    userId = userId,
                    amount = -45.0,
                    description = "Gas Station",
                    category = "Transportation",
                    type = TransactionType.EXPENSE,
                    date = calendar.apply { set(Calendar.DAY_OF_MONTH, 3 + (week * 14)) }.time,
                    isRecurring = false,
                    recurringPeriod = null,
                    createdAt = getHistoricalCreatedDate(calendar.time)
                )
            )
        }
        
        // Add dining out expenses - varies by month
        val diningOutCount = 1 + (Math.random() * 3).toInt() // 1-3 dining experiences per month
        for (i in 0 until diningOutCount) {
            val diningDay = 5 + (i * 8)
            if (diningDay <= 28) {
                regularExpenses.add(
                    TransactionEntity(
                        id = 0,
                        userId = userId,
                        amount = -(40.0 + (Math.random() * 60)), // Variable dining expenses
                        description = "Restaurant Dinner",
                        category = "Dining Out",
                        type = TransactionType.EXPENSE,
                        date = calendar.apply { set(Calendar.DAY_OF_MONTH, diningDay) }.time,
                        isRecurring = false,
                        recurringPeriod = null,
                        createdAt = getHistoricalCreatedDate(calendar.time)
                    )
                )
            }
        }
        
        // Add seasonal expenses
        addSeasonalExpenses(regularExpenses, userId, calendar, month)
        
        // Add healthcare expenses (occasional)
        if (monthsAgo % 3 == 1) { // Every third month
            regularExpenses.add(
                TransactionEntity(
                    id = 0,
                    userId = userId,
                    amount = -35.0,
                    description = "Pharmacy",
                    category = "Healthcare",
                    type = TransactionType.EXPENSE,
                    date = calendar.apply { set(Calendar.DAY_OF_MONTH, 14) }.time,
                    isRecurring = false,
                    recurringPeriod = null,
                    createdAt = getHistoricalCreatedDate(calendar.time)
                )
            )
        }
        
        // Add entertainment expenses (movies, etc)
        if (monthsAgo % 2 == 0) { // Every other month
            regularExpenses.add(
                TransactionEntity(
                    id = 0,
                    userId = userId,
                    amount = -60.0,
                    description = "Movie Tickets",
                    category = "Entertainment",
                    type = TransactionType.EXPENSE,
                    date = calendar.apply { set(Calendar.DAY_OF_MONTH, 16) }.time,
                    isRecurring = false,
                    recurringPeriod = null,
                    createdAt = getHistoricalCreatedDate(calendar.time)
                )
            )
        }
        
        return regularExpenses
    }
    
    /**
     * Add seasonal expenses based on the month
     */
    private fun addSeasonalExpenses(
        expenses: MutableList<TransactionEntity>,
        userId: Long,
        calendar: Calendar,
        month: Int
    ) {
        when (month) {
            0 -> { // January - Post-holiday sales
                expenses.add(
                    TransactionEntity(
                        id = 0,
                        userId = userId,
                        amount = -150.0,
                        description = "Post-Holiday Sales Shopping",
                        category = "Clothing",
                        type = TransactionType.EXPENSE,
                        date = calendar.apply { set(Calendar.DAY_OF_MONTH, 10) }.time,
                        isRecurring = false,
                        recurringPeriod = null,
                        createdAt = getHistoricalCreatedDate(calendar.time)
                    )
                )
            }
            1 -> { // February - Valentine's Day
                expenses.add(
                    TransactionEntity(
                        id = 0,
                        userId = userId,
                        amount = -120.0,
                        description = "Valentine's Day Dinner",
                        category = "Dining Out",
                        type = TransactionType.EXPENSE,
                        date = calendar.apply { set(Calendar.DAY_OF_MONTH, 14) }.time,
                        isRecurring = false,
                        recurringPeriod = null,
                        createdAt = getHistoricalCreatedDate(calendar.time)
                    )
                )
            }
            3, 4 -> { // April, May - Spring shopping
                expenses.add(
                    TransactionEntity(
                        id = 0,
                        userId = userId,
                        amount = -180.0,
                        description = "Spring Wardrobe Update",
                        category = "Clothing",
                        type = TransactionType.EXPENSE,
                        date = calendar.apply { set(Calendar.DAY_OF_MONTH, 12) }.time,
                        isRecurring = false,
                        recurringPeriod = null,
                        createdAt = getHistoricalCreatedDate(calendar.time)
                    )
                )
            }
            5, 6, 7 -> { // June, July, August - Summer activities
                expenses.add(
                    TransactionEntity(
                        id = 0,
                        userId = userId,
                        amount = -95.0,
                        description = "Beach Day Trip",
                        category = "Entertainment",
                        type = TransactionType.EXPENSE,
                        date = calendar.apply { set(Calendar.DAY_OF_MONTH, 18) }.time,
                        isRecurring = false,
                        recurringPeriod = null,
                        createdAt = getHistoricalCreatedDate(calendar.time)
                    )
                )
            }
            8, 9 -> { // September, October - Back to school/fall
                expenses.add(
                    TransactionEntity(
                        id = 0,
                        userId = userId,
                        amount = -210.0,
                        description = "Fall Shopping",
                        category = "Clothing",
                        type = TransactionType.EXPENSE,
                        date = calendar.apply { set(Calendar.DAY_OF_MONTH, 15) }.time,
                        isRecurring = false,
                        recurringPeriod = null,
                        createdAt = getHistoricalCreatedDate(calendar.time)
                    )
                )
            }
            10, 11 -> { // November, December - Holiday shopping
                expenses.add(
                    TransactionEntity(
                        id = 0,
                        userId = userId,
                        amount = -350.0,
                        description = "Holiday Gift Shopping",
                        category = "Gifts",
                        type = TransactionType.EXPENSE,
                        date = calendar.apply { 
                            set(Calendar.DAY_OF_MONTH, if (month == 10) 28 else 15) 
                        }.time,
                        isRecurring = false,
                        recurringPeriod = null,
                        createdAt = getHistoricalCreatedDate(calendar.time)
                    )
                )
            }
        }
    }
    
    /**
     * Get the appropriate created date for a historical transaction
     */
    private fun getHistoricalCreatedDate(transactionDate: Date): Date {
        val createdCalendar = Calendar.getInstance()
        createdCalendar.time = transactionDate
        // Created date should be a day or two before the transaction date
        createdCalendar.add(Calendar.DAY_OF_MONTH, -1 * (1 + (Math.random() * 2).toInt()))
        return createdCalendar.time
    }
    
    /**
     * Calculate the previous month
     */
    private fun getPreviousMonth(currentMonth: Int, monthsAgo: Int): Int {
        var result = currentMonth - monthsAgo
        while (result < 0) {
            result += 12
        }
        return result
    }
    
    /**
     * Calculate the next month
     */
    private fun getNextMonth(currentMonth: Int, monthsAhead: Int): Int {
        var result = currentMonth + monthsAhead
        while (result > 11) {
            result -= 12
        }
        return result
    }
    
    /**
     * Get a month name from its number (0-based)
     */
    private fun getMonthName(month: Int): String {
        val months = arrayOf(
            "January", "February", "March", "April", "May", "June", 
            "July", "August", "September", "October", "November", "December"
        )
        return months[month]
    }
    
    /**
     * Get a seasonal factor for utility bills
     */
    private fun getSeasonalUtilityFactor(month: Int): Double {
        return when (month) {
            11, 0, 1 -> 1.5  // Winter (Dec, Jan, Feb): Higher heating costs
            5, 6, 7 -> 1.3   // Summer (Jun, Jul, Aug): Higher cooling costs
            else -> 1.0      // Spring/Fall: Normal costs
        }
    }
    
    /**
     * Create future recurring expenses for a specific month
     */
    private fun createFutureRecurringExpenses(userId: Long, month: Int, year: Int, monthsAhead: Int): List<TransactionEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        return listOf(
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -1200.0, // Rent stays the same
                description = "Rent Payment - ${getMonthName(month)} ${year}",
                category = "Rent",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 1) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -150.0, // Electricity varies slightly
                description = "Electricity Bill",
                category = "Electricity",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 5) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -80.0,
                description = "Internet Bill",
                category = "Internet",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 7) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -25.0,
                description = "Netflix Subscription",
                category = "Streaming",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 10) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -60.0,
                description = "Phone Plan",
                category = "Mobile",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 15) }.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            )
        )
    }
    
    /**
     * Create future income transactions for a specific month
     */
    private fun createFutureIncomeTransactions(userId: Long, month: Int, year: Int, monthsAhead: Int): List<TransactionEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        val incomeTransactions = mutableListOf(
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = 3500.0,
                description = "Salary - ${getMonthName(month)} ${year}",
                category = "Salary",
                type = TransactionType.INCOME,
                date = calendar.time,
                isRecurring = true,
                recurringPeriod = RecurringPeriod.MONTHLY,
                createdAt = getHistoricalCreatedDate(calendar.time)
            )
        )
        
        // Add occasional freelance income
        if (monthsAhead % 2 == 0) {
            incomeTransactions.add(
                TransactionEntity(
                    id = 0,
                    userId = userId,
                    amount = 450.0,
                    description = "Freelance Project",
                    category = "Freelance",
                    type = TransactionType.INCOME,
                    date = calendar.apply { set(Calendar.DAY_OF_MONTH, 15) }.time,
                    isRecurring = false,
                    recurringPeriod = null,
                    createdAt = getHistoricalCreatedDate(calendar.time)
                )
            )
        }
        
        return incomeTransactions
    }
    
    /**
     * Create future regular expenses for a specific month
     */
    private fun createFutureRegularExpenses(userId: Long, month: Int, year: Int, monthsAhead: Int): List<TransactionEntity> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1)
        
        return listOf(
            // Grocery expenses
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -110.0,
                description = "Grocery Shopping",
                category = "Groceries",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 2) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -125.0,
                description = "Grocery Shopping",
                category = "Groceries",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 16) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -95.0,
                description = "Grocery Shopping",
                category = "Groceries",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 28) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            
            // Transportation
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -50.0,
                description = "Gas Station",
                category = "Transportation",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 5) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -45.0,
                description = "Gas Station",
                category = "Transportation",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 20) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            
            // Dining out
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -75.0,
                description = "Restaurant Dinner",
                category = "Dining Out",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 8) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -55.0,
                description = "Coffee Shop",
                category = "Coffee",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 22) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            
            // Entertainment
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -65.0,
                description = "Movie Tickets",
                category = "Entertainment",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 12) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            
            // Miscellaneous
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -180.0,
                description = "Shopping Mall",
                category = "Clothing",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 18) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
            ),
            TransactionEntity(
                id = 0,
                userId = userId,
                amount = -40.0,
                description = "Pharmacy",
                category = "Healthcare",
                type = TransactionType.EXPENSE,
                date = calendar.apply { set(Calendar.DAY_OF_MONTH, 25) }.time,
                isRecurring = false,
                recurringPeriod = null,
                createdAt = getHistoricalCreatedDate(calendar.time)
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
