package com.example.fortuna_android.classification

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.fortuna_android.vlm.SmolVLMManager
import kotlinx.coroutines.flow.fold

/**
 * Classifies detected objects into element categories using VLM
 */
class VLMElementClassifier(private val context: Context) {
    private val tag = "VLMElementClassifier"
    private val vlmManager = SmolVLMManager.getInstance(context)
    private var isInitialized = false

    companion object {
        // Prompt for element classification
        private const val ELEMENT_CLASSIFICATION_PROMPT = """Classify this image into one of these elements: water, land, fire, wood, metal.

Element:"""
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
     * Returns the Element enum or null if classification fails
     */
    suspend fun classifyElement(croppedBitmap: Bitmap): ElementMapper.Element? {
        if (!isInitialized) {
            Log.w(tag, "VLM not initialized, cannot classify")
            return null
        }

        return try {
            // Collect all tokens from the VLM stream
            val fullResponse = vlmManager.analyzeImage(croppedBitmap, ELEMENT_CLASSIFICATION_PROMPT)
                .fold("") { acc, token -> acc + token }
                .trim()
                .lowercase()

            Log.d(tag, "VLM raw response: '$fullResponse'")

            // Parse the response to extract element
            parseElementFromResponse(fullResponse)
        } catch (e: Exception) {
            Log.e(tag, "Failed to classify element with VLM", e)
            null
        }
    }

    /**
     * Parse element from VLM response and map to Element enum
     * Expected VLM output: "water", "land", "fire", "wood", "metal"
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

        // Try to find any valid element in the response
        for ((vlmLabel, element) in elementMap) {
            if (response.contains(vlmLabel)) {
                Log.i(tag, "Detected VLM label '$vlmLabel' -> ${element.displayName}")
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
