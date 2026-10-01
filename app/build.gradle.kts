plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.snappvoiceassistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.snappbox.bikerapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = project.findProperty("RELEASE_STORE_FILE") as String?
            val storePasswordValue = project.findProperty("RELEASE_STORE_PASSWORD") as String?
            val keyAliasValue = project.findProperty("RELEASE_KEY_ALIAS") as String?
            val keyPasswordValue = project.findProperty("RELEASE_KEY_PASSWORD") as String?

            if (!storeFilePath.isNullOrBlank() &&
                !storePasswordValue.isNullOrBlank() &&
                !keyAliasValue.isNullOrBlank() &&
                !keyPasswordValue.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.12.0")
}
