package com.example.fortuna_android.ui

import androidx.fragment.app.Fragment
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for ProfileEditFragment
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ProfileEditFragmentTest {

    @Test
    fun `test ProfileEditFragment instantiation`() {
        val fragment = ProfileEditFragment()
        assertNotNull(fragment)
    }

    @Test
    fun `test ProfileEditFragment is a Fragment`() {
        val fragment = ProfileEditFragment()
        assertTrue(fragment is Fragment)
    }
}
