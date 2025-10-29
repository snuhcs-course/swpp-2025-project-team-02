package com.example.fortuna_android.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PendingCollectionManagerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PendingCollectionManager.clearPendingCollection(context)
    }

    @After
    fun tearDown() {
        PendingCollectionManager.clearPendingCollection(context)
    }

    @Test
    fun testSavePendingCollection() {
        PendingCollectionManager.savePendingCollection(context, "fire", 3)

        assertTrue(PendingCollectionManager.hasPendingCollection(context))
    }

    @Test
    fun testGetPendingCollection() {
        PendingCollectionManager.savePendingCollection(context, "water", 5)

        val pending = PendingCollectionManager.getPendingCollection(context)

        assertNotNull(pending)
        assertEquals("water", pending?.first)
        assertEquals(5, pending?.second)
    }

    @Test
    fun testClearPendingCollection() {
        PendingCollectionManager.savePendingCollection(context, "earth", 2)
        PendingCollectionManager.clearPendingCollection(context)

        assertFalse(PendingCollectionManager.hasPendingCollection(context))
    }

    @Test
    fun testHasPendingCollection_Initially() {
        assertFalse(PendingCollectionManager.hasPendingCollection(context))
    }

    @Test
    fun testGetPendingCollection_WhenNone() {
        val pending = PendingCollectionManager.getPendingCollection(context)

        assertNull(pending)
    }

    @Test
    fun testSavePendingCollection_DefaultCount() {
        PendingCollectionManager.savePendingCollection(context, "metal")

        val pending = PendingCollectionManager.getPendingCollection(context)

        assertEquals(1, pending?.second)
    }

    @Test
    fun testSavePendingCollection_MultipleElements() {
        PendingCollectionManager.savePendingCollection(context, "wood", 7)
        val pending1 = PendingCollectionManager.getPendingCollection(context)
        assertEquals("wood", pending1?.first)
        assertEquals(7, pending1?.second)

        // Overwrite with new element
        PendingCollectionManager.savePendingCollection(context, "fire", 3)
        val pending2 = PendingCollectionManager.getPendingCollection(context)
        assertEquals("fire", pending2?.first)
        assertEquals(3, pending2?.second)
    }
}
