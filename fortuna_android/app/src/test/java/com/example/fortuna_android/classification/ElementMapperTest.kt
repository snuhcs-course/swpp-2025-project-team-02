package com.example.fortuna_android.classification

import org.junit.Assert.*
import org.junit.Test

class ElementMapperTest {
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
