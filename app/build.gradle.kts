plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.routineapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.routineapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // firma debug por defecto
        }
    }

    // ✅ Evita el error de “Inconsistent JVM-target compatibility”
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        // ✅ Compatible con BOM 2024.05.00 (Compose 1.6.7)
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ✅ BOM de Compose (controla versiones del ecosistema Compose)
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))

    // Core Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    // ✅ Íconos extendidos (evita los “Unresolved reference” de DarkMode, Today, Bolt, Save, etc.)
    implementation("androidx.compose.material:material-icons-extended")

    // (Opcional) Material clásico si tienes layouts XML o estilos heredados
    implementation("com.google.android.material:material:1.12.0")

    // Debug / tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
