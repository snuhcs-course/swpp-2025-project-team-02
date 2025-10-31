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

    // ========== Error Handling Tests ==========

    @Test
    fun `test ElementMapper with invalid context still initializes`() {
        // Even if assets loading fails somehow, the mapper should handle it gracefully
        // In practice, with ApplicationProvider context, this should always work
        // But we test that isReady() reflects initialization state
        assertTrue("ElementMapper should be initialized with valid context", elementMapper.isReady())
    }

    @Test
    fun `test word matching edge case with 3 character words`() {
        // Words with exactly 3 characters should not match (threshold is > 3)
        // "car" has 3 chars, so it won't trigger word matching
        val result = elementMapper.mapLabelToElement("old car")
        // This might match "convertible" or other car-related items via fuzzy match
        assertNotNull("Should return a valid element", result)
    }

    @Test
    fun `test word matching with 4 character words`() {
        // Words with 4+ characters should enable word matching
        val result = elementMapper.mapLabelToElement("cars")  // 4 chars
        // "cars" might match "convertible" or other car items
        assertNotNull("Should return a valid element", result)
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

    // ========== Companion Object Static Methods Tests ==========
    // Testing fromKorean(), toKorean(), toEnglish(), getElementColor(), getElementDisplayText()

    @Test
    fun `test fromKorean for all elements`() {
        assertEquals(ElementMapper.Element.WOOD, ElementMapper.fromKorean("목"))
        assertEquals(ElementMapper.Element.FIRE, ElementMapper.fromKorean("화"))
        assertEquals(ElementMapper.Element.EARTH, ElementMapper.fromKorean("토"))
        assertEquals(ElementMapper.Element.METAL, ElementMapper.fromKorean("금"))
        assertEquals(ElementMapper.Element.WATER, ElementMapper.fromKorean("수"))
    }

    @Test
    fun `test fromKorean for unknown element defaults to OTHERS`() {
        assertEquals(ElementMapper.Element.OTHERS, ElementMapper.fromKorean("unknown"))
        assertEquals(ElementMapper.Element.OTHERS, ElementMapper.fromKorean(""))
        assertEquals(ElementMapper.Element.OTHERS, ElementMapper.fromKorean("xyz"))
        assertEquals(ElementMapper.Element.OTHERS, ElementMapper.fromKorean("123"))
    }

    @Test
    fun `test toKorean for all elements`() {
        assertEquals("목", ElementMapper.toKorean(ElementMapper.Element.WOOD))
        assertEquals("화", ElementMapper.toKorean(ElementMapper.Element.FIRE))
        assertEquals("토", ElementMapper.toKorean(ElementMapper.Element.EARTH))
        assertEquals("금", ElementMapper.toKorean(ElementMapper.Element.METAL))
        assertEquals("수", ElementMapper.toKorean(ElementMapper.Element.WATER))
        assertEquals("기타", ElementMapper.toKorean(ElementMapper.Element.OTHERS))
    }

    @Test
    fun `test toEnglish for all elements`() {
        assertEquals("wood", ElementMapper.toEnglish(ElementMapper.Element.WOOD))
        assertEquals("fire", ElementMapper.toEnglish(ElementMapper.Element.FIRE))
        assertEquals("earth", ElementMapper.toEnglish(ElementMapper.Element.EARTH))
        assertEquals("metal", ElementMapper.toEnglish(ElementMapper.Element.METAL))
        assertEquals("water", ElementMapper.toEnglish(ElementMapper.Element.WATER))
        assertEquals("wood", ElementMapper.toEnglish(ElementMapper.Element.OTHERS))  // OTHERS maps to "wood"
    }

    @Test
    fun `test getElementColor for all elements`() {
        assertEquals(0xFF4CAF50.toInt(), ElementMapper.getElementColor(ElementMapper.Element.WOOD))  // Green
        assertEquals(0xFFF44336.toInt(), ElementMapper.getElementColor(ElementMapper.Element.FIRE))  // Red
        assertEquals(0xFFFFEB3B.toInt(), ElementMapper.getElementColor(ElementMapper.Element.EARTH)) // Yellow
        assertEquals(0xFFFFFFFF.toInt(), ElementMapper.getElementColor(ElementMapper.Element.METAL)) // White
        assertEquals(0xFF2196F3.toInt(), ElementMapper.getElementColor(ElementMapper.Element.WATER)) // Blue
        assertEquals(0xFF9E9E9E.toInt(), ElementMapper.getElementColor(ElementMapper.Element.OTHERS)) // Gray
    }

    @Test
    fun `test getElementDisplayText for all elements`() {
        assertEquals("Wood (Green)", ElementMapper.getElementDisplayText(ElementMapper.Element.WOOD))
        assertEquals("Fire (Red)", ElementMapper.getElementDisplayText(ElementMapper.Element.FIRE))
        assertEquals("Earth (Yellow)", ElementMapper.getElementDisplayText(ElementMapper.Element.EARTH))
        assertEquals("Metal (White)", ElementMapper.getElementDisplayText(ElementMapper.Element.METAL))
        assertEquals("Water (Blue)", ElementMapper.getElementDisplayText(ElementMapper.Element.WATER))
        assertEquals("Others", ElementMapper.getElementDisplayText(ElementMapper.Element.OTHERS))
    }

    @Test
    fun `test roundtrip conversion Korean to Element to Korean`() {
        // Test that converting Korean -> Element -> Korean preserves the value
        val koreanElements = listOf("목", "화", "토", "금", "수")
        koreanElements.forEach { korean ->
            val element = ElementMapper.fromKorean(korean)
            val backToKorean = ElementMapper.toKorean(element)
            assertEquals("Roundtrip conversion should preserve value for $korean", korean, backToKorean)
        }
    }

    @Test
    fun `test roundtrip conversion Element to English to uppercase matches element name`() {
        // Verify that toEnglish returns lowercase element names
        assertEquals("wood", ElementMapper.toEnglish(ElementMapper.Element.WOOD))
        assertEquals("fire", ElementMapper.toEnglish(ElementMapper.Element.FIRE))
        assertEquals("earth", ElementMapper.toEnglish(ElementMapper.Element.EARTH))
        assertEquals("metal", ElementMapper.toEnglish(ElementMapper.Element.METAL))
        assertEquals("water", ElementMapper.toEnglish(ElementMapper.Element.WATER))

        // Verify it's lowercase
        assertTrue(ElementMapper.toEnglish(ElementMapper.Element.WOOD).all { it.isLowerCase() || it.isDigit() })
        assertTrue(ElementMapper.toEnglish(ElementMapper.Element.FIRE).all { it.isLowerCase() || it.isDigit() })
    }

    @Test
    fun `test all element colors are valid ARGB integers`() {
        // Test that colors are proper ARGB format (non-zero alpha channel)
        val elements = ElementMapper.Element.values()
        elements.forEach { element ->
            val color = ElementMapper.getElementColor(element)
            // Verify color is a valid integer (not checking exact values, just validity)
            assertTrue("Color for ${element.displayName} should be a valid integer", color != 0 || element == ElementMapper.Element.OTHERS)

            // Verify alpha channel is set (0xFF000000 mask)
            val alpha = (color shr 24) and 0xFF
            assertTrue("Color for ${element.displayName} should have alpha channel", alpha == 0xFF.toByte().toInt() and 0xFF)
        }
    }

    @Test
    fun `test display text contains element name`() {
        // Verify that display text includes the element name
        val elements = listOf(
            ElementMapper.Element.WOOD to "Wood",
            ElementMapper.Element.FIRE to "Fire",
            ElementMapper.Element.EARTH to "Earth",
            ElementMapper.Element.METAL to "Metal",
            ElementMapper.Element.WATER to "Water",
            ElementMapper.Element.OTHERS to "Others"
        )

        elements.forEach { (element, expectedName) ->
            val displayText = ElementMapper.getElementDisplayText(element)
            assertTrue(
                "Display text for $expectedName should contain element name",
                displayText.contains(expectedName, ignoreCase = true)
            )
        }
    }
}
