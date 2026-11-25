plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    // Regras da Stone: br.com.<empresa>.<app>.<livre>, sem citar Stone, concorrentes, fabricantes ou modelos
    namespace = "br.com.saborey.garcom.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.saborey.garcom.app"
        minSdk = 23            // Stone: Android 6.0 (API 23) ou superior
        targetSdk = 34
        versionCode = 2        // Stone: inteiro positivo, sobe a cada versão
        versionName = "1.1.0"  // Stone: só números e pontos, até 12 caracteres
        // Servidor do sistema (Stone exige HTTPS para qualquer endereço fora da Stone)
        buildConfigField("String", "SERVIDOR", "\"https://garcom.ecodados.app.br\"")
        // Esquema de retorno do Deeplink da Stone (igual ao do AndroidManifest)
        buildConfigField("String", "ESQUEMA_RETORNO", "\"saboreygarcom\"")
        manifestPlaceholders["esquemaRetorno"] = "saboreygarcom"
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
