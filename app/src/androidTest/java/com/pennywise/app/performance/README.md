# Currency Performance Tests

This directory contains comprehensive performance tests for currency-related operations in the PennyWise app. The tests use AndroidX Microbenchmark library to measure and analyze the performance of various currency operations.

## Test Files

### 1. CurrencyConversionPerformanceTest.kt
Tests the performance of currency conversion operations:
- Same currency conversion (no-op optimization)
- Cached conversion performance
- Multiple currency conversions
- Large amount conversions
- Cache operations
- Currency availability checks
- Concurrent conversions
- Cache invalidation
- JSON serialization/deserialization

### 2. CurrencySortingPerformanceTest.kt
Tests the performance of currency sorting operations:
- Small dataset sorting
- Cached sorting performance
- Top currencies retrieval
- Used currencies retrieval
- Currency usage tracking
- Cache statistics
- Cache invalidation
- Reactive sorting
- Enhanced reactive sorting
- Concurrent sorting operations
- Large dataset sorting
- Memory usage during sorting

### 3. CurrencyUsageTrackerPerformanceTest.kt
Tests the performance of currency usage tracking:
- Single currency usage tracking
- Multiple currency usage tracking
- Currency popularity retrieval
- Top currencies retrieval
- Usage statistics calculation
- Most/least used currencies
- Trend analysis
- Summary generation
- Concurrent operations
- Large dataset statistics
- Memory usage during calculations
- Percentage calculations
- Date-based trend analysis

### 4. DatabasePerformanceTest.kt
Tests the performance of database operations:
- Single transaction insertion
- Batch transaction insertion
- Transaction queries by user, date range, category, type
- Total calculations
- Balance calculations
- Currency usage tracking
- User queries
- Complex queries with joins
- Concurrent database operations
- Large dataset queries
- Database transaction performance

### 5. CurrencyUIPerformanceTest.kt
Tests the performance of UI-related currency operations:
- Currency formatting performance
- Currency search performance

### 6. CurrencyOperationsLoadTest.kt
Load tests for currency operations:
- High-frequency conversions
- Large dataset operations
- Memory-intensive operations
- Concurrent operations simulation

## Running the Tests

### Prerequisites
- Android device or emulator
- AndroidX Microbenchmark library (already configured in build.gradle.kts)

### Running Individual Test Classes
```bash
# Run all performance tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.performance.CurrencyConversionPerformanceTest

# Run specific test class
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.performance.CurrencySortingPerformanceTest

# Run all performance tests
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=com.pennywise.app.performance
```

### Running from Android Studio
1. Open the test file in Android Studio
2. Right-click on the test class or method
3. Select "Run [TestName]"
4. Choose a connected device or emulator

## Performance Baselines

The tests establish performance baselines for:
- Currency conversion operations: < 100ms for cached operations
- Database queries: < 50ms for single queries
- UI formatting: < 10ms for formatting operations
- Cache operations: < 5ms for cache hits
- Concurrent operations: Maintain performance under load

## Monitoring Performance

The tests provide detailed metrics including:
- Average execution time
- Throughput (operations per second)
- Memory usage
- Cache hit/miss ratios
- Error rates under load

## Best Practices

1. **Run tests on real devices** for accurate performance measurements
2. **Use release builds** for performance testing
3. **Monitor memory usage** during intensive operations
4. **Test under different load conditions** to identify bottlenecks
5. **Compare results** across different device specifications

## Troubleshooting

### Common Issues
1. **Tests fail to run**: Ensure device/emulator is connected and USB debugging is enabled
2. **Performance varies**: Run tests multiple times and take averages
3. **Memory issues**: Monitor memory usage and optimize data structures
4. **Network timeouts**: Mock network calls for consistent testing

### Debugging
- Use Android Studio's Profiler to analyze performance
- Check logcat for error messages
- Verify test data setup is correct
- Ensure proper cleanup in @After methods
