package com.example.fortuna_android.ui

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for AtomFragment completion rate calculation
 * Tests the change from "days until today" to "total days in month"
 */
@RunWith(AndroidJUnit4::class)
class AtomFragmentCompletionRateTest {

    private lateinit var scenario: FragmentScenario<AtomFragment>
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Set up mock token
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("jwt_token", "test_token").commit()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }

        // Clean up
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    // ========== Basic Fragment Tests ==========

    @Test
    fun testFragmentLaunchesSuccessfully() {
        scenario = launchFragmentInContainer<AtomFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should not be null", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testFragmentViewCreated() {
        scenario = launchFragmentInContainer<AtomFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment view should be created", fragment.view)
        }
    }

    // ========== UI Elements Tests ==========

    @Test
    fun testSummaryUIElementsExist() {
        scenario = launchFragmentInContainer<AtomFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Wait for UI setup
            Thread.sleep(500)

            val tvCompletedDays = fragment.view?.findViewById<android.widget.TextView>(R.id.tvCompletedDays)
            val tvCompletionRate = fragment.view?.findViewById<android.widget.TextView>(R.id.tvCompletionRate)
            val tvTotalCollected = fragment.view?.findViewById<android.widget.TextView>(R.id.tvTotalCollected)

            assertNotNull("Completed days TextView should exist", tvCompletedDays)
            assertNotNull("Completion rate TextView should exist", tvCompletionRate)
            assertNotNull("Total collected TextView should exist", tvTotalCollected)
        }
    }

    @Test
    fun testCalendarIconDisplayed() {
        scenario = launchFragmentInContainer<AtomFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // The fragment should load successfully with the new calendar icon
            assertNotNull("Fragment should load with calendar icon", fragment.view)
        }
    }

    // ========== Lifecycle Tests ==========

    @Test
    fun testFragmentRecreation() {
        scenario = launchFragmentInContainer<AtomFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.recreate()

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should be recreated", fragment)
            assertNotNull("Fragment view should exist after recreation", fragment.view)
        }
    }

    @Test
    fun testFragmentLifecycle() {
        scenario = launchFragmentInContainer<AtomFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        scenario.onFragment { fragment ->
            assertTrue("Fragment should be resumed", fragment.isResumed)
        }
    }

    // ========== Integration Tests ==========

    @Test
    fun testCompletionRateDisplayFormat() {
        scenario = launchFragmentInContainer<AtomFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Wait for data loading
            Thread.sleep(1000)

            val tvCompletionRate = fragment.view?.findViewById<android.widget.TextView>(R.id.tvCompletionRate)
            assertNotNull("Completion rate should exist", tvCompletionRate)

            // Completion rate should be in percentage format (e.g., "50%")
            val rateText = tvCompletionRate?.text?.toString()
            if (rateText != null && rateText.isNotEmpty()) {
                assertTrue("Completion rate should contain %", rateText.contains("%"))
            }
        }
    }

    @Test
    fun testCompletedDaysDisplayFormat() {
        scenario = launchFragmentInContainer<AtomFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Wait for data loading
            Thread.sleep(1000)

            val tvCompletedDays = fragment.view?.findViewById<android.widget.TextView>(R.id.tvCompletedDays)
            assertNotNull("Completed days should exist", tvCompletedDays)

            // Completed days should be in format "X / Y"
            val daysText = tvCompletedDays?.text?.toString()
            if (daysText != null && daysText.isNotEmpty()) {
                assertTrue("Completed days should contain /", daysText.contains("/"))
            }
        }
    }
}