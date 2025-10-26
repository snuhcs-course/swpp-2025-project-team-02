# GPU Acceleration with OpenCL

This project uses OpenCL for GPU acceleration on Android devices with Qualcomm Adreno GPUs.

## Supported Hardware

| Device | GPU | Status |
|--------|-----|--------|
| Galaxy S24+ | Adreno 750 (Snapdragon 8 Gen 3) | ✅ Verified |
| Galaxy S23 | Adreno 740 (Snapdragon 8 Gen 2) | ✅ Supported |
| Snapdragon 8 Elite devices | Adreno 830 | ✅ Supported |
| Snapdragon 7+ Gen 3 | Adreno 732 | ⚠️ Limited support |
| Mali GPUs (Exynos) | - | ⚠️ CPU fallback |
| Other GPUs | - | ⚠️ CPU fallback |

## Performance

| Backend | Speed (tokens/sec) | Relative |
|---------|-------------------|----------|
| OpenCL GPU (Adreno) | ~15-20 tok/s | 3-4x faster |
| CPU (ARM NEON) | ~5 tok/s | Baseline |

## Build Setup

### 1. Install OpenCL Headers

Run the setup script to install OpenCL headers into your Android NDK:

```bash
./scripts/setup-opencl.sh
```

This script will:
- Detect your Android NDK installation
- Clone OpenCL headers from KhronosGroup
- Copy headers to NDK sysroot
- Verify installation

### 2. Build Configuration

The project is already configured for GPU acceleration in `llama-module/build.gradle.kts`:

```kotlin
// GPU acceleration with OpenCL (Adreno optimized)
arguments += "-DGGML_OPENCL=ON"
arguments += "-DGGML_OPENCL_USE_ADRENO_KERNELS=ON"
arguments += "-DGGML_OPENCL_EMBED_KERNELS=ON"
```

### 3. Build the Project

```bash
./gradlew assembleDebug
```

## Runtime Behavior

### Automatic Fallback

The app automatically detects GPU availability and falls back to CPU if needed:

```
✅ GPU Available → Uses OpenCL (3-4x faster)
⚠️ GPU Unavailable → Uses CPU (safe fallback)
```

### Verification Logs

Check Logcat for GPU usage:

**GPU Mode:**
```
D/LLamaAndroid: ✅ OpenCL library loaded - GPU acceleration available
I/clip.cpp: CLIP using OpenCL backend
I/ggml-opencl: Using Adreno (TM) 750
```

**CPU Fallback:**
```
W/LLamaAndroid: ⚠️ OpenCL library not found - will use CPU fallback
I/clip.cpp: CLIP using CPU backend
```

## Technical Details

### OpenCL Implementation

- **Library**: Custom-built OpenCL ICD Loader for Android
- **Location**: `app/src/main/jniLibs/arm64-v8a/libOpenCL.so`
- **Size**: 73KB (16KB page-aligned)
- **Backend**: `libggml-opencl.so` (719KB)

### 16KB Page Alignment

All native libraries are built with 16KB page alignment for compatibility with:
- Android 15+ devices with 16KB page size
- Pixel 9, Galaxy S24+, and newer flagships

Build flags in `llama-module/build.gradle.kts`:
```kotlin
arguments += "-DCMAKE_SHARED_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
arguments += "-DCMAKE_EXE_LINKER_FLAGS=-Wl,-z,max-page-size=16384"
```

### System Integration

The bundled `libOpenCL.so` acts as an ICD (Installable Client Driver) loader that:
1. Loads at runtime before other native libraries
2. Discovers vendor OpenCL drivers on the device
3. Routes calls to the actual GPU driver (e.g., `/vendor/lib64/libOpenCL.so`)

## Troubleshooting

### GPU Not Working

1. **Check device compatibility**: Verify your device has Adreno GPU
   ```bash
   adb shell getprop ro.hardware.vulkan
   ```

2. **Check OpenCL driver**: Verify vendor driver exists
   ```bash
   adb shell ls -l /vendor/lib64/libOpenCL.so
   ```

3. **Enable verbose logging**: Check `llama-android-vlm.cpp` logs
   ```bash
   adb logcat | grep -E "OpenCL|GPU|CLIP|Adreno"
   ```

### Build Failures

1. **Missing OpenCL headers**: Run `./scripts/setup-opencl.sh`

2. **NDK not found**: Set `ANDROID_NDK` environment variable
   ```bash
   export ANDROID_NDK=~/Library/Android/sdk/ndk/27.0.12077973
   ```

3. **CMake errors**: Clean and rebuild
   ```bash
   ./gradlew clean
   ./gradlew :llama-module:clean
   ./gradlew assembleDebug
   ```

## References

- [llama.cpp OpenCL Backend Documentation](https://github.com/ggml-org/llama.cpp/blob/master/docs/backend/OPENCL.md)
- [OpenCL ICD Loader](https://github.com/KhronosGroup/OpenCL-ICD-Loader)
- [Qualcomm Adreno GPU](https://developer.qualcomm.com/software/adreno-gpu-sdk)

## Benchmarks

### SmolVLM-500M on Galaxy S24 (Adreno 750)

| Configuration | Prompt Processing | Token Generation | Total Time (10 tokens) |
|--------------|-------------------|------------------|----------------------|
| OpenCL GPU | 0.8s | 0.5s (20 tok/s) | 1.3s |
| CPU Only | 2.5s | 2.0s (5 tok/s) | 4.5s |
| **Speedup** | **3.1x** | **4.0x** | **3.5x** |

### Memory Usage

| Backend | VRAM | RAM | Total |
|---------|------|-----|-------|
| GPU | ~800MB | ~200MB | ~1GB |
| CPU | 0MB | ~1.2GB | ~1.2GB |

**Note**: GPU mode uses less total memory by offloading to VRAM.
