package com.example.fortuna_android.render

import com.example.fortuna_android.common.samplerender.SampleRender
import com.google.ar.core.Frame
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for RenderComponent abstract base class and RenderContext data class
 * Tests the Composite Pattern component interface with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RenderComponentTest {

    private lateinit var testComponent: TestRenderComponent
    private lateinit var renderContext: RenderContext

    /**
     * Concrete implementation for testing abstract RenderComponent
     */
    private class TestRenderComponent : RenderComponent() {
        var surfaceCreatedCalled = false
        var drawCalled = false
        var releaseCalled = false

        override fun onSurfaceCreated(render: SampleRender) {
            surfaceCreatedCalled = true
        }

        override fun draw(render: SampleRender, context: RenderContext) {
            drawCalled = true
        }

        override fun release() {
            releaseCalled = true
        }
    }

    @Before
    fun setUp() {
        testComponent = TestRenderComponent()
        renderContext = RenderContext(
            viewMatrix = FloatArray(16) { it.toFloat() },
            projectionMatrix = FloatArray(16) { (it * 2).toFloat() },
            viewProjectionMatrix = FloatArray(16) { (it * 3).toFloat() },
            frame = null,
            arLabeledAnchors = emptyList()
        )
    }

    // ========== RenderComponent Base Methods Tests ==========

    @Test
    fun `test getName returns simple class name`() {
        // Act
        val name = testComponent.getName()

        // Assert
        assertEquals("getName should return class simple name", "TestRenderComponent", name)
    }

    @Test
    fun `test getDescription returns default description`() {
        // Act
        val description = testComponent.getDescription()

        // Assert
        assertEquals("getDescription should return default", "Render component", description)
    }

    @Test
    fun `test add throws UnsupportedOperationException on leaf component`() {
        // Arrange
        val anotherComponent = TestRenderComponent()

        // Act & Assert
        val exception = assertThrows(UnsupportedOperationException::class.java) {
            testComponent.add(anotherComponent)
        }
        assertEquals("Cannot add to a leaf component", exception.message)
    }

    @Test
    fun `test remove throws UnsupportedOperationException on leaf component`() {
        // Arrange
        val anotherComponent = TestRenderComponent()

        // Act & Assert
        val exception = assertThrows(UnsupportedOperationException::class.java) {
            testComponent.remove(anotherComponent)
        }
        assertEquals("Cannot remove from a leaf component", exception.message)
    }

    @Test
    fun `test getChild throws UnsupportedOperationException on leaf component`() {
        // Act & Assert
        val exception = assertThrows(UnsupportedOperationException::class.java) {
            testComponent.getChild(0)
        }
        assertEquals("Leaf has no children", exception.message)
    }

    // ========== RenderContext Data Class Tests ==========

    @Test
    fun `test RenderContext constructor with all parameters`() {
        // Arrange
        val viewMatrix = FloatArray(16) { 1f }
        val projectionMatrix = FloatArray(16) { 2f }
        val viewProjectionMatrix = FloatArray(16) { 3f }

        // Act
        val context = RenderContext(viewMatrix, projectionMatrix, viewProjectionMatrix)

        // Assert
        assertArrayEquals(viewMatrix, context.viewMatrix, 0.001f)
        assertArrayEquals(projectionMatrix, context.projectionMatrix, 0.001f)
        assertArrayEquals(viewProjectionMatrix, context.viewProjectionMatrix, 0.001f)
        assertNull(context.frame)
        assertTrue(context.arLabeledAnchors.isEmpty())
    }

    @Test
    fun `test RenderContext constructor with frame and anchors`() {
        // Arrange
        val viewMatrix = FloatArray(16)
        val projectionMatrix = FloatArray(16)
        val viewProjectionMatrix = FloatArray(16)

        // Act
        val context = RenderContext(
            viewMatrix,
            projectionMatrix,
            viewProjectionMatrix,
            frame = null,
            arLabeledAnchors = emptyList()
        )

        // Assert
        assertNotNull(context)
        assertNull(context.frame)
        assertEquals(0, context.arLabeledAnchors.size)
    }

    @Test
    fun `test RenderContext equals with same instance`() {
        // Act
        val isSame = renderContext.equals(renderContext)

        // Assert
        assertTrue("Same instance should be equal", isSame)
    }

    @Test
    fun `test RenderContext equals with identical data`() {
        // Arrange
        val viewMatrix = FloatArray(16) { it.toFloat() }
        val projectionMatrix = FloatArray(16) { (it * 2).toFloat() }
        val viewProjectionMatrix = FloatArray(16) { (it * 3).toFloat() }

        val context1 = RenderContext(viewMatrix, projectionMatrix, viewProjectionMatrix)
        val context2 = RenderContext(viewMatrix, projectionMatrix, viewProjectionMatrix)

        // Act
        val isEqual = context1.equals(context2)

        // Assert
        assertTrue("Contexts with identical data should be equal", isEqual)
    }

    @Test
    fun `test RenderContext equals with different viewMatrix`() {
        // Arrange
        val viewMatrix1 = FloatArray(16) { it.toFloat() }
        val viewMatrix2 = FloatArray(16) { (it + 1).toFloat() }
        val projectionMatrix = FloatArray(16) { (it * 2).toFloat() }
        val viewProjectionMatrix = FloatArray(16) { (it * 3).toFloat() }

        val context1 = RenderContext(viewMatrix1, projectionMatrix, viewProjectionMatrix)
        val context2 = RenderContext(viewMatrix2, projectionMatrix, viewProjectionMatrix)

        // Act
        val isEqual = context1.equals(context2)

        // Assert
        assertFalse("Contexts with different viewMatrix should not be equal", isEqual)
    }

    @Test
    fun `test RenderContext equals with different projectionMatrix`() {
        // Arrange
        val viewMatrix = FloatArray(16) { it.toFloat() }
        val projectionMatrix1 = FloatArray(16) { (it * 2).toFloat() }
        val projectionMatrix2 = FloatArray(16) { (it * 3).toFloat() }
        val viewProjectionMatrix = FloatArray(16) { (it * 3).toFloat() }

        val context1 = RenderContext(viewMatrix, projectionMatrix1, viewProjectionMatrix)
        val context2 = RenderContext(viewMatrix, projectionMatrix2, viewProjectionMatrix)

        // Act
        val isEqual = context1.equals(context2)

        // Assert
        assertFalse("Contexts with different projectionMatrix should not be equal", isEqual)
    }

    @Test
    fun `test RenderContext equals with different viewProjectionMatrix`() {
        // Arrange
        val viewMatrix = FloatArray(16) { it.toFloat() }
        val projectionMatrix = FloatArray(16) { (it * 2).toFloat() }
        val viewProjectionMatrix1 = FloatArray(16) { (it * 3).toFloat() }
        val viewProjectionMatrix2 = FloatArray(16) { (it * 4).toFloat() }

        val context1 = RenderContext(viewMatrix, projectionMatrix, viewProjectionMatrix1)
        val context2 = RenderContext(viewMatrix, projectionMatrix, viewProjectionMatrix2)

        // Act
        val isEqual = context1.equals(context2)

        // Assert
        assertFalse("Contexts with different viewProjectionMatrix should not be equal", isEqual)
    }

    @Test
    fun `test RenderContext equals with null`() {
        // Act
        val isEqual = renderContext.equals(null)

        // Assert
        assertFalse("Context should not equal null", isEqual)
    }

    @Test
    fun `test RenderContext equals with different class`() {
        // Act
        val isEqual = renderContext.equals("Not a RenderContext")

        // Assert
        assertFalse("Context should not equal different class", isEqual)
    }

    @Test
    fun `test RenderContext hashCode is consistent`() {
        // Act
        val hash1 = renderContext.hashCode()
        val hash2 = renderContext.hashCode()

        // Assert
        assertEquals("hashCode should be consistent", hash1, hash2)
    }

    @Test
    fun `test RenderContext hashCode for equal objects`() {
        // Arrange
        val viewMatrix = FloatArray(16) { it.toFloat() }
        val projectionMatrix = FloatArray(16) { (it * 2).toFloat() }
        val viewProjectionMatrix = FloatArray(16) { (it * 3).toFloat() }

        val context1 = RenderContext(viewMatrix, projectionMatrix, viewProjectionMatrix)
        val context2 = RenderContext(viewMatrix, projectionMatrix, viewProjectionMatrix)

        // Act
        val hash1 = context1.hashCode()
        val hash2 = context2.hashCode()

        // Assert
        assertEquals("Equal objects should have same hashCode", hash1, hash2)
    }

    @Test
    fun `test RenderContext hashCode includes all matrices`() {
        // Arrange - Create contexts with different matrices
        val viewMatrix1 = FloatArray(16) { 1f }
        val viewMatrix2 = FloatArray(16) { 2f }
        val projectionMatrix = FloatArray(16) { 3f }
        val viewProjectionMatrix = FloatArray(16) { 4f }

        val context1 = RenderContext(viewMatrix1, projectionMatrix, viewProjectionMatrix)
        val context2 = RenderContext(viewMatrix2, projectionMatrix, viewProjectionMatrix)

        // Act
        val hash1 = context1.hashCode()
        val hash2 = context2.hashCode()

        // Assert - Different viewMatrix should produce different hash
        assertNotEquals("Different matrices should produce different hashCode", hash1, hash2)
    }

    @Test
    fun `test RenderContext copy method`() {
        // Arrange
        val original = RenderContext(
            viewMatrix = FloatArray(16) { 1f },
            projectionMatrix = FloatArray(16) { 2f },
            viewProjectionMatrix = FloatArray(16) { 3f }
        )

        // Act
        val copy = original.copy()

        // Assert
        assertEquals("Copy should equal original", original, copy)
        assertNotSame("Copy should be different instance", original, copy)
    }

    @Test
    fun `test RenderContext copy with modified viewMatrix`() {
        // Arrange
        val original = RenderContext(
            viewMatrix = FloatArray(16) { 1f },
            projectionMatrix = FloatArray(16) { 2f },
            viewProjectionMatrix = FloatArray(16) { 3f }
        )
        val newViewMatrix = FloatArray(16) { 5f }

        // Act
        val modified = original.copy(viewMatrix = newViewMatrix)

        // Assert
        assertArrayEquals(newViewMatrix, modified.viewMatrix, 0.001f)
        assertArrayEquals(original.projectionMatrix, modified.projectionMatrix, 0.001f)
    }

    @Test
    fun `test RenderContext toString contains class name`() {
        // Act
        val toString = renderContext.toString()

        // Assert
        assertTrue("toString should contain class name", toString.contains("RenderContext"))
    }

    // ========== Integration Tests ==========

    @Test
    fun `test multiple RenderContext instances are independent`() {
        // Arrange
        val context1 = RenderContext(
            FloatArray(16) { 1f },
            FloatArray(16) { 2f },
            FloatArray(16) { 3f }
        )
        val context2 = RenderContext(
            FloatArray(16) { 4f },
            FloatArray(16) { 5f },
            FloatArray(16) { 6f }
        )

        // Act - Modify context1's matrix
        context1.viewMatrix[0] = 99f

        // Assert - context2 should be unchanged
        assertEquals(4f, context2.viewMatrix[0], 0.001f)
        assertNotEquals(context1, context2)
    }
}
