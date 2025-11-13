package com.example.fortuna_android.classification

import android.app.Activity
import android.graphics.Bitmap
import android.media.Image
import com.example.fortuna_android.classification.utils.ImageUtils
import com.example.fortuna_android.vlm.SmolVLMManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.fold
import kotlinx.coroutines.launch

/**
 * Hybrid object detector that uses ML Kit for bounding boxes and VLM for classification
 *
 * Progressive detection flow:
 * 1. ML Kit detects objects instantly → returns bounding box with "Analyzing..." label
 * 2. VLM classifies in background → updates label via callback
 */
class VLMObjectDetector(
    context: Activity,
    private val vlmManager: SmolVLMManager,
    private val onVLMClassified: ((DetectedObjectResult) -> Unit)? = null
) : ObjectDetector(context) {

    // Use ML Kit for fast and accurate bounding box detection
    private val mlKitDetector = MLKitObjectDetector(
        context = context,
        maxDetectedObjects = 1  // Only detect 1 object at a time
    )

    // VLM prompt matching training data format
    private val VLM_ELEMENT_PROMPT = "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:"

    override suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult> {
        // Step 1: Get bounding box from ML Kit (fast)
        val mlKitResults = mlKitDetector.analyze(image, imageRotation)

        if (mlKitResults.isEmpty()) {
            return emptyList()
        }

        // Get the first (and only) detection from ML Kit
        val detection = mlKitResults[0]

        // Step 2: Return ML Kit result immediately with ML Kit label + "Analyzing..." indicator
        val preliminaryResult = DetectedObjectResult(
            confidence = detection.confidence,
            label = "Analyzing ${detection.label} ...",  // Show ML Kit prediction while VLM processes
            centerCoordinate = detection.centerCoordinate,
            width = detection.width,
            height = detection.height
        )

        // Step 3: Run VLM classification in background
        val bitmap = convertYuv(image)
        val rotatedBitmap = ImageUtils.rotateBitmap(bitmap, imageRotation)

        // Start VLM classification asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val vlmClassification = classifyWithVLM(rotatedBitmap)

                // Update result with VLM classification
                val finalResult = preliminaryResult.copy(label = vlmClassification)
                onVLMClassified?.invoke(finalResult)
            } catch (e: Exception) {
                // If VLM completely fails, update with error state
                android.util.Log.e("VLMObjectDetector", "VLM classification failed in coroutine", e)
                val errorResult = preliminaryResult.copy(label = "Detection Failed")
                onVLMClassified?.invoke(errorResult)
            }
        }

        // Return preliminary result immediately (box appears right away)
        return listOf(preliminaryResult)
    }

    /**
     * Classify object using VLM
     * Returns element name: Wood, Fire, Earth, Metal, or Water
     */
    private suspend fun classifyWithVLM(bitmap: Bitmap): String {
        return try {
            val startTime = System.currentTimeMillis()

            // Collect all tokens from VLM streaming response with proper termination handling
            val response = try {
                vlmManager.analyzeImage(bitmap, VLM_ELEMENT_PROMPT)
                    .fold("") { acc, token -> acc + token }
                    .trim()
            } catch (e: Exception) {
                android.util.Log.w("VLMObjectDetector", "VLM streaming failed, using fallback", e)
                "Unknown" // Return fallback if streaming fails
            }

            response.let { responseText ->
                    val elapsedTime = System.currentTimeMillis() - startTime

                    // Log VLM raw output and inference time
                    android.util.Log.i("VLMObjectDetector", "⚡ VLM Response (${elapsedTime}ms): \"$response\"")

                    // Extract element name from VLM response
                    val element = parseElementFromResponse(response)
                    android.util.Log.i("VLMObjectDetector", "🎯 Parsed Element: \"$element\" (from \"$response\")")
                    element
                }
        } catch (e: Exception) {
            android.util.Log.e("VLMObjectDetector", "VLM classification failed", e)
            "Unknown"  // Fallback label
        }
    }

    /**
     * Parse element name from VLM response
     * VLM returns: water, land, fire, wood, metal (lowercase)
     * Map to app format: Water, Earth, Fire, Wood, Metal (capitalized, land->Earth)
     */
    private fun parseElementFromResponse(response: String): String {
        val elementMap = mapOf(
            "water" to "Water",
            "land" to "Earth",  // Map land -> Earth for app compatibility
            "fire" to "Fire",
            "wood" to "Wood",
            "metal" to "Metal"
        )

        // Try to find exact match (case-insensitive)
        elementMap.forEach { (vlmName, appName) ->
            if (response.contains(vlmName, ignoreCase = true)) {
                return appName
            }
        }

        // If no valid element found, return the raw response for debugging
        android.util.Log.w("VLMObjectDetector", "Could not parse element from VLM response: $response")
        return response.trim().take(20)  // Return first 20 chars for debugging
    }
}
