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
    // SQLDelight does not publish a plugin-portal marker artifact under the
    // `app.cash.sqldelight` id; the real plugin coordinates are
    // `app.cash.sqldelight:gradle-plugin` on Maven Central. Without this
    // mapping Gradle searches for `app.cash.sqldelight.gradle.plugin` and
    // fails. See https://cashapp.github.io/sqldelight/2.0.2/android_sqlite/
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "app.cash.sqldelight") {
                useModule("app.cash.sqldelight:gradle-plugin:${requested.version}")
            }
        }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ChatterinoMobile"
include(":app")
 