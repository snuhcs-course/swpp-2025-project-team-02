package com.example.fortuna_android.common.helpers

import android.app.Activity
import android.view.Window
import android.view.WindowManager
import com.google.ar.core.Camera
import com.google.ar.core.TrackingFailureReason
import com.google.ar.core.TrackingState
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Unit tests for TrackingStateHelper
 * Tests tracking state management and failure reason strings with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TrackingStateHelperTest {

    private lateinit var activity: Activity
    private lateinit var trackingStateHelper: TrackingStateHelper

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().resume().get()
        trackingStateHelper = TrackingStateHelper(activity)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== Constructor Tests ==========

    @Test
    fun `test TrackingStateHelper initializes with activity`() {
        // Assert
        assertNotNull("TrackingStateHelper should be initialized", trackingStateHelper)
    }

    @Test
    fun `test multiple TrackingStateHelper instances can be created`() {
        // Act
        val helper1 = TrackingStateHelper(activity)
        val helper2 = TrackingStateHelper(activity)

        // Assert
        assertNotNull("First helper should be initialized", helper1)
        assertNotNull("Second helper should be initialized", helper2)
        assertNotSame("Helpers should be different instances", helper1, helper2)
    }

    // ========== updateKeepScreenOnFlag Tests - TRACKING State ==========

    @Test
    fun `test updateKeepScreenOnFlag with TRACKING sets FLAG_KEEP_SCREEN_ON`() {
        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()

        // Assert
        val flags = activity.window.attributes.flags
        assertTrue("FLAG_KEEP_SCREEN_ON should be set",
            (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `test updateKeepScreenOnFlag with TRACKING called twice does not change flag`() {
        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        val flagsAfterFirst = activity.window.attributes.flags

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        val flagsAfterSecond = activity.window.attributes.flags

        // Assert
        assertEquals("Flags should not change when same state is set twice",
            flagsAfterFirst, flagsAfterSecond)
    }

    // ========== updateKeepScreenOnFlag Tests - PAUSED State ==========

    @Test
    fun `test updateKeepScreenOnFlag with PAUSED clears FLAG_KEEP_SCREEN_ON`() {
        // Arrange - First set TRACKING
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()

        // Act - Then set PAUSED
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()

        // Assert
        val flags = activity.window.attributes.flags
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared",
            (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `test updateKeepScreenOnFlag with PAUSED called twice does not change flag`() {
        // Arrange
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()

        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()
        val flagsAfterFirst = activity.window.attributes.flags

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()
        val flagsAfterSecond = activity.window.attributes.flags

        // Assert
        assertEquals("Flags should not change when same state is set twice",
            flagsAfterFirst, flagsAfterSecond)
    }

    // ========== updateKeepScreenOnFlag Tests - STOPPED State ==========

    @Test
    fun `test updateKeepScreenOnFlag with STOPPED clears FLAG_KEEP_SCREEN_ON`() {
        // Arrange - First set TRACKING
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()

        // Act - Then set STOPPED
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.STOPPED)
        shadowOf(activity.mainLooper).idle()

        // Assert
        val flags = activity.window.attributes.flags
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared",
            (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `test updateKeepScreenOnFlag with STOPPED called twice does not change flag`() {
        // Arrange
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()

        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.STOPPED)
        shadowOf(activity.mainLooper).idle()
        val flagsAfterFirst = activity.window.attributes.flags

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.STOPPED)
        shadowOf(activity.mainLooper).idle()
        val flagsAfterSecond = activity.window.attributes.flags

        // Assert
        assertEquals("Flags should not change when same state is set twice",
            flagsAfterFirst, flagsAfterSecond)
    }

    // ========== updateKeepScreenOnFlag Tests - State Transitions ==========

    @Test
    fun `test updateKeepScreenOnFlag transitions from TRACKING to PAUSED`() {
        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        val flagsWhileTracking = activity.window.attributes.flags

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()
        val flagsWhilePaused = activity.window.attributes.flags

        // Assert
        assertTrue("FLAG_KEEP_SCREEN_ON should be set while tracking",
            (flagsWhileTracking and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared while paused",
            (flagsWhilePaused and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `test updateKeepScreenOnFlag transitions from TRACKING to STOPPED`() {
        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        val flagsWhileTracking = activity.window.attributes.flags

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.STOPPED)
        shadowOf(activity.mainLooper).idle()
        val flagsWhileStopped = activity.window.attributes.flags

        // Assert
        assertTrue("FLAG_KEEP_SCREEN_ON should be set while tracking",
            (flagsWhileTracking and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared while stopped",
            (flagsWhileStopped and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `test updateKeepScreenOnFlag transitions from PAUSED to TRACKING`() {
        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()
        val flagsWhilePaused = activity.window.attributes.flags

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        val flagsWhileTracking = activity.window.attributes.flags

        // Assert
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared while paused",
            (flagsWhilePaused and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
        assertTrue("FLAG_KEEP_SCREEN_ON should be set while tracking",
            (flagsWhileTracking and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `test updateKeepScreenOnFlag transitions from STOPPED to TRACKING`() {
        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.STOPPED)
        shadowOf(activity.mainLooper).idle()
        val flagsWhileStopped = activity.window.attributes.flags

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        val flagsWhileTracking = activity.window.attributes.flags

        // Assert
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared while stopped",
            (flagsWhileStopped and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
        assertTrue("FLAG_KEEP_SCREEN_ON should be set while tracking",
            (flagsWhileTracking and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `test updateKeepScreenOnFlag transitions from PAUSED to STOPPED`() {
        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()
        val flagsWhilePaused = activity.window.attributes.flags

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.STOPPED)
        shadowOf(activity.mainLooper).idle()
        val flagsWhileStopped = activity.window.attributes.flags

        // Assert - Both should have cleared flags
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared while paused",
            (flagsWhilePaused and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared while stopped",
            (flagsWhileStopped and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `test updateKeepScreenOnFlag transitions from STOPPED to PAUSED`() {
        // Act
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.STOPPED)
        shadowOf(activity.mainLooper).idle()
        val flagsWhileStopped = activity.window.attributes.flags

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()
        val flagsWhilePaused = activity.window.attributes.flags

        // Assert - Both should have cleared flags
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared while stopped",
            (flagsWhileStopped and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared while paused",
            (flagsWhilePaused and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    // ========== updateKeepScreenOnFlag Tests - Multiple Transitions ==========

    @Test
    fun `test updateKeepScreenOnFlag with multiple state transitions`() {
        // Act & Assert
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        assertTrue("Should be set after TRACKING",
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()
        assertFalse("Should be cleared after PAUSED",
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        assertTrue("Should be set again after TRACKING",
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)

        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.STOPPED)
        shadowOf(activity.mainLooper).idle()
        assertFalse("Should be cleared after STOPPED",
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    // ========== getTrackingFailureReasonString Tests ==========

    @Test
    fun `test getTrackingFailureReasonString with NONE returns empty string`() {
        // Arrange
        val mockCamera = mockk<Camera>()
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.NONE

        // Act
        val message = TrackingStateHelper.getTrackingFailureReasonString(mockCamera)

        // Assert
        assertEquals("Should return empty string for NONE", "", message)
        verify { mockCamera.trackingFailureReason }
    }

    @Test
    fun `test getTrackingFailureReasonString with BAD_STATE returns correct message`() {
        // Arrange
        val mockCamera = mockk<Camera>()
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.BAD_STATE

        // Act
        val message = TrackingStateHelper.getTrackingFailureReasonString(mockCamera)

        // Assert
        assertEquals("Should return BAD_STATE message",
            "Tracking lost due to bad internal state. Please try restarting the AR experience.",
            message)
        verify { mockCamera.trackingFailureReason }
    }

    @Test
    fun `test getTrackingFailureReasonString with INSUFFICIENT_LIGHT returns correct message`() {
        // Arrange
        val mockCamera = mockk<Camera>()
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.INSUFFICIENT_LIGHT

        // Act
        val message = TrackingStateHelper.getTrackingFailureReasonString(mockCamera)

        // Assert
        assertEquals("Should return INSUFFICIENT_LIGHT message",
            "Too dark. Try moving to a well-lit area.",
            message)
        verify { mockCamera.trackingFailureReason }
    }

    @Test
    fun `test getTrackingFailureReasonString with EXCESSIVE_MOTION returns correct message`() {
        // Arrange
        val mockCamera = mockk<Camera>()
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.EXCESSIVE_MOTION

        // Act
        val message = TrackingStateHelper.getTrackingFailureReasonString(mockCamera)

        // Assert
        assertEquals("Should return EXCESSIVE_MOTION message",
            "Moving too fast. Slow down.",
            message)
        verify { mockCamera.trackingFailureReason }
    }

    @Test
    fun `test getTrackingFailureReasonString with INSUFFICIENT_FEATURES returns correct message`() {
        // Arrange
        val mockCamera = mockk<Camera>()
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.INSUFFICIENT_FEATURES

        // Act
        val message = TrackingStateHelper.getTrackingFailureReasonString(mockCamera)

        // Assert
        assertEquals("Should return INSUFFICIENT_FEATURES message",
            "Can't find anything. Aim device at a surface with more texture or color.",
            message)
        verify { mockCamera.trackingFailureReason }
    }

    @Test
    fun `test getTrackingFailureReasonString with CAMERA_UNAVAILABLE returns correct message`() {
        // Arrange
        val mockCamera = mockk<Camera>()
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.CAMERA_UNAVAILABLE

        // Act
        val message = TrackingStateHelper.getTrackingFailureReasonString(mockCamera)

        // Assert
        assertEquals("Should return CAMERA_UNAVAILABLE message",
            "Another app is using the camera. Tap on this app or try closing the other one.",
            message)
        verify { mockCamera.trackingFailureReason }
    }

    @Test
    fun `test getTrackingFailureReasonString can be called multiple times`() {
        // Arrange
        val mockCamera = mockk<Camera>()
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.EXCESSIVE_MOTION

        // Act
        val message1 = TrackingStateHelper.getTrackingFailureReasonString(mockCamera)
        val message2 = TrackingStateHelper.getTrackingFailureReasonString(mockCamera)
        val message3 = TrackingStateHelper.getTrackingFailureReasonString(mockCamera)

        // Assert
        assertEquals("All messages should be the same", message1, message2)
        assertEquals("All messages should be the same", message2, message3)
        verify(exactly = 3) { mockCamera.trackingFailureReason }
    }

    @Test
    fun `test getTrackingFailureReasonString with different cameras`() {
        // Arrange
        val mockCamera1 = mockk<Camera>()
        val mockCamera2 = mockk<Camera>()
        every { mockCamera1.trackingFailureReason } returns TrackingFailureReason.INSUFFICIENT_LIGHT
        every { mockCamera2.trackingFailureReason } returns TrackingFailureReason.EXCESSIVE_MOTION

        // Act
        val message1 = TrackingStateHelper.getTrackingFailureReasonString(mockCamera1)
        val message2 = TrackingStateHelper.getTrackingFailureReasonString(mockCamera2)

        // Assert
        assertEquals("Camera1 message should be INSUFFICIENT_LIGHT",
            "Too dark. Try moving to a well-lit area.", message1)
        assertEquals("Camera2 message should be EXCESSIVE_MOTION",
            "Moving too fast. Slow down.", message2)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test typical ARCore tracking lifecycle`() {
        // Act & Assert - Start with PAUSED
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()
        assertFalse("Screen should be allowed to sleep when paused",
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)

        // Transition to TRACKING
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        assertTrue("Screen should stay on when tracking",
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)

        // Transition to STOPPED
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.STOPPED)
        shadowOf(activity.mainLooper).idle()
        assertFalse("Screen should be allowed to sleep when stopped",
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)

        // Back to TRACKING
        trackingStateHelper.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()
        assertTrue("Screen should stay on when tracking again",
            (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }

    @Test
    fun `test complete error message flow for all failure reasons`() {
        // Arrange
        val mockCamera = mockk<Camera>()

        // Test NONE
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.NONE
        assertEquals("", TrackingStateHelper.getTrackingFailureReasonString(mockCamera))

        // Test BAD_STATE
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.BAD_STATE
        assertTrue(TrackingStateHelper.getTrackingFailureReasonString(mockCamera).isNotEmpty())

        // Test INSUFFICIENT_LIGHT
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.INSUFFICIENT_LIGHT
        assertTrue(TrackingStateHelper.getTrackingFailureReasonString(mockCamera).contains("dark"))

        // Test EXCESSIVE_MOTION
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.EXCESSIVE_MOTION
        assertTrue(TrackingStateHelper.getTrackingFailureReasonString(mockCamera).contains("fast"))

        // Test INSUFFICIENT_FEATURES
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.INSUFFICIENT_FEATURES
        assertTrue(TrackingStateHelper.getTrackingFailureReasonString(mockCamera).contains("texture"))

        // Test CAMERA_UNAVAILABLE
        every { mockCamera.trackingFailureReason } returns TrackingFailureReason.CAMERA_UNAVAILABLE
        assertTrue(TrackingStateHelper.getTrackingFailureReasonString(mockCamera).contains("camera"))
    }

    @Test
    fun `test multiple helpers do not interfere with each other`() {
        // Arrange
        val helper1 = TrackingStateHelper(activity)
        val helper2 = TrackingStateHelper(activity)

        // Act
        helper1.updateKeepScreenOnFlag(TrackingState.TRACKING)
        shadowOf(activity.mainLooper).idle()

        helper2.updateKeepScreenOnFlag(TrackingState.PAUSED)
        shadowOf(activity.mainLooper).idle()

        // Assert - The last helper to update should determine the flag state
        // Since both helpers share the same activity, the last update wins
        val flags = activity.window.attributes.flags
        assertFalse("FLAG_KEEP_SCREEN_ON should be cleared by helper2",
            (flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0)
    }
}
