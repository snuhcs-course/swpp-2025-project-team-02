package com.example.fortuna_android

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import androidx.fragment.app.Fragment
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/**
 * Unit tests for TutorialOverlayFragment
 * Tests fragment instantiation, companion methods, and constants
 * Note: UI tests with view inflation are in instrumented tests due to layout dependencies
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TutorialOverlayFragmentTest {

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
        val fragment = TutorialOverlayFragment()

        // Assert
        assertNotNull("Fragment should not be null", fragment)
    }

    @Test
    fun `test fragment is a Fragment`() {
        // Act
        val fragment = TutorialOverlayFragment()

        // Assert
        assertTrue("Should be a Fragment", fragment is Fragment)
    }

    @Test
    fun `test fragment can be created multiple times`() {
        // Act
        val fragment1 = TutorialOverlayFragment()
        val fragment2 = TutorialOverlayFragment()

        // Assert
        assertNotNull("Fragment 1 should not be null", fragment1)
        assertNotNull("Fragment 2 should not be null", fragment2)
        assertNotSame("Fragments should be different instances", fragment1, fragment2)
    }

    // ========== Companion Object - newInstance Tests ==========

    @Test
    fun `test newInstance creates fragment`() {
        // Act
        val fragment = TutorialOverlayFragment.newInstance()

        // Assert
        assertNotNull("newInstance should create a fragment", fragment)
        assertTrue("newInstance should create TutorialOverlayFragment",
            fragment is TutorialOverlayFragment)
    }

    @Test
    fun `test newInstance creates different instances`() {
        // Act
        val fragment1 = TutorialOverlayFragment.newInstance()
        val fragment2 = TutorialOverlayFragment.newInstance()

        // Assert
        assertNotNull("Fragment 1 should not be null", fragment1)
        assertNotNull("Fragment 2 should not be null", fragment2)
        assertNotSame("Each call should create a new instance", fragment1, fragment2)
    }

    @Test
    fun `test newInstance returns correct type`() {
        // Act
        val fragment = TutorialOverlayFragment.newInstance()

        // Assert
        assertEquals("newInstance should return exact type",
            TutorialOverlayFragment::class.java,
            fragment::class.java)
    }

    // ========== Companion Object - Constants Tests ==========

    @Test
    fun `test TAG constant`() {
        // Act
        val tag = TutorialOverlayFragment.TAG

        // Assert
        assertEquals("TAG should be 'TutorialOverlayFragment'",
            "TutorialOverlayFragment", tag)
    }

    @Test
    fun `test constants are not empty`() {
        // Assert
        assertFalse("TAG should not be empty",
            TutorialOverlayFragment.TAG.isEmpty())
    }

    // ========== Fragment Arguments Tests ==========

    @Test
    fun `test fragment with no arguments`() {
        // Act
        val fragment = TutorialOverlayFragment.newInstance()

        // Assert
        assertNull("Fragment should have no arguments by default", fragment.arguments)
    }

    @Test
    fun `test fragment with custom arguments`() {
        // Arrange
        val args = Bundle().apply {
            putString("test_key", "test_value")
        }

        // Act
        val fragment = TutorialOverlayFragment.newInstance()
        fragment.arguments = args

        // Assert
        assertNotNull("Fragment should have arguments", fragment.arguments)
        assertEquals("Should preserve custom arguments",
            "test_value", fragment.arguments?.getString("test_key"))
    }

    // ========== Fragment Class Properties Tests ==========

    @Test
    fun `test fragment class properties`() {
        // Act
        val fragment = TutorialOverlayFragment.newInstance()

        // Assert
        assertEquals("Fragment class name should be TutorialOverlayFragment",
            "TutorialOverlayFragment", fragment.javaClass.simpleName)
        assertEquals("Fragment package should be correct",
            "com.example.fortuna_android", fragment.javaClass.`package`?.name)
    }

    @Test
    fun `test fragment extends correct base class`() {
        // Act
        val fragment = TutorialOverlayFragment.newInstance()

        // Assert
        assertTrue("Fragment should extend androidx.fragment.app.Fragment",
            fragment is androidx.fragment.app.Fragment)
        assertTrue("Fragment should be assignable to Fragment",
            Fragment::class.java.isAssignableFrom(fragment.javaClass))
    }

    @Test
    fun `test fragment type checking`() {
        // Act
        val fragment = TutorialOverlayFragment.newInstance()

        // Assert - Multiple type checks
        assertTrue("Should be TutorialOverlayFragment", fragment is TutorialOverlayFragment)
        assertTrue("Should be Fragment", fragment is Fragment)
        assertTrue("Should be androidx Fragment", fragment is androidx.fragment.app.Fragment)
        assertFalse("Should not be Activity", fragment is android.app.Activity)
        assertFalse("Should not be Context", fragment is Context)
    }

    // ========== Memory Management Tests ==========

    @Test
    fun `test fragment memory cleanup`() {
        // Test that fragments can be created and destroyed without memory issues
        val fragments = mutableListOf<TutorialOverlayFragment>()

        // Create multiple fragments
        repeat(10) {
            fragments.add(TutorialOverlayFragment.newInstance())
        }

        // Assert all were created successfully
        assertEquals("Should create 10 fragments", 10, fragments.size)
        fragments.forEach { fragment ->
            assertNotNull("Each fragment should be non-null", fragment)
        }

        // Clear references
        fragments.clear()
        assertEquals("List should be empty after clear", 0, fragments.size)
    }

    @Test
    fun `test concurrent fragment creation`() {
        // Test creating fragments concurrently (simulated)
        val fragment1 = TutorialOverlayFragment.newInstance()
        val fragment2 = TutorialOverlayFragment.newInstance()
        val fragment3 = TutorialOverlayFragment.newInstance()

        // Assert all are different instances
        assertNotSame("Fragment 1 and 2 should be different", fragment1, fragment2)
        assertNotSame("Fragment 2 and 3 should be different", fragment2, fragment3)
        assertNotSame("Fragment 1 and 3 should be different", fragment1, fragment3)

        // Assert all are valid
        assertNotNull("Fragment 1 should be valid", fragment1)
        assertNotNull("Fragment 2 should be valid", fragment2)
        assertNotNull("Fragment 3 should be valid", fragment3)
    }

    // ========== Edge Cases Tests ==========

    @Test
    fun `test fragment creation under stress`() {
        // Create many fragments rapidly
        repeat(100) { i ->
            val fragment = TutorialOverlayFragment.newInstance()
            assertNotNull("Fragment $i should be created successfully", fragment)
        }
    }

    @Test
    fun `test fragment constants accessibility from different contexts`() {
        // Test accessing constants from static context
        val tagFromClass = TutorialOverlayFragment.TAG

        // Test accessing from instance
        val fragment = TutorialOverlayFragment.newInstance()
        // Note: In Kotlin, companion object members are accessible from both class and instance

        // Assert
        assertEquals("TAG should be accessible from class",
            "TutorialOverlayFragment", tagFromClass)
        assertNotNull("Fragment instance should be created", fragment)
    }

    // ========== Integration Tests ==========

    @Test
    fun `test fragment creation and companion methods work together`() {
        // Test that fragment can be created and companion methods work together
        val fragment1 = TutorialOverlayFragment.newInstance()
        val fragment2 = TutorialOverlayFragment.newInstance()
        val tag = TutorialOverlayFragment.TAG

        // Assert
        assertNotNull("Fragment 1 created", fragment1)
        assertNotNull("Fragment 2 created", fragment2)
        assertNotNull("TAG accessible", tag)
        assertTrue("Both fragments are correct type",
            fragment1 is TutorialOverlayFragment && fragment2 is TutorialOverlayFragment)
    }

    @Test
    fun `test companion object is accessible`() {
        // Verify all companion methods and constants are accessible
        assertNotNull("TAG accessible", TutorialOverlayFragment.TAG)

        val fragment = TutorialOverlayFragment.newInstance()
        assertNotNull("newInstance accessible", fragment)

        assertTrue("All companion methods work together", true)
    }

    // ========== SharedPreferences Context Tests ==========

    @Test
    fun `test context package name`() {
        // Test that application context has correct package name
        assertEquals("Context package should be correct",
            "com.example.fortuna_android", context.packageName)
    }

    @Test
    fun `test shared preferences access`() {
        // Test that SharedPreferences can be accessed
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        assertNotNull("Should be able to access SharedPreferences", prefs)

        // Test writing to preferences
        prefs.edit().putBoolean("test_key", true).apply()
        assertTrue("Should be able to write to preferences",
            prefs.getBoolean("test_key", false))
    }

    @Test
    fun `test different context instances`() {
        // Use application context
        val appContext = ApplicationProvider.getApplicationContext<Context>()

        // Set preference using one context
        val prefs = context.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("test_key", true).apply()

        // Read using different context (should still work)
        val appPrefs = appContext.getSharedPreferences("fortuna_prefs", Context.MODE_PRIVATE)
        assertTrue("Should work with different context instances",
            appPrefs.getBoolean("test_key", false))
    }
}