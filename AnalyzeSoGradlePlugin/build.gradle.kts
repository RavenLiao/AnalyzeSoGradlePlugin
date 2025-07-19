plugins {
    `kotlin-dsl`
    `version-catalog`
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation(libs.android.build)
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        register("analyze-so") {
            group = "io.github.ravenliao"
            id = "${group}.analyze-so"
            implementationClass = "io.github.ravenliao.plugin.AnalyzeSoGradlePlugin"
            version = "0.0.1"
        }
    }
}