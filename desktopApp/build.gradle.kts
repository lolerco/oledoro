plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose") version "1.7.3"
}

compose.desktop {
    application {
        mainClass = "com.lolerco.oledoro.desktop.MainKt"
    }
}

dependencies {
    implementation(project(":common"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.compose.ui:ui:1.7.3")
    implementation("org.jetbrains.compose.foundation:foundation:1.7.3")
    implementation("org.jetbrains.compose.material3:material3:1.7.3")
    implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.0.21")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}