package com.example.fortuna_android.render

import com.example.fortuna_android.common.samplerender.SampleRender
import com.google.ar.core.TrackingState
import android.util.Log

/**
 * Composite Pattern: Leaf
 * Represents an individual rendering component (3D Objects).
 * Renders 3D sphere objects at AR anchor positions.
 */
class ObjectRendererComponent : RenderComponent() {

    companion object {
        private const val TAG = "ObjectRenderer"
    }

    // Delegate to existing ObjectRender implementation
    private val objectRenderer = ObjectRender()

    override fun getName(): String = "Object Renderer"

    override fun getDescription(): String = "Renders 3D element spheres in AR"

    override fun onSurfaceCreated(render: SampleRender) {
        Log.d(TAG, "Initializing object renderer")
        objectRenderer.onSurfaceCreated(render)
    }

    /**
     * Perform actual rendering - leaf implementation
     * Draws all AR anchored objects
     */
    override fun draw(render: SampleRender, context: RenderContext) {
        // Create a safe copy to avoid concurrent modification
        val anchorsCopy = context.arLabeledAnchors.toList()

        for (arLabeledAnchor in anchorsCopy) {
            val anchor = arLabeledAnchor.anchor
            if (anchor.trackingState != TrackingState.TRACKING) continue

            try {
                // Draw 3D sphere object for each element
                objectRenderer.draw(
                    render,
                    context.viewMatrix,
                    context.projectionMatrix,
                    anchor.pose,
                    arLabeledAnchor.element,
                    arLabeledAnchor.distance,
                    arLabeledAnchor.animationType
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error drawing object at anchor", e)
            }
        }
    }

    override fun release() {
        Log.d(TAG, "Releasing object renderer")
        // ObjectRender manages its own resources
    }
}
