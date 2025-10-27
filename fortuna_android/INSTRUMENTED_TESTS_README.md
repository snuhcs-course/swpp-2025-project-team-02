# Instrumented Tests for Fortuna Android

## Overview
Comprehensive instrumented tests have been created to increase code coverage for the Fortuna Android project. These tests run on an actual Android device or emulator and test UI components, Activities, and Fragments.

## Created Test Files

### Activity Tests
1. **MainActivityInstrumentedTest.kt** (33 tests)
   - Activity lifecycle tests
   - Permission tests (CAMERA, ACCESS_FINE_LOCATION)
   - SharedPreferences tests
   - ARCore session tests
   - Configuration change tests
   - Memory and resource tests
   - Integration tests

2. **AuthContainerActivityInstrumentedTest.kt** (10 tests)
   - Activity lifecycle tests
   - Configuration change tests
   - Fragment manager tests
   - Context and theme tests

### Fragment Tests
3. **HomeFragmentInstrumentedTest.kt** (5 tests)
   - Fragment lifecycle tests
   - View creation tests
   - Configuration change tests

4. **ProfileFragmentInstrumentedTest.kt** (7 tests)
   - Fragment lifecycle tests
   - SharedPreferences access tests
   - Configuration change tests

5. **SettingsFragmentInstrumentedTest.kt** (7 tests)
   - Fragment lifecycle tests
   - SharedPreferences tests
   - Multiple lifecycle cycle tests

6. **ProfileInputFragmentInstrumentedTest.kt** (7 tests)
   - Fragment lifecycle tests
   - SharedPreferences tests
   - Configuration change tests

7. **SignInFragmentInstrumentedTest.kt** (9 tests)
   - Fragment lifecycle tests
   - Initial state tests
   - Configuration change tests
   - SharedPreferences tests

**Total: 78 instrumented tests**

## Running Instrumented Tests

### Prerequisites
1. Connect an Android device via USB with USB debugging enabled, OR
2. Start an Android emulator (API level 24 or higher)

### Run All Instrumented Tests
```bash
./gradlew connectedDebugAndroidTest
```

### Run Specific Test Class
```bash
./gradlew connectedDebugAndroidTest --tests "com.example.fortuna_android.MainActivityInstrumentedTest"
```

### Generate Coverage Report for Instrumented Tests
```bash
# Step 1: Run instrumented tests with coverage
./gradlew createDebugCoverageReport

# Step 2: Generate Jacoco report
./gradlew jacocoAndroidTestReport
```

### Generate Combined Coverage Report (Unit + Instrumented)
```bash
# Run both unit and instrumented tests, then generate combined report
./gradlew testDebugUnitTest
./gradlew createDebugCoverageReport
./gradlew jacocoFullReport
```

## Coverage Reports Location

After running the coverage tasks, reports will be available at:

1. **Unit Test Coverage Only:**
   - `app/build/reports/jacoco/jacocoTestReport/html/index.html`

2. **Instrumented Test Coverage Only:**
   - `app/build/reports/coverage/androidTest/debug/index.html`
   - `app/build/reports/jacoco/jacocoAndroidTestReport/html/index.html`

3. **Combined Coverage (Unit + Instrumented):**
   - `app/build/reports/jacoco/jacocoFullReport/html/index.html`

## Expected Coverage Improvement

With the addition of 78 instrumented tests covering:
- MainActivity (393 lines)
- AuthContainerActivity
- HomeFragment
- ProfileFragment
- SettingsFragment
- ProfileInputFragment
- SignInFragment

**Estimated additional coverage:** ~600-800 lines of code from UI components

**Current unit test coverage:** 27% (870/3,319 lines)

**Estimated total coverage with instrumented tests:** 45-50%

To reach 80% coverage, additional instrumented tests would need to be created for:
- CameraFragment
- ARFragment
- TodayFortuneFragment
- DetailAnalysisFragment
- ProfileEditFragment
- Additional MainActivity edge cases with real Android system services

## Troubleshooting

### Device/Emulator Not Found
```
Error: No connected devices!
```
**Solution:** Connect a device or start an emulator before running tests.

### Insufficient Permissions
```
Error: Permission denied
```
**Solution:** Tests use `GrantPermissionRule` to automatically grant permissions. Ensure your device/emulator allows permission granting.

### Coverage Data Not Generated
```
No .ec file found
```
**Solution:** Ensure `enableAndroidTestCoverage = true` is set in build.gradle debug buildType.

### Test Timeout
```
Test timed out
```
**Solution:** Increase timeout in test options or ensure the device/emulator is responsive.

## Notes

1. **Instrumented tests are slower** than unit tests because they require an actual Android environment.

2. **ARCore features** may not work properly on emulators without Google Play Services. Use a physical device for best results.

3. **Network-dependent tests** (API calls) may fail without proper mocking. Current tests focus on local functionality.

4. **Firebase services** are not fully tested in instrumented tests due to authentication requirements.

5. For **CI/CD integration**, consider using Firebase Test Lab or AWS Device Farm for running instrumented tests in the cloud.

## Quick Command Reference

```bash
# List available devices
adb devices

# Run all instrumented tests
./gradlew connectedDebugAndroidTest

# Run with coverage
./gradlew createDebugCoverageReport

# Generate full coverage report
./gradlew jacocoFullReport

# View HTML report (macOS)
open app/build/reports/jacoco/jacocoFullReport/html/index.html

# View HTML report (Linux)
xdg-open app/build/reports/jacoco/jacocoFullReport/html/index.html

# Clean build
./gradlew clean
```
