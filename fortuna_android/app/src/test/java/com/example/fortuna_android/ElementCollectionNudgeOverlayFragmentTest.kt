package com.example.fortuna_android

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Unit tests for ElementCollectionNudgeOverlayFragment
 * Tests fragment lifecycle, companion methods, and SharedPreferences logic
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ElementCollectionNudgeOverlayFragmentTest {

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
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Assert
        assertNotNull("Fragment should not be null", fragment)
    }

    @Test
    fun `test fragment is a Fragment`() {
        // Act
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Assert
        assertTrue("Should be a Fragment",
            fragment is androidx.fragment.app.Fragment)
    }

    // ========== Companion Object - newInstance Tests ==========

    @Test
    fun `test newInstance creates fragment`() {
        // Act
        val fragment = ElementCollectionNudgeOverlayFragment.newInstance()

        // Assert
        assertNotNull("newInstance should create a fragment", fragment)
        assertTrue("newInstance should create ElementCollectionNudgeOverlayFragment",
            fragment is ElementCollectionNudgeOverlayFragment)
    }

    @Test
    fun `test newInstance creates different instances`() {
        // Act
        val fragment1 = ElementCollectionNudgeOverlayFragment.newInstance()
        val fragment2 = ElementCollectionNudgeOverlayFragment.newInstance()

        // Assert
        assertNotNull("Fragment 1 should not be null", fragment1)
        assertNotNull("Fragment 2 should not be null", fragment2)
        assertNotSame("Each call should create a new instance", fragment1, fragment2)
    }

    // ========== Companion Object - shouldShowNudge Tests ==========

    @Test
    fun `test shouldShowNudge returns false when element nudge already seen`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE, true)
            .apply()

        // Act
        val shouldShow = ElementCollectionNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertFalse("Should not show when already seen", shouldShow)
    }

    @Test
    fun `test shouldShowNudge returns false when saju guide nudge not seen`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false)
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false)
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE, true)
            .apply()

        // Act
        val shouldShow = ElementCollectionNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertFalse("Should not show when saju guide nudge not seen", shouldShow)
    }

    @Test
    fun `test shouldShowNudge returns false when did not go to saju guide`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false)
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE, false)
            .apply()

        // Act
        val shouldShow = ElementCollectionNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertFalse("Should not show when user did not go to saju guide", shouldShow)
    }

    @Test
    fun `test shouldShowNudge returns true when all conditions met`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false)
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE, true)
            .apply()

        // Act
        val shouldShow = ElementCollectionNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertTrue("Should show when all conditions are met", shouldShow)
    }

    @Test
    fun `test shouldShowNudge with fresh preferences`() {
        // Arrange - No preferences set (all default to false)

        // Act
        val shouldShow = ElementCollectionNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertFalse("Should not show with default preferences", shouldShow)
    }

    // ========== Companion Object - markWentToSajuGuide Tests ==========

    @Test
    fun `test markWentToSajuGuide sets preference`() {
        // Act
        ElementCollectionNudgeOverlayFragment.markWentToSajuGuide(context)

        // Assert
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        val wentToGuide = prefs.getBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE, false)
        assertTrue("Preference should be set to true", wentToGuide)
    }

    @Test
    fun `test markWentToSajuGuide called multiple times`() {
        // Act
        ElementCollectionNudgeOverlayFragment.markWentToSajuGuide(context)
        ElementCollectionNudgeOverlayFragment.markWentToSajuGuide(context)
        ElementCollectionNudgeOverlayFragment.markWentToSajuGuide(context)

        // Assert
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        val wentToGuide = prefs.getBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE, false)
        assertTrue("Preference should still be true", wentToGuide)
    }

    @Test
    fun `test markWentToSajuGuide affects shouldShowNudge`() {
        // Arrange - Set up conditions
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false)
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        // Act
        val beforeMark = ElementCollectionNudgeOverlayFragment.shouldShowNudge(context)
        ElementCollectionNudgeOverlayFragment.markWentToSajuGuide(context)
        val afterMark = ElementCollectionNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertFalse("Should not show before marking", beforeMark)
        assertTrue("Should show after marking", afterMark)
    }

    // ========== Companion Object - Constants Tests ==========

    @Test
    fun `test TAG constant`() {
        // Act
        val tag = ElementCollectionNudgeOverlayFragment.TAG

        // Assert
        assertEquals("TAG should be 'ElementCollectionNudge'", "ElementCollectionNudge", tag)
    }

    @Test
    fun `test PREF_KEY_HAS_SEEN_NUDGE constant`() {
        // Act
        val key = ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE

        // Assert
        assertEquals("Key should be 'has_seen_element_collection_nudge'",
            "has_seen_element_collection_nudge", key)
    }

    @Test
    fun `test PREF_KEY_WENT_TO_SAJU_GUIDE constant`() {
        // Act
        val key = ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE

        // Assert
        assertEquals("Key should be 'went_to_saju_guide_from_nudge'",
            "went_to_saju_guide_from_nudge", key)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test complete workflow - user sees nudge`() {
        // Arrange - User has seen saju guide nudge and went to guide
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        ElementCollectionNudgeOverlayFragment.markWentToSajuGuide(context)

        // Act & Assert - Should show element nudge
        assertTrue("Should show element collection nudge",
            ElementCollectionNudgeOverlayFragment.shouldShowNudge(context))

        // Simulate user seeing the nudge
        prefs.edit()
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        // Should not show again
        assertFalse("Should not show element nudge again",
            ElementCollectionNudgeOverlayFragment.shouldShowNudge(context))
    }

    @Test
    fun `test workflow - user skips saju guide`() {
        // Arrange - User sees saju guide nudge but doesn't go
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE, false)
            .apply()

        // Act & Assert - Should not show element nudge
        assertFalse("Should not show when user skipped saju guide",
            ElementCollectionNudgeOverlayFragment.shouldShowNudge(context))
    }

    @Test
    fun `test preferences isolation`() {
        // Test that different preference keys don't interfere

        // Set element nudge seen
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        // Check saju guide nudge is still false
        val sajuSeen = prefs.getBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false)
        assertFalse("Saju guide nudge should be independent", sajuSeen)
    }

    @Test
    fun `test multiple conditions edge cases`() {
        // Test various combinations
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)

        // Case 1: All true
        prefs.edit()
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE, true)
            .apply()
        assertFalse("Should not show when already seen",
            ElementCollectionNudgeOverlayFragment.shouldShowNudge(context))

        // Case 2: Only went to guide
        prefs.edit().clear().apply()
        prefs.edit()
            .putBoolean(ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE, true)
            .apply()
        assertFalse("Should not show with only went to guide",
            ElementCollectionNudgeOverlayFragment.shouldShowNudge(context))

        // Case 3: Only saw saju guide
        prefs.edit().clear().apply()
        prefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()
        assertFalse("Should not show with only saju guide seen",
            ElementCollectionNudgeOverlayFragment.shouldShowNudge(context))
    }

    @Test
    fun `test companion object methods are accessible`() {
        // Verify all companion methods can be called
        val fragment = ElementCollectionNudgeOverlayFragment.newInstance()
        assertNotNull("newInstance works", fragment)

        val shouldShow = ElementCollectionNudgeOverlayFragment.shouldShowNudge(context)
        assertNotNull("shouldShowNudge works", shouldShow)

        ElementCollectionNudgeOverlayFragment.markWentToSajuGuide(context)
        assertTrue("markWentToSajuGuide works", true)
    }

    @Test
    fun `test constants are unique`() {
        // Verify constants have unique values
        assertNotEquals("TAG and PREF_KEY_HAS_SEEN_NUDGE should be different",
            ElementCollectionNudgeOverlayFragment.TAG,
            ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE)

        assertNotEquals("PREF_KEY_HAS_SEEN_NUDGE and PREF_KEY_WENT_TO_SAJU_GUIDE should be different",
            ElementCollectionNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE,
            ElementCollectionNudgeOverlayFragment.PREF_KEY_WENT_TO_SAJU_GUIDE)
    }

    // ========== Pure Function Tests - calculateDialogueAdvance ==========

    @Test
    fun `test calculateDialogueAdvance advances when not at last dialogue`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Act
        val result = fragment.calculateDialogueAdvance(0, 3)

        // Assert
        assertTrue("Should advance", result.shouldAdvance)
        assertEquals("Next index should be 1", 1, result.nextIndex)
    }

    @Test
    fun `test calculateDialogueAdvance through all dialogues`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()
        val totalDialogues = fragment.dialogues.size

        // Act & Assert
        for (i in 0 until totalDialogues - 1) {
            val result = fragment.calculateDialogueAdvance(i, totalDialogues)
            assertTrue("Should advance from dialogue $i", result.shouldAdvance)
            assertEquals("Next index should be ${i + 1}", i + 1, result.nextIndex)
        }
    }

    @Test
    fun `test calculateDialogueAdvance does not advance at last dialogue`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()
        val totalDialogues = fragment.dialogues.size

        // Act
        val result = fragment.calculateDialogueAdvance(totalDialogues - 1, totalDialogues)

        // Assert
        assertFalse("Should not advance at last dialogue", result.shouldAdvance)
        assertEquals("Index should remain at last", totalDialogues - 1, result.nextIndex)
    }

    // ========== Pure Function Tests - calculateDialogueUIState ==========

    @Test
    fun `test calculateDialogueUIState for first dialogue`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Act
        val result = fragment.calculateDialogueUIState(0, 3)

        // Assert
        assertFalse("Should not be last dialogue", result.isLastDialogue)
        assertTrue("Should show arrow", result.showArrow)
        assertFalse("Should not show buttons", result.showButtons)
    }

    @Test
    fun `test calculateDialogueUIState for middle dialogues`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Act
        val result = fragment.calculateDialogueUIState(1, 3)

        // Assert
        assertFalse("Should not be last dialogue", result.isLastDialogue)
        assertTrue("Should show arrow", result.showArrow)
        assertFalse("Should not show buttons", result.showButtons)
    }

    @Test
    fun `test calculateDialogueUIState for last dialogue`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Act
        val result = fragment.calculateDialogueUIState(2, 3)

        // Assert
        assertTrue("Should be last dialogue", result.isLastDialogue)
        assertFalse("Should not show arrow", result.showArrow)
        assertTrue("Should show buttons", result.showButtons)
    }

    // ========== Pure Function Tests - getDialogueForIndex ==========

    @Test
    fun `test getDialogueForIndex returns correct dialogue`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Act & Assert
        for (i in fragment.dialogues.indices) {
            val dialogue = fragment.getDialogueForIndex(i)
            assertNotNull("Dialogue at index $i should not be null", dialogue)
            assertEquals("Dialogue should match", fragment.dialogues[i], dialogue)
        }
    }

    @Test
    fun `test getDialogueForIndex returns null for invalid index`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Act & Assert
        assertNull("Dialogue at -1 should be null", fragment.getDialogueForIndex(-1))
        assertNull("Dialogue beyond size should be null", fragment.getDialogueForIndex(100))
    }

    // ========== Data Class Tests ==========

    @Test
    fun `test DialogueAdvanceResult data class`() {
        // Act
        val result1 = ElementCollectionNudgeOverlayFragment.DialogueAdvanceResult(true, 1)
        val result2 = ElementCollectionNudgeOverlayFragment.DialogueAdvanceResult(true, 1)
        val result3 = ElementCollectionNudgeOverlayFragment.DialogueAdvanceResult(false, 0)

        // Assert
        assertEquals("Equal results should be equal", result1, result2)
        assertNotEquals("Different results should not be equal", result1, result3)
    }

    @Test
    fun `test DialogueUIState data class`() {
        // Act
        val state1 = ElementCollectionNudgeOverlayFragment.DialogueUIState(false, true, false)
        val state2 = ElementCollectionNudgeOverlayFragment.DialogueUIState(false, true, false)
        val state3 = ElementCollectionNudgeOverlayFragment.DialogueUIState(true, false, true)

        // Assert
        assertEquals("Equal states should be equal", state1, state2)
        assertNotEquals("Different states should not be equal", state1, state3)
    }

    // ========== Internal Fields Tests ==========

    @Test
    fun `test dialogues list has correct count`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Assert
        assertEquals("Dialogues should have 3 items", 3, fragment.dialogues.size)
    }

    @Test
    fun `test currentDialogueIndex initial value`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Assert
        assertEquals("Initial dialogue index should be 0", 0, fragment.currentDialogueIndex)
    }

    @Test
    fun `test currentDialogueIndex can be modified`() {
        // Arrange
        val fragment = ElementCollectionNudgeOverlayFragment()

        // Act
        fragment.currentDialogueIndex = 2

        // Assert
        assertEquals("Dialogue index should be 2", 2, fragment.currentDialogueIndex)
    }
}
