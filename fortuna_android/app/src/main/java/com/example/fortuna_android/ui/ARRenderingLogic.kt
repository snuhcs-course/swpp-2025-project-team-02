package com.example.fortuna_android.ui

import android.opengl.Matrix
import android.util.Log
import com.example.fortuna_android.classification.DetectedObjectResult
import com.example.fortuna_android.classification.ElementMapper
import com.google.ar.core.TrackingState

/**
 * Extracted business logic from ARRenderer.onDrawFrame for testability.
 * This class contains the core rendering logic separated from ARCore/OpenGL dependencies.
 */
class ARRenderingLogic {

    companion object {
        private const val TAG = "ARRenderingLogic"
    }

    /**
     * Data class representing frame state for rendering
     */
    data class FrameState(
        val isSessionAvailable: Boolean = false,
        val isTrackingActive: Boolean = false,
        val hasValidFrame: Boolean = false,
        val pendingTap: Pair<Float, Float>? = null,
        val frameCount: Int = 0,
        val detectedObjects: List<DetectedObjectResult> = emptyList(),
        val neededElement: ElementMapper.Element? = null,
        val collectedCount: Int = 0
    )

    /**
     * Data class representing rendering decisions
     */
    data class RenderingDecision(
        val shouldEarlyReturn: Boolean = false,
        val shouldProcessTap: Boolean = false,
        val shouldPerformObjectDetection: Boolean = false,
        val shouldAnalyzeWithVLM: Boolean = false,
        val shouldRenderObjects: Boolean = false,
        val earlyReturnReason: String? = null
    )

    /**
     * Core rendering logic - testable without ARCore dependencies
     */
    fun determineRenderingActions(frameState: FrameState): RenderingDecision {
        // Early return conditions (these are the testable business rules)
        if (!frameState.isSessionAvailable) {
            return RenderingDecision(
                shouldEarlyReturn = true,
                earlyReturnReason = "Session not available"
            )
        }

        if (!frameState.hasValidFrame) {
            return RenderingDecision(
                shouldEarlyReturn = true,
                earlyReturnReason = "Frame not valid"
            )
        }

        if (!frameState.isTrackingActive) {
            return RenderingDecision(
                shouldEarlyReturn = true,
                earlyReturnReason = "Tracking not active"
            )
        }

        // Determine what actions to take
        val shouldProcessTap = frameState.pendingTap != null
        val shouldPerformObjectDetection = frameState.frameCount % 30 == 0 // Every 30 frames
        val shouldAnalyzeWithVLM = shouldPerformObjectDetection && frameState.frameCount % 60 == 0 // Every 60 frames
        val shouldRenderObjects = frameState.detectedObjects.isNotEmpty()

        return RenderingDecision(
            shouldEarlyReturn = false,
            shouldProcessTap = shouldProcessTap,
            shouldPerformObjectDetection = shouldPerformObjectDetection,
            shouldAnalyzeWithVLM = shouldAnalyzeWithVLM,
            shouldRenderObjects = shouldRenderObjects
        )
    }

    /**
     * Process tap coordinates and determine if it hits any objects
     */
    fun processTapLogic(
        tapX: Float,
        tapY: Float,
        detectedObjects: List<DetectedObjectResult>,
        screenWidth: Float,
        screenHeight: Float
    ): TapResult {
        if (detectedObjects.isEmpty()) {
            return TapResult(false, null, "No objects detected")
        }

        // Check if tap is within any detected object bounds
        for (obj in detectedObjects) {
            val (objX, objY) = obj.centerCoordinate
            val tolerance = 50f // 50 pixel tolerance

            if (kotlin.math.abs(tapX - objX) <= tolerance &&
                kotlin.math.abs(tapY - objY) <= tolerance) {
                return TapResult(true, obj, "Object hit")
            }
        }

        return TapResult(false, null, "No object hit")
    }

    /**
     * Filter detected objects based on needed element
     */
    fun filterObjectsByElement(
        detectedObjects: List<DetectedObjectResult>,
        neededElement: ElementMapper.Element?,
        elementMapper: ElementMapper
    ): List<DetectedObjectResult> {
        if (neededElement == null) {
            return detectedObjects
        }

        return detectedObjects.filter { obj ->
            val mappedElement = elementMapper.mapLabelToElement(obj.label)
            mappedElement == neededElement
        }
    }

    /**
     * Calculate view-projection matrix (testable matrix math)
     */
    fun calculateViewProjectionMatrix(
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray
    ): FloatArray {
        val viewProjectionMatrix = FloatArray(16)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)
        return viewProjectionMatrix
    }

    /**
     * Determine if quest is completed
     */
    fun isQuestCompleted(collectedCount: Int, targetCount: Int = 5): Boolean {
        return collectedCount >= targetCount
    }

    /**
     * Calculate anchor position from screen coordinates
     */
    fun calculateAnchorPosition(
        screenX: Float,
        screenY: Float,
        screenWidth: Float,
        screenHeight: Float
    ): Pair<Float, Float> {
        // Normalize to 0-1 range
        val normalizedX = screenX / screenWidth
        val normalizedY = screenY / screenHeight
        return Pair(normalizedX, normalizedY)
    }

    /**
     * Result of tap processing
     */
    data class TapResult(
        val hit: Boolean,
        val hitObject: DetectedObjectResult?,
        val reason: String
    )
}