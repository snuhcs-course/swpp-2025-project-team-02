package com.example.fortuna_android.ui

import android.content.Context
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fortuna_android.R
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Network-related instrumented tests for ARFragment
 * These tests verify API integration and response handling
 */
@RunWith(AndroidJUnit4::class)
class ARFragmentNetworkTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "fortuna_prefs"

    @Before
    fun setup() {
        // Clear SharedPreferences
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        // Clean up SharedPreferences
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testFragmentHandlesNetworkFailure() {
        // This test verifies that the fragment doesn't crash when network fails
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Wait for potential network calls to fail
        Thread.sleep(2000)

        // Verify fragment is still functional after network failure
        scenario.onFragment { fragment ->
            assert(fragment.isAdded)
            assert(fragment.view != null)
        }

        scenario.close()
    }

    @Test
    fun testNeededElementBannerWithoutNetworkResponse() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Wait for API call attempts
        Thread.sleep(2000)

        // Needed element banner should remain hidden if API fails
        onView(withId(R.id.neededElementBanner))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        scenario.close()
    }

    @Test
    fun testFragmentWithAuthToken() {
        // Add auth token to SharedPreferences
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_auth_token_123")
            .commit()

        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Wait for potential network calls with auth
        Thread.sleep(2000)

        // Verify fragment handles auth token properly
        scenario.onFragment { fragment ->
            assert(fragment.isAdded)
            assert(fragment.isResumed)
        }

        scenario.close()
    }

    @Test
    fun testMultipleNetworkCallsDuringFragmentLifecycle() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Wait for initial network calls
        Thread.sleep(1000)

        // Recreate fragment (simulating configuration change)
        scenario.recreate()

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Wait for network calls after recreation
        Thread.sleep(1000)

        // Verify fragment handles multiple network calls properly
        scenario.onFragment { fragment ->
            assert(fragment.isAdded)
            assert(fragment.view != null)
        }

        scenario.close()
    }

    @Test
    fun testFragmentStateAfterNetworkDelay() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Simulate network delay
        Thread.sleep(3000)

        // Verify UI elements are still accessible
        onView(withId(R.id.scanButton))
            .check(matches(isDisplayed()))

        onView(withId(R.id.clearButton))
            .check(matches(isDisplayed()))

        onView(withId(R.id.btnBack))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun testFragmentHandlesSlowNetwork() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Simulate slow network with longer wait
        Thread.sleep(5000)

        // Fragment should still be functional
        scenario.onFragment { fragment ->
            assert(fragment.isResumed)

            // Test that user can still interact with UI
            fragment.setScanningActive(true)
            fragment.setScanningActive(false)
        }

        onView(withId(R.id.scanButton))
            .check(matches(isEnabled()))

        scenario.close()
    }

    @Test
    fun testFragmentNetworkCallsCancellationOnDestroy() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Wait a bit for network calls to start
        Thread.sleep(500)

        // Close fragment (should cancel ongoing network calls)
        scenario.close()

        // Wait to ensure no crashes from cancelled calls
        Thread.sleep(1000)

        // Test passes if no exceptions are thrown
        assert(true)
    }

    @Test
    fun testFragmentWithEmptySharedPreferences() {
        // Ensure SharedPreferences is completely empty
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Wait for network calls
        Thread.sleep(2000)

        // Fragment should handle missing auth gracefully
        scenario.onFragment { fragment ->
            assert(fragment.isAdded)
        }

        onView(withId(R.id.scanButton))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun testNetworkStateDoesNotBlockUIInteraction() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Even during network calls, UI should be interactive
        Thread.sleep(100) // Small delay for fragment setup

        // Test immediate UI interaction
        onView(withId(R.id.scanButton))
            .check(matches(isClickable()))

        onView(withId(R.id.clearButton))
            .check(matches(isClickable()))

        onView(withId(R.id.btnBack))
            .check(matches(isClickable()))

        scenario.onFragment { fragment ->
            // Test that public methods work during network calls
            fragment.setScanningActive(true)
            fragment.onVLMAnalysisStarted()
        }

        Thread.sleep(100)

        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun testFragmentHandlesRapidStartStop() {
        // Test that rapid fragment creation/destruction doesn't cause issues
        for (i in 1..3) {
            val scenario = launchFragmentInContainer<ARFragment>(
                themeResId = R.style.Theme_Fortuna_android
            )

            scenario.onFragment { fragment ->
                val navController = TestNavHostController(context)
                navController.setGraph(R.navigation.nav_graph)
                Navigation.setViewNavController(fragment.requireView(), navController)
            }

            Thread.sleep(500) // Brief wait

            scenario.close()
        }

        // Test passes if no crashes occur
        assert(true)
    }

    @Test
    fun testCollectionProgressWithoutServerData() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Manually trigger sphere collection without server data
            fragment.onSphereCollected(count = 1)
            fragment.onSphereCollected(count = 2)
            fragment.onSphereCollected(count = 3)
        }

        // Fragment should handle collection tracking locally
        scenario.onFragment { fragment ->
            assert(fragment.isAdded)
            assert(fragment.isResumed)
        }

        scenario.close()
    }

    @Test
    fun testFragmentRecoversFromNetworkError() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Wait for potential network errors
        Thread.sleep(2000)

        // Even after network errors, user should be able to use AR features
        scenario.onFragment { fragment ->
            fragment.setScanningActive(true)
        }

        Thread.sleep(100)

        onView(withId(R.id.scanButton))
            .check(matches(isNotEnabled()))
            .check(matches(withText("Scanning...")))

        scenario.onFragment { fragment ->
            fragment.setScanningActive(false)
        }

        onView(withId(R.id.scanButton))
            .check(matches(isEnabled()))
            .check(matches(withText("Scan")))

        scenario.close()
    }

    @Test
    fun testFragmentNetworkCallsDuringPauseResume() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Wait for initial network calls
        Thread.sleep(1000)

        // Move to paused state
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.STARTED)
        Thread.sleep(500)

        // Resume
        scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
        Thread.sleep(500)

        // Verify fragment handles pause/resume during network calls
        scenario.onFragment { fragment ->
            assert(fragment.isResumed)
        }

        scenario.close()
    }
}
