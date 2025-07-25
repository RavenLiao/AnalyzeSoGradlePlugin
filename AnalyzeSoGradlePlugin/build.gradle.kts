import org.jetbrains.kotlin.gradle.dsl.kotlinExtension

plugins {
    `kotlin-dsl`
    alias(libs.plugins.serialization.plugin)
    alias(libs.plugins.mavenpublish)
}

// Configure Java toolchain to use Java 11 for broader compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

// Ensure the Gradle daemon uses the same Java version
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_11.toString()
    targetCompatibility = JavaVersion.VERSION_11.toString()
}

kotlinExtension.jvmToolchain(11)

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(libs.android.build)
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        register("analyze-so") {
            group = "io.github.ravenliao"
            id = "${group}.analyze-so"
            implementationClass = "io.github.ravenliao.plugin.AnalyzeSoGradlePlugin"
            version = "0.0.5"
        }
    }
}