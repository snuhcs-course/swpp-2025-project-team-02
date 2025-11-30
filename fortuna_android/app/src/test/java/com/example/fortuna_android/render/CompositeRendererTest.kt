package com.example.fortuna_android.render

import com.example.fortuna_android.common.samplerender.SampleRender
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for CompositeRenderer - Composite Pattern implementation
 * Tests composite functionality with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CompositeRendererTest {

    private lateinit var compositeRenderer: CompositeRenderer
    private lateinit var mockRender: SampleRender
    private lateinit var renderContext: RenderContext

    private class TestLeafComponent(private val name: String) : RenderComponent() {
        var initCalled = false
        var drawCalled = false
        var releaseCalled = false

        override fun getName(): String = name
        override fun getDescription(): String = "$name description"

        override fun onSurfaceCreated(render: SampleRender) {
            initCalled = true
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
        compositeRenderer = CompositeRenderer("Test Composite", "Test composite renderer")
        mockRender = mockk(relaxed = true)
        renderContext = RenderContext(
            viewMatrix = FloatArray(16),
            projectionMatrix = FloatArray(16),
            viewProjectionMatrix = FloatArray(16)
        )
    }

    // ========== Constructor and Basic Properties Tests ==========

    @Test
    fun `test constructor sets name and description`() {
        // Arrange
        val composite = CompositeRenderer("My Composite", "My Description")

        // Assert
        assertEquals("My Composite", composite.getName())
        assertEquals("My Description", composite.getDescription())
    }

    @Test
    fun `test initial child count is zero`() {
        // Assert
        assertEquals(0, compositeRenderer.getChildCount())
    }

    // ========== Add Component Tests ==========

    @Test
    fun `test add single component`() {
        // Arrange
        val leaf = TestLeafComponent("Leaf1")

        // Act
        compositeRenderer.add(leaf)

        // Assert
        assertEquals(1, compositeRenderer.getChildCount())
    }

    @Test
    fun `test add multiple components`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        val leaf3 = TestLeafComponent("Leaf3")

        // Act
        compositeRenderer.add(leaf1)
        compositeRenderer.add(leaf2)
        compositeRenderer.add(leaf3)

        // Assert
        assertEquals(3, compositeRenderer.getChildCount())
    }

    @Test
    fun `test add nested composite`() {
        // Arrange
        val nestedComposite = CompositeRenderer("Nested", "Nested composite")
        val leaf = TestLeafComponent("Leaf")
        nestedComposite.add(leaf)

        // Act
        compositeRenderer.add(nestedComposite)

        // Assert
        assertEquals(1, compositeRenderer.getChildCount())
        assertEquals(1, nestedComposite.getChildCount())
    }

    // ========== Remove Component Tests ==========

    @Test
    fun `test remove component`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        compositeRenderer.add(leaf1)
        compositeRenderer.add(leaf2)

        // Act
        compositeRenderer.remove(leaf1)

        // Assert
        assertEquals(1, compositeRenderer.getChildCount())
    }

    @Test
    fun `test remove all components`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        compositeRenderer.add(leaf1)
        compositeRenderer.add(leaf2)

        // Act
        compositeRenderer.remove(leaf1)
        compositeRenderer.remove(leaf2)

        // Assert
        assertEquals(0, compositeRenderer.getChildCount())
    }

    @Test
    fun `test remove non-existent component does not throw`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        compositeRenderer.add(leaf1)

        // Act - Should not throw
        compositeRenderer.remove(leaf2)

        // Assert
        assertEquals(1, compositeRenderer.getChildCount())
    }

    // ========== Get Child Tests ==========

    @Test
    fun `test getChild returns correct component`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        compositeRenderer.add(leaf1)
        compositeRenderer.add(leaf2)

        // Act
        val child0 = compositeRenderer.getChild(0)
        val child1 = compositeRenderer.getChild(1)

        // Assert
        assertEquals("Leaf1", child0.getName())
        assertEquals("Leaf2", child1.getName())
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `test getChild with invalid index throws exception`() {
        // Arrange
        val leaf = TestLeafComponent("Leaf1")
        compositeRenderer.add(leaf)

        // Act
        compositeRenderer.getChild(10)  // Invalid index
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun `test getChild with negative index throws exception`() {
        // Act
        compositeRenderer.getChild(-1)  // Invalid index
    }

    // ========== onSurfaceCreated Tests ==========

    @Test
    fun `test onSurfaceCreated calls all children`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        val leaf3 = TestLeafComponent("Leaf3")
        compositeRenderer.add(leaf1)
        compositeRenderer.add(leaf2)
        compositeRenderer.add(leaf3)

        // Act
        compositeRenderer.onSurfaceCreated(mockRender)

        // Assert
        assertTrue(leaf1.initCalled)
        assertTrue(leaf2.initCalled)
        assertTrue(leaf3.initCalled)
    }

    @Test
    fun `test onSurfaceCreated on empty composite does not throw`() {
        // Act & Assert - Should not throw
        compositeRenderer.onSurfaceCreated(mockRender)
        assertTrue(true)
    }

    @Test
    fun `test onSurfaceCreated on nested composite`() {
        // Arrange
        val nestedComposite = CompositeRenderer("Nested", "Nested")
        val leaf = TestLeafComponent("Leaf")
        nestedComposite.add(leaf)
        compositeRenderer.add(nestedComposite)

        // Act
        compositeRenderer.onSurfaceCreated(mockRender)

        // Assert
        assertTrue(leaf.initCalled)
    }

    // ========== draw Tests ==========

    @Test
    fun `test draw calls all children`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        compositeRenderer.add(leaf1)
        compositeRenderer.add(leaf2)

        // Act
        compositeRenderer.draw(mockRender, renderContext)

        // Assert
        assertTrue(leaf1.drawCalled)
        assertTrue(leaf2.drawCalled)
    }

    @Test
    fun `test draw on empty composite does not throw`() {
        // Act & Assert - Should not throw
        compositeRenderer.draw(mockRender, renderContext)
        assertTrue(true)
    }

    @Test
    fun `test draw continues on child exception`() {
        // Arrange
        val failingComponent = mockk<RenderComponent>(relaxed = true)
        every { failingComponent.draw(any(), any()) } throws RuntimeException("Test exception")

        val successComponent = TestLeafComponent("Success")

        compositeRenderer.add(failingComponent)
        compositeRenderer.add(successComponent)

        // Act - Should not throw, should continue to next component
        compositeRenderer.draw(mockRender, renderContext)

        // Assert
        verify { failingComponent.draw(any(), any()) }
        assertTrue(successComponent.drawCalled)
    }

    @Test
    fun `test draw on nested composite`() {
        // Arrange
        val nestedComposite = CompositeRenderer("Nested", "Nested")
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        nestedComposite.add(leaf1)
        nestedComposite.add(leaf2)
        compositeRenderer.add(nestedComposite)

        // Act
        compositeRenderer.draw(mockRender, renderContext)

        // Assert
        assertTrue(leaf1.drawCalled)
        assertTrue(leaf2.drawCalled)
    }

    // ========== release Tests ==========

    @Test
    fun `test release calls all children`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        compositeRenderer.add(leaf1)
        compositeRenderer.add(leaf2)

        // Act
        compositeRenderer.release()

        // Assert
        assertTrue(leaf1.releaseCalled)
        assertTrue(leaf2.releaseCalled)
    }

    @Test
    fun `test release clears children list`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        compositeRenderer.add(leaf1)
        compositeRenderer.add(leaf2)

        // Act
        compositeRenderer.release()

        // Assert
        assertEquals(0, compositeRenderer.getChildCount())
    }

    @Test
    fun `test release on empty composite does not throw`() {
        // Act & Assert - Should not throw
        compositeRenderer.release()
        assertTrue(true)
    }

    @Test
    fun `test release continues on child exception`() {
        // Arrange
        val failingComponent = mockk<RenderComponent>(relaxed = true)
        every { failingComponent.release() } throws RuntimeException("Test exception")

        val successComponent = TestLeafComponent("Success")

        compositeRenderer.add(failingComponent)
        compositeRenderer.add(successComponent)

        // Act - Should not throw
        compositeRenderer.release()

        // Assert
        verify { failingComponent.release() }
        assertTrue(successComponent.releaseCalled)
        assertEquals(0, compositeRenderer.getChildCount())
    }

    // ========== printTree Tests ==========

    @Test
    fun `test printTree does not throw`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        compositeRenderer.add(leaf1)
        compositeRenderer.add(leaf2)

        // Act & Assert - Should not throw
        compositeRenderer.printTree()
        assertTrue(true)
    }

    @Test
    fun `test printTree with nested composite`() {
        // Arrange
        val nestedComposite = CompositeRenderer("Nested", "Nested composite")
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        nestedComposite.add(leaf1)
        compositeRenderer.add(nestedComposite)
        compositeRenderer.add(leaf2)

        // Act & Assert - Should not throw
        compositeRenderer.printTree()
        assertTrue(true)
    }

    @Test
    fun `test printTree with custom indent`() {
        // Arrange
        val leaf = TestLeafComponent("Leaf")
        compositeRenderer.add(leaf)

        // Act & Assert - Should not throw
        compositeRenderer.printTree("  ")
        assertTrue(true)
    }

    @Test
    fun `test printTree on empty composite`() {
        // Act & Assert - Should not throw
        compositeRenderer.printTree()
        assertTrue(true)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test complex composite tree structure`() {
        // Arrange - Build tree structure
        val root = CompositeRenderer("Root", "Root composite")
        val branch1 = CompositeRenderer("Branch1", "First branch")
        val branch2 = CompositeRenderer("Branch2", "Second branch")
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        val leaf3 = TestLeafComponent("Leaf3")
        val leaf4 = TestLeafComponent("Leaf4")

        branch1.add(leaf1)
        branch1.add(leaf2)
        branch2.add(leaf3)
        branch2.add(leaf4)
        root.add(branch1)
        root.add(branch2)

        // Act
        root.onSurfaceCreated(mockRender)
        root.draw(mockRender, renderContext)
        root.release()

        // Assert
        assertTrue(leaf1.initCalled && leaf1.drawCalled && leaf1.releaseCalled)
        assertTrue(leaf2.initCalled && leaf2.drawCalled && leaf2.releaseCalled)
        assertTrue(leaf3.initCalled && leaf3.drawCalled && leaf3.releaseCalled)
        assertTrue(leaf4.initCalled && leaf4.drawCalled && leaf4.releaseCalled)
        assertEquals(0, root.getChildCount())
    }

    @Test
    fun `test add and remove operations maintain correct count`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        val leaf2 = TestLeafComponent("Leaf2")
        val leaf3 = TestLeafComponent("Leaf3")

        // Act & Assert
        compositeRenderer.add(leaf1)
        assertEquals(1, compositeRenderer.getChildCount())

        compositeRenderer.add(leaf2)
        assertEquals(2, compositeRenderer.getChildCount())

        compositeRenderer.add(leaf3)
        assertEquals(3, compositeRenderer.getChildCount())

        compositeRenderer.remove(leaf2)
        assertEquals(2, compositeRenderer.getChildCount())

        compositeRenderer.remove(leaf1)
        assertEquals(1, compositeRenderer.getChildCount())

        compositeRenderer.remove(leaf3)
        assertEquals(0, compositeRenderer.getChildCount())
    }

    @Test
    fun `test getChildCount after release`() {
        // Arrange
        compositeRenderer.add(TestLeafComponent("Leaf1"))
        compositeRenderer.add(TestLeafComponent("Leaf2"))

        // Act
        compositeRenderer.release()

        // Assert
        assertEquals(0, compositeRenderer.getChildCount())
    }

    @Test
    fun `test composite can be reused after release`() {
        // Arrange
        val leaf1 = TestLeafComponent("Leaf1")
        compositeRenderer.add(leaf1)
        compositeRenderer.release()

        // Act - Add new component after release
        val leaf2 = TestLeafComponent("Leaf2")
        compositeRenderer.add(leaf2)

        // Assert
        assertEquals(1, compositeRenderer.getChildCount())
        assertFalse(leaf2.initCalled)

        compositeRenderer.onSurfaceCreated(mockRender)
        assertTrue(leaf2.initCalled)
    }
}
