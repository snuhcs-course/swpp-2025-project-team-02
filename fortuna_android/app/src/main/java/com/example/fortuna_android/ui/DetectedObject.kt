package com.example.fortuna_android.ui

import android.graphics.RectF
import com.example.fortuna_android.classification.ElementMapper
import java.util.UUID

/**
 * VLM processing state for detected objects
 */
enum class VLMProcessingState {
    PENDING,      // Not yet processed by VLM
    PROCESSING,   // VLM is currently analyzing
    COMPLETED,    // VLM analysis complete
    FAILED        // VLM analysis failed
}

/**
 * Represents a detected object with bounding box, VLM state, and results
 */
data class DetectedObject(
    val id: String = UUID.randomUUID().toString(),
    val boundingBox: RectF,
    val label: String,
    val confidence: Float,
    var vlmState: VLMProcessingState = VLMProcessingState.PENDING,
    var classifiedElement: ElementMapper.Element? = null,
    var rawVlmOutput: String? = null,
    var vlmError: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Check if this object can be clicked for details
     */
    fun isClickable(): Boolean = vlmState == VLMProcessingState.COMPLETED || vlmState == VLMProcessingState.FAILED

    /**
     * Check if VLM processing is in progress
     */
    fun isProcessing(): Boolean = vlmState == VLMProcessingState.PROCESSING

    /**
     * Get display text for the object
     */
    fun getDisplayText(): String {
        return when (vlmState) {
            VLMProcessingState.PENDING -> "$label (${(confidence * 100).toInt()}%)"
            VLMProcessingState.PROCESSING -> "Analyzing..."
            VLMProcessingState.COMPLETED -> classifiedElement?.displayName ?: "Unknown"
            VLMProcessingState.FAILED -> "Failed"
        }
    }
}
