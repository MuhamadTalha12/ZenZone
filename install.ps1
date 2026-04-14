# ZenZone: Automated APK Installation Script (PowerShell)
# Run with PowerShell (supports Windows 7+)

param(
    [Parameter(HelpMessage="Device serial number")]
    [string]$Device = "",
    
    [Parameter(HelpMessage="Install action: install, launch, uninstall, clear")]
    [ValidateSet("install", "launch", "uninstall", "clear")]
    [string]$Action = ""
)

$ErrorActionPreference = "Stop"

# Colors for output
$Colors = @{
    Success = "Green"
    Error   = "Red"
    Warning = "Yellow"
    Info    = "Cyan"
}

function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

function Check-ADB {
    try {
        $null = adb version
        Write-ColorOutput "[OK] ADB is installed" $Colors.Success
        return $true
    } catch {
        Write-ColorOutput "[ERROR] ADB not found in PATH!" $Colors.Error
        Write-ColorOutput "Install Android Studio or add platform-tools to PATH" $Colors.Warning
        return $false
    }
}

function Get-ConnectedDevices {
    $devices = @()
    $output = adb devices | Select-Object -Skip 1
    foreach ($line in $output) {
        if ($line -match "device$") {
            $devices += ($line -split "`t")[0]
        }
    }
    return $devices
}

function Build-APK {
    Write-ColorOutput "Building APK..." $Colors.Info
    $result = & .\gradlew.bat assembleDebug 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "[SUCCESS] APK built successfully" $Colors.Success
        return $true
    } else {
        Write-ColorOutput "[ERROR] Build failed" $Colors.Error
        return $false
    }
}

function Check-APK {
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path $apkPath)) {
        Write-ColorOutput "[WARNING] APK not found, building..." $Colors.Warning
        if (-not (Build-APK)) {
            return $false
        }
    }
    
    if (Test-Path $apkPath) {
        $size = (Get-Item $apkPath).Length / 1MB
        Write-ColorOutput "[OK] APK found: $apkPath ($([math]::Round($size, 2)) MB)" $Colors.Success
        return $true
    }
    return $false
}

function Install-App {
    param([string]$Device)
    
    $apkPath = "app\build\outputs\apk\debug\app-debug.apk"
    
    if ($Device) {
        Write-ColorOutput "Installing on device: $Device" $Colors.Info
        $result = adb -s $Device install -r $apkPath
    } else {
        Write-ColorOutput "Installing on connected device..." $Colors.Info
        $result = adb install -r $apkPath
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "[SUCCESS] App installed successfully!" $Colors.Success
        return $true
    } else {
        Write-ColorOutput "[ERROR] Installation failed, trying uninstall + reinstall..." $Colors.Warning
        if ($Device) {
            adb -s $Device uninstall com.zenzone.app | Out-Null
            adb -s $Device install $apkPath
        } else {
            adb uninstall com.zenzone.app | Out-Null
            adb install $apkPath
        }
        
        if ($LASTEXITCODE -eq 0) {
            Write-ColorOutput "[SUCCESS] App installed successfully!" $Colors.Success
            return $true
        }
        return $false
    }
}

function Launch-App {
    param([string]$Device)
    
    Write-ColorOutput "Launching app..." $Colors.Info
    if ($Device) {
        adb -s $Device shell am start -n com.zenzone.app/.ui.splash.SplashActivity
    } else {
        adb shell am start -n com.zenzone.app/.ui.splash.SplashActivity
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "[SUCCESS] App launched!" $Colors.Success
        return $true
    }
    return $false
}

function Uninstall-App {
    param([string]$Device)
    
    Write-ColorOutput "Uninstalling app..." $Colors.Info
    if ($Device) {
        adb -s $Device uninstall com.zenzone.app
    } else {
        adb uninstall com.zenzone.app
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "[SUCCESS] App uninstalled!" $Colors.Success
        return $true
    }
    return $false
}

function Clear-AppData {
    param([string]$Device)
    
    Write-ColorOutput "Clearing app data..." $Colors.Info
    if ($Device) {
        adb -s $Device shell pm clear com.zenzone.app
    } else {
        adb shell pm clear com.zenzone.app
    }
    
    if ($LASTEXITCODE -eq 0) {
        Write-ColorOutput "[SUCCESS] App data cleared!" $Colors.Success
        return $true
    }
    return $false
}

function Show-Menu {
    Write-Host "`n" 
    Write-ColorOutput "===========================================" $Colors.Info
    Write-ColorOutput "  ZenZone APK Installation" $Colors.Info
    Write-ColorOutput "===========================================" $Colors.Info
    Write-Host "`nOptions:"
    Write-Host "  1) Install APK"
    Write-Host "  2) Install and Launch"
    Write-Host "  3) Uninstall"
    Write-Host "  4) Clear app data"
    Write-Host "  5) View app logs"
    Write-Host "  6) Exit"
    Write-Host ""
}

# Main Execution
Write-ColorOutput "ZenZone APK Installation Script" $Colors.Info
Write-Host ""

# Check ADB
if (-not (Check-ADB)) {
    exit 1
}

# Check APK
if (-not (Check-APK)) {
    exit 1
}

Write-Host ""

# Get devices
$devices = Get-ConnectedDevices
if ($devices.Count -eq 0) {
    Write-ColorOutput "[ERROR] No Android devices detected!" $Colors.Error
    Write-ColorOutput "Please connect your device and enable USB Debugging" $Colors.Warning
    exit 1
}

if ($devices.Count -eq 1) {
    $selectedDevice = $devices[0]
    Write-ColorOutput "[OK] Device found: $selectedDevice" $Colors.Success
} else {
    Write-Host "Multiple devices found:"
    for ($i = 0; $i -lt $devices.Count; $i++) {
        Write-Host "  $($i+1)) $($devices[$i])"
    }
    $choice = Read-Host "Select device (1-$($devices.Count))"
    $selectedDevice = $devices[$choice - 1]
}

Write-Host ""

# If action provided via parameter, execute it
if ($Action) {
    switch ($Action) {
        "install" { Install-App $selectedDevice }
        "launch" { Install-App $selectedDevice; Launch-App $selectedDevice }
        "uninstall" { Uninstall-App $selectedDevice }
        "clear" { Clear-AppData $selectedDevice }
    }
    exit 0
}

# Interactive menu
while ($true) {
    Show-Menu
    $choice = Read-Host "Enter choice (1-6)"
    
    switch ($choice) {
        "1" {
            Install-App $selectedDevice
        }
        "2" {
            Install-App $selectedDevice
            if ($?) { Launch-App $selectedDevice }
        }
        "3" {
            $confirm = Read-Host "Are you sure? (y/n)"
            if ($confirm -eq "y") {
                Uninstall-App $selectedDevice
            }
        }
        "4" {
            Clear-AppData $selectedDevice
        }
        "5" {
            Write-ColorOutput "Showing logs (press Ctrl+C to stop)..." $Colors.Info
            adb -s $selectedDevice logcat | Select-String -Pattern "ZenZone|com.zenzone"
        }
        "6" {
            Write-ColorOutput "Exiting..." $Colors.Info
            exit 0
        }
        default {
            Write-ColorOutput "Invalid choice!" $Colors.Error
        }
    }
    
    Write-Host ""
    Read-Host "Press Enter to continue"
    Clear-Host
}
