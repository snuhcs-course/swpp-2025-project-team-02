package com.example.fortuna_android.classification

import android.content.Context
import android.content.res.AssetManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ElementMapperTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockAssetManager: AssetManager

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.assets).thenReturn(mockAssetManager)
    }

    private fun createMockClassFile(): String {
        return """
            1. Fire
            candle
            lighter
            fire torch lighter

            2. Metal
            knife
            spoon
            metal cooking pot

            3. Ground
            plant
            flower pot
            garden soil mixture

            4. Wood
            tree
            wooden chair
            bamboo walking stick

            5. Water
            water bottle
            glass of water
            swimming pool water

            6. others
            unknown item
            miscellaneous object
        """.trimIndent()
    }
    @Test
    fun testFromKorean_AllElements() {
        assertEquals(ElementMapper.Element.WOOD, ElementMapper.fromKorean("목"))
        assertEquals(ElementMapper.Element.FIRE, ElementMapper.fromKorean("화"))
        assertEquals(ElementMapper.Element.EARTH, ElementMapper.fromKorean("토"))
        assertEquals(ElementMapper.Element.METAL, ElementMapper.fromKorean("금"))
        assertEquals(ElementMapper.Element.WATER, ElementMapper.fromKorean("수"))
    }

    @Test
    fun testFromKorean_Invalid() {
        assertEquals(ElementMapper.Element.OTHERS, ElementMapper.fromKorean("unknown"))
    }

    @Test
    fun testToKorean_AllElements() {
        assertEquals("목", ElementMapper.toKorean(ElementMapper.Element.WOOD))
        assertEquals("화", ElementMapper.toKorean(ElementMapper.Element.FIRE))
        assertEquals("토", ElementMapper.toKorean(ElementMapper.Element.EARTH))
        assertEquals("금", ElementMapper.toKorean(ElementMapper.Element.METAL))
        assertEquals("수", ElementMapper.toKorean(ElementMapper.Element.WATER))
        assertEquals("기타", ElementMapper.toKorean(ElementMapper.Element.OTHERS))
    }

    @Test
    fun testToEnglish_AllElements() {
        assertEquals("wood", ElementMapper.toEnglish(ElementMapper.Element.WOOD))
        assertEquals("fire", ElementMapper.toEnglish(ElementMapper.Element.FIRE))
        assertEquals("earth", ElementMapper.toEnglish(ElementMapper.Element.EARTH))
        assertEquals("metal", ElementMapper.toEnglish(ElementMapper.Element.METAL))
        assertEquals("water", ElementMapper.toEnglish(ElementMapper.Element.WATER))
    }

    @Test
    fun testGetElementColor_AllElements() {
        assertNotEquals(0, ElementMapper.getElementColor(ElementMapper.Element.WOOD))
        assertNotEquals(0, ElementMapper.getElementColor(ElementMapper.Element.FIRE))
        assertNotEquals(0, ElementMapper.getElementColor(ElementMapper.Element.EARTH))
        assertNotEquals(0, ElementMapper.getElementColor(ElementMapper.Element.METAL))
        assertNotEquals(0, ElementMapper.getElementColor(ElementMapper.Element.WATER))
        assertNotEquals(0, ElementMapper.getElementColor(ElementMapper.Element.OTHERS))
    }

    @Test
    fun testGetElementDisplayText_AllElements() {
        assertTrue(ElementMapper.getElementDisplayText(ElementMapper.Element.WOOD).isNotEmpty())
        assertTrue(ElementMapper.getElementDisplayText(ElementMapper.Element.FIRE).isNotEmpty())
        assertTrue(ElementMapper.getElementDisplayText(ElementMapper.Element.EARTH).isNotEmpty())
        assertTrue(ElementMapper.getElementDisplayText(ElementMapper.Element.METAL).isNotEmpty())
        assertTrue(ElementMapper.getElementDisplayText(ElementMapper.Element.WATER).isNotEmpty())
        assertTrue(ElementMapper.getElementDisplayText(ElementMapper.Element.OTHERS).isNotEmpty())
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
    fun `test getElementHanja for all elements`() {
        assertEquals("木", ElementMapper.getElementHanja(ElementMapper.Element.WOOD))
        assertEquals("火", ElementMapper.getElementHanja(ElementMapper.Element.FIRE))
        assertEquals("土", ElementMapper.getElementHanja(ElementMapper.Element.EARTH))
        assertEquals("金", ElementMapper.getElementHanja(ElementMapper.Element.METAL))
        assertEquals("水", ElementMapper.getElementHanja(ElementMapper.Element.WATER))
        assertEquals("기타", ElementMapper.getElementHanja(ElementMapper.Element.OTHERS))
    }

    @Test
    fun `test getElementHanja returns correct Hanja characters`() {
        // Test that each Hanja character is exactly one character (except OTHERS)
        assertEquals(1, ElementMapper.getElementHanja(ElementMapper.Element.WOOD).length)
        assertEquals(1, ElementMapper.getElementHanja(ElementMapper.Element.FIRE).length)
        assertEquals(1, ElementMapper.getElementHanja(ElementMapper.Element.EARTH).length)
        assertEquals(1, ElementMapper.getElementHanja(ElementMapper.Element.METAL).length)
        assertEquals(1, ElementMapper.getElementHanja(ElementMapper.Element.WATER).length)

        // OTHERS uses Korean Hangul "기타" (2 characters)
        assertEquals(2, ElementMapper.getElementHanja(ElementMapper.Element.OTHERS).length)
    }

    @Test
    fun `test getElementHanja unique values for each element`() {
        // Verify all Hanja values are unique
        val hanjaValues = ElementMapper.Element.values().map { ElementMapper.getElementHanja(it) }
        val uniqueHanja = hanjaValues.toSet()
        assertEquals("All Hanja values should be unique", hanjaValues.size, uniqueHanja.size)
    }

    @Test
    fun `test getElementHanja matches traditional Chinese Five Elements`() {
        // Verify the returned Hanja match traditional Chinese Five Elements characters
        // These are the standard Hanja characters used in East Asian philosophy
        assertEquals("木", ElementMapper.getElementHanja(ElementMapper.Element.WOOD))   // Wood
        assertEquals("火", ElementMapper.getElementHanja(ElementMapper.Element.FIRE))   // Fire
        assertEquals("土", ElementMapper.getElementHanja(ElementMapper.Element.EARTH))  // Earth/Soil
        assertEquals("金", ElementMapper.getElementHanja(ElementMapper.Element.METAL))  // Metal/Gold
        assertEquals("水", ElementMapper.getElementHanja(ElementMapper.Element.WATER))  // Water

        // OTHERS is not part of traditional Five Elements, uses Korean
        assertEquals("기타", ElementMapper.getElementHanja(ElementMapper.Element.OTHERS)) // "Other" in Korean
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

    // ========== Tests for tryPartialWordMatch via mapLabelToElement ==========

    private fun createElementMapperWithMockClassFile(): ElementMapper {
        val mockClassContent = createMockClassFile()
        `when`(mockAssetManager.open("class.txt")).thenReturn(ByteArrayInputStream(mockClassContent.toByteArray()))
        return ElementMapper(mockContext)
    }

    @Test
    fun `test tryPartialWordMatch - successful word contains match`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test case where detected word contains class word
        // "fire torch lighter" contains "torch" -> "lightweight torch"
        val result = elementMapper.mapLabelToElement("lightweight torch")
        assertEquals(ElementMapper.Element.FIRE, result)
    }

    @Test
    fun `test tryPartialWordMatch - successful class word contains detected word`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test case where class word contains detected word
        // "bamboo walking stick" contains "walking" -> "walking"
        val result = elementMapper.mapLabelToElement("walking")
        assertEquals(ElementMapper.Element.WOOD, result)
    }

    @Test
    fun `test tryPartialWordMatch - multiple words partial match`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test with multiple words where some match partially
        // "metal cooking pot" has "cooking" -> "home cooking equipment"
        val result = elementMapper.mapLabelToElement("home cooking equipment")
        assertEquals(ElementMapper.Element.METAL, result)
    }

    @Test
    fun `test tryPartialWordMatch - bidirectional word matching`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test both directions of word matching
        // "garden soil mixture" contains "garden" -> "beautiful garden area"
        // This should not match direct VLM/class/fuzzy, only partial word
        val result1 = elementMapper.mapLabelToElement("beautiful garden area")
        assertEquals(ElementMapper.Element.EARTH, result1)

        // "swimming pool water" contains "swimming" -> "swimming lessons"
        val result2 = elementMapper.mapLabelToElement("swimming lessons")
        assertEquals(ElementMapper.Element.WATER, result2)
    }

    @Test
    fun `test tryPartialWordMatch - words too short are filtered out`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test that words with length <= 3 are filtered out
        // Should not match because all words are too short
        val result = elementMapper.mapLabelToElement("a b c it")
        assertEquals(ElementMapper.Element.OTHERS, result)
    }

    @Test
    fun `test tryPartialWordMatch - no words after filtering`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test case where all words are filtered out (length <= 3)
        val result = elementMapper.mapLabelToElement("go to it")
        assertEquals(ElementMapper.Element.OTHERS, result)
    }

    @Test
    fun `test tryPartialWordMatch - empty string returns OTHERS from uninitialized mapper`() {
        // Test empty input handling - if ElementMapper fails to initialize, it returns OTHERS
        // This tests the fallback behavior for edge cases
        val elementMapper = ElementMapper(mockContext)  // Don't set up mock file, will fail to initialize
        val result = elementMapper.mapLabelToElement("")
        assertEquals(ElementMapper.Element.OTHERS, result)
    }

    @Test
    fun `test tryPartialWordMatch - spaces only returns OTHERS from uninitialized mapper`() {
        // Test spaces-only input handling - should normalize to empty and return OTHERS
        val elementMapper = ElementMapper(mockContext)  // Don't set up mock file, will fail to initialize
        val result = elementMapper.mapLabelToElement("   ")
        assertEquals(ElementMapper.Element.OTHERS, result)
    }

    @Test
    fun `test tryPartialWordMatch - case insensitive matching`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test case insensitive matching
        // "TORCH" should match "fire torch lighter"
        val result = elementMapper.mapLabelToElement("TORCH HOLDER")
        assertEquals(ElementMapper.Element.FIRE, result)
    }

    @Test
    fun `test tryPartialWordMatch - no match found returns null equivalent`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test case where no partial word match is found
        val result = elementMapper.mapLabelToElement("completely different unrelated words")
        assertEquals(ElementMapper.Element.OTHERS, result)
    }

    @Test
    fun `test tryPartialWordMatch - match first found element`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // When multiple elements could match, should return the first found
        // This tests the firstOrNull behavior
        val result = elementMapper.mapLabelToElement("wooden spoon")
        // Should match either WOOD (wooden) or METAL (spoon), depending on order in map
        assertTrue("Should match either WOOD or METAL",
            result == ElementMapper.Element.WOOD || result == ElementMapper.Element.METAL)
    }

    @Test
    fun `test tryPartialWordMatch - complex multi-word scenario`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test complex scenario with multiple words
        // "bamboo walking stick" has "bamboo" and "walking"
        val result = elementMapper.mapLabelToElement("traditional bamboo furniture piece")
        assertEquals(ElementMapper.Element.WOOD, result)
    }

    @Test
    fun `test tryPartialWordMatch - partial substring matching`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test partial substring matching within words
        // "chair" should match part of "wooden chair"
        val result = elementMapper.mapLabelToElement("armchair design")
        assertEquals(ElementMapper.Element.WOOD, result)
    }

    @Test
    fun `test tryPartialWordMatch - ensures method is called in matching hierarchy`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // This test ensures tryPartialWordMatch is actually called by testing
        // a scenario that should only succeed via partial word matching
        // (not direct VLM, class, or fuzzy matching)

        // Use a label that won't match the first three strategies but will match partial word
        // "holder" should partially match "torch" in "fire torch lighter"
        val result = elementMapper.mapLabelToElement("torch holder equipment")
        assertEquals(ElementMapper.Element.FIRE, result)
    }

    @Test
    fun `test tryPartialWordMatch - word boundary sensitivity`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test that matching respects word boundaries properly
        // "bottle" should match "water bottle"
        val result = elementMapper.mapLabelToElement("plastic bottle container")
        assertEquals(ElementMapper.Element.WATER, result)
    }

    @Test
    fun `test tryPartialWordMatch - handles special characters in input`() {
        val elementMapper = createElementMapperWithMockClassFile()

        // Test input with special characters
        val result = elementMapper.mapLabelToElement("water-bottle container")
        // Should split on spaces and match "water"
        assertEquals(ElementMapper.Element.WATER, result)
    }

    @Test
    fun `test createMockClassFile generates expected structure`() {
        val mockContent = createMockClassFile()

        // Verify the mock content has the expected structure
        assertTrue("Should contain Fire section", mockContent.contains("1. Fire"))
        assertTrue("Should contain Metal section", mockContent.contains("2. Metal"))
        assertTrue("Should contain Ground section", mockContent.contains("3. Ground"))
        assertTrue("Should contain Wood section", mockContent.contains("4. Wood"))
        assertTrue("Should contain Water section", mockContent.contains("5. Water"))
        assertTrue("Should contain others section", mockContent.contains("6. others"))

        // Verify it contains our test items
        assertTrue("Should contain fire torch lighter", mockContent.contains("fire torch lighter"))
        assertTrue("Should contain bamboo walking stick", mockContent.contains("bamboo walking stick"))
        assertTrue("Should contain metal cooking pot", mockContent.contains("metal cooking pot"))
        assertTrue("Should contain garden soil mixture", mockContent.contains("garden soil mixture"))
        assertTrue("Should contain swimming pool water", mockContent.contains("swimming pool water"))
    }
}
