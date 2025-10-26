#include <android/log.h>
#include <android/bitmap.h>
#include <jni.h>
#include <string>
#include <vector>
#include "llama.h"
#include "common.h"
#include "mtmd/mtmd.h"
#include "mtmd/mtmd-helper.h"

#define TAG "llama-android-vlm.cpp"
#define LOGi(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGe(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// Load mmproj (multimodal projector) model
extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_load_1mmproj(
        JNIEnv *env,
        jobject,
        jstring mmproj_path,
        jlong model_ptr) {

    const char *path = env->GetStringUTFChars(mmproj_path, nullptr);
    auto *text_model = reinterpret_cast<llama_model *>(model_ptr);

    LOGi("Loading mmproj from %s", path);

    struct mtmd_context_params params = mtmd_context_params_default();
    params.use_gpu = true;
    // Optimize thread count for vision encoder: use half of cores (P-cores)
    int total_cores = (int) sysconf(_SC_NPROCESSORS_ONLN);
    params.n_threads = std::max(2, std::min(4, total_cores / 2));
    params.verbosity = GGML_LOG_LEVEL_ERROR;

    LOGi("🚀 GPU acceleration: use_gpu=%d, cores=%d, threads=%d (P-cores optimized)", params.use_gpu, total_cores, params.n_threads);
    LOGi("📱 Device will use best available backend (GPU -> CPU fallback)");

    mtmd_context *ctx = mtmd_init_from_file(path, text_model, params);

    env->ReleaseStringUTFChars(mmproj_path, path);

    if (!ctx) {
        LOGe("❌ Failed to load mmproj");
        env->ThrowNew(env->FindClass("java/lang/IllegalStateException"), "Failed to load mmproj");
        return 0;
    }

    LOGi("✅ Mmproj loaded successfully - check above logs for backend type");
    return reinterpret_cast<jlong>(ctx);
}

// Free mmproj context
extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_free_1mmproj(JNIEnv *, jobject, jlong ctx_ptr) {
    auto *ctx = reinterpret_cast<mtmd_context *>(ctx_ptr);
    if (ctx) {
        mtmd_free(ctx);
    }
}

// Create bitmap from Android Bitmap
extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_bitmap_1from_1android(
        JNIEnv *env,
        jobject,
        jobject bitmap) {

    // Get bitmap info
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGe("Failed to get bitmap info");
        return 0;
    }

    // Lock bitmap pixels
    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGe("Failed to lock bitmap pixels");
        return 0;
    }

    // Convert RGBA to RGB
    uint32_t width = info.width;
    uint32_t height = info.height;
    std::vector<uint8_t> rgb_data(width * height * 3);

    uint32_t *rgba_pixels = static_cast<uint32_t *>(pixels);
    for (uint32_t y = 0; y < height; y++) {
        for (uint32_t x = 0; x < width; x++) {
            uint32_t idx = y * width + x;
            uint32_t rgba = rgba_pixels[idx];

            rgb_data[idx * 3 + 0] = (rgba >> 16) & 0xFF; // R
            rgb_data[idx * 3 + 1] = (rgba >> 8) & 0xFF;  // G
            rgb_data[idx * 3 + 2] = rgba & 0xFF;         // B
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    // Create mtmd bitmap
    mtmd_bitmap *mtmd_bmp = mtmd_bitmap_init(width, height, rgb_data.data());
    if (!mtmd_bmp) {
        LOGe("Failed to create mtmd bitmap");
        return 0;
    }

    LOGi("Bitmap created: %dx%d", width, height);
    return reinterpret_cast<jlong>(mtmd_bmp);
}

// Free bitmap
extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_bitmap_1free(JNIEnv *, jobject, jlong bitmap_ptr) {
    auto *bitmap = reinterpret_cast<mtmd_bitmap *>(bitmap_ptr);
    if (bitmap) {
        mtmd_bitmap_free(bitmap);
    }
}

// Tokenize text + image
extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_tokenize_1with_1image(
        JNIEnv *env,
        jobject,
        jlong mtmd_ctx_ptr,
        jstring prompt,
        jlong bitmap_ptr) {

    auto *mtmd_ctx = reinterpret_cast<mtmd_context *>(mtmd_ctx_ptr);
    auto *bitmap = reinterpret_cast<mtmd_bitmap *>(bitmap_ptr);

    if (!mtmd_ctx || !bitmap) {
        LOGe("Invalid mtmd context or bitmap");
        return 0;
    }

    const char *prompt_str = env->GetStringUTFChars(prompt, nullptr);

    mtmd_input_text input_text;
    input_text.text = prompt_str;
    input_text.add_special = true;
    input_text.parse_special = true;

    mtmd_input_chunks *chunks = mtmd_input_chunks_init();
    const mtmd_bitmap *bitmaps[] = {bitmap};

    int32_t ret = mtmd_tokenize(mtmd_ctx, chunks, &input_text, bitmaps, 1);

    env->ReleaseStringUTFChars(prompt, prompt_str);

    if (ret != 0) {
        LOGe("mtmd_tokenize failed with code %d", ret);
        mtmd_input_chunks_free(chunks);
        return 0;
    }

    size_t n_chunks = mtmd_input_chunks_size(chunks);
    LOGi("Tokenized into %zu chunks", n_chunks);

    return reinterpret_cast<jlong>(chunks);
}

// Free input chunks
extern "C"
JNIEXPORT void JNICALL
Java_android_llama_cpp_LLamaAndroid_chunks_1free(JNIEnv *, jobject, jlong chunks_ptr) {
    auto *chunks = reinterpret_cast<mtmd_input_chunks *>(chunks_ptr);
    if (chunks) {
        mtmd_input_chunks_free(chunks);
    }
}

// Evaluate chunks using mtmd_helper
// This handles both text and image chunks correctly
extern "C"
JNIEXPORT jlong JNICALL
Java_android_llama_cpp_LLamaAndroid_eval_1chunks(
        JNIEnv *,
        jobject,
        jlong mtmd_ctx_ptr,
        jlong llama_ctx_ptr,
        jlong chunks_ptr,
        jint n_past,
        jint n_batch) {

    auto *mtmd_ctx = reinterpret_cast<mtmd_context *>(mtmd_ctx_ptr);
    auto *llama_ctx = reinterpret_cast<llama_context *>(llama_ctx_ptr);
    auto *chunks = reinterpret_cast<mtmd_input_chunks *>(chunks_ptr);

    if (!mtmd_ctx || !llama_ctx || !chunks) {
        LOGe("eval_chunks: Invalid pointers");
        return -1;
    }

    llama_pos new_n_past = 0;

    // Use mtmd_helper to evaluate all chunks (text + image)
    int32_t ret = mtmd_helper_eval_chunks(
        mtmd_ctx,
        llama_ctx,
        chunks,
        n_past,          // n_past
        0,               // seq_id
        n_batch,         // n_batch
        true,            // logits_last
        &new_n_past      // output: new position
    );

    if (ret != 0) {
        LOGe("mtmd_helper_eval_chunks failed with code %d", ret);
        return -1;
    }

    LOGi("Chunks evaluated successfully, new n_past: %d", (int)new_n_past);
    return static_cast<jlong>(new_n_past);
}
