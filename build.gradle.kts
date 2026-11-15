plugins {
    kotlin("jvm") version "2.0.21"
}

dependencies {
    implementation("com.github.Michdo93:android-openhab-rest-client:2.0.0")
    implementation("com.github.Michdo93:android-openhab-test-suite:2.0.0")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    jvmToolchain(21)
}

// Die Beispiele liegen in den Kapitelordnern, nicht unter src/main/kotlin
val kapitel = rootDir.listFiles()!!
    .filter { it.isDirectory && it.name.startsWith("kapitel") }
sourceSets {
    main { kotlin.srcDirs(kapitel.filter { it.name != "kapitel19_tests" }) }
    test { kotlin.srcDirs("kapitel19_tests") }
}

// ./gradlew bereit: übersetzt alles und legt die Bibliotheken nach build/lib
// (für starte.sh)
tasks.register<Copy>("bereit") {
    dependsOn("classes")
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("lib"))
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}
