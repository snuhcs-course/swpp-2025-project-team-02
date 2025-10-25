package com.example.fortuna_android.common.helpers

import android.app.Activity
import android.view.View
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for FullScreenHelper
 * Tests full screen mode setup with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class FullScreenHelperTest {

    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().resume().get()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== setFullScreenOnWindowFocusChanged Tests ==========

    @Test
    fun `test setFullScreenOnWindowFocusChanged with hasFocus true sets full screen flags`() {
        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)

        // Assert
        val decorView = activity.window.decorView
        val flags = decorView.systemUiVisibility

        val expectedFlags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        assertEquals("Should set correct fullscreen flags", expectedFlags, flags)
    }

    @Test
    fun `test setFullScreenOnWindowFocusChanged with hasFocus false does not change flags`() {
        // Arrange - Get initial flags
        val initialFlags = activity.window.decorView.systemUiVisibility

        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, false)

        // Assert - Flags should remain unchanged
        val currentFlags = activity.window.decorView.systemUiVisibility
        assertEquals("Flags should not change when hasFocus is false", initialFlags, currentFlags)
    }

    @Test
    fun `test setFullScreenOnWindowFocusChanged can be called multiple times with true`() {
        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)
        val flags1 = activity.window.decorView.systemUiVisibility

        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)
        val flags2 = activity.window.decorView.systemUiVisibility

        // Assert - Flags should remain the same
        assertEquals("Flags should remain the same", flags1, flags2)
    }

    @Test
    fun `test setFullScreenOnWindowFocusChanged with alternating hasFocus values`() {
        // Act & Assert
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)
        val flagsAfterTrue = activity.window.decorView.systemUiVisibility

        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, false)
        val flagsAfterFalse = activity.window.decorView.systemUiVisibility

        // When hasFocus is true, flags are set
        assertNotEquals("Flags should be set when hasFocus is true", 0, flagsAfterTrue)

        // When hasFocus is false, flags should remain unchanged from last set
        assertEquals("Flags should remain unchanged when hasFocus is false",
            flagsAfterTrue, flagsAfterFalse)
    }

    @Test
    fun `test setFullScreenOnWindowFocusChanged with different activities`() {
        // Arrange
        val activity1 = Robolectric.buildActivity(Activity::class.java).create().resume().get()
        val activity2 = Robolectric.buildActivity(Activity::class.java).create().resume().get()

        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity1, true)
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity2, true)

        // Assert
        val flags1 = activity1.window.decorView.systemUiVisibility
        val flags2 = activity2.window.decorView.systemUiVisibility

        val expectedFlags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

        assertEquals("Activity1 should have correct flags", expectedFlags, flags1)
        assertEquals("Activity2 should have correct flags", expectedFlags, flags2)
    }

    // ========== Flag Verification Tests ==========

    @Test
    fun `test fullscreen flags include LAYOUT_STABLE`() {
        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)

        // Assert
        val flags = activity.window.decorView.systemUiVisibility
        assertTrue("Should include LAYOUT_STABLE flag",
            (flags and View.SYSTEM_UI_FLAG_LAYOUT_STABLE) != 0)
    }

    @Test
    fun `test fullscreen flags include LAYOUT_HIDE_NAVIGATION`() {
        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)

        // Assert
        val flags = activity.window.decorView.systemUiVisibility
        assertTrue("Should include LAYOUT_HIDE_NAVIGATION flag",
            (flags and View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION) != 0)
    }

    @Test
    fun `test fullscreen flags include LAYOUT_FULLSCREEN`() {
        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)

        // Assert
        val flags = activity.window.decorView.systemUiVisibility
        assertTrue("Should include LAYOUT_FULLSCREEN flag",
            (flags and View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN) != 0)
    }

    @Test
    fun `test fullscreen flags include HIDE_NAVIGATION`() {
        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)

        // Assert
        val flags = activity.window.decorView.systemUiVisibility
        assertTrue("Should include HIDE_NAVIGATION flag",
            (flags and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) != 0)
    }

    @Test
    fun `test fullscreen flags include FULLSCREEN`() {
        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)

        // Assert
        val flags = activity.window.decorView.systemUiVisibility
        assertTrue("Should include FULLSCREEN flag",
            (flags and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0)
    }

    @Test
    fun `test fullscreen flags include IMMERSIVE_STICKY`() {
        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)

        // Assert
        val flags = activity.window.decorView.systemUiVisibility
        assertTrue("Should include IMMERSIVE_STICKY flag",
            (flags and View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) != 0)
    }

    @Test
    fun `test fullscreen flags include all required flags`() {
        // Act
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)

        // Assert
        val flags = activity.window.decorView.systemUiVisibility
        val allRequiredFlags = listOf(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE,
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION,
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN,
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION,
            View.SYSTEM_UI_FLAG_FULLSCREEN,
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        allRequiredFlags.forEach { flag ->
            assertTrue("Should include flag $flag", (flags and flag) != 0)
        }
    }

    // ========== Integration Tests ==========

    @Test
    fun `test typical onWindowFocusChanged flow - gained focus`() {
        // Act - Simulate gaining focus
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)

        // Assert
        val flags = activity.window.decorView.systemUiVisibility
        assertNotEquals("Flags should be set", 0, flags)
    }

    @Test
    fun `test typical onWindowFocusChanged flow - lost focus`() {
        // Arrange - First set flags
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)
        val flagsAfterGainingFocus = activity.window.decorView.systemUiVisibility

        // Act - Simulate losing focus
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, false)

        // Assert - Flags should remain the same (not cleared)
        val flagsAfterLosingFocus = activity.window.decorView.systemUiVisibility
        assertEquals("Flags should not change when losing focus",
            flagsAfterGainingFocus, flagsAfterLosingFocus)
    }

    @Test
    fun `test typical onWindowFocusChanged flow - focus changes`() {
        // Act - Simulate focus changes
        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)
        val flags1 = activity.window.decorView.systemUiVisibility

        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, false)
        val flags2 = activity.window.decorView.systemUiVisibility

        FullScreenHelper.setFullScreenOnWindowFocusChanged(activity, true)
        val flags3 = activity.window.decorView.systemUiVisibility

        // Assert
        assertNotEquals("Flags should be set after first focus gain", 0, flags1)
        assertEquals("Flags should not change when focus is lost", flags1, flags2)
        assertEquals("Flags should remain the same when focus is gained again", flags1, flags3)
    }
}
