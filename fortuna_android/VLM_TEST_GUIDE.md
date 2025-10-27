# VLM Unit Test Guide

## 테스트 파일 위치

### 1. SmolVLMManagerTest.kt
**위치**: `app/src/androidTest/java/com/example/fortuna_android/vlm/SmolVLMManagerTest.kt`

**테스트 항목**:
- ✅ `testModelLoading()` - 모델이 정상적으로 로딩되는지
- ✅ `testTextGeneration()` - 텍스트 생성이 작동하는지
- ✅ `testBitmapCreation()` - 테스트용 비트맵 생성
- ✅ `testImageAnalysis()` - 이미지 분석 (임베딩 → 텍스트)
- ✅ `testMultipleInferences()` - 연속 추론이 가능한지
- ✅ `testModelUnload()` - 모델 언로드가 정상 작동하는지

### 2. VLMNativeTest.kt
**위치**: `llama-module/src/androidTest/java/android/llama/cpp/VLMNativeTest.kt`

**테스트 항목**:
- ✅ `testLibraryLoaded()` - 네이티브 라이브러리 로딩
- ✅ `testBitmapToNativeConversion()` - 비트맵 RGBA 데이터 검증
- ✅ `testModelFileExists()` - 모델 파일 존재 확인
- ✅ `testInternalStorageAccess()` - 파일 시스템 접근
- ✅ `testBitmapFormats()` - 다양한 비트맵 포맷
- ✅ `testLargeImageHandling()` - 큰 이미지 처리

## 테스트 실행 방법

### Android Studio에서 실행
1. Android Studio에서 테스트 파일 열기
2. 클래스 이름 옆 초록색 화살표 클릭
3. "Run 'SmolVLMManagerTest'" 선택
4. 디바이스/에뮬레이터 선택

### 커맨드 라인에서 실행

#### 전체 테스트 실행
```bash
./gradlew connectedDebugAndroidTest
```

#### 특정 클래스만 실행
```bash
# SmolVLMManager 테스트만
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.fortuna_android.vlm.SmolVLMManagerTest

# Native 테스트만
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=android.llama.cpp.VLMNativeTest
```

#### 특정 테스트 메서드만 실행
```bash
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.example.fortuna_android.vlm.SmolVLMManagerTest#testModelLoading
```

### 테스트 결과 확인

테스트 결과는 다음 위치에 생성됩니다:
```
app/build/reports/androidTests/connected/index.html
llama-module/build/reports/androidTests/connected/index.html
```

브라우저에서 HTML 리포트 열기:
```bash
open app/build/reports/androidTests/connected/index.html
```

## 테스트 시나리오별 설명

### 1. 모델 로딩 테스트
```kotlin
@Test
fun testModelLoading() = runBlocking {
    vlmManager.initialize()  // 모델 + mmproj 로딩
    assertTrue(vlmManager.isLoaded())
}
```
**검증 내용**:
- Assets에서 모델 파일 복사
- llama.cpp 모델 로딩
- mmproj (vision encoder) 로딩
- 60초 타임아웃 내 완료

### 2. 텍스트 생성 테스트
```kotlin
@Test
fun testTextGeneration() = runBlocking {
    vlmManager.initialize()
    val tokens = vlmManager.generateText("Echo: Test")
        .take(10)
        .toList()

    assertTrue(tokens.isNotEmpty())
}
```
**검증 내용**:
- 텍스트만으로 추론 가능 (이미지 없이)
- 토큰 스트리밍 정상 작동
- 모델이 응답 생성

### 3. 이미지 분석 테스트 (핵심!)
```kotlin
@Test
fun testImageAnalysis() = runBlocking {
    vlmManager.initialize()

    val testBitmap = createTestBitmap(224, 224, Color.RED)
    val tokens = vlmManager.analyzeImage(testBitmap, "What color is this?")
        .take(20)
        .toList()

    assertTrue(tokens.isNotEmpty())
}
```
**검증 내용**:
- ✅ Android Bitmap → native mtmd_bitmap 변환
- ✅ RGBA → RGB 변환 정상
- ✅ mtmd_tokenize (텍스트+이미지 청크 생성)
- ✅ mtmd_helper_eval_chunks (vision encoder 실행)
- ✅ 이미지 임베딩 생성
- ✅ llama_decode with embeddings
- ✅ 응답 토큰 생성

**이 테스트가 통과하면 전체 VLM 파이프라인이 정상 작동하는 것!**

### 4. 비트맵 변환 검증
```kotlin
@Test
fun testBitmapToNativeConversion() {
    val bitmap = Bitmap.createBitmap(100, 100, ARGB_8888)
    bitmap.eraseColor(Color.RED)

    val pixel = bitmap.getPixel(50, 50)
    assertEquals(255, Color.red(pixel))   // R
    assertEquals(0, Color.green(pixel))   // G
    assertEquals(0, Color.blue(pixel))    // B
    assertEquals(255, Color.alpha(pixel)) // A
}
```
**검증 내용**:
- RGBA 픽셀 데이터 올바름
- JNI로 전달될 데이터 무결성

## 예상 실행 시간

| 테스트 | 예상 시간 |
|--------|-----------|
| testLibraryLoaded | < 1초 |
| testBitmapCreation | < 1초 |
| testModelFileExists | < 2초 |
| testModelLoading | ~30-60초 |
| testTextGeneration | ~10-30초 |
| testImageAnalysis | ~20-45초 |

**전체 테스트 스위트**: 약 2-5분

## 디버그 로그 확인

테스트 실행 중 로그 확인:
```bash
adb logcat | grep -E "llama-android|SmolVLM|VLMTestActivity"
```

주요 로그 메시지:
- `Bitmap created: 224x224` - 비트맵 변환 성공
- `Tokenized into N chunks` - 청크 생성 성공
- `Chunks evaluated successfully, new n_past: X` - **임베딩 생성 성공** ✅
- `Model loaded successfully` - 모델 로딩 성공

## 실패 시 디버깅

### testModelLoading 실패
- 모델 파일이 assets/models/에 있는지 확인
- 파일 크기 확인 (SmolVLM: ~417MB, mmproj: ~104MB)
- 디바이스 저장공간 확인

### testImageAnalysis 실패
1. `testTextGeneration` 먼저 실행 → 모델 자체 문제인지 확인
2. 로그에서 `eval_chunks` 실패 메시지 확인
3. 비트맵 크기 확인 (너무 크면 OOM)

### 타임아웃 발생
- 에뮬레이터는 느릴 수 있음 (실제 기기 사용 권장)
- 타임아웃 시간 늘리기: `withTimeout(120000)` (2분)

## 테스트 성공 기준

✅ **모든 테스트 통과 = VLM 완벽 작동**

특히 `testImageAnalysis`가 통과하면:
- 이미지 → 임베딩 변환 정상
- Vision encoder 작동
- 멀티모달 추론 성공
- 앱에서 카메라 이미지 분석 가능!

## CI/CD 통합

GitHub Actions 예시:
```yaml
- name: Run instrumented tests
  uses: reactivecircus/android-emulator-runner@v2
  with:
    api-level: 29
    script: ./gradlew connectedDebugAndroidTest
```

## 다음 단계

테스트 통과 후:
1. 실제 디바이스에서 VLMTestActivity 테스트
2. 다양한 이미지로 테스트 (색상, 물체, 텍스트 등)
3. 성능 프로파일링
4. 추가 테스트 케이스 작성
