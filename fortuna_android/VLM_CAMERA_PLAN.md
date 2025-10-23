# VLM Camera Activity Redesign Plan

## User Requirements
- **전체 화면 카메라 뷰** (fullscreen camera preview)
- **버튼 1개만** 아래에 배치
- **버튼 클릭** → 현재 카메라 프레임 캡처 → VLM description → 화면에 오버레이

## Architecture

### UI Components (activity_vlm_test.xml)
```
├── CameraX PreviewView (전체 화면)
├── TextView (description overlay, 상단, 반투명 배경)
├── ProgressBar (중앙, 로딩 표시)
└── FloatingActionButton (하단 중앙, 캡처 버튼)
```

### VLMTestActivity.kt Flow
1. **onCreate**:
   - CameraX 초기화
   - SmolVLMManager 초기화 (백그라운드에서 모델 로드)
   - PreviewView 설정
   - 카메라 권한 체크 & 요청

2. **Camera Setup**:
   - CameraX PreviewView로 실시간 프리뷰
   - ImageCapture usecase 설정
   - Back camera 사용

3. **Button Click Flow**:
   ```
   Click → Check model loaded
         → ImageCapture.takePicture()
         → ImageProxy → Bitmap conversion
         → vlmManager.analyzeImage(bitmap, "Describe what you see in this image in detail.")
         → Stream tokens → Update overlay TextView with fade-in animation
   ```

4. **Overlay Update**:
   - TextView (top, semi-transparent black background #AA000000)
   - Fade in animation (300ms) when text arrives
   - Text updates in real-time as tokens stream in

## Implementation Complete ✅

### Files Modified
1. **activity_vlm_test.xml**: ✅
   - PreviewView (match_parent, fullscreen)
   - TextView overlay (top, semi-transparent)
   - ProgressBar (center, loading indicator)
   - FAB (bottom center, camera icon)

2. **VLMTestActivity.kt**: ✅
   - Complete rewrite (365 lines → 296 lines)
   - CameraX integration with Preview + ImageCapture
   - Permission handling with ActivityResultContracts
   - ImageProxy → Bitmap conversion
   - Real-time VLM streaming with overlay update
   - Fade-in animation for description
   - Background model loading
   - Proper cleanup in onDestroy

### Dependencies
- ✅ CameraX already included (libs.bundles.camerax)
  - camera-core, camera-camera2, camera-lifecycle
  - camera-video, camera-view, camera-extensions

### Build Status
- ✅ BUILD SUCCESSFUL
- ✅ No compilation errors
- ✅ Ready for testing

## Usage
1. Open app → Navigate to VLM Test
2. Camera permission requested automatically
3. Full screen camera preview appears
4. Model loads in background (may take ~10 seconds)
5. Tap camera button at bottom
6. VLM analyzes current frame
7. Description appears at top with fade-in
8. Tap again for new description
