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
 * Unit tests for SajuGuideNudgeOverlayFragment
 * Tests fragment lifecycle, companion methods, and SharedPreferences logic
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SajuGuideNudgeOverlayFragmentTest {

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
        val fragment = SajuGuideNudgeOverlayFragment()

        // Assert
        assertNotNull("Fragment should not be null", fragment)
    }

    @Test
    fun `test fragment is a Fragment`() {
        // Act
        val fragment = SajuGuideNudgeOverlayFragment()

        // Assert
        assertTrue("Should be a Fragment",
            fragment is androidx.fragment.app.Fragment)
    }

    @Test
    fun `test fragment can be created multiple times`() {
        // Act
        val fragment1 = SajuGuideNudgeOverlayFragment()
        val fragment2 = SajuGuideNudgeOverlayFragment()

        // Assert
        assertNotNull("Fragment 1 should not be null", fragment1)
        assertNotNull("Fragment 2 should not be null", fragment2)
        assertNotSame("Fragments should be different instances", fragment1, fragment2)
    }

    // ========== Companion Object - newInstance Tests ==========

    @Test
    fun `test newInstance creates fragment`() {
        // Act
        val fragment = SajuGuideNudgeOverlayFragment.newInstance()

        // Assert
        assertNotNull("newInstance should create a fragment", fragment)
        assertTrue("newInstance should create SajuGuideNudgeOverlayFragment",
            fragment is SajuGuideNudgeOverlayFragment)
    }

    @Test
    fun `test newInstance creates different instances`() {
        // Act
        val fragment1 = SajuGuideNudgeOverlayFragment.newInstance()
        val fragment2 = SajuGuideNudgeOverlayFragment.newInstance()

        // Assert
        assertNotNull("Fragment 1 should not be null", fragment1)
        assertNotNull("Fragment 2 should not be null", fragment2)
        assertNotSame("Each call should create a new instance", fragment1, fragment2)
    }

    @Test
    fun `test newInstance returns correct type`() {
        // Act
        val fragment = SajuGuideNudgeOverlayFragment.newInstance()

        // Assert
        assertEquals("newInstance should return exact type",
            SajuGuideNudgeOverlayFragment::class.java,
            fragment::class.java)
    }

    // ========== Companion Object - shouldShowNudge Tests ==========

    @Test
    fun `test shouldShowNudge returns true when not seen`() {
        // Arrange - Fresh preferences, nudge not seen

        // Act
        val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertTrue("Should show nudge when not seen before", shouldShow)
    }

    @Test
    fun `test shouldShowNudge returns false when already seen`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        // Act
        val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertFalse("Should not show nudge when already seen", shouldShow)
    }

    @Test
    fun `test shouldShowNudge with explicit false preference`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false)
            .apply()

        // Act
        val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertTrue("Should show nudge when explicitly set to false", shouldShow)
    }

    @Test
    fun `test shouldShowNudge called multiple times`() {
        // Act - Call multiple times without changing preferences
        val result1 = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)
        val result2 = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)
        val result3 = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert - Results should be consistent
        assertEquals("Results should be consistent", result1, result2)
        assertEquals("Results should be consistent", result2, result3)
        assertTrue("Should show nudge on all calls", result1)
    }

    @Test
    fun `test shouldShowNudge after marking as seen`() {
        // Arrange & Act
        val beforeSeen = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Mark as seen
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        val afterSeen = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertTrue("Should show before marking as seen", beforeSeen)
        assertFalse("Should not show after marking as seen", afterSeen)
    }

    // ========== Companion Object - Constants Tests ==========

    @Test
    fun `test TAG constant`() {
        // Act
        val tag = SajuGuideNudgeOverlayFragment.TAG

        // Assert
        assertEquals("TAG should be 'SajuGuideNudgeOverlay'",
            "SajuGuideNudgeOverlay", tag)
    }

    @Test
    fun `test PREF_KEY_HAS_SEEN_NUDGE constant`() {
        // Act
        val key = SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE

        // Assert
        assertEquals("Key should be 'has_seen_saju_guide_nudge'",
            "has_seen_saju_guide_nudge", key)
    }

    @Test
    fun `test constants are not empty`() {
        // Assert
        assertFalse("TAG should not be empty",
            SajuGuideNudgeOverlayFragment.TAG.isEmpty())
        assertFalse("PREF_KEY_HAS_SEEN_NUDGE should not be empty",
            SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE.isEmpty())
    }

    @Test
    fun `test constants are unique`() {
        // Assert
        assertNotEquals("TAG and PREF_KEY should be different",
            SajuGuideNudgeOverlayFragment.TAG,
            SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE)
    }

    // ========== SharedPreferences Integration Tests ==========

    @Test
    fun `test preference key is correct`() {
        // Arrange
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("has_seen_saju_guide_nudge", true)
            .apply()

        // Act
        val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertFalse("Should read from correct preference key", shouldShow)
    }

    @Test
    fun `test preference isolation from other preferences`() {
        // Arrange - Set other unrelated preferences
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("some_other_preference", true)
            .putBoolean("another_preference", false)
            .putString("string_pref", "value")
            .apply()

        // Act
        val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertTrue("Should not be affected by other preferences", shouldShow)
    }

    @Test
    fun `test preference uses correct SharedPreferences file`() {
        // Arrange - Set preference in different file
        val wrongPrefs = context.getSharedPreferences("wrong_prefs", Context.MODE_PRIVATE)
        wrongPrefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        // Act
        val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertTrue("Should use 'fortuna_prefs' file only", shouldShow)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test complete user workflow`() {
        // 1. User opens app for first time
        val firstTime = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)
        assertTrue("Should show nudge on first time", firstTime)

        // 2. User sees the nudge
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        // 3. User opens app again
        val secondTime = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)
        assertFalse("Should not show nudge on second time", secondTime)
    }

    @Test
    fun `test fragment creation and companion methods`() {
        // Test that fragment can be created and companion methods work together
        val fragment1 = SajuGuideNudgeOverlayFragment.newInstance()
        val shouldShow1 = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        val fragment2 = SajuGuideNudgeOverlayFragment.newInstance()
        val shouldShow2 = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertNotNull("Fragment 1 created", fragment1)
        assertNotNull("Fragment 2 created", fragment2)
        assertEquals("shouldShowNudge consistent", shouldShow1, shouldShow2)
    }

    @Test
    fun `test preference state persistence`() {
        // Set preference to true
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        // Check multiple times
        repeat(5) { i ->
            val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)
            assertFalse("Should remain false on call $i", shouldShow)
        }
    }

    @Test
    fun `test boolean inversion logic`() {
        // shouldShowNudge returns !hasSeenNudge
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)

        // Test false -> should show (true)
        prefs.edit().putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, false).apply()
        assertTrue("false pref should return true", SajuGuideNudgeOverlayFragment.shouldShowNudge(context))

        // Test true -> should not show (false)
        prefs.edit().putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true).apply()
        assertFalse("true pref should return false", SajuGuideNudgeOverlayFragment.shouldShowNudge(context))
    }

    @Test
    fun `test companion object accessibility`() {
        // Verify all companion object members are accessible
        assertNotNull("TAG is accessible", SajuGuideNudgeOverlayFragment.TAG)
        assertNotNull("PREF_KEY is accessible", SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE)
        assertNotNull("newInstance is accessible", SajuGuideNudgeOverlayFragment.newInstance())

        val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)
        assertNotNull("shouldShowNudge is accessible", shouldShow)
    }

    @Test
    fun `test different context instances`() {
        // Use application context
        val appContext = ApplicationProvider.getApplicationContext<Context>()

        // Set preference using one context
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(SajuGuideNudgeOverlayFragment.PREF_KEY_HAS_SEEN_NUDGE, true)
            .apply()

        // Read using different context (should still work)
        val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(appContext)

        // Assert
        assertFalse("Should work with different context instances", shouldShow)
    }

    @Test
    fun `test preference default value behavior`() {
        // When preference doesn't exist, getBoolean returns default (false)
        // shouldShowNudge negates it, so returns true

        // Don't set any preference
        val shouldShow = SajuGuideNudgeOverlayFragment.shouldShowNudge(context)

        // Assert
        assertTrue("Should return true when preference doesn't exist (default false)", shouldShow)
    }
}
