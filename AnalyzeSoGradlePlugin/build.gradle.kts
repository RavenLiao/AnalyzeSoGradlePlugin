plugins {
    `kotlin-dsl`
    alias(libs.plugins.serialization.plugin)
    alias(libs.plugins.mavenpublish)
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
            version = "0.0.2"
        }
    }
}