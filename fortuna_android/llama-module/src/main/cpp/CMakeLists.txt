# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html.
# For more examples on how to use CMake, see https://github.com/android/ndk-samples.

# Sets the minimum CMake version required for this project.
cmake_minimum_required(VERSION 3.22.1)

# Declares the project name. The project name can be accessed via ${ PROJECT_NAME},
# Since this is the top level CMakeLists.txt, the project name is also accessible
# with ${CMAKE_PROJECT_NAME} (both CMake variables are in-sync within the top level
# build script scope).
project("llama-android")

# ===========================================================================
# Android Mobile Optimizations for llama.cpp
# ===========================================================================

# Android-specific optimizations
if (ANDROID)
    # Disable GGML_NATIVE to avoid TRY_RUN issues in cross-compilation
    # ARM NEON is enabled by default on Android arm64-v8a
    set(GGML_NATIVE OFF CACHE BOOL "Disable native feature detection for cross-compile" FORCE)

    # Disable Vulkan due to cross-compilation issues with shader build
    # CPU optimizations (NEON, KV Cache Q4, Flash Attention) are sufficient
    set(GGML_VULKAN OFF CACHE BOOL "Disable Vulkan for cross-compile compatibility" FORCE)

    if (ANDROID_ABI STREQUAL "arm64-v8a")
        message(STATUS "✅ Building for Android ARM64 (${ANDROID_ABI})")
        message(STATUS "✅ ARM NEON: enabled by default on arm64-v8a")
        message(STATUS "✅ CPU optimizations: enabled (KV Cache Q4, Flash Attention)")
        message(STATUS "⚠️  Vulkan: disabled (cross-compile compatibility)")
    else()
        message(STATUS "Building for Android ABI: ${ANDROID_ABI}")
    endif()
else()
    message(STATUS "Non-Android build detected")
    # Enable native optimizations for non-Android builds
    set(GGML_NATIVE ON CACHE BOOL "Enable native optimizations" FORCE)
endif()

# ===========================================================================

#include(FetchContent)
#FetchContent_Declare(
#        llama
#        GIT_REPOSITORY https://github.com/ggml-org/llama.cpp
#        GIT_TAG        master
#)

# Also provides "common"
#FetchContent_MakeAvailable(llama)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.
#
# In this top level CMakeLists.txt, ${CMAKE_PROJECT_NAME} is used to define
# the target library name; in the sub-module's CMakeLists.txt, ${PROJECT_NAME}
# is preferred for the same purpose.
#

#load local llama.cpp
add_subdirectory(../../../../llama.cpp build-llama)

# Build mtmd library for vision/multimodal support
add_library(mtmd STATIC
        mtmd/mtmd.cpp
        mtmd/clip.cpp
        mtmd/mtmd-helper.cpp
        mtmd/mtmd-audio.cpp
        )

target_link_libraries(mtmd PUBLIC ggml llama)
target_include_directories(mtmd PUBLIC mtmd)
target_include_directories(mtmd PRIVATE ../../../../llama.cpp)
target_include_directories(mtmd PRIVATE ../../../../llama.cpp/vendor)
target_compile_features(mtmd PRIVATE cxx_std_17)

# In order to load a library into your app from Java/Kotlin, you must call
# System.loadLibrary() and pass the name of the library defined here;
# for GameActivity/NativeActivity derived applications, the same library name must be
# used in the AndroidManifest.xml file.
add_library(${CMAKE_PROJECT_NAME} SHARED
        # List C/C++ source files with relative paths to this CMakeLists.txt.
        llama-android.cpp
        llama-android-vlm.cpp)

# Specifies libraries CMake should link to your target library. You
# can link libraries from various origins, such as libraries defined in this
# build script, prebuilt third-party libraries, or Android system libraries.
target_link_libraries(${CMAKE_PROJECT_NAME}
        # List libraries link to the target library
        llama
        common
        mtmd
        android
        jnigraphics
        log)
