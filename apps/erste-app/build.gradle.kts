import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Adresse und Token aus apps/local.properties (steht in .gitignore):
//   openhab.url=http://192.168.0.10:8080
//   openhab.token=oh.app.xxxxxxxx
val lokal = Properties().apply {
    val datei = rootProject.file("local.properties")
    if (datei.exists()) datei.inputStream().use { load(it) }
}

android {
    namespace = "de.openhabbuch.ersteapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.openhabbuch.ersteapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // landen als Konstanten in der Klasse BuildConfig
        // (10.0.2.2 ist im Emulator der Rechner, auf dem er läuft)
        val url = lokal.getProperty("openhab.url", "http://10.0.2.2:8080")
        val token = lokal.getProperty("openhab.token", "")
        buildConfigField("String", "OPENHAB_URL", "\"$url\"")
        buildConfigField("String", "OPENHAB_TOKEN", "\"$token\"")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.openhab.client)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)
    implementation(libs.activity.compose)
    debugImplementation(libs.compose.tooling)
}
