package com.example.fortuna_android.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fortuna_android.MainActivity
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests for tutorial navigation logic
 * Tests the SharedPreferences-based tutorial state management
 */
@RunWith(AndroidJUnit4::class)
class TutorialNavigationTest {

    private lateinit var context: Context
    private val PREFS_NAME = "fortuna_prefs"
    private val KEY_HOME_TUTORIAL = "has_seen_home_tutorial"
    private val KEY_AR_TUTORIAL = "has_seen_ar_tutorial"

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clear tutorial flags before each test
        clearTutorialFlags()
    }

    @After
    fun tearDown() {
        // Clean up after tests
        clearTutorialFlags()
    }

    private fun clearTutorialFlags() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            remove(KEY_HOME_TUTORIAL)
            remove(KEY_AR_TUTORIAL)
            apply()
        }
    }

    @Test
    fun testInitialState_bothTutorialsNotSeen() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val hasSeenHome = prefs.getBoolean(KEY_HOME_TUTORIAL, false)
        val hasSeenAR = prefs.getBoolean(KEY_AR_TUTORIAL, false)

        assertFalse("Home tutorial should not be seen initially", hasSeenHome)
        assertFalse("AR tutorial should not be seen initially", hasSeenAR)
    }

    @Test
    fun testHomeTutorialMarkedAsSeen() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Mark home tutorial as seen
        prefs.edit().putBoolean(KEY_HOME_TUTORIAL, true).apply()

        val hasSeenHome = prefs.getBoolean(KEY_HOME_TUTORIAL, false)
        assertTrue("Home tutorial should be marked as seen", hasSeenHome)
    }

    @Test
    fun testARTutorialMarkedAsSeen() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Mark AR tutorial as seen
        prefs.edit().putBoolean(KEY_AR_TUTORIAL, true).apply()

        val hasSeenAR = prefs.getBoolean(KEY_AR_TUTORIAL, false)
        assertTrue("AR tutorial should be marked as seen", hasSeenAR)
    }

    @Test
    fun testBothTutorialsMarkedAsSeen() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Mark both tutorials as seen
        prefs.edit().apply {
            putBoolean(KEY_HOME_TUTORIAL, true)
            putBoolean(KEY_AR_TUTORIAL, true)
            apply()
        }

        val hasSeenHome = prefs.getBoolean(KEY_HOME_TUTORIAL, false)
        val hasSeenAR = prefs.getBoolean(KEY_AR_TUTORIAL, false)

        assertTrue("Home tutorial should be marked as seen", hasSeenHome)
        assertTrue("AR tutorial should be marked as seen", hasSeenAR)
    }

    @Test
    fun testNavigationLogic_noTutorialsSeen() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val hasSeenHome = prefs.getBoolean(KEY_HOME_TUTORIAL, false)
        val hasSeenAR = prefs.getBoolean(KEY_AR_TUTORIAL, false)

        // Logic: If either tutorial not seen, should show home tutorial
        val shouldShowHomeTutorial = !hasSeenHome || !hasSeenAR

        assertTrue("Should show home tutorial when no tutorials seen", shouldShowHomeTutorial)
    }

    @Test
    fun testNavigationLogic_onlyHomeTutorialSeen() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HOME_TUTORIAL, true).apply()

        val hasSeenHome = prefs.getBoolean(KEY_HOME_TUTORIAL, false)
        val hasSeenAR = prefs.getBoolean(KEY_AR_TUTORIAL, false)

        // Logic: If AR tutorial not seen, should still show home tutorial
        val shouldShowHomeTutorial = !hasSeenHome || !hasSeenAR

        assertTrue("Should show home tutorial when only home tutorial seen", shouldShowHomeTutorial)
    }

    @Test
    fun testNavigationLogic_bothTutorialsSeen() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_HOME_TUTORIAL, true)
            putBoolean(KEY_AR_TUTORIAL, true)
            apply()
        }

        val hasSeenHome = prefs.getBoolean(KEY_HOME_TUTORIAL, false)
        val hasSeenAR = prefs.getBoolean(KEY_AR_TUTORIAL, false)

        // Logic: If both tutorials seen, should skip directly to AR
        val shouldSkipToAR = hasSeenHome && hasSeenAR

        assertTrue("Should skip to AR when both tutorials seen", shouldSkipToAR)
    }

    @Test
    fun testTutorialFlagPersistence() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Set flags
        prefs.edit().apply {
            putBoolean(KEY_HOME_TUTORIAL, true)
            putBoolean(KEY_AR_TUTORIAL, true)
            apply()
        }

        // Read flags in a new prefs instance (simulating app restart)
        val newPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasSeenHome = newPrefs.getBoolean(KEY_HOME_TUTORIAL, false)
        val hasSeenAR = newPrefs.getBoolean(KEY_AR_TUTORIAL, false)

        assertTrue("Home tutorial flag should persist", hasSeenHome)
        assertTrue("AR tutorial flag should persist", hasSeenAR)
    }

    @Test
    fun testNavigateToARIntent() {
        // Test that navigate_to_ar intent extra can be created
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("navigate_to_ar", true)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        assertTrue("Intent should have navigate_to_ar extra",
            intent.hasExtra("navigate_to_ar"))
        assertTrue("navigate_to_ar should be true",
            intent.getBooleanExtra("navigate_to_ar", false))
        assertEquals("Intent flags should include CLEAR_TOP and SINGLE_TOP",
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
            intent.flags and (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
    }

    @Test
    fun testClearTutorialFlags() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Set flags
        prefs.edit().apply {
            putBoolean(KEY_HOME_TUTORIAL, true)
            putBoolean(KEY_AR_TUTORIAL, true)
            apply()
        }

        // Clear flags
        prefs.edit().apply {
            remove(KEY_HOME_TUTORIAL)
            remove(KEY_AR_TUTORIAL)
            apply()
        }

        // Verify flags are cleared
        val hasSeenHome = prefs.getBoolean(KEY_HOME_TUTORIAL, false)
        val hasSeenAR = prefs.getBoolean(KEY_AR_TUTORIAL, false)

        assertFalse("Home tutorial flag should be cleared", hasSeenHome)
        assertFalse("AR tutorial flag should be cleared", hasSeenAR)
    }

    @Test
    fun testContextIsNotNull() {
        assertNotNull("Context should not be null", context)
        assertEquals("Context package name should match app package",
            "com.example.fortuna_android", context.packageName)
    }

    @Test
    fun testSharedPreferencesAccessible() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        assertNotNull("SharedPreferences should be accessible", prefs)
    }

    @Test
    fun testNavigationDecisionMatrix() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Test all 4 combinations of tutorial states
        val testCases = listOf(
            Pair(false, false) to true,  // No tutorials → show home tutorial
            Pair(true, false) to true,   // Only home → show home tutorial (to get to AR tutorial)
            Pair(false, true) to true,   // Only AR → show home tutorial (unusual case)
            Pair(true, true) to false    // Both seen → skip to AR
        )

        for ((tutorialStates, expectedShowTutorial) in testCases) {
            val (homeState, arState) = tutorialStates

            // Set states
            prefs.edit().apply {
                putBoolean(KEY_HOME_TUTORIAL, homeState)
                putBoolean(KEY_AR_TUTORIAL, arState)
                apply()
            }

            // Check logic
            val hasSeenHome = prefs.getBoolean(KEY_HOME_TUTORIAL, false)
            val hasSeenAR = prefs.getBoolean(KEY_AR_TUTORIAL, false)
            val shouldShowTutorial = !(hasSeenHome && hasSeenAR)

            assertEquals(
                "Navigation decision should be correct for state (home=$homeState, ar=$arState)",
                expectedShowTutorial,
                shouldShowTutorial
            )
        }
    }
}
