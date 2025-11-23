package com.example.fortuna_android.classification

import android.app.Activity
import com.example.fortuna_android.vlm.SmolVLMManager

/**
 * Factory Method Pattern: Concrete Creator
 * Implements the factory method to create specific ObjectDetector instances based on the detector type (MLKit or VLM).
 * @param vlmManager VLM manager instance (required for VLM detector)
 * @param onVLMClassified Callback for VLM classification results (optional)
 */
class ConfigurableDetectorFactory(
    private val vlmManager: SmolVLMManager? = null,
    private val onVLMClassified: ((DetectedObjectResult) -> Unit)? = null
) : ObjectDetectorFactory() {

    /**
     * Factory Method Implementation
     *   - "VLM" -> VLMObjectDetector
     *   - "MLKit" -> MLKitObjectDetector
     * @param type Detector type: "VLM" or "MLKit"
     * @return Concrete ObjectDetector instance
     * @throws IllegalArgumentException if VLM type is requested without vlmManager
     */
    override fun createDetector(context: Activity): ObjectDetector {
        return createDetectorByType(context, "VLM") // Default to VLM
    }

    /**
     * Extended factory method with explicit type parameter
     * @param context Activity context
     * @param type Detector type: "VLM" or "MLKit"
     * @return Concrete ObjectDetector instance
     */
    fun createDetectorByType(context: Activity, type: String): ObjectDetector {
        return when (type) {
            "VLM" -> {
                requireNotNull(vlmManager) {
                    "VLM detector requires SmolVLMManager to be provided in factory constructor"
                }
                VLMObjectDetector(
                    context = context,
                    vlmManager = vlmManager,
                    onVLMClassified = onVLMClassified
                )
            }
            "MLKit" -> {
                MLKitObjectDetector(
                    context = context,
                    minObjectSizePercent = 0.1f,
                    maxObjectSizePercent = 0.8f,
                    maxDetectedObjects = 1
                )
            }
            else -> {
                // Default fallback: VLM detector
                requireNotNull(vlmManager) {
                    "Unknown detector type '$type'. VLM detector (default) requires SmolVLMManager."
                }
                VLMObjectDetector(
                    context = context,
                    vlmManager = vlmManager,
                    onVLMClassified = onVLMClassified
                )
            }
        }
    }
}
