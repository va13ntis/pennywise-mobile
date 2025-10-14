@echo off
REM Setup script for Android emulator in CI/CD environments
REM This script configures an AVD with settings compatible with headless CI environments

echo Setting up Android emulator for CI/CD...

REM Get Android SDK path
if "%ANDROID_SDK_ROOT%"=="" (
    if "%ANDROID_HOME%"=="" (
        echo Error: ANDROID_SDK_ROOT or ANDROID_HOME not set
        exit /b 1
    ) else (
        set ANDROID_SDK_ROOT=%ANDROID_HOME%
    )
)

echo Using Android SDK: %ANDROID_SDK_ROOT%

REM AVD configuration
set AVD_NAME=test
set AVD_TARGET=system-images;android-29;google_apis;arm64-v8a
set AVD_ABI=arm64-v8a

REM Create AVD if it doesn't exist
echo Creating AVD: %AVD_NAME%
echo no | "%ANDROID_SDK_ROOT%\cmdline-tools\latest\bin\avdmanager.bat" create avd --name %AVD_NAME% --package %AVD_TARGET% --abi %AVD_ABI% --force

REM Configure AVD for CI environment
set AVD_CONFIG=%USERPROFILE%\.android\avd\%AVD_NAME%.avd\config.ini

echo Configuring AVD for CI environment...
echo. >> "%AVD_CONFIG%"
echo # CI/CD optimizations >> "%AVD_CONFIG%"
echo hw.cpu.ncore=2 >> "%AVD_CONFIG%"
echo hw.ramSize=2048M >> "%AVD_CONFIG%"
echo hw.heapSize=512M >> "%AVD_CONFIG%"
echo hw.gpu.enabled=no >> "%AVD_CONFIG%"
echo hw.gpu.mode=off >> "%AVD_CONFIG%"
echo hw.keyboard=no >> "%AVD_CONFIG%"
echo hw.mainKeys=no >> "%AVD_CONFIG%"
echo hw.dPad=no >> "%AVD_CONFIG%"
echo hw.trackBall=no >> "%AVD_CONFIG%"
echo hw.camera.back=none >> "%AVD_CONFIG%"
echo hw.camera.front=none >> "%AVD_CONFIG%"
echo vm.heapSize=256 >> "%AVD_CONFIG%"
echo hw.sensors.orientation=no >> "%AVD_CONFIG%"
echo hw.sensors.proximity=no >> "%AVD_CONFIG%"
echo hw.sensors.light=no >> "%AVD_CONFIG%"
echo hw.sensors.pressure=no >> "%AVD_CONFIG%"
echo hw.sensors.humidity=no >> "%AVD_CONFIG%"
echo hw.sensors.magnetic_field=no >> "%AVD_CONFIG%"
echo hw.sensors.accelerometer=no >> "%AVD_CONFIG%"
echo hw.sensors.gyroscope=no >> "%AVD_CONFIG%"
echo hw.sensors.rotation_vector=no >> "%AVD_CONFIG%"
echo hw.sensors.gravity=no >> "%AVD_CONFIG%"
echo hw.sensors.linear_acceleration=no >> "%AVD_CONFIG%"
echo hw.sensors.magnetic_field_uncalibrated=no >> "%AVD_CONFIG%"
echo hw.sensors.gyroscope_uncalibrated=no >> "%AVD_CONFIG%"
echo hw.sensors.game_rotation_vector=no >> "%AVD_CONFIG%"
echo hw.sensors.significant_motion=no >> "%AVD_CONFIG%"
echo hw.sensors.step_detector=no >> "%AVD_CONFIG%"
echo hw.sensors.step_counter=no >> "%AVD_CONFIG%"
echo hw.sensors.geomagnetic_rotation_vector=no >> "%AVD_CONFIG%"
echo hw.sensors.heart_rate=no >> "%AVD_CONFIG%"
echo hw.sensors.tilt_detector=no >> "%AVD_CONFIG%"
echo hw.sensors.wake_gesture=no >> "%AVD_CONFIG%"
echo hw.sensors.glance_gesture=no >> "%AVD_CONFIG%"
echo hw.sensors.pick_up_gesture=no >> "%AVD_CONFIG%"
echo hw.sensors.wrist_tilt_gesture=no >> "%AVD_CONFIG%"
echo hw.sensors.device_orientation=no >> "%AVD_CONFIG%"
echo hw.sensors.pose_6dof=no >> "%AVD_CONFIG%"
echo hw.sensors.stationary_detect=no >> "%AVD_CONFIG%"
echo hw.sensors.motion_detect=no >> "%AVD_CONFIG%"
echo hw.sensors.heart_beat=no >> "%AVD_CONFIG%"
echo hw.sensors.multicam=no >> "%AVD_CONFIG%"
echo hw.sensors.fold=no >> "%AVD_CONFIG%"
echo hw.sensors.hinge_angle=no >> "%AVD_CONFIG%"
echo hw.sensors.hinge=no >> "%AVD_CONFIG%"
echo hw.sensors.lid_switch=no >> "%AVD_CONFIG%"
echo hw.sensors.table_mode=no >> "%AVD_CONFIG%"
echo hw.sensors.book_mode=no >> "%AVD_CONFIG%"
echo hw.sensors.clamshell_mode=no >> "%AVD_CONFIG%"
echo hw.sensors.convertible_mode=no >> "%AVD_CONFIG%"
echo hw.sensors.laptop_mode=no >> "%AVD_CONFIG%"
echo hw.sensors.stand_mode=no >> "%AVD_CONFIG%"
echo hw.sensors.tent_mode=no >> "%AVD_CONFIG%"

echo AVD setup completed successfully!
echo AVD Name: %AVD_NAME%
echo Config: %AVD_CONFIG%
