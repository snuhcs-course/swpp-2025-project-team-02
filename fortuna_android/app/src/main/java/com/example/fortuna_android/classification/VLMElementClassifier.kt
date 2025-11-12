package com.example.fortuna_android.classification

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.fortuna_android.vlm.SmolVLMManager
import kotlinx.coroutines.flow.fold

/**
 * Result of VLM element classification
 */
data class VLMResult(
    val element: ElementMapper.Element?,
    val rawOutput: String
)

/**
 * Classifies detected objects into element categories using VLM
 */
class VLMElementClassifier(private val context: Context) {
    private val tag = "VLMElementClassifier"
    private val vlmManager = SmolVLMManager.getInstance(context)
    private var isInitialized = false

    companion object {
        // Prompt for element classification (descriptive format)
        // Matches the training format: "{reason}. The element is {element}."
        private const val ELEMENT_CLASSIFICATION_PROMPT = """Classify this image into one of these elements: water, land, fire, wood, metal.

Provide your answer with a brief description."""
    }

    /**
     * Initialize VLM model
     */
    suspend fun initialize() {
        if (!isInitialized) {
            Log.i(tag, "Initializing VLM for element classification...")
            vlmManager.initialize()
            isInitialized = true
            Log.i(tag, "VLM initialized successfully")
        }
    }

    /**
     * Classify a cropped object bitmap into an element category
     * Returns VLMResult containing both the Element enum and raw VLM output
     * Returns null if classification fails
     */
    suspend fun classifyElement(croppedBitmap: Bitmap): VLMResult? {
        if (!isInitialized) {
            Log.w(tag, "VLM not initialized, cannot classify")
            return null
        }

        return try {
            // Collect all tokens from the VLM stream
            val fullResponse = vlmManager.analyzeImage(croppedBitmap, ELEMENT_CLASSIFICATION_PROMPT)
                .fold("") { acc, token -> acc + token }
                .trim()

            Log.d(tag, "VLM raw response: '$fullResponse'")

            // Parse the response to extract element
            val element = parseElementFromResponse(fullResponse.lowercase())

            // Return both element and raw output
            VLMResult(element = element, rawOutput = fullResponse)
        } catch (e: Exception) {
            Log.e(tag, "Failed to classify element with VLM", e)
            null
        }
    }

    /**
     * Parse element from VLM response and map to Element enum
     * Expected VLM output format: "{reason}. The element is {element}."
     * Also handles fallback to simple element mentions
     * Maps "land" -> EARTH to match Element enum
     */
    private fun parseElementFromResponse(response: String): ElementMapper.Element? {
        // Map VLM output strings to Element enum
        val elementMap = mapOf(
            "water" to ElementMapper.Element.WATER,
            "land" to ElementMapper.Element.EARTH,  // VLM says "land", we use EARTH
            "fire" to ElementMapper.Element.FIRE,
            "wood" to ElementMapper.Element.WOOD,
            "metal" to ElementMapper.Element.METAL
        )

        // Try to extract element from "The element is {element}" pattern first
        val pattern = "The element is (\\w+)".toRegex(RegexOption.IGNORE_CASE)
        val match = pattern.find(response)
        if (match != null) {
            val elementWord = match.groupValues[1].lowercase()
            val element = elementMap[elementWord]
            if (element != null) {
                Log.i(tag, "Extracted element from pattern '$elementWord' -> ${element.displayName}")
                return element
            }
        }

        // Fallback: Try to find any valid element in the response
        for ((vlmLabel, element) in elementMap) {
            if (response.contains(vlmLabel)) {
                Log.i(tag, "Detected VLM label '$vlmLabel' (fallback) -> ${element.displayName}")
                return element
            }
        }

        Log.w(tag, "No valid element found in response: '$response'")
        return null
    }

    /**
     * Check if VLM is initialized
     */
    fun isReady(): Boolean = isInitialized
}
