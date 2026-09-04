import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.gradle.util.GradleVersion

plugins {
    `kotlin-dsl`
    alias(libs.plugins.serialization.plugin)
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
}

// The publishing plugin is build infrastructure only. Its current release
// requires Gradle 8.5+, while the runtime plugin remains useful on older
// Gradle versions. Keep it out of old-version composite compatibility builds.
if (GradleVersion.current() >= GradleVersion.version("8.5")) {
    apply(plugin = "com.vanniktech.maven.publish")
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
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.serialization.json)
}

gradlePlugin {
    plugins {
        register("analyze-so") {
            group = "io.github.ravenliao"
            id = "${group}.analyze-so"
            implementationClass = "io.github.ravenliao.plugin.AnalyzeSoGradlePlugin"
            version = "0.0.9"
        }
    }
}
