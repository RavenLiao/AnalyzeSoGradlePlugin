plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("io.github.ravenliao.analyze-so")
}

android {
    namespace = "io.github.ravenliao.librarydemo"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    flavorDimensions.add("flavors")
    productFlavors {
        create("Aflavor")
        create("Bflavor")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.gsyvideoplayer)
    implementation(libs.onepass)
}