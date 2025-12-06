package com.example.fortuna_android.render

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import kotlin.math.PI
import kotlin.math.sqrt
import kotlin.math.tan

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
        val viewProjectionMatrix = createViewProjectionMatrix()
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)

        // Test sphere at origin (should be visible)
        assertTrue(
            "Sphere at origin should be visible",
            frustumCuller.isSphereInFrustum(0f, 0f, 0f, 0.5f)
        )
    }

    @Test
    fun `test sphere outside frustum is culled`() {
        val viewProjectionMatrix = createViewProjectionMatrix()
        frustumCuller.extractFrustumPlanes(viewProjectionMatrix)

        assertFalse(
            "Sphere far to the right should be culled",
            frustumCuller.isSphereInFrustum(100f, 0f, 0f, 0.5f)
        )

        assertFalse(
            "Sphere behind camera should be culled",
            frustumCuller.isSphereInFrustum(0f, 0f, 10f, 0.5f)
        )

        assertFalse(
            "Sphere beyond far plane should be culled",
            frustumCuller.isSphereInFrustum(0f, 0f, -200f, 0.5f)
        )
    }

    @Test
    fun `test point inside frustum is visible`() {
        val viewProjectionMatrix = createViewProjectionMatrix()
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
        val viewProjectionMatrix = createViewProjectionMatrix()
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
        val viewProjectionMatrix = createViewProjectionMatrix()
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
        val viewProjectionMatrix = createViewProjectionMatrix()
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
        val viewProjectionMatrix = createViewProjectionMatrix()
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

    /**
     * Build a deterministic view-projection matrix without depending on Android's Matrix
     * (Robolectric leaves those methods as no-ops in unit tests).
     */
    private fun createViewProjectionMatrix(): FloatArray {
        val projection = FloatArray(16)
        val view = FloatArray(16)
        perspectiveM(projection, 0, 60f, 1f, 0.1f, 100f)
        setLookAtM(view, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)

        val viewProjection = FloatArray(16)
        multiplyMM(viewProjection, 0, projection, 0, view, 0)
        return viewProjection
    }

    private fun perspectiveM(
        m: FloatArray,
        offset: Int,
        fovy: Float,
        aspect: Float,
        zNear: Float,
        zFar: Float
    ) {
        val f = 1f / tan(fovy * (PI / 360f)).toFloat()
        m.fill(0f)

        m[offset + 0] = f / aspect
        m[offset + 5] = f
        m[offset + 10] = (zFar + zNear) / (zNear - zFar)
        m[offset + 11] = -1f
        m[offset + 14] = (2f * zFar * zNear) / (zNear - zFar)
    }

    private fun setLookAtM(
        rm: FloatArray,
        rmOffset: Int,
        eyeX: Float,
        eyeY: Float,
        eyeZ: Float,
        centerX: Float,
        centerY: Float,
        centerZ: Float,
        upX: Float,
        upY: Float,
        upZ: Float
    ) {
        val fx = centerX - eyeX
        val fy = centerY - eyeY
        val fz = centerZ - eyeZ

        val rlf = 1.0f / sqrt(fx * fx + fy * fy + fz * fz)
        val nfx = fx * rlf
        val nfy = fy * rlf
        val nfz = fz * rlf

        var sx = nfy * upZ - nfz * upY
        var sy = nfz * upX - nfx * upZ
        var sz = nfx * upY - nfy * upX

        val rls = 1.0f / sqrt(sx * sx + sy * sy + sz * sz)
        sx *= rls
        sy *= rls
        sz *= rls

        val ux = sy * nfz - sz * nfy
        val uy = sz * nfx - sx * nfz
        val uz = sx * nfy - sy * nfx

        rm[rmOffset + 0] = sx
        rm[rmOffset + 4] = sy
        rm[rmOffset + 8] = sz
        rm[rmOffset + 12] = 0f

        rm[rmOffset + 1] = ux
        rm[rmOffset + 5] = uy
        rm[rmOffset + 9] = uz
        rm[rmOffset + 13] = 0f

        rm[rmOffset + 2] = -nfx
        rm[rmOffset + 6] = -nfy
        rm[rmOffset + 10] = -nfz
        rm[rmOffset + 14] = 0f

        rm[rmOffset + 3] = 0f
        rm[rmOffset + 7] = 0f
        rm[rmOffset + 11] = 0f
        rm[rmOffset + 15] = 1f

        translateM(rm, rmOffset, -eyeX, -eyeY, -eyeZ)
    }

    private fun translateM(m: FloatArray, mOffset: Int, x: Float, y: Float, z: Float) {
        for (i in 0..3) {
            val mi0 = mOffset + i
            val mi4 = mOffset + 4 + i
            val mi8 = mOffset + 8 + i
            val mi12 = mOffset + 12 + i
            m[mi12] += m[mi0] * x + m[mi4] * y + m[mi8] * z
        }
    }

    private fun multiplyMM(
        result: FloatArray,
        resultOffset: Int,
        lhs: FloatArray,
        lhsOffset: Int,
        rhs: FloatArray,
        rhsOffset: Int
    ) {
        for (i in 0..3) {
            for (j in 0..3) {
                var sum = 0f
                for (k in 0..3) {
                    sum += lhs[lhsOffset + i + k * 4] * rhs[rhsOffset + k + j * 4]
                }
                result[resultOffset + i + j * 4] = sum
            }
        }
    }
}
