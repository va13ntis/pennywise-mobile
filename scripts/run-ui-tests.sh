#!/bin/bash

# Complete UI test runner script for Linux/macOS
# This script handles emulator setup, test execution, and cleanup

set -e

echo "========================================"
echo "PennyWise UI Test Runner"
echo "========================================"

# Check if Android SDK is available
if [ -z "$ANDROID_SDK_ROOT" ] && [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_SDK_ROOT or ANDROID_HOME not set"
    echo "Please set one of these environment variables"
    exit 1
fi

if [ -z "$ANDROID_SDK_ROOT" ]; then
    export ANDROID_SDK_ROOT="$ANDROID_HOME"
fi

echo "Using Android SDK: $ANDROID_SDK_ROOT"

# Step 1: Setup emulator
echo ""
echo "Step 1: Setting up emulator..."
./scripts/setup-emulator.sh

# Step 2: Start emulator
echo ""
echo "Step 2: Starting emulator..."
./scripts/start-emulator.sh

# Step 3: Wait for emulator to be ready
echo ""
echo "Step 3: Waiting for emulator to be ready..."
sleep 10

# Check if emulator is ready
while ! $ANDROID_SDK_ROOT/platform-tools/adb -s emulator-5554 shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; do
    echo "Still waiting for emulator..."
    sleep 5
done

echo "Emulator is ready!"

# Step 4: Run UI tests
echo ""
echo "Step 4: Running UI tests..."
echo "Running: ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyUiTestSuite"

./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyUiTestSuite
TEST_RESULT=$?

# Step 5: Stop emulator and cleanup
echo ""
echo "Step 5: Stopping emulator and cleaning up..."
./scripts/stop-emulator.sh

# Report results
echo ""
echo "========================================"
if [ $TEST_RESULT -eq 0 ]; then
    echo "UI Tests: PASSED"
else
    echo "UI Tests: FAILED (exit code: $TEST_RESULT)"
fi
echo "========================================"

exit $TEST_RESULT
