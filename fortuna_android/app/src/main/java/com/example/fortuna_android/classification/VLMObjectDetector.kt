package com.example.fortuna_android.classification

import android.app.Activity
import android.graphics.Bitmap
import android.media.Image
import com.example.fortuna_android.classification.utils.ImageUtils
import com.example.fortuna_android.vlm.SmolVLMManager
import kotlinx.coroutines.flow.fold

/**
 * Hybrid object detector that uses ML Kit for bounding boxes and VLM for classification
 *
 * Architecture:
 * 1. ML Kit detects objects and provides bounding boxes (coordinates + dimensions)
 * 2. VLM analyzes the image and classifies the detected object
 * 3. Combine ML Kit's spatial data with VLM's classification
 */
class VLMObjectDetector(
    context: Activity,
    private val vlmManager: SmolVLMManager
) : ObjectDetector(context) {

    // Use ML Kit for fast and accurate bounding box detection
    private val mlKitDetector = MLKitObjectDetector(
        context = context,
        maxDetectedObjects = 1  // Only detect 1 object at a time
    )

    // VLM prompt matching training data format
    private val VLM_ELEMENT_PROMPT = "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:"

    override suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult> {
        // Step 1: Get bounding box from ML Kit
        val mlKitResults = mlKitDetector.analyze(image, imageRotation)

        if (mlKitResults.isEmpty()) {
            return emptyList()
        }

        // Get the first (and only) detection from ML Kit
        val detection = mlKitResults[0]

        // Step 2: Convert camera image to Bitmap for VLM
        val bitmap = convertYuv(image)
        val rotatedBitmap = ImageUtils.rotateBitmap(bitmap, imageRotation)

        // Step 3: Get classification from VLM
        val vlmClassification = classifyWithVLM(rotatedBitmap)

        // Step 4: Combine ML Kit's bounding box with VLM's classification
        return listOf(
            DetectedObjectResult(
                confidence = detection.confidence,  // Use ML Kit's confidence for detection
                label = vlmClassification,          // Use VLM's classification as label
                centerCoordinate = detection.centerCoordinate,
                width = detection.width,
                height = detection.height
            )
        )
    }

    /**
     * Classify object using VLM
     * Returns element name: Wood, Fire, Earth, Metal, or Water
     */
    private suspend fun classifyWithVLM(bitmap: Bitmap): String {
        return try {
            // Collect all tokens from VLM streaming response
            val result = StringBuilder()
            vlmManager.analyzeImage(bitmap, VLM_ELEMENT_PROMPT)
                .fold("") { acc, token -> acc + token }
                .trim()
                .let { response ->
                    // Extract element name from VLM response
                    parseElementFromResponse(response)
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
