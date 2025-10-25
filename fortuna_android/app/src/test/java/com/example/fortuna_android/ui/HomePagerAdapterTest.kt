package com.example.fortuna_android.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for HomePagerAdapter
 * Tests ViewPager2 adapter functionality with 100% coverage
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomePagerAdapterTest {

    private lateinit var activity: FragmentActivity
    private lateinit var parentFragment: Fragment
    private lateinit var adapter: HomePagerAdapter

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(FragmentActivity::class.java)
            .create()
            .start()
            .resume()
            .get()

        // Create a parent fragment to pass to the adapter
        parentFragment = Fragment()
        activity.supportFragmentManager.beginTransaction()
            .add(parentFragment, "parent")
            .commitNow()

        adapter = HomePagerAdapter(parentFragment)
    }

    @Test
    fun `test getItemCount returns 2`() {
        // Act
        val count = adapter.itemCount

        // Assert
        assertEquals("Item count should be 2", 2, count)
    }

    @Test
    fun `test createFragment returns TodayFortuneFragment at position 0`() {
        // Act
        val fragment = adapter.createFragment(0)

        // Assert
        assertNotNull("Fragment should not be null", fragment)
        assertTrue("Fragment at position 0 should be TodayFortuneFragment",
            fragment is TodayFortuneFragment)
    }

    @Test
    fun `test createFragment returns DetailAnalysisFragment at position 1`() {
        // Act
        val fragment = adapter.createFragment(1)

        // Assert
        assertNotNull("Fragment should not be null", fragment)
        assertTrue("Fragment at position 1 should be DetailAnalysisFragment",
            fragment is DetailAnalysisFragment)
    }

    @Test
    fun `test createFragment creates new instances each time`() {
        // Act
        val fragment1 = adapter.createFragment(0)
        val fragment2 = adapter.createFragment(0)

        // Assert
        assertNotSame("Should create new instances each time", fragment1, fragment2)
    }

    @Test
    fun `test adapter can be created with different parent fragment`() {
        // Arrange
        val newActivity = Robolectric.buildActivity(FragmentActivity::class.java)
            .create()
            .start()
            .resume()
            .get()
        val newParentFragment = Fragment()
        newActivity.supportFragmentManager.beginTransaction()
            .add(newParentFragment, "new_parent")
            .commitNow()

        // Act
        val newAdapter = HomePagerAdapter(newParentFragment)

        // Assert
        assertNotNull("New adapter should not be null", newAdapter)
        assertEquals("Item count should be 2", 2, newAdapter.itemCount)
    }

    @Test
    fun `test fragments at different positions are different types`() {
        // Act
        val fragment0 = adapter.createFragment(0)
        val fragment1 = adapter.createFragment(1)

        // Assert
        assertNotEquals("Fragments should be of different types",
            fragment0::class.java, fragment1::class.java)
    }
}
