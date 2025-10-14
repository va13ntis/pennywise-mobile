@echo off
REM Stop Android emulator and cleanup

echo Stopping Android emulator...

REM Kill any emulator processes
echo Killing emulator processes...
taskkill /f /im emulator.exe 2>nul || echo No emulator processes found

REM Kill any qemu processes
echo Killing qemu processes...
taskkill /f /im qemu-system-aarch64-headless.exe 2>nul || echo No qemu processes found

REM Clean up emulator logs
if exist emulator.log (
    echo Cleaning up emulator logs...
    del emulator.log
)

REM Clean up PID file if it exists
if exist emulator.pid (
    del emulator.pid
)

echo Emulator stopped and cleaned up!
