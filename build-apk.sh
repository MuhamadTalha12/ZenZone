#!/bin/bash

echo "========================================"
echo "ZenZone APK Builder"
echo "========================================"
echo ""

echo "Cleaning previous builds..."
./gradlew clean
echo ""

echo "Building Debug APK..."
./gradlew assembleDebug
echo ""

if [ $? -eq 0 ]; then
    echo "========================================"
    echo "BUILD SUCCESSFUL!"
    echo "========================================"
    echo ""
    echo "APK Location: app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "You can now transfer this APK to any Android device."
    echo ""
    echo "To install:"
    echo "1. Enable 'Unknown Sources' in device settings"
    echo "2. Transfer the APK file to your device"
    echo "3. Open the APK file and tap Install"
    echo ""
else
    echo "========================================"
    echo "BUILD FAILED!"
    echo "========================================"
    echo ""
    echo "Please check the error messages above."
    echo ""
fi
