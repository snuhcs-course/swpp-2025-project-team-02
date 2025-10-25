package com.example.fortuna_android.classification.utils

import com.example.fortuna_android.classification.utils.VertexUtils.calculateAverage
import com.example.fortuna_android.classification.utils.VertexUtils.rotateCoordinates
import com.example.fortuna_android.classification.utils.VertexUtils.toAbsoluteCoordinates
import com.google.cloud.vision.v1.NormalizedVertex
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for VertexUtils
 * Tests coordinate transformation and vertex manipulation utilities
 */
class VertexUtilsTest {

    // Test toAbsoluteCoordinates extension function
    @Test
    fun `test toAbsoluteCoordinates with zero coordinates`() {
        val vertex = NormalizedVertex.newBuilder().setX(0f).setY(0f).build()
        val result = vertex.toAbsoluteCoordinates(1920, 1080)

        assertEquals(0, result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun `test toAbsoluteCoordinates with normalized coordinates`() {
        val vertex = NormalizedVertex.newBuilder().setX(0.5f).setY(0.5f).build()
        val result = vertex.toAbsoluteCoordinates(1920, 1080)

        assertEquals(960, result.first)
        assertEquals(540, result.second)
    }

    @Test
    fun `test toAbsoluteCoordinates with max coordinates`() {
        val vertex = NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build()
        val result = vertex.toAbsoluteCoordinates(1920, 1080)

        assertEquals(1920, result.first)
        assertEquals(1080, result.second)
    }

    @Test
    fun `test toAbsoluteCoordinates with fractional coordinates`() {
        val vertex = NormalizedVertex.newBuilder().setX(0.25f).setY(0.75f).build()
        val result = vertex.toAbsoluteCoordinates(1000, 800)

        assertEquals(250, result.first)
        assertEquals(600, result.second)
    }

    @Test
    fun `test toAbsoluteCoordinates with small image size`() {
        val vertex = NormalizedVertex.newBuilder().setX(0.5f).setY(0.5f).build()
        val result = vertex.toAbsoluteCoordinates(100, 100)

        assertEquals(50, result.first)
        assertEquals(50, result.second)
    }

    @Test
    fun `test toAbsoluteCoordinates with large image size`() {
        val vertex = NormalizedVertex.newBuilder().setX(0.5f).setY(0.5f).build()
        val result = vertex.toAbsoluteCoordinates(4000, 3000)

        assertEquals(2000, result.first)
        assertEquals(1500, result.second)
    }

    // Test rotateCoordinates extension function
    @Test
    fun `test rotateCoordinates with 0 degree rotation`() {
        val coords = 100 to 200
        val result = coords.rotateCoordinates(1920, 1080, 0)

        assertEquals(100, result.first)
        assertEquals(200, result.second)
    }

    @Test
    fun `test rotateCoordinates with 180 degree rotation`() {
        val coords = 100 to 200
        val result = coords.rotateCoordinates(1920, 1080, 180)

        assertEquals(1920 - 100, result.first)
        assertEquals(1080 - 200, result.second)
    }

    @Test
    fun `test rotateCoordinates with 90 degree rotation`() {
        val coords = 100 to 200
        val result = coords.rotateCoordinates(1920, 1080, 90)

        assertEquals(200, result.first)
        assertEquals(1920 - 100, result.second)
    }

    @Test
    fun `test rotateCoordinates with 270 degree rotation`() {
        val coords = 100 to 200
        val result = coords.rotateCoordinates(1920, 1080, 270)

        assertEquals(1080 - 200, result.first)
        assertEquals(100, result.second)
    }

    @Test
    fun `test rotateCoordinates with origin point and 0 degree`() {
        val coords = 0 to 0
        val result = coords.rotateCoordinates(1920, 1080, 0)

        assertEquals(0, result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun `test rotateCoordinates with origin point and 180 degree`() {
        val coords = 0 to 0
        val result = coords.rotateCoordinates(1920, 1080, 180)

        assertEquals(1920, result.first)
        assertEquals(1080, result.second)
    }

    @Test
    fun `test rotateCoordinates with origin point and 90 degree`() {
        val coords = 0 to 0
        val result = coords.rotateCoordinates(1920, 1080, 90)

        assertEquals(0, result.first)
        assertEquals(1920, result.second)
    }

    @Test
    fun `test rotateCoordinates with origin point and 270 degree`() {
        val coords = 0 to 0
        val result = coords.rotateCoordinates(1920, 1080, 270)

        assertEquals(1080, result.first)
        assertEquals(0, result.second)
    }

    @Test
    fun `test rotateCoordinates with center point and 180 degree`() {
        val coords = 960 to 540
        val result = coords.rotateCoordinates(1920, 1080, 180)

        assertEquals(960, result.first)
        assertEquals(540, result.second)
    }

    @Test(expected = IllegalStateException::class)
    fun `test rotateCoordinates with invalid rotation throws error`() {
        val coords = 100 to 200
        coords.rotateCoordinates(1920, 1080, 45)
    }

    @Test(expected = IllegalStateException::class)
    fun `test rotateCoordinates with negative rotation throws error`() {
        val coords = 100 to 200
        coords.rotateCoordinates(1920, 1080, -90)
    }

    @Test(expected = IllegalStateException::class)
    fun `test rotateCoordinates with 360 rotation throws error`() {
        val coords = 100 to 200
        coords.rotateCoordinates(1920, 1080, 360)
    }

    // Test calculateAverage function
    @Test
    fun `test calculateAverage with single vertex`() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.5f).setY(0.5f).build()
        )
        val result = vertices.calculateAverage()

        assertEquals(0.5f, result.x, 0.001f)
        assertEquals(0.5f, result.y, 0.001f)
    }

    @Test
    fun `test calculateAverage with two vertices`() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build()
        )
        val result = vertices.calculateAverage()

        assertEquals(0.5f, result.x, 0.001f)
        assertEquals(0.5f, result.y, 0.001f)
    }

    @Test
    fun `test calculateAverage with four vertices forming a square`() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(1.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build(),
            NormalizedVertex.newBuilder().setX(0.0f).setY(1.0f).build()
        )
        val result = vertices.calculateAverage()

        assertEquals(0.5f, result.x, 0.001f)
        assertEquals(0.5f, result.y, 0.001f)
    }

    @Test
    fun `test calculateAverage with asymmetric vertices`() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.1f).setY(0.2f).build(),
            NormalizedVertex.newBuilder().setX(0.3f).setY(0.4f).build(),
            NormalizedVertex.newBuilder().setX(0.5f).setY(0.6f).build()
        )
        val result = vertices.calculateAverage()

        assertEquals(0.3f, result.x, 0.001f)
        assertEquals(0.4f, result.y, 0.001f)
    }

    @Test
    fun `test calculateAverage with zero coordinates`() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build(),
            NormalizedVertex.newBuilder().setX(0.0f).setY(0.0f).build()
        )
        val result = vertices.calculateAverage()

        assertEquals(0.0f, result.x, 0.001f)
        assertEquals(0.0f, result.y, 0.001f)
    }

    @Test
    fun `test calculateAverage with max coordinates`() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build(),
            NormalizedVertex.newBuilder().setX(1.0f).setY(1.0f).build()
        )
        val result = vertices.calculateAverage()

        assertEquals(1.0f, result.x, 0.001f)
        assertEquals(1.0f, result.y, 0.001f)
    }

    @Test
    fun `test calculateAverage with many vertices`() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.1f).setY(0.1f).build(),
            NormalizedVertex.newBuilder().setX(0.2f).setY(0.2f).build(),
            NormalizedVertex.newBuilder().setX(0.3f).setY(0.3f).build(),
            NormalizedVertex.newBuilder().setX(0.4f).setY(0.4f).build(),
            NormalizedVertex.newBuilder().setX(0.5f).setY(0.5f).build()
        )
        val result = vertices.calculateAverage()

        assertEquals(0.3f, result.x, 0.001f)
        assertEquals(0.3f, result.y, 0.001f)
    }

    // Integration tests combining multiple functions
    @Test
    fun `test conversion and rotation workflow`() {
        val vertex = NormalizedVertex.newBuilder().setX(0.5f).setY(0.5f).build()
        val absolute = vertex.toAbsoluteCoordinates(1920, 1080)
        val rotated = absolute.rotateCoordinates(1920, 1080, 180)

        assertEquals(960, rotated.first)
        assertEquals(540, rotated.second)
    }

    @Test
    fun `test average calculation and conversion workflow`() {
        val vertices = listOf(
            NormalizedVertex.newBuilder().setX(0.25f).setY(0.25f).build(),
            NormalizedVertex.newBuilder().setX(0.75f).setY(0.75f).build()
        )
        val average = vertices.calculateAverage()
        val absolute = average.toAbsoluteCoordinates(1000, 1000)

        assertEquals(500, absolute.first)
        assertEquals(500, absolute.second)
    }

    // Edge cases
    @Test
    fun `test toAbsoluteCoordinates with very small normalized values`() {
        val vertex = NormalizedVertex.newBuilder().setX(0.001f).setY(0.001f).build()
        val result = vertex.toAbsoluteCoordinates(1000, 1000)

        assertEquals(1, result.first)
        assertEquals(1, result.second)
    }

    @Test
    fun `test rotateCoordinates with square image`() {
        val coords = 100 to 100
        val result90 = coords.rotateCoordinates(1000, 1000, 90)
        val result180 = coords.rotateCoordinates(1000, 1000, 180)
        val result270 = coords.rotateCoordinates(1000, 1000, 270)

        assertEquals(100, result90.first)
        assertEquals(900, result90.second)
        assertEquals(900, result180.first)
        assertEquals(900, result180.second)
        assertEquals(900, result270.first)
        assertEquals(100, result270.second)
    }
}
