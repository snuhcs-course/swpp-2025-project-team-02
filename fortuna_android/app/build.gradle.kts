import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.fortuna_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fortuna_android"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load local.properties
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        // Build config fields
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"${localProperties.getProperty("GOOGLE_CLIENT_ID", "")}\"")
        buildConfigField("String", "API_BASE_URL", "\"${localProperties.getProperty("API_BASE_URL", "")}\"")
        buildConfigField("String", "API_HOST", "\"${localProperties.getProperty("API_HOST", "")}\"")
    }

    buildFeatures {
        buildConfig = true
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true // Enable ViewBinding
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE*"
            excludes += "/META-INF/license*"
            excludes += "/META-INF/NOTICE*"
            excludes += "/META-INF/notice*"
            excludes += "/META-INF/ASL2.0"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/*.version"
            excludes += "/META-INF/*.versions"
            excludes += "/META-INF/*.properties"
            excludes += "/META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.appdistribution.gradle)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.google.ar.core)
    implementation(libs.play.services.auth)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Dependency for CameraX API
    implementation(libs.bundles.camerax)
    // Dependency for fragment navigation
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    // Dependency for ExifInterface
    implementation(libs.androidx.exifinterface)
    // Dependency for Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
    // Dependency for image processing with Glide
    implementation(libs.glide)
    // Dependency for Guava (required for CameraX ListenableFuture)
    implementation(libs.guava)

}