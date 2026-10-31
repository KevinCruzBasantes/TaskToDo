import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
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
        versionCode = 4
        versionName = "3.0"

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
            enableUnitTestCoverage = true
            // Se instala al lado de la app real, con sus propios datos
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "TaskToDo (dev)"
        }
        release {
            // Sin la clave, el APK sale sin firmar y Android no lo instala encima de la app real
            signingConfig = signingConfigs.findByName("release")
            // R8 desactivado: al reubicar clases internas de Kotlin rompía la app solo en release
            // (IllegalAccessError al abrir). Así release ejecuta el mismo código que pasa las pruebas.
            // Ver docs/03-arquitectura.md §5.3.
            optimization {
                enable = false
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

room {
    // Cada versión del esquema queda en el repositorio para probar las migraciones (RNF-04)
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.documentfile)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.work.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
