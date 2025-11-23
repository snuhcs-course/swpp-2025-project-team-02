package com.example.fortuna_android.render

import com.example.fortuna_android.common.samplerender.SampleRender
import com.example.fortuna_android.common.samplerender.arcore.BackgroundRenderer
import android.util.Log

/**
 * Composite Pattern: Leaf
 * Represents an individual rendering component (AR Background).
 * Renders the camera feed background in AR.
 */
class BackgroundRendererComponent : RenderComponent() {

    companion object {
        private const val TAG = "BackgroundRenderer"
    }

    // Background renderer instance
    private lateinit var backgroundRenderer: BackgroundRenderer

    override fun getName(): String = "Background Renderer"

    override fun getDescription(): String = "Renders AR camera background"

    override fun onSurfaceCreated(render: SampleRender) {
        Log.d(TAG, "Initializing background renderer")
        backgroundRenderer = BackgroundRenderer(render).apply {
            setUseDepthVisualization(render, false)
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
