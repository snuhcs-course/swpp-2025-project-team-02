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
}
