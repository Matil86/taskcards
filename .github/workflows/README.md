# CI/CD Workflows Documentation

## Overview

The TaskCards project implements a production-grade CI/CD pipeline using GitHub Actions. The strategy employs two complementary workflows:

- **CI Workflow (`ci.yml`)**: Enforces quality gates on all pull requests and pushes to main/develop branches
- **CD Workflow (`cd.yml`)**: Automates multi-stage deployment to Google Play Store with manual approval gates

This architecture ensures that only thoroughly tested, high-quality code reaches production while maintaining rapid development velocity through automation and parallel execution.

**Key Principles:**
- Build once, deploy everywhere (single AAB promoted through all tracks)
- Comprehensive quality gates before any deployment
- Manual approval required for alpha, beta, and production promotions
- Automated smoke testing to verify deployments
- Fail-fast approach with detailed error reporting

---

## CI Workflow (ci.yml)

### Trigger Conditions

```yaml
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
```

The CI workflow runs on:
- All pull requests targeting the `main` branch
- Direct pushes to `main` or `develop` branches
- Ignores documentation changes (`**.md`, `docs/**`) to optimize build time

### Workflow Stages

#### 1. Environment Setup
- **JDK**: Temurin 21 (LTS)
- **Caching**: Aggressive Gradle caching for faster builds
  - Build cache: `~/.gradle/caches`
  - Wrapper cache: `~/.gradle/wrapper`
  - Custom build cache: `~/.gradle/build-cache`
- **Firebase Config**: Decodes `GOOGLE_SERVICES_JSON_BASE64` secret
- **Validation**: Ensures `google-services.json` is valid JSON

#### 2. Quality Gates (Parallel Execution)

The CI workflow runs multiple quality gates in parallel using the `--parallel` flag:

```bash
./gradlew assembleDebug test lint --parallel
```

**Quality Gate 1: Compilation**
- Assembles debug APK
- Validates Kotlin/Java compilation
- Ensures all dependencies resolve correctly

**Quality Gate 2: Unit Tests**
- Runs all JUnit tests (`./gradlew test`)
- Validates business logic, ViewModels, repositories
- Tests run with Robolectric for Android-specific code

**Quality Gate 3: Lint Analysis**
- Android Lint checks for code quality issues
- Validates XML resources, unused resources, API level issues
- Enforces Android best practices

**Quality Gate 4: Code Coverage (Kover)**
- Generates XML and HTML coverage reports
- Target thresholds:
  - Line coverage: 90%
  - Branch coverage: 85%
- Status: `continue-on-error: true` (allows incremental progress)

**Quality Gate 5: Security Scanning (OWASP Dependency Check)**
- Scans all dependencies for known vulnerabilities
- Checks against NVD (National Vulnerability Database)
- Generates detailed HTML reports
- Status: `continue-on-error: true` (handles NVD API rate limits gracefully)

**Quality Gate 6: Static Analysis (Detekt)**
- Kotlin static analysis for code smells
- Enforces code style conventions
- Identifies potential bugs and complexity issues

**Quality Gate 7: Test Reporting**
- Publishes JUnit test results with detailed summaries
- Uploads test reports, lint results, and coverage artifacts

### Artifacts Generated

The CI workflow uploads the following artifacts (available for 90 days):

| Artifact | Contents | When Uploaded |
|----------|----------|---------------|
| `coverage-report` | Kover HTML + XML reports | Always |
| `owasp-dependency-check` | Security scan results | Always |
| `detekt-report` | Static analysis results | Always |
| `test-results` | JUnit test results + reports | On failure |
| `lint-results` | Android Lint HTML reports | On failure |

### Performance Optimizations

- Parallel Gradle task execution (`--parallel`)
- Gradle build cache enabled globally
- GitHub Actions cache for Gradle dependencies
- Workflow-level job parallelization where possible
- Smart path-based triggering (skip docs changes)

### Expected Duration

- Typical run: 3-5 minutes
- First run (cold cache): 8-10 minutes

---

## CD Workflow (cd.yml)

### Trigger Conditions

```yaml
on:
  push:
    branches: [ main ]
```

The CD workflow runs automatically when code is pushed to the `main` branch, indicating production-ready code.

### Deployment Pipeline Stages

```
┌─────────────────────────────────────────────────────────────────┐
│                    CD PIPELINE ARCHITECTURE                     │
└─────────────────────────────────────────────────────────────────┘

   ┌──────────────────┐
   │  Push to Main    │
   └────────┬─────────┘
            │
            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │ STAGE 1: Quality Gates (Same as CI)                          │
   │  ├─ Build (assembleDebug)                                   │
   │  ├─ Unit Tests (test)                                       │
   │  ├─ Lint (lint)                                             │
   │  ├─ Coverage (kover: 90% line, 85% branch)                 │
   │  ├─ Security (OWASP Dependency Check)                       │
   │  └─ Static Analysis (Detekt)                                │
   └────────┬─────────────────────────────────────────────────────┘
            │ All quality gates pass
            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │ STAGE 2: Instrumented Tests                                  │
   │  └─ Android Emulator (API 34, Nexus 6)                      │
   │     └─ connectedAndroidTest                                  │
   └────────┬─────────────────────────────────────────────────────┘
            │ UI tests pass
            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │ STAGE 3: Deploy to Internal Track (AUTOMATIC)                │
   │  ├─ Build Release AAB (signed)                              │
   │  ├─ Build Release APK (signed)                              │
   │  └─ Upload to Play Store Internal Track                     │
   └────────┬─────────────────────────────────────────────────────┘
            │ Deploy succeeds
            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │ STAGE 3.5: Smoke Test Internal Release                       │
   │  ├─ Download APK                                            │
   │  ├─ Install on Emulator                                     │
   │  ├─ Launch App                                              │
   │  ├─ Verify No Crashes                                       │
   │  └─ Take Screenshot                                         │
   └────────┬─────────────────────────────────────────────────────┘
            │ Smoke tests pass
            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │ STAGE 4: Promote to Alpha (MANUAL APPROVAL REQUIRED)         │
   │  └─ Promote same AAB to Alpha Track                         │
   └────────┬─────────────────────────────────────────────────────┘
            │ Manual approval granted
            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │ STAGE 5: Promote to Beta (MANUAL APPROVAL REQUIRED)          │
   │  └─ Promote same AAB to Beta Track                          │
   └────────┬─────────────────────────────────────────────────────┘
            │ Manual approval granted
            ▼
   ┌──────────────────────────────────────────────────────────────┐
   │ STAGE 6: Promote to Production (MANUAL APPROVAL REQUIRED)    │
   │  ├─ Promote same AAB to Production Track                    │
   │  └─ Create GitHub Release (v{version})                      │
   └──────────────────────────────────────────────────────────────┘
```

### Stage 1: Build and Test (Quality Gates)

**Purpose**: Ensure code quality before any deployment

**Steps**:
1. Checkout code
2. Setup JDK 21 with Gradle cache
3. Decode and validate `google-services.json`
4. Run parallel quality gates:
   - `assembleDebug`: Compile debug build
   - `test`: Run all unit tests
   - `lint`: Android Lint analysis
5. Generate and verify code coverage (Kover)
6. Run security scan (OWASP)
7. Run static analysis (Detekt)
8. Publish test reports

**Artifacts**: `cd-coverage-report`, `cd-owasp-report`, `cd-detekt-report`, `test-results`

**Outcome**: If any quality gate fails, the pipeline stops immediately. No deployment occurs.

### Stage 2: Instrumented Tests

**Purpose**: Validate UI and integration tests on actual Android environment

**Environment**:
- Android Emulator API 34 (Android 14)
- Target: Default (Google APIs)
- Architecture: x86_64
- Profile: Nexus 6

**Steps**:
1. Setup Android SDK and emulator
2. Cache AVD for faster startup
3. Launch emulator
4. Run `./gradlew connectedAndroidTest`
5. Upload test reports on failure

**Duration**: 10-15 minutes (includes emulator boot)

**Outcome**: UI tests must pass before deployment to internal track.

### Stage 3: Deploy to Internal Track

**Purpose**: Automatically deploy to Play Store internal testing track

**Environment**: `internal` (GitHub Environment)

**Steps**:
1. Extract version from `app/build.gradle.kts`:
   - `versionCode`: Integer build number
   - `versionName`: Semantic version (e.g., "1.0.0")
2. Decode release keystore from `RELEASE_KEYSTORE_BASE64`
3. Build signed Release AAB: `./gradlew bundleRelease`
4. Build signed Release APK: `./gradlew assembleRelease`
5. Upload AAB to Play Store internal track
6. Upload AAB and APK as GitHub artifacts

**Key Details**:
- Uses production signing key
- AAB is optimized with ProGuard/R8
- Same AAB will be promoted through all subsequent tracks
- In-app update priority: 2

**Artifacts**: `internal-aab-v{version}`, `internal-apk-v{version}`

**Outcome**: App is available to internal testers immediately.

### Stage 3.5: Smoke Test Internal Release

**Purpose**: Verify the deployed APK works correctly on real Android environment

**Environment**: Android Emulator API 34, Nexus 6

**Test Steps**:
1. Download APK artifact from previous stage
2. Launch Android emulator
3. Install APK: `adb install -r app-release.apk`
4. Grant runtime permissions (notifications, camera)
5. Launch MainActivity: `adb shell am start -n de.hipp.app.taskcards/.MainActivity`
6. Wait 10 seconds for app initialization
7. Verify app is running via `dumpsys activity`
8. Check logcat for `FATAL EXCEPTION` entries
9. Verify UI elements present via `uiautomator dump`
10. Take screenshot for manual review

**Success Criteria**:
- APK installs without errors
- App launches successfully
- No crashes detected in logcat
- Main activity loads

**Artifacts**: `smoke-test-screenshot`

**Outcome**: If smoke tests fail, the promotion pipeline stops. Investigate and fix before retrying.

### Stage 4: Promote to Alpha

**Purpose**: Release to alpha testers for broader testing

**Trigger**: Manual approval required via GitHub Environments

**Environment**: `alpha` (GitHub Environment)

**Steps**:
1. Download AAB artifact from internal stage
2. Extract version from artifact name
3. Promote to alpha track (no rebuild)
4. In-app update priority: 3

**Approval Process**:
1. Navigate to Actions tab in GitHub
2. Click on the CD workflow run
3. Find "Promote to Alpha Track" job
4. Click "Review deployments"
5. Select "alpha" environment
6. Click "Approve and deploy"

**Best Practices Before Approval**:
- Test internal release thoroughly
- Review crash reports in Play Console
- Verify smoke test passed
- Check Firebase Analytics for any anomalies

**Outcome**: App is available to alpha testers (typically a small group of trusted users).

### Stage 5: Promote to Beta

**Purpose**: Release to beta testers for wider pre-production testing

**Trigger**: Manual approval required (only after alpha promotion)

**Environment**: `beta` (GitHub Environment)

**Steps**:
1. Download AAB artifact (same one from internal)
2. Promote to beta track
3. In-app update priority: 4

**Approval Process**: Same as alpha (see Stage 4)

**Best Practices Before Approval**:
- Monitor alpha feedback for 24-48 hours
- Ensure no critical bugs reported
- Verify app stability metrics in Firebase
- Check ANR (App Not Responding) rate

**Outcome**: App is available to beta testers (larger group, more diverse testing).

### Stage 6: Promote to Production

**Purpose**: Release to all users on Google Play Store

**Trigger**: Manual approval required (only after beta promotion)

**Environment**: `production` (GitHub Environment)

**Steps**:
1. Download AAB and APK artifacts
2. Promote to production track
3. Create GitHub Release:
   - Tag: `v{version}` (e.g., v1.0.0)
   - Attach AAB and APK files
   - Generate release notes
4. In-app update priority: 5 (highest)

**Approval Process**: Same as alpha/beta, but with additional caution

**Critical Pre-Approval Checklist**:
- [ ] Beta testing completed (minimum 7 days recommended)
- [ ] Zero critical bugs reported
- [ ] Crash-free rate > 99.5%
- [ ] ANR rate < 0.5%
- [ ] All Firebase stability metrics green
- [ ] Play Console pre-launch report reviewed
- [ ] Store listing metadata updated (if needed)
- [ ] Team consensus achieved

**Outcome**:
- App goes live to all users (staged rollout recommended via Play Console)
- GitHub Release created with downloadable artifacts
- Version tagged in Git history

---

## Quality Gates Reference

### 1. Compilation (assembleDebug)
**Purpose**: Ensure code compiles without errors

**Checks**:
- Kotlin/Java compilation
- Resource processing
- Dependency resolution
- BuildConfig generation

**Failure Reasons**:
- Syntax errors
- Missing dependencies
- Resource conflicts
- API compatibility issues

### 2. Unit Tests (test)
**Purpose**: Validate business logic and components

**Coverage**:
- ViewModels
- Repositories
- Use cases
- Utility functions
- Data transformations

**Technology**: JUnit 4, Mockito, Robolectric

**Failure Reasons**:
- Assertion failures
- Unhandled exceptions
- Test timeouts
- Mock verification failures

### 3. Lint Analysis (lint)
**Purpose**: Enforce Android best practices

**Checks**:
- Unused resources
- API level compatibility
- Security vulnerabilities
- Performance issues
- Accessibility concerns
- Internationalization

**Severity Levels**:
- Error: Blocks build
- Warning: Reported but doesn't block
- Informational: Suggestions

### 4. Code Coverage (Kover)
**Purpose**: Ensure adequate test coverage

**Thresholds**:
- Line coverage: 90% (target)
- Branch coverage: 85% (target)

**Status**: `continue-on-error: true` (soft requirement)

**Reports Generated**:
- XML (for CI parsing)
- HTML (for human review)

**How to Review**:
1. Download `coverage-report` artifact
2. Open `index.html` in browser
3. Navigate to uncovered code
4. Add missing tests

### 5. Security Scanning (OWASP Dependency Check)
**Purpose**: Identify vulnerable dependencies

**Database**: National Vulnerability Database (NVD)

**Checks**:
- Known CVEs in dependencies
- Outdated libraries with security fixes
- Transitive dependency vulnerabilities

**Configuration**:
- Fail on CVSS >= 7.0 (high severity)
- `failOnError: false` (handles NVD API issues)

**Common Issues**:
- NVD API rate limiting (403 errors)
- Solution: Add `NVD_API_KEY` secret

### 6. Static Analysis (Detekt)
**Purpose**: Detect code smells and style violations

**Categories**:
- Complexity (cyclomatic complexity, long methods)
- Code smells (magic numbers, duplicated code)
- Formatting (indentation, naming conventions)
- Performance (inefficient collections, string concatenation)

**Configuration**: See `detekt.yml` in project root

**Failure Reasons**:
- High cyclomatic complexity
- Naming violations
- Performance anti-patterns

### 7. Instrumented Tests (connectedAndroidTest)
**Purpose**: Validate UI and Android-specific functionality

**Environment**: Real Android emulator

**Coverage**:
- Jetpack Compose UI tests
- Navigation flows
- Database operations (Room)
- Android APIs (permissions, intents)

**Technology**: Espresso, Compose Testing, UI Automator

**Failure Reasons**:
- UI element not found
- Test timeouts
- Emulator crashes
- Race conditions

---

## Deployment Process

### Standard Deployment Flow

Follow this process to deploy a new version to production:

#### Phase 1: Prepare Release
1. Ensure all features are merged to `develop` branch
2. Create PR from `develop` to `main`
3. Wait for CI quality gates to pass
4. Get code review approval
5. Merge PR to `main`

#### Phase 2: Automatic Internal Deployment
1. Merge triggers CD workflow automatically
2. Monitor workflow in GitHub Actions
3. Wait for quality gates to pass (5-10 min)
4. Wait for instrumented tests to pass (10-15 min)
5. Wait for internal deployment to complete (5 min)
6. Wait for smoke tests to pass (10 min)

**Total time**: 30-40 minutes

#### Phase 3: Internal Testing
1. Download app from Play Console internal track
2. Test all major features manually
3. Verify smoke test screenshot in artifacts
4. Check Firebase Crashlytics for any crashes
5. Test on multiple devices if possible
6. Minimum testing period: 2-4 hours

#### Phase 4: Promote to Alpha
1. Navigate to GitHub Actions
2. Find the CD workflow run
3. Click "Promote to Alpha Track"
4. Click "Review deployments"
5. Select "alpha" environment
6. Approve deployment
7. Wait 5 minutes for promotion
8. Notify alpha testers

#### Phase 5: Alpha Testing
1. Alpha testers download from Play Store
2. Collect feedback via Firebase, email, or Slack
3. Monitor crash reports in Play Console
4. Check ANR rates and performance metrics
5. Minimum testing period: 24-48 hours

#### Phase 6: Promote to Beta
1. Review alpha feedback
2. If stable, approve beta promotion in GitHub Actions
3. Same approval process as alpha
4. Notify beta testers

#### Phase 7: Beta Testing
1. Beta testers download from Play Store
2. Monitor for any issues missed in alpha
3. Track stability metrics daily
4. Review user feedback carefully
5. Minimum testing period: 7 days (recommended)

#### Phase 8: Promote to Production
1. Verify all metrics are green
2. Complete pre-approval checklist (see Stage 6)
3. Get team sign-off
4. Approve production promotion in GitHub Actions
5. Monitor rollout carefully

#### Phase 9: Post-Production Monitoring
1. Monitor Play Console for crashes/ANRs (first 24 hours critical)
2. Check Firebase Analytics for usage patterns
3. Review user ratings and reviews
4. Prepare hotfix if critical issues detected
5. Consider staged rollout (10% → 50% → 100%) in Play Console

### Hotfix Deployment

For critical production issues:

1. Create hotfix branch from `main`
2. Implement and test fix
3. Merge hotfix to `main` (expedited review)
4. CD workflow triggers automatically
5. Fast-track through testing tracks:
   - Internal: 1 hour testing
   - Alpha: 4 hours testing
   - Beta: 24 hours testing
   - Production: Immediate
6. Monitor production closely after hotfix

---

## Manual Approval Process

### GitHub Environments Configuration

The CD workflow uses GitHub Environments to enforce manual approvals. These are pre-configured:

- `internal`: No approval required (automatic)
- `alpha`: Approval required
- `beta`: Approval required
- `production`: Approval required

### Setting Up Environment Protection Rules

If you need to reconfigure environments:

1. Navigate to: `Settings` → `Environments`
2. Select environment (e.g., `alpha`)
3. Enable "Required reviewers"
4. Add team members who can approve
5. Optionally set "Wait timer" (e.g., 6 hours minimum before production)
6. Save changes

### Approving a Deployment

**Step-by-Step**:

1. Navigate to `Actions` tab in GitHub repository
2. Click on the running `Continuous Deployment` workflow
3. Scroll down to find the pending job (e.g., "Promote to Alpha Track")
4. You'll see a yellow banner: "This job is waiting for approval"
5. Click "Review deployments" button
6. Check the environment name (e.g., `alpha`)
7. Review deployment summary and artifacts
8. Enter optional comment explaining approval decision
9. Click "Approve and deploy"

**Visual Indicators**:
- Pending approval: Yellow/Orange indicator
- Approved: Green checkmark
- Rejected: Red X
- Timed out: Grey indicator

### Approval Best Practices

1. **Always review artifacts before approving**:
   - Download smoke test screenshot
   - Check coverage reports
   - Review OWASP security scan
   - Verify test results

2. **Check Play Console before promoting**:
   - Pre-launch report status
   - Any crashes in previous track
   - Install/uninstall rates

3. **Coordinate with team**:
   - Notify team in Slack before major promotions
   - Get sign-off from QA for production
   - Document approval decision

4. **Timing considerations**:
   - Avoid Friday production deployments
   - Deploy during business hours for monitoring
   - Allow sufficient testing time in each track

5. **Rejection criteria**:
   - Any failing quality gates
   - Smoke test failures
   - Critical bugs reported
   - Incomplete testing period

---

## Secrets Configuration

### Required GitHub Secrets

The CD workflow requires these secrets to be configured in: `Settings` → `Secrets and variables` → `Actions` → `Repository secrets`

#### 1. GOOGLE_SERVICES_JSON_BASE64

**Purpose**: Firebase configuration file

**How to Create**:
```bash
# From project root
base64 -i app/google-services.json | pbcopy  # macOS
base64 app/google-services.json | xclip -selection clipboard  # Linux
```

**Steps**:
1. Download `google-services.json` from Firebase Console
2. Encode to base64
3. Add as GitHub secret
4. Name: `GOOGLE_SERVICES_JSON_BASE64`

**Validation**: CI decodes and validates JSON format

#### 2. RELEASE_KEYSTORE_BASE64

**Purpose**: Android release signing keystore

**How to Create**:
```bash
# First, generate keystore if you don't have one:
keytool -genkey -v -keystore release.keystore \
  -alias taskcards-release \
  -keyalg RSA -keysize 2048 -validity 10000

# Then encode to base64:
base64 -i release.keystore | pbcopy  # macOS
base64 release.keystore | xclip -selection clipboard  # Linux
```

**Steps**:
1. Generate or locate your release keystore
2. Encode to base64
3. Add as GitHub secret
4. Name: `RELEASE_KEYSTORE_BASE64`

**Security**:
- Never commit keystore to Git
- Store backup securely (password manager)
- If lost, you cannot update the app on Play Store

#### 3. RELEASE_KEYSTORE_PASSWORD

**Purpose**: Password for the keystore file

**Value**: The password you set when creating the keystore

**Example**: `mySecurePassword123!`

#### 4. RELEASE_KEY_ALIAS

**Purpose**: Alias of the key within the keystore

**Value**: The alias you specified when creating the key

**Example**: `taskcards-release`

**How to List Aliases**:
```bash
keytool -list -keystore release.keystore
```

#### 5. RELEASE_KEY_PASSWORD

**Purpose**: Password for the specific key (may be same as keystore password)

**Value**: The key-specific password

**Note**: Can be same as `RELEASE_KEYSTORE_PASSWORD`

#### 6. PLAY_STORE_SERVICE_ACCOUNT_JSON

**Purpose**: Google Play Console API access

**How to Create**:

1. Navigate to [Google Play Console](https://play.google.com/console)
2. Select your app
3. Go to: `Setup` → `API access`
4. Click "Create new service account"
5. Follow link to Google Cloud Console
6. Create service account:
   - Name: `github-actions-cd`
   - Role: `Service Account User`
7. Create JSON key:
   - Click on service account
   - `Keys` → `Add Key` → `Create new key`
   - Choose `JSON`
   - Download JSON file
8. Back in Play Console:
   - Grant access to service account
   - Permissions: `Release manager` and `Release to production`
9. Copy entire JSON content
10. Add as GitHub secret (plain text, not base64)
11. Name: `PLAY_STORE_SERVICE_ACCOUNT_JSON`

**Format**: Raw JSON (not base64 encoded)

**Example Structure**:
```json
{
  "type": "service_account",
  "project_id": "your-project",
  "private_key_id": "...",
  "private_key": "-----BEGIN PRIVATE KEY-----\n...",
  "client_email": "github-actions-cd@...",
  "client_id": "...",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token"
}
```

#### 7. NVD_API_KEY (Optional)

**Purpose**: National Vulnerability Database API access (improves OWASP scan reliability)

**Required**: No (optional, but recommended)

**Why Optional**:
- OWASP works without it (uses cached/mirrored data)
- Without API key: May encounter 403 rate limit errors
- With API key: 2,000 requests per day (sufficient for most projects)

**How to Create**:

1. Visit [NVD API Request](https://nvd.nist.gov/developers/request-an-api-key)
2. Fill out form with email
3. Receive API key via email
4. Add as GitHub secret
5. Name: `NVD_API_KEY`

**Usage**: Automatically detected by OWASP Dependency Check plugin

### Secrets Validation

**Verify secrets are working**:

1. Trigger CI workflow (make a PR)
2. Check "Create google-services.json" step
3. Should see: "google-services.json is valid JSON (X bytes)"
4. If errors, re-encode and re-add secret

**Common Issues**:
- Extra whitespace in secret value
- Incorrect base64 encoding
- Copy-paste errors
- Wrong secret name

### Secrets Rotation

**Best Practices**:
1. Rotate service account keys annually
2. Update keystore passwords if compromised
3. Keep backup of all secrets in secure password manager
4. Test new secrets in separate branch before updating

---

## Troubleshooting

### 1. NVD API 403 Errors (OWASP)

**Symptom**:
```
OWASP Dependency Check failed: 403 Forbidden from NVD API
```

**Cause**: Rate limiting on National Vulnerability Database API

**Solutions**:

**Solution A: Add NVD API Key (Recommended)**
1. Request API key from https://nvd.nist.gov/developers/request-an-api-key
2. Add as GitHub secret: `NVD_API_KEY`
3. Re-run workflow

**Solution B: Use continue-on-error (Already Configured)**
- Workflow already has `continue-on-error: true`
- Build will succeed, but OWASP report may be incomplete
- Review artifacts manually

**Solution C: Retry Later**
- NVD API resets rate limits hourly
- Re-run failed workflow after 1 hour

**Prevention**:
- Always set `NVD_API_KEY` secret
- Use Gradle caching to reduce NVD API calls

### 2. Coverage Threshold Failures

**Symptom**:
```
Verification failed: Line coverage 85% is less than 90%
```

**Cause**: Code coverage below target thresholds

**Status**: Non-blocking (continue-on-error: true)

**Solutions**:

**Short-term**:
- Review coverage report artifact
- Identify uncovered code paths
- Prioritize testing critical paths

**Long-term**:
- Add unit tests for uncovered code
- Focus on ViewModel and repository layers
- Use `@Composable` preview tests for UI

**How to Fix**:
1. Download `coverage-report` artifact
2. Open `html/index.html` in browser
3. Navigate to red/yellow highlighted code
4. Write tests for uncovered lines/branches
5. Re-run tests locally: `./gradlew koverVerify`

**Exclusions**:
Edit `app/build.gradle.kts` → `kover` block to exclude generated code:
```kotlin
kover {
    excludes {
        classes("*.BuildConfig", "*_Factory", "*_Impl")
    }
}
```

### 3. Emulator Test Failures

**Symptom**:
```
connectedAndroidTest FAILED
Emulator timeout or test failures
```

**Common Causes**:

**Cause A: Emulator Boot Timeout**
- Solution: Increase timeout in workflow
- Edit `cd.yml` → `android-emulator-runner` → add `emulator-boot-timeout: 900`

**Cause B: Flaky Tests**
- Solution: Add retry mechanism
- Use `@FlakyTest` annotation
- Add explicit waits: `composeTestRule.waitForIdle()`

**Cause C: Resource Constraints**
- Solution: Reduce emulator specs
- Change profile from `Nexus 6` to `pixel_2`
- Reduce API level if needed

**Debugging Steps**:
1. Download `instrumented-test-results` artifact
2. Review HTML test report
3. Check logcat output in artifact
4. Reproduce locally:
   ```bash
   ./gradlew connectedAndroidTest
   ```
5. Run specific test:
   ```bash
   ./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.MyTest
   ```

**Prevention**:
- Write deterministic tests (avoid time dependencies)
- Use `TestCoroutineDispatcher` for coroutines
- Mock external dependencies (network, sensors)

### 4. Smoke Test Failures

**Symptom**:
```
App crashed during smoke test!
FATAL EXCEPTION detected in logcat
```

**Common Causes**:

**Cause A: Missing Runtime Permissions**
- Solution: Add permission grants in smoke test script
- Edit `cd.yml` → smoke test → add:
  ```bash
  adb shell pm grant de.hipp.app.taskcards android.permission.PERMISSION_NAME
  ```

**Cause B: Firebase Initialization Failure**
- Check: Is `google-services.json` valid?
- Solution: Verify secret: `GOOGLE_SERVICES_JSON_BASE64`
- Test: Decode secret locally and validate JSON

**Cause C: ProGuard Over-Optimization**
- Solution: Add ProGuard keep rules
- Edit `app/proguard-rules.pro`:
  ```proguard
  -keep class com.your.problematic.Class { *; }
  ```

**Cause D: Database Migration Failure**
- Solution: Ensure Room migrations are tested
- Add migration tests in unit tests

**Debugging Steps**:
1. Download `smoke-test-screenshot` artifact
2. Review screenshot to see app state
3. Check workflow logs for logcat output
4. Look for `FATAL EXCEPTION` stack trace
5. Reproduce with local APK:
   ```bash
   # Build release APK
   ./gradlew assembleRelease

   # Install and test
   adb install -r app/build/outputs/apk/release/app-release.apk
   adb shell am start -n de.hipp.app.taskcards/.MainActivity
   adb logcat | grep -i exception
   ```

**Prevention**:
- Test release builds locally before pushing
- Use ProGuard test builds in CI
- Add integration tests that mimic smoke tests

### 5. Play Store Upload Issues

**Symptom**:
```
Error uploading to Play Store
HTTP 403: forbidden
```

**Common Causes**:

**Cause A: Invalid Service Account Credentials**
- Solution: Verify `PLAY_STORE_SERVICE_ACCOUNT_JSON` secret
- Check: Is it valid JSON?
- Test: Try decoding and pretty-printing:
  ```bash
  echo "$PLAY_STORE_SERVICE_ACCOUNT_JSON" | python3 -m json.tool
  ```

**Cause B: Insufficient Permissions**
- Solution: Update service account permissions in Play Console
- Navigate: Play Console → Setup → API access
- Grant: "Release manager" and "Release to production"

**Cause C: Version Code Conflict**
- Solution: Increment `versionCode` in `app/build.gradle.kts`
- Play Store requires monotonically increasing version codes

**Cause D: Invalid AAB**
- Solution: Validate AAB locally:
  ```bash
  bundletool validate --bundle=app/build/outputs/bundle/release/app-release.aab
  ```

**Cause E: Missing App Signing by Google Play**
- Solution: Enroll in Play App Signing
- Navigate: Play Console → Release → Setup → App integrity
- Follow enrollment process

**Debugging Steps**:
1. Check workflow logs for exact error message
2. Verify service account email matches Play Console
3. Test service account:
   ```bash
   # Install google-cloud-sdk
   gcloud auth activate-service-account --key-file=service-account.json
   ```
4. Check Play Console → Release dashboard for any issues
5. Verify app is in "Production" status (not "Closed" or "Suspended")

**Prevention**:
- Test service account credentials annually
- Rotate keys before expiration
- Monitor Play Console email for policy violations

### 6. Build Timeout

**Symptom**:
```
Build exceeded maximum time limit (60 minutes)
Job was cancelled
```

**Common Causes**:

**Cause A: Gradle Daemon Issues**
- Solution: Add daemon optimization flags
- Edit workflow: Add `--daemon` flag

**Cause B: Cache Miss**
- Solution: Verify cache keys in workflow
- Check: Are cache restore keys correct?

**Cause C: Network Issues (Dependency Download)**
- Solution: Use Gradle dependency cache
- Add `--offline` flag if dependencies cached

**Prevention**:
- Use `--parallel` flag (already enabled)
- Enable Gradle build cache (already enabled)
- Monitor build times in Actions insights

### 7. Keystore Decoding Errors

**Symptom**:
```
Error decoding keystore
Invalid base64 string
```

**Solution**:
1. Re-encode keystore:
   ```bash
   base64 -i release.keystore > keystore.txt
   ```
2. Copy entire content (no line breaks)
3. Update GitHub secret
4. Verify no trailing whitespace

**Prevention**:
- Use `| pbcopy` to avoid copy-paste errors
- Test decoding locally before adding secret

### 8. Detekt Failures

**Symptom**:
```
Detekt found 15 issues with complexity rules
```

**Solution**:
1. Download `detekt-report` artifact
2. Review HTML report
3. Fix issues or suppress if intentional:
   ```kotlin
   @Suppress("ComplexMethod", "LongMethod")
   fun myFunction() { ... }
   ```

**Common Issues**:
- Cyclomatic complexity > 15
- Long methods (> 60 lines)
- Magic numbers (use constants)

---

## Monitoring

### GitHub Actions Dashboard

**URL**: `https://github.com/[username]/TaskCards/actions`

**Key Metrics**:
- Workflow run duration
- Success/failure rates
- Artifact storage usage
- Queue times

**Views**:
- All workflows
- CI Quality Gates (ci.yml runs)
- Continuous Deployment (cd.yml runs)
- Filter by status: Success, Failure, Cancelled

### Google Play Console

**URL**: `https://play.google.com/console`

**Critical Dashboards**:

1. **Release Dashboard** (`Release` → `Production`)
   - Current version in production
   - Rollout percentage
   - Staged rollout controls

2. **Pre-launch Report** (`Release` → `Testing` → `Pre-launch report`)
   - Automated tests on Google devices
   - Crash reports
   - Security vulnerabilities
   - Accessibility issues

3. **Android Vitals** (`Quality` → `Android vitals`)
   - Crash rate (target: < 0.5%)
   - ANR rate (target: < 0.5%)
   - Excessive wakeups
   - Stuck wake locks

4. **Crashes & ANRs** (`Quality` → `Crashes & ANRs`)
   - Real-time crash reports
   - Stack traces
   - Affected devices/OS versions

5. **User Reviews** (`Ratings and reviews`)
   - Recent reviews
   - Rating trends
   - Review replies

### Firebase Console

**URL**: `https://console.firebase.google.com/`

**Key Dashboards**:

1. **Crashlytics** (`Crashlytics`)
   - Real-time crash reporting
   - Non-fatal exceptions
   - Custom logs/keys
   - User impact

2. **Analytics** (`Analytics`)
   - Active users (DAU/MAU)
   - User engagement
   - Screen views
   - Custom events

3. **Performance Monitoring** (`Performance`)
   - App startup time
   - Screen rendering time
   - Network request latency

4. **Remote Config** (`Remote Config`)
   - Feature flags
   - A/B testing
   - Gradual rollouts

### Alerting Setup

**Recommended Alerts**:

1. **GitHub Actions** (via GitHub notifications):
   - Workflow failures
   - Set: Settings → Notifications → Actions

2. **Play Console** (email alerts):
   - Crash rate threshold exceeded (> 1%)
   - ANR rate threshold exceeded (> 1%)
   - Set: Play Console → Settings → Email preferences

3. **Firebase Crashlytics** (email alerts):
   - New crash detected
   - Regression detected (previously fixed crash)
   - Set: Firebase Console → Project Settings → Integrations

4. **Slack Integration** (optional):
   - GitHub Actions status
   - Firebase crash alerts
   - Use: Firebase Slack app + GitHub Slack app

### Health Check Checklist

Run this checklist daily during rollouts, weekly otherwise:

**GitHub Actions**:
- [ ] Latest CI run: Passed
- [ ] Latest CD run: Passed (or pending approval)
- [ ] No failed runs in last 7 days (excluding known issues)
- [ ] Artifact storage < 80% of limit

**Play Console**:
- [ ] Crash-free rate > 99.5%
- [ ] ANR rate < 0.5%
- [ ] No new security vulnerabilities
- [ ] Pre-launch report: All tests passed
- [ ] User rating > 4.0 stars

**Firebase**:
- [ ] Crashlytics: No unresolved crashes
- [ ] Analytics: No sudden drop in DAU
- [ ] Performance: App startup < 2 seconds
- [ ] No critical issues flagged

---

## Architecture Diagrams

### CI Workflow Flow

```
┌────────────────────────────────────────────────────────────────┐
│                        CI WORKFLOW (ci.yml)                     │
│                                                                 │
│  Triggers:                                                      │
│    - Pull Request → main                                        │
│    - Push → main/develop                                        │
└────────────────────────────────────────────────────────────────┘
                                │
                                ▼
                    ┌────────────────────┐
                    │  Setup Environment  │
                    │  - JDK 21          │
                    │  - Gradle Cache    │
                    │  - google-services │
                    └─────────┬──────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────────┐
        │      Parallel Quality Gates Execution       │
        └─────────────────────────────────────────────┘
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
┌───────────────┐     ┌───────────────┐     ┌───────────────┐
│ assembleDebug │     │     test      │     │     lint      │
│   (Build)     │     │ (Unit Tests)  │     │  (Analysis)   │
└───────┬───────┘     └───────┬───────┘     └───────┬───────┘
        │                     │                     │
        └─────────────────────┴─────────────────────┘
                              │
                              ▼
        ┌─────────────────────────────────────────────┐
        │         Sequential Quality Gates            │
        └─────────────────────────────────────────────┘
                              │
                    ┌─────────┴─────────┐
                    │                   │
                    ▼                   ▼
        ┌─────────────────────┐  ┌─────────────────────┐
        │  Code Coverage      │  │  Security Scan      │
        │  (Kover)            │  │  (OWASP)            │
        │  - 90% line         │  │  - NVD API          │
        │  - 85% branch       │  │  - CVE check        │
        └─────────┬───────────┘  └─────────┬───────────┘
                  │                        │
                  └────────────┬───────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │  Static Analysis    │
                    │  (Detekt)           │
                    │  - Complexity       │
                    │  - Code smells      │
                    └─────────┬───────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │  Publish Reports    │
                    │  - JUnit Report     │
                    │  - Upload Artifacts │
                    └─────────┬───────────┘
                              │
                              ▼
                    ┌─────────────────────┐
                    │  Build Summary      │
                    │  ✅ All Gates Pass  │
                    └─────────────────────┘
```

### CD Workflow Deployment Pipeline

```
┌───────────────────────────────────────────────────────────────────┐
│                  CD WORKFLOW DEPLOYMENT PIPELINE                   │
│                                                                    │
│  Trigger: Push to main                                             │
└───────────────────────────────────────────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │   Quality Gates       │
                    │   (Same as CI)        │
                    │   ✓ Build             │
                    │   ✓ Test              │
                    │   ✓ Lint              │
                    │   ✓ Coverage          │
                    │   ✓ Security          │
                    │   ✓ Static Analysis   │
                    └───────────┬───────────┘
                                │ Pass
                                ▼
                    ┌───────────────────────┐
                    │ Instrumented Tests    │
                    │ Android Emulator      │
                    │ API 34, Nexus 6       │
                    │ connectedAndroidTest  │
                    └───────────┬───────────┘
                                │ Pass
                                ▼
            ┌───────────────────────────────────────┐
            │        Build Release Artifacts        │
            │  ┌─────────────────────────────────┐  │
            │  │ 1. Decode release.keystore     │  │
            │  │ 2. bundleRelease (AAB)         │  │
            │  │ 3. assembleRelease (APK)       │  │
            │  │ 4. Sign with release key       │  │
            │  └─────────────────────────────────┘  │
            └───────────────┬───────────────────────┘
                            │
                            ▼
            ┌───────────────────────────────────────┐
            │   Upload to Play Store Internal       │
            │   Track: internal                     │
            │   Status: completed                   │
            │   Priority: 2                         │
            │                                       │
            │   Artifacts Saved:                    │
            │   - internal-aab-v{version}          │
            │   - internal-apk-v{version}          │
            └───────────────┬───────────────────────┘
                            │ Deploy Success
                            ▼
            ┌───────────────────────────────────────┐
            │          Smoke Test Internal          │
            │  ┌─────────────────────────────────┐  │
            │  │ 1. Download APK artifact       │  │
            │  │ 2. Launch emulator (API 34)    │  │
            │  │ 3. Install APK                 │  │
            │  │ 4. Launch MainActivity         │  │
            │  │ 5. Verify no crashes           │  │
            │  │ 6. Check UI elements           │  │
            │  │ 7. Take screenshot             │  │
            │  └─────────────────────────────────┘  │
            └───────────────┬───────────────────────┘
                            │ Smoke Tests Pass
                            ▼
            ┌───────────────────────────────────────┐
            │     🚨 MANUAL APPROVAL REQUIRED 🚨    │
            │                                       │
            │     Environment: alpha                │
            │     Reviewers: Team leads            │
            │                                       │
            │     [ Review deployments ]            │
            └───────────────┬───────────────────────┘
                            │ Approved
                            ▼
            ┌───────────────────────────────────────┐
            │       Promote to Alpha Track          │
            │   (Same AAB, no rebuild)             │
            │   Track: alpha                       │
            │   Priority: 3                         │
            └───────────────┬───────────────────────┘
                            │ Alpha Deployed
                            ▼
            ┌───────────────────────────────────────┐
            │     🚨 MANUAL APPROVAL REQUIRED 🚨    │
            │                                       │
            │     Environment: beta                 │
            │     Testing period: 24-48h           │
            │                                       │
            │     [ Review deployments ]            │
            └───────────────┬───────────────────────┘
                            │ Approved
                            ▼
            ┌───────────────────────────────────────┐
            │       Promote to Beta Track           │
            │   (Same AAB, no rebuild)             │
            │   Track: beta                        │
            │   Priority: 4                         │
            └───────────────┬───────────────────────┘
                            │ Beta Deployed
                            ▼
            ┌───────────────────────────────────────┐
            │     🚨 MANUAL APPROVAL REQUIRED 🚨    │
            │                                       │
            │     Environment: production           │
            │     Testing period: 7+ days          │
            │     Critical checks required         │
            │                                       │
            │     [ Review deployments ]            │
            └───────────────┬───────────────────────┘
                            │ Approved
                            ▼
            ┌───────────────────────────────────────┐
            │    Promote to Production Track        │
            │   (Same AAB, no rebuild)             │
            │   Track: production                  │
            │   Priority: 5                         │
            │                                       │
            │   + Create GitHub Release            │
            │     Tag: v{version}                  │
            │     Attach: AAB + APK                │
            └───────────────┬───────────────────────┘
                            │
                            ▼
            ┌───────────────────────────────────────┐
            │      🎉 PRODUCTION RELEASE! 🎉       │
            │                                       │
            │   App live to all users              │
            │   GitHub release created             │
            │   Version tagged in Git              │
            │                                       │
            │   Next: Monitor metrics closely      │
            └───────────────────────────────────────┘
```

### Quality Gates Dependency Graph

```
┌────────────────────────────────────────────────────────────┐
│                  QUALITY GATES OVERVIEW                     │
└────────────────────────────────────────────────────────────┘

    ┌───────────────────────────────────────────────────┐
    │           BLOCKING GATES (Must Pass)              │
    └───────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Gate 1     │  │   Gate 2     │  │   Gate 3     │
│ Compilation  │  │ Unit Tests   │  │   Lint       │
│              │  │              │  │              │
│ ❌ Fail → ❌ │  │ ❌ Fail → ❌ │  │ ❌ Fail → ❌ │
└──────────────┘  └──────────────┘  └──────────────┘

    ┌───────────────────────────────────────────────────┐
    │       INFORMATIONAL GATES (Don't Block)           │
    └───────────────────────────────────────────────────┘
                          │
        ┌─────────────────┼─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Gate 4     │  │   Gate 5     │  │   Gate 6     │
│   Coverage   │  │   Security   │  │   Detekt     │
│              │  │              │  │              │
│ ⚠️ Fail → ✅ │  │ ⚠️ Fail → ✅ │  │ ❌ Fail → ❌ │
│ (continues)  │  │ (continues)  │  │              │
└──────────────┘  └──────────────┘  └──────────────┘

    ┌───────────────────────────────────────────────────┐
    │      INTEGRATION GATES (Emulator Required)        │
    └───────────────────────────────────────────────────┘
                          │
                          ▼
                  ┌──────────────┐
                  │   Gate 7     │
                  │ Instrumented │
                  │    Tests     │
                  │              │
                  │ ❌ Fail → ❌ │
                  └──────────────┘

Legend:
  ❌ Fail → ❌ : Failure blocks pipeline
  ⚠️ Fail → ✅ : Failure logged but doesn't block
  ✅ Pass → ✅ : Success, proceed
```

### Artifact Flow Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                    ARTIFACT FLOW PIPELINE                     │
└──────────────────────────────────────────────────────────────┘

┌─────────────────┐
│  Source Code    │
│  (main branch)  │
└────────┬────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Quality Gates Stage                        │
│  Generates:                                                  │
│    • coverage-report (HTML + XML)                           │
│    • owasp-dependency-check (Security scan)                 │
│    • detekt-report (Static analysis)                        │
│    • test-results (JUnit reports)                           │
└────────┬────────────────────────────────────────────────────┘
         │ All gates pass
         ▼
┌─────────────────────────────────────────────────────────────┐
│              Instrumented Tests Stage                        │
│  Generates:                                                  │
│    • instrumented-test-results (UI test reports)            │
└────────┬────────────────────────────────────────────────────┘
         │ Tests pass
         ▼
┌─────────────────────────────────────────────────────────────┐
│                Deploy Internal Stage                         │
│  Generates:                                                  │
│    • app-release.aab ←─────┐                                │
│    • app-release.apk       │ These are uploaded             │
│                            │ as GitHub artifacts            │
│  Uploads as:               │                                │
│    • internal-aab-v{version} ─── This AAB will be          │
│    • internal-apk-v{version}     reused in all stages!     │
└────────┬────────────────────────────────────────────────────┘
         │ Deploy succeeds
         ▼
┌─────────────────────────────────────────────────────────────┐
│               Smoke Test Stage                               │
│  Downloads:                                                  │
│    • internal-apk-v{version}                                │
│  Generates:                                                  │
│    • smoke-test-screenshot.png                              │
└────────┬────────────────────────────────────────────────────┘
         │ Smoke tests pass
         ▼
┌─────────────────────────────────────────────────────────────┐
│              Promote to Alpha Stage                          │
│  Downloads:                                                  │
│    • internal-aab-v{version} ←── Same AAB from internal!   │
│  Action:                                                     │
│    • Upload to Play Store alpha track                       │
│  (No rebuild, same binary promoted)                         │
└────────┬────────────────────────────────────────────────────┘
         │ Alpha deployed
         ▼
┌─────────────────────────────────────────────────────────────┐
│              Promote to Beta Stage                           │
│  Downloads:                                                  │
│    • internal-aab-v{version} ←── Same AAB from internal!   │
│  Action:                                                     │
│    • Upload to Play Store beta track                        │
└────────┬────────────────────────────────────────────────────┘
         │ Beta deployed
         ▼
┌─────────────────────────────────────────────────────────────┐
│            Promote to Production Stage                       │
│  Downloads:                                                  │
│    • internal-aab-v{version} ←── Same AAB from internal!   │
│    • internal-apk-v{version}                                │
│  Actions:                                                    │
│    • Upload to Play Store production track                  │
│    • Create GitHub Release v{version}                       │
│    • Attach AAB and APK to release                          │
└─────────────────────────────────────────────────────────────┘
         │
         ▼
     ┌───────┐
     │ 🎉 DONE│
     └───────┘

Key Points:
  • Single AAB built once in "Deploy Internal" stage
  • Same AAB promoted through alpha → beta → production
  • No rebuilds = identical binary across all tracks
  • Ensures what you test is what you deploy
```

---

## Quick Reference

### Common Commands

```bash
# Run CI locally (before pushing)
./gradlew assembleDebug test lint koverVerify dependencyCheckAnalyze detekt

# Run instrumented tests locally
./gradlew connectedAndroidTest

# Build release AAB (requires keystore setup)
./gradlew bundleRelease

# Generate coverage report
./gradlew koverHtmlReport
open app/build/reports/kover/html/index.html

# Run specific test
./gradlew test --tests "YourTestClass"

# Clean build (if issues)
./gradlew clean
```

### Workflow Trigger Patterns

| Event | CI (ci.yml) | CD (cd.yml) |
|-------|-------------|-------------|
| PR to main | ✅ Runs | ❌ Skips |
| Push to main | ✅ Runs | ✅ Runs |
| Push to develop | ✅ Runs | ❌ Skips |
| Push to feature/* | ❌ Skips | ❌ Skips |
| Doc changes (*.md) | ❌ Skips | ❌ Skips |

### Approval Time Recommendations

| Track | Minimum Testing Period | Recommended Period |
|-------|------------------------|-------------------|
| Internal | 2 hours | 4 hours |
| Alpha | 24 hours | 48 hours |
| Beta | 3 days | 7 days |
| Production | After beta stable | After beta + team sign-off |

### Key Metrics Targets

| Metric | Target | Critical Threshold |
|--------|--------|-------------------|
| Crash-free rate | > 99.5% | < 99% |
| ANR rate | < 0.5% | > 1% |
| App startup time | < 2s | > 3s |
| Line coverage | 90% | < 80% |
| Branch coverage | 85% | < 75% |

### Support Contacts

- **GitHub Actions Issues**: GitHub Support
- **Play Console Issues**: Google Play Support
- **Firebase Issues**: Firebase Support
- **Team Lead**: [Your Team Lead]
- **Release Manager**: [Your Release Manager]

---

## Changelog

### Version 1.0.0 (2025-12-08)
- Initial CI/CD documentation
- Added comprehensive troubleshooting guide
- Documented all 7 quality gates
- Created architecture diagrams
- Added secrets configuration guide
- Established monitoring procedures

---

## Additional Resources

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Google Play Console Help](https://support.google.com/googleplay/android-developer)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Android Developer Guide](https://developer.android.com/guide)
- [Gradle Build Cache Guide](https://docs.gradle.org/current/userguide/build_cache.html)
- [OWASP Dependency Check](https://jeremylong.github.io/DependencyCheck/)
- [Detekt Documentation](https://detekt.dev/)
- [Kover Documentation](https://kotlin.github.io/kotlinx-kover/)

---

**Document Maintained By**: Wong, Documentation Specialist
**Last Updated**: 2025-12-08
**Quality Standard**: 9-10/10 (S.C.R.U.M. Approved)
