package com.example.fortuna_android.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for PendingCollectionManager
 * Tests SharedPreferences-based pending collection management
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PendingCollectionManagerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear any existing pending collections before each test
        PendingCollectionManager.clearPendingCollection(context)
    }

    // ========== savePendingCollection() Tests ==========

    @Test
    fun `test savePendingCollection with valid data`() {
        val element = "fire"
        val count = 1

        PendingCollectionManager.savePendingCollection(context, element, count)

        // Verify data was saved
        assertTrue("Should have pending collection", PendingCollectionManager.hasPendingCollection(context))

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNotNull("Pending data should not be null", pendingData)
        assertEquals("Element should match", element, pendingData!!.first)
        assertEquals("Count should match", count, pendingData.second)
    }

    @Test
    fun `test savePendingCollection with all elements`() {
        val elements = listOf("fire", "water", "earth", "metal", "wood")

        elements.forEach { element ->
            // Clear before each test
            PendingCollectionManager.clearPendingCollection(context)

            PendingCollectionManager.savePendingCollection(context, element, 1)

            val pendingData = PendingCollectionManager.getPendingCollection(context)
            assertNotNull("Pending data should not be null for $element", pendingData)
            assertEquals("Element should be $element", element, pendingData!!.first)
        }
    }

    @Test
    fun `test savePendingCollection with different counts`() {
        val counts = listOf(1, 5, 10, 100)

        counts.forEach { count ->
            PendingCollectionManager.clearPendingCollection(context)

            PendingCollectionManager.savePendingCollection(context, "water", count)

            val pendingData = PendingCollectionManager.getPendingCollection(context)
            assertEquals("Count should be $count", count, pendingData!!.second)
        }
    }

    @Test
    fun `test savePendingCollection overwrites existing data`() {
        // Save first collection
        PendingCollectionManager.savePendingCollection(context, "fire", 1)

        // Save second collection (should overwrite)
        PendingCollectionManager.savePendingCollection(context, "water", 5)

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNotNull("Pending data should not be null", pendingData)
        assertEquals("Element should be water", "water", pendingData!!.first)
        assertEquals("Count should be 5", 5, pendingData.second)
    }

    @Test
    fun `test savePendingCollection with default count`() {
        PendingCollectionManager.savePendingCollection(context, "earth")

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNotNull("Pending data should not be null", pendingData)
        assertEquals("Element should be earth", "earth", pendingData!!.first)
        assertEquals("Default count should be 1", 1, pendingData.second)
    }

    // ========== hasPendingCollection() Tests ==========

    @Test
    fun `test hasPendingCollection returns false initially`() {
        assertFalse("Should not have pending collection initially",
            PendingCollectionManager.hasPendingCollection(context))
    }

    @Test
    fun `test hasPendingCollection returns true after save`() {
        PendingCollectionManager.savePendingCollection(context, "metal", 1)

        assertTrue("Should have pending collection after save",
            PendingCollectionManager.hasPendingCollection(context))
    }

    @Test
    fun `test hasPendingCollection returns false after clear`() {
        PendingCollectionManager.savePendingCollection(context, "wood", 1)
        assertTrue("Should have pending collection",
            PendingCollectionManager.hasPendingCollection(context))

        PendingCollectionManager.clearPendingCollection(context)

        assertFalse("Should not have pending collection after clear",
            PendingCollectionManager.hasPendingCollection(context))
    }

    // ========== getPendingCollection() Tests ==========

    @Test
    fun `test getPendingCollection returns null when no data`() {
        val pendingData = PendingCollectionManager.getPendingCollection(context)

        assertNull("Should return null when no pending collection", pendingData)
    }

    @Test
    fun `test getPendingCollection returns correct data`() {
        val element = "fire"
        val count = 3

        PendingCollectionManager.savePendingCollection(context, element, count)

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNotNull("Pending data should not be null", pendingData)
        assertEquals("Element should match", element, pendingData!!.first)
        assertEquals("Count should match", count, pendingData.second)
    }

    @Test
    fun `test getPendingCollection multiple times returns same data`() {
        PendingCollectionManager.savePendingCollection(context, "water", 2)

        val firstCall = PendingCollectionManager.getPendingCollection(context)
        val secondCall = PendingCollectionManager.getPendingCollection(context)
        val thirdCall = PendingCollectionManager.getPendingCollection(context)

        assertEquals("First and second call should return same data", firstCall, secondCall)
        assertEquals("Second and third call should return same data", secondCall, thirdCall)
    }

    // ========== clearPendingCollection() Tests ==========

    @Test
    fun `test clearPendingCollection removes data`() {
        PendingCollectionManager.savePendingCollection(context, "earth", 1)
        assertTrue("Should have pending collection",
            PendingCollectionManager.hasPendingCollection(context))

        PendingCollectionManager.clearPendingCollection(context)

        assertFalse("Should not have pending collection after clear",
            PendingCollectionManager.hasPendingCollection(context))
        assertNull("getPendingCollection should return null after clear",
            PendingCollectionManager.getPendingCollection(context))
    }

    @Test
    fun `test clearPendingCollection is idempotent`() {
        PendingCollectionManager.savePendingCollection(context, "metal", 1)

        // Clear multiple times - should not throw error
        PendingCollectionManager.clearPendingCollection(context)
        PendingCollectionManager.clearPendingCollection(context)
        PendingCollectionManager.clearPendingCollection(context)

        assertFalse("Should still not have pending collection",
            PendingCollectionManager.hasPendingCollection(context))
    }

    @Test
    fun `test clearPendingCollection on empty state`() {
        // Clear when nothing is stored - should not throw error
        PendingCollectionManager.clearPendingCollection(context)

        assertFalse("Should not have pending collection",
            PendingCollectionManager.hasPendingCollection(context))
    }

    // ========== Integration Tests ==========

    @Test
    fun `test complete workflow - save, check, get, clear`() {
        // Initially no pending collection
        assertFalse("Should not have pending collection initially",
            PendingCollectionManager.hasPendingCollection(context))

        // Save collection
        PendingCollectionManager.savePendingCollection(context, "wood", 5)

        // Check it exists
        assertTrue("Should have pending collection after save",
            PendingCollectionManager.hasPendingCollection(context))

        // Get and verify data
        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNotNull("Pending data should not be null", pendingData)
        assertEquals("Element should be wood", "wood", pendingData!!.first)
        assertEquals("Count should be 5", 5, pendingData.second)

        // Clear collection
        PendingCollectionManager.clearPendingCollection(context)

        // Verify it's cleared
        assertFalse("Should not have pending collection after clear",
            PendingCollectionManager.hasPendingCollection(context))
        assertNull("getPendingCollection should return null after clear",
            PendingCollectionManager.getPendingCollection(context))
    }

    @Test
    fun `test multiple save and clear cycles`() {
        repeat(5) { i ->
            // Save
            PendingCollectionManager.savePendingCollection(context, "fire", i + 1)
            assertTrue("Should have pending collection in cycle $i",
                PendingCollectionManager.hasPendingCollection(context))

            val pendingData = PendingCollectionManager.getPendingCollection(context)
            assertEquals("Count should be ${i + 1}", i + 1, pendingData!!.second)

            // Clear
            PendingCollectionManager.clearPendingCollection(context)
            assertFalse("Should not have pending collection after clear in cycle $i",
                PendingCollectionManager.hasPendingCollection(context))
        }
    }

    // ========== Edge Case Tests ==========

    @Test
    fun `test savePendingCollection with empty string element`() {
        PendingCollectionManager.savePendingCollection(context, "", 1)

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNotNull("Pending data should not be null", pendingData)
        assertEquals("Element should be empty string", "", pendingData!!.first)
    }

    @Test
    fun `test savePendingCollection with zero count`() {
        // Zero count is invalid - getPendingCollection should return null (count > 0 validation)
        PendingCollectionManager.savePendingCollection(context, "water", 0)

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNull("Pending data should be null for count = 0 (invalid)", pendingData)
    }

    @Test
    fun `test savePendingCollection with negative count`() {
        // Negative count is invalid - getPendingCollection should return null (count > 0 validation)
        PendingCollectionManager.savePendingCollection(context, "earth", -1)

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNull("Pending data should be null for count = -1 (invalid)", pendingData)
    }

    @Test
    fun `test savePendingCollection with very large count`() {
        val largeCount = Int.MAX_VALUE
        PendingCollectionManager.savePendingCollection(context, "metal", largeCount)

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNotNull("Pending data should not be null", pendingData)
        assertEquals("Count should be Int.MAX_VALUE", largeCount, pendingData!!.second)
    }

    @Test
    fun `test savePendingCollection with special characters in element`() {
        val specialElement = "fire!@#$%^&*()"
        PendingCollectionManager.savePendingCollection(context, specialElement, 1)

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNotNull("Pending data should not be null", pendingData)
        assertEquals("Element should match with special characters", specialElement, pendingData!!.first)
    }

    @Test
    fun `test savePendingCollection with Korean element name`() {
        val koreanElement = "불"
        PendingCollectionManager.savePendingCollection(context, koreanElement, 1)

        val pendingData = PendingCollectionManager.getPendingCollection(context)
        assertNotNull("Pending data should not be null", pendingData)
        assertEquals("Element should match with Korean characters", koreanElement, pendingData!!.first)
    }

    // ========== Persistence Tests ==========

    @Test
    fun `test data persists across multiple context instances`() {
        // Save with first context instance
        PendingCollectionManager.savePendingCollection(context, "wood", 3)

        // Get new context instance
        val newContext = ApplicationProvider.getApplicationContext<Context>()

        // Data should still be accessible
        assertTrue("Should have pending collection in new context",
            PendingCollectionManager.hasPendingCollection(newContext))

        val pendingData = PendingCollectionManager.getPendingCollection(newContext)
        assertNotNull("Pending data should not be null in new context", pendingData)
        assertEquals("Element should be wood", "wood", pendingData!!.first)
        assertEquals("Count should be 3", 3, pendingData.second)
    }
}