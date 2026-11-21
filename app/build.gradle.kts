plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.penny.wallet"
    compileSdk {
        version = release(37) { minorApiLevel = 1 }
    }

    defaultConfig {
        applicationId = "com.penny.wallet"
        minSdk = 26
        // Real phones are ARM; dropping x86 (emulator-only) ML Kit native libs roughly halves the APK.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
        targetSdk = 35
        versionCode = 6
        versionName = "1.5"
    }

    buildTypes {
        release {
            // R8 optimisation + resource shrinking: the main speed win over the debug build.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Personal sideloaded app: sign with the local debug key so the release APK installs over the
            // debug one (same signature) and keeps all data.
            signingConfig = signingConfigs.getByName("debug")
        }
        // Release build (same R8 shrinking) plus the OCR probe, installed as a separate app so on-phone
        // receipt tests never touch the real app's data. Used by scripts/ocr-smoke.sh.
        create("smoke") {
            initWith(getByName("release"))
            applicationIdSuffix = ".smoke"
            versionNameSuffix = "-smoke"
            matchingFallbacks += listOf("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Distinct APK name (penny-wallet-debug.apk) so it can't be confused with other projects' app-debug.apk.
base {
    archivesName.set("penny-wallet")
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.glance.appwidget)
    implementation(libs.glance.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.documentfile)
    implementation(libs.reorderable)
    // Installs the Compose/AndroidX baseline profiles at install time, even for sideloaded APKs.
    implementation(libs.androidx.profileinstaller)
    // On-device receipt OCR, Latin model bundled in the APK: works offline, no Play Services download.
    implementation(libs.mlkit.text.recognition)

    testImplementation(libs.junit)
}
