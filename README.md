# Fortuna - Iteration 5 Demo

> **Branch**: `iteration-5-demo`
> **Team**: Team 02 (TEAM 을사)

---

## Table of Contents
1. [What This Demo Demonstrates](#what-this-demo-demonstrates)
2. [How to Run the Demo](#how-to-run-the-demo)
3. [Demo Video](#demo-video)

---

## What This Demo Demonstrates

### Features Implemented in Iteration 5

#### 1. Enhanced Fortune Experience
- **AI-Generated 4-Panel Comic Fortune Image**
  - Migrated from gpt-image-1 to Google Gemini 2.5 Flash (nanobana)
  - Fortune visualization with character images
  - Completion celebration image when 5+ elements collected

- **Fortune Generation UX**
  - Loading messages during AI generation ("운세를 생성하고 있습니다...", etc.)
  - AI generation placeholder UI while processing
  - Score calculation: +4 points per collected element (can exceed 100)

- **TTS (Text-to-Speech) System**
  - TTS replay button for fortune narration
  - Android native TTS with Korean language support
  - Single-active-text to prevent audio overlap

#### 2. Improved AR Game Mechanics
- **Collection Feedback**
  - Required element: "수집 완료!" with long vibration
  - Non-required element: short vibration with "XX는 더 이상 필요하지 않아요"
  - Quest completion: "목표 달성 완료" message

- **AR Reset Button**
  - "리셋" button clears all 3D characters
  - Shows "AR 초기화 완료" confirmation message

- **Camera & Detection Improvements**
  - Camera permission handling with device settings navigation
  - Dark environment detection failure guidance
  - Distance-based scaling for 3D character readability

#### 3. Tutorial System Overhaul
- **Separated Tutorial Buttons**
  - "사주 가이드" - Saju education walkthrough (6 pages)
  - "개운 튜토리얼" - AR game tutorial replay
  - Close ("닫기") button returns to main page

- **6-Page Onboarding**
  - IntroActivity with ViewPager2 and dot indicators
  - Skip button for returning users
  - Pokemon-style dialogue overlays

#### 4. Profile & Data Management
- **Profile Mid-Exit Handling**
  - Dropout ninja case: returns to profile input on app restart
  - Profile sync with main screen after updates

- **Caching for Fast Fortune Loading**
  - FortuneResult caching prevents repeated API calls
  - ARFragment cache invalidation on quest completion

- **Element Collection History**
  - Click element icon to see date and count
  - Monthly collection log with calendar view

#### 5. Design Pattern Implementation
- **Factory Method Pattern** - Object Detector (ConfigurableDetectorFactory)
- **Composite Pattern** - ARRenderer layered rendering
- **UML Class Diagram** - Full architecture documentation

#### 6. Code Quality & Refactoring
- Removed unused code
- Replaced hard-coded color values
- Color mismatch bug fix
- Race condition fix in image generation
- Tutorial end lock bug fix

### Goals Achieved
- **Usability**: Comprehensive tutorial system for new users
- **Reliability**: Proper error handling and edge case coverage
- **Performance**: Caching and async processing optimization
- **Code Quality**: Design patterns and refactoring applied
- **Documentation**: Updated R&S.md, WIKI.md aligned with UAT test cases

---

## How to Run the Demo

### Environment Used
| Component | Version |
|-----------|---------|
| Java | 17 |
| Android SDK | 34 |
| NDK | 27.0.12077973 |
| Kotlin | 1.9.x |
| Gradle | 8.x |
| Test Device | Samsung Galaxy S23 |

### Prerequisites
- Java 17
- Android Studio or command-line build tools
- Android smartphone for APK installation
- NDK version 27.0.12077973 (stable) for llama-module compile

### Setup Instructions

1. **Clone and navigate to the project:**
```bash
git clone https://github.com/snuhcs-course/swpp-2025-project-team-02.git
cd swpp-2025-project-team-02
git checkout iteration-5-demo
```

2. **Navigate to the Android project directory:**
```bash
cd fortuna_android
```

3. **Create local configuration file:**
```bash
touch local.properties
```

4. **Add the following configuration to `local.properties`:**
```properties
# Android SDK location (replace with your actual path)
# macOS: /Users/YOUR_USERNAME/Library/Android/sdk
# Linux: /home/YOUR_USERNAME/Android/Sdk
# Windows: C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk
sdk.dir=/path/to/your/Android/sdk

GOOGLE_CLIENT_ID=30527837999-vs55bhsnu1rf4qnrikgpko6p5gtj76jd.apps.googleusercontent.com

# API Configuration
API_BASE_URL=https://fortuna.up.railway.app/
API_HOST=fortuna.up.railway.app
```

5. **Update submodule for on-device VLM llama.cpp:**
```bash
git submodule update --init --recursive
```

6. **Setup OpenCL for GPU acceleration:**
```bash
./scripts/setup-opencl.sh
```

**Supported Hardware:**

| Device | GPU | Status |
|--------|-----|--------|
| Galaxy S24+ | Adreno 750 (Snapdragon 8 Gen 3) | Verified |
| Galaxy S23 | Adreno 740 (Snapdragon 8 Gen 2) | Supported |
| Snapdragon 8 Elite devices | Adreno 830 | Supported |
| Snapdragon 7+ Gen 3 | Adreno 732 | Limited support |
| Mali GPUs (Exynos) | - | CPU fallback |

7. **Build the release APK:**
```bash
./gradlew assembleRelease \
    -Pandroid.injected.signing.store.file=$(pwd)/testkey.jks \
    -Pandroid.injected.signing.store.password=1q2w3e4r! \
    -Pandroid.injected.signing.key.alias=key0 \
    -Pandroid.injected.signing.key.password=1q2w3e4r!
```

8. **Install the APK:**
   - Locate APK at: `app/build/outputs/apk/release/app-release.apk`
   - Transfer to Android smartphone
   - Install and run

---

## Demo Video

[Watch Demo Video](demo.mp4)

The demo video showcases:
- Google login and profile setup
- Daily fortune display with AI-generated image
- TTS narration playback
- AR game element collection
- Quest completion celebration
- Tutorial walkthrough
- Profile management

---

## Project Structure

```
swpp-2025-project-team-02/
├── fortuna_android/     # Android client (Kotlin)
├── fortuna_api/         # Django backend (Python)
├── R&S.md              # Requirements & Specifications
├── WIKI.md             # System Design & Testing Plan
├── MEETING_LOGS.md     # Daily standup logs
└── README.md           # This file
```
