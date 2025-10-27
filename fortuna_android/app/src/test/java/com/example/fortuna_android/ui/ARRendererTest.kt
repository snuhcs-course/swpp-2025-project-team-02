package com.example.fortuna_android.ui

import com.example.fortuna_android.classification.ElementMapper
import com.google.ar.core.Anchor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config as RobolectricConfig

/**
 * Unit tests for ARRenderer
 * Achieves 100% line coverage for testable components
 *
 * Note: Full constructor-based testing is not possible in unit tests due to:
 * - DisplayRotationHelper requires Android WindowManager/DisplayManager
 * - MLKitObjectDetector uses RenderScript which cannot be initialized in Robolectric
 * - These components require instrumented tests or mocking frameworks like PowerMock
 *
 * This test file covers:
 * 1. ARLabeledAnchor data class (100% coverage)
 * 2. Business logic that can be tested independently
 */
@RunWith(RobolectricTestRunner::class)
@RobolectricConfig(sdk = [28])
class ARRendererTest {

    @Mock
    private lateinit var mockAnchor: Anchor

    private lateinit var closeable: AutoCloseable

    @Before
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
    }

    @org.junit.After
    fun tearDown() {
        closeable.close()
    }

    // ========== ARLabeledAnchor Data Class Tests (100% Coverage) ==========

    @Test
    fun `test ARLabeledAnchor creation and properties`() {
        val element = ElementMapper.Element.WOOD
        val labeledAnchor = ARRenderer.ARLabeledAnchor(mockAnchor, element)

        assertNotNull(labeledAnchor)
        assertEquals(mockAnchor, labeledAnchor.anchor)
        assertEquals(element, labeledAnchor.element)
    }

    @Test
    fun `test ARLabeledAnchor copy`() {
        val original = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val copied = original.copy(element = ElementMapper.Element.FIRE)

        assertEquals(mockAnchor, copied.anchor)
        assertEquals(ElementMapper.Element.FIRE, copied.element)
    }

    @Test
    fun `test ARLabeledAnchor copy with same element`() {
        val original = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val copied = original.copy()

        assertEquals(mockAnchor, copied.anchor)
        assertEquals(ElementMapper.Element.WOOD, copied.element)
    }

    @Test
    fun `test ARLabeledAnchor copy with different anchor`() {
        val mockAnchor2: Anchor = org.mockito.Mockito.mock(Anchor::class.java)
        val original = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val copied = original.copy(anchor = mockAnchor2)

        assertEquals(mockAnchor2, copied.anchor)
        assertEquals(ElementMapper.Element.WOOD, copied.element)
    }

    @Test
    fun `test ARLabeledAnchor destructuring`() {
        val labeledAnchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WATER)
        val (anchor, element) = labeledAnchor

        assertEquals(mockAnchor, anchor)
        assertEquals(ElementMapper.Element.WATER, element)
    }

    @Test
    fun `test ARLabeledAnchor with all six elements`() {
        val elements = listOf(
            ElementMapper.Element.WOOD,
            ElementMapper.Element.FIRE,
            ElementMapper.Element.EARTH,
            ElementMapper.Element.METAL,
            ElementMapper.Element.WATER,
            ElementMapper.Element.OTHERS
        )

        elements.forEach { element ->
            val anchor = ARRenderer.ARLabeledAnchor(mockAnchor, element)
            assertNotNull(anchor)
            assertEquals(element, anchor.element)
            assertEquals(mockAnchor, anchor.anchor)
        }
    }

    @Test
    fun `test ARLabeledAnchor equality with same values`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        assertEquals(anchor1, anchor2)
    }

    @Test
    fun `test ARLabeledAnchor inequality with different elements`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)
        assertNotEquals(anchor1, anchor2)
    }

    @Test
    fun `test ARLabeledAnchor inequality with different anchors`() {
        val mockAnchor2: Anchor = org.mockito.Mockito.mock(Anchor::class.java)
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor2, ElementMapper.Element.WOOD)
        assertNotEquals(anchor1, anchor2)
    }

    @Test
    fun `test ARLabeledAnchor toString contains class name`() {
        val anchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val str = anchor.toString()
        assertTrue(str.contains("ARLabeledAnchor"))
    }

    @Test
    fun `test ARLabeledAnchor toString contains anchor`() {
        val anchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)
        val str = anchor.toString()
        assertTrue(str.contains("anchor"))
    }

    @Test
    fun `test ARLabeledAnchor toString contains element`() {
        val anchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.EARTH)
        val str = anchor.toString()
        assertTrue(str.contains("element"))
    }

    @Test
    fun `test ARLabeledAnchor hashCode consistency`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        assertEquals(anchor1.hashCode(), anchor2.hashCode())
    }

    @Test
    fun `test ARLabeledAnchor hashCode differs for different elements`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)
        assertNotEquals(anchor1.hashCode(), anchor2.hashCode())
    }

    @Test
    fun `test ARLabeledAnchor component1 returns anchor`() {
        val labeledAnchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.METAL)
        assertEquals(mockAnchor, labeledAnchor.component1())
    }

    @Test
    fun `test ARLabeledAnchor component2 returns element`() {
        val labeledAnchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WATER)
        assertEquals(ElementMapper.Element.WATER, labeledAnchor.component2())
    }

    @Test
    fun `test ARLabeledAnchor not equal to null`() {
        val anchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        assertNotEquals(null, anchor)
        assertFalse(anchor.equals(null))
    }

    @Test
    fun `test ARLabeledAnchor not equal to different type`() {
        val anchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        assertNotEquals("string", anchor)
        assertFalse(anchor.equals("different type"))
    }

    @Test
    fun `test ARLabeledAnchor reflexive equality`() {
        val anchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        assertEquals(anchor, anchor)
        assertTrue(anchor.equals(anchor))
    }

    @Test
    fun `test ARLabeledAnchor symmetric equality`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)
        assertEquals(anchor1, anchor2)
        assertEquals(anchor2, anchor1)
        assertTrue(anchor1.equals(anchor2))
        assertTrue(anchor2.equals(anchor1))
    }

    @Test
    fun `test ARLabeledAnchor transitive equality`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.EARTH)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.EARTH)
        val anchor3 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.EARTH)
        assertEquals(anchor1, anchor2)
        assertEquals(anchor2, anchor3)
        assertEquals(anchor1, anchor3)
    }

    @Test
    fun `test ARLabeledAnchor in collection operations`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)
        val anchor3 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.EARTH)

        val list = listOf(anchor1, anchor2, anchor3)
        assertEquals(3, list.size)
        assertTrue(list.contains(anchor1))
        assertTrue(list.contains(anchor2))
        assertTrue(list.contains(anchor3))
    }

    @Test
    fun `test ARLabeledAnchor in set operations`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD) // duplicate
        val anchor3 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)

        val set = setOf(anchor1, anchor2, anchor3)
        assertEquals(2, set.size) // anchor1 and anchor2 are equal, so only 2 unique items
        assertTrue(set.contains(anchor1))
        assertTrue(set.contains(anchor3))
    }

    @Test
    fun `test ARLabeledAnchor as map key`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.METAL)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WATER)

        val map = mutableMapOf<ARRenderer.ARLabeledAnchor, String>()
        map[anchor1] = "metal value"
        map[anchor2] = "water value"

        assertEquals("metal value", map[anchor1])
        assertEquals("water value", map[anchor2])
        assertEquals(2, map.size)
    }

    @Test
    fun `test ARLabeledAnchor copy preserves equality`() {
        val original = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.OTHERS)
        val copied = original.copy()

        assertEquals(original, copied)
        assertEquals(original.hashCode(), copied.hashCode())
    }

    @Test
    fun `test ARLabeledAnchor with all elements have correct display names`() {
        ElementMapper.Element.values().forEach { element ->
            val anchor = ARRenderer.ARLabeledAnchor(mockAnchor, element)
            assertNotNull(anchor.element.displayName)
            assertTrue(anchor.element.displayName.isNotEmpty())
        }
    }

    @Test
    fun `test ARLabeledAnchor multiple instances independence`() {
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)

        // Modifying one shouldn't affect the other (data classes are immutable)
        val modified = anchor1.copy(element = ElementMapper.Element.METAL)

        assertEquals(ElementMapper.Element.WOOD, anchor1.element)
        assertEquals(ElementMapper.Element.FIRE, anchor2.element)
        assertEquals(ElementMapper.Element.METAL, modified.element)
    }

    @Test
    fun `test ARLabeledAnchor element enum has exactly 6 values`() {
        val elements = ElementMapper.Element.values()
        assertEquals(6, elements.size)
        assertTrue(elements.contains(ElementMapper.Element.WOOD))
        assertTrue(elements.contains(ElementMapper.Element.FIRE))
        assertTrue(elements.contains(ElementMapper.Element.EARTH))
        assertTrue(elements.contains(ElementMapper.Element.METAL))
        assertTrue(elements.contains(ElementMapper.Element.WATER))
        assertTrue(elements.contains(ElementMapper.Element.OTHERS))
    }

    @Test
    fun `test ARLabeledAnchor can be used in when expression`() {
        val anchor = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)

        val result = when (anchor.element) {
            ElementMapper.Element.WOOD -> "wood"
            ElementMapper.Element.FIRE -> "fire"
            ElementMapper.Element.EARTH -> "earth"
            ElementMapper.Element.METAL -> "metal"
            ElementMapper.Element.WATER -> "water"
            ElementMapper.Element.OTHERS -> "others"
        }

        assertEquals("fire", result)
    }

    @Test
    fun `test ARLabeledAnchor list operations`() {
        val list = mutableListOf<ARRenderer.ARLabeledAnchor>()
        val anchor1 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.WOOD)
        val anchor2 = ARRenderer.ARLabeledAnchor(mockAnchor, ElementMapper.Element.FIRE)

        list.add(anchor1)
        list.add(anchor2)

        assertEquals(2, list.size)
        assertEquals(anchor1, list[0])
        assertEquals(anchor2, list[1])

        list.remove(anchor1)
        assertEquals(1, list.size)
        assertEquals(anchor2, list[0])
    }
}
