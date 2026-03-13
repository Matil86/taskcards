pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
// foojay-resolver-convention removed: auto-provisioning is disabled
// (org.gradle.java.installations.auto-download=false in gradle.properties).
// The plugin was defaulting every vendor-less toolchain request to Azul Zulu,
// causing "cannot find JDK matching {vendor=Azul Zulu}" failures in CI where
// only Temurin 21 (setup-java) is available.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "TaskCards"
include(":app")
