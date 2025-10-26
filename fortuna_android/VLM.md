# SmolVLM + llama.cpp 안드로이드 통합 리서치

> 연구 날짜: 2025-10-23
> 목적: SmolVLM-500M-Instruct 모델을 llama.cpp 서버로 안드로이드 앱에 통합하는 방법 조사

## 📋 목차

1. [개요](#개요)
2. [SmolVLM 모델 정보](#smolvlm-모델-정보)
3. [안드로이드 실행 가능성](#안드로이드-실행-가능성)
4. [통합 방법](#통합-방법)
5. [레퍼런스 프로젝트](#레퍼런스-프로젝트)
6. [구현 가이드](#구현-가이드)
7. [성능 및 요구사항](#성능-및-요구사항)
8. [트러블슈팅](#트러블슈팅)

---

## 개요

### SmolVLM이란?

- **모델 크기**: 2B 파라미터 (500M 버전도 존재)
- **타입**: Vision Language Model (VLM)
- **특징**: 모바일 및 엣지 디바이스에 최적화된 경량 멀티모달 모델
- **라이선스**: Apache 2.0
- **모델 저장소**: [ggml-org/SmolVLM-500M-Instruct-GGUF](https://huggingface.co/ggml-org/SmolVLM-500M-Instruct-GGUF)

### 주요 장점

- **메모리 효율성**: VLM 중 가장 낮은 메모리 사용량
- **빠른 속도**:
  - Prefill 속도: 3-4.5배 빠름
  - Generation 속도: 최대 16배 빠름 (Qwen2-VL 대비)
- **효율적인 인코딩**: 384x384 이미지를 81 토큰으로 인코딩
- **실시간 처리**: 웹캠 실시간 객체 탐지 가능

---

## 안드로이드 실행 가능성

### ✅ 결론: 완전히 가능

llama.cpp는 안드로이드를 공식 지원하며, SmolVLM-500M은 모바일 환경에 매우 적합합니다.

### 지원 플랫폼

- **아키텍처**: arm64-v8a, x86_64
- **최소 Android API**: 28 (Android 9.0)
- **GPU 가속**: Qualcomm Adreno (OpenCL) - b5028 이전 버전

### 실제 사례

- SmolVLM2 안드로이드 앱 구현 사례 존재
- CPU-only 추론으로도 우수한 성능 보고
- 실시간 비전 작업 가능

---

## 통합 방법

### 방법 비교

| 방법 | 난이도 | 성능 | 유지보수 | 추천도 |
|------|--------|------|----------|--------|
| **Termux** | ⭐ | 중 | 낮음 | 프로토타입용 |
| **React Native (llama.rn)** | ⭐⭐ | 중상 | 중 | 크로스플랫폼 |
| **Kotlin + JNA** | ⭐⭐⭐ | 상 | 중상 | **기존 앱 통합 추천** |
| **Kotlin + JNI** | ⭐⭐⭐⭐ | 최상 | 높음 | 고성능 필요시 |

### 기존 Kotlin 앱 통합 시 권장: JNA 방식

**장점:**
- JNI보다 간단한 바인딩
- 네이티브 코드 작성 불필요
- 유지보수 용이

**단점:**
- JNI보다 약간 느림 (실용적으로는 무시 가능)

---

## 레퍼런스 프로젝트

### 1. kotlinllamacpp ⭐ 최우선 추천

- **URL**: https://github.com/ljcamargo/kotlinllamacpp
- **특징**:
  - Kotlin 네이티브 바인딩
  - 16kb pagination 지원 (최신 Android)
  - 쉬운 사용법
- **사용 예시**:
  ```kotlin
  implementation("io.github.ljcamargo:kotlinllamacpp:1.0.0")
  ```

### 2. llama-cpp-kt (JNA 기반)

- **URL**: https://github.com/hurui200320/llama-cpp-kt
- **특징**:
  - JNA 사용으로 간편한 통합
  - `-DBUILD_SHARED_LIBS=ON` 플래그로 빌드 필요

### 3. llama-jni (JNI 기반)

- **URL**: https://github.com/shixiangcap/llama-jni
- **특징**:
  - 전통적인 JNI 방식
  - 최고 성능

### 4. llama.cpp-android-tutorial

- **URL**: https://github.com/JackZeng0208/llama.cpp-android-tutorial
- **특징**:
  - GPU 가속 가이드
  - Adreno OpenCL 지원

### 5. llama.rn (React Native)

- **URL**: https://github.com/mybigday/llama.rn
- **특징**:
  - 크로스플랫폼
  - Vision 모델 지원
  - 설치 간편: `npm install llama.rn`

### 6. 실시간 웹캠 예제

- **URL**: https://github.com/ngxson/smolvlm-realtime-webcam
- **특징**:
  - SmolVLM 실시간 객체 탐지
  - HTML + llama-server 구조

---

## 구현 가이드

### 1단계: llama.cpp 빌드

```bash
# Android NDK 설정
export ANDROID_NDK=/path/to/android-ndk

# llama.cpp 클론 (b5028 이전 버전 권장)
git clone https://github.com/ggml-org/llama.cpp
cd llama.cpp
git checkout <commit-before-b5028>

# Android용 빌드 (arm64-v8a)
cmake -B build-android \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-28 \
  -DGGML_OPENMP=OFF \
  -DGGML_LLAMAFILE=OFF \
  -DBUILD_SHARED_LIBS=ON

# 빌드 실행
cmake --build build-android --config Release -j8

# 결과물: build-android/libllama.so
```

### 2단계: Android 프로젝트 구조

```
your-app/
├── app/
│   ├── src/main/
│   │   ├── jniLibs/
│   │   │   └── arm64-v8a/
│   │   │       └── libllama.so         # 여기에 복사
│   │   ├── assets/
│   │   │   └── models/
│   │   │       └── smolvlm-500m-q4.gguf
│   │   └── java/com/yourapp/
│   │       ├── llama/
│   │       │   ├── LlamaWrapper.kt
│   │       │   └── LlamaViewModel.kt
│   │       └── MainActivity.kt
│   └── build.gradle.kts
```

### 3단계: build.gradle.kts 설정

```kotlin
android {
    defaultConfig {
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    // 16kb pagination 지원 (Android 15+)
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // JNA 방식 사용시
    implementation("net.java.dev.jna:5.13.0@aar")

    // 또는 kotlinllamacpp 사용
    // implementation("io.github.ljcamargo:kotlinllamacpp:1.0.0")
}
```

### 4단계: Kotlin 바인딩 (JNA 예시)

```kotlin
// LlamaWrapper.kt
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface LlamaLib : Library {
    fun llama_model_load(path: String, params: Pointer): Pointer
    fun llama_context_new(model: Pointer, params: Pointer): Pointer
    fun llama_generate(context: Pointer, prompt: String): String
    fun llama_free(context: Pointer)

    companion object {
        val INSTANCE: LlamaLib by lazy {
            Native.load("llama", LlamaLib::class.java)
        }
    }
}

class LlamaManager(private val context: Context) {
    private var modelContext: Pointer? = null

    fun loadModel(modelPath: String) {
        val params = createDefaultParams()
        val model = LlamaLib.INSTANCE.llama_model_load(modelPath, params)
        modelContext = LlamaLib.INSTANCE.llama_context_new(model, params)
    }

    fun generate(prompt: String): String {
        return modelContext?.let {
            LlamaLib.INSTANCE.llama_generate(it, prompt)
        } ?: ""
    }

    fun cleanup() {
        modelContext?.let { LlamaLib.INSTANCE.llama_free(it) }
    }
}
```

### 5단계: Vision 모델 사용

```kotlin
// 이미지 로딩
fun loadImageAsBytes(uri: Uri): ByteArray {
    return context.contentResolver.openInputStream(uri)?.use {
        it.readBytes()
    } ?: byteArrayOf()
}

// SmolVLM 추론
fun analyzeImage(imageUri: Uri, prompt: String): String {
    val imageBytes = loadImageAsBytes(imageUri)
    return llamaManager.generateWithImage(
        prompt = prompt,
        imageData = imageBytes
    )
}
```

### 6단계: 모델 파일 관리

```kotlin
// ViewModel에서
class LlamaViewModel(application: Application) : AndroidViewModel(application) {
    private val modelPath = File(application.filesDir, "models/smolvlm-500m-q4.gguf")

    init {
        copyModelFromAssets()
    }

    private fun copyModelFromAssets() {
        if (!modelPath.exists()) {
            modelPath.parentFile?.mkdirs()
            application.assets.open("models/smolvlm-500m-q4.gguf").use { input ->
                FileOutputStream(modelPath).use { output ->
                    input.copyTo(output)
                }
            }
        }
    }
}
```

### 7단계: 권한 설정 (AndroidManifest.xml)

```xml
<manifest>
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />

    <application>
        <!-- extractNativeLibs for 16kb pagination -->
        android:extractNativeLibs="false"
        ...
    </application>
</manifest>
```

---

## 성능 및 요구사항

### 하드웨어 요구사항

| 항목 | 최소 | 권장 |
|------|------|------|
| **RAM** | 4GB | 6GB+ |
| **저장공간** | 2GB | 4GB+ |
| **프로세서** | 64-bit ARM | Snapdragon 8 Gen 1+ |
| **Android 버전** | 9.0 (API 28) | 12.0+ (API 31) |

### 모델 크기별 메모리 사용량

| 모델 | 크기 | RAM 사용량 | 추론 속도 |
|------|------|------------|-----------|
| SmolVLM-500M-Q4_K_M | ~300MB | ~1.5GB | 빠름 |
| SmolVLM-500M-Q5_K_M | ~400MB | ~2GB | 보통 |
| SmolVLM-500M-Q6_K | ~500MB | ~2.5GB | 느림 |

### 성능 최적화 팁

```kotlin
// Context 크기 조절
val params = LlamaParams().apply {
    n_ctx = 2048  // 4096은 메모리 부족 가능
    n_threads = 4 // CPU 코어 수에 맞게
    use_mlock = true
    use_mmap = true
}

// 배치 크기 조절
val batchSize = 512 // 기본값, 메모리 부족시 256으로
```

### 예상 성능

- **이미지 인코딩**: ~100-200ms (CPU)
- **텍스트 생성**: ~50-100 tokens/sec (CPU)
- **전체 추론**: ~1-2초 (간단한 질문)

---

## 트러블슈팅

### 1. 라이브러리 로드 실패

**에러:**
```
java.lang.UnsatisfiedLinkError: dlopen failed: library "libllama.so" not found
```

**해결:**
```bash
# adb로 확인
adb shell run-as com.yourapp ls -la /data/data/com.yourapp/lib/

# build.gradle에 추가
android {
    packagingOptions {
        jniLibs.useLegacyPackaging = true
    }
}
```

### 2. Segmentation Fault

**원인:** llama.cpp 최신 버전 (b5028 이후)

**해결:**
```bash
# b5028 이전 버전으로 체크아웃
cd llama.cpp
git checkout <commit-before-b5028>
```

### 3. Out of Memory

**해결:**
```kotlin
// Context 크기 줄이기
n_ctx = 2048  // 4096에서 줄임

// 더 작은 quantization 모델 사용
// Q6 -> Q5 -> Q4
```

### 4. 16kb Pagination 에러 (Android 15+)

**에러:**
```
CANNOT LINK EXECUTABLE: requires 16kb page size
```

**해결:**
```kotlin
// build.gradle
android {
    packagingOptions {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

// AndroidManifest.xml
<application android:extractNativeLibs="false">
```

### 5. GPU 가속 실패 (Adreno)

**해결:**
```bash
# OpenCL 지원 빌드
cmake -B build-android \
  -DGGML_OPENCL=ON \
  -DGGML_OPENCL_USE_ADRENO=ON \
  ...

# b5028 이전 버전 필수!
```

---

## 빠른 시작 체크리스트

- [ ] Android NDK 설치
- [ ] llama.cpp 빌드 (b5028 이전 버전)
- [ ] libllama.so를 jniLibs/arm64-v8a/에 복사
- [ ] build.gradle 설정 (ndk, packagingOptions)
- [ ] JNA 또는 kotlinllamacpp dependency 추가
- [ ] GGUF 모델 다운로드 (SmolVLM-500M-Q4 권장)
- [ ] assets/models/에 모델 배치
- [ ] 권한 설정 (CAMERA, STORAGE)
- [ ] Kotlin 래퍼 작성
- [ ] 테스트 실행

---

## 추가 리소스

### 공식 문서

- [llama.cpp GitHub](https://github.com/ggml-org/llama.cpp)
- [llama.cpp Android Docs](https://github.com/ggml-org/llama.cpp/blob/master/docs/android.md)
- [SmolVLM Hugging Face](https://huggingface.co/blog/smolvlm)

### 튜토리얼 & 블로그

- [How to compile LLM on Android using LLama.cpp (Medium)](https://medium.com/@mmonteirojs/how-to-compile-any-llm-on-android-using-llama-cpp-46885569768d)
- [Run Gemma and VLMs on mobile (Medium)](https://farmaker47.medium.com/run-gemma-and-vlms-on-mobile-with-llama-cpp-dbb6e1b19a93)
- [LLM Inference on Edge (Hugging Face)](https://huggingface.co/blog/llm-inference-on-edge)

### 커뮤니티

- [llama.cpp Discussions](https://github.com/ggml-org/llama.cpp/discussions)
- [Building llama.cpp for Android Discussion](https://github.com/ggml-org/llama.cpp/discussions/4960)

---

## 결론

SmolVLM-500M + llama.cpp를 기존 Kotlin Android 앱에 통합하는 것은 **완전히 가능**하며, 다음 방법을 추천합니다:

1. **kotlinllamacpp 라이브러리 사용** (가장 간단)
2. 또는 **llama-cpp-kt (JNA 기반)** 사용
3. 모델은 **SmolVLM-500M-Q4_K_M** 사용 (성능/크기 밸런스)

프로토타입을 빠르게 만들고 싶다면 기존 프로젝트를 포크해서 필요한 부분만 복사하는 것이 가장 효율적입니다.

---

**연구 완료일**: 2025-10-23
**다음 단계**: 실제 구현 및 성능 테스트
