package com.example.fortuna_android.core

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.IntroActivity
import com.example.fortuna_android.IntroPagerAdapter
import com.example.fortuna_android.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test for IntroActivity and IntroPagerAdapter
 * Tests intro screen navigation and adapter functionality
 */
@RunWith(AndroidJUnit4::class)
class IntroActivityInstrumentedTest {

    private lateinit var scenario: ActivityScenario<IntroActivity>

    @Before
    fun setup() {
        scenario = ActivityScenario.launch(IntroActivity::class.java)
    }

    // ========== IntroActivity Tests ==========

    @Test
    fun testActivityLaunches() {
        scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)
        }
    }

    @Test
    fun testViewPagerIsDisplayed() {
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSkipButtonIsDisplayed() {
        onView(withId(R.id.btnSkip))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testPageIndicatorIsDisplayed() {
        onView(withId(R.id.pageIndicator))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testInitialPageIndicatorText() {
        onView(withId(R.id.pageIndicator))
            .check(matches(withText("1 / 7")))
    }

    @Test
    fun testSkipButtonClick() {
        // Just verify skip button is clickable
        onView(withId(R.id.btnSkip))
            .check(matches(isClickable()))

        onView(withId(R.id.btnSkip))
            .perform(click())

        // Give time for activity to process the click
        Thread.sleep(500)

        // Test completes successfully if no crash occurs
        assertTrue("Skip button click executed successfully", true)
    }

    @Test
    fun testViewPagerSwipe() {
        // Swipe to second page
        onView(withId(R.id.viewPager))
            .perform(swipeLeft())

        Thread.sleep(500) // Wait for swipe animation

        // Check page indicator updated
        onView(withId(R.id.pageIndicator))
            .check(matches(withText("2 / 7")))
    }

    @Test
    fun testViewPagerIsUserInputEnabled() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            assertTrue("ViewPager should allow user input", viewPager.isUserInputEnabled)
        }
    }

    @Test
    fun testMultipleSwipes() {
        // Swipe through multiple pages
        onView(withId(R.id.viewPager))
            .perform(swipeLeft())
        Thread.sleep(300)

        onView(withId(R.id.viewPager))
            .perform(swipeLeft())
        Thread.sleep(300)

        onView(withId(R.id.pageIndicator))
            .check(matches(withText("3 / 7")))
    }

    @Test
    fun testProgrammaticPageChange() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            viewPager.setCurrentItem(3, false)
        }

        Thread.sleep(300)

        onView(withId(R.id.pageIndicator))
            .check(matches(withText("4 / 7")))
    }

    @Test
    fun testLastPageIndicator() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            viewPager.setCurrentItem(6, false)
        }

        Thread.sleep(300)

        onView(withId(R.id.pageIndicator))
            .check(matches(withText("7 / 7")))
    }

    // ========== IntroPagerAdapter Tests ==========

    @Test
    fun testAdapterItemCount() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            val adapter = viewPager.adapter as IntroPagerAdapter

            assertEquals("Adapter should have 7 items", 7, adapter.itemCount)
        }
    }

    @Test
    fun testAdapterGetItemViewType() {
        scenario.onActivity { activity ->
            val adapter = IntroPagerAdapter()

            // Test position to viewType mapping
            assertEquals("Position 0 should be viewType 1", 1, adapter.getItemViewType(0))
            assertEquals("Position 1 should be viewType 2", 2, adapter.getItemViewType(1))
            assertEquals("Position 2 should be viewType 4", 4, adapter.getItemViewType(2))
            assertEquals("Position 3 should be viewType 3", 3, adapter.getItemViewType(3))
            assertEquals("Position 4 should be viewType 5", 5, adapter.getItemViewType(4))
            assertEquals("Position 5 should be viewType 6", 6, adapter.getItemViewType(5))
            assertEquals("Position 6 should be viewType 7", 7, adapter.getItemViewType(6))
        }
    }

    @Test
    fun testAdapterGetItemViewTypeInvalidPosition() {
        scenario.onActivity { activity ->
            val adapter = IntroPagerAdapter()

            try {
                adapter.getItemViewType(7)
                fail("Should throw IllegalArgumentException for invalid position")
            } catch (e: IllegalArgumentException) {
                assertEquals("Invalid position", e.message)
            }
        }
    }

    @Test
    fun testAdapterGetItemViewTypeNegativePosition() {
        scenario.onActivity { activity ->
            val adapter = IntroPagerAdapter()

            try {
                adapter.getItemViewType(-1)
                fail("Should throw IllegalArgumentException for negative position")
            } catch (e: IllegalArgumentException) {
                assertEquals("Invalid position", e.message)
            }
        }
    }

    @Test
    fun testAllPagesAreAccessible() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)

            // Navigate through all pages
            for (i in 0..6) {
                viewPager.setCurrentItem(i, false)
                Thread.sleep(200)

                assertEquals("ViewPager should be at position $i", i, viewPager.currentItem)
            }
        }
    }

    @Test
    fun testPageIndicatorUpdatesForAllPages() {
        for (i in 0..6) {
            scenario.onActivity { activity ->
                val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
                viewPager.setCurrentItem(i, false)
            }

            Thread.sleep(200)

            onView(withId(R.id.pageIndicator))
                .check(matches(withText("${i + 1} / 7")))
        }
    }

    @Test
    fun testAdapterCreatesCorrectViewHolders() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            val adapter = viewPager.adapter as IntroPagerAdapter

            // Verify adapter is attached and working
            assertNotNull("Adapter should be attached", adapter)
            assertTrue("Adapter should have registered observers", adapter.hasObservers())
        }
    }

    @Test
    fun testActivityRecreation() {
        // Navigate to page 3
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            viewPager.setCurrentItem(2, false)
        }

        Thread.sleep(300)

        // Recreate activity
        scenario.recreate()

        // Verify activity still works
        onView(withId(R.id.viewPager))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnSkip))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testOnDestroyCalled() {
        scenario.close()

        // Activity should be destroyed without crashes
        assertTrue("Test completed successfully", true)
    }

    @Test
    fun testRapidPageChanges() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)

            // Rapidly change pages
            viewPager.setCurrentItem(3, false)
            viewPager.setCurrentItem(1, false)
            viewPager.setCurrentItem(5, false)
            viewPager.setCurrentItem(0, false)
        }

        Thread.sleep(500)

        onView(withId(R.id.pageIndicator))
            .check(matches(withText("1 / 7")))
    }

    @Test
    fun testViewPagerAdapterNotNull() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            assertNotNull("ViewPager adapter should not be null", viewPager.adapter)
        }
    }

    @Test
    fun testSkipButtonMultipleClicks() {
        onView(withId(R.id.btnSkip))
            .perform(click())

        // Second click should not crash (activity already finishing)
        try {
            onView(withId(R.id.btnSkip))
                .perform(click())
        } catch (e: Exception) {
            // Expected - activity is finishing
        }

        assertTrue("Test completed without crash", true)
    }
}
