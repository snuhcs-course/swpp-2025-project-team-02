package com.example.fortuna_android.ui

import androidx.fragment.app.Fragment
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for SajuGuideFragment
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class SajuGuideFragmentTest {

    @Test
    fun `test SajuGuideFragment instantiation`() {
        val fragment = SajuGuideFragment()
        assertNotNull(fragment)
    }

    @Test
    fun `test SajuGuideFragment is a Fragment`() {
        val fragment = SajuGuideFragment()
        assertTrue(fragment is Fragment)
    }
}
