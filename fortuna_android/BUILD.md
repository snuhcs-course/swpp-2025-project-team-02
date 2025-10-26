# Build Instructions

## Prerequisites

- **Android Studio** Arctic Fox or later
- **Android NDK** 26.3+ (automatically detected)
- **JDK** 17 or later
- **macOS** or **Linux** (Windows via WSL2)

## Quick Start

### 1. Clone and Open Project

```bash
git clone <repository-url>
cd fortuna_android
```

Open in Android Studio or build from command line:

```bash
./gradlew assembleDebug
```

### 2. GPU Acceleration Setup (Automatic)

The build system automatically sets up OpenCL for GPU acceleration.

**First build output:**
```
🔧 Running OpenCL setup for GPU acceleration...
📦 Using Android NDK: /Users/.../Android/sdk/ndk/27.0.12077973
✅ OpenCL headers installed successfully!
```

**No manual steps required!** The setup script runs automatically before native code compilation.

### 3. Build Variants

```bash
# Debug build (faster, includes logging)
./gradlew assembleDebug

# Release build (optimized, signed)
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

## GPU Acceleration

### Supported Hardware

- ✅ Qualcomm Snapdragon 8 Gen 3 (Adreno 750)
- ✅ Qualcomm Snapdragon 8 Gen 2 (Adreno 740)
- ✅ Qualcomm Snapdragon 8 Elite (Adreno 830)
- ⚠️ Other GPUs: Automatic CPU fallback

### Performance

| Backend | Speed | Relative |
|---------|-------|----------|
| GPU (Adreno) | ~20 tok/s | **4x faster** |
| CPU (ARM NEON) | ~5 tok/s | Baseline |

### Configuration

GPU acceleration is enabled by default in `llama-module/build.gradle.kts`:

```kotlin
// GPU acceleration with OpenCL (Adreno optimized)
arguments += "-DGGML_OPENCL=ON"
arguments += "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON"
arguments += "-DGGML_OPENCL_EMBED_KERNELS=ON"
```

See [GPU_ACCELERATION.md](GPU_ACCELERATION.md) for detailed information.

## Build Configuration

### NDK Detection

The build system automatically detects your NDK installation from:
1. `$ANDROID_NDK` environment variable
2. `$ANDROID_HOME/ndk` or `$ANDROID_SDK_ROOT/ndk`
3. `~/Library/Android/sdk/ndk` (macOS)
4. `~/Android/Sdk/ndk` (Linux)
5. `local.properties` file

### 16KB Page Alignment

All native libraries are built with 16KB page alignment for compatibility with Android 15+ devices:

```kotlin
arguments += "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
arguments += "-DCMAKE_EXE_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
```

### Architecture Support

Currently building for:
- `arm64-v8a` only (64-bit ARM)

To add more architectures, edit `llama-module/build.gradle.kts`:

```kotlin
ndk {
    abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
}
```

## Troubleshooting

### Build Fails with "NDK not found"

1. Install Android NDK via Android Studio:
   - Tools → SDK Manager → SDK Tools → NDK (Side by side)

2. Or set `ANDROID_NDK` manually:
   ```bash
   export ANDROID_NDK=~/Library/Android/sdk/ndk/27.0.12077973
   ./gradlew assembleDebug
   ```

### "OpenCL headers not found" during build

The setup script should run automatically. If it doesn't:

```bash
./scripts/setup-opencl.sh
./gradlew clean
./gradlew assembleDebug
```

### CMake configuration errors

Clean and rebuild:

```bash
./gradlew clean
./gradlew :llama-module:clean
rm -rf llama-module/.cxx
./gradlew assembleDebug
```

### "Library not aligned to 16KB" error

Ensure you have the latest code with 16KB alignment flags. Check `llama-module/build.gradle.kts`:

```kotlin
arguments += "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
```

## Clean Build

For a completely fresh build:

```bash
# Clean Gradle cache
./gradlew clean
./gradlew :llama-module:clean

# Remove CMake build artifacts
rm -rf llama-module/.cxx
rm -rf llama-module/build

# Rebuild
./gradlew assembleDebug
```

## Build Time

| Configuration | First Build | Incremental |
|--------------|-------------|-------------|
| Debug (with OpenCL) | ~3-5 min | ~30 sec |
| Release (optimized) | ~5-8 min | ~45 sec |

**Note**: First build downloads and compiles llama.cpp (~2.4GB source).

## Advanced Configuration

### Disable GPU Acceleration

To build without GPU support, edit `llama-module/build.gradle.kts`:

```kotlin
// Comment out OpenCL flags
// arguments += "-DGGML_OPENCL=ON"
// arguments += "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON"
// arguments += "-DGGML_OPENCL_EMBED_KERNELS=ON"
```

### Custom NDK Version

Specify NDK version in `llama-module/build.gradle.kts`:

```kotlin
android {
    ndkVersion = "27.0.12077973"
}
```

### Build Type Optimization

Configure build types in `app/build.gradle.kts`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles("proguard-rules.pro")
    }
}
```

## Project Structure

```
fortuna_android/
├── app/                          # Main Android app
│   └── src/main/jniLibs/        # Prebuilt native libraries
│       └── arm64-v8a/
│           └── libOpenCL.so     # OpenCL ICD Loader (73KB)
├── llama-module/                # llama.cpp Android wrapper
│   ├── build.gradle.kts         # Build configuration with GPU flags
│   └── src/main/cpp/
│       ├── CMakeLists.txt       # CMake build script
│       ├── llama-android.cpp    # JNI bindings
│       ├── llama-android-vlm.cpp # VLM support
│       └── mtmd/                # Multimodal support
├── llama.cpp/                   # llama.cpp submodule (2.4GB)
├── scripts/
│   └── setup-opencl.sh          # Auto-run OpenCL setup
├── BUILD.md                     # This file
└── GPU_ACCELERATION.md          # GPU acceleration details
```

## Development Workflow

1. **Make code changes** in `app/` or `llama-module/`
2. **Sync Gradle** (if build files changed)
3. **Build**: `./gradlew assembleDebug`
4. **Install**: `./gradlew installDebug`
5. **Test** on device or emulator

### Native Code Changes

If you modify C++ code (`llama-module/src/main/cpp/`):

```bash
# Force rebuild native libraries
./gradlew :llama-module:clean
./gradlew :llama-module:build
```

### Kotlin/Java Changes

Incremental builds work automatically:

```bash
./gradlew assembleDebug  # Fast incremental build
```

## CI/CD Integration

Example GitHub Actions workflow:

```yaml
- name: Setup NDK
  run: |
    echo "ndk.dir=$ANDROID_HOME/ndk/27.0.12077973" >> local.properties

- name: Build APK
  run: ./gradlew assembleDebug

- name: Verify GPU libraries
  run: unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libOpenCL
```

## Getting Help

- **Build issues**: Check [Troubleshooting](#troubleshooting) section
- **GPU issues**: See [GPU_ACCELERATION.md](GPU_ACCELERATION.md)
- **llama.cpp issues**: Check [llama.cpp/docs/android.md](llama.cpp/docs/android.md)
