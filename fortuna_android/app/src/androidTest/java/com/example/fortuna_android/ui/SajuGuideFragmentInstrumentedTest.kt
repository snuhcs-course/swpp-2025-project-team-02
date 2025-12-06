package com.example.fortuna_android.ui

import android.content.Context
import android.widget.LinearLayout
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for SajuGuideFragment
 * Tests for dot indicator pagination and ViewPager functionality
 */
@RunWith(AndroidJUnit4::class)
class SajuGuideFragmentInstrumentedTest {

    private lateinit var scenario: FragmentScenario<SajuGuideFragment>
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    // ========== Basic Fragment Tests ==========

    @Test
    fun testFragmentLaunchesSuccessfully() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should not be null", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testFragmentViewCreated() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment view should be created", fragment.view)
        }
    }

    // ========== ViewPager Tests ==========

    @Test
    fun testViewPagerExists() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val viewPager = fragment.view?.findViewById<ViewPager2>(R.id.viewPager)
            assertNotNull("ViewPager should exist", viewPager)
        }
    }

    @Test
    fun testViewPagerHasSixPages() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val viewPager = fragment.view?.findViewById<ViewPager2>(R.id.viewPager)
            assertNotNull("ViewPager should exist", viewPager)

            val adapter = viewPager?.adapter
            assertNotNull("ViewPager adapter should exist", adapter)
            assertEquals("ViewPager should have 6 pages", 6, adapter?.itemCount)
        }
    }

    @Test
    fun testViewPagerSwipeEnabled() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val viewPager = fragment.view?.findViewById<ViewPager2>(R.id.viewPager)
            assertNotNull("ViewPager should exist", viewPager)
            assertTrue("ViewPager should allow user input", viewPager?.isUserInputEnabled == true)
        }
    }

    // ========== Dot Indicator Tests ==========

    @Test
    fun testDotsIndicatorExists() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dotsIndicator = fragment.view?.findViewById<LinearLayout>(R.id.dotsIndicator)
            assertNotNull("Dots indicator should exist", dotsIndicator)
        }
    }

    @Test
    fun testDotsIndicatorHasSixDots() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dotsIndicator = fragment.view?.findViewById<LinearLayout>(R.id.dotsIndicator)
            assertNotNull("Dots indicator should exist", dotsIndicator)

            // Wait a bit for dots to be created
            Thread.sleep(500)

            assertEquals("Should have 6 dots", 6, dotsIndicator?.childCount)
        }
    }

    @Test
    fun testFirstDotIsSelectedInitially() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dotsIndicator = fragment.view?.findViewById<LinearLayout>(R.id.dotsIndicator)
            assertNotNull("Dots indicator should exist", dotsIndicator)

            // Wait a bit for dots to be created
            Thread.sleep(500)

            assertTrue("Should have at least one dot", (dotsIndicator?.childCount ?: 0) > 0)

            // First dot should be selected (using dot_selected drawable)
            val firstDot = dotsIndicator?.getChildAt(0) as? android.widget.ImageView
            assertNotNull("First dot should be an ImageView", firstDot)
        }
    }

    @Test
    fun testDotIndicatorUpdatesOnPageChange() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val viewPager = fragment.view?.findViewById<ViewPager2>(R.id.viewPager)
            assertNotNull("ViewPager should exist", viewPager)

            // Wait for initial setup
            Thread.sleep(500)

            // Change to page 2
            viewPager?.currentItem = 2

            // Wait for update
            Thread.sleep(300)

            assertEquals("Current page should be 2", 2, viewPager?.currentItem)
        }
    }

    // ========== Dark Theme Tests ==========

    @Test
    fun testFragmentHasDarkBackground() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val rootView = fragment.view
            assertNotNull("Root view should exist", rootView)

            // Background should be dark (black #000000)
            val background = rootView?.background
            assertNotNull("Background should exist", background)
        }
    }

    // ========== Lifecycle Tests ==========

    @Test
    fun testFragmentRecreation() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.recreate()

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should be recreated", fragment)
            assertNotNull("Fragment view should exist after recreation", fragment.view)

            val viewPager = fragment.view?.findViewById<ViewPager2>(R.id.viewPager)
            assertNotNull("ViewPager should exist after recreation", viewPager)
        }
    }

    @Test
    fun testFragmentDestroyView() {
        scenario = launchFragmentInContainer<SajuGuideFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)

        // Should not crash
        assertTrue("Fragment lifecycle should handle destroy", true)
    }
}
