# TaskCards

A beautifully designed Android task management app built with Jetpack Compose featuring an intuitive card deck interface and smooth, physics-based animations.

## Features

### 🎴 Card Deck Interface
- Draw tasks from a visual card deck (up to 5 active tasks)
- Swipe cards horizontally to mark as complete with satisfying animations
- Cards slide off-screen with realistic tilt effects
- Dynamic deck that shrinks as you complete tasks
- Celebration animations when all tasks are done

### 📝 List Management
- View all your tasks: active, completed, and removed
- Drag and drop to reorder tasks with live preview
- Swipe right to remove, swipe left to restore
- Real-time visual feedback during interactions
- Supports Unicode text (emojis, international characters)

### 📋 Task List Management
- Single task list for focused, streamlined task management
- Reliable persistence across app restarts
- Cloud sync support when authenticated with Google Sign-In
- Share list access via deep link URL and QR code
- Easy collaboration - share list link for others to join

### 📅 Due Dates & Reminders
- Set due dates on tasks to track deadlines
- Add reminders with flexible timing (on due date, 1 day before, 1 week before, custom)
- **Daily reminder notifications at scheduled time** - Get a daily reminder about your active tasks at a time you choose
- System notifications for upcoming task deadlines
- Visual due date badges showing status (overdue, due today, upcoming)
- WorkManager-based reliable notification delivery
- Configure reminder time, sound, and vibration in Settings

### 🔍 Search & Filtering
- Full-text search across all tasks in a list
- Multi-criteria filtering (status, due dates)
- Save frequently used filter combinations
- Real-time search results as you type
- Filter by active/done/removed status
- Filter by due date ranges (overdue, today, this week, custom)
- Visual filter chips showing active filters

### 🔗 Share & Collaboration Features
- Share list access via deep link URL (taskcards://list/...)
- Generate QR codes for easy mobile sharing
- QR codes display in beautiful Material3 dialog
- Copy deep link to clipboard or share via Android share sheet
- Quick access via Share button in List screen
- Onscreen notifications for successful/failed list loading
- Real-time feedback when scanning QR codes (success/error messages)
- Enables real-time collaboration on shared lists

### 📱 Home Screen Widgets
- Three widget types: Task List, Quick Add, Due Today
- Task List Widget: View up to 5 tasks from any list on your home screen
- Quick Add Widget: Rapidly add tasks without opening the app
- Due Today Widget: See all tasks due today at a glance
- Widgets update automatically when tasks change
- Material3-themed widgets matching app design
- Configurable widget preferences

### 🎨 Premium Design
- Material3 design with custom brand colors (purple/blue palette)
- Smooth spring-based animations throughout
- Gradient backgrounds and borders
- Elevated shadows and depth effects
- Dark and light theme support
- Responsive to system navigation bars

### 🛡️ Production Ready
- Comprehensive error handling with user-friendly messages
- ProGuard/R8 rules for optimized release builds
- Memory management with cleanup APIs
- Robust logging for debugging
- No crashes from repository failures
- Thread-safe with Mutex-based synchronization

### 🌍 Internationalization (i18n)
- **Three Languages**: English, German (Deutsch), Japanese (日本語)
- **Language Selection**: Choose language in Settings with visual flag emojis
- **System Default**: Automatically detects and uses device language
- **Immediate Updates**: Language changes apply instantly without app restart
- **Complete Translation**: All 278+ strings translated across all languages
- **Native Names**: Language picker shows languages in their native scripts
- **Persistent Preference**: Language choice saved and restored across sessions

### ♿ Accessibility (WCAG 2.1 Level AA)
- **High Contrast Mode**: Optional high contrast color theme for low vision users
- **Screen Reader Support**: Full TalkBack compatibility with semantic labels
- **Keyboard Navigation**: All features accessible via keyboard
- **Touch Targets**: Minimum 48×48 dp for all interactive elements
- **Color Contrast**: 4.5:1+ ratio on all text (WCAG AA compliant)
- **Custom Actions**: Alternatives to gesture-only interactions
- **No Color-Only Information**: All states use text + icons + color

For complete accessibility information, see [ACCESSIBILITY.md](ACCESSIBILITY.md)

## Screenshots

*(Screenshots coming soon)*

## Getting Started

### Prerequisites
- Android Studio Ladybug or later
- Android SDK 34+
- JDK 17

### Building the Project

Clone the repository and build:

```bash
git clone <repository-url>
cd TaskCards
./gradlew assembleDebug
```

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run specific test suite
./gradlew testDebugUnitTest
```

### Installing on Device

```bash
# Install debug build
./gradlew installDebug

# Or manually install the APK from:
# app/build/outputs/apk/debug/app-debug.apk
```

### Firebase Setup (Optional)

Firebase integration is **optional** - the app works fully offline with local storage. Cloud sync and authentication are only needed if you want to test those features.

**To enable Firebase features:**

1. **Create a Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use an existing one

2. **Add Android App to Firebase**
   - Package name: `de.hipp.app.taskcards`
   - Download `google-services.json`

3. **Configure Firebase Services**
   - Enable **Authentication** → Sign-in method → Google
   - Enable **Firestore Database** → Create database
   - Deploy security rules from `firestore.rules` (see [FIRESTORE_RULES_DEPLOYMENT.md](FIRESTORE_RULES_DEPLOYMENT.md))

4. **Add Configuration Files**
   ```bash
   # Place your google-services.json in the app directory
   cp ~/Downloads/google-services.json app/google-services.json

   # Update OAuth client ID in strings.xml
   # Replace YOUR_WEB_CLIENT_ID_HERE with the web client ID from google-services.json
   ```

5. **Add SHA-1 Fingerprint** (for Google Sign-In)
   ```bash
   # Get your debug keystore SHA-1
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android

   # Add the SHA-1 to Firebase Console → Project Settings → Your App
   ```

**Note:** The repository includes `app/google-services.json.EXAMPLE` as a template. See that file for detailed setup instructions.

**Without Firebase:** The app will use local Room database storage and all features except Google Sign-In will work normally.

## Tech Stack

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (BOM 2024.10.01)
- **Architecture**: MVVM with StateFlow
- **Navigation**: Jetpack Navigation Compose
- **Backend**: Firebase Firestore + Authentication
- **Persistence**: Room Database + DataStore
- **Background Work**: WorkManager (reminders, widget updates)
- **Widgets**: Jetpack Glance (Material3 widgets)
- **Dependency Injection**: Hilt
- **Concurrency**: Kotlin Coroutines & Flow
- **QR Codes**: ZXing (deep link sharing)
- **Serialization**: Kotlinx Serialization JSON
- **Testing**: Kotest 5.9.1 with JUnit5 Platform
- **Build Tool**: Gradle with Kotlin DSL
- **CI/CD**: GitHub Actions

## Architecture

The app follows Clean Architecture principles with MVVM (Model-View-ViewModel) pattern and clear separation of concerns:

- **Model**: Data classes representing domain entities (TaskItem, SearchFilter, Language, etc.)
- **Repository**: Abstract data layer with multiple implementations:
  - `TaskListRepository` - Task operations (InMemory/Firestore/Room implementations)
  - `PreferencesRepository` - App settings and preferences (DataStore)
- **Authentication**: Firebase Auth with Google Sign-In (optional)
- **Background Work**:
  - `ReminderWorker` - Scheduled task reminders via WorkManager
  - `WidgetUpdateWorker` - Periodic widget data refresh
  - `ReminderScheduler` - Notification scheduling logic
- **Sharing**: `QRCodeGenerator` for QR code generation and sharing lists
- **Internationalization**: `LocaleHelper` for runtime locale switching
- **Widgets**: Glance-based home screen widgets (Task List, Quick Add, Due Today)
- **ViewModel**: Reactive state management with StateFlow
- **View**: Composable UI components

### Key Design Patterns
- Repository pattern for data abstraction
- MVVM for presentation layer
- Factory pattern for ViewModel creation
- Reactive programming with Kotlin Flow
- Comprehensive error handling with StateFlow

### Error Handling
All ViewModels implement production-grade error handling:
- Exposed error state via `errorState: StateFlow<String?>`
- Try-catch blocks around all operations
- User-friendly error messages
- Android logging for debugging
- Graceful failure without crashes

### Technical Documentation

For in-depth technical details, architecture deep-dives, and implementation guides, see:

**[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)** - Comprehensive technical reference covering:
- Data layer implementation and repository pattern details
- State management and Flow-based reactive architecture
- Database schema and persistence strategies
- Background processing with WorkManager
- Widget architecture with Jetpack Glance
- Internationalization system implementation
- Build configuration and ProGuard optimization
- Testing architecture and strategies
- Performance considerations and optimization techniques
- Security and privacy implementation

## Project Structure

```
app/src/main/java/de/hipp/app/taskcards/
├── model/           # Domain models (TaskItem, SearchFilter, Language, etc.)
├── data/            # Repository layer (TaskList, Preferences, Room entities)
├── di/              # Hilt dependency injection modules
├── auth/            # Firebase authentication
├── worker/          # WorkManager workers (ReminderWorker, WidgetUpdateWorker, ReminderScheduler)
├── qr/              # QR code generation (QRCodeGenerator)
├── util/            # Utilities (LocaleHelper, Constants)
├── widget/          # Glance widgets (TaskListWidget, QuickAddWidget, DueTodayWidget)
└── ui/
    ├── viewmodel/   # ViewModels (Cards, List, Share, Settings)
    ├── screens/     # Composable screens (CardsScreen, ListScreen, SettingsScreen,
    │                #   FilterBottomSheet, ShareDialog, ActiveFilterChips, etc.)
    ├── theme/       # Material3 theming
    └── MainActivity.kt

app/src/test/java/   # Unit tests (79 tests)
docs/                # Documentation (release guides)
.github/workflows/   # CI/CD automation
```

## Testing

Comprehensive test coverage with Kotest (63 tests, all passing):

- **17 Repository Tests**: CRUD operations, edge cases, memory management (clearList, getActiveListCount), input validation (max 500 chars), order compaction
- **46 ViewModel Tests**: State management, user actions, Unicode handling, error scenarios
  - CardsViewModel: 8 tests (basic + error handling)
  - ListViewModel: 14 tests (basic + Unicode + error handling)
  - SettingsViewModel: 24 tests (basic state + error handling)
- **Test Framework**: Kotest 5.9.1 with StringSpec style
- **Coroutine Testing**: Proper dispatcher injection for testability
- **Error Coverage**: Comprehensive error scenario testing for all ViewModels

## Production Readiness

### ProGuard/R8 Configuration
Comprehensive rules in `app/proguard-rules.pro` (61 lines):
- Preserves data models, ViewModels, and Compose components
- Protects Coroutines and Flow operators
- Removes debug logging in release builds
- Maintains stack traces for debugging

### Code Quality
- Mutex-based thread safety in repository
- Error handling in all ViewModel operations
- No memory leaks (proper cleanup mechanisms)
- WindowInsets-aware layouts
- Optimized recomposition with `derivedStateOf`

## Development

### Code Style
- Follow Kotlin coding conventions
- Use Compose best practices
- Prefer immutability and functional patterns

### Adding New Features
1. Create/update domain models in `model/`
2. Add repository methods in `data/TaskListRepository.kt`
3. Update ViewModels to expose new state
4. Build UI in `ui/screens/`
5. Write tests in `app/src/test/`

### Task Ordering System
Tasks use integer `order` values where **lower = higher priority**. New tasks receive the smallest existing order minus one, ensuring they appear at the top.

## Release Automation

### 🚀 Automated Release Workflow

The project includes a GitHub Actions workflow that automates the entire release process:

**Features:**
- 📦 Builds signed release APK and AAB
- 🛡️ **Dry run mode enabled by default** (safe testing before publishing)
- 🏪 Publishes to Google Play Store (internal/alpha/beta/production tracks)
- 🏷️ Creates GitHub Releases with automatic tagging
- 📥 Downloadable artifacts for testing

**Quick Start:**

1. **Test Build (Dry Run)**
   ```
   GitHub → Actions → Build and Publish Release → Run workflow
   - Dry run: ✅ true (default)
   - Version: 1.0.0
   - Version Code: 1
   ```
   Downloads the built APK/AAB for local testing without publishing.

2. **Publish Release**
   ```
   Same workflow, set Dry run: ❌ false
   ```
   Publishes to Play Store and creates GitHub Release.

**Setup Required:**
- Release keystore (create with `keytool`)
- Google Play Console service account
- 5 GitHub Secrets (keystore credentials + Play Store API)

📖 **Full documentation:** [docs/release-quick-start.md](docs/release-quick-start.md) and [docs/release-setup.md](docs/release-setup.md)

### Firebase Integration

The app supports:
- ☁️ **Cloud sync** with Firebase Firestore
- 🔐 **Google Sign-In** authentication
- 👥 **Collaborative lists** (multiple users can edit same list)
- 📱 **Local-first** (works without authentication, optional cloud sync)

When not authenticated, tasks are stored locally. Sign in via Settings to enable cloud sync and collaboration.

## Roadmap

- [x] Add Firestore backend support
- [x] Implement Google Sign-In authentication
- [x] Automated release workflow with GitHub Actions
- [x] Add task due dates and reminders
- [x] Add widgets for home screen
- [x] Implement task search and filtering
- [x] Add copy and share functionality with QR codes
- [x] Internationalization (English, German, Japanese)
- [x] Simplified single-list architecture for reliability

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Write tests for new functionality
4. Ensure all tests pass (`./gradlew test`)
5. Submit a pull request

## License

This project is open source and available under the MIT License.

## Acknowledgments

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Tested with [Kotest](https://kotest.io/)
- Material Design 3 guidelines

---

Made with ❤️ using Kotlin and Jetpack Compose
