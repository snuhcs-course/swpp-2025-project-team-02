package com.example.fortuna_android.api

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for API Response data classes
 * Focus on line coverage - test instantiation and getters
 */
class ResponseDataClassTest {

    @Test
    fun testCollectionStatus_GetCount() {
        val collections = listOf(
            CollectionItem("wood", 5),
            CollectionItem("fire", 3),
            CollectionItem("earth", 0),
            CollectionItem("metal", 2),
            CollectionItem("water", 1)
        )
        val status = CollectionStatus(collections, 11)

        assertEquals(5, status.getCount("wood"))
        assertEquals(3, status.getCount("fire"))
        assertEquals(0, status.getCount("earth"))
        assertEquals(2, status.getCount("metal"))
        assertEquals(1, status.getCount("water"))
        assertEquals(0, status.getCount("unknown"))
    }

    @Test
    fun testCollectionStatus_HelperProperties() {
        val collections = listOf(
            CollectionItem("wood", 5),
            CollectionItem("fire", 3),
            CollectionItem("earth", 7),
            CollectionItem("metal", 2),
            CollectionItem("water", 1)
        )
        val status = CollectionStatus(collections, 18)

        assertEquals(5, status.wood)
        assertEquals(3, status.fire)
        assertEquals(7, status.earth)
        assertEquals(2, status.metal)
        assertEquals(1, status.water)
        assertEquals(18, status.totalCount)
    }

    @Test
    fun testCollectionStatus_CaseInsensitive() {
        val collections = listOf(CollectionItem("WOOD", 10))
        val status = CollectionStatus(collections, 10)

        assertEquals(10, status.getCount("wood"))
        assertEquals(10, status.getCount("Wood"))
        assertEquals(10, status.getCount("WOOD"))
    }

    @Test
    fun testCollectionItem_Instantiation() {
        val item = CollectionItem("fire", 5)

        assertEquals("fire", item.chakraType)
        assertEquals(5, item.count)
    }

    @Test
    fun testCollectedElements_Instantiation() {
        val elements = CollectedElements(1, 2, 3, 4, 5)

        assertEquals(1, elements.wood)
        assertEquals(2, elements.fire)
        assertEquals(3, elements.earth)
        assertEquals(4, elements.metal)
        assertEquals(5, elements.water)
    }

    @Test
    fun testCollectedElements_DefaultValues() {
        val elements = CollectedElements()

        assertEquals(0, elements.wood)
        assertEquals(0, elements.fire)
        assertEquals(0, elements.earth)
        assertEquals(0, elements.metal)
        assertEquals(0, elements.water)
    }

    @Test
    fun testCollectElementRequest_Instantiation() {
        val request = CollectElementRequest("fire", "2025-11-30")

        assertEquals("fire", request.chakraType)
        assertEquals("2025-11-30", request.date)
    }

    @Test
    fun testCollectElementResponse_Instantiation() {
        val data = CollectElementData("Success", CollectedElements(1, 1, 1, 1, 1))
        val response = CollectElementResponse("success", data)

        assertEquals("success", response.status)
        assertEquals("Success", response.data.message)
    }

    @Test
    fun testCollectElementData_Instantiation() {
        val elements = CollectedElements(2, 2, 2, 2, 2)
        val data = CollectElementData("Collected", elements)

        assertEquals("Collected", data.message)
        assertEquals(2, data.collectedElements.wood)
    }

    @Test
    fun testCollectionStatusResponse_Instantiation() {
        val elements = CollectedElements(3, 3, 3, 3, 3)
        val statusData = CollectionStatusData(elements)
        val response = CollectionStatusResponse("success", statusData)

        assertEquals("success", response.status)
        assertEquals(3, response.data.collectedElements.fire)
    }

    @Test
    fun testCollectionStatusData_Instantiation() {
        val elements = CollectedElements(4, 4, 4, 4, 4)
        val data = CollectionStatusData(elements)

        assertEquals(4, data.collectedElements.earth)
    }

    @Test
    fun testMultipleCollectionItems() {
        val items = listOf(
            CollectionItem("wood", 1),
            CollectionItem("fire", 2),
            CollectionItem("earth", 3),
            CollectionItem("metal", 4),
            CollectionItem("water", 5)
        )

        assertEquals(5, items.size)
        assertEquals("wood", items[0].chakraType)
        assertEquals(5, items[4].count)
    }
}
