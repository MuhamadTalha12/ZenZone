@echo off
REM ZenZone: Automated APK Installation Script
REM This script automates the process of installing the app on your device

setlocal enabledelayedexpansion
title ZenZone - APK Installation

echo.
echo ============================================
echo  ZenZone APK Installation Script
echo ============================================
echo.

REM Check if ADB is available
adb version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] ADB not found in PATH!
    echo Please install Android Studio or add platform-tools to PATH:
    echo https://developer.android.com/Studio/command-line/adb
    echo.
    pause
    exit /b 1
)

echo [OK] ADB is installed
echo.

REM Check if APK exists
set APK_PATH=app\build\outputs\apk\debug\app-debug.apk
if not exist "%APK_PATH%" (
    echo [ERROR] APK not found at: %APK_PATH%
    echo.
    echo Building APK now...
    echo.
    call gradlew.bat assembleDebug
    echo.
)

if not exist "%APK_PATH%" (
    echo [ERROR] Failed to build APK. Build logs above.
    pause
    exit /b 1
)

echo [OK] APK found: %APK_PATH%
echo.

REM Check connected devices
echo Checking connected devices...
echo.
for /f "skip=1 tokens=1" %%A in ('adb devices') do (
    if not "%%A"=="" (
        echo    Device: %%A
    )
)
echo.

REM Get device count
for /f "skip=1 tokens=1" %%A in ('adb devices') do (
    if not "%%A"=="" set DEVICE=%%A
)

if "!DEVICE!"=="" (
    echo [ERROR] No Android devices detected!
    echo.
    echo Please:
    echo  1. Connect your device via USB
    echo  2. Enable USB Debugging in Developer Options
    echo  3. Run this script again
    echo.
    pause
    exit /b 1
)

echo [OK] Device found: !DEVICE!
echo.

REM Ask for action
echo What would you like to do?
echo  1) Install APK on device
echo  2) Install and Launch app
echo  3) Uninstall app
echo  4) Clear app data
echo.
set /p ACTION="Enter choice (1-4): "

if "!ACTION!"=="1" (
    echo.
    echo Installing APK on device...
    adb install -r "%APK_PATH%"
    if errorlevel 1 (
        echo.
        echo [ERROR] Installation failed!
        echo.
        echo Trying with force uninstall...
        adb uninstall com.zenzone.app
        adb install "%APK_PATH%"
    ) else (
        echo.
        echo [SUCCESS] App installed successfully!
        echo.
    )
) else if "!ACTION!"=="2" (
    echo.
    echo Installing APK on device...
    adb install -r "%APK_PATH%"
    if not errorlevel 1 (
        echo.
        echo [SUCCESS] App installed!
        echo Launching app...
        echo.
        adb shell am start -n com.zenzone.app/.ui.splash.SplashActivity
        echo.
        echo [SUCCESS] App launched!
        echo.
    ) else (
        echo.
        echo [ERROR] Installation failed!
        echo.
    )
) else if "!ACTION!"=="3" (
    echo.
    echo Uninstalling app...
    adb uninstall com.zenzone.app
    if not errorlevel 1 (
        echo [SUCCESS] App uninstalled!
        echo.
    ) else (
        echo [ERROR] Uninstall failed!
        echo.
    )
) else if "!ACTION!"=="4" (
    echo.
    echo Clearing app data...
    adb shell pm clear com.zenzone.app
    if not errorlevel 1 (
        echo [SUCCESS] App data cleared!
        echo.
    ) else (
        echo [ERROR] Clear operation failed!
        echo.
    )
) else (
    echo Invalid choice!
)

echo.
pause
