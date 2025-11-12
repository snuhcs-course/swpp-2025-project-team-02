/*
 * Renders 3D objects (spheres) for AR element visualization
 */
package com.example.fortuna_android.render

import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.example.fortuna_android.classification.ElementMapper
import com.example.fortuna_android.common.samplerender.Mesh
import com.example.fortuna_android.common.samplerender.SampleRender
import com.example.fortuna_android.common.samplerender.Shader
import com.example.fortuna_android.common.samplerender.Texture
import com.google.ar.core.Pose
import java.io.IOException

/**
 * Renders 3D sphere objects at anchor positions in AR
 */
class ObjectRender {
    companion object {
        private const val TAG = "ObjectRender"
        private const val OBJECT_SCALE = 0.1f // Base scale for the objects (0.02 = small, 0.1 = medium, 0.2 = large)

        // Minimum scale to prevent tiny Pokemon (guarantees readable size)
        // At 1.5m distance: scale = 0.1 (normal)
        // At 3m distance: scale = 0.2 (2x larger to compensate)
        // At 5m+ distance: scale = 0.3+ (capped at reasonable size)
        private const val MIN_VISUAL_SCALE = 0.08f // Minimum absolute scale
        private const val REFERENCE_DISTANCE = 1.5f // Distance where OBJECT_SCALE is used as-is

        // Animation constants
        private const val BOUNCE_HEIGHT = 0.05f // How high objects bounce (in meters)
        private const val BOUNCE_SPEED = 3.0f // Speed of bouncing animation
    }

    // Animation time tracker
    private var animationTime = 0f

    // Map to store loaded meshes and textures for each element
    private val meshes = mutableMapOf<ElementMapper.Element, Mesh>()
    private val textures = mutableMapOf<ElementMapper.Element, Texture>()

    // Map element to sphere color (RGB + Alpha)
    private val elementColors = mapOf(
        ElementMapper.Element.FIRE to floatArrayOf(1.0f, 0.0f, 0.0f, 1.0f),      // Red
        ElementMapper.Element.METAL to floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f),     // White
        ElementMapper.Element.EARTH to floatArrayOf(1.0f, 1.0f, 0.0f, 1.0f),     // Yellow
        ElementMapper.Element.WOOD to floatArrayOf(0.0f, 1.0f, 0.0f, 1.0f),      // Green
        ElementMapper.Element.WATER to floatArrayOf(0.0f, 0.5f, 1.0f, 1.0f),     // Blue
        ElementMapper.Element.OTHERS to floatArrayOf(0.5f, 0.5f, 0.5f, 1.0f)     // Gray
    )

    // Map element to obj file name
    private val elementToSphereFile = mapOf(
        ElementMapper.Element.FIRE to "rendered/fire/base.obj",
        ElementMapper.Element.METAL to "rendered/metal/base.obj",
        ElementMapper.Element.EARTH to "rendered/earth/base.obj",
        ElementMapper.Element.WOOD to "rendered/wood/base.obj",
        ElementMapper.Element.WATER to "rendered/water/base.obj",
        ElementMapper.Element.OTHERS to "spheres/white.obj"
    )

    // Map element to texture file name
    private val elementToTextureFile = mapOf(
        ElementMapper.Element.FIRE to "rendered/fire/texture_diffuse.png",
        ElementMapper.Element.METAL to "rendered/metal/texture_diffuse.png",
        ElementMapper.Element.EARTH to "rendered/earth/texture_diffuse.png",
        ElementMapper.Element.WOOD to "rendered/wood/texture_diffuse.png",
        ElementMapper.Element.WATER to "rendered/water/texture_diffuse.png",
        ElementMapper.Element.OTHERS to null  // No texture for others
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

            // Load meshes and textures for each element
            ElementMapper.Element.values().forEach { element ->
                val objFile = elementToSphereFile[element]
                val textureFile = elementToTextureFile[element]

                // Load mesh
                if (objFile != null) {
                    try {
                        val mesh = Mesh.createFromAsset(render, objFile)
                        meshes[element] = mesh
                        Log.d(TAG, "✓ Loaded mesh for ${element.displayName} from $objFile")
                    } catch (e: IOException) {
                        Log.e(TAG, "✗ Failed to load mesh for ${element.displayName} from $objFile", e)
                    }
                }

                // Load texture
                if (textureFile != null) {
                    try {
                        val texture = Texture.createFromAsset(
                            render,
                            textureFile,
                            Texture.WrapMode.CLAMP_TO_EDGE,
                            Texture.ColorFormat.SRGB
                        )
                        textures[element] = texture
                        Log.d(TAG, "✓ Loaded texture for ${element.displayName} from $textureFile")
                    } catch (e: IOException) {
                        Log.e(TAG, "✗ Failed to load texture for ${element.displayName} from $textureFile", e)
                    }
                }
            }

            Log.i(TAG, "ObjectRender initialized: ${meshes.size} meshes, ${textures.size} textures")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create shader", e)
        }
    }

    /**
     * Update animation time - call this every frame
     */
    fun updateAnimation(deltaTime: Float) {
        animationTime += deltaTime
    }

    /**
     * Draw a 3D sphere object at the given pose with animations
     * @param distance Distance from camera when anchor was created (for size compensation)
     */
    fun draw(
        render: SampleRender,
        viewMatrix: FloatArray,
        projectionMatrix: FloatArray,
        pose: Pose,
        element: ElementMapper.Element,
        distance: Float = REFERENCE_DISTANCE
    ) {
        // Skip rendering for OTHERS category
        if (element == ElementMapper.Element.OTHERS) {
            return
        }

        val mesh = meshes[element]
        val texture = textures[element]

        if (mesh == null) {
            Log.w(TAG, "No mesh loaded for element ${element.displayName}")
            return
        }

        // Build model matrix - position and billboard rotation to face camera
        Matrix.setIdentityM(modelMatrix, 0)

        // Extract position from pose
        val translation = pose.translation
        val objectPos = floatArrayOf(translation[0], translation[1], translation[2])

        // Extract camera position from view matrix (inverse transform)
        val cameraPos = FloatArray(3)
        // Create inverse view matrix to get camera world position
        val inverseViewMatrix = FloatArray(16)
        Matrix.invertM(inverseViewMatrix, 0, viewMatrix, 0)

        // Camera position is the translation part of the inverse view matrix
        cameraPos[0] = inverseViewMatrix[12]
        cameraPos[1] = inverseViewMatrix[13]
        cameraPos[2] = inverseViewMatrix[14]

        // Calculate direction from object to camera
        val dirX = cameraPos[0] - objectPos[0]
        val dirY = cameraPos[1] - objectPos[1]
        val dirZ = cameraPos[2] - objectPos[2]

        // Normalize direction vector
        val length = kotlin.math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ).toFloat()
        if (length > 0.001f) {
            val normDirX = dirX / length
            val normDirZ = dirZ / length

            // Calculate rotation angle around Y axis to face camera
            val cameraFaceAngle = kotlin.math.atan2(normDirX, normDirZ) * 180.0f / kotlin.math.PI.toFloat()

            // Calculate bouncing animation (using absolute sine for bouncing effect)
            val bounceTime = animationTime * BOUNCE_SPEED
            val bounceOffset = kotlin.math.abs(kotlin.math.sin(bounceTime)) * BOUNCE_HEIGHT

            // Apply transformations: translate with bouncing, rotate to face camera only
            Matrix.translateM(modelMatrix, 0, objectPos[0], objectPos[1] + bounceOffset.toFloat(), objectPos[2])
            Matrix.rotateM(modelMatrix, 0, cameraFaceAngle, 0f, 1f, 0f) // Face camera only
        } else {
            // Fallback: just translate with bouncing animation
            val bounceTime = animationTime * BOUNCE_SPEED
            val bounceOffset = kotlin.math.abs(kotlin.math.sin(bounceTime)) * BOUNCE_HEIGHT

            Matrix.translateM(modelMatrix, 0, objectPos[0], objectPos[1] + bounceOffset.toFloat(), objectPos[2])
            // No rotation when camera position calculation fails
        }

        // Calculate distance-based scale to maintain visual size
        // Formula: scale increases with distance to keep apparent size consistent
        // distanceScale = max(1.0, distance / REFERENCE_DISTANCE)
        val distanceScale = kotlin.math.max(1.0f, distance / REFERENCE_DISTANCE)
        val baseScale = OBJECT_SCALE * distanceScale

        // Ensure minimum visual scale (prevents tiny Pokemon)
        val effectiveScale = kotlin.math.max(MIN_VISUAL_SCALE, baseScale)

        // Apply scale with squash-and-stretch effect for bouncing
        val bouncePhase = kotlin.math.sin(animationTime * BOUNCE_SPEED)
        val scaleY = 1.0f + kotlin.math.abs(bouncePhase) * 0.15f // Stretch when bouncing up
        val scaleXZ = 1.0f - kotlin.math.abs(bouncePhase) * 0.1f // Squash horizontally when bouncing

        val finalScaleX = effectiveScale * scaleXZ.toFloat()
        val finalScaleY = effectiveScale * scaleY.toFloat()
        val finalScaleZ = effectiveScale * scaleXZ.toFloat()

        Matrix.scaleM(modelMatrix, 0, finalScaleX, finalScaleY, finalScaleZ)

        // Log scale info for debugging (only occasionally to avoid spam)
        if (kotlin.random.Random.nextDouble() < 0.01) { // 1% of frames
            Log.d(TAG, "Scale: distance=${distance}m, distanceScale=$distanceScale, effectiveScale=$effectiveScale")
        }

        // Calculate model-view matrix
        Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0)

        // Calculate model-view-projection matrix
        Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0)

        // Set shader uniforms
        shader
            .setMat4("u_ModelViewProjection", modelViewProjectionMatrix)
            .setMat4("u_ModelView", modelViewMatrix)
            .setVec3("u_LightDirection", floatArrayOf(0.0f, 1.0f, 0.0f))
            .setFloat("u_LightIntensity", 0.7f)

        // Use texture if available, otherwise fallback to solid color
        if (texture != null) {
            shader
                .setTexture("u_Texture", texture)
                .setInt("u_UseTexture", 1)
        } else {
            val color = elementColors[element] ?: floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f)
            shader
                .setVec4("u_Color", color)
                .setInt("u_UseTexture", 0)
        }

        // Enable face culling for proper rendering
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)
        GLES30.glFrontFace(GLES30.GL_CCW)

        render.draw(mesh, shader)

        GLES30.glDisable(GLES30.GL_CULL_FACE)
    }

    /**
     * Clean up resources
     */
    fun close() {
        meshes.values.forEach { it.close() }
        meshes.clear()
        textures.values.forEach { it.close() }
        textures.clear()
    }
}