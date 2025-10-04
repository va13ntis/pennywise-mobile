# Complete userId Removal for Single-User Architecture

## Overview
PennyWise is a personal finance mobile app with ONE user per installation. The current multi-user database design with userId fields is unnecessary and over-engineered. This PRD outlines the complete removal of all userId references from the codebase to align with the single-user architecture documented in the main PRD.

## Background
The app currently uses a multi-tenant database design with userId foreign keys, filtering, and relationships. This was based on incorrect assumptions. Since there is only one user per app installation, all userId-related infrastructure should be removed.

## Goals
1. Remove userId from all database entities
2. Remove userId filtering from all DAO queries
3. Remove userId from all domain models
4. Simplify repositories to not use currentUserId
5. Simplify BaseAuthenticatedRepository to not depend on AuthManager
6. Maintain authentication for security and backup purposes (not data isolation)

## Implementation Tasks

### Task 1: Create Room Database Migration
**Description:** Create a Room migration to remove userId columns from all tables and drop foreign key constraints to the users table.

**Details:**
- Create migration from current version to next version
- Drop foreign key constraints from all tables to users table
- Remove userId column from: transactions, bank_cards, currency_usage, split_payment_installments, payment_method_config
- Test migration with existing data
- Ensure no data loss during migration

**Acceptance Criteria:**
- Migration runs successfully on test device
- All userId columns removed from database schema
- No foreign key constraints remain
- Existing data preserved (minus userId columns)

---

### Task 2: Update TransactionEntity
**Description:** Remove userId field and foreign key constraint from TransactionEntity.

**Details:**
- Remove userId field from data class
- Remove foreign key definition from @Entity annotation
- Remove userId index
- Update fromDomainModel and toDomainModel methods
- Ensure companion object doesn't reference userId

**Acceptance Criteria:**
- No userId field in TransactionEntity
- No compilation errors
- Entity compiles successfully

---

### Task 3: Update BankCardEntity  
**Description:** Remove userId field and foreign key constraint from BankCardEntity.

**Details:**
- Remove userId field from data class
- Remove foreign key definition from @Entity annotation
- Remove userId index
- Update any conversion methods

**Acceptance Criteria:**
- No userId field in BankCardEntity
- No compilation errors
- Entity compiles successfully

---

### Task 4: Update CurrencyUsageEntity
**Description:** Remove userId field from CurrencyUsageEntity.

**Details:**
- Remove userId field from data class
- Remove any foreign key constraints
- Update conversion methods if present

**Acceptance Criteria:**
- No userId field in CurrencyUsageEntity
- No compilation errors

---

### Task 5: Update SplitPaymentInstallmentEntity
**Description:** Remove userId field from SplitPaymentInstallmentEntity.

**Details:**
- Remove userId field from data class
- Remove any foreign key constraints
- Update conversion methods

**Acceptance Criteria:**
- No userId field in SplitPaymentInstallmentEntity
- No compilation errors

---

### Task 6: Update PaymentMethodConfigEntity
**Description:** Remove userId field from PaymentMethodConfigEntity.

**Details:**
- Remove userId field from data class  
- Remove any foreign key constraints
- Update conversion methods

**Acceptance Criteria:**
- No userId field in PaymentMethodConfigEntity
- No compilation errors

---

### Task 7: Update TransactionDao
**Description:** Remove userId parameters and filtering from all TransactionDao queries.

**Details:**
- Remove userId parameter from: getTransactionsByUser, getTransactionsByDateRange, getTransactionsByCategory, getTransactionsByType, getRecurringTransactions, getTotalIncome, getTotalExpense, getBalance, getTotalByTypeAndDateRange, getTransactionCount, getTransactionCountByDateRange
- Update all @Query annotations to remove WHERE userId = :userId clauses
- Rename methods (e.g., getTransactionsByUser → getTransactions)

**Acceptance Criteria:**
- No userId parameters in any DAO methods
- All queries work without userId filtering
- No compilation errors

---

### Task 8: Update BankCardDao
**Description:** Remove userId parameters from all BankCardDao queries.

**Details:**
- Remove userId filtering from all queries
- Update method signatures
- Simplify query logic

**Acceptance Criteria:**
- No userId parameters in BankCardDao
- Queries return all data (no user filtering)

---

### Task 9: Update CurrencyUsageDao
**Description:** Remove userId parameters from CurrencyUsageDao queries.

**Details:**
- Remove userId filtering from all queries
- Update method signatures

**Acceptance Criteria:**
- No userId parameters in CurrencyUsageDao

---

### Task 10: Update SplitPaymentInstallmentDao
**Description:** Remove userId parameters from SplitPaymentInstallmentDao queries.

**Details:**
- Remove userId filtering from all queries
- Update method signatures

**Acceptance Criteria:**
- No userId parameters in SplitPaymentInstallmentDao

---

### Task 11: Update PaymentMethodConfigDao
**Description:** Remove userId parameters from PaymentMethodConfigDao queries.

**Details:**
- Remove userId filtering from all queries
- Update method signatures

**Acceptance Criteria:**
- No userId parameters in PaymentMethodConfigDao

---

### Task 12: Update Transaction Domain Model
**Description:** Remove userId field from Transaction domain model.

**Details:**
- Remove userId field from data class
- Update all usages of Transaction model
- Ensure ViewModels don't reference transaction.userId

**Acceptance Criteria:**
- No userId in Transaction model
- No compilation errors

---

### Task 13: Update BankCard Domain Model
**Description:** Remove userId field from BankCard domain model.

**Details:**
- Remove userId field from data class
- Update all usages

**Acceptance Criteria:**
- No userId in BankCard model
- No compilation errors

---

### Task 14: Update Other Domain Models
**Description:** Remove userId from all remaining domain models (SplitPaymentInstallment, etc.).

**Details:**
- Scan all domain models for userId fields
- Remove userId from each model
- Update usages

**Acceptance Criteria:**
- No userId in any domain models
- No compilation errors

---

### Task 15: Update TransactionRepositoryImpl
**Description:** Remove currentUserId usage from TransactionRepositoryImpl.

**Details:**
- Remove all references to currentUserId property
- Update DAO method calls to not pass userId
- Keep withAuthentication() wrapper for security
- Remove AuthManager from constructor

**Acceptance Criteria:**
- No currentUserId references
- No userId passed to DAO
- Authentication still enforced

---

### Task 16: Update BankCardRepositoryImpl
**Description:** Remove currentUserId usage from BankCardRepositoryImpl.

**Details:**
- Remove all references to currentUserId
- Update DAO method calls
- Remove AuthManager from constructor

**Acceptance Criteria:**
- No currentUserId references
- Authentication still enforced

---

### Task 17: Update All Other Repositories
**Description:** Remove currentUserId from CurrencyUsageRepositoryImpl, SplitPaymentInstallmentRepositoryImpl, PaymentMethodConfigRepositoryImpl.

**Details:**
- Remove currentUserId from each repository
- Update DAO calls
- Remove AuthManager dependencies

**Acceptance Criteria:**
- No currentUserId in any repository
- All repositories compile successfully

---

### Task 18: Simplify BaseAuthenticatedRepository
**Description:** Remove AuthManager dependency and currentUserId property from BaseAuthenticatedRepository.

**Details:**
- Remove AuthManager from constructor
- Remove currentUserId property
- Keep withAuthentication() method for security
- Update documentation

**Before:**
```kotlin
abstract class BaseAuthenticatedRepository(
    private val authValidator: AuthenticationValidator,
    private val authManager: AuthManager
) {
    protected val currentUserId: Long
        get() = authManager.currentUser.value?.id 
            ?: throw SecurityException("No authenticated user found")
}
```

**After:**
```kotlin
abstract class BaseAuthenticatedRepository(
    private val authValidator: AuthenticationValidator
) {
    // No currentUserId property
    protected suspend fun <T> withAuthentication(block: suspend () -> T): T {
        if (!authValidator.validateUserAuthenticated()) {
            throw SecurityException("Authentication required")
        }
        return block()
    }
}
```

**Acceptance Criteria:**
- No AuthManager dependency
- No currentUserId property
- withAuthentication() still works
- All repositories still authenticate properly

---

### Task 19: Update Repository Constructors
**Description:** Update all repository implementations to not inject AuthManager.

**Details:**
- Remove authManager parameter from all repository constructors
- Pass only authValidator to BaseAuthenticatedRepository
- Update RepositoryModule.kt Hilt bindings

**Acceptance Criteria:**
- No AuthManager in repository constructors
- Dependency injection works correctly

---

### Task 20: Update RepositoryModule.kt
**Description:** Update Hilt dependency injection module for repositories.

**Details:**
- Remove AuthManager from @Provides methods
- Update all repository bindings
- Ensure proper injection

**Acceptance Criteria:**
- App compiles with updated DI
- Repositories inject correctly

---

### Task 21: Verify ViewModels
**Description:** Ensure all ViewModels work correctly without userId.

**Details:**
- Check HomeViewModel, AddExpenseViewModel, BankCardViewModel, etc.
- Remove any remaining userId references
- Verify all repository calls work

**Acceptance Criteria:**
- No userId in ViewModels
- All features work correctly

---

### Task 22: Update Test Files
**Description:** Update all unit and instrumented tests to work without userId.

**Details:**
- Update repository tests
- Update DAO tests
- Update ViewModel tests
- Remove userId from test data

**Acceptance Criteria:**
- All tests pass
- No userId references in tests

---

### Task 23: Integration Testing
**Description:** Comprehensive testing of the simplified single-user architecture.

**Details:**
- Test all CRUD operations
- Test authentication flows
- Test data persistence
- Test app across device restart
- Verify no data loss

**Acceptance Criteria:**
- All features work correctly
- Data persists correctly
- No regression bugs

---

### Task 24: Documentation Update
**Description:** Update all code documentation to reflect single-user architecture.

**Details:**
- Update README if needed
- Update code comments
- Remove references to "multi-user" or "user isolation"
- Document that authentication is for security/backup only

**Acceptance Criteria:**
- Documentation is accurate
- No misleading comments

## Success Criteria
- ✅ All userId fields removed from database entities
- ✅ All userId parameters removed from DAOs
- ✅ All userId fields removed from domain models
- ✅ All repositories simplified (no currentUserId)
- ✅ BaseAuthenticatedRepository simplified (no AuthManager)
- ✅ All tests passing
- ✅ App works correctly with single-user design
- ✅ Documentation updated

## Risk Mitigation
- **Data Loss:** Create database migration carefully, test on backup
- **Compilation Errors:** Make changes incrementally, test after each step
- **Regression Bugs:** Comprehensive testing after completion
- **User Impact:** This is an internal refactoring with no user-facing changes

## Timeline
- Estimated: 2-3 development sessions
- Each subtask: 15-30 minutes
- Testing: 1 session

## References
- Main PRD: `.taskmaster/docs/PRD.md`
- Architecture Doc: `SINGLE_USER_ARCHITECTURE.md`
- Cancelled Task 29: Previous incorrect approach
