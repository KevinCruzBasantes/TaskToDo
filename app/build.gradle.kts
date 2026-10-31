import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// La firma vive fuera del proyecto: local.properties dice dónde está su archivo de propiedades.
// Ver docs/03-arquitectura.md §5.3.
val firma = Properties().apply {
    val local = rootProject.file("local.properties")
    if (local.exists()) {
        val ruta = Properties().apply { local.inputStream().use { load(it) } }
            .getProperty("keystoreProperties")
        val archivo = ruta?.let { file(it) }
        if (archivo != null && archivo.exists()) archivo.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.calendar.tasktodo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.calendar.tasktodo"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["appLabel"] = "TaskToDo"
    }

    signingConfigs {
        if (!firma.isEmpty) {
            create("release") {
                storeFile = file(firma.getProperty("storeFile"))
                storePassword = firma.getProperty("storePassword")
                keyAlias = firma.getProperty("keyAlias")
                keyPassword = firma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Se instala al lado de la app real, con sus propios datos
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "TaskToDo (dev)"
        }
        release {
            // Sin la clave, el APK sale sin firmar y Android no lo instala encima de la app real
            signingConfig = signingConfigs.findByName("release")
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}