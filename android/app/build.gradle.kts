plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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

        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/api/v1/\"")
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
            buildConfigField("String","BASE_URL",
                "\"https://delay-demote-pulse.ngrok-free.dev/api/v1\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources=true
            signingConfig=signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL",
                "\"https://ascend-backend-production-5fad.up.railway.app/api/v1\"")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.ui.unit)
    implementation(libs.androidx.runtime)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    implementation(libs.lottie.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

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
}