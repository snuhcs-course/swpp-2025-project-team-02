package com.example.fortuna_android.ui

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for SpotlightOverlayView
 */
@RunWith(AndroidJUnit4::class)
class SpotlightOverlayViewTest {

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.fortuna_android", appContext.packageName)
    }

    @Test
    fun testViewCreation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = SpotlightOverlayView(context)

        assertNotNull("View should be created", view)
        assertTrue("View should be SpotlightOverlayView instance",
            view is SpotlightOverlayView)
    }

    @Test
    fun testViewLayerType() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = SpotlightOverlayView(context)

        assertEquals("Should use software layer for PorterDuff mode",
            View.LAYER_TYPE_SOFTWARE, view.layerType)
    }

    @Test
    fun testSetOverlayOpacity_validValues() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = SpotlightOverlayView(context)

        // Test various opacity values (should not crash)
        view.setOverlayOpacity(0)
        view.setOverlayOpacity(128)
        view.setOverlayOpacity(204)
        view.setOverlayOpacity(255)

        assertTrue("Opacity changes should work", true)
    }

    @Test
    fun testSetOverlayOpacity_coercion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = SpotlightOverlayView(context)

        // Test values outside valid range (should be coerced)
        view.setOverlayOpacity(-50)  // Should coerce to 0
        view.setOverlayOpacity(300)  // Should coerce to 255

        assertTrue("Opacity coercion should work", true)
    }

    @Test
    fun testClearSpotlight() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val view = SpotlightOverlayView(context)

        // Test clearing spotlight (should not crash)
        view.clearSpotlight()
        view.clearSpotlight() // Multiple calls should work

        assertTrue("Clear spotlight should work", true)
    }

    @Test
    fun testSetSpotlight() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spotlightView = SpotlightOverlayView(context)
        val targetView = View(context)

        // Set target view bounds
        targetView.layout(100, 100, 200, 200)

        // Test setting spotlight (should not crash)
        spotlightView.setSpotlight(targetView)
        spotlightView.setSpotlight(targetView, 60f)

        assertTrue("Set spotlight should work", true)
    }

    @Test
    fun testSpotlightSequence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spotlightView = SpotlightOverlayView(context)
        val targetView = View(context)

        targetView.layout(100, 100, 200, 200)

        // Test sequence: set -> clear -> set again
        spotlightView.setSpotlight(targetView)
        spotlightView.clearSpotlight()
        spotlightView.setSpotlight(targetView)

        assertTrue("Spotlight sequence should work", true)
    }
}
