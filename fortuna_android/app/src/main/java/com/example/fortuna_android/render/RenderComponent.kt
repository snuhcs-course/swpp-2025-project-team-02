package com.example.fortuna_android.render

import com.example.fortuna_android.common.samplerender.SampleRender
import com.google.ar.core.Frame
import com.example.fortuna_android.ui.ARRenderer

/**
 * Composite Pattern: Component (Abstract Base)
 * Defines the interface for objects in the composition tree.
 * Declares common operations for both Leaf and Composite objects.
 */
abstract class RenderComponent {
    /**
     * Initialize rendering resources
     * Called when the OpenGL surface is created
     */
    open fun onSurfaceCreated(render: SampleRender) {}

    /**
     * Perform rendering operation
     *
     * @param render SampleRender instance for drawing
     * @param context Rendering context containing matrices and AR data
     */
    open fun draw(render: SampleRender, context: RenderContext) {}

    /**
     * Release rendering resources
     * Called when the component is no longer needed
     */
    open fun release() {}

    // Composite-specific methods (throw exception in Leaf nodes)

    /**
     * Add a child component (Composite only)
     * @throws UnsupportedOperationException if called on Leaf
     */
    open fun add(component: RenderComponent) {
        throw UnsupportedOperationException("Cannot add to a leaf component")
    }

    /**
     * Remove a child component (Composite only)
     * @throws UnsupportedOperationException if called on Leaf
     */
    open fun remove(component: RenderComponent) {
        throw UnsupportedOperationException("Cannot remove from a leaf component")
    }

    /**
     * Get child at index (Composite only)
     * @throws UnsupportedOperationException if called on Leaf
     */
    open fun getChild(i: Int): RenderComponent {
        throw UnsupportedOperationException("Leaf has no children")
    }

    /**
     * Get component name
     */
    open fun getName(): String = this::class.simpleName ?: "Unknown"

    /**
     * Get component description
     */
    open fun getDescription(): String = "Render component"
}

/**
 * Context object containing rendering state
 * Passed to all components during draw() operation
 */
data class RenderContext(
    val viewMatrix: FloatArray,
    val projectionMatrix: FloatArray,
    val viewProjectionMatrix: FloatArray,
    val frame: Frame? = null,
    val arLabeledAnchors: List<ARRenderer.ARLabeledAnchor> = emptyList(),
    val allAnchors: List<ARRenderer.ARLabeledAnchor> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RenderContext

        if (!viewMatrix.contentEquals(other.viewMatrix)) return false
        if (!projectionMatrix.contentEquals(other.projectionMatrix)) return false
        if (!viewProjectionMatrix.contentEquals(other.viewProjectionMatrix)) return false
        if (frame != other.frame) return false

        return true
    }

    override fun hashCode(): Int {
        var result = viewMatrix.contentHashCode()
        result = 31 * result + projectionMatrix.contentHashCode()
        result = 31 * result + viewProjectionMatrix.contentHashCode()
        result = 31 * result + (frame?.hashCode() ?: 0)
        return result
    }
}
