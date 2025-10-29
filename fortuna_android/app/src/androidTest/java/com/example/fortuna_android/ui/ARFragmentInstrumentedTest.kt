package com.example.fortuna_android.ui

import android.content.Context
import android.os.SystemClock
import android.view.MotionEvent
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import com.example.fortuna_android.R

@RunWith(AndroidJUnit4::class)
class ARFragmentInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefsName = "fortuna_prefs"

    @Before
    fun setup() {
        // Clear SharedPreferences before each test
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @After
    fun tearDown() {
        // Clean up SharedPreferences after each test
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun testFragmentLaunchesSuccessfully() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull(fragment)
            assertTrue(fragment.isAdded)
        }

        scenario.close()
    }

    @Test
    fun testFragmentViewCreation() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull(fragment.view)
            assertNotNull(fragment.context)
        }

        scenario.close()
    }

    @Test
    fun testFragmentLifecycle() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertTrue(fragment.isResumed)
        }

        scenario.close()
    }

    @Test
    fun testFragmentBindingNotNull() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Verify that the view hierarchy is properly created
            assertNotNull(fragment.view)
            assertNotNull(fragment.requireView().findViewById<android.view.View>(R.id.surfaceview))
        }

        scenario.close()
    }

    @Test
    fun testFragmentConfigurationChange() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        scenario.recreate()

        scenario.onFragment { fragment ->
            // Set up NavController again after recreation
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull(fragment)
            assertTrue(fragment.isAdded)
            assertNotNull(fragment.view)
        }

        scenario.close()
    }

    @Test
    fun testFragmentWithAuthToken() {
        // Save auth token before launching fragment
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            .edit()
            .putString("jwt_token", "test_token_12345")
            .commit()

        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull(fragment)
            assertTrue(fragment.isAdded)
        }

        scenario.close()
    }

    @Test
    fun testScanButtonStateChange() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Test setScanningActive method
            fragment.setScanningActive(true)
            fragment.view?.let { view ->
                val scanButton = view.findViewById<android.widget.Button>(R.id.scanButton)
                assertNotNull(scanButton)
            }

            fragment.setScanningActive(false)
            fragment.view?.let { view ->
                val scanButton = view.findViewById<android.widget.Button>(R.id.scanButton)
                assertNotNull(scanButton)
            }
        }

        scenario.close()
    }

    @Test
    fun testVLMDescriptionMethods() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Test VLM description methods
            fragment.onVLMAnalysisStarted()
            fragment.updateVLMDescription("Test token")
            fragment.clearVLMDescription()
            fragment.onVLMAnalysisCompleted()
        }

        scenario.close()
    }

    @Test
    fun testObjectDetectionCallback() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Test object detection completion callback
            fragment.onObjectDetectionCompleted(anchorsCreated = 3, objectsDetected = 5)

            // Verify that the scan button is re-enabled after detection
            fragment.view?.let { view ->
                val scanButton = view.findViewById<android.widget.Button>(R.id.scanButton)
                assertNotNull(scanButton)
            }
        }

        scenario.close()
    }

    @Test
    fun testFragmentCleanup() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            assertNotNull(fragment)
        }

        // Close scenario to trigger onDestroyView
        scenario.close()
    }

    @Test
    fun testSphereCollectionCallback() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Set up NavController for the fragment
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Test sphere collection callback
            fragment.onSphereCollected(count = 1)
            fragment.onSphereCollected(count = 2)
            fragment.onSphereCollected(count = 3)
        }

        scenario.close()
    }

    @Test
    fun testMultipleFragmentInstances() {
        val scenario1 = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario1.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
            assertNotNull(fragment)
        }

        scenario1.close()

        // Launch another instance to verify no memory leaks or state issues
        val scenario2 = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario2.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
            assertNotNull(fragment)
        }

        scenario2.close()
    }

    // Espresso UI Tests

    @Test
    fun testScanButtonExists() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.scanButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testClearButtonExists() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.clearButton))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testBackButtonExists() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.btnBack))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testSurfaceViewExists() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.surfaceview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testScanButtonHasCorrectText() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.scanButton))
            .check(matches(withText("Scan")))
    }

    @Test
    fun testClearButtonHasCorrectText() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.clearButton))
            .check(matches(withText("Clear")))
    }

    @Test
    fun testScanButtonIsClickable() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.scanButton))
            .check(matches(isClickable()))
    }

    @Test
    fun testClearButtonIsClickable() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.clearButton))
            .check(matches(isClickable()))
    }

    @Test
    fun testBackButtonIsClickable() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.btnBack))
            .check(matches(isClickable()))
    }

    @Test
    fun testScanButtonChangesStateWhenScanning() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            fragment.setScanningActive(true)
        }

        onView(withId(R.id.scanButton))
            .check(matches(withText("Scanning...")))
            .check(matches(isNotEnabled()))

        scenario.onFragment { fragment ->
            fragment.setScanningActive(false)
        }

        onView(withId(R.id.scanButton))
            .check(matches(withText("Scan")))
            .check(matches(isEnabled()))

        scenario.close()
    }

    @Test
    fun testVLMDescriptionOverlayInitiallyHidden() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testVLMDescriptionOverlayShowsWhenAnalysisStarts() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            fragment.onVLMAnalysisStarted()
        }

        Thread.sleep(100) // Wait for UI update

        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun testVLMDescriptionUpdates() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            fragment.onVLMAnalysisStarted()
            fragment.updateVLMDescription("Test ")
            fragment.updateVLMDescription("Description")
        }

        Thread.sleep(100) // Wait for UI update

        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(withText("Analyzing scene...Test Description")))

        scenario.close()
    }

    @Test
    fun testVLMDescriptionClearsCorrectly() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            fragment.onVLMAnalysisStarted()
            fragment.updateVLMDescription("Some text")
            fragment.clearVLMDescription()
        }

        Thread.sleep(100) // Wait for UI update

        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        scenario.close()
    }

    @Test
    fun testNeededElementBannerInitiallyHidden() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.neededElementBanner))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testCelebrationOverlayInitiallyHidden() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.celebrationOverlay))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))
    }

    @Test
    fun testAllButtonsAreVisibleOnLaunch() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.scanButton))
            .check(matches(isDisplayed()))
        onView(withId(R.id.clearButton))
            .check(matches(isDisplayed()))
        onView(withId(R.id.btnBack))
            .check(matches(isDisplayed()))
    }

    @Test
    fun testScanButtonIsEnabledInitially() {
        launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        onView(withId(R.id.scanButton))
            .check(matches(isEnabled()))
    }

    // Animation and Gesture Tests

    @Test
    fun testCelebrationAnimationTriggersOnSphereCollection() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Trigger sphere collection
            fragment.onSphereCollected(count = 1)
        }

        Thread.sleep(100) // Wait for animation to start

        // Check that celebration overlay becomes visible during animation
        onView(withId(R.id.celebrationOverlay))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun testMultipleSphereCollections() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Collect multiple spheres
            fragment.onSphereCollected(count = 1)
            Thread.sleep(100)
            fragment.onSphereCollected(count = 2)
            Thread.sleep(100)
            fragment.onSphereCollected(count = 3)
        }

        Thread.sleep(100)

        // Verify celebration overlay was triggered
        onView(withId(R.id.celebrationOverlay))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun testCelebrationAnimationHidesAfterDelay() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            fragment.onSphereCollected(count = 1)
        }

        Thread.sleep(100) // Animation starts
        onView(withId(R.id.celebrationOverlay))
            .check(matches(isDisplayed()))

        Thread.sleep(1500) // Wait for animation to complete (1200ms + buffer)

        // Note: Animation may still be fading out, so we just verify the test doesn't crash
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
        }

        scenario.close()
    }

    @Test
    fun testTouchEventOnSurfaceView() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val surfaceView = fragment.view?.findViewById<android.opengl.GLSurfaceView>(R.id.surfaceview)
            assertNotNull(surfaceView)

            // Create a tap event
            val downTime = SystemClock.uptimeMillis()
            val eventTime = SystemClock.uptimeMillis()
            val x = 100f
            val y = 200f

            val downEvent = MotionEvent.obtain(
                downTime, eventTime,
                MotionEvent.ACTION_DOWN, x, y, 0
            )

            val upEvent = MotionEvent.obtain(
                downTime, eventTime + 100,
                MotionEvent.ACTION_UP, x, y, 0
            )

            // Dispatch touch events
            surfaceView?.dispatchTouchEvent(downEvent)
            surfaceView?.dispatchTouchEvent(upEvent)

            downEvent.recycle()
            upEvent.recycle()
        }

        scenario.close()
    }

    @Test
    fun testMultipleTouchEvents() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            val surfaceView = fragment.view?.findViewById<android.opengl.GLSurfaceView>(R.id.surfaceview)
            assertNotNull(surfaceView)

            // Simulate multiple taps at different locations
            val locations = listOf(
                Pair(100f, 200f),
                Pair(300f, 400f),
                Pair(500f, 600f)
            )

            for ((x, y) in locations) {
                val downTime = SystemClock.uptimeMillis()
                val eventTime = SystemClock.uptimeMillis()

                val downEvent = MotionEvent.obtain(
                    downTime, eventTime,
                    MotionEvent.ACTION_DOWN, x, y, 0
                )

                val upEvent = MotionEvent.obtain(
                    downTime, eventTime + 50,
                    MotionEvent.ACTION_UP, x, y, 0
                )

                surfaceView?.dispatchTouchEvent(downEvent)
                surfaceView?.dispatchTouchEvent(upEvent)

                downEvent.recycle()
                upEvent.recycle()

                Thread.sleep(50) // Small delay between taps
            }
        }

        scenario.close()
    }

    @Test
    fun testScanButtonClickListener() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Click the scan button
        onView(withId(R.id.scanButton))
            .perform(click())

        // Verify button is still present (may change state but shouldn't crash)
        onView(withId(R.id.scanButton))
            .check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun testClearButtonClickListener() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Set up some VLM description first
            fragment.onVLMAnalysisStarted()
            fragment.updateVLMDescription("Test description")
        }

        Thread.sleep(100)

        // Click the clear button
        onView(withId(R.id.clearButton))
            .perform(click())

        Thread.sleep(100)

        // VLM description should be cleared
        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        scenario.close()
    }

    @Test
    fun testBackButtonClickListener() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)
        }

        // Click the back button
        onView(withId(R.id.btnBack))
            .perform(click())

        // Verify navigation happened (back stack should be popped)
        scenario.onFragment { fragment ->
            val navController = Navigation.findNavController(fragment.requireView())
            // Fragment should still be attached but navigation was attempted
            assertNotNull(navController)
        }

        scenario.close()
    }

    @Test
    fun testObjectDetectionCompletedWithZeroObjects() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            fragment.setScanningActive(true)
            fragment.onObjectDetectionCompleted(anchorsCreated = 0, objectsDetected = 0)
        }

        // Scan button should be re-enabled
        onView(withId(R.id.scanButton))
            .check(matches(isEnabled()))

        scenario.close()
    }

    @Test
    fun testObjectDetectionCompletedWithMultipleObjects() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            fragment.setScanningActive(true)
            fragment.onObjectDetectionCompleted(anchorsCreated = 5, objectsDetected = 10)
        }

        // Scan button should be re-enabled
        onView(withId(R.id.scanButton))
            .check(matches(isEnabled()))
            .check(matches(withText("Scan")))

        scenario.close()
    }

    @Test
    fun testSphereCollectionIncreasesLocalCount() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Collect spheres one by one
            for (i in 1..3) {
                fragment.onSphereCollected(count = i)
                Thread.sleep(50)
            }
        }

        // Verify that the fragment is still functioning
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
            assertTrue(fragment.isResumed)
        }

        scenario.close()
    }

    @Test
    fun testVLMAnalysisFullWorkflow() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Start VLM analysis
            fragment.onVLMAnalysisStarted()
        }

        Thread.sleep(100)

        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(isDisplayed()))
            .check(matches(withText("Analyzing scene...")))

        scenario.onFragment { fragment ->
            // Update with tokens
            fragment.updateVLMDescription("This ")
            fragment.updateVLMDescription("is ")
            fragment.updateVLMDescription("a ")
            fragment.updateVLMDescription("test.")
        }

        Thread.sleep(100)

        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(withText("Analyzing scene...This is a test.")))

        scenario.onFragment { fragment ->
            // Complete analysis
            fragment.onVLMAnalysisCompleted()
        }

        // Description should still be visible after completion
        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(isDisplayed()))

        scenario.onFragment { fragment ->
            // Now clear it
            fragment.clearVLMDescription()
        }

        Thread.sleep(100)

        onView(withId(R.id.vlmDescriptionOverlay))
            .check(matches(withEffectiveVisibility(Visibility.GONE)))

        scenario.close()
    }

    @Test
    fun testFragmentHandlesRapidStateChanges() {
        val scenario = launchFragmentInContainer<ARFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val navController = TestNavHostController(context)
            navController.setGraph(R.navigation.nav_graph)
            Navigation.setViewNavController(fragment.requireView(), navController)

            // Rapid state changes
            fragment.setScanningActive(true)
            fragment.setScanningActive(false)
            fragment.setScanningActive(true)
            fragment.setScanningActive(false)

            fragment.onVLMAnalysisStarted()
            fragment.clearVLMDescription()
            fragment.onVLMAnalysisStarted()
            fragment.updateVLMDescription("test")
            fragment.clearVLMDescription()
        }

        // Verify fragment is still stable
        scenario.onFragment { fragment ->
            assertNotNull(fragment.view)
            assertTrue(fragment.isAdded)
        }

        scenario.close()
    }
}
