#!/bin/bash

# Stop Android emulator and cleanup

set -e

echo "Stopping Android emulator..."

# Kill emulator process if PID file exists
if [ -f emulator.pid ]; then
    EMULATOR_PID=$(cat emulator.pid)
    echo "Stopping emulator process: $EMULATOR_PID"
    kill $EMULATOR_PID 2>/dev/null || true
    rm emulator.pid
fi

# Kill any remaining emulator processes
echo "Killing any remaining emulator processes..."
pkill -f emulator || true

# Clean up emulator logs
if [ -f emulator.log ]; then
    echo "Cleaning up emulator logs..."
    rm emulator.log
fi

echo "Emulator stopped and cleaned up!"
