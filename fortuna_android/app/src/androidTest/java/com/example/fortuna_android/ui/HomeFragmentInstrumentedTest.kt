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

@RunWith(AndroidJUnit4::class)
class HomeFragmentInstrumentedTest {

    private lateinit var scenario: FragmentScenario<HomeFragment>
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

    @Test
    fun testFragmentLaunchesSuccessfully() {
        scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should not be null", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testFragmentViewCreated() {
        scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment view should be created", fragment.view)
        }
    }

    @Test
    fun testFragmentLifecycle() {
        scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)

        scenario.onFragment { fragment ->
            assertTrue("Fragment should be resumed", fragment.isResumed)
        }
    }

    @Test
    fun testFragmentRecreation() {
        scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.recreate()

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should be recreated", fragment)
            assertNotNull("Fragment view should exist after recreation", fragment.view)
        }
    }

    @Test
    fun testFragmentContext() {
        scenario = launchFragmentInContainer<HomeFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment context should not be null", fragment.context)
            assertNotNull("Fragment requireContext should not be null", fragment.requireContext())
        }
    }
}
