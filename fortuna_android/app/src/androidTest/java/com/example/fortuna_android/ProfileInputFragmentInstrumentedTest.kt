package com.example.fortuna_android

import android.content.Context
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileInputFragmentInstrumentedTest {

    private lateinit var scenario: FragmentScenario<ProfileInputFragment>
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

    @Test
    fun testFragmentLaunchesSuccessfully() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should not be null", fragment)
            assertTrue("Fragment should be added", fragment.isAdded)
        }
    }

    @Test
    fun testFragmentViewCreated() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment view should be created", fragment.view)
        }
    }

    @Test
    fun testFragmentLifecycle() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
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
        scenario = launchFragmentInContainer<ProfileInputFragment>(
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
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment context should not be null", fragment.context)
            assertNotNull("Fragment requireContext should not be null", fragment.requireContext())
        }
    }

    @Test
    fun testFragmentCanAccessSharedPreferences() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val prefs = fragment.requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("jwt_token", null)

            assertNotNull("Token should be accessible from fragment", token)
            assertEquals("test_token", token)
        }
    }

    @Test
    fun testFragmentHandlesMultipleLifecycleCycles() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        repeat(3) {
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        }

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should survive multiple lifecycle cycles", fragment)
            assertTrue("Fragment should be resumed", fragment.isResumed)
        }
    }

    @Test
    fun testFragmentSurvivesConfigurationChange() {
        scenario = launchFragmentInContainer<ProfileInputFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.recreate()

        scenario.onFragment { fragment ->
            val prefs = fragment.requireContext().getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            val token = prefs.getString("jwt_token", null)

            assertEquals("Token should persist after configuration change", "test_token", token)
        }
    }
}
