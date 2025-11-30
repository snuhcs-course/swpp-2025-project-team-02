package com.example.fortuna_android.ui

import androidx.fragment.app.Fragment
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ProfileFragment
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileFragmentTest {

    @Test
    fun `test ProfileFragment instantiation`() {
        val fragment = ProfileFragment()
        assertNotNull(fragment)
    }

    @Test
    fun `test ProfileFragment is a Fragment`() {
        val fragment = ProfileFragment()
        assertTrue(fragment is Fragment)
    }
}
