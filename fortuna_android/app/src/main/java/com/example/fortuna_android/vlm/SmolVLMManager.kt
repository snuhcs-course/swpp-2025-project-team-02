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
    private var isMmprojLoaded = false
    private var modelPath: String? = null
    private var mmprojPath: String? = null

    companion object {
        private const val MODEL_FILENAME = "SmolVLM2-500M-Video-Instruct-Q8_0.gguf"
        private const val MMPROJ_FILENAME = "mmproj-SmolVLM2-500M-Video-Instruct-Q8_0.gguf"
        private const val MODELS_DIR = "models"
        private const val IMAGE_MARKER = "<__media__>"

        // Target image size for VLM processing (384x384 is optimal for mobile VLM)
        private const val TARGET_IMAGE_SIZE = 384

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

        // Log original image size
        Log.i(tag, "📸 Original image size: ${bitmap.width}x${bitmap.height}")

        // Resize image for optimal VLM performance (384x384 is optimal for mobile)
        val resizedBitmap = resizeImageForVLM(bitmap, TARGET_IMAGE_SIZE)
        Log.i(tag, "📸 Resized image size: ${resizedBitmap.width}x${resizedBitmap.height}")

        val fullPrompt = buildVisionPrompt(prompt)
        return llamaAndroid.sendWithImage(fullPrompt, resizedBitmap)
    }

    /**
     * Build a prompt in the format expected by SmolVLM
     * The IMAGE_MARKER will be replaced with actual image embeddings by mtmd
     */
    private fun buildVisionPrompt(userPrompt: String): String {
        // SmolVLM format: image marker followed by the text prompt
        // The mtmd library will replace IMAGE_MARKER with actual image tokens
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
     * Resize image to target size while maintaining aspect ratio
     * Adds padding if needed to maintain square dimensions
     */
    private fun resizeImageForVLM(bitmap: Bitmap, targetSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // If already at target size, return as-is
        if (width == targetSize && height == targetSize) {
            return bitmap
        }

        // Calculate scaling factor to fit within target size while maintaining aspect ratio
        val scaleFactor = minOf(
            targetSize.toFloat() / width,
            targetSize.toFloat() / height
        )

        val scaledWidth = (width * scaleFactor).toInt()
        val scaledHeight = (height * scaleFactor).toInt()

        // First, scale the image
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)

        // If not square, create a square canvas and center the image
        if (scaledWidth != targetSize || scaledHeight != targetSize) {
            val paddedBitmap = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(paddedBitmap)

            // Fill with black background
            canvas.drawColor(android.graphics.Color.BLACK)

            // Center the scaled image
            val left = (targetSize - scaledWidth) / 2f
            val top = (targetSize - scaledHeight) / 2f
            canvas.drawBitmap(scaledBitmap, left, top, null)

            // Recycle the intermediate bitmap
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }

            return paddedBitmap
        }

        return scaledBitmap
    }

    /**
     * Check if model is loaded
     */
    fun isLoaded(): Boolean = isModelLoaded
}
