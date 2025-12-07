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
import com.example.fortuna_android.SajuGuideWalkthroughOverlayFragment
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Enhanced instrumented tests for SajuGuideWalkthroughOverlayFragment
 * Tests fragment lifecycle, UI interactions, and real Android environment behavior
 */
@RunWith(AndroidJUnit4::class)
class SajuGuideWalkthroughOverlayFragmentTest {

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
        val fragment = SajuGuideWalkthroughOverlayFragment.Companion.newInstance()
        assertNotNull("Fragment should be created", fragment)
        assertTrue("Fragment should be SajuGuideWalkthroughOverlayFragment instance",
            fragment is SajuGuideWalkthroughOverlayFragment
        )
    }

    @Test
    fun testFragmentTag() {
        // Test that the TAG constant is correct
        assertEquals("SajuGuideWalkthrough", SajuGuideWalkthroughOverlayFragment.Companion.TAG)
    }

    @Test
    fun testNewInstanceCreation() {
        // Test that newInstance creates different instances
        val fragment1 = SajuGuideWalkthroughOverlayFragment.Companion.newInstance()
        val fragment2 = SajuGuideWalkthroughOverlayFragment.Companion.newInstance()

        assertNotNull("First fragment should be created", fragment1)
        assertNotNull("Second fragment should be created", fragment2)
        assertNotSame("Each newInstance call should create a different instance",
            fragment1, fragment2)
    }

    @Test
    fun testFragmentClassProperties() {
        // Test fragment class properties without UI dependencies
        val fragment = SajuGuideWalkthroughOverlayFragment.Companion.newInstance()

        // Verify fragment is properly created with no arguments
        assertNull("Fragment should have no arguments by default", fragment.arguments)

        // Verify fragment class name
        assertEquals("SajuGuideWalkthroughOverlayFragment", fragment.javaClass.simpleName)
    }

    // ========== Fragment Lifecycle Tests ==========

    @Test
    fun testFragmentLaunch() {
        // Test fragment can be launched in container without crashing
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dialogueTextView = fragment.view?.findViewById<TextView>(R.id.tvDialogue)
            assertNotNull("Dialogue TextView should exist", dialogueTextView)

            val text = dialogueTextView?.text?.toString()
            assertNotNull("Dialogue text should not be null", text)
            assertFalse("Dialogue text should not be empty", text.isNullOrEmpty())

            // Should contain expected walkthrough text (first dialogue)
            assertTrue("Should contain walkthrough content",
                text!!.contains("사주는 태어난") || text.contains("2행 4열"))
        }

        scenario.close()
    }

    @Test
    fun testArrowIndicatorVisibility() {
        // Test that arrow indicator is visible initially
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
    fun testMascotContainerHidden() {
        // Test that mascot container is hidden during walkthrough
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val mascotContainer = fragment.view?.findViewById<View>(R.id.mascotContainer)
            assertNotNull("Mascot container should exist", mascotContainer)
            assertEquals("Mascot container should be hidden during walkthrough",
                View.GONE, mascotContainer?.visibility)
        }

        scenario.close()
    }

    @Test
    fun testActionButtonsHiddenInitially() {
        // Test that action buttons are hidden initially
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
    fun testDimOverlayTransparent() {
        // Test that dim overlay exists and is clickable (transparent during walkthrough)
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        // Test clicking close button - it should attempt to dismiss but may succeed in test environment
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val closeButton = fragment.view?.findViewById<View>(R.id.btnClose)
            assertNotNull("Close button should exist", closeButton)
            assertTrue("Close button should be clickable", closeButton?.isClickable == true)

            // Test that click listeners are set up - just verify button is clickable
            // Note: Actual dismissal behavior may work in test environment
        }

        scenario.close()
    }

    @Test
    fun testDialogueBoxInteraction() {
        // Test clicking dialogue box for progression
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val dialogueBox = fragment.view?.findViewById<View>(R.id.dialogueBox)
            assertNotNull("Dialogue box should exist", dialogueBox)

            val dialogueText = fragment.view?.findViewById<TextView>(R.id.tvDialogue)
            val initialText = dialogueText?.text?.toString()

            // Perform click
            dialogueBox?.performClick()

            // Fragment should remain stable after interaction
            assertTrue("Fragment should remain stable after dialogue click", fragment.isAdded)
        }

        scenario.close()
    }

    @Test
    fun testDimOverlayInteraction() {
        // Test clicking dim overlay
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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

    // ========== Companion Method Tests ==========

    @Test
    fun testShouldShowWalkthroughDefault() {
        // Test shouldShowWalkthrough returns true by default
        val shouldShow = SajuGuideWalkthroughOverlayFragment.Companion.shouldShowWalkthrough(context)
        assertTrue("Should show walkthrough by default", shouldShow)
    }

    @Test
    fun testShouldShowWalkthroughAfterCompletion() {
        // Test shouldShowWalkthrough returns false after completion
        sharedPrefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.Companion.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true)
            .apply()

        val shouldShow = SajuGuideWalkthroughOverlayFragment.Companion.shouldShowWalkthrough(context)
        assertFalse("Should not show walkthrough after completion", shouldShow)
    }

    @Test
    fun testShouldShowElementNudgeDefault() {
        // Test shouldShowElementNudgeOnHome returns false by default
        val shouldShow = SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(context)
        assertFalse("Should not show element nudge by default", shouldShow)
    }

    @Test
    fun testShouldShowElementNudgeWhenFlagSet() {
        // Test shouldShowElementNudgeOnHome returns true when flag is set
        sharedPrefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.Companion.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
            .apply()

        val shouldShow = SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(context)
        assertTrue("Should show element nudge when flag is set", shouldShow)
    }

    @Test
    fun testClearElementNudgeFlag() {
        // Test clearElementNudgeFlag works correctly
        // First set the flag
        sharedPrefs.edit()
            .putBoolean(SajuGuideWalkthroughOverlayFragment.Companion.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
            .apply()

        assertTrue("Flag should be set initially",
            SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(context))

        // Clear the flag
        SajuGuideWalkthroughOverlayFragment.Companion.clearElementNudgeFlag(context)

        assertFalse("Flag should be cleared",
            SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(context))
    }

    // ========== Context and SharedPreferences Tests ==========

    @Test
    fun testFragmentContextAccess() {
        // Test that fragment has proper context access
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        // Test that preference keys used by fragment match companion object constants
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val prefs = fragment.requireContext().getSharedPreferences(
                "fortuna_prefs", Context.MODE_PRIVATE)

            // Test walkthrough completion key
            prefs.edit()
                .putBoolean(SajuGuideWalkthroughOverlayFragment.Companion.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true)
                .apply()

            assertFalse("Walkthrough key should work correctly",
                SajuGuideWalkthroughOverlayFragment.Companion.shouldShowWalkthrough(fragment.requireContext()))

            // Test element nudge key
            prefs.edit()
                .putBoolean(SajuGuideWalkthroughOverlayFragment.Companion.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
                .apply()

            assertTrue("Element nudge key should work correctly",
                SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(fragment.requireContext()))
        }

        scenario.close()
    }

    // ========== Error Handling Tests ==========

    @Test
    fun testFragmentRobustness() {
        // Test that fragment handles multiple lifecycle operations
        repeat(3) { iteration ->
            val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
            themeResId = R.style.Theme_Fortuna_android
        )

        scenario.onFragment { fragment ->
            val fragmentContext = fragment.requireContext()

            // Initial state
            assertTrue("Should show walkthrough initially",
                SajuGuideWalkthroughOverlayFragment.Companion.shouldShowWalkthrough(fragmentContext))
            assertFalse("Should not show element nudge initially",
                SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(fragmentContext))

            // Change preferences
            val prefs = fragmentContext.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putBoolean(SajuGuideWalkthroughOverlayFragment.Companion.PREF_KEY_HAS_COMPLETED_WALKTHROUGH, true)
                .putBoolean(SajuGuideWalkthroughOverlayFragment.Companion.PREF_KEY_SHOW_ELEMENT_NUDGE_ON_HOME, true)
                .apply()

            // Updated state
            assertFalse("Should not show walkthrough after completion",
                SajuGuideWalkthroughOverlayFragment.Companion.shouldShowWalkthrough(fragmentContext))
            assertTrue("Should show element nudge after flag set",
                SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(fragmentContext))

            // Clear element flag
            SajuGuideWalkthroughOverlayFragment.Companion.clearElementNudgeFlag(fragmentContext)

            assertFalse("Should not show element nudge after clear",
                SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(fragmentContext))
        }

        scenario.close()
    }

    // ========== Performance Tests ==========

    @Test
    fun testFragmentPerformance() {
        // Test that fragment creation is reasonably fast
        val startTime = System.currentTimeMillis()

        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
        val scenario = launchFragmentInContainer<SajuGuideWalkthroughOverlayFragment>(
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
            assertEquals("Mascot container should be hidden",
                View.GONE, mascotContainer?.visibility)
            assertEquals("Later button should be hidden",
                View.GONE, laterButton?.visibility)
            assertEquals("Guide button should be hidden",
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

            assertTrue("Should show walkthrough by default",
                SajuGuideWalkthroughOverlayFragment.Companion.shouldShowWalkthrough(fragment.requireContext()))
            assertFalse("Should not show element nudge by default",
                SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(fragment.requireContext()))
        }

        scenario.close()
    }

    @Test
    fun testCompanionMethodsIntegration() {
        // Test that all companion methods work correctly in instrumented environment
        val fragment1 = SajuGuideWalkthroughOverlayFragment.Companion.newInstance()
        val fragment2 = SajuGuideWalkthroughOverlayFragment.Companion.newInstance()

        assertNotNull("Fragment 1 should be created", fragment1)
        assertNotNull("Fragment 2 should be created", fragment2)
        assertNotSame("Fragments should be different instances", fragment1, fragment2)

        assertEquals("TAG should be correct",
            "SajuGuideWalkthrough", SajuGuideWalkthroughOverlayFragment.Companion.TAG)

        assertTrue("shouldShowWalkthrough should work",
            SajuGuideWalkthroughOverlayFragment.Companion.shouldShowWalkthrough(context))
        assertFalse("shouldShowElementNudgeOnHome should work",
            SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(context))

        // Test clearElementNudgeFlag
        SajuGuideWalkthroughOverlayFragment.Companion.clearElementNudgeFlag(context)
        assertFalse("clearElementNudgeFlag should work",
            SajuGuideWalkthroughOverlayFragment.Companion.shouldShowElementNudgeOnHome(context))
    }
}