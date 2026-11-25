plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // Regras da Stone: br.com.<empresa>.<app>.<livre>, sem citar Stone, concorrentes, fabricantes ou modelos
    namespace = "br.com.saborey.garcom.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.saborey.garcom.app"
        minSdk = 23            // Stone: Android 6.0 (API 23) ou superior
        targetSdk = 34
        versionCode = 1        // Stone: inteiro positivo, sobe a cada versão
        versionName = "1.0.0"  // Stone: só números e pontos, até 12 caracteres
        // Endereço do app do garçom (Stone exige HTTPS para qualquer site fora da Stone)
        buildConfigField("String", "URL_APP", "\"https://garcom.ecodados.app.br/garcom\"")
        // Esquema de retorno do Deeplink da Stone (tem que ser igual ao do AndroidManifest)
        buildConfigField("String", "ESQUEMA_RETORNO", "\"saboreygarcom\"")
        manifestPlaceholders["esquemaRetorno"] = "saboreygarcom"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
}
