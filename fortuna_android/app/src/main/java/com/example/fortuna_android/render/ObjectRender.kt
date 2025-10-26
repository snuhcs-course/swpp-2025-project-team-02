/*
 * Renders 3D objects (spheres) for AR element visualization
 */
package com.example.fortuna_android.render

import android.opengl.Matrix
import android.util.Log
import com.example.fortuna_android.classification.ElementMapper
import com.example.fortuna_android.common.samplerender.Mesh
import com.example.fortuna_android.common.samplerender.SampleRender
import com.example.fortuna_android.common.samplerender.Shader
import com.google.ar.core.Pose
import java.io.IOException

/**
 * Renders 3D sphere objects at anchor positions in AR
 */
class ObjectRender {
    companion object {
        private const val TAG = "ObjectRender"
        private const val OBJECT_SCALE = 0.02f // Scale for the spheres
    }

    // Map to store loaded meshes for each element
    private val meshes = mutableMapOf<ElementMapper.Element, Mesh>()

    // Map element to sphere color (RGB + Alpha)
    private val elementColors = mapOf(
        ElementMapper.Element.FIRE to floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f),      // Red
        ElementMapper.Element.METAL to floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),     // White
        ElementMapper.Element.EARTH to floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f),     // Yellow
        ElementMapper.Element.WOOD to floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f),      // Green
        ElementMapper.Element.WATER to floatArrayOf(0.0f, 0.5f, 1.0f, 1.0f),     // Blue
        ElementMapper.Element.OTHERS to floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f)     // Gray
    )

    // Map element to sphere obj file name
    private val elementToSphereFile = mapOf(
        ElementMapper.Element.FIRE to "spheres/red.obj",
        ElementMapper.Element.METAL to "spheres/white.obj",
        ElementMapper.Element.EARTH to "spheres/yellow.obj",
        ElementMapper.Element.WOOD to "spheres/green.obj",
        ElementMapper.Element.WATER to "spheres/blue.obj",
        ElementMapper.Element.OTHERS to "spheres/white.obj" // Default to white for others
    )

    private lateinit var shader: Shader

    private val modelMatrix = FloatArray(16)
    private val modelViewMatrix = FloatArray(16)
    private val modelViewProjectionMatrix = FloatArray(16)

    /**
     * Initialize the renderer and load meshes
     */
    fun onSurfaceCreated(render: SampleRender) {
        try {
            // Create shader
            shader = Shader.createFromAssets(
                render,
                "shaders/object.vert",
                "shaders/object.frag",
                null
            )
                .setDepthTest(true)
                .setDepthWrite(true)

            // Load meshes for each element
            ElementMapper.Element.values().forEach { element ->
                val objFile = elementToSphereFile[element]
                if (objFile != null) {
                    try {
                        val mesh = Mesh.createFromAsset(render, objFile)
                        meshes[element] = mesh
                        Log.d(TAG, "Loaded mesh for element ${element.displayName} from $objFile")
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to load mesh for element ${element.displayName} from $objFile", e)
                    }
                }
            }

            Log.i(TAG, "ObjectRender initialized successfully with ${meshes.size} meshes")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create shader", e)
        }
    }

    /**
     * Draw a 3D sphere object at the given pose
     */
    fun draw(
        render: SampleRender,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        pose: Pose,
        element: ElementMapper.Element
    ) {
        // Skip rendering for OTHERS category
        if (element == ElementMapper.Element.OTHERS) {
            return
        }

        val mesh = meshes[element]
        if (mesh == null) {
            Log.w(TAG, "No mesh loaded for element ${element.displayName}")
            return
        }

        val color = elementColors[element] ?: floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)

        // Build model matrix - position and scale
        pose.toMatrix(modelMatrix, 0)
        Matrix.scaleM(modelMatrix, 0, OBJECT_SCALE, OBJECT_SCALE, OBJECT_SCALE)

        // Calculate model-view matrix
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)

        // Calculate model-view-projection matrix
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        // Set shader uniforms and draw
        shader
            .setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            .setMat4("u_ModelView", modelViewMatrix)
            .setVec4("u_Color", color)
            .setVec3("u_LightDirection", floatArrayOf(0.0f, 1.0f, 0.0f))
            .setFloat("u_LightIntensity", 0.7f)

        render.draw(mesh, shader)
    }

    /**
     * Clean up resources
     */
    fun close() {
        meshes.values.forEach { it.close() }
        meshes.clear()
    }
}