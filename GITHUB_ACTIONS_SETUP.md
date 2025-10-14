# GitHub Actions Setup for PennyWise Mobile

This guide explains how to set up GitHub Actions for running UI tests with Android emulators and alternative testing strategies.

## 🚀 Quick Start

The repository includes three GitHub Actions workflows:

1. **`android-tests.yml`** - Complete CI/CD pipeline with unit tests, UI tests, and builds
2. **`ui-tests.yml`** - UI tests only (for quick testing)
3. **`firebase-tests.yml`** - Alternative testing using Firebase Test Lab

## 📋 Prerequisites

### For Local Emulator Testing
- No additional setup required - uses GitHub-hosted runners
- Automatically installs Android SDK and required system images
- Uses macOS runners for emulator compatibility

### For Firebase Test Lab (Optional)
- Google Cloud Project with Firebase Test Lab enabled
- Service Account with Test Lab permissions
- Firebase project with billing enabled

## 🔧 Workflow Configurations

### 1. Main CI/CD Pipeline (`android-tests.yml`)

This is the primary workflow that runs on every push and pull request:

**Jobs:**
- **Unit Tests** (Ubuntu) - Fast unit tests using Robolectric
- **UI Tests** (macOS) - Instrumented tests with Android emulator
- **Build** (Ubuntu) - APK compilation and validation

**Features:**
- ✅ Parallel job execution for faster CI
- ✅ Comprehensive test coverage
- ✅ Automatic artifact uploads
- ✅ Emulator logs on failure
- ✅ 30-minute timeout for UI tests

### 2. UI Tests Only (`ui-tests.yml`)

Lightweight workflow for UI testing only:

**Use Cases:**
- Quick UI test validation
- Debugging emulator issues
- Testing UI changes only

### 3. Firebase Test Lab (`firebase-tests.yml`)

Cloud-based testing alternative:

**Advantages:**
- ✅ Real device testing
- ✅ No emulator setup required
- ✅ Multiple device configurations
- ✅ Reliable and fast execution

**Disadvantages:**
- ❌ Requires Google Cloud setup
- ❌ May incur costs
- ❌ Longer execution time

## 🛠️ Setup Instructions

### Option 1: Use Local Emulator (Recommended)

No additional setup required! The workflows will automatically:

1. Install Android SDK
2. Create and configure emulator
3. Run UI tests
4. Upload results

### Option 2: Firebase Test Lab Setup

If you prefer cloud-based testing:

1. **Create Google Cloud Project:**
   ```bash
   gcloud projects create your-project-id
   gcloud config set project your-project-id
   ```

2. **Enable Firebase Test Lab:**
   ```bash
   gcloud services enable testing.googleapis.com
   gcloud services enable toolresults.googleapis.com
   ```

3. **Create Service Account:**
   ```bash
   gcloud iam service-accounts create github-actions
   gcloud projects add-iam-policy-binding your-project-id \
     --member="serviceAccount:github-actions@your-project-id.iam.gserviceaccount.com" \
     --role="roles/test.serviceUser"
   ```

4. **Create and download service account key:**
   ```bash
   gcloud iam service-accounts keys create key.json \
     --iam-account=github-actions@your-project-id.iam.gserviceaccount.com
   ```

5. **Add GitHub Secrets:**
   - `GCP_SA_KEY`: Contents of `key.json`
   - `FIREBASE_RESULTS_BUCKET`: Your GCS bucket name

## 🎯 Emulator Configuration

The workflows use optimized emulator settings for CI environments:

### Key Optimizations
```bash
# Disable hardware acceleration
-qemu -enable-hvf=false
-qemu -machine virt-accel=tcg

# Software rendering
-gpu off
-accel off

# Minimal resources
-memory 2048
-cores 2

# Headless mode
-no-window
-no-audio
-no-boot-anim
```

### System Image
- **API Level:** 29 (Android 10)
- **Architecture:** arm64-v8a
- **Type:** google_apis (Google Play Services)

## 📊 Test Results and Artifacts

### Artifacts Generated
- **Unit Test Results** - JUnit XML reports
- **UI Test Results** - Espresso test reports
- **Coverage Reports** - JaCoCo HTML reports
- **APK Files** - Debug and release builds
- **Emulator Logs** - Debug information on failure

### Accessing Results
1. Go to your repository's **Actions** tab
2. Click on the workflow run
3. Download artifacts from the **Artifacts** section

## 🔍 Troubleshooting

### Common Issues

#### 1. Emulator Fails to Start
**Symptoms:**
```
HVF error: HV_UNSUPPORTED
qemu-system-aarch64-headless: failed to initialize HVF
```

**Solution:**
- The workflow automatically handles this with software rendering
- Check emulator logs in artifacts if issues persist

#### 2. Tests Timeout
**Symptoms:**
```
Error: Emulator failed to boot within 300 seconds
```

**Solutions:**
- Increase timeout in workflow (currently 30 minutes)
- Check if system image is properly installed
- Verify emulator configuration

#### 3. ADB Device Not Found
**Symptoms:**
```
adb: device 'emulator-5554' not found
```

**Solutions:**
- Check emulator startup logs
- Verify emulator port configuration
- Ensure emulator fully booted before running tests

### Debug Commands

Add these steps to your workflow for debugging:

```yaml
- name: Debug Emulator Status
  run: |
    echo "=== ADB Devices ==="
    $ANDROID_SDK_ROOT/platform-tools/adb devices
    echo "=== Emulator Logs ==="
    tail -20 emulator.log
    echo "=== System Properties ==="
    $ANDROID_SDK_ROOT/platform-tools/adb -s emulator-5554 shell getprop
```

## 🚀 Performance Optimization

### For Faster CI
1. **Use unit tests for quick feedback** - Run unit tests first
2. **Parallel jobs** - Run unit tests and builds in parallel
3. **Cache dependencies** - Gradle packages are cached
4. **Skip UI tests on documentation changes** - Use path filters

### Example Path Filters
```yaml
on:
  push:
    paths-ignore:
      - '**.md'
      - 'docs/**'
  pull_request:
    paths-ignore:
      - '**.md'
      - 'docs/**'
```

## 📈 Monitoring and Metrics

### Key Metrics to Track
- **Test Execution Time** - Should be < 15 minutes
- **Success Rate** - Aim for > 95%
- **Flaky Test Rate** - Should be < 5%
- **Resource Usage** - Monitor runner resource consumption

### GitHub Actions Insights
- Go to **Insights** > **Actions** in your repository
- Monitor workflow performance and success rates
- Set up notifications for workflow failures

## 🔐 Security Considerations

### Secrets Management
- Never commit API keys or credentials
- Use GitHub Secrets for sensitive data
- Rotate service account keys regularly

### Runner Security
- Use GitHub-hosted runners for public repositories
- Consider self-hosted runners for private repositories
- Regularly update runner images

## 📚 Additional Resources

- [Android Testing Guide](https://developer.android.com/training/testing)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Firebase Test Lab Documentation](https://firebase.google.com/docs/test-lab)
- [Emulator Troubleshooting Guide](./EMULATOR_TROUBLESHOOTING.md)

## 🤝 Contributing

When adding new workflows or modifying existing ones:

1. Test changes in a feature branch first
2. Ensure workflows run successfully
3. Update this documentation
4. Consider performance impact
5. Add appropriate error handling

## 📞 Support

If you encounter issues:

1. Check the troubleshooting section above
2. Review workflow logs and artifacts
3. Consult the emulator troubleshooting guide
4. Create an issue with detailed logs and error messages
