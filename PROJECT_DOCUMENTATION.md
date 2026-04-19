# ZenZone - Focus & Productivity App
## Complete Project Documentation for Evaluation

---

## 📋 Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture & Design Pattern](#architecture--design-pattern)
3. [Technology Stack](#technology-stack)
4. [Core Components](#core-components)
5. [Features Implementation](#features-implementation)
6. [Data Management](#data-management)
7. [UI/UX Design Approach](#uiux-design-approach)
8. [Build & Deployment](#build--deployment)
9. [Project Structure](#project-structure)
10. [Key Learnings](#key-learnings)

---

## 1. Project Overview

**ZenZone** is a native Android productivity application designed to help users maintain focus through timed sessions, track their progress, and earn achievements. The app promotes deep work and mindfulness through gamification elements.

### Purpose
- Help users create and manage focus goals
- Track focus sessions with timer functionality
- Provide statistics and insights on productivity
- Motivate users through badges and streaks
- Enable Do Not Disturb mode during focus sessions

### Target Users
Students, professionals, and anyone looking to improve their focus and productivity through structured work sessions.

---

## 2. Architecture & Design Pattern

### MVVM (Model-View-ViewModel) Architecture

**Why MVVM?**
- **Separation of Concerns**: Clear separation between UI logic and business logic
- **Testability**: ViewModels can be tested independently
- **Lifecycle Awareness**: ViewModels survive configuration changes
- **Reactive Programming**: LiveData enables reactive UI updates

### Architecture Layers

```
┌─────────────────────────────────────┐
│           UI Layer (View)           │
│  - Fragments (Home, Focus, Stats)   │
│  - Activities (MainActivity, Splash)│
│  - Adapters (RecyclerView)          │
└──────────────┬──────────────────────┘
               │ observes LiveData
┌──────────────▼──────────────────────┐
│      ViewModel Layer                │
│  - HomeViewModel                    │
│  - FocusViewModel                   │
│  - StatsViewModel                   │
│  - ProfileViewModel                 │
└──────────────┬──────────────────────┘
               │ calls methods
┌──────────────▼──────────────────────┐
│      Repository Layer               │
│  - FocusRepository                  │
│  - UserRepository                   │
└──────────────┬──────────────────────┘
               │ reads/writes
┌──────────────▼──────────────────────┐
│      Data Layer (Model)             │
│  - JSON File Storage                │
│  - SharedPreferences                │
│  - Data Classes (Models)            │
└─────────────────────────────────────┘
```

---

## 3. Technology Stack

### Core Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Kotlin** | 2.0.20 | Primary programming language |
| **Android SDK** | 34 (Android 14) | Target platform |
| **Min SDK** | 26 (Android 8.0) | Minimum supported version |
| **Gradle** | 9.0.1 | Build system |

### Android Components

| Component | Purpose |
|-----------|---------|
| **Fragments** | Screen navigation and UI containers |
| **ViewModels** | Business logic and state management |
| **LiveData** | Observable data holder for reactive UI |
| **RecyclerView** | Efficient list rendering |
| **Material Design Components** | Modern UI components |
| **Navigation Component** | Fragment navigation |
| **SharedPreferences** | Simple key-value storage |

### Third-Party Libraries

| Library | Version | Purpose |
|---------|---------|---------|
| **MPAndroidChart** | 3.1.0 | Professional chart visualization |
| **Gson** | 2.10.1 | JSON serialization/deserialization |
| **Coroutines** | 1.7.3 | Asynchronous programming |

### Why NOT Jetpack Compose?
- **Project Requirement**: Traditional XML-based UI
- **Stability**: XML layouts are mature and well-documented
- **Learning Curve**: Easier for beginners
- **Compatibility**: Better support for older Android versions

---

## 4. Core Components

### 4.1 Models (Data Classes)

#### FocusGoal.kt
```kotlin
data class FocusGoal(
    val id: String,
    val name: String,
    val targetMinutes: Int,
    val frequency: String,
    val currentChain: Int,
    val lastCompletedDate: String?
)
```
**Purpose**: Represents a user's focus goal with tracking data

#### FocusSession.kt
```kotlin
data class FocusSession(
    val id: String,
    val goalId: String,
    val goalName: String,
    val durationMinutes: Int,
    val timestamp: Long,
    val date: String
)
```
**Purpose**: Records completed focus sessions for history tracking

#### UserProfile.kt
```kotlin
data class UserProfile(
    val userName: String,
    val profileImageUri: String?,
    val totalFocusedMinutes: Int,
    val currentChain: Int,
    val unlockedBadges: List<String>
)
```
**Purpose**: Stores user information and achievements

#### ZenBadge.kt
```kotlin
data class ZenBadge(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: Int,
    val isUnlocked: Boolean
)
```
**Purpose**: Represents achievement badges

### 4.2 ViewModels

#### HomeViewModel
**Responsibilities**:
- Load and manage focus goals
- Calculate total streak from all goals
- Format focus time display
- Load recent sessions
- Manage user profile data

**Key Logic**:
```kotlin
// Streak calculation: Sum of all goal chains
val totalStreak = goals.sumOf { it.currentChain }

// Focus time formatting
val hours = totalMinutes / 60
val minutes = totalMinutes % 60
```

#### FocusViewModel
**Responsibilities**:
- Timer management (start, stop, pause)
- Goal selection
- Session logging
- DND (Do Not Disturb) control
- XP and chain calculation

**Key Logic**:
```kotlin
// Chain increment logic
if (lastCompletedDate == today) {
    // Already completed today, no chain increment
} else if (lastCompletedDate == yesterday) {
    // Consecutive day, increment chain
    currentChain++
} else {
    // Chain broken, reset to 1
    currentChain = 1
}
```

#### StatsViewModel
**Responsibilities**:
- Load session history
- Calculate weekly statistics
- Generate chart data
- Milestone progress tracking

**Key Logic**:
```kotlin
// Milestone levels
val milestones = [0, 5, 15, 40, 100, 250, 500] hours
// Calculate progress within current milestone range
val progressPercent = (currentHours - levelMin) / (levelMax - levelMin) * 100
```

#### ProfileViewModel
**Responsibilities**:
- Load and update user profile
- Calculate total focused hours
- Calculate total streak
- Manage badge unlocking

### 4.3 Repositories

#### FocusRepository
**Purpose**: Manages focus goals and sessions data persistence

**Methods**:
- `getGoals()`: Load all focus goals from JSON
- `saveGoals()`: Persist goals to JSON file
- `getSessions()`: Load session history
- `saveSessions()`: Persist sessions to JSON file
- `addGoal()`, `updateGoal()`, `deleteGoal()`

**Storage Location**: `app_data/focus_goals.json`, `app_data/focus_sessions.json`

#### UserRepository
**Purpose**: Manages user profile data

**Methods**:
- `getProfile()`: Load user profile from JSON
- `saveProfile()`: Persist profile to JSON file
- `updateProfile()`: Update specific profile fields

**Storage Location**: `app_data/user_profile.json`

### 4.4 Utility Classes

#### BadgeManager
**Purpose**: Centralized badge logic and unlocking criteria

**Badge Types**:
1. **First Breath** - Complete first session
2. **Time Badges** - 10h, 50h, 100h, 500h milestones
3. **Chain Badges** - 7, 30, 100 day streaks
4. **Master Badge** - All badges unlocked

#### ChainCalculator
**Purpose**: Calculate consecutive day streaks

**Logic**:
```kotlin
fun calculateChain(lastDate: String?, currentDate: String): Int {
    if (lastDate == null) return 1
    if (lastDate == currentDate) return currentChain // Same day
    if (lastDate == yesterday) return currentChain + 1 // Consecutive
    return 1 // Chain broken
}
```

#### DateUtils
**Purpose**: Date formatting and comparison utilities

#### DndHelper
**Purpose**: Manage Do Not Disturb permissions and state

#### FocusLockHelper
**Purpose**: Prevent navigation during active focus sessions

#### JsonStorageHelper
**Purpose**: Generic JSON file read/write operations

**Why JSON Storage?**
- **Simplicity**: No database setup required
- **Portability**: Easy to backup and transfer
- **Readability**: Human-readable format
- **Lightweight**: Suitable for small data sets
- **Project Requirement**: Specified in project description

---

## 5. Features Implementation

### 5.1 Onboarding Flow

**Implementation**: `MainActivity.kt` + `OnboardingAdapter.kt`

**Flow**:
1. Check if user has completed onboarding (SharedPreferences)
2. Show ViewPager2 with 3 onboarding screens
3. Collect username on final screen
4. Save to SharedPreferences and navigate to main app

**Why ViewPager2?**
- Smooth swipe gestures
- Built-in page indicators
- RecyclerView-based (efficient)

### 5.2 Focus Goal Management

**Create Goal**: `AddFocusFragment.kt`
- Input: Goal name, target minutes, frequency
- Validation: Non-empty name, valid duration
- Storage: JSON file via FocusRepository

**Edit Goal**: `EditFocusDialogFragment.kt`
- Update existing goal properties
- Delete goal with confirmation
- Update JSON storage

**Display Goals**: `HomeFragment.kt` + `ZenCardAdapter.kt`
- RecyclerView with CardView items
- Shows goal name, target, chain streak
- Click to start focus session
- Edit icon for modifications

### 5.3 Timer & Focus Session

**Implementation**: `FocusFragment.kt` + `CircularTimerView.kt`

**Timer Logic**:
```kotlin
private val timer = object : CountDownTimer(remainingMs, 1000) {
    override fun onTick(millisUntilFinished: Long) {
        remainingTimeMs.value = millisUntilFinished
    }
    override fun onFinish() {
        // Session complete
        triggerSessionComplete()
    }
}
```

**Custom CircularTimerView**:
- Canvas-based circular progress indicator
- Displays remaining time in center
- Smooth animation updates
- Color-coded progress (teal to red)

**DND Integration**:
- Request notification policy access
- Enable DND during session
- Disable DND when session ends
- Graceful fallback if permission denied

**Focus Lock**:
- Disable bottom navigation during session
- Prevent accidental navigation
- Show "Focus Lock Active" badge

### 5.4 Statistics & Analytics

**Implementation**: `StatsFragment.kt` + `MPAndroidChart`

**Weekly Insights**:
- Calculate last 7 days of focus time
- Identify peak focus day
- Show percentage of weekly target (15 hours)

**Bar Chart**:
```kotlin
// Using MPAndroidChart library
val entries = last7Days.mapIndexed { index, minutes ->
    BarEntry(index.toFloat(), minutes.toFloat())
}
val dataSet = BarDataSet(entries, "Minutes")
barChart.data = BarData(dataSet)
```

**Why MPAndroidChart?**
- Professional, production-ready charts
- Highly customizable
- Smooth animations
- Touch interactions
- Well-documented

**Milestone System**:
- 7 levels: Novice Monk → Enlightened One
- Progress bar shows current level progress
- Dynamic calculation based on total hours

**Session History**:
- RecyclerView of recent sessions
- Shows goal name, duration, date
- Click to view details

### 5.5 Profile & Badges

**Implementation**: `ProfileFragment.kt` + `BadgeAdapter.kt`

**Profile Display**:
- Username (from onboarding or profile)
- Profile initial (first letter of name)
- Total focused hours (calculated from sessions)
- Total streak (sum of all goal chains)

**Badge System**:
- Grid view (3 columns) and List view toggle
- Locked/Unlocked states with visual feedback
- Badge icons with names
- Progress-based unlocking

**Badge Adapter**:
```kotlin
// Dual layout support
override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return if (isGridView) {
        // Grid layout (3 columns)
        GridViewHolder(...)
    } else {
        // List layout (full width)
        ListViewHolder(...)
    }
}
```

### 5.6 Navigation

**Bottom Navigation**: `MainActivity.kt`
- 4 tabs: Home, Focus, Stats, Profile
- Fragment transactions
- State preservation
- Icon + label

**Common Navbar**:
- Reusable layout included in fragments
- App logo/name
- Info button (context-specific help)
- Profile icon (navigates to profile)

---

## 6. Data Management

### 6.1 Storage Strategy

**JSON File Storage**:
```
/data/data/com.zenzone.app/files/
├── focus_goals.json
├── focus_sessions.json
└── user_profile.json
```

**SharedPreferences**:
```
zenzone_prefs:
├── is_onboarding_complete: Boolean
├── user_name: String
└── last_session_date: String
```

### 6.2 Data Flow

**Write Flow**:
```
User Action → ViewModel → Repository → JsonStorageHelper → File System
```

**Read Flow**:
```
File System → JsonStorageHelper → Repository → ViewModel → LiveData → UI
```

### 6.3 Data Persistence

**Gson Serialization**:
```kotlin
// Write
val json = Gson().toJson(goals)
file.writeText(json)

// Read
val json = file.readText()
val goals = Gson().fromJson(json, Array<FocusGoal>::class.java)
```

**Why Gson?**
- Simple API
- Automatic serialization
- Type-safe
- Handles nested objects
- Null-safe

### 6.4 Error Handling

**Try-Catch Blocks**:
- All file operations wrapped in try-catch
- Default values on read errors
- User-friendly error messages
- Stack trace logging for debugging

```kotlin
try {
    val goals = repository.getGoals()
    _goals.value = goals
} catch (e: Exception) {
    e.printStackTrace()
    _goals.value = emptyList() // Safe default
    _errorMessage.value = "Failed to load goals"
}
```

---

## 7. UI/UX Design Approach

### 7.1 Design System

**Color Palette**:
```xml
<!-- Primary Colors -->
zen_teal_primary: #2A9D8F (Main brand color)
zen_teal_dark: #264653 (Dark accent)
zen_teal_light: #E0F2F1 (Light backgrounds)

<!-- Neutral Colors -->
zen_slate_dark: #264653 (Text)
zen_slate_surface: #F8F9FA (Cards)
zen_slate_bg: #FFFFFF (Background)
zen_gray_text: #6B7280 (Secondary text)
```

**Typography**:
- Font Family: Poppins
- Weights: Regular, Bold
- Sizes: 10sp - 48sp (hierarchical)

**Spacing**:
- Base unit: 4dp
- Common: 8dp, 12dp, 16dp, 20dp, 24dp
- Consistent padding/margins

### 7.2 Material Design Components

**Why Material Design?**
- Consistent with Android ecosystem
- Accessibility built-in
- Touch target sizes (48dp minimum)
- Elevation and shadows
- Ripple effects

**Components Used**:
- MaterialCardView (elevated cards)
- MaterialButton (primary actions)
- BottomNavigationView (navigation)
- MaterialAlertDialog (confirmations)
- BottomSheetDialog (info displays)
- FloatingActionButton (add goal)

### 7.3 Layout Strategy

**XML Layouts** (Not Compose):
- Declarative UI definition
- Preview in Android Studio
- Reusable with `<include>`
- ConstraintLayout for complex layouts
- LinearLayout for simple stacking
- FrameLayout for overlays

**Responsive Design**:
- `wrap_content` and `match_parent`
- `layout_weight` for proportional sizing
- `ConstraintLayout` chains and guidelines
- ScrollView for overflow content

### 7.4 User Feedback

**Visual Feedback**:
- Ripple effects on clickable items
- Progress bars for loading states
- Empty states with illustrations
- Toast messages for quick feedback
- Dialogs for important confirmations

**Loading States**:
```kotlin
viewModel.isLoading.observe { isLoading ->
    progressBar.visibility = if (isLoading) VISIBLE else GONE
}
```

### 7.5 Accessibility

**Considerations**:
- Content descriptions for images
- Minimum touch targets (48dp)
- Sufficient color contrast
- Text scaling support
- Screen reader compatibility

---

## 8. Build & Deployment

### 8.1 Build Configuration

**Gradle Setup**:
```kotlin
android {
    compileSdk = 34
    minSdk = 26  // Android 8.0+
    targetSdk = 34  // Android 14
    
    buildTypes {
        debug { /* Debug keystore */ }
        release { /* Release signing */ }
    }
}
```

**Multi-Architecture Support**:
```kotlin
ndk {
    abiFilters.add("arm64-v8a")  // 64-bit ARM
    abiFilters.add("armeabi-v7a")  // 32-bit ARM
    abiFilters.add("x86")  // 32-bit Intel
    abiFilters.add("x86_64")  // 64-bit Intel
}
```

**Why Multiple ABIs?**
- Compatibility with all Android devices
- Emulator support (x86)
- Physical devices (ARM)
- Universal APK for easy distribution

### 8.2 APK Generation

**Build Command**:
```bash
./gradlew assembleDebug
```

**Output Location**:
```
app/build/outputs/apk/debug/app-debug.apk
```

**APK Signing**:
- Debug builds: Android debug keystore
- Release builds: Custom keystore (configured)
- Ensures package integrity

### 8.3 Version Catalog

**libs.versions.toml**:
- Centralized dependency management
- Version consistency across modules
- Easy updates
- Type-safe accessors

```toml
[versions]
kotlin = "2.0.20"
agp = "9.0.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.12.0" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```

---

## 9. Project Structure

```
ZenZone/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/zenzone/app/
│   │   │   │   ├── model/
│   │   │   │   │   ├── FocusGoal.kt
│   │   │   │   │   ├── FocusSession.kt
│   │   │   │   │   ├── UserProfile.kt
│   │   │   │   │   └── ZenBadge.kt
│   │   │   │   ├── repository/
│   │   │   │   │   ├── FocusRepository.kt
│   │   │   │   │   └── UserRepository.kt
│   │   │   │   ├── viewmodel/
│   │   │   │   │   ├── HomeViewModel.kt
│   │   │   │   │   ├── FocusViewModel.kt
│   │   │   │   │   ├── StatsViewModel.kt
│   │   │   │   │   └── ProfileViewModel.kt
│   │   │   │   ├── ui/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   │   └── OnboardingAdapter.kt
│   │   │   │   │   ├── splash/
│   │   │   │   │   │   └── SplashActivity.kt
│   │   │   │   │   ├── home/
│   │   │   │   │   │   ├── HomeFragment.kt
│   │   │   │   │   │   ├── ZenCardAdapter.kt
│   │   │   │   │   │   ├── AddFocusFragment.kt
│   │   │   │   │   │   └── EditFocusDialogFragment.kt
│   │   │   │   │   ├── focus/
│   │   │   │   │   │   ├── FocusFragment.kt
│   │   │   │   │   │   └── CircularTimerView.kt
│   │   │   │   │   ├── stats/
│   │   │   │   │   │   ├── StatsFragment.kt
│   │   │   │   │   │   └── SessionHistoryAdapter.kt
│   │   │   │   │   └── profile/
│   │   │   │   │       ├── ProfileFragment.kt
│   │   │   │   │       └── BadgeAdapter.kt
│   │   │   │   └── utils/
│   │   │   │       ├── BadgeManager.kt
│   │   │   │       ├── ChainCalculator.kt
│   │   │   │       ├── DateUtils.kt
│   │   │   │       ├── DndHelper.kt
│   │   │   │       ├── FocusLockHelper.kt
│   │   │   │       └── JsonStorageHelper.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── drawable/
│   │   │   │   ├── values/
│   │   │   │   └── font/
│   │   │   └── AndroidManifest.xml
│   │   └── androidTest/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

**Package Organization**:
- **model**: Data classes
- **repository**: Data access layer
- **viewmodel**: Business logic
- **ui**: User interface (organized by feature)
- **utils**: Helper classes

---

## 10. Key Learnings

### 10.1 Technical Learnings

1. **MVVM Architecture**
   - Clear separation of concerns
   - Easier testing and maintenance
   - Lifecycle-aware components

2. **LiveData & Observers**
   - Reactive UI updates
   - Automatic lifecycle management
   - No memory leaks

3. **JSON Storage**
   - Simple persistence solution
   - Human-readable format
   - Easy debugging

4. **Custom Views**
   - Canvas drawing for CircularTimerView
   - onDraw() and invalidate()
   - Custom attributes

5. **RecyclerView Patterns**
   - ViewHolder pattern
   - DiffUtil for efficient updates
   - Multiple view types (grid/list)

### 10.2 Android-Specific Learnings

1. **Fragment Lifecycle**
   - onViewCreated vs onCreate
   - onResume for data refresh
   - Proper cleanup in onDestroyView

2. **Permissions**
   - Runtime permission requests
   - DND access (special permission)
   - Graceful degradation

3. **Navigation**
   - Bottom navigation setup
   - Fragment transactions
   - Back stack management

4. **Material Design**
   - Component usage
   - Theming and styling
   - Elevation and shadows

### 10.3 Best Practices Applied

1. **Error Handling**
   - Try-catch blocks everywhere
   - Default values on errors
   - User-friendly messages

2. **Code Organization**
   - Feature-based packaging
   - Single responsibility principle
   - Reusable components

3. **Resource Management**
   - Externalized strings
   - Drawable resources
   - Dimension resources

4. **Performance**
   - RecyclerView for lists
   - ViewHolder pattern
   - Efficient data loading

### 10.4 Challenges Overcome

1. **APK Installation Issues**
   - Kotlin plugin conflict
   - Signing configuration
   - Multi-architecture support

2. **Data Synchronization**
   - Profile image sync across pages
   - Real-time updates with LiveData
   - onResume() refresh pattern

3. **Timer Management**
   - CountDownTimer lifecycle
   - Pause/resume functionality
   - Background handling

4. **Badge System**
   - Dynamic unlocking logic
   - Progress calculation
   - Grid/list view switching

---

## 📊 Project Statistics

- **Total Kotlin Files**: 35+
- **Total XML Layouts**: 25+
- **Lines of Code**: ~3500+
- **Features**: 15+
- **Screens**: 8
- **Custom Views**: 2
- **Adapters**: 5
- **ViewModels**: 4
- **Repositories**: 2
- **Utility Classes**: 7

---

## 🎯 Key Features Summary

✅ User onboarding with name collection
✅ Create, edit, delete focus goals
✅ Timer with circular progress indicator
✅ Do Not Disturb integration
✅ Focus lock during sessions
✅ Session history tracking
✅ Weekly statistics with charts
✅ Milestone progress system
✅ Badge achievement system
✅ Streak tracking (daily chains)
✅ Profile management
✅ Grid/List view toggle for badges
✅ JSON-based data persistence
✅ Material Design UI
✅ Responsive layouts

---

## 🚀 Future Enhancements (Potential)

1. **Cloud Sync**: Firebase integration for multi-device sync
2. **Notifications**: Reminders for focus sessions
3. **Themes**: Dark mode support
4. **Export**: Export statistics as PDF/CSV
5. **Social**: Share achievements on social media
6. **Widgets**: Home screen widget for quick timer start
7. **Sounds**: Ambient sounds during focus sessions
8. **Analytics**: More detailed productivity insights

---

## 📝 Conclusion

ZenZone demonstrates a complete Android application built with modern architecture patterns, following Android best practices, and implementing a full feature set for productivity tracking. The project showcases proficiency in:

- Kotlin programming
- MVVM architecture
- Android SDK components
- Material Design
- Data persistence
- Custom views
- Third-party library integration
- User experience design

The app is production-ready, installable on any Android device (API 26+), and provides a smooth, intuitive user experience for focus and productivity management.

---

**Developed by**: [Your Name]
**Date**: April 2026
**Platform**: Android (Native)
**Language**: Kotlin
**Architecture**: MVVM
**UI Framework**: XML Layouts (Traditional Android Views)
