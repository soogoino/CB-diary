import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    // id("com.google.gms.google-services")
}

android {
    namespace = "com.chastity.diary"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.chastity.diary"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            // Values are read from environment variables (CI) or local.properties (local dev).
            // Never commit keystore passwords to source control.
            val localProps = rootProject.file("local.properties")
            val props = if (localProps.exists()) {
                Properties().also { it.load(localProps.inputStream()) }
            } else null
            storeFile = file(
                System.getenv("KEYSTORE_PATH")
                    ?: props?.getProperty("KEYSTORE_PATH") ?: "../cb-diary-release.jks"
            )
            storePassword = System.getenv("KEYSTORE_PASS")
                ?: props?.getProperty("KEYSTORE_PASS") ?: ""
            keyAlias = System.getenv("KEY_ALIAS")
                ?: props?.getProperty("KEY_ALIAS") ?: "cb-diary"
            keyPassword = System.getenv("KEY_PASS")
                ?: props?.getProperty("KEY_PASS") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    // A-2: SplashScreen API — eliminates blank-window flash on cold start (Android 11-)
    implementation("androidx.core:core-splashscreen:1.0.1")
    
    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2023.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // ViewModel & Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    
    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    
    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Security (Encryption)
    // Note: 1.1.0-alpha06 is the latest available; stable 1.0.0 uses older MasterKeys API
    // which is incompatible with the current EncryptedSharedPreferences usage pattern.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Biometric — downgraded from alpha05 to latest stable
    implementation("androidx.biometric:biometric:1.1.0")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // ExifInterface (for reading photo orientation)
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    // Charts - Vico (Compose native)
    implementation("com.patrykandpatrick.vico:compose:1.13.1")
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")
    implementation("com.patrykandpatrick.vico:core:1.13.1")
    
    // Firebase — plugin (google-services) is commented out; dependencies removed until
    // cloud sync is implemented. Re-enable by:
    //   1. Uncomment id("com.google.gms.google-services") in the plugins block
    //   2. Replace google-services.json with a real project config
    //   3. Add Firebase deps:
    //      implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    //      implementation("com.google.firebase:firebase-auth-ktx")
    //      implementation("com.google.firebase:firebase-firestore-ktx")

    // Gson for JSON serialization
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
}
