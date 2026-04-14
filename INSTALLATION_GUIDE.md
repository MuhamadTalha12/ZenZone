# ZenZone - Installation Guide

## 📍 APK Location
```
app/build/outputs/apk/debug/app-debug.apk
```

**File Size**: 6.12 MB  
**Supports**: arm64-v8a, armeabi-v7a, x86, x86_64 (Universal - all devices)

---

## 🚀 Installation Methods

### **Method 1: Using ADB (Recommended - Most Reliable)**

#### Requirements:
- Android Device or Emulator
- USB Cable (or WiFi debugging enabled)
- ADB installed (comes with Android Studio)

#### Steps:

**Step 1: Connect Device**
```bash
# For USB connection
adb devices

# For WiFi connection (Android 11+)
adb connect <device-ip-address>:5555
```

**Step 2: Install APK**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Step 3: Launch App**
```bash
adb shell am start -n com.zenzone.app/.ui.splash.SplashActivity
```

---

### **Method 2: Manual File Transfer (via USB)**

#### Steps:

1. **Connect device to computer via USB**
   - Enable USB Debugging on device:
     - Settings → Developer Options → USB Debugging (toggle ON)
     - If Developer Options not visible: Settings → About → tap Build Number 7 times

2. **Transfer APK to device**
   - Connect as File Transfer mode
   - Copy `app-debug.apk` from `app/build/outputs/apk/debug/`
   - Paste to device's **Downloads** folder

3. **Install on device**
   - Open Files/File Manager on device
   - Navigate to **Downloads**
   - Tap `app-debug.apk`
   - Tap **Install**
   - Grant permissions

4. **Launch app**
   - Tap **Open** or find in App Drawer

---

### **Method 3: Using Android Studio (Easiest)**

1. Open Android Studio with the project
2. Click **Run** (green play button)
3. Select connected device
4. Click **OK**

---

## ⚠️ Troubleshooting

### **Error: "Package Not Installed"**

**Cause 1: Unknown Sources Not Enabled**
- Fix: Settings → Security → Unknown Sources → Enable
- For Android 12+: Settings → Apps → Special app access → Install unknown apps → File Manager → Toggle ON

**Cause 2: APK Signature Issues**
```bash
# Uninstall old version first
adb uninstall com.zenzone.app

# Then install
adb install app/build/outputs/apk/debug/app-debug.apk
```

**Cause 3: Device Storage Full**
- Free up space on device
- Clear cache: Settings → Apps → app name → Clear Cache

**Cause 4: Incompatible Android Version**
- Your app requires: **Android 8.0 (API 26) - Android 14 (API 34)**
- Device must be API 26 or higher

---

### **Error: "ADB Device Not Found"**

```bash
# For USB:
1. Check USB cable connection
2. Enable Developer Options & USB Debugging
3. Restart ADB: adb kill-server && adb start-server

# For WiFi:
adb tcpip 5555
adb connect <device-ip>:5555
```

---

### **Error: "INSTALL_FAILED_VERSION_DOWNGRADE"**

```bash
# Device has newer version installed
adb uninstall com.zenzone.app
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

### **Error: "Failed to Install on Device"**

**Check app integrity:**
```bash
# Rebuild fresh
./gradlew clean assembleDebug

# Clear device cache before installing
adb shell pm clear --cache-only com.zenzone.app
```

---

## 🔍 Verify Installation

```bash
# Check if app is installed
adb shell pm list packages | findstr zenzone

# Get app information
adb shell dumpsys package com.zenzone.app | findstr version

# View app logs
adb logcat | findstr ZenZone
```

---

## 📦 Device Compatibility

### ✅ Supported Android Versions
- Android 8.0 Oreo (API 26)
- Android 8.1 Oreo (API 27)
- Android 9 Pie (API 28)
- Android 10 (API 29)
- Android 11 (API 30)
- Android 12 (API 31)
- Android 13 (API 33)
- Android 14 (API 34)

### ✅ Supported CPU Architectures
- **arm64-v8a** (64-bit ARM - Most modern devices)
- **armeabi-v7a** (32-bit ARM - Older devices)
- **x86** (32-bit Intel)
- **x86_64** (64-bit Intel)

---

## 🛠️ Build Commands

### Rebuild APK
```bash
./gradlew clean assembleDebug
```

### Build Release APK
```bash
./gradlew assembleRelease
# Located at: app/build/outputs/apk/release/app-release.apk
```

### Install and Run
```bash
./gradlew installDebug
./gradlew runDebugApp
```

---

## 📱 Quick Commands Reference

| Command | Purpose |
|---------|---------|
| `adb devices` | List connected devices |
| `adb install app-debug.apk` | Install APK |
| `adb uninstall com.zenzone.app` | Uninstall app |
| `adb shell am start -n com.zenzone.app/.ui.splash.SplashActivity` | Launch app |
| `adb logcat` | View live logs |
| `adb shell pm clear com.zenzone.app` | Clear app data |

---

## ✅ Successful Installation Checklist

- [ ] APK built without errors (`BUILD SUCCESSFUL`)
- [ ] APK file exists at `app/build/outputs/apk/debug/app-debug.apk`
- [ ] Device connected (`adb devices` shows device)
- [ ] Unknown Sources enabled on device
- [ ] No previous version conflicting
- [ ] Device storage has space
- [ ] Device is API 26+ (Android 8.0+)

---

## 💡 Pro Tips

1. **For testing on multiple devices:**
   ```bash
   for /F "tokens=*" %a in ('adb devices ^| find "device"') do (
       adb -s %a install app/build/outputs/apk/debug/app-debug.apk
   )
   ```

2. **Monitor installation progress:**
   ```bash
   adb logcat | findstr -i install
   ```

3. **Emulator users**: No unknown sources needed - just run from Android Studio

---

## 📞 Still Having Issues?

1. Check Android Studio Logcat for error messages
2. Rebuild with `./gradlew clean assembleDebug`
3. Try uninstalling completely: `adb uninstall com.zenzone.app`
4. Restart ADB: `adb kill-server && adb start-server`
5. Update Android SDK Platform to API 34

---

**Last Updated**: April 11, 2026  
**App Version**: 1.0  
**Target API**: 34 (Android 14)  
**Min API**: 26 (Android 8.0)
