import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10"
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.performance)
    alias(libs.plugins.dependency.check)
    alias(libs.plugins.detekt)
    id("org.jetbrains.kotlinx.kover") version "0.9.7"
}

android {
    namespace = "de.hipp.app.taskcards"
    compileSdk = 36

    defaultConfig {
        applicationId = "de.hipp.app.taskcards"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("RELEASE_KEYSTORE_PATH") ?: "../release.keystore")
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            // Disable monitoring in debug builds to avoid polluting production data
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = false
            }
            // Firebase Performance is automatically disabled in debug by the SDK
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            // Enable Firebase Crashlytics mapping file upload for deobfuscated stack traces
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }

            // Firebase Performance monitoring is automatically enabled in release builds
            // Configure performance thresholds via Firebase Console
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
        jvmToolchain(21)  // Android doesn't support Java 24 yet - AGP limitation
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        // Disable MissingTranslation check to allow partial translations
        disable += "MissingTranslation"
        // Don't abort build on lint errors (let CI decide)
        abortOnError = false
    }
    packaging {
        resources {
            excludes += listOf(
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/licenses/ASM",
                "win32-x86/attach_hotspot_windows.dll",
                "win32-x86-64/attach_hotspot_windows.dll"
            )
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.useJUnitPlatform()
            // Kotest timeouts
            it.systemProperty("kotest.framework.timeout", "300000") // 5 minute timeout per test
            it.systemProperty("kotest.framework.invocation.timeout", "60000") // 1 minute timeout per test case
            // Kotest parallel execution - run test classes in parallel
            it.systemProperty("kotest.framework.parallelism", "3") // Run up to 3 test classes concurrently
            // Gradle parallel test execution
            it.maxParallelForks = Runtime.getRuntime().availableProcessors() // Use all available CPU cores
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation(libs.androidx.datastore.preferences)

    // WorkManager for task reminders
    implementation("androidx.work:work-runtime-ktx:2.11.1")

    // Jetpack Glance for widgets
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Koin Dependency Injection
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.workmanager)

    // Room Database
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Firebase — Note: -ktx suffix removed in BOM 32.x+, KTX is now built into base artifacts
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-perf")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-auth")

    // Google Credential Manager (replaces deprecated Google Sign-In)
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    // QR Code generation for deep link sharing
    implementation("com.google.zxing:core:3.5.4")

    // CameraX for QR code scanning
    implementation("androidx.camera:camera-core:1.5.3")
    implementation("androidx.camera:camera-camera2:1.5.3")
    implementation("androidx.camera:camera-lifecycle:1.5.3")
    implementation("androidx.camera:camera-view:1.5.3")

    // ML Kit Barcode Scanning for QR code recognition
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // JSON serialization for ShareableList
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.02.01"))
    androidTestImplementation(libs.kotest.assertions.core)
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// OWASP Dependency Check Configuration
dependencyCheck {
    failBuildOnCVSS = 7.0f  // Fail on High/Critical vulnerabilities (CVSS >= 7.0)
    suppressionFile = file("owasp-suppressions.xml").toString()
    analyzers.assemblyEnabled = false
    analyzers.nugetconfEnabled = false
    analyzers.nodeEnabled = false
    formats = listOf("HTML", "JSON")

    // Don't fail the build on errors (e.g., NVD API failures)
    // This allows builds to continue when NVD service is unavailable (403 errors)
    failOnError = false

    // Configure NVD API key from environment (optional - reduces rate limiting)
    nvd.apiKey = System.getenv("NVD_API_KEY") ?: ""
}

// Detekt Static Analysis Configuration
detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/detekt-config.yml"))
    // Note: ReportingExtension.file() deprecation warning from Detekt 1.23.x is a known
    // upstream issue (github.com/detekt/detekt/issues/8452). Fix will ship in Detekt 2.0.0
    // stable. No stable release supports AGP 9 yet (as of 2026-03-10).
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
    jvmTarget = "21"
}

// Kover Code Coverage Configuration (v0.9.7 API)
kover {
    reports {
        filters {
            excludes {
                // Compose UI screens are not unit-testable — covered by instrumented tests only
                packages(
                    "de.hipp.app.taskcards.ui.screens",
                    "de.hipp.app.taskcards.ui.screens.*",
                    "de.hipp.app.taskcards.ui.navigation",
                    "de.hipp.app.taskcards.ui.preview",
                    "de.hipp.app.taskcards.ui.theme",
                    // Android-context-bound code (Workers, Widgets, Auth) requires instrumented tests
                    "de.hipp.app.taskcards.widget",
                    "de.hipp.app.taskcards.worker",
                    "de.hipp.app.taskcards.auth",
                )
                // Exclude generated, boilerplate and DI wiring classes
                annotatedBy("androidx.compose.runtime.Composable")
            }
        }

        // Thresholds apply to the testable layer only (ViewModels, Repos, Models, Utils).
        // DI wiring, deep-link handlers and other Android-context-bound code are not
        // unit-testable and are reflected in the lower baseline. Raise these incrementally
        // as test coverage grows.
        verify {
            rule("Minimum Line Coverage") {
                minBound(20)
            }
            rule("Minimum Branch Coverage") {
                minBound(15, CoverageUnit.BRANCH)
            }
        }

        // Generate XML report for CI/CD integration
        total {
            xml {
                onCheck = false  // Don't auto-generate on check (CI handles it)
            }
            html {
                onCheck = false
            }
        }
    }
}

// Make check task depend on security and quality checks
// FULL FIX PRINCIPLE: Security and quality checks MUST run in CI/CD to catch issues early
// NVD API failures are handled with failOnError=false in dependencyCheck configuration
tasks.named("check") {
    dependsOn("dependencyCheckAnalyze")  // Security vulnerability scanning
    dependsOn("detekt")  // Static code analysis
    dependsOn("ktlintCheck")  // Code style enforcement
    // Note: Coverage verification (koverVerify) runs separately in CI to allow incremental progress
}
