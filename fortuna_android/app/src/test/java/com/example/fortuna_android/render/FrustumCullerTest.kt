package com.example.fortuna_android.render

import android.opengl.Matrix
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

/**
 * Unit tests for FrustumCuller
 * Tests frustum plane extraction and sphere-frustum intersection
 */
class FrustumCullerTest {

    private lateinit var frustumCuller: FrustumCuller

    @Before
    fun setup() {
        frustumCuller = FrustumCuller()
    }

    @Test
    fun `test sphere inside frustum is visible`() {
        // Create a simple perspective projection matrix
        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val viewProjectionMatrix = FloatArray(16)

        // Standard perspective projection (field of view = 60°, aspect = 1, near = 0.1, far = 100)
        Matrix.perspectiveM(projectionMatrix, 0, 60f, 1f, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Extract frustum planes
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)

        // Test sphere at origin (should be visible)
        assertTrue(
            "Sphere at origin should be visible",
            frustumCuller.isSphereInFrustum(0f, 0f, 0f, 0.5f)
        )
    }

    @Test
    fun `test sphere outside frustum is culled`() {
        // Create a simple perspective projection matrix
        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val viewProjectionMatrix = FloatArray(16)

        // Standard perspective projection
        Matrix.perspectiveM(projectionMatrix, 0, 60f, 1f, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Extract frustum planes
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)

        // Test sphere far to the right (should be culled)
        assertFalse(
            "Sphere far to the right should be culled",
            frustumCuller.isSphereInFrustum(100f, 0f, 0f, 0.5f)
        )

        // Test sphere behind camera (should be culled)
        assertFalse(
            "Sphere behind camera should be culled",
            frustumCuller.isSphereInFrustum(0f, 0f, 10f, 0.5f)
        )

        // Test sphere too far away (should be culled)
        assertFalse(
            "Sphere beyond far plane should be culled",
            frustumCuller.isSphereInFrustum(0f, 0f, -200f, 0.5f)
        )
    }

    @Test
    fun `test point inside frustum is visible`() {
        // Create a simple perspective projection matrix
        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val viewProjectionMatrix = FloatArray(16)

        // Standard perspective projection
        Matrix.perspectiveM(projectionMatrix, 0, 60f, 1f, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Extract frustum planes
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)

        // Test point at origin (should be visible)
        assertTrue(
            "Point at origin should be visible",
            frustumCuller.isPointInFrustum(0f, 0f, 0f)
        )

        // Test point slightly in front of camera (should be visible)
        assertTrue(
            "Point in front of camera should be visible",
            frustumCuller.isPointInFrustum(0f, 0f, -1f)
        )
    }

    @Test
    fun `test point outside frustum is culled`() {
        // Create a simple perspective projection matrix
        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val viewProjectionMatrix = FloatArray(16)

        // Standard perspective projection
        Matrix.perspectiveM(projectionMatrix, 0, 60f, 1f, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Extract frustum planes
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)

        // Test point far to the left (should be culled)
        assertFalse(
            "Point far to the left should be culled",
            frustumCuller.isPointInFrustum(-100f, 0f, 0f)
        )

        // Test point behind camera (should be culled)
        assertFalse(
            "Point behind camera should be culled",
            frustumCuller.isPointInFrustum(0f, 0f, 10f)
        )
    }

    @Test
    fun `test sphere partially intersecting frustum is visible`() {
        // Create a simple perspective projection matrix
        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val viewProjectionMatrix = FloatArray(16)

        // Standard perspective projection
        Matrix.perspectiveM(projectionMatrix, 0, 60f, 1f, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Extract frustum planes
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)

        // Test large sphere that should partially intersect the frustum
        // Place sphere at edge of view with large radius
        assertTrue(
            "Large sphere at edge should be partially visible",
            frustumCuller.isSphereInFrustum(2f, 0f, -1f, 2f)
        )
    }

    @Test
    fun `test culling statistics are tracked correctly`() {
        // Create a simple perspective projection matrix
        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val viewProjectionMatrix = FloatArray(16)

        // Standard perspective projection
        Matrix.perspectiveM(projectionMatrix, 0, 60f, 1f, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Extract frustum planes
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)

        // Reset statistics
        frustumCuller.resetStats()

        // Perform some tests
        frustumCuller.isSphereInFrustum(0f, 0f, 0f, 0.5f) // Visible
        frustumCuller.isSphereInFrustum(100f, 0f, 0f, 0.5f) // Culled
        frustumCuller.isSphereInFrustum(0f, 100f, 0f, 0.5f) // Culled

        val (totalTests, culledObjects) = frustumCuller.getCullingStats()

        assertEquals("Should have performed 3 tests", 3, totalTests)
        assertEquals("Should have culled 2 objects", 2, culledObjects)
    }

    @Test
    fun `test frustum plane extraction produces valid planes`() {
        // Create a simple perspective projection matrix
        val projectionMatrix = FloatArray(16)
        val viewMatrix = FloatArray(16)
        val viewProjectionMatrix = FloatArray(16)

        // Standard perspective projection
        Matrix.perspectiveM(projectionMatrix, 0, 60f, 1f, 0.1f, 100f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        // Extract frustum planes
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)

        // Get debug info (should contain 6 planes)
        val debugInfo = frustumCuller.getFrustumPlanesDebugInfo()
        assertEquals("Should have 6 frustum planes", 6, debugInfo.size)

        // Verify that each plane has the expected format
        debugInfo.forEach { planeInfo ->
            assertTrue("Plane info should contain coefficients",
                planeInfo.matches(Regex(".*a=-?\\d+\\.\\d+.*")))
        }
    }
}