pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ReplicaScan"

include(
    ":app",
    ":core-common",
    ":core-data",
    ":core-ui",
    ":feature-home",
    ":feature-camera",
    ":feature-editor",
    ":feature-export",
    ":feature-history",
    ":feature-settings",
    ":feature-ocr",
)
