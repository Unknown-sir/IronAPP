import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val wireguardEmbedded =
    (project.findProperty("ironapp.wireguardEmbedded") as String? ?: "true").toBoolean()

android {
    namespace = "com.ironpanel.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ironpanel.app"
        // Installable on Android 5.0 (API 21) and newer — one universal APK
        // plus per-ABI APKs (see splits below) cover every device.
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables { useSupportLibrary = true }
    }

    // Per-core/per-ABI outputs: universal + armeabi-v7a + arm64-v8a + x86 + x86_64.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            isUniversalApk = true
        }
    }

    signingConfigs {
        // CI has no release key: sign with the debug key so assembleRelease
        // stays green. For Play/store releases, provide IRONAPP_KEYSTORE_* env
        // vars (see .github/workflows/android.yml) or a local keystore.properties.
        val keystoreProps = Properties()
        val keystoreFile = rootProject.file("keystore.properties")
        if (keystoreFile.exists()) keystoreFile.inputStream().use { keystoreProps.load(it) }
        create("release") {
            val storePath = System.getenv("IRONAPP_KEYSTORE_PATH")
                ?: keystoreProps.getProperty("storeFile")
            if (storePath != null) {
                storeFile = rootProject.file(storePath)
                storePassword = System.getenv("IRONAPP_KEYSTORE_PASSWORD")
                    ?: keystoreProps.getProperty("storePassword")
                keyAlias = System.getenv("IRONAPP_KEY_ALIAS")
                    ?: keystoreProps.getProperty("keyAlias")
                keyPassword = System.getenv("IRONAPP_KEY_PASSWORD")
                    ?: keystoreProps.getProperty("keyPassword")
            } else {
                // Fallback: debug key (dev/CI artifacts only, never for release).
                storeFile = File(System.getProperty("user.home") + "/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES", "META-INF/LICENSE", "META-INF/LICENSE.txt",
                "META-INF/NOTICE", "META-INF/NOTICE.txt", "META-INF/*.kotlin_module"
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.browser:browser:1.8.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")

    // QR scan of subscription links (works back to API 21).
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // Embedded WireGuard core (Maven Central, Apache-2.0).
    // Disable with -Pironapp.wireguardEmbedded=false to build a pure-handoff APK.
    if (wireguardEmbedded) {
        implementation("com.wireguard.android:tunnel:1.0.20230706")
    }

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
