package com.example.fortuna_android.render

import com.example.fortuna_android.common.samplerender.SampleRender
import android.util.Log

/**
 * Composite Pattern: Client Example
 * This demonstrates how to use the RenderComponent composite pattern
 * following the course material example structure.
 */
class RenderCompositeClient {

    companion object {
        private const val TAG = "RenderCompositeClient"
    }

    /**
     * Example 1: Basic usage following course pattern exactly
     * Demonstrates building a composite tree structure and using it uniformly
     */
    fun basicExample(render: SampleRender) {
        Log.d(TAG, "=== Basic Example: Building Render Tree ===")

        // Create composite renderer (analogous to MenuComposite)
        val rootRenderer = CompositeRenderer(
            "AR Root",
            "Main rendering pipeline"
        )

        // Add leaf components (analogous to MenuItem)
        rootRenderer.add(BackgroundRendererComponent())
        rootRenderer.add(PointCloudRendererComponent())
        rootRenderer.add(ObjectRendererComponent())

        // Initialize tree (calls onSurfaceCreated on all children)
        rootRenderer.onSurfaceCreated(render)

        // Print tree structure
        rootRenderer.printTree()

        // Treat composite uniformly - single call renders all components
        // This is analogous to: allMenus.print()
        val context = RenderContext(
            viewMatrix = FloatArray(16),
            projectionMatrix = FloatArray(16),
            viewProjectionMatrix = FloatArray(16)
        )
        rootRenderer.draw(render, context)

        Log.d(TAG, "All components rendered through single draw() call")
    }

    /**
     * Example 2: Nested composite structure
     * Demonstrates building hierarchical menu-like structure:
     * Root
     *   ├── Background (Leaf)
     *   ├── 3D Scene (Composite)
     *   │   ├── Point Cloud (Leaf)
     *   │   └── Objects (Leaf)
     *   └── UI Layer (Composite)
     */
    fun nestedCompositeExample(render: SampleRender) {
        Log.d(TAG, "=== Nested Composite Example ===")

        // Root composite (like AllMenus)
        val rootRenderer = CompositeRenderer("AR Root", "Main pipeline")

        // Background layer - single leaf
        rootRenderer.add(BackgroundRendererComponent())

        // 3D Scene layer - composite with multiple children (like PancakeHouseMenu)
        val sceneComposite = CompositeRenderer("3D Scene", "AR 3D content layer")
        sceneComposite.add(PointCloudRendererComponent())
        sceneComposite.add(ObjectRendererComponent())
        rootRenderer.add(sceneComposite)

        // UI layer - another composite (like DinerMenu)
        val uiComposite = CompositeRenderer("UI Layer", "Overlay elements")
        // Could add label renderers, etc.
        rootRenderer.add(uiComposite)

        // Print hierarchical structure
        rootRenderer.printTree()
        // Output will show:
        // + AR Root: Main pipeline
        //   - Background Renderer: Renders AR camera background
        //   + 3D Scene: AR 3D content layer
        //     - Point Cloud Renderer: Renders AR point cloud visualization
        //     - Object Renderer: Renders 3D element spheres in AR
        //   + UI Layer: Overlay elements

        Log.d(TAG, "Nested composite has ${rootRenderer.getChildCount()} top-level children")
        Log.d(TAG, "3D Scene has ${sceneComposite.getChildCount()} children")
    }

    /**
     * Example 3: Dynamic composition at runtime
     * Shows adding/removing components dynamically
     */
    fun dynamicCompositionExample(render: SampleRender) {
        Log.d(TAG, "=== Dynamic Composition Example ===")

        val rootRenderer = CompositeRenderer("Dynamic Root", "Runtime composition")

        // Start with just background
        rootRenderer.add(BackgroundRendererComponent())
        Log.d(TAG, "Initial: ${rootRenderer.getChildCount()} components")

        // Add more components dynamically
        val pointCloud = PointCloudRendererComponent()
        rootRenderer.add(pointCloud)
        Log.d(TAG, "After adding point cloud: ${rootRenderer.getChildCount()} components")

        val objects = ObjectRendererComponent()
        rootRenderer.add(objects)
        Log.d(TAG, "After adding objects: ${rootRenderer.getChildCount()} components")

        // Remove a component
        rootRenderer.remove(pointCloud)
        Log.d(TAG, "After removing point cloud: ${rootRenderer.getChildCount()} components")

        // All operations treat components uniformly
        rootRenderer.printTree()
    }

    /**
     * Example 4: Demonstrating uniform treatment
     * Shows that individual components and composites are treated the same way
     */
    fun uniformTreatmentExample(render: SampleRender) {
        Log.d(TAG, "=== Uniform Treatment Example ===")

        // Individual component
        val leafComponent: RenderComponent = PointCloudRendererComponent()

        // Composite component
        val compositeComponent: RenderComponent = CompositeRenderer("Scene", "3D Scene")
            .apply {
                add(ObjectRendererComponent())
            }

        // Both are RenderComponent - treated uniformly
        val context = RenderContext(
            viewMatrix = FloatArray(16),
            projectionMatrix = FloatArray(16),
            viewProjectionMatrix = FloatArray(16)
        )

        // Same method call for both
        leafComponent.draw(render, context)
        compositeComponent.draw(render, context)

        Log.d(TAG, "Leaf name: ${leafComponent.getName()}")
        Log.d(TAG, "Composite name: ${compositeComponent.getName()}")
        Log.d(TAG, "Both treated uniformly through RenderComponent interface")
    }

    /**
     * Run all examples
     */
    fun runAllExamples(render: SampleRender) {
        basicExample(render)
        nestedCompositeExample(render)
        dynamicCompositionExample(render)
        uniformTreatmentExample(render)
    }
}
