@echo off
REM Complete UI test runner script for Windows
REM This script handles emulator setup, test execution, and cleanup

echo ========================================
echo PennyWise UI Test Runner
echo ========================================

REM Check if Android SDK is available
if "%ANDROID_SDK_ROOT%"=="" (
    if "%ANDROID_HOME%"=="" (
        echo Error: ANDROID_SDK_ROOT or ANDROID_HOME not set
        echo Please set one of these environment variables
        exit /b 1
    ) else (
        set ANDROID_SDK_ROOT=%ANDROID_HOME%
    )
)

echo Using Android SDK: %ANDROID_SDK_ROOT%

REM Step 1: Setup emulator
echo.
echo Step 1: Setting up emulator...
call scripts\setup-emulator.bat
if %errorlevel% neq 0 (
    echo Error: Failed to setup emulator
    exit /b 1
)

REM Step 2: Start emulator
echo.
echo Step 2: Starting emulator...
call scripts\start-emulator.bat
if %errorlevel% neq 0 (
    echo Error: Failed to start emulator
    exit /b 1
)

REM Step 3: Wait for emulator to be ready
echo.
echo Step 3: Waiting for emulator to be ready...
timeout /t 10 /nobreak >nul

REM Check if emulator is ready
:check_ready
"%ANDROID_SDK_ROOT%\platform-tools\adb.exe" -s emulator-5554 shell getprop sys.boot_completed 2>nul | find "1" >nul
if %errorlevel% neq 0 (
    echo Still waiting for emulator...
    timeout /t 5 /nobreak >nul
    goto :check_ready
)

echo Emulator is ready!

REM Step 4: Run UI tests
echo.
echo Step 4: Running UI tests...
echo Running: gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyUiTestSuite

gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pennywise.app.CurrencyUiTestSuite
set TEST_RESULT=%errorlevel%

REM Step 5: Stop emulator and cleanup
echo.
echo Step 5: Stopping emulator and cleaning up...
call scripts\stop-emulator.bat

REM Report results
echo.
echo ========================================
if %TEST_RESULT% equ 0 (
    echo UI Tests: PASSED
) else (
    echo UI Tests: FAILED (exit code: %TEST_RESULT%)
)
echo ========================================

exit /b %TEST_RESULT%
