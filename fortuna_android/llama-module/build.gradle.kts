plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// Step 1: Automatically install NDK if not present
tasks.register<Exec>("setupNDK") {
    description = "Automatically install Android NDK if not present"
    group = "build setup"

    val installScript = file("../scripts/install-ndk.sh")

    commandLine("bash", installScript.absolutePath)

    // Only run if script exists
    onlyIf { installScript.exists() }

    // Don't fail the build if setup fails (will fall back to CPU mode)
    isIgnoreExitValue = true

    doFirst {
        println("🔍 Checking Android NDK installation...")
    }
}

// Step 2: Setup OpenCL headers before building
tasks.register<Exec>("setupOpenCL") {
    description = "Install OpenCL headers for GPU acceleration"
    group = "build setup"

    val setupScript = file("../scripts/setup-opencl.sh")

    commandLine("bash", setupScript.absolutePath)

    // Only run if script exists
    onlyIf { setupScript.exists() }

    // Don't fail the build if setup fails
    isIgnoreExitValue = true

    // Run NDK setup first
    dependsOn("setupNDK")

    doFirst {
        println("🔧 Running OpenCL setup for GPU acceleration...")
    }
}

// Run setup before CMake configuration
tasks.matching { it.name.startsWith("externalNativeBuild") }.configureEach {
    dependsOn("setupOpenCL")
}

android {
    namespace = "android.llama.cpp"
    compileSdk = 36

    ndkVersion = "29.0.14206865"

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            // Add NDK properties if wanted, e.g.
            abiFilters += listOf("arm64-v8a")
        }
        externalNativeBuild {
            cmake {
                arguments += "-DLLAMA_CURL=OFF"
                arguments += "-DLLAMA_BUILD_COMMON=ON"
                arguments += "-DGGML_LLAMAFILE=OFF"
                arguments += "-DCMAKE_BUILD_TYPE=Release"
                // GPU acceleration with OpenCL (Adreno optimized)
                arguments += "-DGGML_OPENCL=ON"
                arguments += "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON"
                arguments += "-DGGML_OPENCL_EMBED_KERNELS=ON"

                // Set OpenCL library path for Android cross-compilation
                val openclLib = "${project.rootDir}/app/src/main/jniLibs/${android.defaultConfig.ndk.abiFilters.first()}/libOpenCL.so"
                arguments += "-DOpenCL_LIBRARY=${openclLib}"

                // Detect NDK path dynamically for OpenCL headers
                val ndkPath = System.getenv("ANDROID_NDK")
                    ?: System.getenv("ANDROID_HOME")?.let { "$it/ndk/29.0.14206865" }
                    ?: System.getenv("ANDROID_SDK_ROOT")?.let { "$it/ndk/29.0.14206865" }
                    ?: "${System.getProperty("user.home")}/Library/Android/sdk/ndk/29.0.14206865"

                // Detect host architecture for prebuilt path
                val hostArch = when {
                    System.getProperty("os.name").contains("Mac") && System.getProperty("os.arch").contains("aarch64") -> "darwin-aarch64"
                    System.getProperty("os.name").contains("Mac") -> "darwin-x86_64"
                    System.getProperty("os.name").contains("Linux") -> "linux-x86_64"
                    System.getProperty("os.name").contains("Windows") -> "windows-x86_64"
                    else -> "darwin-x86_64"
                }

                val openclInclude = "${ndkPath}/toolchains/llvm/prebuilt/${hostArch}/sysroot/usr/include"
                arguments += "-DOpenCL_INCLUDE_DIR=${openclInclude}"
                // 16KB page size support
                arguments += "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
                arguments += "-DCMAKE_EXE_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
                cppFlags += listOf()

                cppFlags("")
            }
        }
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
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
