import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.google.services)
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
            // Exclude unnecessary files
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
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
    implementation(libs.play.services.auth)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("org.mockito:mockito-core:5.7.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.6")
    androidTestImplementation("androidx.fragment:fragment-testing:1.6.2")
    androidTestImplementation("org.mockito:mockito-android:5.7.0")
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Dependency for Google ARCore
    implementation(libs.google.ar.core)
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
    // Dependency for firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)
    // Dependency for WorkManager (scheduled notifications)
    implementation(libs.androidx.work.runtime.ktx)
    // Dependency for custom toast
    implementation("androidx.cardview:cardview:1.0.0")
    // Dependency for Google Cloud Vision
    implementation("com.google.cloud:google-cloud-vision:3.72.0")
    implementation("io.grpc:grpc-okhttp:1.36.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.9.4")
    // MLKit
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:object-detection-custom:17.0.2")
    implementation("de.javagl:obj:0.4.0")
    // Dependency for SceneView AndroidX (3D model rendering)
    implementation("io.github.sceneview:arsceneview:2.3.0")
    implementation("io.github.sceneview:sceneview:2.3.0")
    // Dependency for MPAndroidChart (pie chart)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}