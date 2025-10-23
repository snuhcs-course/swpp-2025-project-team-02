package com.example.fortuna_android.vlm

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.llama.cpp.LLamaAndroid
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * Manager class for SmolVLM vision-language model
 * Wraps llama.cpp Android library with vision capabilities
 */
class SmolVLMManager(private val context: Context) {
    private val tag = "SmolVLMManager"
    private val llamaAndroid = LLamaAndroid.instance()
    private var isModelLoaded = false
    private var modelPath: String? = null

    companion object {
        private const val MODEL_FILENAME = "SmolVLM-500M-Instruct-Q8_0.gguf"
        private const val MMPROJ_FILENAME = "mmproj-SmolVLM-500M-Instruct-Q8_0.gguf"
        private const val MODELS_DIR = "models"

        @Volatile
        private var instance: SmolVLMManager? = null

        fun getInstance(context: Context): SmolVLMManager {
            return instance ?: synchronized(this) {
                instance ?: SmolVLMManager(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Initialize and load the SmolVLM model
     * Downloads model from assets if not already present
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        if (isModelLoaded) {
            Log.i(tag, "Model already loaded")
            return@withContext
        }

        try {
            // Ensure model is copied to internal storage
            modelPath = ensureModelExists()

            Log.i(tag, "Loading model from: $modelPath")
            llamaAndroid.load(modelPath!!)
            isModelLoaded = true
            Log.i(tag, "Model loaded successfully")
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
     * Analyze an image with a text prompt
     * @param imageUri URI of the image to analyze
     * @param prompt Text prompt/question about the image
     * @return Flow of generated text tokens
     */
    fun analyzeImage(imageUri: Uri, prompt: String): Flow<String> {
        if (!isModelLoaded) {
            throw IllegalStateException("Model not loaded. Call initialize() first.")
        }

        // For now, use text-only mode
        // TODO: Extend native code to support image inputs via llava_image_embed
        val fullPrompt = buildVisionPrompt(prompt)
        return llamaAndroid.send(fullPrompt, formatChat = false)
    }

    /**
     * Analyze an image from a Bitmap
     */
    fun analyzeImage(bitmap: Bitmap, prompt: String): Flow<String> {
        if (!isModelLoaded) {
            throw IllegalStateException("Model not loaded. Call initialize() first.")
        }

        // TODO: Process bitmap and convert to format expected by SmolVLM
        val fullPrompt = buildVisionPrompt(prompt)
        return llamaAndroid.send(fullPrompt, formatChat = false)
    }

    /**
     * Build a prompt in the format expected by SmolVLM
     */
    private fun buildVisionPrompt(userPrompt: String): String {
        // SmolVLM uses a specific format for vision tasks
        // Format: <|image|>\n<|prompt|>Question: {question}\nAnswer:
        return """
            <|image|>
            <|prompt|>Question: $userPrompt
            Answer:
        """.trimIndent()
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
            Log.i(tag, "Model unloaded")
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
