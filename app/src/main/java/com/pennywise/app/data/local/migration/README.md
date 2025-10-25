# Database Migrations

This directory contains Room database migrations for the PennyWise app. These migrations preserve user data when the database schema changes.

## Overview

**CRITICAL:** This app now uses proper database migrations instead of `fallbackToDestructiveMigration()`. This ensures that user data is **never** lost during app updates.

## Migration Strategy

### Version History

- **Version 1→2**: Added currency support to transactions
- **Version 2→3**: Added `bank_cards` and `split_payment_installments` tables
- **Version 3→4**: Added `payment_method_configs` table and nullable `paymentMethodConfigId`, `installments`, `installmentAmount` columns to transactions
- **Version 2→4**: Combined migration for users upgrading directly from v2 to v4

### How Migrations Work

All migrations are defined in `DatabaseMigrations.kt` and automatically applied when the database is opened. Room handles:
- Detecting the current database version
- Applying all necessary migrations in sequence
- Validating that the final schema matches the expected schema

### Testing Migrations

Migration tests are in `app/src/androidTest/java/com/pennywise/app/data/local/migration/CurrencyMigrationTest.kt`.

These tests verify:
- ✅ User data is preserved during migration
- ✅ New tables and columns are created correctly
- ✅ All migration paths work (2→3, 3→4, 2→4)
- ✅ Final schema matches Room's expectations

Run migration tests with:
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.data.local.migration.CurrencyMigrationTest
```

## Adding New Migrations

### Step 1: Increment Database Version

Update the version in `PennyWiseDatabase.kt`:
```kotlin
@Database(
    entities = [...],
    version = 5, // Increment this
    exportSchema = true
)
```

### Step 2: Create Migration Object

Add a new migration to `DatabaseMigrations.kt`:
```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // For new nullable columns (SAFE - preserves data):
        db.execSQL("ALTER TABLE table_name ADD COLUMN new_column TYPE")
        
        // For new tables:
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS new_table (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                ...
            )
        """)
        
        // For data transformations (when needed):
        db.execSQL("UPDATE table_name SET column = 'default_value' WHERE column IS NULL")
    }
}
```

### Step 3: Register Migration

Add the new migration to `getAllMigrations()`:
```kotlin
fun getAllMigrations(): Array<Migration> {
    return arrayOf(
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_2_4,
        MIGRATION_4_5 // Add new migration
    )
}
```

### Step 4: Write Tests

Add test cases to `CurrencyMigrationTest.kt`:
```kotlin
@Test
fun migrate4To5_preservesData() {
    // Test that your migration works correctly
}
```

### Step 5: Build & Verify

1. Clean build: `./gradlew clean`
2. Build app: `./gradlew assembleDebug`
3. Room will generate new schema file in `app/schemas/`
4. Commit the schema file to version control
5. Run migration tests: `./gradlew connectedAndroidTest`

## Schema Files

Room exports schema JSON files to `app/schemas/`. These files:
- ✅ **Should be committed to version control**
- Document the database structure at each version
- Enable Room to validate migrations automatically
- Help the team understand schema evolution

## Best Practices

### Safe Migrations (No Data Loss)

✅ **Adding nullable columns**: Always safe
```kotlin
db.execSQL("ALTER TABLE table_name ADD COLUMN new_column TEXT")
```

✅ **Adding new tables**: Always safe
```kotlin
db.execSQL("CREATE TABLE IF NOT EXISTS new_table (...)")
```

✅ **Adding indexes**: Always safe
```kotlin
db.execSQL("CREATE INDEX index_name ON table_name(column)")
```

### Unsafe Operations (Require Care)

⚠️ **Dropping columns**: Not directly supported by SQLite
- Solution: Create new table, copy data, drop old table, rename new table

⚠️ **Changing column types**: Requires data transformation
- Solution: Add new column, migrate data, drop old column (multi-step)

⚠️ **Adding non-nullable columns**: Requires default value
```kotlin
// Bad: Will fail if table has data
db.execSQL("ALTER TABLE table ADD COLUMN new_col TEXT NOT NULL")

// Good: Add as nullable, populate, then add constraint if needed
db.execSQL("ALTER TABLE table ADD COLUMN new_col TEXT")
db.execSQL("UPDATE table SET new_col = 'default' WHERE new_col IS NULL")
```

### Testing Checklist

Before releasing a migration:
- [ ] Migration test passes locally
- [ ] Migration test passes in CI
- [ ] Clean install works (new users)
- [ ] Upgrade from previous version works (existing users)
- [ ] Schema file is committed to git
- [ ] Migration is documented in this README

## Troubleshooting

### "Expected schema hash doesn't match"
- This means the schema file doesn't match the entities
- Solution: Run `./gradlew clean` and rebuild

### "Migration didn't properly handle..."
- Room detected the final schema doesn't match
- Solution: Check your SQL in the migration matches the entity definitions

### "Migration path not found"
- Room can't find a path from old version to new version
- Solution: Ensure all migration objects are registered in `getAllMigrations()`

## Emergency: Destructive Migration

If you absolutely must wipe user data (e.g., during early development):

1. **Never do this in production**
2. Temporarily add `.fallbackToDestructiveMigration()` to database builder
3. Increment version number
4. Test thoroughly
5. Remove destructive migration before release
6. Add proper migrations

## References

- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Testing Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions#test)

