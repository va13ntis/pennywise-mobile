@echo off
REM Start Android emulator with CI-optimized settings
REM This script starts the emulator with settings that work in headless CI environments

echo Starting Android emulator...

REM Get Android SDK path
if "%ANDROID_SDK_ROOT%"=="" (
    if "%ANDROID_HOME%"=="" (
        echo Error: ANDROID_SDK_ROOT or ANDROID_HOME not set
        exit /b 1
    ) else (
        set ANDROID_SDK_ROOT=%ANDROID_HOME%
    )
)

REM AVD configuration
set AVD_NAME=test
set EMULATOR_PORT=5554

REM Emulator command with CI-optimized flags
set EMULATOR_CMD=%ANDROID_SDK_ROOT%\emulator\emulator

echo Starting emulator with CI-optimized settings...
echo AVD: %AVD_NAME%
echo Port: %EMULATOR_PORT%

REM Start emulator in background with CI-optimized settings
start /b "%EMULATOR_CMD%" -avd %AVD_NAME% -port %EMULATOR_PORT% -no-window -no-audio -no-boot-anim -no-snapshot -wipe-data -gpu off -accel off -memory 2048 -cores 2 -qemu -enable-hvf=false -qemu -machine virt-accel=kvm:tcg > emulator.log 2>&1

REM Wait for emulator to boot
echo Waiting for emulator to boot...
set timeout=300
set counter=0

:wait_loop
if %counter% geq %timeout% (
    echo Error: Emulator failed to boot within %timeout% seconds
    echo Emulator logs:
    type emulator.log | more +20
    exit /b 1
)

"%ANDROID_SDK_ROOT%\platform-tools\adb.exe" -s emulator-%EMULATOR_PORT% shell getprop sys.boot_completed 2>nul | find "1" >nul
if %errorlevel% equ 0 (
    echo Emulator is ready!
    goto :ready
)

echo Waiting for emulator... (%counter%/%timeout%)
timeout /t 5 /nobreak >nul
set /a counter+=5
goto :wait_loop

:ready
echo Emulator is ready for testing!
echo Device: emulator-%EMULATOR_PORT%
echo Log file: emulator.log
