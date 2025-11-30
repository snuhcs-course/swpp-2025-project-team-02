package com.example.fortuna_android.ui

import com.example.fortuna_android.api.ElementHistoryDay
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ElementHistoryAdapter
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ElementHistoryAdapterTest {

    @Test
    fun `test adapter instantiation`() {
        val adapter = ElementHistoryAdapter()
        assertNotNull(adapter)
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `test submitList updates item count`() {
        val adapter = ElementHistoryAdapter()

        val history = listOf(
            ElementHistoryDay("2023-10-01", 3),
            ElementHistoryDay("2023-10-02", 5),
            ElementHistoryDay("2023-10-03", 2)
        )

        adapter.submitList(history)
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `test submitList with empty list`() {
        val adapter = ElementHistoryAdapter()
        adapter.submitList(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `test submitList replaces previous data`() {
        val adapter = ElementHistoryAdapter()

        val firstHistory = listOf(
            ElementHistoryDay("2023-10-01", 3)
        )
        adapter.submitList(firstHistory)
        assertEquals(1, adapter.itemCount)

        val secondHistory = listOf(
            ElementHistoryDay("2023-10-01", 3),
            ElementHistoryDay("2023-10-02", 5)
        )
        adapter.submitList(secondHistory)
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `test ElementHistoryDay data class`() {
        val day = ElementHistoryDay("2023-10-15", 4)

        assertEquals("2023-10-15", day.date)
        assertEquals(4, day.collectedCount)
    }

    @Test
    fun `test ElementHistoryDay equality`() {
        val day1 = ElementHistoryDay("2023-10-15", 4)
        val day2 = ElementHistoryDay("2023-10-15", 4)

        assertEquals(day1, day2)
    }

    @Test
    fun `test ElementHistoryDay copy`() {
        val original = ElementHistoryDay("2023-10-15", 4)
        val copy = original.copy(collectedCount = 5)

        assertEquals(original.date, copy.date)
        assertEquals(5, copy.collectedCount)
    }
}
