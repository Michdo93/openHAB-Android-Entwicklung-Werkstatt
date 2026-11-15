plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "de.openhabbuch.ohapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.openhabbuch.ohapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.openhab.client)
    implementation(libs.openhab.testsuite)          // Kapitel 19: Vorab-Test
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.vm.compose)
    implementation(libs.lifecycle.rt.compose)
    implementation(libs.datastore)
    implementation(libs.glance)                      // Kapitel 20: Widget
    implementation(libs.glance.material3)
    debugImplementation(libs.compose.tooling)

    testImplementation(libs.junit4)                  // Kapitel 19: Unit-Tests
    testImplementation(libs.coroutines.test)
}
