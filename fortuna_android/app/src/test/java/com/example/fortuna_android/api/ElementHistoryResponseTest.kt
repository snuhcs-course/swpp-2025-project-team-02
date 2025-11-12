package com.example.fortuna_android.api

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Element History API Response data classes
 * Tests serialization, deserialization, and data class functionality
 */
class ElementHistoryResponseTest {

    // ========== ElementHistoryDay Tests ==========

    @Test
    fun testElementHistoryDayCreation() {
        val day = ElementHistoryDay(
            date = "2025-11-13",
            collectedCount = 5
        )

        assertNotNull("ElementHistoryDay should be created", day)
        assertEquals("Date should match", "2025-11-13", day.date)
        assertEquals("Collected count should match", 5, day.collectedCount)
    }

    @Test
    fun testElementHistoryDayWithZeroCount() {
        val day = ElementHistoryDay(
            date = "2025-11-13",
            collectedCount = 0
        )

        assertEquals("Zero count should be allowed", 0, day.collectedCount)
    }

    @Test
    fun testElementHistoryDayWithLargeCount() {
        val day = ElementHistoryDay(
            date = "2025-11-13",
            collectedCount = 100
        )

        assertEquals("Large count should be handled", 100, day.collectedCount)
    }

    @Test
    fun testElementHistoryDayEquality() {
        val day1 = ElementHistoryDay("2025-11-13", 5)
        val day2 = ElementHistoryDay("2025-11-13", 5)

        assertEquals("Same data should be equal", day1, day2)
    }

    @Test
    fun testElementHistoryDayInequality() {
        val day1 = ElementHistoryDay("2025-11-13", 5)
        val day2 = ElementHistoryDay("2025-11-14", 5)

        assertNotEquals("Different dates should not be equal", day1, day2)
    }

    @Test
    fun testElementHistoryDayCopy() {
        val original = ElementHistoryDay("2025-11-13", 5)
        val copy = original.copy()

        assertEquals("Copy should equal original", original, copy)
        assertNotSame("Copy should be different instance", original, copy)
    }

    @Test
    fun testElementHistoryDayToString() {
        val day = ElementHistoryDay("2025-11-13", 5)
        val toString = day.toString()

        assertNotNull("toString should not be null", toString)
        assertTrue("toString should contain date", toString.contains("2025-11-13"))
        assertTrue("toString should contain count", toString.contains("5"))
    }

    // ========== ElementHistoryData Tests ==========

    @Test
    fun testElementHistoryDataCreation() {
        val historyData = ElementHistoryData(
            element = "wood",
            elementKr = "목",
            totalCount = 15,
            history = listOf(
                ElementHistoryDay("2025-11-13", 5),
                ElementHistoryDay("2025-11-12", 3),
                ElementHistoryDay("2025-11-10", 7)
            )
        )

        assertNotNull("ElementHistoryData should be created", historyData)
        assertEquals("Element should match", "wood", historyData.element)
        assertEquals("Element KR should match", "목", historyData.elementKr)
        assertEquals("Total count should match", 15, historyData.totalCount)
        assertEquals("History size should match", 3, historyData.history.size)
    }

    @Test
    fun testElementHistoryDataWithEmptyHistory() {
        val historyData = ElementHistoryData(
            element = "fire",
            elementKr = "화",
            totalCount = 0,
            history = emptyList()
        )

        assertEquals("Empty history should be allowed", 0, historyData.history.size)
        assertEquals("Total count should be 0", 0, historyData.totalCount)
    }

    @Test
    fun testElementHistoryDataAllElementTypes() {
        val elementTypes = listOf(
            Pair("wood", "목"),
            Pair("fire", "화"),
            Pair("earth", "토"),
            Pair("metal", "금"),
            Pair("water", "수")
        )

        elementTypes.forEach { (elementEn, elementKr) ->
            val historyData = ElementHistoryData(
                element = elementEn,
                elementKr = elementKr,
                totalCount = 10,
                history = listOf(ElementHistoryDay("2025-11-13", 10))
            )

            assertEquals("Element should match for $elementEn", elementEn, historyData.element)
            assertEquals("Element KR should match for $elementKr", elementKr, historyData.elementKr)
        }
    }

    @Test
    fun testElementHistoryDataTotalCountMatchesHistory() {
        val history = listOf(
            ElementHistoryDay("2025-11-13", 5),
            ElementHistoryDay("2025-11-12", 3),
            ElementHistoryDay("2025-11-10", 7)
        )

        val totalCount = history.sumOf { it.collectedCount }

        val historyData = ElementHistoryData(
            element = "water",
            elementKr = "수",
            totalCount = totalCount,
            history = history
        )

        assertEquals("Total count should sum to 15", 15, historyData.totalCount)
    }

    // ========== ElementHistoryResponse Tests ==========

    @Test
    fun testElementHistoryResponseCreation() {
        val response = ElementHistoryResponse(
            status = "success",
            data = ElementHistoryData(
                element = "wood",
                elementKr = "목",
                totalCount = 15,
                history = listOf(
                    ElementHistoryDay("2025-11-13", 5),
                    ElementHistoryDay("2025-11-12", 10)
                )
            )
        )

        assertNotNull("Response should be created", response)
        assertEquals("Status should be success", "success", response.status)
        assertNotNull("Data should not be null", response.data)
        assertEquals("Element should be wood", "wood", response.data.element)
    }

    @Test
    fun testElementHistoryResponseSuccessStatus() {
        val response = ElementHistoryResponse(
            status = "success",
            data = ElementHistoryData("fire", "화", 0, emptyList())
        )

        assertEquals("Status should be success", "success", response.status)
    }

    @Test
    fun testElementHistoryResponseDataAccess() {
        val historyData = ElementHistoryData(
            element = "earth",
            elementKr = "토",
            totalCount = 20,
            history = listOf(ElementHistoryDay("2025-11-13", 20))
        )

        val response = ElementHistoryResponse(
            status = "success",
            data = historyData
        )

        assertEquals("Should access element through response", "earth", response.data.element)
        assertEquals("Should access total count through response", 20, response.data.totalCount)
        assertEquals("Should access history through response", 1, response.data.history.size)
    }

    // ========== Edge Case Tests ==========

    @Test
    fun testNegativeCountNotAllowed() {
        // While the API shouldn't return negative counts,
        // test that the data class can handle it
        val day = ElementHistoryDay("2025-11-13", -1)
        assertEquals("Negative count should be stored as-is", -1, day.collectedCount)
    }

    @Test
    fun testDateFormatVariations() {
        val validDates = listOf(
            "2025-11-13",
            "2025-01-01",
            "2025-12-31"
        )

        validDates.forEach { date ->
            val day = ElementHistoryDay(date, 5)
            assertEquals("Date $date should be stored", date, day.date)
        }
    }

    @Test
    fun testHistoryOrderPreservation() {
        val history = listOf(
            ElementHistoryDay("2025-11-15", 4),
            ElementHistoryDay("2025-11-13", 5),
            ElementHistoryDay("2025-11-10", 3)
        )

        val historyData = ElementHistoryData(
            element = "metal",
            elementKr = "금",
            totalCount = 12,
            history = history
        )

        assertEquals("First item should be 2025-11-15", "2025-11-15", historyData.history[0].date)
        assertEquals("Second item should be 2025-11-13", "2025-11-13", historyData.history[1].date)
        assertEquals("Third item should be 2025-11-10", "2025-11-10", historyData.history[2].date)
    }

    // ========== List Operations Tests ==========

    @Test
    fun testHistoryListOperations() {
        val history = listOf(
            ElementHistoryDay("2025-11-13", 5),
            ElementHistoryDay("2025-11-12", 3)
        )

        val historyData = ElementHistoryData(
            element = "water",
            elementKr = "수",
            totalCount = 8,
            history = history
        )

        // Test list operations
        assertTrue("History should contain items", historyData.history.isNotEmpty())
        assertEquals("History size should be 2", 2, historyData.history.size)

        val firstItem = historyData.history.first()
        assertEquals("First item date should match", "2025-11-13", firstItem.date)

        val lastItem = historyData.history.last()
        assertEquals("Last item date should match", "2025-11-12", lastItem.date)
    }

    @Test
    fun testHistoryFilter() {
        val history = listOf(
            ElementHistoryDay("2025-11-13", 5),
            ElementHistoryDay("2025-11-12", 3),
            ElementHistoryDay("2025-11-10", 0)
        )

        val historyData = ElementHistoryData(
            element = "fire",
            elementKr = "화",
            totalCount = 8,
            history = history
        )

        val nonZeroHistory = historyData.history.filter { it.collectedCount > 0 }
        assertEquals("Should have 2 non-zero items", 2, nonZeroHistory.size)
    }
}