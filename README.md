# Fortuna Android - Iteration 4 Demo

## Features Implemented

### 1. Enhanced AR Game Experience
- **Immersive Audio & Visual Feedback**
  - Background music during AR gameplay
  - Character crying voices after detection
  - Sound effects on capture events
  - Vibration feedback when capturing elements
  - Random bouncing animations for 3D character objects

- **Improved Game Mechanics**
  - All types of element characters now discoverable
  - One-click collection system (collect one element per click)
  - Quest clear condition improvements
  - Bounding box view optimization
  - VLM scan state stability fixes
  - Status bar hidden for immersive experience

### 2. On-Device SmolVLM Optimization
- **Model Fine-tuning & Integration**
  - Custom instruction-tuning dataset generated from COCO dataset
  - LoRA-based training pipeline with W&B monitoring
  - Teacher forcing to generation-based training approach
  - OpenCL GPU acceleration for improved tokens-per-second
  - Memory optimization for L40S deployment
  - Bounding box integration with VLM analysis
  - Performance analysis report and Gradio demo deployment

### 3. Comprehensive UI/UX Overhaul
- **Main Screen Redesign**
  - Beautiful game entry button design
  - Today's collected elements status display
  - Score addition UI after collection
  - Saju-style commentary below daily fortune
  - Main navigation alignment and centering fixes
  - Purple-themed central game button

- **Profile & Navigation Updates**
  - Confirmation modals for user actions
  - Redesigned navigation system
  - Enhanced profile view
  - Improved calendar interface
  - "Replenish Today's Energy" button with animations

- **Element Collection Features**
  - Monthly collection log API integration
  - Element-specific collection date and count tracking
  - Collection log modal per element type

### 4. App Tutorial System
- **Onboarding Experience**
  - Main screen explanation tutorial
  - Game entry tutorial with instructions
  - Tutorial replay button for returning users
  - Step-by-step guidance for new users

### 5. Backend Improvements
- **Saju Calculation Updates**
  - Automatic daily fortune recalculation on profile update
  - Great fortune (Daeun) recalculation on attribute changes
  - Null value bug fixes for great fortune
  - Multiple element collection API support
  - Monthly collection log retrieval API

### 6. Guidebook & Documentation
- Redesigned guidebook with clear explanations
- Improved readability with condensed content
- Saju explanation page UI flow optimization

## How to Run the Demo

### Prerequisites
- Java 17
- Android smartphone for APK installation
- NDK version 27.0.12077973 (stable) for llama-module compile

### Setup Instructions

1. Navigate to the Android project directory:

```bash
cd fortuna_android
```

2. Create local configuration file:

```bash
touch local.properties
```

3. Add the following configuration to `local.properties`:

```properties
# Android SDK location (replace with your actual Android SDK path)
# Common paths:
# - macOS: /Users/YOUR_USERNAME/Library/Android/sdk
# - Linux: /home/YOUR_USERNAME/Android/Sdk
# - Windows: C:\Users\YOUR_USERNAME\AppData\Local\Android\Sdk
sdk.dir=/path/to/your/Android/sdk

GOOGLE_CLIENT_ID=30527837999-vs55bhsnu1rf4qnrikgpko6p5gtj76jd.apps.googleusercontent.com

# API Configuration
API_BASE_URL=https://fortuna.up.railway.app/
API_HOST=fortuna.up.railway.app
```

**Note**: Replace `/path/to/your/Android/sdk` with your actual Android SDK installation path.

4. Update submodule for on-device VLM llama.cpp:

```bash
git submodule update --init --recursive
```

5. Setup OpenCL for GPU acceleration:

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
| Other GPUs | - | CPU fallback |

6. Build the release APK (ensure Java 17 is active):

```bash
./gradlew assembleRelease \
    -Pandroid.injected.signing.store.file=$(pwd)/testkey.jks \
    -Pandroid.injected.signing.store.password=1q2w3e4r! \
    -Pandroid.injected.signing.key.alias=key0 \
    -Pandroid.injected.signing.key.password=1q2w3e4r!
```

7. Install the APK:
   - Locate the generated APK at: `app/build/outputs/apk/release/app-release.apk`
   - Transfer the APK file to your Android smartphone
   - Install and run the app

## Demo Goals Achieved

- **Usability Improvement**: Main goal of Iteration 4 focused on enhancing user experience based on Heuristic Evaluation feedback
- **Immersive Gameplay**: Added comprehensive audio/visual/haptic feedback to AR game
- **Performance Optimization**: Fine-tuned SmolVLM with GPU acceleration for faster inference
- **Polished UI/UX**: Complete redesign of main flow, navigation, and profile interfaces
- **User Onboarding**: Implemented tutorial system to guide new users through app features

## Demo Video

[Demo Video](iteration-4-demo.mp4)
