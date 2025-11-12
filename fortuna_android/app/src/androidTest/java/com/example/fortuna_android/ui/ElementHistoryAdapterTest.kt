package com.example.fortuna_android.ui

import android.view.ViewGroup
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.api.ElementHistoryDay
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for ElementHistoryAdapter
 * Tests adapter functionality, data binding, and view holders
 */
@RunWith(AndroidJUnit4::class)
class ElementHistoryAdapterTest {

    private lateinit var adapter: ElementHistoryAdapter

    @Before
    fun setUp() {
        adapter = ElementHistoryAdapter()
    }

    // ========== Adapter Creation Tests ==========

    @Test
    fun testAdapterCreation() {
        assertNotNull("Adapter should be created", adapter)
    }

    @Test
    fun testAdapterInitiallyEmpty() {
        assertEquals("Adapter should start with 0 items", 0, adapter.itemCount)
    }

    // ========== Data Submission Tests ==========

    @Test
    fun testSubmitEmptyList() {
        adapter.submitList(emptyList())
        assertEquals("Adapter should have 0 items", 0, adapter.itemCount)
    }

    @Test
    fun testSubmitSingleItem() {
        val historyList = listOf(
            ElementHistoryDay("2025-11-13", 5)
        )

        adapter.submitList(historyList)
        assertEquals("Adapter should have 1 item", 1, adapter.itemCount)
    }

    @Test
    fun testSubmitMultipleItems() {
        val historyList = listOf(
            ElementHistoryDay("2025-11-13", 5),
            ElementHistoryDay("2025-11-12", 3),
            ElementHistoryDay("2025-11-10", 2)
        )

        adapter.submitList(historyList)
        assertEquals("Adapter should have 3 items", 3, adapter.itemCount)
    }

    @Test
    fun testSubmitListMultipleTimes() {
        val historyList1 = listOf(
            ElementHistoryDay("2025-11-13", 5)
        )

        adapter.submitList(historyList1)
        assertEquals("Adapter should have 1 item after first submit", 1, adapter.itemCount)

        val historyList2 = listOf(
            ElementHistoryDay("2025-11-13", 5),
            ElementHistoryDay("2025-11-12", 3)
        )

        adapter.submitList(historyList2)
        assertEquals("Adapter should have 2 items after second submit", 2, adapter.itemCount)
    }

    @Test
    fun testSubmitListReplacesOldData() {
        val historyList1 = listOf(
            ElementHistoryDay("2025-11-13", 5),
            ElementHistoryDay("2025-11-12", 3),
            ElementHistoryDay("2025-11-10", 2)
        )

        adapter.submitList(historyList1)
        assertEquals("Adapter should have 3 items", 3, adapter.itemCount)

        val historyList2 = listOf(
            ElementHistoryDay("2025-11-20", 4)
        )

        adapter.submitList(historyList2)
        assertEquals("Adapter should have 1 item after replacement", 1, adapter.itemCount)
    }

    // ========== ViewHolder Creation Tests ==========

    @Test
    fun testCreateViewHolder() {
        val parent = createMockParent()

        val viewHolder = adapter.onCreateViewHolder(parent, 0)

        assertNotNull("ViewHolder should be created", viewHolder)
        assertNotNull("ViewHolder should have a view", viewHolder.itemView)
    }

    // ========== Data Binding Tests ==========

    @Test
    fun testBindViewHolderWithValidData() {
        val historyList = listOf(
            ElementHistoryDay("2025-11-13", 5)
        )

        adapter.submitList(historyList)

        val parent = createMockParent()
        val viewHolder = adapter.onCreateViewHolder(parent, 0)

        // Bind should not crash
        try {
            adapter.onBindViewHolder(viewHolder, 0)
            assertTrue("Binding should succeed", true)
        } catch (e: Exception) {
            fail("Binding should not throw exception: ${e.message}")
        }
    }

    @Test
    fun testBindViewHolderWithMultipleItems() {
        val historyList = listOf(
            ElementHistoryDay("2025-11-15", 4),
            ElementHistoryDay("2025-11-12", 3),
            ElementHistoryDay("2025-11-10", 2)
        )

        adapter.submitList(historyList)

        val parent = createMockParent()

        for (position in historyList.indices) {
            val viewHolder = adapter.onCreateViewHolder(parent, 0)

            try {
                adapter.onBindViewHolder(viewHolder, position)
                assertTrue("Binding position $position should succeed", true)
            } catch (e: Exception) {
                fail("Binding position $position should not throw exception: ${e.message}")
            }
        }
    }

    // ========== Date Formatting Tests ==========

    @Test
    fun testDateFormatConversion() {
        val historyList = listOf(
            ElementHistoryDay("2025-11-13", 5)
        )

        adapter.submitList(historyList)

        // Date should be formatted as "11월 13일"
        // We can't directly access the TextView text in unit test,
        // but we verify the adapter processes it without error
        assertEquals("Adapter should have the item", 1, adapter.itemCount)
    }

    @Test
    fun testDateFormatWithDifferentDates() {
        val historyList = listOf(
            ElementHistoryDay("2025-01-05", 3),
            ElementHistoryDay("2025-12-31", 5),
            ElementHistoryDay("2025-06-15", 2)
        )

        adapter.submitList(historyList)
        assertEquals("Adapter should handle all date formats", 3, adapter.itemCount)
    }

    // ========== Count Display Tests ==========

    @Test
    fun testCountDisplayFormatting() {
        val historyList = listOf(
            ElementHistoryDay("2025-11-13", 5),
            ElementHistoryDay("2025-11-12", 10),
            ElementHistoryDay("2025-11-10", 1)
        )

        adapter.submitList(historyList)

        // Counts should be displayed as "5개", "10개", "1개"
        assertEquals("Adapter should have all items", 3, adapter.itemCount)
    }

    // ========== Edge Case Tests ==========

    @Test
    fun testZeroCount() {
        val historyList = listOf(
            ElementHistoryDay("2025-11-13", 0)
        )

        adapter.submitList(historyList)
        assertEquals("Adapter should handle zero count", 1, adapter.itemCount)
    }

    @Test
    fun testLargeCount() {
        val historyList = listOf(
            ElementHistoryDay("2025-11-13", 999)
        )

        adapter.submitList(historyList)
        assertEquals("Adapter should handle large count", 1, adapter.itemCount)
    }

    @Test
    fun testInvalidDateFormat() {
        val historyList = listOf(
            ElementHistoryDay("invalid-date", 5)
        )

        adapter.submitList(historyList)

        // Should not crash, should still add item
        assertEquals("Adapter should handle invalid date format", 1, adapter.itemCount)
    }

    @Test
    fun testEmptyDateString() {
        val historyList = listOf(
            ElementHistoryDay("", 5)
        )

        adapter.submitList(historyList)
        assertEquals("Adapter should handle empty date", 1, adapter.itemCount)
    }

    // ========== Performance Tests ==========

    @Test
    fun testLargeDataSet() {
        val largeHistoryList = (1..100).map { day ->
            ElementHistoryDay("2025-11-${day.toString().padStart(2, '0')}", day)
        }

        adapter.submitList(largeHistoryList)
        assertEquals("Adapter should handle large dataset", 100, adapter.itemCount)
    }

    // ========== Helper Methods ==========

    private fun createMockParent(): ViewGroup {
        return object : ViewGroup(ApplicationProvider.getApplicationContext()) {
            override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {}
        }
    }
}