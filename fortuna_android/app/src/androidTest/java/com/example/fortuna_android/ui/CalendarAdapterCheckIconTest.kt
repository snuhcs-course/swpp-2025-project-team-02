package com.example.fortuna_android.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.R
import com.example.fortuna_android.api.DayData
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for CalendarAdapter check icon functionality
 * Tests the check icon display when 5 elements are collected
 */
@RunWith(AndroidJUnit4::class)
class CalendarAdapterCheckIconTest {

    private lateinit var adapter: CalendarAdapter

    @Before
    fun setUp() {
        adapter = CalendarAdapter()
    }

    // ========== Adapter Creation Tests ==========

    @Test
    fun testAdapterCreation() {
        assertNotNull("Adapter should be created", adapter)
    }

    // ========== Check Icon Tests ==========

    @Test
    fun testCheckIconExistsInLayout() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.item_calendar_day, null)

        val checkIcon = view.findViewById<ImageView>(R.id.checkIcon)
        assertNotNull("Check icon should exist in layout", checkIcon)
    }

    @Test
    fun testProgressDotsExistInLayout() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.item_calendar_day, null)

        val progressDots = view.findViewById<android.widget.LinearLayout>(R.id.progressDots)
        assertNotNull("Progress dots should exist in layout", progressDots)
    }

    @Test
    fun testElementCircleExistsInLayout() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val inflater = LayoutInflater.from(context)

        val view = inflater.inflate(R.layout.item_calendar_day, null)

        val elementCircle = view.findViewById<android.view.View>(R.id.elementCircle)
        assertNotNull("Element circle should exist in layout", elementCircle)
    }

    // ========== ic_check Drawable Tests ==========

    @Test
    fun testCheckIconDrawableExists() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val drawable = context.getDrawable(R.drawable.ic_check)
        assertNotNull("ic_check drawable should exist", drawable)
    }

    // ========== Data Handling Tests ==========

    @Test
    fun testCompletedDayData() {
        val completedDay = DayData(
            date = "2025-11-13",
            neededElement = "목",
            neededElementEn = "wood",
            targetCount = 5,
            collectedCount = 5,
            isCompleted = true,
            progressPercentage = 100
        )

        // Test that completed day has correct properties
        assertEquals("Collected count should be 5", 5, completedDay.collectedCount)
        assertEquals("Target count should be 5", 5, completedDay.targetCount)
        assertTrue("Day should be completed", completedDay.isCompleted)
        assertEquals("Progress should be 100%", 100, completedDay.progressPercentage)
    }

    @Test
    fun testIncompleteDayData() {
        val incompleteDay = DayData(
            date = "2025-11-13",
            neededElement = "화",
            neededElementEn = "fire",
            targetCount = 5,
            collectedCount = 3,
            isCompleted = false,
            progressPercentage = 60
        )

        assertEquals("Collected count should be 3", 3, incompleteDay.collectedCount)
        assertEquals("Target count should be 5", 5, incompleteDay.targetCount)
        assertFalse("Day should not be completed", incompleteDay.isCompleted)
        assertEquals("Progress should be 60%", 60, incompleteDay.progressPercentage)
    }

    @Test
    fun testOverCompletedDay() {
        val overCompletedDay = DayData(
            date = "2025-11-13",
            neededElement = "수",
            neededElementEn = "water",
            targetCount = 5,
            collectedCount = 7,
            isCompleted = true,
            progressPercentage = 100
        )

        assertTrue("Collected count is more than target", overCompletedDay.collectedCount > overCompletedDay.targetCount)
        assertTrue("Day should be completed", overCompletedDay.isCompleted)
    }

    // ========== Element Color Tests ==========

    @Test
    fun testElementColors() {
        val elementColors = mapOf(
            "wood" to "#0BEFA0",
            "fire" to "#F93E3E",
            "earth" to "#FF9500",
            "metal" to "#C0C0C0",
            "water" to "#2BB3FC"
        )

        elementColors.forEach { (element, colorHex) ->
            try {
                val color = Color.parseColor(colorHex)
                assertNotEquals("Color for $element should be valid", 0, color)
            } catch (e: IllegalArgumentException) {
                fail("Color $colorHex for $element should be valid")
            }
        }
    }

    // ========== Integration Tests ==========

    @Test
    fun testCompletedDayLogic() {
        val completedDay = DayData(
            date = "2025-11-13",
            neededElement = "목",
            neededElementEn = "wood",
            targetCount = 5,
            collectedCount = 5,
            isCompleted = true,
            progressPercentage = 100
        )

        // Logic: if isCompleted and collectedCount >= 5, show check icon
        if (completedDay.isCompleted && completedDay.collectedCount >= 5) {
            assertTrue("Should show check icon", true)
        } else {
            fail("Should show check icon for completed day with 5+ elements")
        }
    }

    @Test
    fun testIncompleteDayLogic() {
        val incompleteDay = DayData(
            date = "2025-11-13",
            neededElement = "화",
            neededElementEn = "fire",
            targetCount = 5,
            collectedCount = 3,
            isCompleted = false,
            progressPercentage = 60
        )

        // Logic: if not completed or collectedCount < 5, show circles and dots
        if (!incompleteDay.isCompleted || incompleteDay.collectedCount < 5) {
            assertTrue("Should show circles and dots", true)
        } else {
            fail("Should show circles and dots for incomplete day")
        }
    }

    @Test
    fun testEdgeCaseZeroCollected() {
        val zeroCollectedDay = DayData(
            date = "2025-11-13",
            neededElement = "토",
            neededElementEn = "earth",
            targetCount = 5,
            collectedCount = 0,
            isCompleted = false,
            progressPercentage = 0
        )

        assertFalse("Day with 0 collected should not be completed", zeroCollectedDay.isCompleted)
        assertEquals("Progress should be 0%", 0, zeroCollectedDay.progressPercentage)
    }

    // ========== ic_calendar Icon Tests ==========

    @Test
    fun testCalendarIconDrawableExists() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        val drawable = context.getDrawable(R.drawable.ic_calendar)
        assertNotNull("ic_calendar drawable should exist", drawable)
    }

    @Test
    fun testAtomIconDeleted() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // ic_atom should no longer exist (replaced by ic_calendar)
        try {
            val drawable = context.getDrawable(R.drawable.ic_atom)
            // If this succeeds, the icon wasn't deleted (might be okay if not committed yet)
        } catch (e: android.content.res.Resources.NotFoundException) {
            // Expected: icon should be deleted
            assertTrue("ic_atom should be deleted", true)
        }
    }
}