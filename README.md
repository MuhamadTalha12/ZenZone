# ZenZone - Focus & Productivity App
## Complete Project Documentation

---

## 📱 Project Overview

**ZenZone** is an Android mobile application designed to help users build focus habits and achieve deep work. The app gamifies productivity through **focus chains** (streak tracking), **badges**, **XP rewards**, and **Do Not Disturb (DND) mode** integration.

### Key Features:
- **Focus Session Timer**: Set and track focused work sessions
- **Focus Chains**: Build streaks by completing consecutive focus goals
- **Goal Management**: Create and manage multiple focus goals
- **Statistics Dashboard**: View detailed progress metrics and session history
- **Badge System**: Unlock badges based on achievements
- **Do Not Disturb Integration**: Automatically enable DND during focus sessions
- **User Profiles**: Personalized experience with user data storage
- **Onboarding Flow**: Interactive first-time user setup

---

## 🏗️ Architecture & Design Pattern

### MVVM Architecture (Model-View-ViewModel)

ZenZone follows the **MVVM (Model-View-ViewModel)** architectural pattern:

```
User Interface (Activities & Fragments)
        ↓
    ViewModels (Business Logic & State Management)
        ↓
    Repositories (Data Access Layer)
        ↓
    Models (Data Classes & Utils)
```

---

## 📁 Project Structure

```
zenzone/
├── app/
│   ├── src/main/
│   │   ├── java/com/zenzone/app/
│   │   │   ├── model/               # Data Models
│   │   │   │   ├── FocusSession.kt
│   │   │   │   ├── FocusGoal.kt
│   │   │   │   ├── UserProfile.kt
│   │   │   │   └── ZenBadge.kt
│   │   │   │
│   │   │   ├── viewmodel/           # ViewModels (State Management)
│   │   │   │   ├── FocusViewModel.kt
│   │   │   │   ├── HomeViewModel.kt
│   │   │   │   ├── StatsViewModel.kt
│   │   │   │   └── ProfileViewModel.kt
│   │   │   │
│   │   │   ├── repository/          # Data Layer
│   │   │   │   ├── FocusRepository.kt
│   │   │   │   └── UserRepository.kt
│   │   │   │
│   │   │   ├── ui/                  # UI Components
│   │   │   │   ├── main/
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── OnboardingAdapter.kt
│   │   │   │   │   └── SplashActivity.kt
│   │   │   │   ├── focus/
│   │   │   │   │   └── FocusFragment.kt
│   │   │   │   ├── home/
│   │   │   │   │   └── HomeFragment.kt
│   │   │   │   ├── stats/
│   │   │   │   │   ├── StatsFragment.kt
│   │   │   │   │   └── SessionHistoryAdapter.kt
│   │   │   │   └── profile/
│   │   │   │       └── ProfileFragment.kt
│   │   │   │
│   │   │   └── utils/               # Helper Utilities
│   │   │       ├── Constants.kt
│   │   │       ├── DateUtils.kt
│   │   │       ├── ChainCalculator.kt
│   │   │       ├── DndHelper.kt
│   │   │       ├── FocusLockHelper.kt
│   │   │       └── JsonStorageHelper.kt
│   │   │
│   │   └── res/                     # Resources
│   │       ├── layout/              # XML Layouts
│   │       ├── drawable/            # Images & Drawables
│   │       ├── values/              # Strings, Colors, Styles
│   │       └── menu/                # Menu Definitions
│   │
│   ├── build.gradle.kts             # App Dependencies
│   └── proguard-rules.pro           # ProGuard Rules (Minification)
│
├── gradle/
│   ├── libs.versions.toml           # Centralized Dependency Versions
│   └── wrapper/                     # Gradle Wrapper
│
├── build.gradle.kts                 # Root Project Config
└── settings.gradle.kts              # Project Settings

```

---

## 🔧 Technology Stack & Dependencies

### Core Android Framework
| Dependency | Version | Purpose |
|-----------|---------|---------|
| **androidx.appcompat** | 1.6.1 | Backward compatibility & Material components |
| **androidx.core-ktx** | 1.12.0 | Kotlin extensions for Android APIs |
| **androidx.constraintlayout** | 2.1.4 | Advanced layouts for responsive UI |

### Architecture & Lifecycle
| Dependency | Version | Purpose |
|-----------|---------|---------|
| **androidx.lifecycle-viewmodel-ktx** | 2.7.0 | ViewModel for MVVM pattern |
| **androidx.lifecycle-livedata-ktx** | 2.7.0 | LiveData for reactive state management |
| **androidx.fragment-ktx** | 1.6.2 | Fragment management & lifecycle handling |

### UI Components
| Dependency | Version | Purpose |
|-----------|---------|---------|
| **com.google.android.material** | 1.11.0 | Material Design 3 components (buttons, input fields, bottom nav) |
| **androidx.viewpager2** | (via AndroidX) | Supports onboarding carousel |
| **BottomNavigationView** | (via Material) | Bottom navigation between screens |

### Data & Serialization
| Dependency | Version | Purpose |
|-----------|---------|---------|
| **com.google.code.gson** | 2.10.1 | JSON serialization for storing focus sessions & user data |

### Concurrency
| Dependency | Version | Purpose |
|-----------|---------|---------|
| **org.jetbrains.kotlinx.coroutines-android** | 1.7.3 | Asynchronous operations & threading |

### Testing
| Dependency | Purpose |
|-----------|---------|
| **JUnit** | Unit testing framework |
| **androidx.test.junit** | Android JUnit extensions |
| **androidx.test.espresso** | UI testing framework |

### Build Configuration
- **AGP (Android Gradle Plugin)**: 9.0.1
- **Gradle**: 9+ (wrapped)
- **Target SDK**: 34 (Android 14)
- **Min SDK**: 26 (Android 8.0)
- **Compile SDK**: 34
- **Language**: Kotlin (100%)
- **Java Compatibility**: Java 11

---

## 📋 Core Components Explained

### 1. **Models** (`model/` package)

Data classes representing core entities:

#### `FocusSession.kt`
```kotlin
data class FocusSession(
    val id: String,
    val goalId: String,
    val goalName: String,
    val durationMinutes: Int,
    val completedAt: String,
    val wasChainSaved: Boolean
)
```
Represents a completed focus work session.

#### `FocusGoal.kt`
Represents a focus goal with target duration and metadata.

#### `UserProfile.kt`
Stores user information: name, XP, badges, current chain, etc.

#### `ZenBadge.kt`
Represents achievement badges (e.g., "10-Day Streak", "1000 Minutes Focused").

---

### 2. **ViewModels** (`viewmodel/` package)

Handle business logic and state management using **MVVM + LiveData**:

#### `FocusViewModel.kt`
- Manages focus session countdown timer
- Calculates chain updates and XP rewards
- Triggers Do Not Disturb mode
- Emits `FocusEvent` for session completion
- **Key Methods**: `startSession()`, `completeSession()`, `pauseSession()`

#### `HomeViewModel.kt`
- Displays user dashboard/home screen
- Shows current chain status

#### `StatsViewModel.kt`
- Fetches and displays focus statistics
- Manages session history

#### `ProfileViewModel.kt`
- Manages user profile data
- Handles badge management

**Why LiveData?**
- Observes data changes reactively
- Bound to UI lifecycle (prevents memory leaks)
- Updates UI automatically when data changes

---

### 3. **Repositories** (`repository/` package)

Data access layer abstracting storage operations:

#### `FocusRepository.kt`
- CRUD operations for focus sessions
- Stored using `JsonStorageHelper` (JSON files)
- Methods: `saveFocusSession()`, `getFocusSessions()`, `getFocusGoals()`

#### `UserRepository.kt`
- Manages user profile persistence
- Stores/retrieves user data
- Methods: `saveUserProfile()`, `getUserProfile()`, `updateChain()`

**Why Repositories?**
- Separates data logic from UI
- Easy to swap data sources (SharedPreferences → Database → API)
- Improves testability

---

### 4. **UI Components** (`ui/` package)

Activities and Fragments implementing the user interface:

#### Activities

**`MainActivity.kt`**
- Main entry point (after splash screen)
- Manages ViewPager2 for onboarding carousel
- Bottom navigation between 4 screens: Home, Focus, Stats, Profile
- Tracks focus lock status

**`SplashActivity.kt`**
- Splash screen shown on app launch
- Theme: `Theme.ZenZone.Splash`

#### Fragments

**`FocusFragment.kt`**
- Timer interface for focus sessions
- Displays countdown
- Uses `FocusViewModel` for state

**`HomeFragment.kt`**
- Dashboard showing user stats
- Current chain display
- Quick-start focus button

**`StatsFragment.kt`**
- Session history with `SessionHistoryAdapter`
- Charts/metrics (total hours focused, best streak, etc.)

**`ProfileFragment.kt`**
- User profile display
- Badge collection

#### Adapters

**`OnboardingAdapter.kt`**
- RecyclerView adapter for onboarding slides
- Displays welcome, features, name input

**`SessionHistoryAdapter.kt`**
- RecyclerView for displaying past focus sessions

---

### 5. **Utilities** (`utils/` package)

Helper functions and constants:

#### `Constants.kt`
Centralized constants:
```kotlin
PREFS_NAME = "zenzone_prefs"
PREF_ONBOARDING_COMPLETE
PREF_USER_NAME
PREF_CURRENT_CHAIN
PREF_TOTAL_XP
```

#### `DateUtils.kt`
Date/time formatting and calculations for session timestamps.

#### `ChainCalculator.kt`
Calculates:
- Chain increments based on consecutive sessions
- XP rewards
- Badge unlocks

#### `DndHelper.kt`
Manages **Do Not Disturb (DND)** mode:
- Enables DND when focus session starts
- Permissions: `android.permission.ACCESS_NOTIFICATION_POLICY`

#### `FocusLockHelper.kt`
Prevents distractions during focus sessions (may lock app navigation).

#### `JsonStorageHelper.kt`
Serializes/deserializes focus sessions to JSON files using **GSON**.

---

## 🔐 Permissions & Manifest

### Permissions Used (`AndroidManifest.xml`)

```xml
<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY"/>
<!-- For Do Not Disturb control -->

<uses-permission android:name="android.permission.VIBRATE"/>
<!-- For haptic feedback on session completion -->
```

### Activities

1. **SplashActivity** → Launcher entry point
2. **MainActivity** → Main app (after onboarding)

### Theme

- **Primary Theme**: `Theme.ZenZone`
- **Status Bar Color**: Zen Teal Dark (`#00897B` or similar)
- App branding: Lotus logo, Material Design 3

---

## 📊 Data Flow & State Management

### Focus Session Flow

```
User Starts Session
    ↓
FocusFragment.startSession()
    ↓
FocusViewModel.startSession()
    ↓
CountDownTimer begins ({duration.milliseconds})
DndHelper.enableDnd()
    ↓
Timer ticks every 1000ms
LiveData updates remainingTimeMs
UI updates countdown display
    ↓
Timer reaches 0
    ↓
ChainCalculator computes:
  - New chain length
  - XP earned
  - New badges unlocked
    ↓
FocusRepository.saveFocusSession()
UserRepository.updateChain()
    ↓
FocusViewModel emits SessionComplete event
UI shows completion screen with rewards
```

### Data Persistence

**SharedPreferences** → Fast key-value storage for:
- User name
- Onboarding status
- Current chain
- Total XP

**JSON Files** (via GSON) → Structured storage for:
- Focus sessions history
- Focus goals
- User profile

---

## 🎨 UI/UX Patterns

### Material Design 3 Components Used

1. **Bottom Navigation View** - 4-tab navigation
2. **Material Buttons** (`MaterialButton`) - "Get Started", etc.
3. **Text Input Layouts** (`TextInputLayout/EditText`) - Name input
4. **Material Color System** - Zen Teal, accent colors
5. **ViewPager2** - Onboarding carousel

### Navigation Flow

```
SplashActivity
    ↓
MainActivity (Onboarding Check)
    ├─→ [If First Time] Onboarding Carousel → Name Input
    │        ↓
    │   Save name & complete flag
    │        ↓
    └─→ Show Bottom Navigation
         ├─ Home Fragment
         ├─ Focus Fragment (Timer)
         ├─ Stats Fragment (History)
         └─ Profile Fragment
```

---

## 🛠️ Building & Running

### Prerequisites
- **Android Studio** Giraffe or later
- **Android SDK** API 34
- **Gradle** 9+
- **Java** 11+

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (with ProGuard minification)
./gradlew assembleRelease

# Run tests
./gradlew test

# Run UI tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean build
```

### Build Configuration

**`app/build.gradle.kts`**
- `compileSdk = 34`
- `minSdk = 26`
- `targetSdk = 34`
- `versionCode = 1`
- `versionName = "1.0"`
- **ProGuard Enabled** for release (obfuscation + minification)

### Run on Emulator/Device

```bash
./gradlew installDebug        # Install debug APK
./gradlew runDebugApp         # Install + run
```

---

## 📦 Gradle Configuration Overview

### Root Level (`build.gradle.kts`)
- Applies Android Gradle Plugin (AGP 9.0.1)

### App Level (`app/build.gradle.kts`)
- Configures app namespace, compileSdk, minSdk
- Declares all dependencies
- Sets up build types (debug/release with ProGuard)

### Dependency Management (`gradle/libs.versions.toml`)
- **Centralized version catalog**
- Versions defined once, referenced everywhere
- Makes updating dependencies easy
- Example:
  ```toml
  [versions]
  agp = "9.0.1"
  coreKtx = "1.18.0"
  
  [libraries]
  androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
  ```

---

## 🧪 Testing

### Unit Tests (`app/src/test/java/`)
- JUnit for testing ViewModels, utils, calculations

### Instrumentation Tests (`app/src/androidTest/java/`)
- Espresso for UI testing
- Tests fragments, activities, navigation

### Test Dependencies
- `JUnit 4.13.2`
- `androidx.test.ext:junit 1.3.0`
- `androidx.test.espresso:espresso-core 3.7.0`

---

## 📝 Key Features Implementation Details

### 1. **Focus Chain Tracking**
- **Location**: `ChainCalculator.kt`
- **Logic**: Increments chain on successful session completion, resets on missed sessions
- **Storage**: SharedPreferences (`PREF_CURRENT_CHAIN`)

### 2. **Do Not Disturb**
- **Location**: `DndHelper.kt`
- **Permission**: `ACCESS_NOTIFICATION_POLICY`
- **Usage**: Called from `FocusViewModel.startSession()`

### 3. **Onboarding**
- **Location**: `MainActivity.showOnboarding()`, `OnboardingAdapter.kt`
- **Flow**: 4 slides → Name input → Save settings
- **Check**: `Constants.PREF_ONBOARDING_COMPLETE`

### 4. **Badge System**
- **Location**: `ZenBadge.kt` model, `ChainCalculator` logic
- **Storage**: `UserRepository.saveUserProfile()`

### 5. **Session Timer**
- **Location**: `FocusViewModel.startSession()` uses `CountDownTimer`
- **Updates**: Every 1000ms via `LiveData<remainingTimeMs>`

---

## 🔒 App Lifecycle & State

### Activity Lifecycle
```
SplashActivity.onCreate()
    ↓
MainActivity.onCreate()
    ├─ Check onboarding status
    ├─ Load user profile
    ├─ Setup bottom navigation
    └─ Load focus goals
```

### ViewModel Lifecycle
- Created when Fragment/Activity created
- Survives configuration changes (screen rotation)
- Destroyed when Activity/Fragment destroyed

### Fragment Lifecycle Management
- ViewModels persist across fragment navigation
- LiveData observers removed on DESTROY
- Coroutines cancelled in `viewModelScope`

---

## 🚀 Performance Optimizations

1. **ProGuard Minification** - Reduces APK size by 30-50%
2. **Kotlin Coroutines** - Non-blocking async operations
3. **ViewPager2** - Efficient recycling for onboarding
4. **LiveData** - Only updates when observers are active
5. **JSON Caching** - Lazy load session history
6. **Multi-Architecture Support** - Universal APK supports arm64-v8a, armeabi-v7a, x86, x86_64

---

## 📱 Installation & Distribution

### APK Details
- **Location**: `app/build/outputs/apk/debug/app-debug.apk`
- **Size**: ~6.12 MB
- **Architecture**: Universal (all CPU types supported)
- **Compatibility**: Android 8.0 - Android 14 (API 26-34)

### Installation Methods

**Option 1: Using Automated Installer (Windows)**
```bash
# Simply run this script in project root
install.bat
# Provides menu for install, launch, uninstall, or clear data
```

**Option 2: Manual ADB Installation**
```bash
# Build APK
./gradlew assembleDebug

# Install on connected device
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch app
adb shell am start -n com.zenzone.app/.ui.splash.SplashActivity
```

**Option 3: Manual File Transfer**
1. Connect device via USB
2. Copy `app-debug.apk` to device Downloads
3. Open Files → Downloads → app-debug.apk → Install

### Troubleshooting Installation
See [INSTALLATION_GUIDE.md](INSTALLATION_GUIDE.md) for detailed troubleshooting:
- "Package Not Installed" errors
- Device not found
- Version conflicts
- Storage issues

---

## 🐛 Debugging & Logging

### Common Debug Points
1. **Onboarding not showing**: Check `PREF_ONBOARDING_COMPLETE` in SharedPreferences
2. **Timer not counting down**: Verify `CountDownTimer` is running
3. **Chain not updating**: Check `ChainCalculator` logic and repository save
4. **DND not working**: Verify `ACCESS_NOTIFICATION_POLICY` permission

### Useful Breakpoints
- `FocusViewModel.startSession()` - Session start
- `FocusViewModel.completeSession()` - Session end
- `ChainCalculator.calculateXP()` - Reward calculation

---

## 📋 Important Files Overview

| File | Purpose |
|------|---------|
| `MainActivity.kt` | App navigation hub, onboarding |
| `FocusViewModel.kt` | Timer & session logic |
| `Constants.kt` | All app constants |
| `ChainCalculator.kt` | XP & chain calculation |
| `DndHelper.kt` | Do Not Disturb control |
| `FocusRepository.kt` | Session data persistence |
| `UserRepository.kt` | User profile persistence |
| `activity_main.xml` | Main layout (bottom nav + fragments) |

---

## 📚 Additional Resources

- **Android Architecture Components**: [Lifecycle, LiveData, ViewModel](https://developer.android.com/jetpack)
- **Material Design 3**: [Material Design Guidelines](https://m3.material.io/)
- **Kotlin Coroutines**: [Coroutines Documentation](https://kotlinlang.org/docs/coroutines-overview.html)
- **Gradle Build System**: [Android Gradle Plugin Guide](https://developer.android.com/build)

---

## 📄 Conclusion

ZenZone is a well-structured MVVM application that gamifies focus sessions through streak tracking and rewards. It leverages modern Android architecture components (ViewModels, LiveData, Fragments) combined with Material Design for a professional user experience. The modular design with separate repositories ensures maintainability and testability.

**For Evaluation**: Demonstrate understanding of:
1. MVVM architecture separation of concerns
2. LiveData for reactive updates
3. Repository pattern for data abstraction
4. Kotlin coroutines for async operations
5. Material Design implementation
6. Android lifecycle management
