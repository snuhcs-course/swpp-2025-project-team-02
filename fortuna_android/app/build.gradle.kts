import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.firebase.appdistribution)
    alias(libs.plugins.google.services)
    jacoco
}

// ========================================
// VLM Model Configuration (Single Source of Truth)
// ========================================
val VLM_MODEL_FILENAME = "InternVL3-1B-Instruct-Q8_0.gguf"
val VLM_MMPROJ_FILENAME = "mmproj-InternVL3-1B-Instruct-f16.gguf"
val VLM_HUGGINGFACE_REPO = "ggml-org/InternVL3-1B-Instruct-GGUF"

android {
    namespace = "com.example.fortuna_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fortuna_android"
        minSdk = 28  // Updated from 24 to 28 for llama.cpp compatibility
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

        // VLM model filenames (from top-level constants)
        buildConfigField("String", "VLM_MODEL_FILENAME", "\"$VLM_MODEL_FILENAME\"")
        buildConfigField("String", "VLM_MMPROJ_FILENAME", "\"$VLM_MMPROJ_FILENAME\"")
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.configure<JacocoTaskExtension> {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
                // Add JVM args to fix Robolectric bytecode verification issues
                it.jvmArgs("-noverify", "-XX:+IgnoreUnrecognizedVMOptions", "--add-opens=java.base/java.lang=ALL-UNNAMED")
            }
        }
    }
    packaging {
        resources {
            // Exclude unnecessary files
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/io.netty.versions.properties"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    implementation(project(":llama-module"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.play.services.auth)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
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
}

// JaCoCo Configuration
tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*",
        "**/databinding/**",
        "**/generated/**"
    )

    val debugTree = fileTree("${buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(buildDir) {
        include("**/*.exec", "**/*.ec")
    })
}

// ========================================
// VLM Model Download Task
// ========================================
// Downloads InternVL3-1B-Instruct GGUF models from HuggingFace at build time
// This keeps large model files out of git repository

tasks.register("downloadVLMModels") {
    description = "Download $VLM_HUGGINGFACE_REPO models from HuggingFace"
    group = "build"

    val modelsDir = file("src/main/assets/models")
    val modelFile = File(modelsDir, VLM_MODEL_FILENAME)
    val mmprojFile = File(modelsDir, VLM_MMPROJ_FILENAME)

    // HuggingFace repository URL
    val baseUrl = "https://huggingface.co/$VLM_HUGGINGFACE_REPO/resolve/main"

    inputs.property("modelUrl", "$baseUrl/$VLM_MODEL_FILENAME")
    inputs.property("mmprojUrl", "$baseUrl/$VLM_MMPROJ_FILENAME")
    outputs.files(modelFile, mmprojFile)

    doLast {
        modelsDir.mkdirs()

        // Download main model if not exists
        if (!modelFile.exists()) {
            println("📥 Downloading VLM model: $VLM_MODEL_FILENAME (~675MB)...")
            println("   This may take several minutes...")
            exec {
                commandLine(
                    "curl", "-L", "-o", modelFile.absolutePath,
                    "--progress-bar",
                    "$baseUrl/$VLM_MODEL_FILENAME"
                )
            }
            println("✅ Model downloaded: ${modelFile.name}")
        } else {
            println("✓ Model already exists: ${modelFile.name}")
        }

        // Download mmproj (vision encoder) if not exists
        if (!mmprojFile.exists()) {
            println("📥 Downloading vision encoder: $VLM_MMPROJ_FILENAME (~591MB)...")
            exec {
                commandLine(
                    "curl", "-L", "-o", mmprojFile.absolutePath,
                    "--progress-bar",
                    "$baseUrl/$VLM_MMPROJ_FILENAME"
                )
            }
            println("✅ Vision encoder downloaded: ${mmprojFile.name}")
        } else {
            println("✓ Vision encoder already exists: ${mmprojFile.name}")
        }

        println("🎉 All VLM models ready at: ${modelsDir.absolutePath}")
    }
}

// Auto-download models before building
tasks.named("preBuild") {
    dependsOn("downloadVLMModels")
}