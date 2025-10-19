# Fortuna Android - Iteration 2 Demo

## Features Implemented

1. **Google Social Login & Basic Saju Information Registration**

   - Users can log in using their Google account
   - Basic profile information can be registered
   - Every token-related error case are solved

2. **AR features to detect deficient chackra**

   - Detect the object through the camera lens and make corresponding anchors

3. **Individualized Saju Generation**
   - Use our scoring logic to generate daily fortune
   - Results displayed on screen when entered in the app

## How to Run the Demo

### Prerequisites

- Java 17
- Android smartphone for APK installation

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

4. Build the release APK (ensure Java 17 is active):

```bash
./gradlew assembleRelease \
    -Pandroid.injected.signing.store.file=$(pwd)/testkey.jks \
    -Pandroid.injected.signing.store.password=1q2w3e4r! \
    -Pandroid.injected.signing.key.alias=key0 \
    -Pandroid.injected.signing.key.password=1q2w3e4r!
```

5. Install the APK:
   - Locate the generated APK at: `app/build/outputs/apk/release/app-release.apk`
   - Transfer the APK file to your Android smartphone
   - Install and run the app

## Demo Video

[Demo Video](iteration-2-demo.mp4.zip)

