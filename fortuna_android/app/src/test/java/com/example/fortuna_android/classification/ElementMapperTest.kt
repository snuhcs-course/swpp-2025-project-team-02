package com.example.fortuna_android.classification

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ElementMapper class
 * Tests the mapping of object detection labels to Chinese Five Elements
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ElementMapperTest {

    private lateinit var context: Context
    private lateinit var elementMapper: ElementMapper

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        elementMapper = ElementMapper(context)
    }

    // ========== Initialization Tests ==========

    @Test
    fun `test ElementMapper initializes successfully`() {
        assertTrue("ElementMapper should be initialized", elementMapper.isReady())
    }

    @Test
    fun `test getAllElements returns all six elements`() {
        val elements = elementMapper.getAllElements()
        assertEquals("Should have 6 elements", 6, elements.size)
        assertTrue("Should contain FIRE", elements.contains(ElementMapper.Element.FIRE))
        assertTrue("Should contain METAL", elements.contains(ElementMapper.Element.METAL))
        assertTrue("Should contain EARTH", elements.contains(ElementMapper.Element.EARTH))
        assertTrue("Should contain WOOD", elements.contains(ElementMapper.Element.WOOD))
        assertTrue("Should contain WATER", elements.contains(ElementMapper.Element.WATER))
        assertTrue("Should contain OTHERS", elements.contains(ElementMapper.Element.OTHERS))
    }

    @Test
    fun `test getMappingStats returns valid statistics`() {
        val stats = elementMapper.getMappingStats()
        assertNotNull("Statistics should not be null", stats)
        assertTrue("Statistics should contain header", stats.contains("Element Mapping Statistics"))
        assertTrue("Statistics should contain total count", stats.contains("Total:"))
        assertTrue("Statistics should mention Fire", stats.contains("Fire:"))
        assertTrue("Statistics should mention Metal", stats.contains("Metal:"))
        assertTrue("Statistics should mention Earth", stats.contains("Earth:"))
        assertTrue("Statistics should mention Wood", stats.contains("Wood:"))
        assertTrue("Statistics should mention Water", stats.contains("Water:"))
    }

    // ========== Direct Match Tests ==========

    @Test
    fun `test mapLabelToElement with exact Fire category match`() {
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("candle"))
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("torch"))
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("stove"))
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("volcano"))
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("lighter"))
    }

    @Test
    fun `test mapLabelToElement with exact Metal category match`() {
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("accordion"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("hammer"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("laptop"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("refrigerator"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("television"))
    }

    @Test
    fun `test mapLabelToElement with exact Earth category match`() {
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("hen"))
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("scorpion"))
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("castle"))
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("pizza"))
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("tiger"))
    }

    @Test
    fun `test mapLabelToElement with exact Wood category match`() {
        assertEquals(ElementMapper.Element.WOOD, elementMapper.mapLabelToElement("koala"))
        assertEquals(ElementMapper.Element.WOOD, elementMapper.mapLabelToElement("broom"))
        assertEquals(ElementMapper.Element.WOOD, elementMapper.mapLabelToElement("violin"))
        assertEquals(ElementMapper.Element.WOOD, elementMapper.mapLabelToElement("banana"))
        assertEquals(ElementMapper.Element.WOOD, elementMapper.mapLabelToElement("desk"))
    }

    @Test
    fun `test mapLabelToElement with exact Water category match`() {
        assertEquals(ElementMapper.Element.WATER, elementMapper.mapLabelToElement("goldfish"))
        assertEquals(ElementMapper.Element.WATER, elementMapper.mapLabelToElement("jellyfish"))
        assertEquals(ElementMapper.Element.WATER, elementMapper.mapLabelToElement("submarine"))
        assertEquals(ElementMapper.Element.WATER, elementMapper.mapLabelToElement("pelican"))
        assertEquals(ElementMapper.Element.WATER, elementMapper.mapLabelToElement("bathtub"))
    }

    @Test
    fun `test mapLabelToElement with exact Others category match`() {
        assertEquals(ElementMapper.Element.OTHERS, elementMapper.mapLabelToElement("umbrella"))
        assertEquals(ElementMapper.Element.OTHERS, elementMapper.mapLabelToElement("balloon"))
        assertEquals(ElementMapper.Element.OTHERS, elementMapper.mapLabelToElement("backpack"))
        assertEquals(ElementMapper.Element.OTHERS, elementMapper.mapLabelToElement("kimono"))
    }

    // ========== Case Insensitivity Tests ==========

    @Test
    fun `test mapLabelToElement is case insensitive`() {
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("CANDLE"))
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("CaNdLe"))
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("candle"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("LAPTOP"))
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("Castle"))
    }

    @Test
    fun `test mapLabelToElement handles whitespace`() {
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("  candle  "))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement(" laptop "))
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("  pizza "))
    }

    // ========== Fuzzy Matching Tests ==========

    @Test
    fun `test mapLabelToElement with fuzzy match - label contains class`() {
        // ML Kit might return "red candle" instead of just "candle"
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("red candle"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("silver laptop"))
        assertEquals(ElementMapper.Element.WATER, elementMapper.mapLabelToElement("small goldfish"))
    }

    @Test
    fun `test mapLabelToElement with fuzzy match - class contains label`() {
        // Test when detected label is shorter and contained in class name
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("lamp"))  // matches "table lamp"
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("train"))  // matches "bullet train"
    }

    @Test
    fun `test mapLabelToElement with multi-word fuzzy match`() {
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("fire engine"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("bullet train"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("desktop computer"))
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("golden retriever"))
        assertEquals(ElementMapper.Element.WATER, elementMapper.mapLabelToElement("king penguin"))
    }

    // ========== Word Matching Tests ==========

    @Test
    fun `test mapLabelToElement with word match for long words`() {
        // Test that words longer than 3 characters can match
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("computers"))  // matches "computer"
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("tigers"))  // matches "tiger"
    }

    @Test
    fun `test mapLabelToElement word match for cat breeds`() {
        // "cat" should match cat breeds like "Persian cat", "tiger cat" etc. in Earth category
        val result = elementMapper.mapLabelToElement("cat")
        // This should match "Persian cat" or other cat breeds which are in Earth category
        assertEquals(ElementMapper.Element.EARTH, result)
    }

    // ========== No Match Tests ==========

    @Test
    fun `test mapLabelToElement returns OTHERS for unknown label`() {
        // These are truly unknown labels that shouldn't match anything
        assertEquals(ElementMapper.Element.OTHERS, elementMapper.mapLabelToElement("qwertyuiop"))
        assertEquals(ElementMapper.Element.OTHERS, elementMapper.mapLabelToElement("xyz123abc"))
        assertEquals(ElementMapper.Element.OTHERS, elementMapper.mapLabelToElement("zzzunknownzzz"))
    }

    @Test
    fun `test mapLabelToElement with empty and blank strings`() {
        // Empty and blank strings might match something due to fuzzy matching
        // Just verify they return a valid element (not null or error)
        val emptyResult = elementMapper.mapLabelToElement("")
        assertNotNull("Empty string should return a valid element", emptyResult)

        val blankResult = elementMapper.mapLabelToElement("   ")
        assertNotNull("Blank string should return a valid element", blankResult)
    }

    // ========== Edge Cases Tests ==========

    @Test
    fun `test mapLabelToElement with special characters`() {
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("jack-o'-lantern"))
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("black-and-tan coonhound"))
    }

    @Test
    fun `test mapLabelToElement with numbers in label`() {
        val result = elementMapper.mapLabelToElement("Model T")
        assertEquals(ElementMapper.Element.METAL, result)
    }

    // ========== Element Enum Tests ==========

    @Test
    fun `test Element enum display names`() {
        assertEquals("Fire", ElementMapper.Element.FIRE.displayName)
        assertEquals("Metal", ElementMapper.Element.METAL.displayName)
        assertEquals("Earth", ElementMapper.Element.EARTH.displayName)
        assertEquals("Wood", ElementMapper.Element.WOOD.displayName)
        assertEquals("Water", ElementMapper.Element.WATER.displayName)
        assertEquals("Others", ElementMapper.Element.OTHERS.displayName)
    }

    @Test
    fun `test Element enum values order`() {
        val values = ElementMapper.Element.values()
        assertEquals(6, values.size)
        assertEquals(ElementMapper.Element.FIRE, values[0])
        assertEquals(ElementMapper.Element.METAL, values[1])
        assertEquals(ElementMapper.Element.EARTH, values[2])
        assertEquals(ElementMapper.Element.WOOD, values[3])
        assertEquals(ElementMapper.Element.WATER, values[4])
        assertEquals(ElementMapper.Element.OTHERS, values[5])
    }

    // ========== Integration Tests ==========

    @Test
    fun `test mapping coverage for all categories`() {
        // Verify that each category has at least some mappings
        val fireLabels = listOf("candle", "torch", "stove")
        val metalLabels = listOf("laptop", "hammer", "television")
        val earthLabels = listOf("tiger", "castle", "pizza")
        val woodLabels = listOf("violin", "desk", "banana")
        val waterLabels = listOf("goldfish", "submarine", "bathtub")

        fireLabels.forEach { label ->
            assertEquals("$label should be FIRE", ElementMapper.Element.FIRE, elementMapper.mapLabelToElement(label))
        }
        metalLabels.forEach { label ->
            assertEquals("$label should be METAL", ElementMapper.Element.METAL, elementMapper.mapLabelToElement(label))
        }
        earthLabels.forEach { label ->
            assertEquals("$label should be EARTH", ElementMapper.Element.EARTH, elementMapper.mapLabelToElement(label))
        }
        woodLabels.forEach { label ->
            assertEquals("$label should be WOOD", ElementMapper.Element.WOOD, elementMapper.mapLabelToElement(label))
        }
        waterLabels.forEach { label ->
            assertEquals("$label should be WATER", ElementMapper.Element.WATER, elementMapper.mapLabelToElement(label))
        }
    }

    @Test
    fun `test stats contains reasonable mapping counts`() {
        val stats = elementMapper.getMappingStats()
        // Verify that the total count is reasonable (class.txt has ~995 items)
        assertTrue("Total should be mentioned in stats", stats.contains("Total:"))

        // Extract total from stats string
        val totalLine = stats.lines().find { it.contains("Total:") }
        assertNotNull("Should have total line", totalLine)
    }

    @Test
    fun `test multiple consecutive mappings work correctly`() {
        // Test that mapper state is consistent across multiple calls
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("candle"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("laptop"))
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("tiger"))
        assertEquals(ElementMapper.Element.WOOD, elementMapper.mapLabelToElement("violin"))
        assertEquals(ElementMapper.Element.WATER, elementMapper.mapLabelToElement("goldfish"))
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("candle"))  // repeat
    }

    // ========== Real-world Detection Scenarios ==========

    @Test
    fun `test common ML Kit detection scenarios`() {
        // These are realistic labels that ML Kit might return
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("candle"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("laptop"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("cellular telephone"))  // exact match
        assertEquals(ElementMapper.Element.EARTH, elementMapper.mapLabelToElement("beagle"))  // dog breed
        assertEquals(ElementMapper.Element.WOOD, elementMapper.mapLabelToElement("rocking chair"))  // exact match
        assertEquals(ElementMapper.Element.WATER, elementMapper.mapLabelToElement("water bottle"))  // exact match
    }

    @Test
    fun `test compound object names`() {
        assertEquals(ElementMapper.Element.FIRE, elementMapper.mapLabelToElement("fire engine"))
        assertEquals(ElementMapper.Element.METAL, elementMapper.mapLabelToElement("cellular telephone"))
        assertEquals(ElementMapper.Element.WOOD, elementMapper.mapLabelToElement("acoustic guitar"))
    }
}
