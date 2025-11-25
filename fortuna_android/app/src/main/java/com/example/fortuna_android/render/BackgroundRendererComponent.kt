package com.example.fortuna_android.render

import com.example.fortuna_android.common.samplerender.SampleRender
import com.example.fortuna_android.common.samplerender.arcore.BackgroundRenderer
import android.util.Log

/**
 * Composite Pattern: Leaf
 * Represents an individual rendering component (AR Background).
 * Renders the camera feed background in AR.
 */
class BackgroundRendererComponent(
    private val sharedBackgroundRenderer: BackgroundRenderer? = null
) : RenderComponent() {

    companion object {
        private const val TAG = "BackgroundRenderer"
    }

    // Background renderer instance - use shared instance if provided
    private lateinit var backgroundRenderer: BackgroundRenderer

    override fun getName(): String = "Background Renderer"

    override fun getDescription(): String = "Renders AR camera background"

    override fun onSurfaceCreated(render: SampleRender) {
        Log.d(TAG, "Initializing background renderer")
        backgroundRenderer = sharedBackgroundRenderer ?: BackgroundRenderer(render).apply {
            setUseDepthVisualization(render, false)
        }

        // Shared instance is already initialized by ARRenderer, no need to re-initialize
        // Only log that we're using the shared instance
        if (sharedBackgroundRenderer != null) {
            Log.d(TAG, "Using shared BackgroundRenderer instance")
        }
    }

    /**
     * Perform actual rendering - leaf implementation
     * Draws camera background
     */
    override fun draw(render: SampleRender, context: RenderContext) {
        val frame = context.frame ?: return

        try {
            backgroundRenderer.updateDisplayGeometry(frame)
            backgroundRenderer.drawBackground(render)
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing background", e)
        }
    }

    override fun release() {
        Log.d(TAG, "Releasing background renderer")
        // BackgroundRenderer manages its own resources
    }
}
