package com.example.fortuna_android.render

import android.opengl.Matrix
import android.util.Log
import kotlin.math.sqrt

/**
 * Frustum Culling implementation for AR objects
 *
 * Extracts camera frustum planes from view-projection matrix and provides
 * efficient sphere-frustum intersection testing to cull objects outside
 * the camera's view.
 */
class FrustumCuller {

    companion object {
        private const val TAG = "FrustumCuller"
    }

    /**
     * Represents a plane in 3D space using the equation ax + by + cz + d = 0
     */
    data class Plane(
        var a: Float = 0f,
        var b: Float = 0f,
        var c: Float = 0f,
        var d: Float = 0f
    ) {
        /**
         * Normalize the plane equation
         */
        fun normalize() {
            val length = sqrt(a * a + b * b + c * c)
            if (length > 0.0001f) {
                a /= length
                b /= length
                c /= length
                d /= length
            }
        }

        /**
         * Calculate signed distance from point to plane
         * Positive = in front of plane, Negative = behind plane
         */
        fun distanceToPoint(x: Float, y: Float, z: Float): Float {
            return a * x + b * y + c * z + d
        }
    }

    /**
     * Frustum planes: Left, Right, Top, Bottom, Near, Far
     */
    private val frustumPlanes = Array(6) { Plane() }

    // Plane indices for clarity
    private val LEFT = 0
    private val RIGHT = 1
    private val TOP = 2
    private val BOTTOM = 3
    private val NEAR = 4
    private val FAR = 5

    // Performance tracking
    private var totalTests = 0
    private var culledObjects = 0
    private var lastLogTime = 0L

    /**
     * Extract frustum planes from view-projection matrix
     *
     * Uses the standard method of extracting frustum planes from the
     * combined view-projection matrix by adding/subtracting rows.
     */
    fun extractFrustumPlanes(viewProjectionMatrix: FloatArray) {
        val m = viewProjectionMatrix

        // Left plane: m[3] + m[0], m[7] + m[4], m[11] + m[8], m[15] + m[12]
        frustumPlanes[LEFT].apply {
            a = m[3] + m[0]
            b = m[7] + m[4]
            c = m[11] + m[8]
            d = m[15] + m[12]
            normalize()
        }

        // Right plane: m[3] - m[0], m[7] - m[4], m[11] - m[8], m[15] - m[12]
        frustumPlanes[RIGHT].apply {
            a = m[3] - m[0]
            b = m[7] - m[4]
            c = m[11] - m[8]
            d = m[15] - m[12]
            normalize()
        }

        // Top plane: m[3] - m[1], m[7] - m[5], m[11] - m[9], m[15] - m[13]
        frustumPlanes[TOP].apply {
            a = m[3] - m[1]
            b = m[7] - m[5]
            c = m[11] - m[9]
            d = m[15] - m[13]
            normalize()
        }

        // Bottom plane: m[3] + m[1], m[7] + m[5], m[11] + m[9], m[15] + m[13]
        frustumPlanes[BOTTOM].apply {
            a = m[3] + m[1]
            b = m[7] + m[5]
            c = m[11] + m[9]
            d = m[15] + m[13]
            normalize()
        }

        // Near plane: m[3] + m[2], m[7] + m[6], m[11] + m[10], m[15] + m[14]
        frustumPlanes[NEAR].apply {
            a = m[3] + m[2]
            b = m[7] + m[6]
            c = m[11] + m[10]
            d = m[15] + m[14]
            normalize()
        }

        // Far plane: m[3] - m[2], m[7] - m[6], m[11] - m[10], m[15] - m[14]
        frustumPlanes[FAR].apply {
            a = m[3] - m[2]
            b = m[7] - m[6]
            c = m[11] - m[10]
            d = m[15] - m[14]
            normalize()
        }
    }

    /**
     * Test if a sphere intersects the camera frustum
     *
     * @param centerX Sphere center X coordinate
     * @param centerY Sphere center Y coordinate
     * @param centerZ Sphere center Z coordinate
     * @param radius Sphere radius
     * @return true if sphere is visible (inside or intersecting frustum), false if completely outside
     */
    fun isSphereInFrustum(centerX: Float, centerY: Float, centerZ: Float, radius: Float): Boolean {
        totalTests++

        // Test sphere against all 6 frustum planes
        for (plane in frustumPlanes) {
            val distance = plane.distanceToPoint(centerX, centerY, centerZ)

            // If sphere is completely outside any plane, it's culled
            if (distance < -radius) {
                culledObjects++
                logPerformanceStats()
                return false
            }
        }

        // Sphere is inside or intersecting frustum
        logPerformanceStats()
        return true
    }

    /**
     * Test if a point is inside the camera frustum
     *
     * @param x Point X coordinate
     * @param y Point Y coordinate
     * @param z Point Z coordinate
     * @return true if point is visible (inside frustum), false if outside
     */
    fun isPointInFrustum(x: Float, y: Float, z: Float): Boolean {
        totalTests++

        // Test point against all 6 frustum planes
        for (plane in frustumPlanes) {
            val distance = plane.distanceToPoint(x, y, z)

            // If point is outside any plane, it's culled
            if (distance < 0) {
                culledObjects++
                logPerformanceStats()
                return false
            }
        }

        // Point is inside frustum
        logPerformanceStats()
        return true
    }

    /**
     * Get current culling statistics
     */
    fun getCullingStats(): Pair<Int, Int> = Pair(totalTests, culledObjects)

    /**
     * Reset performance statistics
     */
    fun resetStats() {
        totalTests = 0
        culledObjects = 0
        lastLogTime = 0L
    }

    /**
     * Log performance statistics periodically
     */
    private fun logPerformanceStats() {
        val currentTime = System.currentTimeMillis()

        // Log stats every 5 seconds
        if (currentTime - lastLogTime > 5000) {
            lastLogTime = currentTime

            val cullRate = if (totalTests > 0) {
                (culledObjects.toFloat() / totalTests * 100).toInt()
            } else {
                0
            }

            Log.d(TAG, "Frustum Culling Stats - Total Tests: $totalTests, Culled: $culledObjects ($cullRate%)")
        }
    }

    /**
     * Get debug information about frustum planes (for visualization)
     */
    fun getFrustumPlanesDebugInfo(): List<String> {
        return listOf(
            "LEFT: a=${frustumPlanes[LEFT].a.format(3)}, b=${frustumPlanes[LEFT].b.format(3)}, c=${frustumPlanes[LEFT].c.format(3)}, d=${frustumPlanes[LEFT].d.format(3)}",
            "RIGHT: a=${frustumPlanes[RIGHT].a.format(3)}, b=${frustumPlanes[RIGHT].b.format(3)}, c=${frustumPlanes[RIGHT].c.format(3)}, d=${frustumPlanes[RIGHT].d.format(3)}",
            "TOP: a=${frustumPlanes[TOP].a.format(3)}, b=${frustumPlanes[TOP].b.format(3)}, c=${frustumPlanes[TOP].c.format(3)}, d=${frustumPlanes[TOP].d.format(3)}",
            "BOTTOM: a=${frustumPlanes[BOTTOM].a.format(3)}, b=${frustumPlanes[BOTTOM].b.format(3)}, c=${frustumPlanes[BOTTOM].c.format(3)}, d=${frustumPlanes[BOTTOM].d.format(3)}",
            "NEAR: a=${frustumPlanes[NEAR].a.format(3)}, b=${frustumPlanes[NEAR].b.format(3)}, c=${frustumPlanes[NEAR].c.format(3)}, d=${frustumPlanes[NEAR].d.format(3)}",
            "FAR: a=${frustumPlanes[FAR].a.format(3)}, b=${frustumPlanes[FAR].b.format(3)}, c=${frustumPlanes[FAR].c.format(3)}, d=${frustumPlanes[FAR].d.format(3)}"
        )
    }

    /**
     * Extension function to format float with specified decimal places
     */
    private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
}