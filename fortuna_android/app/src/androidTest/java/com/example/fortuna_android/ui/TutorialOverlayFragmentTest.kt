package com.example.fortuna_android.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.fortuna_android.TutorialOverlayFragment
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for TutorialOverlayFragment
 */
@RunWith(AndroidJUnit4::class)
class TutorialOverlayFragmentTest {

    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.example.fortuna_android", appContext.packageName)
    }

    @Test
    fun testFragmentCreation() {
        val fragment = TutorialOverlayFragment.newInstance()
        assertNotNull("Fragment should be created", fragment)
        assertTrue("Fragment should be TutorialOverlayFragment instance",
            fragment is TutorialOverlayFragment)
    }

    @Test
    fun testFragmentTag() {
        assertEquals("TutorialOverlayFragment", TutorialOverlayFragment.TAG)
    }

    @Test
    fun testNewInstanceCreation() {
        val fragment1 = TutorialOverlayFragment.newInstance()
        val fragment2 = TutorialOverlayFragment.newInstance()

        assertNotNull("First fragment should be created", fragment1)
        assertNotNull("Second fragment should be created", fragment2)
        assertNotSame("Each newInstance call should create a different instance",
            fragment1, fragment2)
    }
}
