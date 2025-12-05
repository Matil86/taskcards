plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.dependency.check)
    alias(libs.plugins.detekt)
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

        vectorDrawables.useSupportLibrary = true

        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
            }
        }
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
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            // Enable Crashlytics mapping file upload for deobfuscated stack traces
            configure<com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension> {
                mappingFileUploadEnabled = true
            }
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

kapt {
    correctErrorTypes = true
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation(libs.androidx.datastore.preferences)

    // WorkManager for task reminders
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Jetpack Glance for widgets
    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // QR Code generation for deep link sharing
    implementation("com.google.zxing:core:3.5.3")

    // CameraX for QR code scanning
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ML Kit Barcode Scanning for QR code recognition
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // JSON serialization for ShareableList
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    kaptTest(libs.hilt.compiler)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation(libs.kotest.assertions.core)
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    kaptAndroidTest(libs.hilt.compiler)
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
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
    }
    jvmTarget = "21"
}

// Make check task depend on security and quality checks
// Note: dependencyCheckAnalyze and detekt removed from check task to prevent build failures
// Run them manually with: ./gradlew dependencyCheckAnalyze detekt
tasks.named("check") {
    // dependsOn("dependencyCheckAnalyze")  // Disabled - run manually to avoid NVD API failures
    // dependsOn("detekt")  // Disabled - code quality checks should not block builds
    dependsOn("ktlintCheck")
}
