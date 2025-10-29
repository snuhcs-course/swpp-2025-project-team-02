package com.example.fortuna_android.classification.utils

import com.google.cloud.vision.v1.NormalizedVertex
import org.junit.Assert.*
import org.junit.Test

class VertexUtilsTest {

    // Test toAbsoluteCoordinates extension function
    @Test
    fun testToAbsoluteCoordinates_NormalValues() {
        val vertex = NormalizedVertex.newBuilder()
            .setX(0.5f)
            .setY(0.5f)
            .build()

        val result = VertexUtils.run { vertex.toAbsoluteCoordinates(1000, 800) }

        assertEquals(500, result.first)
        assertEquals(400, result.second)
    }

    @Test
    fun testToAbsoluteCoordinates_ZeroValues() {
        val vertex = NormalizedVertex.newBuilder()
            .setX(0.0f)
            .setY(0.0f)
            .build()

        val result = VertexUtils.run { vertex.toAbsoluteCoordinates(1000, 800) }

        assertEquals(0, result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun testToAbsoluteCoordinates_MaxValues() {
        val vertex = NormalizedVertex.newBuilder()
            .setX(1.0f)
            .setY(1.0f)
            .build()

        val result = VertexUtils.run { vertex.toAbsoluteCoordinates(1000, 800) }

        assertEquals(1000, result.first)
        assertEquals(800, result.second)
    }

    @Test
    fun testToAbsoluteCoordinates_FractionalValues() {
        val vertex = NormalizedVertex.newBuilder()
            .setX(0.25f)
            .setY(0.75f)
            .build()

        val result = VertexUtils.run { vertex.toAbsoluteCoordinates(1000, 800) }

        assertEquals(250, result.first)
        assertEquals(600, result.second)
    }

    // Test rotateCoordinates extension function - all rotation angles
    @Test
    fun testRotateCoordinates_0Degrees() {
        val coord = 100 to 200
        val result = VertexUtils.run { coord.rotateCoordinates(1000, 800, 0) }

        assertEquals(100, result.first)
        assertEquals(200, result.second)
    }

    @Test
    fun testRotateCoordinates_90Degrees() {
        val coord = 100 to 200
        val result = VertexUtils.run { coord.rotateCoordinates(1000, 800, 90) }

        assertEquals(200, result.first)
        assertEquals(900, result.second)
    }

    @Test
    fun testRotateCoordinates_180Degrees() {
        val coord = 100 to 200
        val result = VertexUtils.run { coord.rotateCoordinates(1000, 800, 180) }

        assertEquals(900, result.first)
        assertEquals(600, result.second)
    }

    @Test
    fun testRotateCoordinates_270Degrees() {
        val coord = 100 to 200
        val result = VertexUtils.run { coord.rotateCoordinates(1000, 800, 270) }

        assertEquals(600, result.first)
        assertEquals(100, result.second)
    }

    @Test(expected = IllegalStateException::class)
    fun testRotateCoordinates_InvalidRotation() {
        val coord = 100 to 200
        VertexUtils.run { coord.rotateCoordinates(1000, 800, 45) }
    }

    @Test(expected = IllegalStateException::class)
    fun testRotateCoordinates_InvalidRotation_Negative() {
        val coord = 100 to 200
        VertexUtils.run { coord.rotateCoordinates(1000, 800, -90) }
    }

    @Test(expected = IllegalStateException::class)
    fun testRotateCoordinates_InvalidRotation_360() {
        val coord = 100 to 200
        VertexUtils.run { coord.rotateCoordinates(1000, 800, 360) }
    }

    // Test calculateAverage extension function
    @Test
    fun testCalculateAverage_SingleVertex() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.5f).setY(0.5f).build()
        )

        val result = VertexUtils.run { vertices.calculateAverage() }

        assertEquals(0.5f, result.x, 0.001f)
        assertEquals(0.5f, result.y, 0.001f)
    }

    @Test
    fun testCalculateAverage_TwoVertices() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build()
        )

        val result = VertexUtils.run { vertices.calculateAverage() }

        assertEquals(0.5f, result.x, 0.001f)
        assertEquals(0.5f, result.y, 0.001f)
    }

    @Test
    fun testCalculateAverage_FourVertices() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(1.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build(),
            NormalizedVertex.newBuilder().setX(0.0f).setY(1.0f).build()
        )

        val result = VertexUtils.run { vertices.calculateAverage() }

        assertEquals(0.5f, result.x, 0.001f)
        assertEquals(0.5f, result.y, 0.001f)
    }

    @Test
    fun testCalculateAverage_NonSymmetricVertices() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.2f).setY(0.3f).build(),
            NormalizedVertex.newBuilder().setX(0.8f).setY(0.4f).build(),
            NormalizedVertex.newBuilder().setX(0.6f).setY(0.9f).build()
        )

        val result = VertexUtils.run { vertices.calculateAverage() }

        // (0.2 + 0.8 + 0.6) / 3 = 1.6 / 3 = 0.533...
        assertEquals(0.533f, result.x, 0.01f)
        // (0.3 + 0.4 + 0.9) / 3 = 1.6 / 3 = 0.533...
        assertEquals(0.533f, result.y, 0.01f)
    }

    @Test
    fun testCalculateAverage_AllZeroVertices() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build()
        )

        val result = VertexUtils.run { vertices.calculateAverage() }

        assertEquals(0.0f, result.x, 0.001f)
        assertEquals(0.0f, result.y, 0.001f)
    }

    @Test
    fun testCalculateAverage_MaxVertices() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build(),
            NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build()
        )

        val result = VertexUtils.run { vertices.calculateAverage() }

        assertEquals(1.0f, result.x, 0.001f)
        assertEquals(1.0f, result.y, 0.001f)
    }

    // Combined integration tests
    @Test
    fun testIntegration_ToAbsoluteAndRotate() {
        val vertex = NormalizedVertex.newBuilder()
            .setX(0.5f)
            .setY(0.5f)
            .build()

        val absolute = VertexUtils.run { vertex.toAbsoluteCoordinates(1000, 800) }
        val rotated = VertexUtils.run { absolute.rotateCoordinates(1000, 800, 90) }

        assertEquals(400, rotated.first)
        assertEquals(500, rotated.second)
    }

    @Test
    fun testIntegration_CalculateAverageAndConvert() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build()
        )

        val average = VertexUtils.run { vertices.calculateAverage() }
        val absolute = VertexUtils.run { average.toAbsoluteCoordinates(1000, 800) }

        assertEquals(500, absolute.first)
        assertEquals(400, absolute.second)
    }
}
