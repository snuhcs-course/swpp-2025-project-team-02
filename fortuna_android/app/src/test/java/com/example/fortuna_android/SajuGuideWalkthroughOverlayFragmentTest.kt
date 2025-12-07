package com.example.fortuna_android

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Unit tests for SajuGuideWalkthroughOverlayFragment
 * Tests fragment lifecycle, companion methods, and SharedPreferences logic
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SajuGuideWalkthroughOverlayFragmentTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        ShadowLog.stream = System.out
        context = ApplicationProvider.getApplicationContext()
        // Clear shared preferences before each test
        context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            .edit().clear().commit()
    }

    // ========== Fragment Instantiation Tests ==========

    @Test
    fun `test fragment can be instantiated`() {
        // Act
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Assert
        assertNotNull("Fragment should not be null", fragment)
    }

    @Test
    fun `test fragment is a Fragment`() {
        // Act
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Assert
        assertTrue("Should be a Fragment",
            fragment is androidx.fragment.app.Fragment)
    }

    @Test
    fun `test fragment can be created multiple times`() {
        // Act
        val fragment1 = SajuGuideWalkthroughOverlayFragment()
        val fragment2 = SajuGuideWalkthroughOverlayFragment()
        val fragment3 = SajuGuideWalkthroughOverlayFragment()

        // Assert
        assertNotNull("Fragment 1 should not be null", fragment1)
        assertNotNull("Fragment 2 should not be null", fragment2)
        assertNotNull("Fragment 3 should not be null", fragment3)
        assertNotSame("Fragments should be different instances", fragment1, fragment2)
        assertNotSame("Fragments should be different instances", fragment2, fragment3)
    }

    // ========== Companion Object - newInstance Tests ==========

    @Test
    fun `test newInstance creates fragment`() {
        // Act
        val fragment = SajuGuideWalkthroughOverlayFragment.newInstance()

        // Assert
        assertNotNull("newInstance should create a fragment", fragment)
        assertTrue("newInstance should create SajuGuideWalkthroughOverlayFragment",
            fragment is SajuGuideWalkthroughOverlayFragment)
    }

    @Test
    fun `test newInstance creates different instances`() {
        // Act
        val fragment1 = SajuGuideWalkthroughOverlayFragment.newInstance()
        val fragment2 = SajuGuideWalkthroughOverlayFragment.newInstance()

        // Assert
        assertNotNull("Fragment 1 should not be null", fragment1)
        assertNotNull("Fragment 2 should not be null", fragment2)
        assertNotSame("Each call should create a new instance", fragment1, fragment2)
    }

    @Test
    fun `test newInstance returns correct type`() {
        // Act
        val fragment = SajuGuideWalkthroughOverlayFragment.newInstance()

        // Assert
        assertEquals("newInstance should return exact type",
            SajuGuideWalkthroughOverlayFragment::class.java,
            fragment::class.java)
    }

    // ========== Companion Object - shouldShowWalkthrough Tests ==========

    @Test
    fun `test shouldShowWalkthrough returns true when not completed`() {
        // Arrange - Fresh preferences

        // Act
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context)

        // Assert
        assertTrue("Should show walkthrough when not completed", shouldShow)
    }

    @Test
    fun `test shouldShowWalkthrough returns false when completed`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true)
            .apply()

        // Act
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context)

        // Assert
        assertFalse("Should not show walkthrough when completed", shouldShow)
    }

    @Test
    fun `test shouldShowWalkthrough with explicit false preference`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, false)
            .apply()

        // Act
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context)

        // Assert
        assertTrue("Should show walkthrough when explicitly set to false", shouldShow)
    }

    @Test
    fun `test shouldShowWalkthrough called multiple times`() {
        // Act
        val result1 = SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context)
        val result2 = SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context)
        val result3 = SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context)

        // Assert
        assertEquals("Results should be consistent", result1, result2)
        assertEquals("Results should be consistent", result2, result3)
    }

    @Test
    fun `test shouldShowWalkthrough transitions`() {
        // Act & Assert - Initially should show
        assertTrue("Should show before completion",
            SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context))

        // Mark as completed
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true)
            .apply()

        // Should not show after completion
        assertFalse("Should not show after completion",
            SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context))
    }

    // ========== Companion Object - shouldShowElementNudgeOnHome Tests ==========

    @Test
    fun `test shouldShowElementNudgeOnHome returns false by default`() {
        // Arrange - Fresh preferences

        // Act
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context)

        // Assert
        assertFalse("Should not show element nudge by default", shouldShow)
    }

    @Test
    fun `test shouldShowElementNudgeOnHome returns true when flag set`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
            .apply()

        // Act
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context)

        // Assert
        assertTrue("Should show element nudge when flag is set", shouldShow)
    }

    @Test
    fun `test shouldShowElementNudgeOnHome with explicit false`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, false)
            .apply()

        // Act
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context)

        // Assert
        assertFalse("Should not show when explicitly false", shouldShow)
    }

    // ========== Companion Object - clearElementNudgeFlag Tests ==========

    @Test
    fun `test clearElementNudgeFlag clears the flag`() {
        // Arrange - Set flag to true
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
            .apply()

        // Act
        SajuGuideWalkthroughOverlayFragment.clearElementNudgeFlag(context)

        // Assert
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context)
        assertFalse("Flag should be cleared", shouldShow)
    }

    @Test
    fun `test clearElementNudgeFlag when already false`() {
        // Arrange - Flag is already false (default)

        // Act
        SajuGuideWalkthroughOverlayFragment.clearElementNudgeFlag(context)

        // Assert
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context)
        assertFalse("Flag should remain false", shouldShow)
    }

    @Test
    fun `test clearElementNudgeFlag called multiple times`() {
        // Arrange - Set flag
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
            .apply()

        // Act - Clear multiple times
        SajuGuideWalkthroughOverlayFragment.clearElementNudgeFlag(context)
        SajuGuideWalkthroughOverlayFragment.clearElementNudgeFlag(context)
        SajuGuideWalkthroughOverlayFragment.clearElementNudgeFlag(context)

        // Assert
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context)
        assertFalse("Flag should stay cleared", shouldShow)
    }

    // ========== Companion Object - Constants Tests ==========

    @Test
    fun `test TAG constant`() {
        // Act
        val tag = SajuGuideWalkthroughOverlayFragment.TAG

        // Assert
        assertEquals("TAG should be 'SajuGuideWalkthrough'",
            "SajuGuideWalkthrough", tag)
    }

    @Test
    fun `test PREF_KEY_HAS_COMPLETED_WALKTHROUGH constant`() {
        // Act
        val key = SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH

        // Assert
        assertEquals("Key should be 'has_completed_saju_guide_walkthrough'",
            "has_completed_saju_guide_walkthrough", key)
    }

    @Test
    fun `test PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME constant`() {
        // Act
        val key = SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME

        // Assert
        assertEquals("Key should be 'show_element_nudge_on_home'",
            "show_element_nudge_on_home", key)
    }

    @Test
    fun `test constants are not empty`() {
        // Assert
        assertFalse("TAG should not be empty",
            SajuGuideWalkthroughOverlayFragment.TAG.isEmpty())
        assertFalse("PREF_KEY_HAS_COMPLETED_WALKTHROUGH should not be empty",
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH.isEmpty())
        assertFalse("PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME should not be empty",
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME.isEmpty())
    }

    @Test
    fun `test constants are unique`() {
        // Assert
        assertNotEquals("TAG and PREF_KEY_HAS_COMPLETED_WALKTHROUGH should be different",
            SajuGuideWalkthroughOverlayFragment.TAG,
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH)

        assertNotEquals("PREF_KEY_HAS_COMPLETED_WALKTHROUGH and PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME should be different",
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH,
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME)

        assertNotEquals("TAG and PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME should be different",
            SajuGuideWalkthroughOverlayFragment.TAG,
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test complete walkthrough workflow`() {
        // 1. User starts walkthrough
        assertTrue("Should show walkthrough initially",
            SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context))
        assertFalse("Element nudge flag not set initially",
            SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context))

        // 2. User completes walkthrough
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true)
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
            .apply()

        assertFalse("Should not show walkthrough after completion",
            SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context))
        assertTrue("Element nudge flag should be set",
            SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context))

        // 3. User returns to home and sees element nudge
        SajuGuideWalkthroughOverlayFragment.clearElementNudgeFlag(context)

        assertFalse("Element nudge flag should be cleared",
            SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context))
        assertFalse("Walkthrough should still be marked as completed",
            SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context))
    }

    @Test
    fun `test preference isolation`() {
        // Set walkthrough completed
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true)
            .apply()

        // Element nudge flag should still be false
        assertFalse("Element nudge flag should be independent",
            SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context))

        // Set element nudge flag
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
            .apply()

        // Walkthrough completion should still be true
        assertFalse("Walkthrough completion should be independent",
            SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context))
    }

    @Test
    fun `test element nudge flag lifecycle`() {
        // Start with flag not set
        assertFalse("Flag should be false initially",
            SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context))

        // Set flag
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
            .apply()

        assertTrue("Flag should be true after setting",
            SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context))

        // Clear flag
        SajuGuideWalkthroughOverlayFragment.clearElementNudgeFlag(context)

        assertFalse("Flag should be false after clearing",
            SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context))
    }

    @Test
    fun `test all companion methods work together`() {
        // Create instance
        val fragment = SajuGuideWalkthroughOverlayFragment.newInstance()
        assertNotNull("Fragment created", fragment)

        // Check walkthrough
        val shouldShowWalkthrough = SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context)
        assertTrue("Should show walkthrough", shouldShowWalkthrough)

        // Check element nudge
        val shouldShowElement = SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context)
        assertFalse("Should not show element nudge", shouldShowElement)

        // Clear element flag (should work even if false)
        SajuGuideWalkthroughOverlayFragment.clearElementNudgeFlag(context)
        assertTrue("All methods work together", true)
    }

    @Test
    fun `test preference keys are correctly formatted`() {
        // Verify preference keys follow expected naming convention
        assertTrue("Walkthrough key should contain 'walkthrough'",
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH.contains("walkthrough"))
        assertTrue("Element key should contain 'element'",
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME.contains("element"))
        assertTrue("Element key should contain 'home'",
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME.contains("home"))
    }

    @Test
    fun `test boolean inversion logic for walkthrough`() {
        // shouldShowWalkthrough returns !hasCompleted
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)

        // Test false -> should show (true)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, false)
            .apply()
        assertTrue("false completion should return true",
            SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context))

        // Test true -> should not show (false)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true)
            .apply()
        assertFalse("true completion should return false",
            SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context))
    }

    @Test
    fun `test different context instances`() {
        // Use application context
        val appContext = ApplicationProvider.getApplicationContext<Context>()

        // Set preference using one context
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true)
            .apply()

        // Read using different context
        val shouldShow = SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(appContext)

        // Assert
        assertFalse("Should work with different context instances", shouldShow)
    }

    @Test
    fun `test edge case - rapid flag changes`() {
        // Rapidly change element nudge flag
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)

        repeat(10) { i ->
            val setValue = i % 2 == 0
            prefs.edit()
                .putBoolean(SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, setValue)
                .apply()

            val result = SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context)
            assertEquals("Result should match set value on iteration $i", setValue, result)
        }
    }

    @Test
    fun `test companion object is accessible`() {
        // Verify all companion methods and constants are accessible
        assertNotNull("TAG accessible", SajuGuideWalkthroughOverlayFragment.TAG)
        assertNotNull("PREF_KEY_HAS_COMPLETED_WALKTHROUGH accessible",
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_HAS_COMPLETED_WALKTHROUGH)
        assertNotNull("PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME accessible",
            SajuGuideWalkthroughOverlayFragment.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME)

        val fragment = SajuGuideWalkthroughOverlayFragment.newInstance()
        assertNotNull("newInstance accessible", fragment)

        val walkthrough = SajuGuideWalkthroughOverlayFragment.shouldShowWalkthrough(context)
        assertNotNull("shouldShowWalkthrough accessible", walkthrough)

        val elementNudge = SajuGuideWalkthroughOverlayFragment.shouldShowElementNudgeOnHome(context)
        assertNotNull("shouldShowElementNudgeOnHome accessible", elementNudge)

        SajuGuideWalkthroughOverlayFragment.clearElementNudgeFlag(context)
        assertTrue("clearElementNudgeFlag accessible", true)
    }

    // ========== Pure Function Tests - calculateNextPageState ==========

    @Test
    fun `test calculateNextPageState advances when not at last page`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act
        val result = fragment.calculateNextPageState(0, 6)

        // Assert
        assertTrue("Should advance", result.shouldAdvance)
        assertEquals("Next page should be 1", 1, result.nextPageIndex)
    }

    @Test
    fun `test calculateNextPageState advances through middle pages`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act & Assert
        for (i in 0 until 5) {
            val result = fragment.calculateNextPageState(i, 6)
            assertTrue("Should advance from page $i", result.shouldAdvance)
            assertEquals("Next page should be ${i + 1}", i + 1, result.nextPageIndex)
        }
    }

    @Test
    fun `test calculateNextPageState does not advance at last page`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act
        val result = fragment.calculateNextPageState(5, 6)

        // Assert
        assertFalse("Should not advance at last page", result.shouldAdvance)
        assertEquals("Page index should remain 5", 5, result.nextPageIndex)
    }

    @Test
    fun `test calculateNextPageState with single page`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act
        val result = fragment.calculateNextPageState(0, 1)

        // Assert
        assertFalse("Should not advance with single page", result.shouldAdvance)
        assertEquals("Page index should remain 0", 0, result.nextPageIndex)
    }

    // ========== Pure Function Tests - calculateDialogueUIState ==========

    @Test
    fun `test calculateDialogueUIState for first page`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act
        val result = fragment.calculateDialogueUIState(0, 6)

        // Assert
        assertFalse("Should not be last page", result.isLastPage)
        assertTrue("Should show arrow", result.showArrow)
    }

    @Test
    fun `test calculateDialogueUIState for middle pages`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act & Assert
        for (i in 1 until 5) {
            val result = fragment.calculateDialogueUIState(i, 6)
            assertFalse("Should not be last page at index $i", result.isLastPage)
            assertTrue("Should show arrow at index $i", result.showArrow)
        }
    }

    @Test
    fun `test calculateDialogueUIState for last page`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act
        val result = fragment.calculateDialogueUIState(5, 6)

        // Assert
        assertTrue("Should be last page", result.isLastPage)
        assertFalse("Should not show arrow on last page", result.showArrow)
    }

    // ========== Pure Function Tests - getDialogueForPage ==========

    @Test
    fun `test getDialogueForPage returns correct dialogue`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act & Assert
        for (i in fragment.dialogues.indices) {
            val dialogue = fragment.getDialogueForPage(i)
            assertNotNull("Dialogue at index $i should not be null", dialogue)
            assertEquals("Dialogue should match", fragment.dialogues[i], dialogue)
        }
    }

    @Test
    fun `test getDialogueForPage returns null for invalid index`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act & Assert
        assertNull("Dialogue at -1 should be null", fragment.getDialogueForPage(-1))
        assertNull("Dialogue beyond size should be null", fragment.getDialogueForPage(100))
    }

    // ========== Pure Function Tests - getCompletionMessage ==========

    @Test
    fun `test getCompletionMessage returns non-empty message`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act
        val message = fragment.getCompletionMessage()

        // Assert
        assertNotNull("Completion message should not be null", message)
        assertTrue("Completion message should not be empty", message.isNotEmpty())
    }

    @Test
    fun `test getCompletionMessage contains expected content`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act
        val message = fragment.getCompletionMessage()

        // Assert
        assertTrue("Should mention completion", message.contains("모두 둘러봤어요"))
        assertTrue("Should mention home button", message.contains("오늘의 기운 보충하러 가기"))
    }

    // ========== Data Class Tests ==========

    @Test
    fun `test PageAdvanceResult data class`() {
        // Act
        val result1 = SajuGuideWalkthroughOverlayFragment.PageAdvanceResult(true, 1)
        val result2 = SajuGuideWalkthroughOverlayFragment.PageAdvanceResult(true, 1)
        val result3 = SajuGuideWalkthroughOverlayFragment.PageAdvanceResult(false, 0)

        // Assert
        assertEquals("Equal results should be equal", result1, result2)
        assertNotEquals("Different results should not be equal", result1, result3)
        assertTrue("shouldAdvance should be true", result1.shouldAdvance)
        assertEquals("nextPageIndex should be 1", 1, result1.nextPageIndex)
    }

    @Test
    fun `test DialogueUIState data class`() {
        // Act
        val state1 = SajuGuideWalkthroughOverlayFragment.DialogueUIState(false, true)
        val state2 = SajuGuideWalkthroughOverlayFragment.DialogueUIState(false, true)
        val state3 = SajuGuideWalkthroughOverlayFragment.DialogueUIState(true, false)

        // Assert
        assertEquals("Equal states should be equal", state1, state2)
        assertNotEquals("Different states should not be equal", state1, state3)
        assertFalse("isLastPage should be false", state1.isLastPage)
        assertTrue("showArrow should be true", state1.showArrow)
    }

    // ========== Internal Fields Tests ==========

    @Test
    fun `test dialogues list has correct count`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Assert
        assertEquals("Dialogues should have 6 items", 6, fragment.dialogues.size)
        assertEquals("totalPages should match dialogues count", fragment.dialogues.size, fragment.totalPages)
    }

    @Test
    fun `test currentPageIndex initial value`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Assert
        assertEquals("Initial page index should be 0", 0, fragment.currentPageIndex)
    }

    @Test
    fun `test currentPageIndex can be modified`() {
        // Arrange
        val fragment = SajuGuideWalkthroughOverlayFragment()

        // Act
        fragment.currentPageIndex = 3

        // Assert
        assertEquals("Page index should be 3", 3, fragment.currentPageIndex)
    }
}
