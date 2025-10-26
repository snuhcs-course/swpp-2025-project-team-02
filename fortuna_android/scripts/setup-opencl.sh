#!/bin/bash
# Setup OpenCL headers for Android NDK
# This script is automatically run during Gradle build

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🔧 Setting up OpenCL headers for GPU acceleration..."

# Detect NDK path with multiple strategies
if [ -z "$ANDROID_NDK" ]; then
    # Strategy 1: Check ANDROID_HOME/ANDROID_SDK_ROOT
    if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME/ndk" ]; then
        NDK_BASE="$ANDROID_HOME/ndk"
    elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT/ndk" ]; then
        NDK_BASE="$ANDROID_SDK_ROOT/ndk"
    # Strategy 2: macOS default locations
    elif [ -d "$HOME/Library/Android/sdk/ndk" ]; then
        NDK_BASE="$HOME/Library/Android/sdk/ndk"
    # Strategy 3: Linux default locations
    elif [ -d "$HOME/Android/Sdk/ndk" ]; then
        NDK_BASE="$HOME/Android/Sdk/ndk"
    # Strategy 4: Check local.properties
    elif [ -f "$PROJECT_ROOT/local.properties" ]; then
        SDK_DIR=$(grep "^sdk.dir=" "$PROJECT_ROOT/local.properties" | cut -d'=' -f2)
        if [ -n "$SDK_DIR" ] && [ -d "$SDK_DIR/ndk" ]; then
            NDK_BASE="$SDK_DIR/ndk"
        fi
    fi

    if [ -z "$NDK_BASE" ] || [ ! -d "$NDK_BASE" ]; then
        echo "⚠️  Warning: Android NDK not found"
        echo "   Tried locations:"
        echo "   - \$ANDROID_HOME/ndk"
        echo "   - \$ANDROID_SDK_ROOT/ndk"
        echo "   - ~/Library/Android/sdk/ndk (macOS)"
        echo "   - ~/Android/Sdk/ndk (Linux)"
        echo ""
        echo "   🚀 Quick fix: Run automatic installation"
        echo "      ./scripts/install-ndk.sh"
        echo ""
        echo "   Or install manually via Android Studio:"
        echo "      Settings → Android SDK → SDK Tools → NDK (Side by side)"
        echo ""
        echo "   See BUILD.md for detailed instructions."
        echo ""
        echo "   ⚠️  Build will continue but GPU acceleration will not be available."
        echo "      The app will fallback to CPU mode."
        exit 0  # Don't fail the build
    fi

    # Find the latest NDK version
    NDK_VERSION=$(ls "$NDK_BASE" | grep -E '^[0-9]+\.' | sort -V | tail -1)
    if [ -z "$NDK_VERSION" ]; then
        echo "⚠️  Warning: No NDK version found in $NDK_BASE"
        exit 0
    fi

    ANDROID_NDK="$NDK_BASE/$NDK_VERSION"
fi

echo "📦 Using Android NDK: $ANDROID_NDK"

# Check if NDK exists
if [ ! -d "$ANDROID_NDK" ]; then
    echo "⚠️  Warning: NDK directory not found: $ANDROID_NDK"
    exit 0
fi

# Find sysroot (handle both x86_64 and arm64 macOS)
if [ "$(uname)" = "Darwin" ]; then
    if [ "$(uname -m)" = "arm64" ]; then
        NDK_SYSROOT="$ANDROID_NDK/toolchains/llvm/prebuilt/darwin-aarch64/sysroot"
        if [ ! -d "$NDK_SYSROOT" ]; then
            NDK_SYSROOT="$ANDROID_NDK/toolchains/llvm/prebuilt/darwin-x86_64/sysroot"
        fi
    else
        NDK_SYSROOT="$ANDROID_NDK/toolchains/llvm/prebuilt/darwin-x86_64/sysroot"
    fi
elif [ "$(uname)" = "Linux" ]; then
    NDK_SYSROOT="$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/sysroot"
else
    echo "⚠️  Warning: Unsupported OS: $(uname)"
    exit 0
fi

if [ ! -d "$NDK_SYSROOT" ]; then
    echo "⚠️  Warning: NDK sysroot not found: $NDK_SYSROOT"
    exit 0
fi

INCLUDE_DIR="$NDK_SYSROOT/usr/include"
LIB_DIR="$NDK_SYSROOT/usr/lib/aarch64-linux-android"

# Check if OpenCL is already fully installed
OPENCL_INSTALLED=false
if [ -d "$INCLUDE_DIR/CL" ] && [ -f "$LIB_DIR/libOpenCL.so" ]; then
    OPENCL_INSTALLED=true
fi

if [ "$OPENCL_INSTALLED" = true ]; then
    echo "✅ OpenCL headers and library already installed"
    echo "   Headers: $INCLUDE_DIR/CL"
    echo "   Library: $LIB_DIR/libOpenCL.so"
    exit 0
fi

echo "📥 Installing OpenCL headers and library..."

# Clone OpenCL headers
TEMP_DIR=$(mktemp -d)
cd "$TEMP_DIR"

# Install headers if needed
if [ ! -d "$INCLUDE_DIR/CL" ]; then
    echo "  Cloning OpenCL-Headers..."
    git clone --depth 1 https://github.com/KhronosGroup/OpenCL-Headers.git

    echo "  Copying headers to NDK sysroot..."
    cp -r OpenCL-Headers/CL "$INCLUDE_DIR/"
    echo "  ✅ Headers installed"
fi

# Install library if needed
if [ ! -f "$LIB_DIR/libOpenCL.so" ]; then
    echo "  Copying libOpenCL.so to NDK sysroot..."

    # Source: our prebuilt libOpenCL.so
    OPENCL_LIB="$PROJECT_ROOT/app/src/main/jniLibs/arm64-v8a/libOpenCL.so"

    if [ -f "$OPENCL_LIB" ]; then
        # Ensure lib directory exists
        mkdir -p "$LIB_DIR"

        # Copy library to NDK
        cp "$OPENCL_LIB" "$LIB_DIR/"

        echo "  ✅ Library installed at $LIB_DIR/libOpenCL.so"
    else
        echo "  ⚠️  Warning: libOpenCL.so not found at $OPENCL_LIB"
        echo "     CMake may fail to find OpenCL library"
    fi
fi

# Cleanup
cd "$PROJECT_ROOT"
rm -rf "$TEMP_DIR"

echo ""
echo "✅ OpenCL setup complete!"
echo "   Headers: $INCLUDE_DIR/CL"
echo "   Library: $LIB_DIR/libOpenCL.so"
echo ""
echo "📝 CMake will now be able to find OpenCL automatically"
