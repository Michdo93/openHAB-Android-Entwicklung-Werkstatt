// Android-Studio-Projekt mit allen Apps des Buchs: File -> Open -> dieser Ordner
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
        maven("https://jitpack.io")                // android-openhab-rest-client
    }
}

rootProject.name = "openhab-android-apps"
include(":erste-app")
include(":ohapp")
include(":geraet")
include(":wear")
