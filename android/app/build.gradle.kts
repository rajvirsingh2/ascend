plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
}

android {
    namespace = "com.ascend.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ascend.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BASE_URL", "\"http://13.48.43.172/api/v1/\"")
        buildConfigField("String", "HMAC_SECRET", "\"${System.getenv("HMAC_SECRET")?: "dev_hmac_secret_32bytes_minimum"}\"")
    }

    signingConfigs{
        create("release"){
            storeFile=file(System.getenv("KEYSTORE_PATH")?:"keystore/debug.jks")
            storePassword=System.getenv("KEYSTORE_PASSWORD")?:"android"
            keyAlias=System.getenv("KEY_ALIAS")?:"androiddebugkey"
            keyPassword=System.getenv("KEY_PASSWORD")?:"android"
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            buildConfigField("String", "BASE_URL",
                "\"http://10.0.2.2:8080/api/v1/\"")
        }
        create("ngrok"){
            initWith(getByName("debug"))
            // Using your host PC's Wi-Fi LAN IP to bypass ngrok timeouts
            buildConfigField("String","BASE_URL",
                "\"http://10.125.9.14:8080/api/v1/\"")
        }
        create("ec2"){
            initWith(getByName("release"))
            buildConfigField("String", "BASE_URL", "\"http://13.48.43.172/api/v1/\"")
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources=true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL",
                "\"https://ascend-backend-production-5fad.up.railway.app/api/v1/\"")
            signingConfig=signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        // Opt in to the future behaviour where an un-targeted annotation (e.g.
        // Moshi's @Json on a data-class constructor param) applies to both the
        // parameter and the generated property. Silences the KT-73255 warnings.
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

dependencies {
    implementation(libs.androidx.animation.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.hilt.common)
    implementation(libs.androidx.ui.text)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)
    implementation(libs.androidx.ui.unit)
    implementation(libs.androidx.runtime)
    implementation(libs.firebase.inappmessaging.display)
    implementation(libs.firebase.messaging)
    implementation(libs.compose.ui.tooling)
    implementation(libs.foundation)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.lottie.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler.ext)

    implementation(libs.work.runtime.ktx)

    implementation(libs.navigation.compose)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

    implementation(libs.moshi.kotlin)
    ksp(libs.moshi.codegen)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    implementation(libs.coroutines.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    implementation(libs.coil.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}