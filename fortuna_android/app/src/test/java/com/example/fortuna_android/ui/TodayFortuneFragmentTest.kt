package com.example.fortuna_android.ui

import androidx.fragment.app.Fragment
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for TodayFortuneFragment
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TodayFortuneFragmentTest {

    @Test
    fun `test TodayFortuneFragment instantiation`() {
        val fragment = TodayFortuneFragment()
        assertNotNull(fragment)
    }

    @Test
    fun `test TodayFortuneFragment is a Fragment`() {
        val fragment = TodayFortuneFragment()
        assertTrue(fragment is Fragment)
    }
}
