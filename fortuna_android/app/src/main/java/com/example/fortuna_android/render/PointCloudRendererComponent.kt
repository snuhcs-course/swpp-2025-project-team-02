package com.example.fortuna_android.render

import com.example.fortuna_android.common.samplerender.SampleRender
import android.util.Log

/**
 * Composite Pattern: Leaf
 *
 * Represents an individual rendering component (Point Cloud).
 * This is analogous to MenuItem in the course material.
 *
 * Key characteristics:
 * - Cannot have children
 * - Performs actual rendering work
 * - Implements operations defined in RenderComponent
 */
class PointCloudRendererComponent : RenderComponent() {

    companion object {
        private const val TAG = "PointCloudRenderer"
    }

    // Delegate to existing PointCloudRender implementation
    private val pointCloudRender = PointCloudRender()

    override fun getName(): String = "Point Cloud Renderer"

    override fun getDescription(): String = "Renders AR point cloud visualization"

    override fun onSurfaceCreated(render: SampleRender) {
        Log.d(TAG, "Initializing point cloud renderer")
        pointCloudRender.onSurfaceCreated(render)
    }

    /**
     * Perform actual rendering - this is the leaf's implementation
     */
    override fun draw(render: SampleRender, context: RenderContext) {
        val frame = context.frame ?: return

        try {
            frame.acquirePointCloud().use { pointCloud ->
                pointCloudRender.drawPointCloud(
                    render,
                    pointCloud,
                    context.viewProjectionMatrix
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing point cloud", e)
        }
    }

    override fun release() {
        Log.d(TAG, "Releasing point cloud renderer")
        // PointCloudRender doesn't have explicit release, but we log it
    }
}
