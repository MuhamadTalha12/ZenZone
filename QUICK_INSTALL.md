# ZenZone Quick Install Reference

## 🚀 3-Second Installation (Pick One)

### **Option A: Windows Batch (Easiest)**
```bash
install.bat
# Then follow menu prompts
```

### **Option B: PowerShell (More Features)**
```powershell
Set-ExecutionPolicy -ExecutionPolicy Bypass -Scope Process
.\install.ps1
```

### **Option C: Manual ADB (Fastest if already connected)**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 Pre-Flight Checklist

Before installing, ensure:

- [ ] **Device Connected**: `adb devices` shows your device
- [ ] **USB Debugging ON**: Settings → Developer Options → USB Debugging
- [ ] **Unknown Sources ON**: Settings → Security/Privacy → Install unknown apps

---

## 🔧 Common Commands

### Install
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Install & Launch
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk && adb shell am start -n com.zenzone.app/.ui.splash.SplashActivity
```

### Uninstall
```bash
adb uninstall com.zenzone.app
```

### Clear Data & Cache
```bash
adb shell pm clear com.zenzone.app
```

### View Logs
```bash
adb logcat | findstr ZenZone
```

### Check If Installed
```bash
adb shell pm list packages | findstr zenzone
```

---

## 🆘 If It Fails

### "Package Not Installed"
```bash
adb uninstall com.zenzone.app
adb install app/build/outputs/apk/debug/app-debug.apk
```

### "Device Not Found"
```bash
# Restart ADB
adb kill-server
adb start-server

# Check connection
adb devices
```

### "Permission Denied"
```bash
# Enable USB Debugging on device
# Settings → Developer Options → USB Debugging ON
```

### "App Already Installed"
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# -r flag = reinstall/replace
```

---

## 📊 APK Info

| Property | Value |
|----------|-------|
| **Location** | `app/build/outputs/apk/debug/app-debug.apk` |
| **Size** | 6.12 MB |
| **Architectures** | arm64-v8a, armeabi-v7a, x86, x86_64 |
| **Android Version** | 8.0 - 14 (API 26-34) |
| **Permissions** | VIBRATE, NOTIFICATION_POLICY |

---

## 🔄 Rebuild APK

If you make code changes and need a fresh APK:

```bash
# Option 1: Clean rebuild
./gradlew clean assembleDebug

# Option 2: Just assemble
./gradlew assembleDebug
```

---

## 📺 Testing on Multiple Devices

```bash
# Get all connected devices
adb devices

# Install on specific device
adb -s <device_serial> install app/build/outputs/apk/debug/app-debug.apk

# Example:
adb -s emulator-5554 install app/build/outputs/apk/debug/app-debug.apk
```

---

## 🎯 WiFi Installation (No USB Cable)

```bash
# Enable TCP/IP on device (via USB first)
adb tcpip 5555

# Disconnect USB

# Connect via WiFi
adb connect <device_ip>:5555

# Install as normal
adb install app/build/outputs/apk/debug/app-debug.apk

# When done, reconnect USB and reset
adb usb
```

---

## 💡 Pro Tips

1. **Keep it connected**: Leave device connected while developing
2. **Auto-install**: Use Android Studio's Run button (Shift+F10)
3. **Watch logs in real-time**: `adb logcat` in another terminal
4. **Test on multiple devices**: Use emulator + physical device simultaneously

---

## 📚 Full Documentation

See **[INSTALLATION_GUIDE.md](INSTALLATION_GUIDE.md)** for complete details on:
- All installation methods
- Troubleshooting all errors
- Device compatibility matrix
- Build configuration options
- Permission requirements

---

**TL;DR**: Run `install.bat` and follow prompts! 🎉
