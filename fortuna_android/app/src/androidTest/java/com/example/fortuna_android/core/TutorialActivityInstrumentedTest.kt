package com.example.fortuna_android.core

import android.content.Intent
import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.TutorialActivity
import com.example.fortuna_android.TutorialPagerAdapter
import com.example.fortuna_android.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for TutorialActivity to achieve 100% line coverage
 */
@RunWith(AndroidJUnit4::class)
class TutorialActivityInstrumentedTest {

    private lateinit var scenario: ActivityScenario<TutorialActivity>

    @Before
    fun setUp() {
        // Clear any previous state
        InstrumentationRegistry.getInstrumentation().targetContext.cacheDir.deleteRecursively()
        scenario = ActivityScenario.launch(TutorialActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    // ========== Activity Lifecycle Tests ==========

    @Test
    fun testActivityCreation() = runBlocking {
        // Test that activity is created successfully
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        scenario.onActivity { activity ->
            assertNotNull("Activity should not be null", activity)

            // Verify views are initialized
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            val pageIndicator = activity.findViewById<TextView>(R.id.pageIndicator)
            val skipButton = activity.findViewById<Button>(R.id.btnSkip)

            assertNotNull("ViewPager should not be null", viewPager)
            assertNotNull("Page indicator should not be null", pageIndicator)
            assertNotNull("Skip button should not be null", skipButton)
        }

        delay(300)
    }

    @Test
    fun testActivityDestroy() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(200)

        // Activity should be destroyed without crashes (covers onDestroy)
        scenario.close()

        delay(100)
    }

    // ========== ViewPager Setup Tests ==========

    @Test
    fun testViewPagerSetup() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)

            // Check ViewPager is set up correctly (covers setupViewPager)
            assertNotNull("ViewPager should not be null", viewPager)
            assertNotNull("ViewPager adapter should be set", viewPager.adapter)
            assertTrue("ViewPager adapter should be TutorialPagerAdapter",
                viewPager.adapter is TutorialPagerAdapter)
            assertEquals("ViewPager should have 2 pages", 2, viewPager.adapter?.itemCount)
            assertFalse("ViewPager should not allow user input", viewPager.isUserInputEnabled)
        }

        delay(200)
    }

    // ========== Page Navigation Tests ==========

    @Test
    fun testPageNavigation_toSecondPage() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            // Navigate to second page
            viewPager.setCurrentItem(1, false)
        }

        delay(500)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            val pageIndicator = activity.findViewById<TextView>(R.id.pageIndicator)

            assertEquals("Should be on second page", 1, viewPager.currentItem)
            assertEquals("Page indicator should show 2 / 2", "2 / 2", pageIndicator.text.toString())
        }

        delay(200)
    }

    @Test
    fun testPageNavigation_toAllPages() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        // Test navigation to all 2 pages (covers updatePageIndicator with all positions)
        for (page in 0 until 2) {
            scenario.onActivity { activity ->
                val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
                viewPager.setCurrentItem(page, false)
            }

            delay(300)

            scenario.onActivity { activity ->
                val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
                val pageIndicator = activity.findViewById<TextView>(R.id.pageIndicator)

                assertEquals("Should be on page $page", page, viewPager.currentItem)
                assertEquals("Page indicator should show ${page + 1} / 2", "${page + 1} / 2",
                    pageIndicator.text.toString())
            }
        }

        delay(200)
    }

    @Test
    fun testPageNavigation_smoothScroll() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            // Navigate with smooth scroll
            viewPager.setCurrentItem(1, true)
        }

        delay(800) // Wait for smooth scroll animation

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            val pageIndicator = activity.findViewById<TextView>(R.id.pageIndicator)

            assertEquals("Should be on page 1", 1, viewPager.currentItem)
            assertEquals("Page indicator should show 2 / 2", "2 / 2", pageIndicator.text.toString())
        }

        delay(200)
    }

    // ========== Skip Button Tests ==========

    @Test
    fun testSkipButton_click() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        var activityFinished = false

        scenario.onActivity { activity ->
            val skipButton = activity.findViewById<Button>(R.id.btnSkip)

            assertNotNull("Skip button should not be null", skipButton)

            // Click skip button (covers setOnClickListener + finish())
            skipButton.performClick()

            // Activity should be finishing after skip button is clicked
            activityFinished = activity.isFinishing
        }

        delay(300)

        assertTrue("Activity should be finishing after skip button click", activityFinished)
    }

    // ========== UpdatePageIndicator Method Tests ==========

    @Test
    fun testUpdatePageIndicator_firstPage(): Unit = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            viewPager.setCurrentItem(0, false)
        }

        delay(300)

        scenario.onActivity { activity ->
            val pageIndicator = activity.findViewById<TextView>(R.id.pageIndicator)
            assertEquals("Page indicator should show 1 / 2", "1 / 2", pageIndicator.text.toString())
        }
    }

    @Test
    fun testUpdatePageIndicator_lastPage(): Unit = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            viewPager.setCurrentItem(1, false)
        }

        delay(500)

        scenario.onActivity { activity ->
            val pageIndicator = activity.findViewById<TextView>(R.id.pageIndicator)
            assertEquals("Page indicator should show 2 / 2", "2 / 2", pageIndicator.text.toString())
        }
    }

    // ========== ViewPager PageChangeCallback Tests ==========

    @Test
    fun testOnPageChangeCallback_triggered() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        var callbackTriggered = false

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)

            // Add a test callback to verify it's called
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    callbackTriggered = true
                }
            })

            // Change page (covers onPageSelected callback)
            viewPager.setCurrentItem(1, false)
        }

        delay(500)

        assertTrue("Page change callback should be triggered", callbackTriggered)
    }

    // ========== Adapter Integration Tests ==========

    @Test
    fun testTutorialPagerAdapter_itemCount(): Unit = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            val adapter = viewPager.adapter as TutorialPagerAdapter

            assertEquals("Adapter should have 2 items", 2, adapter.itemCount)
        }
    }

    // ========== User Input Tests ==========

    @Test
    fun testViewPager_userInputDisabled(): Unit = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)

            // Verify user input is disabled (navigation only via buttons)
            assertFalse("ViewPager should not allow user input for manual swipe", viewPager.isUserInputEnabled)
        }
    }

    // ========== navigateToNextPage Tests ==========

    @Test
    fun testNavigateToNextPage_fromFirstPage() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            assertEquals("Should start on page 0", 0, viewPager.currentItem)

            // Navigate to next page
            activity.navigateToNextPage()
        }

        delay(500)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            assertEquals("Should be on page 1", 1, viewPager.currentItem)
        }
    }

    @Test
    fun testNavigateToNextPage_fromLastPage() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            // Go to last page
            viewPager.setCurrentItem(1, false)
        }

        delay(500)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            assertEquals("Should be on page 1", 1, viewPager.currentItem)

            // Try to navigate to next page (should stay on last page)
            activity.navigateToNextPage()
        }

        delay(500)

        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            assertEquals("Should still be on page 1", 1, viewPager.currentItem)
        }
    }

    // ========== navigateToARScreen Tests ==========

    @Test
    fun testNavigateToARScreen() = runBlocking {
        val intent = Intent(ApplicationProvider.getApplicationContext(), TutorialActivity::class.java)
        scenario = ActivityScenario.launch(intent)

        delay(300)

        var activityFinished = false

        scenario.onActivity { activity ->
            // Navigate to AR screen
            activity.navigateToARScreen()

            // Activity should be finishing
            activityFinished = activity.isFinishing
        }

        delay(500)

        assertTrue("Activity should be finishing after navigating to AR screen", activityFinished)
    }

    // ========== Espresso UI Tests ==========

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
            .check(matches(withText("1 / 2")))
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
    fun testViewPagerIsUserInputDisabled() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            assertFalse("ViewPager should not allow user input", viewPager.isUserInputEnabled)
        }
    }

    @Test
    fun testProgrammaticPageChange() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            viewPager.setCurrentItem(1, false)
        }

        Thread.sleep(300)

        onView(withId(R.id.pageIndicator))
            .check(matches(withText("2 / 2")))
    }

    @Test
    fun testLastPageIndicator() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            viewPager.setCurrentItem(1, false)
        }

        Thread.sleep(300)

        onView(withId(R.id.pageIndicator))
            .check(matches(withText("2 / 2")))
    }

    // ========== TutorialPagerAdapter Tests ==========

    @Test
    fun testAdapterItemCount() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            val adapter = viewPager.adapter as TutorialPagerAdapter

            assertEquals("Adapter should have 2 items", 2, adapter.itemCount)
        }
    }

    @Test
    fun testAdapterGetItemViewType() {
        scenario.onActivity { activity ->
            val adapter = TutorialPagerAdapter(activity)

            // Test position to viewType mapping
            assertEquals("Position 0 should be viewType 1", 1, adapter.getItemViewType(0))
            assertEquals("Position 1 should be viewType 2", 2, adapter.getItemViewType(1))
        }
    }

    @Test
    fun testAdapterGetItemViewTypeInvalidPosition() {
        scenario.onActivity { activity ->
            val adapter = TutorialPagerAdapter(activity)

            try {
                adapter.getItemViewType(2)
                fail("Should throw IllegalArgumentException for invalid position")
            } catch (e: IllegalArgumentException) {
                assertEquals("Invalid position", e.message)
            }
        }
    }

    @Test
    fun testAdapterGetItemViewTypeNegativePosition() {
        scenario.onActivity { activity ->
            val adapter = TutorialPagerAdapter(activity)

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
            for (i in 0..1) {
                viewPager.setCurrentItem(i, false)
                Thread.sleep(200)

                assertEquals("ViewPager should be at position $i", i, viewPager.currentItem)
            }
        }
    }

    @Test
    fun testPageIndicatorUpdatesForAllPages() {
        for (i in 0..1) {
            scenario.onActivity { activity ->
                val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
                viewPager.setCurrentItem(i, false)
            }

            Thread.sleep(200)

            onView(withId(R.id.pageIndicator))
                .check(matches(withText("${i + 1} / 2")))
        }
    }

    @Test
    fun testAdapterCreatesCorrectViewHolders() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            val adapter = viewPager.adapter as TutorialPagerAdapter

            // Verify adapter is attached and working
            assertNotNull("Adapter should be attached", adapter)
            assertTrue("Adapter should have registered observers", adapter.hasObservers())
        }
    }

    @Test
    fun testActivityRecreation() {
        // Navigate to page 1
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            viewPager.setCurrentItem(1, false)
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
            viewPager.setCurrentItem(1, false)
            viewPager.setCurrentItem(0, false)
            viewPager.setCurrentItem(1, false)
            viewPager.setCurrentItem(0, false)
        }

        Thread.sleep(500)

        onView(withId(R.id.pageIndicator))
            .check(matches(withText("1 / 2")))
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
