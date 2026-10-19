plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.alharamayn.mahamess.pro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.alharamayn.mahamess.pro"
        minSdk = 26
        targetSdk = 34
        versionCode = 20000
        versionName = "2.0.0"
        resourceConfigurations += listOf("ar", "en")
    }

    signingConfigs {
        create("release") {
            val ks = file("../../android/app/keystore/haramayn.jks")
            if (ks.exists()) {
                storeFile = ks
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "haramayn2026"
                keyAlias = System.getenv("KEY_ALIAS") ?: "haramayn"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "haramayn2026"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val ks = file("../../android/app/keystore/haramayn.jks")
            if (ks.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.webkit:webkit:1.11.0")
}
