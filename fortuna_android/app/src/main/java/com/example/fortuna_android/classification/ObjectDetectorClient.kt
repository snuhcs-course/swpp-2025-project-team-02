package com.example.fortuna_android.classification

import android.app.Activity

/**
 * Factory Method Pattern: Client Example
 *
 * This demonstrates how to use the ObjectDetectorFactory pattern
 * following the course material example structure.
 */
class ObjectDetectorClient {

    /**
     * Example usage demonstrating Factory Method Pattern
     *
     * This shows how client code uses the factory to create different
     * detector types without knowing the concrete implementation details.
     */
    fun run(
        context: Activity,
        vlmManager: com.example.fortuna_android.vlm.SmolVLMManager
    ) {
        // Step 1: Create the factory (Abstract Creator -> Concrete Creator)
        val detectorFactory: ObjectDetectorFactory = ConfigurableDetectorFactory(
            vlmManager = vlmManager,
            onVLMClassified = { result ->
                println("VLM classified: ${result.label}")
            }
        )

        // Print factory info (inherited from abstract class)
        detectorFactory.info()

        // Step 2: Use factory method to create VLM detector
        // Similar to: val shape1 = shapeFactory.createShape("Circle")
        val vlmDetector = (detectorFactory as ConfigurableDetectorFactory)
            .createDetectorByType(context, "VLM")
        println("Created VLM detector: ${vlmDetector.javaClass.simpleName}")

        // Step 3: Use factory method to create MLKit detector
        // Similar to: val shape2 = shapeFactory.createShape("Rectangle")
        val mlkitDetector = (detectorFactory as ConfigurableDetectorFactory)
            .createDetectorByType(context, "MLKit")
        println("Created MLKit detector: ${mlkitDetector.javaClass.simpleName}")

        // Both detectors implement the same ObjectDetector interface
        // so they can be used polymorphically
        // Similar to: shape1.draw() and shape2.draw()
    }

    /**
     * Alternative simplified usage without explicit type casting
     */
    fun runSimplified(
        context: Activity,
        vlmManager: com.example.fortuna_android.vlm.SmolVLMManager
    ) {
        val factory = ConfigurableDetectorFactory(vlmManager = vlmManager)

        // Create different detectors using the same factory
        val detector1 = factory.createDetectorByType(context, "VLM")
        val detector2 = factory.createDetectorByType(context, "MLKit")

        // Use detectors polymorphically through ObjectDetector interface
        // Both support the same analyze() method
    }
}
