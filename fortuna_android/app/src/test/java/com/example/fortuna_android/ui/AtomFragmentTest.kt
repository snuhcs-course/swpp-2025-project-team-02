package com.example.fortuna_android.ui

import androidx.fragment.app.Fragment
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for AtomFragment
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AtomFragmentTest {

    @Test
    fun `test AtomFragment instantiation`() {
        val fragment = AtomFragment()
        assertNotNull(fragment)
    }

    @Test
    fun `test AtomFragment is a Fragment`() {
        val fragment = AtomFragment()
        assertTrue(fragment is Fragment)
    }
}
