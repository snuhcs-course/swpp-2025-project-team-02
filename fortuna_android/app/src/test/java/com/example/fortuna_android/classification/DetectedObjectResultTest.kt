package com.example.fortuna_android.classification

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for DetectedObjectResult data class
 * Tests object creation, data integrity, and equality
 */
class DetectedObjectResultTest {

    @Test
    fun `test DetectedObjectResult creation with valid data`() {
        val result = DetectedObjectResult(
            confidence = 0.95f,
            label = "cat",
            centerCoordinate = 100 to 200
        )

        assertEquals(0.95f, result.confidence, 0.001f)
        assertEquals("cat", result.label)
        assertEquals(100, result.centerCoordinate.first)
        assertEquals(200, result.centerCoordinate.second)
    }

    @Test
    fun `test DetectedObjectResult with zero confidence`() {
        val result = DetectedObjectResult(
            confidence = 0.0f,
            label = "unknown",
            centerCoordinate = 0 to 0
        )

        assertEquals(0.0f, result.confidence, 0.001f)
        assertEquals("unknown", result.label)
        assertEquals(0, result.centerCoordinate.first)
        assertEquals(0, result.centerCoordinate.second)
    }

    @Test
    fun `test DetectedObjectResult with max confidence`() {
        val result = DetectedObjectResult(
            confidence = 1.0f,
            label = "person",
            centerCoordinate = 500 to 500
        )

        assertEquals(1.0f, result.confidence, 0.001f)
        assertEquals("person", result.label)
    }

    @Test
    fun `test DetectedObjectResult with empty label`() {
        val result = DetectedObjectResult(
            confidence = 0.5f,
            label = "",
            centerCoordinate = 50 to 50
        )

        assertEquals(0.5f, result.confidence, 0.001f)
        assertEquals("", result.label)
    }

    @Test
    fun `test DetectedObjectResult with long label`() {
        val longLabel = "this is a very long label with many words describing the detected object"
        val result = DetectedObjectResult(
            confidence = 0.85f,
            label = longLabel,
            centerCoordinate = 300 to 400
        )

        assertEquals(longLabel, result.label)
    }

    @Test
    fun `test DetectedObjectResult with special characters in label`() {
        val result = DetectedObjectResult(
            confidence = 0.75f,
            label = "cat-dog_123!@#",
            centerCoordinate = 150 to 250
        )

        assertEquals("cat-dog_123!@#", result.label)
    }

    @Test
    fun `test DetectedObjectResult with negative coordinates`() {
        val result = DetectedObjectResult(
            confidence = 0.6f,
            label = "object",
            centerCoordinate = -10 to -20
        )

        assertEquals(-10, result.centerCoordinate.first)
        assertEquals(-20, result.centerCoordinate.second)
    }

    @Test
    fun `test DetectedObjectResult with large coordinates`() {
        val result = DetectedObjectResult(
            confidence = 0.9f,
            label = "building",
            centerCoordinate = 10000 to 10000
        )

        assertEquals(10000, result.centerCoordinate.first)
        assertEquals(10000, result.centerCoordinate.second)
    }

    // Test data class properties
    @Test
    fun `test DetectedObjectResult equality with same values`() {
        val result1 = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 100 to 200
        )
        val result2 = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 100 to 200
        )

        assertEquals(result1, result2)
        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `test DetectedObjectResult inequality with different confidence`() {
        val result1 = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 100 to 200
        )
        val result2 = DetectedObjectResult(
            confidence = 0.7f,
            label = "car",
            centerCoordinate = 100 to 200
        )

        assertNotEquals(result1, result2)
    }

    @Test
    fun `test DetectedObjectResult inequality with different label`() {
        val result1 = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 100 to 200
        )
        val result2 = DetectedObjectResult(
            confidence = 0.8f,
            label = "truck",
            centerCoordinate = 100 to 200
        )

        assertNotEquals(result1, result2)
    }

    @Test
    fun `test DetectedObjectResult inequality with different coordinates`() {
        val result1 = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 100 to 200
        )
        val result2 = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 101 to 200
        )

        assertNotEquals(result1, result2)
    }

    @Test
    fun `test DetectedObjectResult copy function`() {
        val original = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 100 to 200
        )
        val copy = original.copy()

        assertEquals(original, copy)
        assertNotSame(original, copy)
    }

    @Test
    fun `test DetectedObjectResult copy with modified confidence`() {
        val original = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 100 to 200
        )
        val modified = original.copy(confidence = 0.9f)

        assertEquals(0.9f, modified.confidence, 0.001f)
        assertEquals(original.label, modified.label)
        assertEquals(original.centerCoordinate, modified.centerCoordinate)
        assertNotEquals(original, modified)
    }

    @Test
    fun `test DetectedObjectResult copy with modified label`() {
        val original = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 100 to 200
        )
        val modified = original.copy(label = "truck")

        assertEquals(original.confidence, modified.confidence, 0.001f)
        assertEquals("truck", modified.label)
        assertEquals(original.centerCoordinate, modified.centerCoordinate)
    }

    @Test
    fun `test DetectedObjectResult copy with modified coordinates`() {
        val original = DetectedObjectResult(
            confidence = 0.8f,
            label = "car",
            centerCoordinate = 100 to 200
        )
        val modified = original.copy(centerCoordinate = 300 to 400)

        assertEquals(original.confidence, modified.confidence, 0.001f)
        assertEquals(original.label, modified.label)
        assertEquals(300, modified.centerCoordinate.first)
        assertEquals(400, modified.centerCoordinate.second)
    }

    @Test
    fun `test DetectedObjectResult toString contains all properties`() {
        val result = DetectedObjectResult(
            confidence = 0.75f,
            label = "dog",
            centerCoordinate = 150 to 250
        )
        val toString = result.toString()

        assertTrue(toString.contains("0.75") || toString.contains("confidence"))
        assertTrue(toString.contains("dog") || toString.contains("label"))
        assertTrue(toString.contains("150") || toString.contains("250") || toString.contains("centerCoordinate"))
    }

    @Test
    fun `test DetectedObjectResult component1 returns confidence`() {
        val result = DetectedObjectResult(
            confidence = 0.95f,
            label = "cat",
            centerCoordinate = 100 to 200
        )
        val (confidence) = result

        assertEquals(0.95f, confidence, 0.001f)
    }

    @Test
    fun `test DetectedObjectResult component2 returns label`() {
        val result = DetectedObjectResult(
            confidence = 0.95f,
            label = "cat",
            centerCoordinate = 100 to 200
        )
        val (_, label) = result

        assertEquals("cat", label)
    }

    @Test
    fun `test DetectedObjectResult component3 returns centerCoordinate`() {
        val result = DetectedObjectResult(
            confidence = 0.95f,
            label = "cat",
            centerCoordinate = 100 to 200
        )
        val (_, _, centerCoordinate) = result

        assertEquals(100 to 200, centerCoordinate)
    }

    @Test
    fun `test DetectedObjectResult destructuring`() {
        val result = DetectedObjectResult(
            confidence = 0.85f,
            label = "person",
            centerCoordinate = 500 to 600
        )
        val (confidence, label, centerCoordinate) = result

        assertEquals(0.85f, confidence, 0.001f)
        assertEquals("person", label)
        assertEquals(500, centerCoordinate.first)
        assertEquals(600, centerCoordinate.second)
    }

    // Edge cases for confidence values
    @Test
    fun `test DetectedObjectResult with very small confidence`() {
        val result = DetectedObjectResult(
            confidence = 0.001f,
            label = "uncertain",
            centerCoordinate = 10 to 10
        )

        assertEquals(0.001f, result.confidence, 0.0001f)
    }

    @Test
    fun `test DetectedObjectResult with confidence close to 1`() {
        val result = DetectedObjectResult(
            confidence = 0.999f,
            label = "certain",
            centerCoordinate = 10 to 10
        )

        assertEquals(0.999f, result.confidence, 0.0001f)
    }

    @Test
    fun `test DetectedObjectResult with negative confidence`() {
        // While not recommended, data class allows negative values
        val result = DetectedObjectResult(
            confidence = -0.5f,
            label = "invalid",
            centerCoordinate = 0 to 0
        )

        assertEquals(-0.5f, result.confidence, 0.001f)
    }

    @Test
    fun `test DetectedObjectResult with confidence greater than 1`() {
        // While not recommended, data class allows values > 1
        val result = DetectedObjectResult(
            confidence = 1.5f,
            label = "invalid",
            centerCoordinate = 0 to 0
        )

        assertEquals(1.5f, result.confidence, 0.001f)
    }

    // Test with various label formats
    @Test
    fun `test DetectedObjectResult with numeric label`() {
        val result = DetectedObjectResult(
            confidence = 0.7f,
            label = "12345",
            centerCoordinate = 50 to 50
        )

        assertEquals("12345", result.label)
    }

    @Test
    fun `test DetectedObjectResult with unicode label`() {
        val result = DetectedObjectResult(
            confidence = 0.7f,
            label = "고양이",
            centerCoordinate = 50 to 50
        )

        assertEquals("고양이", result.label)
    }

    @Test
    fun `test DetectedObjectResult with whitespace in label`() {
        val result = DetectedObjectResult(
            confidence = 0.7f,
            label = "  cat  ",
            centerCoordinate = 50 to 50
        )

        assertEquals("  cat  ", result.label)
    }

    // Test coordinate edge cases
    @Test
    fun `test DetectedObjectResult with zero coordinates`() {
        val result = DetectedObjectResult(
            confidence = 0.5f,
            label = "origin",
            centerCoordinate = 0 to 0
        )

        assertEquals(0, result.centerCoordinate.first)
        assertEquals(0, result.centerCoordinate.second)
    }

    @Test
    fun `test DetectedObjectResult with asymmetric coordinates`() {
        val result = DetectedObjectResult(
            confidence = 0.5f,
            label = "object",
            centerCoordinate = 1000 to 1
        )

        assertEquals(1000, result.centerCoordinate.first)
        assertEquals(1, result.centerCoordinate.second)
    }

    @Test
    fun `test multiple DetectedObjectResults in a list`() {
        val results = listOf(
            DetectedObjectResult(0.9f, "cat", 100 to 100),
            DetectedObjectResult(0.8f, "dog", 200 to 200),
            DetectedObjectResult(0.7f, "bird", 300 to 300)
        )

        assertEquals(3, results.size)
        assertEquals("cat", results[0].label)
        assertEquals("dog", results[1].label)
        assertEquals("bird", results[2].label)
    }

    @Test
    fun `test DetectedObjectResult as map key`() {
        val result1 = DetectedObjectResult(0.8f, "car", 100 to 200)
        val result2 = DetectedObjectResult(0.8f, "car", 100 to 200)

        val map = mutableMapOf<DetectedObjectResult, String>()
        map[result1] = "first"
        map[result2] = "second"

        // result1 and result2 are equal, so the map should have only 1 entry
        assertEquals(1, map.size)
        assertEquals("second", map[result1])
    }
}
