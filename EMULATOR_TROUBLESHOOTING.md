# Android Emulator Troubleshooting Guide

This guide helps resolve common Android emulator issues, particularly in CI/CD environments where hardware virtualization may not be available.

## Problem: HVF (Hardware Virtualization Framework) Errors

### Symptoms
```
HVF error: HV_UNSUPPORTED
qemu-system-aarch64-headless: failed to initialize HVF: Invalid argument
adb: device 'emulator-5554' not found
```

### Root Cause
The emulator is trying to use hardware virtualization (HVF) which is not supported in the current environment, typically in CI/CD runners or certain cloud environments.

## Solutions Implemented

### 1. Emulator Configuration (`gradle.properties`)
Added CI-optimized emulator settings:
```properties
# Disable hardware acceleration for CI environments
android.emulator.accel=off
# Use software rendering
android.emulator.gpu=off
# Set emulator timeout
android.emulator.timeout=600000
# Disable audio for headless environments
android.emulator.audio=off
# Use no window mode for CI
android.emulator.window=off
```

### 2. Test Configuration (`app/build.gradle.kts`)
Updated test options for better emulator compatibility:
```kotlin
testOptions {
    unitTests.isIncludeAndroidResources = true
    // Configure instrumented tests for emulator compatibility
    animationsDisabled = true
    execution = "ANDROIDX_TEST_ORCHESTRATOR"
}
```

### 3. Emulator Setup Scripts
Created automated scripts for emulator configuration:

#### Windows (`scripts/setup-emulator.bat`)
- Creates AVD with CI-optimized settings
- Disables all hardware sensors and cameras
- Configures minimal resource usage

#### Linux/macOS (`scripts/setup-emulator.sh`)
- Same functionality as Windows version
- Uses Unix-compatible commands

### 4. Emulator Launch Scripts
Created scripts to start emulator with proper flags:

#### Windows (`scripts/start-emulator.bat`)
```batch
emulator -avd test -port 5554 -no-window -no-audio -no-boot-anim -no-snapshot -wipe-data -gpu off -accel off -memory 2048 -cores 2 -qemu -enable-hvf=false -qemu -machine virt-accel=kvm:tcg
```

#### Linux/macOS (`scripts/start-emulator.sh`)
```bash
emulator -avd test -port 5554 -no-window -no-audio -no-boot-anim -no-snapshot -wipe-data -gpu off -accel off -memory 2048 -cores 2 -qemu -enable-hvf=false -qemu -machine virt-accel=kvm:tcg
```

## Usage Instructions

### For CI/CD Environments

1. **Setup Emulator** (run once):
   ```bash
   # Windows
   scripts\setup-emulator.bat
   
   # Linux/macOS
   ./scripts/setup-emulator.sh
   ```

2. **Start Emulator** (before running tests):
   ```bash
   # Windows
   scripts\start-emulator.bat
   
   # Linux/macOS
   ./scripts/start-emulator.sh
   ```

3. **Run UI Tests**:
   ```bash
   ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyUiTestSuite
   ```

4. **Stop Emulator** (after tests):
   ```bash
   # Windows
   scripts\stop-emulator.bat
   
   # Linux/macOS
   ./scripts/stop-emulator.sh
   ```

### For Local Development

1. **Use Android Studio AVD Manager**:
   - Create new AVD with API 29 (Android 10)
   - Select `google_apis/arm64-v8a` system image
   - Configure with minimal hardware features

2. **Or use command line**:
   ```bash
   # Setup
   scripts\setup-emulator.bat  # Windows
   ./scripts/setup-emulator.sh  # Linux/macOS
   
   # Start
   scripts\start-emulator.bat  # Windows
   ./scripts/start-emulator.sh  # Linux/macOS
   ```

## Alternative Testing Strategies

### 1. Robolectric Tests (No Emulator Required)
For unit tests that don't require a real device:
```bash
./gradlew test
```

### 2. Mock UI Tests
Use Compose testing framework with mocked dependencies:
```kotlin
@ComposeTest
@Test
fun testCurrencySelection() {
    // Test with mocked ViewModels and repositories
}
```

### 3. Firebase Test Lab
For cloud-based testing without local emulator setup:
- Upload APK to Firebase Test Lab
- Run tests on real devices in the cloud
- No local emulator configuration required

### 4. GitHub Actions with Emulator
If using GitHub Actions, ensure the workflow includes:
```yaml
- name: Setup Android SDK
  uses: android-actions/setup-android@v3
  
- name: Setup Emulator
  run: scripts/setup-emulator.sh
  
- name: Start Emulator
  run: scripts/start-emulator.sh &
  
- name: Wait for Emulator
  run: adb wait-for-device
  
- name: Run UI Tests
  run: ./gradlew connectedAndroidTest
```

## Troubleshooting Common Issues

### Issue: "Emulator failed to boot within timeout"
**Solution**: Increase timeout in `scripts/start-emulator.sh`:
```bash
timeout=600  # Increase from 300 to 600 seconds
```

### Issue: "AVD creation failed"
**Solution**: Ensure Android SDK is properly installed:
```bash
# Check SDK installation
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --list

# Install required system image
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager "system-images;android-29;google_apis;arm64-v8a"
```

### Issue: "adb: device not found"
**Solution**: Check emulator status:
```bash
adb devices
adb -s emulator-5554 shell getprop sys.boot_completed
```

### Issue: "Out of memory"
**Solution**: Reduce emulator memory allocation:
```bash
# In scripts/start-emulator.sh, change:
-memory 1024  # Instead of 2048
```

## Performance Optimization

### For CI/CD
- Use `-no-snapshot` flag for fresh state
- Use `-wipe-data` to avoid state corruption
- Disable animations with `animationsDisabled = true`

### For Local Development
- Use snapshots for faster startup: remove `-no-snapshot`
- Keep data between runs: remove `-wipe-data`
- Enable animations for better UX testing

## Monitoring and Debugging

### Emulator Logs
Check `emulator.log` for detailed error information:
```bash
tail -f emulator.log
```

### ADB Commands
Useful debugging commands:
```bash
# List devices
adb devices

# Check boot status
adb -s emulator-5554 shell getprop sys.boot_completed

# View device logs
adb -s emulator-5554 logcat

# Install app
adb -s emulator-5554 install app/build/outputs/apk/debug/app-debug.apk
```

## Best Practices

1. **Always use CI-optimized settings** in automated environments
2. **Test emulator setup locally** before deploying to CI
3. **Use appropriate timeouts** based on your CI environment
4. **Monitor resource usage** and adjust memory/cores as needed
5. **Keep emulator scripts updated** with latest Android SDK changes
6. **Use test orchestrator** for better test isolation
7. **Implement fallback strategies** for environments without emulator support

## Contact and Support

If you continue to experience emulator issues:
1. Check the emulator logs in `emulator.log`
2. Verify Android SDK installation
3. Test with different API levels
4. Consider using Firebase Test Lab for cloud testing
5. Review CI/CD provider documentation for emulator support
