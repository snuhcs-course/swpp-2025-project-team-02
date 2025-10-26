#!/bin/bash
# Automatic NDK installation script
# Run this once if you don't have Android NDK installed

set -e

echo "🔧 Automatic Android NDK Installation"
echo ""

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Detect SDK path
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then
        ANDROID_HOME="$HOME/Library/Android/sdk"
    elif [ -d "$HOME/Android/Sdk" ]; then
        ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -f "$PROJECT_ROOT/local.properties" ]; then
        SDK_DIR=$(grep "^sdk.dir=" "$PROJECT_ROOT/local.properties" | cut -d'=' -f2)
        if [ -n "$SDK_DIR" ]; then
            ANDROID_HOME="$SDK_DIR"
        fi
    fi
fi

if [ -z "$ANDROID_HOME" ]; then
    echo "❌ Error: Android SDK not found"
    echo ""
    echo "Please install Android Studio first:"
    echo "  https://developer.android.com/studio"
    echo ""
    echo "Or set ANDROID_HOME manually:"
    echo "  export ANDROID_HOME=~/Library/Android/sdk"
    exit 1
fi

echo "📦 Using Android SDK: $ANDROID_HOME"

# Check if NDK already installed
if [ -d "$ANDROID_HOME/ndk" ] && [ -n "$(ls -A "$ANDROID_HOME/ndk" 2>/dev/null)" ]; then
    NDK_VERSION=$(ls "$ANDROID_HOME/ndk" | grep -E '^[0-9]+\.' | sort -V | tail -1)
    echo "✅ NDK already installed: $NDK_VERSION"
    echo ""
    echo "If you still have build issues, run:"
    echo "  ./gradlew clean"
    echo "  ./gradlew assembleDebug"
    exit 0
fi

# Find sdkmanager
SDKMANAGER=""
if [ -f "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    SDKMANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
elif [ -f "$ANDROID_HOME/tools/bin/sdkmanager" ]; then
    SDKMANAGER="$ANDROID_HOME/tools/bin/sdkmanager"
fi

if [ -z "$SDKMANAGER" ]; then
    echo "⚠️  Warning: sdkmanager not found"
    echo ""
    echo "Please install NDK manually via Android Studio:"
    echo "  1. Open Android Studio"
    echo "  2. Settings → System Settings → Android SDK"
    echo "  3. SDK Tools tab → Check 'NDK (Side by side)'"
    echo "  4. Apply → OK"
    echo ""
    echo "See BUILD.md for detailed instructions."
    exit 1
fi

echo "📥 Installing Android NDK (this may take a few minutes, ~600MB download)..."
echo ""

# Install NDK using sdkmanager
# Accept licenses automatically
yes | "$SDKMANAGER" --licenses > /dev/null 2>&1 || true

# Install NDK
"$SDKMANAGER" "ndk;27.0.12077973" --channel=0

# Verify installation
if [ -d "$ANDROID_HOME/ndk/27.0.12077973" ]; then
    echo ""
    echo "✅ NDK installed successfully!"
    echo "   Location: $ANDROID_HOME/ndk/27.0.12077973"
    echo ""
    echo "Next steps:"
    echo "  1. Run: ./gradlew assembleDebug"
    echo "  2. OpenCL headers will be installed automatically during build"
    echo ""
else
    echo ""
    echo "❌ NDK installation failed"
    echo ""
    echo "Please install manually via Android Studio:"
    echo "  Settings → System Settings → Android SDK → SDK Tools"
    echo "  Check 'NDK (Side by side)' → Apply"
    exit 1
fi
