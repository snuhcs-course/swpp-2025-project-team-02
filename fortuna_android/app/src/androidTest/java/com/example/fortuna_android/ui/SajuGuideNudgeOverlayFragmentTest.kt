package com.example.fortuna_android.ui

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fortuna_android.R
import com.example.fortuna_android.SajuGuideNudgeOverlayFragment
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Enhanced instrumented tests for SajuGuideNudgeOverlayFragment
 * Tests fragment lifecycle, UI interactions, and real Android environment behavior
 */
@RunWith(AndroidJUnit4::class)
class SajuGuideNudgeOverlayFragmentTest {

    private lateinit var context: Context
    private lateinit var sharedPrefs: SharedPreferences

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sharedPrefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        // Clear preferences before each test
        sharedPrefs.edit().clear().apply()
    }

    @After
    fun tearDown() {
        // Clean up after each test
        sharedPrefs.edit().clear().apply()
    }

    // ========== Basic App Context Tests ==========

    @Test
    fun useAppContext() {
        // Verify app context is correct
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.fortuna_android", appContext.packageName)
    }

    // ========== Fragment Creation Tests ==========

    @Test
    fun testFragmentCreation() {
        // Test that fragment can be instantiated
        val fragment = SajuGuideNudgeOverlayFragment.Companion.newInstance()
        assertNotNull("Fragment should be created", fragment)
        assertTrue("Fragment should be SajuGuideNudgeOverlayFragment instance",
            fragment is SajuGuideNudgeOverlayFragment
        )
    }

    @Test
    fun testFragmentTag() {
        // Test that the TAG constant is correct
        assertEquals("SajuGuideNudgeOverlay", SajuGuideNudgeOverlayFragment.Companion.TAG)
    }

    @Test
    fun testNewInstanceCreation() {
        // Test that newInstance creates different instances
        val fragment1 = SajuGuideNudgeOverlayFragment.Companion.newInstance()
        val fragment2 = SajuGuideNudgeOverlayFragment.Companion.newInstance()

        assertNotNull("First fragment should be created", fragment1)
        assertNotNull("Second fragment should be created", fragment2)
        assertNotSame("Each newInstance call should create a different instance",
            fragment1, fragment2)
    }

    @Test
    fun testFragmentClassProperties() {
        // Test fragment class properties without UI dependencies
        val fragment = SajuGuideNudgeOverlayFragment.Companion.newInstance()

        // Verify fragment is properly created with no arguments
        assertNull("Fragment should have no arguments by default", fragment.arguments)

        // Verify fragment class name
        assertEquals("SajuGuideNudgeOverlayFragment", fragment.javaClass.simpleName)
    }

    // ========== Fragment Lifecycle Tests ==========

    @Test
    fun testFragmentLaunch() {
        // Test fragment can be launched in container without crashing
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment should be non-null", fragment)
            assertTrue("Fragment should be added to FragmentManager", fragment.isAdded)
            assertNotNull("Fragment should have a view", fragment.view)
        }

        scenario.close()
    }

    @Test
    fun testFragmentViewCreation() {
        // Test that fragment view is properly created
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val rootView = fragment.view
            assertNotNull("Fragment root view should exist", rootView)
            assertTrue("Fragment should be visible", fragment.isVisible)
            assertFalse("Fragment should not be detached", fragment.isDetached)
        }

        scenario.close()
    }

    @Test
    fun testFragmentLifecycleStates() {
        // Test fragment goes through proper lifecycle states
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertTrue("Fragment should be added", fragment.isAdded)
            assertTrue("Fragment should be visible", fragment.isVisible)
            assertFalse("Fragment should not be hidden", fragment.isHidden)
            assertFalse("Fragment should not be removing", fragment.isRemoving)
            assertFalse("Fragment should not be detached", fragment.isDetached)
        }

        scenario.close()
    }

    // ========== UI Component Tests ==========

    @Test
    fun testDialogueTextViewExists() {
        // Test that dialogue TextView exists and has content
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dialogueTextView = fragment.view?.findViewById<TextView>(R.id.tvDialogue)
            assertNotNull("Dialogue TextView should exist", dialogueTextView)

            val text = dialogueTextView?.text?.toString()
            assertNotNull("Dialogue text should not be null", text)
            assertFalse("Dialogue text should not be empty", text.isNullOrEmpty())

            // Should contain expected nudge text (first dialogue)
            assertTrue("Should contain nudge content",
                text!!.contains("운세가 생성되는 동안") || text.contains("사주 가이드"))
        }

        scenario.close()
    }

    @Test
    fun testArrowIndicatorVisibility() {
        // Test that arrow indicator is visible initially
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val arrowView = fragment.view?.findViewById<View>(R.id.tvArrow)
            assertNotNull("Arrow view should exist", arrowView)
            assertEquals("Arrow should be visible initially",
                View.VISIBLE, arrowView?.visibility)
        }

        scenario.close()
    }

    @Test
    fun testMascotContainerVisible() {
        // Test that mascot container is visible during nudge (unlike walkthrough)
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val mascotContainer = fragment.view?.findViewById<View>(R.id.mascotContainer)
            assertNotNull("Mascot container should exist", mascotContainer)
            assertEquals("Mascot container should be visible during nudge",
                View.VISIBLE, mascotContainer?.visibility)
        }

        scenario.close()
    }

    @Test
    fun testActionButtonsHiddenInitially() {
        // Test that action buttons are hidden initially (shown only on last dialogue)
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val laterButton = fragment.view?.findViewById<View>(R.id.btnLater)
            val guideButton = fragment.view?.findViewById<View>(R.id.btnGoToGuide)

            assertNotNull("Later button should exist", laterButton)
            assertNotNull("Guide button should exist", guideButton)
            assertEquals("Later button should be hidden initially",
                View.GONE, laterButton?.visibility)
            assertEquals("Guide button should be hidden initially",
                View.GONE, guideButton?.visibility)
        }

        scenario.close()
    }

    @Test
    fun testCloseButtonExists() {
        // Test that close button exists and is clickable
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val closeButton = fragment.view?.findViewById<View>(R.id.btnClose)
            assertNotNull("Close button should exist", closeButton)
            assertTrue("Close button should be clickable", closeButton?.isClickable == true)
            assertTrue("Close button should be enabled", closeButton?.isEnabled == true)
            assertEquals("Close button should be visible",
                View.VISIBLE, closeButton?.visibility)
        }

        scenario.close()
    }

    @Test
    fun testDialogueBoxClickable() {
        // Test that dialogue box exists and is clickable
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dialogueBox = fragment.view?.findViewById<View>(R.id.dialogueBox)
            assertNotNull("Dialogue box should exist", dialogueBox)
            assertTrue("Dialogue box should be clickable", dialogueBox?.isClickable == true)
            assertTrue("Dialogue box should be enabled", dialogueBox?.isEnabled == true)
        }

        scenario.close()
    }

    @Test
    fun testDimOverlayExists() {
        // Test that dim overlay exists and is clickable
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dimOverlay = fragment.view?.findViewById<View>(R.id.dimOverlay)
            assertNotNull("Dim overlay should exist", dimOverlay)
            assertTrue("Dim overlay should be clickable", dimOverlay?.isClickable == true)
        }

        scenario.close()
    }

    // ========== Interaction Tests ==========

    @Test
    fun testCloseButtonInteraction() {
        // Test clicking close button - it should attempt to dismiss
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        // Get context before potential fragment dismissal
        val testContext = ApplicationProvider.getApplicationContext<Context>()
        val prefs = testContext.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply() // Clear preferences

        scenario.onFragment { fragment ->
            val closeButton = fragment.view?.findViewById<View>(R.id.btnClose)
            assertNotNull("Close button should exist", closeButton)
            assertTrue("Close button should be clickable", closeButton?.isClickable == true)

            // Click close button - this will trigger dismissOverlay and markNudgeAsSeen
            closeButton?.performClick()
        }

        // Verify that close button click marked nudge as seen
        assertTrue("Should mark nudge as seen after close button click",
            prefs.getBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false))

        scenario.close()
    }

    @Test
    fun testDialogueBoxInteraction() {
        // Test clicking dialogue box for progression
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dialogueBox = fragment.view?.findViewById<View>(R.id.dialogueBox)
            assertNotNull("Dialogue box should exist", dialogueBox)
            assertTrue("Dialogue box should be clickable", dialogueBox?.isClickable == true)

            val dialogueText = fragment.view?.findViewById<TextView>(R.id.tvDialogue)
            assertNotNull("Dialogue text should exist", dialogueText)
            assertFalse("Dialogue should have text", dialogueText?.text.isNullOrEmpty())

            // Perform click to advance dialogue
            dialogueBox?.performClick()
        }

        scenario.close()
    }

    @Test
    fun testDimOverlayInteraction() {
        // Test clicking dim overlay
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dimOverlay = fragment.view?.findViewById<View>(R.id.dimOverlay)
            assertNotNull("Dim overlay should exist", dimOverlay)

            // Perform click
            dimOverlay?.performClick()

            // Fragment should remain stable
            assertTrue("Fragment should remain stable after dim overlay click", fragment.isAdded)
        }

        scenario.close()
    }

    @Test
    fun testMultipleDialogueClicks() {
        // Test clicking dialogue multiple times to advance through dialogues
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dialogueBox = fragment.view?.findViewById<View>(R.id.dialogueBox)
            val dialogueText = fragment.view?.findViewById<TextView>(R.id.tvDialogue)
            val arrowView = fragment.view?.findViewById<View>(R.id.tvArrow)
            val laterButton = fragment.view?.findViewById<View>(R.id.btnLater)
            val guideButton = fragment.view?.findViewById<View>(R.id.btnGoToGuide)

            assertNotNull("All UI components should exist", dialogueBox)
            assertNotNull("All UI components should exist", dialogueText)
            assertNotNull("All UI components should exist", arrowView)
            assertNotNull("All UI components should exist", laterButton)
            assertNotNull("All UI components should exist", guideButton)

            // Initially buttons should be hidden
            assertEquals("Later button hidden initially", View.GONE, laterButton?.visibility)
            assertEquals("Guide button hidden initially", View.GONE, guideButton?.visibility)

            // Click dialogue box multiple times
            repeat(3) {
                dialogueBox?.performClick()
            }

            // After multiple clicks, fragment should remain stable
            assertTrue("Fragment should remain stable after multiple clicks", fragment.isAdded)
        }

        scenario.close()
    }

    // ========== Companion Method Tests ==========

    @Test
    fun testShouldShowNudgeDefault() {
        // Test shouldShowNudge returns true by default
        val shouldShow = SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(context)
        assertTrue("Should show nudge by default", shouldShow)
    }

    @Test
    fun testShouldShowNudgeAfterSeen() {
        // Test shouldShowNudge returns false after being seen
        sharedPrefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.Companion.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        val shouldShow = SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(context)
        assertFalse("Should not show nudge after being seen", shouldShow)
    }

    @Test
    fun testPreferenceKeyConstant() {
        // Test that preference key constant is correct
        assertEquals("Preference key should be correct",
            "has_seen_saju_guide_nudge", SajuGuideNudgeOverlayFragment.Companion.PREF_KEY_HAS_SEEN_NUDGE)
    }

    // ========== Context and SharedPreferences Tests ==========

    @Test
    fun testFragmentContextAccess() {
        // Test that fragment has proper context access
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            assertNotNull("Fragment context should not be null", fragment.context)
            assertEquals("Fragment context should have correct package",
                "com.example.fortuna_android", fragment.requireContext().packageName)
        }

        scenario.close()
    }

    @Test
    fun testSharedPreferencesAccess() {
        // Test that fragment can access SharedPreferences
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val prefs = fragment.requireContext().getSharedPreferences(
                "fortuna_prefs", Context.MODE_PRIVATE)
            assertNotNull("SharedPreferences should be accessible", prefs)

            // Test writing to preferences
            prefs.edit().putBoolean("test_key", true).apply()
            assertTrue("Should be able to write to preferences",
                prefs.getBoolean("test_key", false))
        }

        scenario.close()
    }

    @Test
    fun testPreferenceKeyConsistency() {
        // Test that preference key used by fragment matches companion object constant
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val prefs = fragment.requireContext().getSharedPreferences(
                "fortuna_prefs", Context.MODE_PRIVATE)

            // Test nudge seen key
            prefs.edit()
                .putBoolean(SajuGuideNudgeOverlayFragment.Companion.PREF_KEY_HAS_SEEN_NUDGE, true)
                .apply()

            assertFalse("Nudge seen key should work correctly",
                SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(fragment.requireContext()))

            // Clear and test again
            prefs.edit()
                .putBoolean(SajuGuideNudgeOverlayFragment.Companion.PREF_KEY_HAS_SEEN_NUDGE, false)
                .apply()

            assertTrue("Nudge seen key should work correctly when false",
                SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(fragment.requireContext()))
        }

        scenario.close()
    }

    // ========== Error Handling Tests ==========

    @Test
    fun testFragmentRobustness() {
        // Test that fragment handles multiple lifecycle operations
        repeat(3) { iteration ->
            val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

            scenario.onFragment { fragment ->
                assertNotNull("Fragment should be created on iteration $iteration", fragment)
                assertNotNull("Fragment view should be created on iteration $iteration", fragment.view)
            }

            scenario.close()
        }
    }

    @Test
    fun testFragmentStability() {
        // Test that fragment remains stable under various operations
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Verify fragment is in a stable state
            assertTrue("Fragment should be added", fragment.isAdded)
            assertNotNull("Fragment view should be available", fragment.view)

            // Test that UI components exist and are accessible
            val dialogueBox = fragment.view?.findViewById<View>(R.id.dialogueBox)
            val dimOverlay = fragment.view?.findViewById<View>(R.id.dimOverlay)

            assertNotNull("Dialogue box should exist", dialogueBox)
            assertNotNull("Dim overlay should exist", dimOverlay)

            // Test interactions that don't dismiss the fragment
            dialogueBox?.performClick()
            dimOverlay?.performClick()
        }

        scenario.close()
    }

    @Test
    fun testPreferenceStateChanges() {
        // Test that preference state changes are handled correctly
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val fragmentContext = fragment.requireContext()

            // Initial state
            assertTrue("Should show nudge initially",
                SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(fragmentContext))

            // Change preference
            val prefs = fragmentContext.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(SajuGuideNudgeOverlayFragment.Companion.PREF_KEY_HAS_SEEN_NUDGE, true)
                .apply()

            // Updated state
            assertFalse("Should not show nudge after being seen",
                SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(fragmentContext))

            // Change back
            prefs.edit()
                .putBoolean(SajuGuideNudgeOverlayFragment.Companion.PREF_KEY_HAS_SEEN_NUDGE, false)
                .apply()

            assertTrue("Should show nudge again when set to false",
                SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(fragmentContext))
        }

        scenario.close()
    }

    // ========== Performance Tests ==========

    @Test
    fun testFragmentPerformance() {
        // Test that fragment creation is reasonably fast
        val startTime = System.currentTimeMillis()

        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val endTime = System.currentTimeMillis()
            val duration = endTime - startTime

            assertTrue("Fragment creation should be fast (< 5 seconds)", duration < 5000)
            assertNotNull("Fragment should be properly created", fragment)
        }

        scenario.close()
    }

    // ========== Integration Tests ==========

    @Test
    fun testCompleteFragmentIntegration() {
        // Test complete fragment functionality in real Android environment
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            // Verify all key components exist
            val rootView = fragment.view
            val dialogueText = fragment.view?.findViewById<TextView>(R.id.tvDialogue)
            val arrowView = fragment.view?.findViewById<View>(R.id.tvArrow)
            val mascotContainer = fragment.view?.findViewById<View>(R.id.mascotContainer)
            val closeButton = fragment.view?.findViewById<View>(R.id.btnClose)
            val dialogueBox = fragment.view?.findViewById<View>(R.id.dialogueBox)
            val dimOverlay = fragment.view?.findViewById<View>(R.id.dimOverlay)
            val laterButton = fragment.view?.findViewById<View>(R.id.btnLater)
            val guideButton = fragment.view?.findViewById<View>(R.id.btnGoToGuide)

            assertNotNull("Root view should exist", rootView)
            assertNotNull("Dialogue text should exist", dialogueText)
            assertNotNull("Arrow view should exist", arrowView)
            assertNotNull("Mascot container should exist", mascotContainer)
            assertNotNull("Close button should exist", closeButton)
            assertNotNull("Dialogue box should exist", dialogueBox)
            assertNotNull("Dim overlay should exist", dimOverlay)
            assertNotNull("Later button should exist", laterButton)
            assertNotNull("Guide button should exist", guideButton)

            // Verify proper initial state
            assertFalse("Dialogue text should have content",
                dialogueText?.text.isNullOrEmpty())
            assertEquals("Arrow should be visible",
                View.VISIBLE, arrowView?.visibility)
            assertEquals("Mascot container should be visible (unlike walkthrough)",
                View.VISIBLE, mascotContainer?.visibility)
            assertEquals("Later button should be hidden initially",
                View.GONE, laterButton?.visibility)
            assertEquals("Guide button should be hidden initially",
                View.GONE, guideButton?.visibility)
            assertTrue("Close button should be clickable",
                closeButton?.isClickable == true)
            assertTrue("Dialogue box should be clickable",
                dialogueBox?.isClickable == true)
            assertTrue("Dim overlay should be clickable",
                dimOverlay?.isClickable == true)

            // Test context access and companion methods
            assertNotNull("Fragment should have context", fragment.context)
            assertEquals("Context should be correct",
                "com.example.fortuna_android", fragment.requireContext().packageName)

            assertTrue("Should show nudge by default",
                SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(fragment.requireContext()))
        }

        scenario.close()
    }

    @Test
    fun testCompanionMethodsIntegration() {
        // Test that all companion methods work correctly in instrumented environment
        val fragment1 = SajuGuideNudgeOverlayFragment.Companion.newInstance()
        val fragment2 = SajuGuideNudgeOverlayFragment.Companion.newInstance()

        assertNotNull("Fragment 1 should be created", fragment1)
        assertNotNull("Fragment 2 should be created", fragment2)
        assertNotSame("Fragments should be different instances", fragment1, fragment2)

        assertEquals("TAG should be correct",
            "SajuGuideNudgeOverlay", SajuGuideNudgeOverlayFragment.Companion.TAG)

        assertTrue("shouldShowNudge should work",
            SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(context))

        assertEquals("Preference key should be correct",
            "has_seen_saju_guide_nudge", SajuGuideNudgeOverlayFragment.Companion.PREF_KEY_HAS_SEEN_NUDGE)
    }

    @Test
    fun testDialogueProgression() {
        // Test that dialogue can progress through multiple states
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dialogueBox = fragment.view?.findViewById<View>(R.id.dialogueBox)
            val dialogueText = fragment.view?.findViewById<TextView>(R.id.tvDialogue)
            val laterButton = fragment.view?.findViewById<View>(R.id.btnLater)
            val guideButton = fragment.view?.findViewById<View>(R.id.btnGoToGuide)

            assertNotNull("UI components should exist", dialogueBox)

            val initialText = dialogueText?.text?.toString()
            assertNotNull("Initial dialogue text should exist", initialText)

            // Initially buttons should be hidden
            assertEquals("Buttons hidden initially", View.GONE, laterButton?.visibility)
            assertEquals("Buttons hidden initially", View.GONE, guideButton?.visibility)

            // Click through dialogues
            dialogueBox?.performClick()

            // Fragment should remain stable throughout
            assertTrue("Fragment should remain stable", fragment.isAdded)
            assertNotNull("Fragment view should remain", fragment.view)
        }

        scenario.close()
    }

    @Test
    fun testNudgeWorkflow() {
        // Test complete nudge workflow
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val fragmentContext = fragment.requireContext()

            // 1. Initially should show nudge
            assertTrue("Should show nudge initially",
                SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(fragmentContext))

            // 2. Simulate user seeing the nudge
            val prefs = fragmentContext.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(SajuGuideNudgeOverlayFragment.Companion.PREF_KEY_HAS_SEEN_NUDGE, true)
                .apply()

            // 3. Should not show nudge after being seen
            assertFalse("Should not show nudge after being seen",
                SajuGuideNudgeOverlayFragment.Companion.shouldShowNudge(fragmentContext))

            // 4. UI should still be functional
            val closeButton = fragment.view?.findViewById<View>(R.id.btnClose)
            assertNotNull("Close button should remain functional", closeButton)
            assertTrue("Close button should be clickable", closeButton?.isClickable == true)
        }

        scenario.close()
    }

    @Test
    fun testNavigateToSajuGuideFlow() {
        // Test the complete flow that leads to navigateToSajuGuide() being called
        val scenario = launchFragmentInContainer<SajuGuideNudgeOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        // Get context reference before fragment operations
        val testContext = ApplicationProvider.getApplicationContext<Context>()
        val prefs = testContext.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        // Clear preferences first
        prefs.edit().clear().apply()

        scenario.onFragment { fragment ->
            val dialogueBox = fragment.view?.findViewById<View>(R.id.dialogueBox)
            val laterButton = fragment.view?.findViewById<View>(R.id.btnLater)
            val guideButton = fragment.view?.findViewById<View>(R.id.btnGoToGuide)

            assertNotNull("Dialogue box should exist", dialogueBox)
            assertNotNull("Later button should exist", laterButton)
            assertNotNull("Guide button should exist", guideButton)

            // Initially buttons should be hidden
            assertEquals("Later button hidden initially", View.GONE, laterButton?.visibility)
            assertEquals("Guide button hidden initially", View.GONE, guideButton?.visibility)

            // Click through all dialogues to reach the last one where buttons appear
            // According to the code, there are 3 dialogues (indices 0, 1, 2)
            repeat(3) {
                dialogueBox?.performClick()
            }

            // After all dialogues, buttons should be visible
            assertEquals("Later button should be visible after dialogues", View.VISIBLE, laterButton?.visibility)
            assertEquals("Guide button should be visible after dialogues", View.VISIBLE, guideButton?.visibility)

            // Click the guide button to trigger navigateToSajuGuide()
            // Note: This will attempt navigation and may dismiss the fragment
            guideButton?.performClick()
        }

        // Verify the method was called by checking preferences were marked
        // Use external context since fragment may be detached after navigation
        assertTrue("Should mark nudge as seen after guide button click",
            prefs.getBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false))
        assertTrue("Should set walkthrough flag after guide button click",
            prefs.getBoolean("start_saju_walkthrough", false))

        scenario.close()
    }
}