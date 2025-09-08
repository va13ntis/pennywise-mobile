# PennyWise - Default Currency Feature PRD

## Overview
This document outlines the implementation of a default currency feature for the PennyWise personal finance management app. The feature allows users to set a default currency during registration and provides intelligent currency management throughout the expense tracking experience.

## Feature Goals
- Allow users to select a default currency during registration
- Use the default currency for new expenses with the ability to modify
- Display currencies sorted by popularity (most used first)
- Provide a seamless currency selection experience
- Maintain backward compatibility with existing transactions

## User Stories

### Primary User Stories
1. **As a new user**, I want to select my default currency during registration so that my expenses are automatically recorded in my preferred currency.
2. **As a user**, I want to see my default currency pre-selected when adding new expenses so I don't have to change it every time.
3. **As a user**, I want to be able to change the currency for individual expenses when needed so I can record transactions in different currencies.
4. **As a user**, I want to see currencies sorted by popularity so I can quickly find the most commonly used ones.

### Secondary User Stories
5. **As a user**, I want to change my default currency in settings so I can update my preference if I move to a different country.
6. **As a user**, I want to see my default currency clearly indicated in the app so I always know what currency I'm working with.

## Technical Requirements

### Data Model Changes

#### 1. User Entity Enhancement
```kotlin
data class User(
    val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val email: String? = null,
    val defaultCurrency: String = "USD", // New field
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
```

#### 2. Transaction Entity Enhancement
```kotlin
data class Transaction(
    val id: Long = 0,
    val userId: Long,
    val amount: Double,
    val currency: String, // New field
    val description: String,
    val category: String,
    val type: TransactionType,
    val date: Date,
    val isRecurring: Boolean = false,
    val recurringPeriod: RecurringPeriod? = null,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
```

#### 3. Currency Usage Tracking
```kotlin
data class CurrencyUsage(
    val id: Long = 0,
    val userId: Long,
    val currency: String,
    val usageCount: Int,
    val lastUsed: Date,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
```

### Database Schema Changes

#### 1. Users Table
```sql
ALTER TABLE users ADD COLUMN defaultCurrency TEXT NOT NULL DEFAULT 'USD';
```

#### 2. Transactions Table
```sql
ALTER TABLE transactions ADD COLUMN currency TEXT NOT NULL DEFAULT 'USD';
```

#### 3. Currency Usage Table
```sql
CREATE TABLE currency_usage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    userId INTEGER NOT NULL,
    currency TEXT NOT NULL,
    usageCount INTEGER NOT NULL DEFAULT 0,
    lastUsed INTEGER NOT NULL,
    createdAt INTEGER NOT NULL,
    updatedAt INTEGER NOT NULL,
    FOREIGN KEY (userId) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(userId, currency)
);
```

### Currency Data

#### Supported Currencies
The app will support the following currencies with their symbols and names:

```kotlin
enum class Currency(val code: String, val symbol: String, val name: String, val popularity: Int) {
    USD("USD", "$", "US Dollar", 1),
    EUR("EUR", "€", "Euro", 2),
    GBP("GBP", "£", "British Pound", 3),
    JPY("JPY", "¥", "Japanese Yen", 4),
    CAD("CAD", "C$", "Canadian Dollar", 5),
    AUD("AUD", "A$", "Australian Dollar", 6),
    CHF("CHF", "CHF", "Swiss Franc", 7),
    CNY("CNY", "¥", "Chinese Yuan", 8),
    SEK("SEK", "kr", "Swedish Krona", 9),
    NOK("NOK", "kr", "Norwegian Krone", 10),
    DKK("DKK", "kr", "Danish Krone", 11),
    PLN("PLN", "zł", "Polish Złoty", 12),
    CZK("CZK", "Kč", "Czech Koruna", 13),
    HUF("HUF", "Ft", "Hungarian Forint", 14),
    RUB("RUB", "₽", "Russian Ruble", 15),
    TRY("TRY", "₺", "Turkish Lira", 16),
    BRL("BRL", "R$", "Brazilian Real", 17),
    INR("INR", "₹", "Indian Rupee", 18),
    KRW("KRW", "₩", "South Korean Won", 19),
    SGD("SGD", "S$", "Singapore Dollar", 20),
    HKD("HKD", "HK$", "Hong Kong Dollar", 21),
    ILS("ILS", "₪", "Israeli Shekel", 22),
    AED("AED", "د.إ", "UAE Dirham", 23),
    SAR("SAR", "ر.س", "Saudi Riyal", 24),
    ZAR("ZAR", "R", "South African Rand", 25)
}
```

### UI/UX Requirements

#### 1. Registration Screen Enhancement
- Add currency selection dropdown after username/password fields
- Show currency code, symbol, and name (e.g., "USD - $ - US Dollar")
- Sort currencies by popularity (most used first)
- Pre-select USD as default
- Make currency selection required

#### 2. New Expense Form Enhancement
- Pre-select user's default currency
- Allow currency change via dropdown
- Show currency symbol next to amount field
- Maintain currency selection for the session

#### 3. Settings Screen Enhancement
- Add "Default Currency" section
- Show current default currency
- Allow changing default currency
- Show confirmation dialog for currency change

#### 4. Currency Selection Component
- Reusable dropdown component
- Search functionality for large currency lists
- Group by popularity (Most Popular, All Currencies)
- Show currency code, symbol, and name

### Business Logic

#### 1. Currency Usage Tracking
- Track currency usage when transactions are created
- Update usage count and last used timestamp
- Use this data to sort currencies by popularity

#### 2. Default Currency Logic
- Use user's default currency for new transactions
- Allow override for individual transactions
- Maintain currency information for existing transactions

#### 3. Currency Display
- Show currency symbol with amounts
- Format amounts according to currency conventions
- Handle different decimal places (JPY has 0, most others have 2)

### Migration Strategy

#### 1. Database Migration
- Add new columns to existing tables
- Set default values for existing records
- Create new currency_usage table

#### 2. Data Migration
- Set default currency to USD for existing users
- Set currency to USD for existing transactions
- Initialize currency usage data

### Error Handling
- Validate currency codes
- Handle unsupported currencies gracefully
- Provide fallback to USD if currency is invalid
- Show appropriate error messages

### Testing Requirements

#### 1. Unit Tests
- Currency enum validation
- Currency usage tracking logic
- Default currency assignment
- Currency sorting by popularity

#### 2. Integration Tests
- Registration with currency selection
- Transaction creation with different currencies
- Currency usage tracking updates
- Settings currency change

#### 3. UI Tests
- Currency selection in registration
- Currency selection in expense form
- Currency display in transaction lists
- Settings currency change flow

### Performance Considerations
- Cache currency data to avoid repeated lookups
- Optimize currency usage queries
- Use efficient sorting for currency lists
- Minimize database queries for currency information

### Security Considerations
- Validate currency codes to prevent injection
- Sanitize currency input data
- Ensure currency data integrity

### Accessibility
- Provide proper content descriptions for currency symbols
- Ensure currency selection is keyboard accessible
- Support screen readers for currency information

### Localization
- Support currency names in multiple languages
- Handle RTL languages for currency display
- Provide appropriate currency formatting for different locales

## Implementation Phases

### Phase 1: Core Currency Support
1. Add currency fields to data models
2. Update database schema
3. Implement currency enum and utilities
4. Add currency selection to registration

### Phase 2: Transaction Integration
1. Update transaction creation with currency
2. Modify expense form to include currency selection
3. Update transaction display to show currency
4. Implement currency usage tracking

### Phase 3: Settings and Management
1. Add currency settings to settings screen
2. Implement default currency change functionality
3. Add currency usage analytics
4. Optimize currency sorting by popularity

### Phase 4: Polish and Testing
1. Comprehensive testing
2. Performance optimization
3. Accessibility improvements
4. Documentation updates

## Success Metrics
- 100% of new users select a default currency during registration
- Users can successfully change currency for individual transactions
- Currency usage tracking accurately reflects user preferences
- Currency selection performance is under 100ms
- No data loss during migration from existing transactions

## Future Enhancements
- Real-time currency conversion rates
- Multi-currency expense tracking
- Currency exchange history
- Automatic currency detection based on location
- Currency conversion for reports and analytics
