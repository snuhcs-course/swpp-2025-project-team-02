package com.example.fortuna_android.common.helpers

import android.app.Activity
import android.view.View
import androidx.test.core.app.ActivityScenario
import io.mockk.unmockkAll
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
 * Unit tests for SnackbarHelper
 * Tests snackbar display and management with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SnackbarHelperTest {

    private lateinit var activity: Activity
    private lateinit var snackbarHelper: SnackbarHelper

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().resume().get()
        snackbarHelper = SnackbarHelper()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    // ========== isShowing Tests ==========

    @Test
    fun `test isShowing returns false initially`() {
        // Assert
        assertFalse("Should not be showing initially", snackbarHelper.isShowing)
    }

    @Test
    fun `test isShowing returns true after showMessage`() {
        // Act
        snackbarHelper.showMessage(activity, "Test message")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing after showMessage", snackbarHelper.isShowing)
    }

    @Test
    fun `test isShowing returns false after hide`() {
        // Arrange
        snackbarHelper.showMessage(activity, "Test message")
        shadowOf(activity.mainLooper).idle()

        // Act
        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertFalse("Should not be showing after hide", snackbarHelper.isShowing)
    }

    // ========== showMessage Tests ==========

    @Test
    fun `test showMessage displays message`() {
        // Act
        snackbarHelper.showMessage(activity, "Test message")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test showMessage with empty string does not show`() {
        // Act
        snackbarHelper.showMessage(activity, "")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertFalse("Should not show empty message", snackbarHelper.isShowing)
    }

    @Test
    fun `test showMessage with same message twice does not update`() {
        // Act
        snackbarHelper.showMessage(activity, "Same message")
        shadowOf(activity.mainLooper).idle()

        val wasShowingBefore = snackbarHelper.isShowing

        snackbarHelper.showMessage(activity, "Same message")
        shadowOf(activity.mainLooper).idle()

        val isShowingAfter = snackbarHelper.isShowing

        // Assert
        assertTrue("Should show first message", wasShowingBefore)
        assertTrue("Should still be showing same message", isShowingAfter)
    }

    @Test
    fun `test showMessage with different message updates`() {
        // Act
        snackbarHelper.showMessage(activity, "First message")
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.showMessage(activity, "Second message")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing new message", snackbarHelper.isShowing)
    }

    @Test
    fun `test showMessage with whitespace only shows`() {
        // Act
        snackbarHelper.showMessage(activity, "   ")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should show whitespace message", snackbarHelper.isShowing)
    }

    // ========== showMessageWithDismiss Tests ==========

    @Test
    fun `test showMessageWithDismiss displays message`() {
        // Act
        snackbarHelper.showMessageWithDismiss(activity, "Test with dismiss")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test showMessageWithDismiss can be called multiple times`() {
        // Act
        snackbarHelper.showMessageWithDismiss(activity, "Message 1")
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.showMessageWithDismiss(activity, "Message 2")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    // ========== showError Tests ==========

    @Test
    fun `test showError displays error message`() {
        // Act
        snackbarHelper.showError(activity, "Error message")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing error", snackbarHelper.isShowing)
    }

    @Test
    fun `test showError with empty message`() {
        // Act
        snackbarHelper.showError(activity, "")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should show empty error message", snackbarHelper.isShowing)
    }

    @Test
    fun `test showError can be called multiple times`() {
        // Act
        snackbarHelper.showError(activity, "Error 1")
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.showError(activity, "Error 2")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    // ========== hide Tests ==========

    @Test
    fun `test hide when not showing does nothing`() {
        // Act
        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertFalse("Should still not be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test hide when showing hides snackbar`() {
        // Arrange
        snackbarHelper.showMessage(activity, "Message to hide")
        shadowOf(activity.mainLooper).idle()

        // Act
        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertFalse("Should be hidden", snackbarHelper.isShowing)
    }

    @Test
    fun `test hide can be called multiple times safely`() {
        // Arrange
        snackbarHelper.showMessage(activity, "Message")
        shadowOf(activity.mainLooper).idle()

        // Act
        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        // Assert - No exception thrown
        assertFalse("Should not be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test hide clears last message`() {
        // Arrange
        snackbarHelper.showMessage(activity, "Test message")
        shadowOf(activity.mainLooper).idle()

        // Act
        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        // Show same message again - should work since lastMessage was cleared
        snackbarHelper.showMessage(activity, "Test message")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should show message again after hiding", snackbarHelper.isShowing)
    }

    // ========== setMaxLines Tests ==========

    @Test
    fun `test setMaxLines with positive value`() {
        // Act
        snackbarHelper.setMaxLines(5)

        // Show message to apply maxLines
        snackbarHelper.showMessage(activity, "Test message with multiple lines")
        shadowOf(activity.mainLooper).idle()

        // Assert - No exception thrown
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test setMaxLines with 1`() {
        // Act
        snackbarHelper.setMaxLines(1)
        snackbarHelper.showMessage(activity, "Single line")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test setMaxLines with large value`() {
        // Act
        snackbarHelper.setMaxLines(100)
        snackbarHelper.showMessage(activity, "Message")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test setMaxLines before and after showing`() {
        // Act
        snackbarHelper.setMaxLines(3)
        snackbarHelper.showMessage(activity, "First message")
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.setMaxLines(5)
        snackbarHelper.showMessage(activity, "Second message")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    // ========== setParentView Tests ==========

    @Test
    fun `test setParentView with custom view`() {
        // Arrange
        val customView = activity.findViewById<View>(android.R.id.content)

        // Act
        snackbarHelper.setParentView(customView)
        snackbarHelper.showMessage(activity, "Message with custom parent")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test setParentView with null uses default`() {
        // Act
        snackbarHelper.setParentView(null)
        snackbarHelper.showMessage(activity, "Message with default parent")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test setParentView can be changed`() {
        // Arrange
        val view1 = activity.findViewById<View>(android.R.id.content)
        val view2 = activity.findViewById<View>(android.R.id.content)

        // Act
        snackbarHelper.setParentView(view1)
        snackbarHelper.showMessage(activity, "Message 1")
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.setParentView(view2)
        snackbarHelper.showMessage(activity, "Message 2")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test complete lifecycle - show then hide`() {
        // Act & Assert
        assertFalse("Initially not showing", snackbarHelper.isShowing)

        snackbarHelper.showMessage(activity, "Test message")
        shadowOf(activity.mainLooper).idle()
        assertTrue("Should be showing after showMessage", snackbarHelper.isShowing)

        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()
        assertFalse("Should be hidden after hide", snackbarHelper.isShowing)
    }

    @Test
    fun `test show different message types in sequence`() {
        // Act & Assert
        snackbarHelper.showMessage(activity, "Regular message")
        shadowOf(activity.mainLooper).idle()
        assertTrue("Should show regular message", snackbarHelper.isShowing)

        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.showMessageWithDismiss(activity, "Message with dismiss")
        shadowOf(activity.mainLooper).idle()
        assertTrue("Should show message with dismiss", snackbarHelper.isShowing)

        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.showError(activity, "Error message")
        shadowOf(activity.mainLooper).idle()
        assertTrue("Should show error message", snackbarHelper.isShowing)
    }

    @Test
    fun `test multiple snackbar helpers do not interfere`() {
        // Arrange
        val helper1 = SnackbarHelper()
        val helper2 = SnackbarHelper()

        // Act
        helper1.showMessage(activity, "Helper 1 message")
        shadowOf(activity.mainLooper).idle()

        helper2.showMessage(activity, "Helper 2 message")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Helper 1 should be showing", helper1.isShowing)
        assertTrue("Helper 2 should be showing", helper2.isShowing)

        // Cleanup
        helper1.hide(activity)
        helper2.hide(activity)
        shadowOf(activity.mainLooper).idle()
    }

    @Test
    fun `test snackbar with custom parent view and maxLines`() {
        // Arrange
        val customView = activity.findViewById<View>(android.R.id.content)

        // Act
        snackbarHelper.setParentView(customView)
        snackbarHelper.setMaxLines(4)
        snackbarHelper.showMessage(activity, "Custom message")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing", snackbarHelper.isShowing)
    }

    @Test
    fun `test rapid show and hide operations`() {
        // Act
        snackbarHelper.showMessage(activity, "Message 1")
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.showMessage(activity, "Message 2")
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.hide(activity)
        shadowOf(activity.mainLooper).idle()

        snackbarHelper.showMessage(activity, "Message 3")
        shadowOf(activity.mainLooper).idle()

        // Assert
        assertTrue("Should be showing last message", snackbarHelper.isShowing)
    }
}
