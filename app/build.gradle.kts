// app/build.gradle.kts

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // 💡 SOLUCIÓN 1: Habilita el procesador de anotaciones para Moshi
    id("org.jetbrains.kotlin.kapt")

}

android {
    namespace = "com.example.proyecto"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.proyecto"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    // Java 17 + desugaring para java.time en API bajas
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    // Emparejado con Kotlin 1.9.24
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Definición de versiones para mayor claridad y consistencia
    val retrofit_version = "2.11.0"
    val moshi_version = "1.15.0"
    val okhttp_version = "4.12.0"
    val lifecycle_compose_version = "2.8.4" // Usado para ViewModel y Compose

    // --- Core / lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // --- Compose BOM (mantiene versiones alineadas) ---
    implementation(platform(libs.androidx.compose.bom))

    // --- Compose UI / Material3 ---
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.foundation:foundation") // PaddingValues, LazyColumn, etc.

    // Iconos (para Icons.Filled.Refresh)
    implementation("androidx.compose.material:material-icons-extended")

    // ViewModel + Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:$lifecycle_compose_version")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:$lifecycle_compose_version") // Corregida línea de versión

    // -----------------------------------------------------
    // NETWORKING (Retrofit, Moshi, OkHttp)
    // -----------------------------------------------------

    // Retrofit Base
    implementation("com.squareup.retrofit2:retrofit:$retrofit_version")

    // 💡 SOLUCIÓN 2: Implementación de Moshi Kotlin
    implementation("com.squareup.moshi:moshi-kotlin:$moshi_version")

    // Moshi Converter para Retrofit
    implementation("com.squareup.retrofit2:converter-moshi:$retrofit_version")

    // OkHttp y Logging Interceptor (se usa en debug e implementación)
    implementation("com.squareup.okhttp3:okhttp:$okhttp_version")
    implementation("com.squareup.okhttp3:logging-interceptor:$okhttp_version")

    // 💡 SOLUCIÓN 3: Generador de código (kapt) para Moshi. ¡ESTO RESUELVE EL ERROR!
    kapt("com.squareup.moshi:moshi-kotlin-codegen:$moshi_version")

    // -----------------------------------------------------

    // Imágenes
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Media (opcional, Media3)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")


    // --- Desugaring (java.time en minSdk<26) ---
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // --- TESTS ---
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation(platform(libs.androidx.compose.bom))
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("com.squareup.okhttp3:logging-interceptor:$okhttp_version")
}