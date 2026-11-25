plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

val robolectricJars: Configuration by configurations.creating
val robolectricDir = layout.buildDirectory.dir("robolectric-jars")
val prepareRobolectric = tasks.register<Sync>("prepareRobolectric") {
    from(robolectricJars)
    into(robolectricDir)
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
        targetSdk = 36
        versionCode = 7
        versionName = "2.0"
    }

    // The Play bundle is signed with the upload key only when asked (-Ppenny.signUpload=true), so local release
    // APKs keep the debug signature and install over the existing app without losing data.
    val signUpload = providers.gradleProperty("penny.signUpload").orNull == "true"
    signingConfigs {
        if (signUpload) create("upload") {
            fun prop(name: String) = providers.gradleProperty(name).orNull
                ?: error("$name missing in ~/.gradle/gradle.properties (needed for -Ppenny.signUpload=true)")
            storeFile = file(prop("PENNY_UPLOAD_STORE_FILE"))
            storePassword = prop("PENNY_UPLOAD_STORE_PASSWORD")
            keyAlias = prop("PENNY_UPLOAD_KEY_ALIAS")
            keyPassword = prop("PENNY_UPLOAD_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            // R8 optimisation + resource shrinking: the main speed win over the debug build.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName(if (signUpload) "upload" else "debug")
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.maxHeapSize = "1g"
                it.maxParallelForks = 2
                it.systemProperty("penny.writeFixtures", providers.gradleProperty("penny.writeFixtures").getOrElse("false"))
                // Robolectric would download its Android jar during the test run (flaky on CI); Gradle fetches and caches it instead.
                it.dependsOn(prepareRobolectric)
                it.systemProperty("robolectric.offline", "true")
                it.systemProperty("robolectric.dependency.dir", robolectricDir.get().asFile.absolutePath)
            }
        }
    }
    androidResources {
        generateLocaleConfig = true
        localeFilters += listOf("en", "de", "it")
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
    implementation(libs.play.billing)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    robolectricJars(libs.robolectric.android.all)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)
}
