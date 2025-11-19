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
 * VLM-only object detector that crops a fixed center circle area
 *
 * Direct VLM detection flow:
 * 1. Crops center circle from camera preview
 * 2. Sends directly to VLM for classification
 * 3. Returns fixed center bounding box with VLM result
 */
class VLMObjectDetector(
    context: Activity,
    private val vlmManager: SmolVLMManager,
    private val onVLMClassified: ((DetectedObjectResult) -> Unit)? = null
) : ObjectDetector(context) {

    companion object {
        // Fixed center circle dimensions (as fraction of image size)
        private const val CENTER_CIRCLE_SIZE_RATIO = 0.5f // 50% of image width/height
    }

    // VLM prompt matching training data format
    private val VLM_ELEMENT_PROMPT = "Classify this image into one of these elements: water, land, fire, wood, metal.\n\nElement:"

    override suspend fun analyze(image: Image, imageRotation: Int): List<DetectedObjectResult> {
        // Convert camera image to bitmap - keep original for coordinate calculation
        val originalBitmap = convertYuv(image)

        // Use ORIGINAL camera image dimensions for anchor coordinates (like MLKit did)
        val originalWidth = originalBitmap.width
        val originalHeight = originalBitmap.height

        // Center coordinates in ORIGINAL camera image space (before rotation)
        // This is what ARCore expects for accurate coordinate transformation
        val centerX = originalWidth / 2
        val centerY = originalHeight / 2

        android.util.Log.d("VLMObjectDetector", "Original camera image: ${originalWidth}x${originalHeight}, center: ($centerX, $centerY), rotation: ${imageRotation}°")
        // Create square bounding box in original image coordinates
        val squareSize = (Math.min(originalWidth, originalHeight) * CENTER_CIRCLE_SIZE_RATIO).toInt()

        // Create fixed center bounding box result using ORIGINAL image coordinates
        val fixedCenterResult = DetectedObjectResult(
            confidence = 1.0f, // Fixed confidence since we're always analyzing center
            label = "Analyzing...",
            centerCoordinate = Pair(centerX, centerY), // Original image coordinates
            width = squareSize,
            height = squareSize
        )

        // For VLM processing, rotate the image and crop from rotated version
        val rotatedBitmap = ImageUtils.rotateBitmap(originalBitmap, imageRotation)
        val rotatedWidth = rotatedBitmap.width
        val rotatedHeight = rotatedBitmap.height
        val rotatedCenterX = rotatedWidth / 2
        val rotatedCenterY = rotatedHeight / 2
        val rotatedSquareSize = (Math.min(rotatedWidth, rotatedHeight) * CENTER_CIRCLE_SIZE_RATIO).toInt()

        // Calculate crop bounds in rotated image
        val cropLeft = Math.max(0, rotatedCenterX - rotatedSquareSize / 2)
        val cropTop = Math.max(0, rotatedCenterY - rotatedSquareSize / 2)
        val cropRight = Math.min(rotatedWidth, rotatedCenterX + rotatedSquareSize / 2)
        val cropBottom = Math.min(rotatedHeight, rotatedCenterY + rotatedSquareSize / 2)

        val cropWidth = cropRight - cropLeft
        val cropHeight = cropBottom - cropTop

        // Crop the center area from rotated image for VLM analysis
        val croppedBitmap = Bitmap.createBitmap(
            rotatedBitmap,
            cropLeft,
            cropTop,
            cropWidth,
            cropHeight
        )

        // Start VLM classification asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val vlmClassification = classifyWithVLM(croppedBitmap)

                // Update result with VLM classification
                val finalResult = fixedCenterResult.copy(label = vlmClassification)
                onVLMClassified?.invoke(finalResult)
            } catch (e: Exception) {
                // If VLM fails, update with error state
                android.util.Log.e("VLMObjectDetector", "VLM classification failed", e)
                val errorResult = fixedCenterResult.copy(label = "Detection Failed")
                onVLMClassified?.invoke(errorResult)
            }
        }

        // Return bounding box result immediately (box appears right away)
        return listOf(fixedCenterResult)
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
