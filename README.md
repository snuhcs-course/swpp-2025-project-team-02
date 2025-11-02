# Fortuna Android - Iteration 3 Demo

## Features Implemented

1. **Google Social Login & Basic Saju Information Registration**

   - Users can log in using their Google account
   - Basic profile information can be registered
   - Profile modification, deleting account implemented

2. **AR 개운 game to collect deficient chakra**

   - Detect the object through the camera lens(Object Detection Model) 
   - Distinguish if the object is representing deficient chakra with On-device VLM(SmolVLM) - Currently fine-tuning, just integrated in client app
   - Make corresponding element characters which is 3D colored assets
   - User can find collectd elements so far in his profile

3. **Individualized Saju Generation**
   - Use our scoring logic based on chakra balance to generate daily fortune
   - Results displayed on screen when entered in the app

4. **Test Implementation**
   - Testing for Client has acheived 80% coverage
   - But in current iteration 3 branch, test has some compile errors due to conflict between modified UI and testing codes

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

4. Update submodule for on-device VLM llama.cpp

```bash
git submodule update --init --recursive

```

5. Setup opencl for GPU acceleration

```bash
./scripts/setup-opencl.sh

```

Supported Hardware
| Device | GPU | Status |
|--------|-----|--------|
| Galaxy S24+ | Adreno 750 (Snapdragon 8 Gen 3) | ✅ Verified |
| Galaxy S23 | Adreno 740 (Snapdragon 8 Gen 2) | ✅ Supported |
| Snapdragon 8 Elite devices | Adreno 830 | ✅ Supported |
| Snapdragon 7+ Gen 3 | Adreno 732 | ⚠️ Limited support |
| Mali GPUs (Exynos) | - | ⚠️ CPU fallback |
| Other GPUs | - | ⚠️ CPU fallback |

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

## Demo Video

[Demo Video](iteration-3-demo.mp4.zip)

