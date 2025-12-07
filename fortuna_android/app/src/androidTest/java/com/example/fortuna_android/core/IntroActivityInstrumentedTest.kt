package com.example.fortuna_android.core

import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.viewpager2.widget.ViewPager2
import com.example.fortuna_android.IntroActivity
import com.example.fortuna_android.IntroPagerAdapter
import com.example.fortuna_android.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Lean smoke tests aligned with the current 6-page intro flow.
 */
@RunWith(AndroidJUnit4::class)
class IntroActivityInstrumentedTest {

    private lateinit var scenario: ActivityScenario<IntroActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(IntroActivity::class.java)
    }

    @After
    fun tearDown() {
        if (::scenario.isInitialized) {
            scenario.close()
        }
    }

    @Test
    fun activityLaunchesAndViewsPresent() {
        scenario.onActivity { activity ->
            assertNotNull(activity.findViewById<ViewPager2>(R.id.viewPager))
            assertNotNull(activity.findViewById<TextView>(R.id.pageIndicator))
            assertNotNull(activity.findViewById<Button>(R.id.btnSkip))
        }
    }

    @Test
    fun viewPagerConfiguredWithSixPages() {
        scenario.onActivity { activity ->
            val viewPager = activity.findViewById<ViewPager2>(R.id.viewPager)
            val adapter = viewPager.adapter

            assertTrue(adapter is IntroPagerAdapter)
            assertEquals(6, adapter?.itemCount)
            assertTrue(viewPager.isUserInputEnabled)
        }
    }

    @Test
    fun pageIndicatorUpdatesOnPageChange() {
        scenario.onActivity { activity ->
            activity.findViewById<ViewPager2>(R.id.viewPager).setCurrentItem(2, false)
        }

        onView(withId(R.id.pageIndicator))
            .check(matches(withText("3 / 6")))
    }

    @Test
    fun skipButtonFinishesActivity() {
        var finished = false
        scenario.onActivity { activity ->
            activity.findViewById<Button>(R.id.btnSkip).performClick()
            finished = activity.isFinishing
        }
        assertTrue(finished)
    }
}
