package com.example.fortuna_android.classification

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.example.fortuna_android.classification.utils.VLM_CLASSIFICATION_PROMPT
import com.example.fortuna_android.common.helpers.ImageUtils
import com.example.fortuna_android.vlm.SmolVLMManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

/**
 * VLM-based object classifier using batch inference for parallel processing.
 *
 * Uses SmolVLM to classify cropped objects into Chinese Five Elements categories.
 * Supports batch processing of up to 3 objects simultaneously using llama.cpp's
 * batch inference (nSeqMax=3).
 */
class VLMClassifier(private val context: Context) {
    private val tag = "VLMClassifier"
    private val vlmManager = SmolVLMManager.getInstance(context)
    private val elementMapper = ElementMapper(context)

    companion object {
        private const val CROP_SIZE = 96  // 96x96 for optimal speed/quality balance
        private const val MAX_TOKENS = 5  // Take only first 5 tokens for speed
    }

    /**
     * Result of VLM classification for a single object.
     */
    data class VLMClassificationResult(
        val element: ElementMapper.Element,
        val rawResponse: String,
        val confidence: Float = 1.0f,  // VLM doesn't provide confidence, default to 1.0
        val inferenceTimeMs: Long
    )

    /**
     * Classify multiple objects using batch VLM inference.
     *
     * Processes up to 3 objects in parallel using llama.cpp batch processing.
     * Each object is cropped and downscaled to 96x96 for fast inference.
     *
     * @param fullImage Full camera image
     * @param detectedObjects List of detected objects with bounding boxes
     * @return List of classification results in same order as input
     */
    suspend fun classifyBatch(
        fullImage: Bitmap,
        detectedObjects: List<ExtendedDetectedObjectResult>
    ): List<VLMClassificationResult> = withContext(Dispatchers.IO) {

        if (detectedObjects.isEmpty()) {
            Log.w(tag, "No objects to classify")
            return@withContext emptyList()
        }

        if (!vlmManager.isLoaded()) {
            Log.e(tag, "VLM not loaded, falling back to ML Kit")
            return@withContext detectedObjects.map { fallbackToMLKit(it) }
        }

        val startTime = System.currentTimeMillis()

        try {
            // Crop all objects to 96x96
            val croppedBitmaps = detectedObjects.map { obj ->
                ImageUtils.cropObjectForVLM(fullImage, obj.boundingBox, CROP_SIZE)
            }

            Log.i(tag, "Classifying ${detectedObjects.size} objects with VLM (sequential)")

            // Process objects sequentially to avoid context conflicts
            // VLM uses single-threaded context, parallel calls cause eval_chunks failure
            val results = croppedBitmaps.mapIndexed { index, croppedBitmap ->
                classifySingle(croppedBitmap, detectedObjects[index], index)
            }

            val totalTime = System.currentTimeMillis() - startTime
            Log.i(tag, "Sequential classification completed in ${totalTime}ms (${totalTime / detectedObjects.size}ms per object)")

            // Clean up bitmaps
            croppedBitmaps.forEach { it.recycle() }

            results

        } catch (e: Exception) {
            Log.e(tag, "VLM batch classification failed", e)
            // Fallback to ML Kit
            detectedObjects.map { fallbackToMLKit(it) }
        }
    }

    /**
     * Classify a single cropped object.
     *
     * @param croppedBitmap 96x96 cropped object image
     * @param originalObject Original detection result for fallback
     * @param objectIndex Index for logging
     */
    private suspend fun classifySingle(
        croppedBitmap: Bitmap,
        originalObject: ExtendedDetectedObjectResult,
        objectIndex: Int
    ): VLMClassificationResult {
        val startTime = System.currentTimeMillis()

        try {
            // VLM inference with ultra-short prompt
            // Take only first 5 tokens and cancel to save time
            val responseBuilder = StringBuilder()

            vlmManager.analyzeImage(croppedBitmap, VLM_CLASSIFICATION_PROMPT)
                .take(MAX_TOKENS)  // Cancel after 5 tokens
                .toList()
                .forEach { responseBuilder.append(it) }

            val rawResponse = responseBuilder.toString().trim()
            val inferenceTime = System.currentTimeMillis() - startTime

            Log.d(tag, "Object $objectIndex VLM response (${inferenceTime}ms): '$rawResponse'")

            // Parse response to Element
            val element = parseVLMResponse(rawResponse, originalObject.label)

            return VLMClassificationResult(
                element = element,
                rawResponse = rawResponse,
                confidence = 1.0f,
                inferenceTimeMs = inferenceTime
            )

        } catch (e: Exception) {
            Log.e(tag, "VLM classification failed for object $objectIndex", e)
            return fallbackToMLKit(originalObject)
        }
    }

    /**
     * Parse VLM response to Element enum.
     *
     * Expected responses: "wood", "fire", "earth", "metal", "water"
     * Handles variations like "Fire!", "I see wood", etc.
     *
     * @param rawResponse Raw VLM output
     * @param mlKitLabel ML Kit label for fallback
     * @return Parsed Element
     */
    private fun parseVLMResponse(rawResponse: String, mlKitLabel: String): ElementMapper.Element {
        val normalized = rawResponse.lowercase().trim()

        // Direct keyword matching
        val element = when {
            normalized.contains("wood") -> ElementMapper.Element.WOOD
            normalized.contains("fire") -> ElementMapper.Element.FIRE
            normalized.contains("earth") || normalized.contains("ground") -> ElementMapper.Element.EARTH
            normalized.contains("metal") -> ElementMapper.Element.METAL
            normalized.contains("water") -> ElementMapper.Element.WATER
            else -> {
                Log.w(tag, "VLM response unrecognized: '$rawResponse', using ML Kit fallback")
                // Fallback to ML Kit label mapping
                elementMapper.mapLabelToElement(mlKitLabel)
            }
        }

        return element
    }

    /**
     * Fallback to ML Kit label mapping when VLM fails.
     */
    private fun fallbackToMLKit(obj: ExtendedDetectedObjectResult): VLMClassificationResult {
        val element = elementMapper.mapLabelToElement(obj.label)
        return VLMClassificationResult(
            element = element,
            rawResponse = "ML Kit fallback: ${obj.label}",
            confidence = obj.confidence,
            inferenceTimeMs = 0
        )
    }

    /**
     * Check if VLM is ready for classification.
     */
    fun isReady(): Boolean = vlmManager.isLoaded()
}
