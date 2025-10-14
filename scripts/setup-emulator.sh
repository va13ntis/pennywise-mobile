#!/bin/bash

# Setup script for Android emulator in CI/CD environments
# This script configures an AVD with settings compatible with headless CI environments

set -e

echo "Setting up Android emulator for CI/CD..."

# Get Android SDK path
ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-$ANDROID_HOME}
if [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "Error: ANDROID_SDK_ROOT or ANDROID_HOME not set"
    exit 1
fi

echo "Using Android SDK: $ANDROID_SDK_ROOT"

# AVD configuration
AVD_NAME="test"
AVD_TARGET="system-images;android-29;google_apis;arm64-v8a"
AVD_ABI="arm64-v8a"

# Install required system image if not present
echo "Installing required system image..."
$ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager "$AVD_TARGET" --verbose

# Create AVD if it doesn't exist
echo "Creating AVD: $AVD_NAME"
echo "no" | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/avdmanager create avd \
    --name $AVD_NAME \
    --package $AVD_TARGET \
    --abi $AVD_ABI \
    --force

# Configure AVD for CI environment
AVD_CONFIG="$HOME/.android/avd/${AVD_NAME}.avd/config.ini"

echo "Configuring AVD for CI environment..."
cat >> "$AVD_CONFIG" << EOF

# CI/CD optimizations
hw.cpu.ncore=2
hw.ramSize=2048M
hw.heapSize=512M
hw.gpu.enabled=no
hw.gpu.mode=off
hw.keyboard=no
hw.mainKeys=no
hw.dPad=no
hw.trackBall=no
hw.camera.back=none
hw.camera.front=none
vm.heapSize=256
hw.sensors.orientation=no
hw.sensors.proximity=no
hw.sensors.light=no
hw.sensors.pressure=no
hw.sensors.humidity=no
hw.sensors.magnetic_field=no
hw.sensors.accelerometer=no
hw.sensors.gyroscope=no
hw.sensors.rotation_vector=no
hw.sensors.gravity=no
hw.sensors.linear_acceleration=no
hw.sensors.magnetic_field_uncalibrated=no
hw.sensors.gyroscope_uncalibrated=no
hw.sensors.game_rotation_vector=no
hw.sensors.significant_motion=no
hw.sensors.step_detector=no
hw.sensors.step_counter=no
hw.sensors.geomagnetic_rotation_vector=no
hw.sensors.heart_rate=no
hw.sensors.tilt_detector=no
hw.sensors.wake_gesture=no
hw.sensors.glance_gesture=no
hw.sensors.pick_up_gesture=no
hw.sensors.wrist_tilt_gesture=no
hw.sensors.device_orientation=no
hw.sensors.pose_6dof=no
hw.sensors.stationary_detect=no
hw.sensors.motion_detect=no
hw.sensors.heart_beat=no
hw.sensors.multicam=no
hw.sensors.fold=no
hw.sensors.hinge_angle=no
hw.sensors.hinge=no
hw.sensors.lid_switch=no
hw.sensors.table_mode=no
hw.sensors.book_mode=no
hw.sensors.clamshell_mode=no
hw.sensors.convertible_mode=no
hw.sensors.laptop_mode=no
hw.sensors.stand_mode=no
hw.sensors.tent_mode=no
EOF

echo "AVD setup completed successfully!"
echo "AVD Name: $AVD_NAME"
echo "Config: $AVD_CONFIG"
