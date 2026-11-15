// Gradle-Projekt für die Kotlin-Programme der Kapitel 8 bis 16 und die Tests aus Kapitel 19.
// Die Android-Apps liegen im eigenen Projekt apps/ (in Android Studio öffnen).
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "openhab-android-beispiele"
