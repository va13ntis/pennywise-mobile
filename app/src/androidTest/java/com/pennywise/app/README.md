# Currency UI Tests

This directory contains comprehensive UI tests for currency-related functionality in the PennyWise app. The tests use Espresso and Compose testing frameworks to verify that all currency-related screens and components function correctly from a user perspective.

## Test Structure

### Test Utilities
- **`testutils/CurrencyTestFixtures.kt`**: Provides test data and fixtures for currency-related tests
- **`testutils/BaseCurrencyUiTest.kt`**: Base class with common setup and utilities for currency UI tests

### Component Tests
- **`presentation/components/CurrencySelectionDropdownTest.kt`**: Tests for the currency selection dropdown component
- **`presentation/components/CurrencySearchTest.kt`**: Tests for currency search functionality
- **`presentation/components/CurrencySymbolUpdateTest.kt`**: Tests for currency symbol updates when currency changes
- **`presentation/components/CurrencyValidationTest.kt`**: Tests for error states and validation messages

### Screen Tests
- **`presentation/screens/AddExpenseScreenCurrencyTest.kt`**: Tests for currency selection in the add expense screen
- **`presentation/screens/SettingsScreenCurrencyTest.kt`**: Tests for currency switching in the settings screen
- **`presentation/screens/TransactionListCurrencyTest.kt`**: Tests for currency display in transaction lists

### Test Suite
- **`CurrencyUiTestSuite.kt`**: Test suite that runs all currency-related UI tests

## Test Coverage

### Currency Selection Component Behavior
- ✅ Currency dropdown display and interaction
- ✅ Currency selection from dropdown
- ✅ Currency grouping by popularity
- ✅ Currency search functionality
- ✅ Currency selection dialog behavior

### Currency Display in Transaction Lists
- ✅ Currency symbol display for different currencies (USD, EUR, GBP, JPY, KRW, etc.)
- ✅ Amount formatting for different currencies
- ✅ Currency code display alongside symbols
- ✅ Transaction grouping by currency
- ✅ Recurring transaction indicators with currency

### Currency Switching in Settings
- ✅ Currency conversion toggle functionality
- ✅ Original currency selection
- ✅ Currency conversion settings persistence
- ✅ Currency conversion help text and accessibility

### Currency Search Functionality
- ✅ Search by currency code (USD, EUR, GBP, JPY, etc.)
- ✅ Search by currency symbol ($, €, £, ¥, etc.)
- ✅ Search by display name (US Dollar, Euro, British Pound, etc.)
- ✅ Case-insensitive search
- ✅ Partial match search
- ✅ Search results filtering
- ✅ No results handling

### Currency Symbol Updates
- ✅ Symbol updates when currency changes from USD to EUR
- ✅ Symbol updates when currency changes from USD to JPY
- ✅ Symbol updates when currency changes from USD to GBP
- ✅ Symbol updates when currency changes from USD to KRW
- ✅ Symbol updates for all supported currencies
- ✅ Amount field formatting updates when currency changes

### Error States and Validation
- ✅ Error messages for empty amount fields
- ✅ Error messages for invalid amounts (negative, zero, non-numeric)
- ✅ Error messages for currencies without decimal places (JPY, KRW)
- ✅ Error messages for missing merchant information
- ✅ Error messages for missing category selection
- ✅ Error messages for missing currency selection
- ✅ Validation for different currency types

## Running the Tests

### Run All Currency UI Tests
```bash
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyUiTestSuite
```

### Run Individual Test Classes
```bash
# Currency selection dropdown tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.components.CurrencySelectionDropdownTest

# Currency search tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.components.CurrencySearchTest

# Currency symbol update tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.components.CurrencySymbolUpdateTest

# Currency validation tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.components.CurrencyValidationTest

# Add expense screen currency tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.screens.AddExpenseScreenCurrencyTest

# Settings screen currency tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.screens.SettingsScreenCurrencyTest

# Transaction list currency tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.presentation.screens.TransactionListCurrencyTest
```

### Run Tests with Specific Filters
```bash
# Run only large tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyUiTestSuite -Pandroid.testInstrumentationRunnerArguments.notAnnotation=androidx.test.filters.SmallTest

# Run tests for specific SDK version
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyUiTestSuite -Pandroid.testInstrumentationRunnerArguments.sdkSuppress=26
```

## Test Data

The tests use the following test currencies:
- **USD** (US Dollar) - 2 decimal places
- **EUR** (Euro) - 2 decimal places
- **GBP** (British Pound) - 2 decimal places
- **JPY** (Japanese Yen) - 0 decimal places
- **KRW** (South Korean Won) - 0 decimal places
- **CAD** (Canadian Dollar) - 2 decimal places
- **AUD** (Australian Dollar) - 2 decimal places
- **CHF** (Swiss Franc) - 2 decimal places
- **CNY** (Chinese Yuan) - 2 decimal places
- **INR** (Indian Rupee) - 2 decimal places

## Test Scenarios

### Currency Selection
1. **Basic Selection**: User can select different currencies from dropdown
2. **Search Selection**: User can search and select currencies by code, symbol, or name
3. **Popularity Grouping**: Currencies are grouped by popularity in dropdown
4. **Validation**: Selected currency is properly validated and displayed

### Currency Display
1. **Symbol Display**: Currency symbols are correctly displayed in amount fields
2. **Amount Formatting**: Amounts are formatted according to currency decimal places
3. **Transaction Lists**: Currency symbols and codes are displayed in transaction lists
4. **Settings**: Currency conversion settings are properly displayed and managed

### Currency Validation
1. **Amount Validation**: Amounts are validated according to currency rules
2. **Decimal Places**: Currencies with 0 decimal places (JPY, KRW) reject decimal input
3. **Error Messages**: Appropriate error messages are shown for invalid inputs
4. **Form Validation**: Forms are validated before submission

### Currency Switching
1. **Symbol Updates**: Currency symbols update when currency changes
2. **Amount Reformating**: Amount fields are reformatted when currency changes
3. **Settings Persistence**: Currency settings are persisted across app sessions
4. **Conversion Toggle**: Currency conversion can be enabled/disabled

## Dependencies

The tests require the following dependencies (already included in build.gradle.kts):
- `androidx.test.ext:junit:1.1.5`
- `androidx.test.espresso:espresso-core:3.5.1`
- `androidx.compose.ui:ui-test-junit4`
- `com.google.dagger:hilt-android-testing:2.48`

## Test Environment

- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 34 (Android 14)
- **Test Framework**: Espresso + Compose Testing
- **Dependency Injection**: Hilt for testing
- **Test Data**: Mock data and fixtures for consistent testing

## Best Practices

1. **Test Isolation**: Each test is independent and doesn't rely on other tests
2. **Test Data**: Use consistent test data from `CurrencyTestFixtures`
3. **Error Handling**: Test both success and error scenarios
4. **Accessibility**: Verify accessibility features work correctly
5. **Performance**: Tests are optimized for fast execution
6. **Maintainability**: Tests are well-documented and easy to understand

## Troubleshooting

### Common Issues
1. **Test Timeout**: Increase timeout if tests are slow
2. **Device Compatibility**: Ensure device supports required SDK version
3. **Hilt Issues**: Verify Hilt test setup is correct
4. **Compose Issues**: Ensure Compose test dependencies are properly configured

### Debug Tips
1. Use `adb logcat` to view test logs
2. Check device logs for any runtime errors
3. Verify test data is properly set up
4. Ensure all required permissions are granted
