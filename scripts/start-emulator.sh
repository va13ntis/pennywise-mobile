#!/bin/bash

# Start Android emulator with CI-optimized settings
# This script starts the emulator with settings that work in headless CI environments

set -e

echo "Starting Android emulator..."

# Get Android SDK path
ANDROID_SDK_ROOT=${ANDROID_SDK_ROOT:-$ANDROID_HOME}
if [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "Error: ANDROID_SDK_ROOT or ANDROID_HOME not set"
    exit 1
fi

# AVD configuration
AVD_NAME="test"
EMULATOR_PORT="5554"

# Emulator command with CI-optimized flags
EMULATOR_CMD="$ANDROID_SDK_ROOT/emulator/emulator"

echo "Starting emulator with CI-optimized settings..."
echo "AVD: $AVD_NAME"
echo "Port: $EMULATOR_PORT"

# Start emulator in background with CI-optimized settings
nohup $EMULATOR_CMD \
    -avd $AVD_NAME \
    -port $EMULATOR_PORT \
    -no-window \
    -no-audio \
    -no-boot-anim \
    -no-snapshot \
    -wipe-data \
    -gpu off \
    -accel off \
    -memory 2048 \
    -cores 2 \
    -qemu -enable-hvf=false \
    -qemu -machine virt-accel=tcg \
    -qemu -cpu max \
    > emulator.log 2>&1 &

EMULATOR_PID=$!
echo "Emulator started with PID: $EMULATOR_PID"
echo $EMULATOR_PID > emulator.pid

# Wait for emulator to boot
echo "Waiting for emulator to boot..."
timeout=300  # 5 minutes timeout
counter=0

while [ $counter -lt $timeout ]; do
    if $ANDROID_SDK_ROOT/platform-tools/adb -s emulator-$EMULATOR_PORT shell getprop sys.boot_completed 2>/dev/null | grep -q "1"; then
        echo "Emulator is ready!"
        break
    fi
    
    echo "Waiting for emulator... ($counter/$timeout)"
    sleep 5
    counter=$((counter + 5))
done

if [ $counter -ge $timeout ]; then
    echo "Error: Emulator failed to boot within $timeout seconds"
    echo "Emulator logs:"
    tail -20 emulator.log
    exit 1
fi

echo "Emulator is ready for testing!"
echo "Device: emulator-$EMULATOR_PORT"
echo "Log file: emulator.log"
echo "PID file: emulator.pid"
