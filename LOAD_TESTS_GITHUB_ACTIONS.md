# Load Tests in GitHub Actions

## Problem
The original load tests were instrumentation tests (`androidTest`) that required:
- Connected Android device/emulator
- UI interaction capabilities
- Complex Android testing setup

These don't work well in GitHub Actions without significant setup and are slow to run.

## Solution
Created **unit test versions** of the load tests that can run in GitHub Actions:

### ✅ **What Works in GitHub Actions:**

1. **Unit Test Load Tests** (`app/src/test/java/com/pennywise/app/performance/`):
   - `CurrencyOperationsLoadTestUnit.kt` - Performance tests for currency conversion operations
   - `CurrencyUIPerformanceTestUnit.kt` - Performance tests for currency-related operations
   - `MockCurrencyConversionService.kt` - Mock service for testing

2. **Key Benefits:**
   - ✅ Run in GitHub Actions without Android device
   - ✅ Fast execution (seconds instead of minutes)
   - ✅ Test core business logic performance
   - ✅ Can run on any CI environment
   - ✅ No complex setup required

### ⚠️ **What Still Requires Android Setup:**

1. **Original Instrumentation Tests** (`app/src/androidTest/java/com/pennywise/app/performance/`):
   - `CurrencyOperationsLoadTest.kt` - Full UI-based load tests
   - `CurrencyUIPerformanceTest.kt` - UI component performance tests

2. **Requirements for Instrumentation Tests:**
   - Connected Android device/emulator
   - Android SDK setup in CI
   - Slower execution (minutes)

## GitHub Actions Workflow

The `.github/workflows/test.yml` file includes:

### Unit Tests Job (Fast - ~2-3 minutes):
```yaml
unit-tests:
  runs-on: ubuntu-latest
  steps:
    - Run unit tests
    - Run load tests (unit test versions)
    - Upload test results
```

### Instrumentation Tests Job (Slow - ~10-15 minutes):
```yaml
instrumentation-tests:
  runs-on: macos-latest
  steps:
    - Setup Android SDK
    - Run instrumentation tests
    - Upload test results
```

## Test Coverage

### Unit Test Load Tests Cover:
- ✅ High-frequency currency conversions
- ✅ Large dataset operations
- ✅ Concurrent operations
- ✅ Cache performance
- ✅ Stress testing
- ✅ Performance regression testing
- ✅ Currency enum operations
- ✅ Currency validation
- ✅ Currency search performance
- ✅ Currency sorting performance

### Instrumentation Tests Cover (when Android device available):
- ✅ UI component performance
- ✅ End-to-end load testing
- ✅ Real device performance
- ✅ UI interaction performance

## Running Tests Locally

### Unit Tests (Fast):
```bash
./gradlew testDebugUnitTest --tests="*Performance*"
```

### Instrumentation Tests (Requires device):
```bash
./gradlew connectedAndroidTest
```

## Recommendations

1. **For GitHub Actions**: Use the unit test versions for fast CI feedback
2. **For Local Development**: Run both unit and instrumentation tests
3. **For Release**: Run full instrumentation test suite on real devices

## Performance Thresholds

The unit tests use reduced thresholds for faster execution:
- `MAX_CONVERSION_TIME_MS = 1000L` (vs 2000L in instrumentation)
- `MIN_SUCCESS_RATE = 0.8` (vs 0.5 in instrumentation)
- Smaller test datasets for faster execution

This ensures the tests run quickly in CI while still validating performance characteristics.
