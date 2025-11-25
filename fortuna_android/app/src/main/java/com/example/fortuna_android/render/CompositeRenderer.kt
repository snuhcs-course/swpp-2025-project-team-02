package com.example.fortuna_android.render

import com.example.fortuna_android.common.samplerender.SampleRender
import android.util.Log

/**
 * Composite Pattern: Composite
 * Represents a group of RenderComponent objects.
 *
 * Key responsibilities:
 * - Store and manage child components
 * - Delegate operations to all children
 * - Treat individual and composite objects uniformly
 */
class CompositeRenderer(
    private val name: String,
    private val description: String
) : RenderComponent() {

    companion object {
        private const val TAG = "CompositeRenderer"
    }

    // Composition - analogous to menuChildren in MenuComposite
    private val children: MutableList<RenderComponent> = ArrayList()

    // Methods for composite - same as course material

    /**
     * Add a child component to this composite
     */
    override fun add(component: RenderComponent) {
        children.add(component)
        Log.d(TAG, "[$name] Added component: ${component.getName()}")
    }

    /**
     * Remove a child component from this composite
     */
    override fun remove(component: RenderComponent) {
        children.remove(component)
        Log.d(TAG, "[$name] Removed component: ${component.getName()}")
    }

    /**
     * Get child at specified index
     */
    override fun getChild(i: Int): RenderComponent {
        return children[i]
    }

    override fun getName(): String = name

    override fun getDescription(): String = description

    /**
     * Initialize all child components
     */
    override fun onSurfaceCreated(render: SampleRender) {
        Log.d(TAG, "[$name] Initializing ${children.size} child components")
        // Treat CompositeRenderer and Leaf renderers uniformly (Iteration)
        for (component in children) {
            component.onSurfaceCreated(render)
        }
    }

    /**
     * Draw all child components
     * Key pattern: Iterate over children and call the same method
     * Individual components and composites are treated uniformly
     */
    override fun draw(render: SampleRender, context: RenderContext) {
        // Treat CompositeRenderer and Leaf renderers uniformly (Iteration)
        for (component in children) {
            try {
                component.draw(render, context)
            } catch (e: Exception) {
                Log.e(TAG, "[$name] Error drawing component ${component.getName()}", e)
            }
        }
    }

    /**
     * Release all child components
     * Delegates to children
     */
    override fun release() {
        Log.d(TAG, "[$name] Releasing ${children.size} child components")
        for (component in children) {
            try {
                component.release()
            } catch (e: Exception) {
                Log.e(TAG, "[$name] Error releasing component ${component.getName()}", e)
            }
        }
        children.clear()
    }

    /**
     * Get number of children (utility method)
     */
    fun getChildCount(): Int = children.size

    /**
     * Print component tree structure (for debugging)
     */
    fun printTree(indent: String = "") {
        Log.d(TAG, "$indent+ $name: $description")
        for (component in children) {
            if (component is CompositeRenderer) {
                component.printTree("$indent  ")
            } else {
                Log.d(TAG, "$indent  - ${component.getName()}: ${component.getDescription()}")
            }
        }
    }
}
