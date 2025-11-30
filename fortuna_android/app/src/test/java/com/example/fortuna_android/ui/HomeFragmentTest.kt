package com.example.fortuna_android.ui

import androidx.fragment.app.Fragment
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for HomeFragment
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class HomeFragmentTest {

    @Test
    fun `test HomeFragment instantiation`() {
        val fragment = HomeFragment()
        assertNotNull(fragment)
    }

    @Test
    fun `test HomeFragment is a Fragment`() {
        val fragment = HomeFragment()
        assertTrue(fragment is Fragment)
    }
}
