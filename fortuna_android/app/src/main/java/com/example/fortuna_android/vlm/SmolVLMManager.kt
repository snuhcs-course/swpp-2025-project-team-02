package com.example.fortuna_android.vlm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.util.Log
import com.example.fortuna_android.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Manager class for InternVL3 vision-language model
 * Wraps llama.cpp Android library with vision capabilities
 */
class SmolVLMManager(private val context: Context) {
    private val tag = "SmolVLMManager"
    private val llamaAndroid = LLamaAndroid.instance()
    private var isModelLoaded = false
    private var isMmprojLoaded = false
    private var modelPath: String? = null
    private var mmprojPath: String? = null

    companion object {
        // Model filenames from BuildConfig (Single Source of Truth)
        private val MODEL_FILENAME = BuildConfig.VLM_MODEL_FILENAME
        private val MMPROJ_FILENAME = BuildConfig.VLM_MMPROJ_FILENAME

        private const val MODELS_DIR = "models"
        // llama.cpp mtmd uses <__media__> as the universal media placeholder
        // This works for all VLM models (SmolVLM, InternVL, etc.)
        // The mtmd library internally converts this to model-specific tokens
        private const val IMAGE_MARKER = "<__media__>"

        @Volatile
        private var instance: SmolVLMManager? = null

        fun getInstance(context: Context): SmolVLMManager {
            return instance ?: synchronized(this) {
                instance ?: SmolVLMManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Initialize and load the SmolVLM model with vision support
     * Downloads model and mmproj from assets if not already present
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isModelLoaded && isMmprojLoaded) {
            Log.i(tag, "Model and mmproj already loaded")
            return@withContext
        }

        try {
            // Ensure model is copied to internal storage
            if (!isModelLoaded) {
                modelPath = ensureModelExists()
                Log.i(tag, "Loading model from: $modelPath")
                llamaAndroid.load(modelPath!!)
                isModelLoaded = true
                Log.i(tag, "Model loaded successfully")
            }

            // Ensure mmproj is copied and load it
            if (!isMmprojLoaded) {
                mmprojPath = ensureMmprojExists()
                Log.i(tag, "Loading mmproj from: $mmprojPath")
                llamaAndroid.loadMmproj(mmprojPath!!)
                isMmprojLoaded = true
                Log.i(tag, "Mmproj loaded successfully - Vision support enabled")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to initialize model", e)
            throw e
        }
    }

    /**
     * Copy model from assets to internal storage if not already present
     */
    private fun ensureModelExists(): String {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        val modelFile = File(modelsDir, MODEL_FILENAME)

        if (!modelFile.exists()) {
            Log.i(tag, "Model not found in internal storage, copying from assets...")
            try {
                context.assets.open("$MODELS_DIR/$MODEL_FILENAME").use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(tag, "Model copied successfully to ${modelFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(tag, "Model not found in assets and not in internal storage", e)
                throw IllegalStateException(
                    "Model file not found. Please download $MODEL_FILENAME and place it in assets/$MODELS_DIR/",
                    e
                )
            }
        } else {
            Log.i(tag, "Model found at ${modelFile.absolutePath}")
        }

        return modelFile.absolutePath
    }

    /**
     * Copy mmproj from assets to internal storage if not already present
     */
    private fun ensureMmprojExists(): String {
        val modelsDir = File(context.filesDir, MODELS_DIR)
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
        }

        val mmprojFile = File(modelsDir, MMPROJ_FILENAME)

        if (!mmprojFile.exists()) {
            Log.i(tag, "Mmproj not found in internal storage, copying from assets...")
            try {
                context.assets.open("$MODELS_DIR/$MMPROJ_FILENAME").use { input ->
                    FileOutputStream(mmprojFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.i(tag, "Mmproj copied successfully to ${mmprojFile.absolutePath}")
            } catch (e: Exception) {
                Log.e(tag, "Mmproj not found in assets and not in internal storage", e)
                throw IllegalStateException(
                    "Mmproj file not found. Please download $MMPROJ_FILENAME and place it in assets/$MODELS_DIR/",
                    e
                )
            }
        } else {
            Log.i(tag, "Mmproj found at ${mmprojFile.absolutePath}")
        }

        return mmprojFile.absolutePath
    }

    /**
     * Analyze an image with a text prompt
     * @param imageUri URI of the image to analyze
     * @param prompt Text prompt/question about the image
     * @return Flow of generated text tokens
     */
    fun analyzeImage(imageUri: Uri, prompt: String): Flow<String> {
        if (!isModelLoaded || !isMmprojLoaded) {
            throw IllegalStateException("Model not loaded. Call initialize() first.")
        }

        val bitmap = loadImageFromUri(imageUri)
            ?: throw IllegalArgumentException("Failed to load image from URI")

        return analyzeImage(bitmap, prompt)
    }

    /**
     * Analyze an image from a Bitmap
     * @param bitmap Image to analyze
     * @param prompt Text prompt/question about the image
     * @return Flow of generated text tokens
     */
    fun analyzeImage(bitmap: Bitmap, prompt: String): Flow<String> {
        if (!isModelLoaded || !isMmprojLoaded) {
            throw IllegalStateException("Model not loaded. Call initialize() first.")
        }

        val fullPrompt = buildVisionPrompt(prompt)
        return llamaAndroid.sendWithImage(fullPrompt, bitmap)
    }

    /**
     * Build a prompt in the format expected by llama.cpp mtmd
     * The IMAGE_MARKER will be replaced with actual image embeddings by llama.cpp
     */
    private fun buildVisionPrompt(userPrompt: String): String {
        // llama.cpp mtmd format: <__media__> placeholder for any media (image/audio)
        // The mtmd library will replace this with model-specific image tokens
        return "$IMAGE_MARKER\n$userPrompt"
    }

    /**
     * Generate text from a prompt (text-only mode)
     */
    fun generateText(prompt: String): Flow<String> {
        if (!isModelLoaded) {
            throw IllegalStateException("Model not loaded. Call initialize() first.")
        }
        return llamaAndroid.send(prompt, formatChat = false)
    }

    /**
     * Run benchmark on the model
     */
    suspend fun benchmark(pp: Int = 512, tg: Int = 128, pl: Int = 1, nr: Int = 1): String {
        if (!isModelLoaded) {
            throw IllegalStateException("Model not loaded. Call initialize() first.")
        }
        return llamaAndroid.bench(pp, tg, pl, nr)
    }

    /**
     * Unload the model and free resources
     */
    suspend fun unload() {
        if (isModelLoaded) {
            llamaAndroid.unload()
            isModelLoaded = false
            isMmprojLoaded = false
            Log.i(tag, "Model and mmproj unloaded")
        }
    }

    /**
     * Load image from URI
     */
    private fun loadImageFromUri(uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to load image from URI", e)
            null
        }
    }

    /**
     * Check if model is loaded
     */
    fun isLoaded(): Boolean = isModelLoaded
}
