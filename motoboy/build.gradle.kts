plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// App do motoboy Saborey: abre o app do motoboy do sistema (/motoboy) e, enquanto ele tem
// entrega em rota, manda o GPS em segundo plano (tela apagada, Google Maps aberto etc.).
android {
    namespace = "br.com.saborey.motoboy"
    compileSdk = 34

    defaultConfig {
        applicationId = "br.com.saborey.motoboy"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
        buildConfigField("String", "SERVIDOR", "\"https://garcom.ecodados.app.br\"")
    }
    // Chave de TESTE fixa (senha padrão "android"): o APK de cada versão nova instala por cima da anterior.
    signingConfigs {
        getByName("debug") {
            storeFile = file("teste.keystore")
            storePassword = "android"; keyAlias = "androiddebugkey"; keyPassword = "android"
        }
    }
    buildTypes { release { isMinifyEnabled = false } }
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
